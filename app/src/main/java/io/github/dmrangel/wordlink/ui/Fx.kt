package io.github.dmrangel.wordlink.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.dmrangel.wordlink.R
import kotlin.math.pow

/**
 * Sound effects and haptics. SoundPool keeps the decoded clips in memory,
 * so playing one is a cheap, allocation-free call on the UI thread.
 */
class Fx(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val tap = pool.load(context, R.raw.tap, 1)
    private val word = pool.load(context, R.raw.word, 1)
    private val bonus = pool.load(context, R.raw.bonus, 1)
    private val error = pool.load(context, R.raw.error, 1)
    private val shuffle = pool.load(context, R.raw.shuffle, 1)
    private val complete = pool.load(context, R.raw.complete, 1)

    var view: View? = null
    var sound = true
    var haptics = true

    /** Each extra letter plays one whole tone higher, like a rising scale. */
    fun letter(index: Int) {
        play(tap, 0.7f, 2f.pow(index * 2 / 12f).coerceAtMost(2f))
        haptic(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun word() {
        play(word, 0.8f)
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun bonus() {
        play(bonus, 0.7f)
        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun error() {
        play(error, 0.7f)
        haptic(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    }

    fun shuffle() {
        play(shuffle, 0.6f)
        haptic(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun complete() {
        play(complete, 0.8f)
        haptic(HapticFeedbackConstants.LONG_PRESS)
    }

    fun tick() = haptic(HapticFeedbackConstants.KEYBOARD_TAP)

    private fun play(sound: Int, volume: Float, rate: Float = 1f) {
        if (this.sound) pool.play(sound, volume, volume, 1, 0, rate)
    }

    private fun haptic(type: Int) {
        if (haptics) view?.performHapticFeedback(type)
    }

    fun release() = pool.release()
}

val LocalFx = staticCompositionLocalOf<Fx> { error("Fx not provided") }
