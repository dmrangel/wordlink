import { campaignLevel, dailyLevel, engine, type LevelId } from './engine'
import { fx } from './fx'
import { setLang, type Lang } from './i18n'
import { GameSession, type Listener } from './session'
import * as store from './store'
import type { Profile, Settings } from './store'
import { applyPalette, resolvePalette, type Customization } from './theme'

// App state and actions, mirroring app/.../ui/AppViewModel.kt.

// A hint costs about four levels of income, so helpers stay a choice.
export const HINT_COST = 40
export const TARGET_COST = 80
export const LEVEL_REWARD = 10
export const DAILY_REWARD = 50
export const JAR_SIZE = 10
export const JAR_REWARD = 20

export type Screen = 'home' | 'game' | 'settings'

/** Shown over the board once every word is found. */
export interface Completion {
  key: string
  daily: boolean
  reward: number
  words: number
  bonus: number
}

export interface Toast {
  kind: 'jar' | 'coins'
  amount: number
  id: number
}

/** What changed: 'screen' rebuilds the current screen, the others are partial updates. */
export type Change = 'screen' | 'profile' | 'toast' | 'completion'

let toastIds = 0

class App {
  settings: Settings = store.loadSettings()
  profile: Profile = store.loadProfile()
  screen: Screen = 'home'
  session: GameSession | null = null
  completion: Completion | null = null
  toast: Toast | null = null
  loading = false
  loadError = false
  private listeners = new Set<(change: Change) => void>()

  constructor() {
    setLang(this.settings.language)
    fx.sound = this.settings.sound
    fx.haptics = this.settings.haptics
  }

  subscribe(listener: (change: Change) => void): () => void {
    this.listeners.add(listener)
    return () => this.listeners.delete(listener)
  }

  private emit(change: Change): void {
    for (const listener of [...this.listeners]) listener(change)
  }

  get today(): number {
    return store.today()
  }

  level(language: string = this.settings.language): number {
    return store.levelOf(this.profile, language)
  }

  dailySolved(language: string = this.settings.language): boolean {
    return this.profile.dailyDone[language] === this.today
  }

  /** The streak still counts if the last daily was solved today or yesterday. */
  get streak(): number {
    const last = this.profile.lastDaily
    return last !== null && last >= this.today - 1 ? this.profile.streak : 0
  }

  applyTheme(): void {
    applyPalette(resolvePalette(this.settings.theme, this.settings.custom), this.settings.custom)
  }

  /** Applies a settings change; sliders pass persist = false while dragging. */
  update(transform: (s: Settings) => Settings, { persist = true, rerender = true } = {}): void {
    const previous = this.settings
    this.settings = transform(previous)
    if (this.settings.language !== previous.language) {
      setLang(this.settings.language)
      void engine(this.settings.language).catch(() => undefined)
    }
    fx.sound = this.settings.sound
    fx.haptics = this.settings.haptics
    this.applyTheme()
    if (persist) store.saveSettings(this.settings)
    if (rerender) this.emit('screen')
  }

  customize(transform: (c: Customization) => Customization, options?: { persist?: boolean; rerender?: boolean }): void {
    this.update((s) => ({ ...s, custom: transform(s.custom) }), options)
  }

  navigate(to: Screen): void {
    if (to !== 'game') this.completion = null
    if (this.session) this.session.targeting = false
    this.screen = to
    this.emit('screen')
  }

  async play(daily = false): Promise<void> {
    if (this.loading) return
    const language = this.settings.language
    this.loading = true
    this.loadError = false
    this.emit('screen')
    try {
      const generator = await engine(language)
      const level = daily ? await dailyLevel(language, this.today) : await campaignLevel(language, this.level(language))
      if (level.id.kind === 'daily') store.clearStaleDailies(level.key)
      this.completion = null
      const session = new GameSession(level, generator, store.loadProgress(level.key), this.listener)
      this.session = session
      this.screen = 'game'
      this.loading = false
      this.emit('screen')
      // Progress saved by an older version can already be complete.
      if (session.isSolved) this.listener.onSolved(session)
    } catch (error) {
      console.error(error)
      this.loading = false
      this.loadError = true
      this.emit('screen')
    }
  }

  /** Builds the next level's word bucket while the player is still on the home screen. */
  warmUp(): void {
    const language = this.settings.language
    void campaignLevel(language, this.level(language)).catch(() => undefined)
  }

  backupCode(): string {
    return store.backupCode(this.profile)
  }

  /** Replaces the profile with the one in [code]; false if the code is not valid. */
  restore(code: string): boolean {
    const profile = store.profileFromCode(code)
    if (!profile) return false
    this.updateProfile(() => profile)
    this.session = null
    this.completion = null
    return true
  }

  dismissToast(id: number): void {
    if (this.toast?.id === id) {
      this.toast = null
      this.emit('toast')
    }
  }

  private showToast(kind: Toast['kind'], amount = 0): void {
    this.toast = { kind, amount, id: ++toastIds }
    this.emit('toast')
  }

  private updateProfile(transform: (p: Profile) => Profile): void {
    this.profile = transform(this.profile)
    store.saveProfile(this.profile)
    this.emit('profile')
  }

  private listener: Listener = {
    onProgress: (session) => {
      if (!session.isSolved) store.saveProgress(session.level.key, session.snapshot())
    },
    onWordFound: (bonus) => {
      if (!bonus) {
        this.updateProfile((p) => ({ ...p, wordsFound: p.wordsFound + 1 }))
        return
      }
      const full = this.profile.jar + 1 >= JAR_SIZE
      this.updateProfile((p) => ({
        ...p,
        bonusFound: p.bonusFound + 1,
        jar: full ? 0 : p.jar + 1,
        coins: full ? p.coins + JAR_REWARD : p.coins,
      }))
      if (full) this.showToast('jar', JAR_REWARD)
    },
    trySpend: (coins) => {
      if (this.profile.coins < coins) {
        this.showToast('coins')
        return false
      }
      this.updateProfile((p) => ({ ...p, coins: p.coins - coins }))
      return true
    },
    onSolved: (session) => {
      const id: LevelId = session.level.id
      store.clearProgress(session.level.key)
      let reward: number
      if (id.kind === 'campaign') {
        reward = LEVEL_REWARD
        this.updateProfile((p) => ({
          ...p,
          coins: p.coins + reward,
          levels: { ...p.levels, [id.language]: Math.max(store.levelOf(p, id.language), id.number + 1) },
        }))
      } else {
        reward = this.profile.dailyDone[id.language] === id.epochDay ? 0 : DAILY_REWARD
        this.updateProfile((p) => ({
          ...p,
          coins: p.coins + reward,
          streak: p.lastDaily === id.epochDay ? p.streak : p.lastDaily === id.epochDay - 1 ? p.streak + 1 : 1,
          lastDaily: id.epochDay,
          dailyDone: { ...p.dailyDone, [id.language]: id.epochDay },
        }))
      }
      this.completion = {
        key: session.level.key,
        daily: id.kind === 'daily',
        reward,
        words: session.found.size,
        bonus: session.bonus.size,
      }
      this.emit('completion')
    },
  }
}

export const app = new App()
export type { Lang }
