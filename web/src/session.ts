import type { Engine } from 'wordlink-core'
import { MIN_WORD_LENGTH, type Level } from './engine'
import type { SavedProgress } from './store'

// Port of app/.../game/GameSession.kt: the rules of one puzzle being played.

export type WordResult = 'found' | 'repeat' | 'bonus' | 'bonusRepeat' | 'invalid'

export interface Listener {
  onProgress(session: GameSession): void
  onWordFound(bonus: boolean): void
  trySpend(coins: number): boolean
  onSolved(session: GameSession): void
}

export class GameSession {
  found: Set<string>
  bonus: Set<string>
  hinted: Set<number>
  /** Wheel order as indices into level.letters; shuffling permutes it. */
  order: number[]
  targeting = false
  /** Delay slot of each newly revealed cell, so a word lights up letter by letter. */
  revealDelay = new Map<number, number>()

  constructor(
    readonly level: Level,
    private readonly engine: Engine,
    saved: SavedProgress,
    private readonly listener: Listener,
  ) {
    this.found = new Set(saved.found.filter((w) => level.cellsOf.has(w)))
    this.bonus = new Set(saved.bonus)
    this.hinted = new Set(saved.hinted.filter((c) => level.letterAt.has(c)))
    this.order = [...level.letters].map((_, i) => i)
    this.absorbUncoveredWords()
  }

  get solvedCells(): Set<number> {
    const cells = new Set<number>()
    for (const word of this.found) for (const c of this.level.cellsOf.get(word)!) cells.add(c)
    return cells
  }

  get isSolved(): boolean {
    return this.found.size === this.level.words.length
  }

  display(word: string): string {
    return this.engine.display(word)
  }

  submit(word: string): WordResult {
    if (word.length < MIN_WORD_LENGTH || this.isSolved) return 'invalid'
    let result: WordResult
    if (this.found.has(word)) result = 'repeat'
    else if (this.level.cellsOf.has(word)) {
      this.revealDelay = new Map(this.level.cellsOf.get(word)!.map((c, i) => [c, i]))
      this.found.add(word)
      this.listener.onWordFound(false)
      this.absorbUncoveredWords()
      result = 'found'
    } else if (this.bonus.has(word)) result = 'bonusRepeat'
    else if (this.engine.isValid(word)) {
      this.bonus.add(word)
      this.listener.onWordFound(true)
      result = 'bonus'
    } else result = 'invalid'
    if (result === 'found' || result === 'bonus') this.changed()
    return result
  }

  shuffle(): void {
    const current = this.order.join()
    if (this.order.length < 2) return
    let next = [...this.order]
    do {
      for (let i = next.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1))
        ;[next[i], next[j]] = [next[j], next[i]]
      }
    } while (next.join() === current)
    this.order = next
  }

  /** Reveals the first hidden letter of a random unfinished word. */
  hint(cost: number): boolean {
    const visible = this.visibleCells()
    const open = this.level.words.filter((w) => !this.found.has(w))
    if (open.length === 0) return false
    const word = open[Math.floor(Math.random() * open.length)]
    const target = this.level.cellsOf.get(word)!.find((c) => !visible.has(c))
    if (target === undefined || !this.listener.trySpend(cost)) return false
    this.reveal(target)
    return true
  }

  /** Reveals a cell the player picked while targeting. */
  target(cell: number, cost: number): boolean {
    this.targeting = false
    if (this.solvedCells.has(cell) || this.hinted.has(cell) || !this.level.letterAt.has(cell)) return false
    if (!this.listener.trySpend(cost)) return false
    this.reveal(cell)
    return true
  }

  snapshot(): SavedProgress {
    return { found: [...this.found], bonus: [...this.bonus], hinted: [...this.hinted] }
  }

  private visibleCells(): Set<number> {
    const visible = this.solvedCells
    for (const c of this.hinted) visible.add(c)
    return visible
  }

  private reveal(cell: number): void {
    this.revealDelay = new Map([[cell, 0]])
    this.hinted.add(cell)
    this.absorbUncoveredWords()
    this.changed()
  }

  /**
   * A word whose letters are all visible counts as found, whether they were
   * uncovered by hints or by the words crossing it.
   */
  private absorbUncoveredWords(): void {
    const visible = this.visibleCells()
    for (const word of this.level.words) {
      if (!this.found.has(word) && this.level.cellsOf.get(word)!.every((c) => visible.has(c))) this.found.add(word)
    }
  }

  private changed(): void {
    this.listener.onProgress(this)
    if (this.isSolved) this.listener.onSolved(this)
  }
}
