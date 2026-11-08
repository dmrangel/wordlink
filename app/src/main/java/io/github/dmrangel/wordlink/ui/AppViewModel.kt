package io.github.dmrangel.wordlink.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dmrangel.wordlink.data.Backgrounds
import io.github.dmrangel.wordlink.data.Profile
import io.github.dmrangel.wordlink.data.Settings
import io.github.dmrangel.wordlink.data.Store
import io.github.dmrangel.wordlink.game.Dictionary
import io.github.dmrangel.wordlink.game.GameSession
import io.github.dmrangel.wordlink.game.Language
import io.github.dmrangel.wordlink.game.LevelGenerator
import io.github.dmrangel.wordlink.game.LevelId
import io.github.dmrangel.wordlink.ui.theme.Customization
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

enum class Screen { Home, Game, Settings }

/** Shown over the board once every word is found. */
data class Completion(val id: LevelId, val reward: Int, val words: Int, val bonus: Int)

/** Short message at the top of the game screen. */
data class Toast(val kind: Kind, val amount: Int = 0, val id: Long = System.nanoTime()) {
    enum class Kind { JarReward, NotEnoughCoins }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)

    var settings by mutableStateOf(store.settings())
        private set
    var profile by mutableStateOf(store.profile())
        private set
    var screen by mutableStateOf(Screen.Home)
        private set
    var session by mutableStateOf<GameSession?>(null)
        private set
    var completion by mutableStateOf<Completion?>(null)
        private set
    var toast by mutableStateOf<Toast?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    private val generators = HashMap<Language, Deferred<LevelGenerator>>()
    private var opening: Job? = null

    init {
        generator(settings.language)
    }

    val today: LocalDate get() = LocalDate.now()

    private fun generator(language: Language): Deferred<LevelGenerator> = generators.getOrPut(language) {
        viewModelScope.async(Dispatchers.Default) {
            val text = getApplication<Application>().assets.open("dict/${language.code}.txt").use { it.readBytes().decodeToString() }
            LevelGenerator(Dictionary.parse(language, text))
        }
    }

    var importingBackground by mutableStateOf(false)
        private set

    /** Applies a settings change; sliders pass persist = false while dragging. */
    fun update(persist: Boolean = true, transform: (Settings) -> Settings) {
        val next = transform(settings)
        if (next != settings) {
            settings = next
            generator(next.language)
        }
        if (persist) store.save(settings)
    }

    fun customize(persist: Boolean = true, transform: (Customization) -> Customization) =
        update(persist) { it.copy(custom = transform(it.custom)) }

    fun setBackground(uri: Uri) {
        if (importingBackground) return
        importingBackground = true
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { Backgrounds.import(getApplication(), uri) }
            if (name != null) customize { it.copy(background = name) }
            importingBackground = false
        }
    }

    fun clearBackground() {
        Backgrounds.clear(getApplication())
        customize { it.copy(background = null) }
    }

    fun resetCustomization() {
        Backgrounds.clear(getApplication())
        customize { Customization() }
    }

    fun navigate(to: Screen) {
        if (to != Screen.Game) completion = null
        screen = to
    }

    fun play(daily: Boolean = false) {
        if (opening?.isActive == true) return
        val language = settings.language
        opening = viewModelScope.launch {
            loading = true
            val generator = generator(language).await()
            val level = withContext(Dispatchers.Default) {
                if (daily) generator.daily(today) else generator.campaign(profile.level(language))
            }
            (level.id as? LevelId.Daily)?.let(store::clearStaleDailies)
            completion = null
            val next = GameSession(level, generator.dictionary, store.progress(level.id), listener)
            session = next
            screen = Screen.Game
            loading = false
            // Progress saved before a fix to the solved-check can already be complete.
            if (next.isSolved) listener.onSolved(next)
        }
    }

    fun dismissToast(id: Long) {
        if (toast?.id == id) toast = null
    }

    private fun updateProfile(transform: (Profile) -> Profile) {
        profile = transform(profile)
        store.save(profile)
    }

    private val listener = object : GameSession.Listener {
        override fun onProgress(session: GameSession) {
            if (!session.isSolved) store.save(session.level.id, session.snapshot())
        }

        override fun onWordFound(bonus: Boolean) {
            if (!bonus) {
                updateProfile { it.copy(wordsFound = it.wordsFound + 1) }
                return
            }
            val full = profile.jar + 1 >= JAR_SIZE
            updateProfile {
                it.copy(
                    bonusFound = it.bonusFound + 1,
                    jar = if (full) 0 else it.jar + 1,
                    coins = if (full) it.coins + JAR_REWARD else it.coins,
                )
            }
            if (full) toast = Toast(Toast.Kind.JarReward, JAR_REWARD)
        }

        override fun trySpend(coins: Int): Boolean {
            if (profile.coins < coins) {
                toast = Toast(Toast.Kind.NotEnoughCoins)
                return false
            }
            updateProfile { it.copy(coins = it.coins - coins) }
            return true
        }

        override fun onSolved(session: GameSession) {
            val id = session.level.id
            store.clear(id)
            val reward = when (id) {
                is LevelId.Campaign -> LEVEL_REWARD
                is LevelId.Daily -> if (profile.dailySolved(id.language, id.date)) 0 else DAILY_REWARD
            }
            updateProfile { p ->
                when (id) {
                    is LevelId.Campaign -> p.copy(
                        coins = p.coins + reward,
                        levels = p.levels + (id.language to maxOf(p.level(id.language), id.number + 1)),
                    )
                    is LevelId.Daily -> {
                        val streak = when (p.lastDaily) {
                            id.date -> p.streak
                            id.date.minusDays(1) -> p.streak + 1
                            else -> 1
                        }
                        p.copy(
                            coins = p.coins + reward,
                            streak = streak,
                            lastDaily = id.date,
                            dailyDone = p.dailyDone + (id.language to id.date),
                        )
                    }
                }
            }
            completion = Completion(id, reward, session.found.size, session.bonus.size)
        }
    }

    companion object {
        // A hint costs about four levels of income, so helpers stay a choice.
        const val HINT_COST = 40
        const val TARGET_COST = 80
        const val LEVEL_REWARD = 10
        const val DAILY_REWARD = 50
        const val JAR_SIZE = 10
        const val JAR_REWARD = 20
    }
}
