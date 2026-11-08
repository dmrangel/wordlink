import { app } from './app'
import { blurred } from './background'
import { loadBackground } from './store'

// The background photo layer behind every screen (#backdrop in index.html's body).

const layer = document.createElement('div')
layer.className = 'backdrop'
layer.setAttribute('aria-hidden', 'true')
document.body.prepend(layer)

let generation = 0
let pending: Promise<void> | null = null
let again = false

/** Re-renders the photo for the current blur; slider drags coalesce into one pass at a time. */
export function refreshBackground(): Promise<void> {
  if (pending) {
    again = true
    return pending
  }
  const run = async () => {
    do {
      again = false
      const mine = ++generation
      const photo = app.settings.custom.hasBackground ? loadBackground() : null
      const url = photo ? await blurred(photo, app.settings.custom.blur).catch(() => null) : null
      if (mine !== generation) continue
      layer.style.backgroundImage = url ? `url("${url}")` : ''
      layer.classList.toggle('visible', url !== null)
    } while (again)
  }
  // Cleared after the assignment: run() can finish synchronously when there is no photo.
  const current = run().finally(() => {
    if (pending === current) pending = null
  })
  pending = current
  return current
}
