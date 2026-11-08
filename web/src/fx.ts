// Sound effects (Web Audio, decoded once and kept in memory) and vibration
// where the browser has it (Android browsers do; iOS Safari has no vibration API).

type Sound = 'tap' | 'word' | 'bonus' | 'error' | 'shuffle' | 'complete'
const SOUNDS: Sound[] = ['tap', 'word', 'bonus', 'error', 'shuffle', 'complete']

export const canVibrate = typeof navigator.vibrate === 'function'

class Fx {
  sound = true
  haptics = true
  private context: AudioContext | null = null
  private buffers = new Map<Sound, AudioBuffer>()

  /**
   * Browsers only allow audio after a user gesture, so the context is created
   * on the first touch and the clips are decoded then.
   */
  unlock(): void {
    if (this.context) {
      if (this.context.state === 'suspended') void this.context.resume()
      return
    }
    const Context = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!Context) return
    const context = new Context()
    this.context = context
    for (const name of SOUNDS) {
      fetch(`./sounds/${name}.wav`)
        .then((r) => r.arrayBuffer())
        .then((data) => context.decodeAudioData(data))
        .then((buffer) => this.buffers.set(name, buffer))
        .catch(() => undefined)
    }
  }

  /** Each extra letter plays one whole tone higher, like a rising scale. */
  letter(index: number): void {
    this.play('tap', 0.7, Math.min(2, 2 ** ((index * 2) / 12)))
    this.vibrate(6)
  }

  word(): void {
    this.play('word', 0.8)
    this.vibrate(18)
  }

  bonus(): void {
    this.play('bonus', 0.7)
    this.vibrate(12)
  }

  error(): void {
    this.play('error', 0.7)
    this.vibrate([20, 40, 20])
  }

  shuffle(): void {
    this.play('shuffle', 0.6)
    this.vibrate(8)
  }

  complete(): void {
    this.play('complete', 0.8)
    this.vibrate(40)
  }

  tick(): void {
    this.vibrate(8)
  }

  private play(name: Sound, volume: number, rate = 1): void {
    const context = this.context
    const buffer = this.buffers.get(name)
    if (!this.sound || !context || !buffer) return
    const source = context.createBufferSource()
    source.buffer = buffer
    source.playbackRate.value = rate
    const gain = context.createGain()
    gain.gain.value = volume
    source.connect(gain).connect(context.destination)
    source.start()
  }

  private vibrate(pattern: number | number[]): void {
    if (this.haptics && canVibrate) navigator.vibrate(pattern)
  }
}

export const fx = new Fx()
