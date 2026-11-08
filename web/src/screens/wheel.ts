// The circle of letters, ported from app/.../ui/game/LetterWheel.kt. Drag
// across letters to spell a word; dragging back onto the previous letter
// undoes the last one. Everything is drawn on one canvas, redrawn only while
// something moves.

const LETTER_SIZE = 0.2
const TAU = Math.PI * 2

export interface WheelCallbacks {
  onChange(word: string): void
  onLetter(count: number): void
  onSubmit(word: string): void
}

const slotAngle = (slot: number, count: number) => -Math.PI / 2 + (slot * TAU) / count

export class LetterWheel {
  readonly canvas = document.createElement('canvas')
  private readonly ctx = this.canvas.getContext('2d')!
  private size = 0
  private angles: number[]
  private targets: number[]
  private velocities: number[]
  private selected: number[] = []
  private pointer: { x: number; y: number } | null = null
  private pointerId: number | null = null
  private enabled = true
  private frame = 0
  private lastTime = 0

  constructor(
    private readonly letters: string,
    order: number[],
    private readonly callbacks: WheelCallbacks,
    private reduceMotion: boolean,
  ) {
    const count = letters.length
    this.angles = [...letters].map((_, i) => slotAngle(order.indexOf(i), count))
    this.targets = [...this.angles]
    this.velocities = this.angles.map(() => 0)
    this.canvas.className = 'wheel'
    this.canvas.setAttribute('role', 'img')
    this.canvas.setAttribute('aria-label', letters.toUpperCase().split('').join(' '))
    this.canvas.addEventListener('pointerdown', this.down)
    this.canvas.addEventListener('pointermove', this.move)
    this.canvas.addEventListener('pointerup', this.up)
    this.canvas.addEventListener('pointercancel', this.up)
    this.canvas.addEventListener('lostpointercapture', this.up)
  }

  resize(diameter: number): void {
    const dpr = Math.min(window.devicePixelRatio || 1, 3)
    this.size = diameter
    this.canvas.style.width = `${diameter}px`
    this.canvas.style.height = `${diameter}px`
    this.canvas.width = Math.round(diameter * dpr)
    this.canvas.height = Math.round(diameter * dpr)
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
    this.draw()
  }

  /** Shuffling glides every letter along the ring to its new slot. */
  setOrder(order: number[]): void {
    const count = this.letters.length
    for (let i = 0; i < count; i++) {
      let target = slotAngle(order.indexOf(i), count)
      while (target - this.angles[i] > Math.PI) target -= TAU
      while (this.angles[i] - target > Math.PI) target += TAU
      this.targets[i] = target
      if (this.reduceMotion) this.angles[i] = target
    }
    this.animate()
  }

  setEnabled(enabled: boolean): void {
    this.enabled = enabled
    if (!enabled && (this.selected.length || this.pointer)) {
      this.selected = []
      this.pointer = null
      this.pointerId = null
      this.draw()
    }
  }

  private word(): string {
    return this.selected.map((i) => this.letters[i]).join('')
  }

  private center(angle: number): { x: number; y: number } {
    const radius = this.size / 2
    const ring = radius * 0.62
    return { x: radius + ring * Math.cos(angle), y: radius + ring * Math.sin(angle) }
  }

  private local(e: PointerEvent): { x: number; y: number } {
    const rect = this.canvas.getBoundingClientRect()
    return { x: e.clientX - rect.left, y: e.clientY - rect.top }
  }

  private hit(p: { x: number; y: number }): number | null {
    const letterRadius = (this.size / 2) * LETTER_SIZE
    for (let i = 0; i < this.letters.length; i++) {
      const c = this.center(this.angles[i])
      if (Math.hypot(p.x - c.x, p.y - c.y) < letterRadius * 1.08) return i
    }
    return null
  }

  private down = (e: PointerEvent) => {
    if (!this.enabled || this.pointerId !== null) return
    const p = this.local(e)
    const first = this.hit(p)
    if (first === null) return
    e.preventDefault()
    this.canvas.setPointerCapture(e.pointerId)
    this.pointerId = e.pointerId
    this.selected = [first]
    this.pointer = p
    this.callbacks.onLetter(1)
    this.callbacks.onChange(this.word())
    this.draw()
  }

  private move = (e: PointerEvent) => {
    if (e.pointerId !== this.pointerId) return
    e.preventDefault()
    const p = this.local(e)
    this.pointer = p
    const index = this.hit(p)
    if (index !== null) {
      if (!this.selected.includes(index)) {
        this.selected.push(index)
        this.callbacks.onLetter(this.selected.length)
        this.callbacks.onChange(this.word())
      } else if (this.selected.length >= 2 && this.selected[this.selected.length - 2] === index) {
        this.selected.pop()
        this.callbacks.onChange(this.word())
      }
    }
    this.draw()
  }

  private up = (e: PointerEvent) => {
    if (e.pointerId !== this.pointerId) return
    const result = this.word()
    this.pointerId = null
    this.selected = []
    this.pointer = null
    this.draw()
    this.callbacks.onSubmit(result)
  }

  /** A critically damped spring per letter, like the Compose spring(0.8, 220). */
  private animate(): void {
    if (this.frame) return
    this.lastTime = performance.now()
    const step = (now: number) => {
      const dt = Math.min(0.032, (now - this.lastTime) / 1000)
      this.lastTime = now
      let moving = false
      for (let i = 0; i < this.angles.length; i++) {
        const stiffness = 220
        const damping = 2 * 0.8 * Math.sqrt(stiffness)
        const force = stiffness * (this.targets[i] - this.angles[i]) - damping * this.velocities[i]
        this.velocities[i] += force * dt
        this.angles[i] += this.velocities[i] * dt
        if (Math.abs(this.targets[i] - this.angles[i]) > 0.001 || Math.abs(this.velocities[i]) > 0.01) moving = true
        else {
          this.angles[i] = this.targets[i]
          this.velocities[i] = 0
        }
      }
      this.draw()
      this.frame = moving ? requestAnimationFrame(step) : 0
    }
    this.frame = requestAnimationFrame(step)
  }

  destroy(): void {
    if (this.frame) cancelAnimationFrame(this.frame)
  }

  draw(): void {
    if (!this.size) return
    const ctx = this.ctx
    const style = getComputedStyle(document.documentElement)
    const color = (name: string) => style.getPropertyValue(name).trim()
    const radius = this.size / 2
    const letterRadius = radius * LETTER_SIZE
    ctx.clearRect(0, 0, this.size, this.size)

    ctx.beginPath()
    ctx.arc(radius, radius, radius - 1, 0, TAU)
    ctx.fillStyle = color('--surface-3')
    ctx.fill()
    ctx.lineWidth = 1.5
    ctx.strokeStyle = color('--surface-5')
    ctx.stroke()

    const brand = color('--brand')
    if (this.selected.length) {
      ctx.beginPath()
      this.selected.forEach((index, i) => {
        const p = this.center(this.angles[index])
        if (i === 0) ctx.moveTo(p.x, p.y)
        else ctx.lineTo(p.x, p.y)
      })
      if (this.pointer) ctx.lineTo(this.pointer.x, this.pointer.y)
      ctx.strokeStyle = brand
      ctx.lineWidth = letterRadius * 0.34
      ctx.lineCap = 'round'
      ctx.lineJoin = 'round'
      ctx.stroke()
    }

    ctx.font = `800 ${letterRadius * 1.15}px Inter, system-ui, sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const contrast = color('--contrast')
    const accentContrast = color('--accent-contrast')
    for (let i = 0; i < this.letters.length; i++) {
      const p = this.center(this.angles[i])
      const isSelected = this.selected.includes(i)
      if (isSelected) {
        ctx.beginPath()
        ctx.arc(p.x, p.y, letterRadius, 0, TAU)
        ctx.fillStyle = brand
        ctx.fill()
      }
      ctx.fillStyle = isSelected ? accentContrast : contrast
      // Cap height sits a little above the middle baseline; nudge it to look centered.
      ctx.fillText(this.letters[i].toUpperCase(), p.x, p.y + letterRadius * 0.06)
    }
  }
}
