import { decodeBackup, encodeBackup, WebBackup } from 'wordlink-core'
import type { Lang } from './i18n'
import { DEFAULT_CUSTOMIZATION, type Customization, type ThemeMode } from './theme'

// Everything is small key/value data, like SharedPreferences on Android.
// Keys are versioned so a future format can migrate instead of misreading.

export const LANGUAGES: { code: Lang; nativeName: string }[] = [
  { code: 'en', nativeName: 'English' },
  { code: 'pt', nativeName: 'Português' },
]

export interface Settings {
  language: Lang
  theme: ThemeMode
  sound: boolean
  haptics: boolean
  custom: Customization
}

export interface Profile {
  coins: number
  /** Bonus words collected towards the next jar reward. */
  jar: number
  /** Next campaign level by language code. */
  levels: Record<string, number>
  wordsFound: number
  bonusFound: number
  streak: number
  /** Epoch day of the last solved daily, in any language. */
  lastDaily: number | null
  /** Epoch day of the last solved daily, by language code. */
  dailyDone: Record<string, number | null>
}

/** Words and cells already uncovered in a level still in progress. */
export interface SavedProgress {
  found: string[]
  bonus: string[]
  hinted: number[]
}

export const STARTING_COINS = 150
const PREFIX = 'wordlink.v1.'
const BACKGROUND_KEY = PREFIX + 'background'

/** False when storage is unavailable (Safari private mode, blocked site data). */
export let storageWorks = true

function read<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(PREFIX + key)
    return raw === null ? null : (JSON.parse(raw) as T)
  } catch {
    return null
  }
}

function write(key: string, value: unknown): void {
  try {
    localStorage.setItem(PREFIX + key, JSON.stringify(value))
  } catch {
    storageWorks = false
  }
}

function remove(key: string): void {
  try {
    localStorage.removeItem(PREFIX + key)
  } catch {
    // Nothing to remove when storage is unavailable.
  }
}

export function defaultLanguage(): Lang {
  return (navigator.languages ?? [navigator.language]).some((l) => l.toLowerCase().startsWith('pt')) ? 'pt' : 'en'
}

export function loadSettings(): Settings {
  const saved = read<Partial<Settings>>('settings') ?? {}
  return {
    language: saved.language === 'pt' || saved.language === 'en' ? saved.language : defaultLanguage(),
    theme: saved.theme ?? 'dark',
    sound: saved.sound ?? true,
    haptics: saved.haptics ?? true,
    custom: { ...DEFAULT_CUSTOMIZATION, ...saved.custom },
  }
}

export function saveSettings(settings: Settings): void {
  write('settings', settings)
}

export function emptyProfile(): Profile {
  return {
    coins: STARTING_COINS,
    jar: 0,
    levels: {},
    wordsFound: 0,
    bonusFound: 0,
    streak: 0,
    lastDaily: null,
    dailyDone: {},
  }
}

export function loadProfile(): Profile {
  return { ...emptyProfile(), ...read<Partial<Profile>>('profile') }
}

export function saveProfile(profile: Profile): void {
  write('profile', profile)
}

export function levelOf(profile: Profile, language: string): number {
  return profile.levels[language] ?? 1
}

export function loadProgress(key: string): SavedProgress {
  return read<SavedProgress>('progress.' + key) ?? { found: [], bonus: [], hinted: [] }
}

export function saveProgress(key: string, progress: SavedProgress): void {
  write('progress.' + key, progress)
}

export function clearProgress(key: string): void {
  remove('progress.' + key)
}

/** Drops unfinished daily puzzles from previous days. */
export function clearStaleDailies(todayKey: string): void {
  const prefix = PREFIX + 'progress.' + todayKey.slice(0, todayKey.indexOf(':') + 2)
  try {
    const stale = Object.keys(localStorage).filter((k) => k.startsWith(prefix) && k !== PREFIX + 'progress.' + todayKey)
    for (const key of stale) localStorage.removeItem(key)
  } catch {
    // Storage unavailable.
  }
}

/** The background photo as a JPEG data URL, kept apart from the small settings. */
export function loadBackground(): string | null {
  try {
    return localStorage.getItem(BACKGROUND_KEY)
  } catch {
    return null
  }
}

export function saveBackground(dataUrl: string | null): boolean {
  try {
    if (dataUrl === null) localStorage.removeItem(BACKGROUND_KEY)
    else localStorage.setItem(BACKGROUND_KEY, dataUrl)
    return true
  } catch {
    return false
  }
}

/** Days since 1970-01-01 for the local calendar date, like LocalDate.now().toEpochDay(). */
export function today(): number {
  const now = new Date()
  return Math.floor(Date.UTC(now.getFullYear(), now.getMonth(), now.getDate()) / 86_400_000)
}

export function dateOf(epochDay: number): Date {
  const utc = new Date(epochDay * 86_400_000)
  return new Date(utc.getUTCFullYear(), utc.getUTCMonth(), utc.getUTCDate())
}

/** Asks the browser not to evict this site's storage. Resolves to whether it is protected. */
export async function requestPersistence(): Promise<boolean> {
  try {
    if (!navigator.storage?.persist) return false
    if (await navigator.storage.persisted()) return true
    return await navigator.storage.persist()
  } catch {
    return false
  }
}

// Backup codes use the format shared with Android (core/.../backup/BackupCode.kt).

export function backupCode(profile: Profile): string {
  const levels = Object.entries(profile.levels)
  const dailies = Object.entries(profile.dailyDone).filter((e): e is [string, number] => e[1] !== null)
  return encodeBackup(
    new WebBackup(
      profile.coins,
      profile.jar,
      profile.wordsFound,
      profile.bonusFound,
      profile.streak,
      profile.lastDaily ?? -1,
      levels.map(([code]) => code),
      Int32Array.from(levels.map(([, level]) => level)),
      dailies.map(([code]) => code),
      Int32Array.from(dailies.map(([, day]) => day)),
    ),
  )
}

export function profileFromCode(code: string): Profile | null {
  const backup = decodeBackup(code)
  if (!backup) return null
  const levels: Record<string, number> = {}
  backup.levelLanguages.forEach((code, i) => (levels[code] = backup.levels[i]))
  const dailyDone: Record<string, number | null> = {}
  backup.dailyLanguages.forEach((code, i) => (dailyDone[code] = backup.dailyDays[i]))
  return {
    coins: backup.coins,
    jar: backup.jar,
    levels,
    wordsFound: backup.wordsFound,
    bonusFound: backup.bonusFound,
    streak: backup.streak,
    lastDaily: backup.lastDaily >= 0 ? backup.lastDaily : null,
    dailyDone,
  }
}
