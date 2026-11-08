import { Engine, type WebLevel } from 'wordlink-core'
import type { Lang } from './i18n'

/** Grid positions packed into one number, as Cell does on Android. */
export const STRIDE = 64
export const cell = (row: number, col: number) => row * STRIDE + col
export const rowOf = (packed: number) => Math.floor(packed / STRIDE)
export const colOf = (packed: number) => packed % STRIDE

export const MIN_WORD_LENGTH = 3

export type LevelId =
  | { kind: 'campaign'; language: Lang; number: number }
  | { kind: 'daily'; language: Lang; epochDay: number }

export interface Level {
  id: LevelId
  /** Storage key, same as Android: "en:12" or "en:d2026-11-08". */
  key: string
  /** Wheel letters in their initial order. */
  letters: string
  rows: number
  cols: number
  words: string[]
  cellsOf: Map<string, number[]>
  letterAt: Map<number, string>
}

const engines = new Map<Lang, Promise<Engine>>()

/** Downloads and parses a dictionary once; the service worker keeps it offline. */
export function engine(language: Lang): Promise<Engine> {
  let pending = engines.get(language)
  if (!pending) {
    pending = fetch(`./dict/${language}.txt`)
      .then((response) => {
        if (!response.ok) throw new Error(`dictionary ${language}: HTTP ${response.status}`)
        return response.text()
      })
      .then((text) => new Engine(language, text))
    pending.catch(() => engines.delete(language))
    engines.set(language, pending)
  }
  return pending
}

function toLevel(id: LevelId, web: WebLevel): Level {
  const cellsOf = new Map<string, number[]>()
  const letterAt = new Map<number, string>()
  for (const p of web.placements) {
    const cells = Array.from(p.word, (_, i) => (p.across ? cell(p.row, p.col + i) : cell(p.row + i, p.col)))
    cellsOf.set(p.word, cells)
    cells.forEach((c, i) => letterAt.set(c, p.word[i]))
  }
  return { id, key: web.key, letters: web.letters, rows: web.rows, cols: web.cols, words: [...cellsOf.keys()], cellsOf, letterAt }
}

export async function campaignLevel(language: Lang, number: number): Promise<Level> {
  return toLevel({ kind: 'campaign', language, number }, (await engine(language)).campaign(number))
}

export async function dailyLevel(language: Lang, epochDay: number): Promise<Level> {
  return toLevel({ kind: 'daily', language, epochDay }, (await engine(language)).daily(epochDay))
}
