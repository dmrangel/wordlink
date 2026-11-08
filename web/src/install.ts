// Install state. On iOS, Safari deletes the storage of websites left unopened
// for 7 days, but apps added to the home screen are exempt; so the app keeps
// nudging Safari users to install it.

interface InstallPromptEvent extends Event {
  prompt(): Promise<void>
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>
}

export const isIos =
  /iPad|iPhone|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)

export function isStandalone(): boolean {
  return window.matchMedia('(display-mode: standalone)').matches || (navigator as Navigator & { standalone?: boolean }).standalone === true
}

let deferredPrompt: InstallPromptEvent | null = null
const listeners = new Set<() => void>()

window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault()
  deferredPrompt = event as InstallPromptEvent
  listeners.forEach((l) => l())
})
window.addEventListener('appinstalled', () => {
  deferredPrompt = null
  listeners.forEach((l) => l())
})

export function onInstallChange(listener: () => void): void {
  listeners.add(listener)
}

/** Chrome/Edge/Android offer a native install prompt; Safari never does. */
export function canPromptInstall(): boolean {
  return deferredPrompt !== null
}

export async function promptInstall(): Promise<void> {
  const prompt = deferredPrompt
  if (!prompt) return
  deferredPrompt = null
  await prompt.prompt()
  await prompt.userChoice.catch(() => undefined)
  listeners.forEach((l) => l())
}

const DISMISS_KEY = 'wordlink.v1.installDismissed'
const REMIND_AFTER_DAYS = 3

/** Whether the home screen should show the install card. */
export function shouldSuggestInstall(): boolean {
  if (isStandalone() || !(isIos || canPromptInstall())) return false
  try {
    const dismissed = Number(localStorage.getItem(DISMISS_KEY) ?? 0)
    return Date.now() - dismissed > REMIND_AFTER_DAYS * 86_400_000
  } catch {
    return true
  }
}

export function dismissInstall(): void {
  try {
    localStorage.setItem(DISMISS_KEY, String(Date.now()))
  } catch {
    // Without storage the card simply comes back next time.
  }
}
