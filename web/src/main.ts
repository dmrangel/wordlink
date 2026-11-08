import './styles.css'
import { app, type Change } from './app'
import { refreshBackground } from './backdrop'
import { fx } from './fx'
import { onInstallChange } from './install'
import { gameScreen } from './screens/game'
import { homeScreen } from './screens/home'
import { settingsScreen } from './screens/settings'
import { requestPersistence } from './store'
import { onSystemThemeChange } from './theme'

const root = document.getElementById('app')!
let dispose: (() => void) | null = null
let shown: string | null = null

function render(): void {
  // Keep the scroll position when a settings change rebuilds the same screen.
  const key = app.screen === 'game' ? `game:${app.session?.level.key}` : app.screen
  const scroll = key === shown ? (root.querySelector('.scroll')?.scrollTop ?? 0) : 0
  dispose?.()
  dispose = null
  root.querySelectorAll('.modal-backdrop').forEach((m) => m.remove())

  let el: HTMLElement
  if (app.screen === 'game' && app.session) {
    const game = gameScreen(app.session)
    el = game.el
    dispose = game.dispose
  } else if (app.screen === 'settings') el = settingsScreen()
  else el = homeScreen()
  root.replaceChildren(el)
  const scroller = root.querySelector('.scroll')
  if (scroller) scroller.scrollTop = scroll
  shown = key
  syncHistory()
}

// The browser/Android back button returns to the home screen, like on Android.
function syncHistory(): void {
  const inner = app.screen !== 'home'
  if (inner && history.state?.screen !== 'inner') history.pushState({ screen: 'inner' }, '')
  else if (!inner && history.state?.screen === 'inner') history.back()
}

window.addEventListener('popstate', () => {
  if (app.screen !== 'home') app.navigate('home')
})

app.subscribe((change: Change) => {
  if (change === 'screen') render()
  else if (change === 'profile' && app.screen === 'home') render()
})

app.applyTheme()
onSystemThemeChange(() => app.applyTheme())
onInstallChange(() => {
  if (app.screen !== 'game') render()
})
render()
void refreshBackground()

// Audio may only start after a gesture; the first touch anywhere unlocks it.
window.addEventListener('pointerdown', () => fx.unlock(), { capture: true })

// Ask for storage that is not evicted under pressure, and preload the
// dictionary so the first level opens quickly.
void requestPersistence()
app.warmUp()

// A new day can start while the app sits in the background.
document.addEventListener('visibilitychange', () => {
  if (document.visibilityState === 'visible' && app.screen === 'home') render()
})

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch((error) => console.warn('service worker', error))
  })
}
