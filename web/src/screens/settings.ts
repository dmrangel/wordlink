import { app } from '../app'
import { importPhoto } from '../background'
import { button, card, chip, frame, logo, navButton, openModal, sectionTitle, settingRow, slider, swatch, toggle } from '../components'
import { h, icon } from '../dom'
import { canVibrate } from '../fx'
import { t, type Key } from '../i18n'
import type { IconName } from '../icons'
import { isStandalone } from '../install'
import { LANGUAGES, requestPersistence, saveBackground, storageWorks } from '../store'
import { ACCENT_PRESETS, DARK, DEFAULT_CUSTOMIZATION, LIGHT, THEME_BASE_PRESETS, resolvePalette, type CellShape, type ThemeMode } from '../theme'
import { installCard } from './home'
import { refreshBackground } from '../backdrop'

declare const __APP_VERSION__: string

export function settingsScreen(): HTMLElement {
  const settings = app.settings
  const custom = settings.custom

  const languageCard = card([
    h('div', { class: 'chips' }, LANGUAGES.map((l) => chip(l.nativeName, settings.language === l.code, () => app.update((s) => ({ ...s, language: l.code })), 'Languages'))),
    h('p', { class: 'small secondary gap-sm' }, t('interface_language_desc')),
  ])

  const themes: [ThemeMode, Key, IconName][] = [
    ['dark', 'theme_dark', 'Moon'],
    ['oled', 'theme_oled', 'Moon'],
    ['light', 'theme_light', 'Sun'],
    ['system', 'theme_system', 'Palette'],
    ['custom', 'theme_custom', 'Paintbrush'],
  ]
  const themeCard = card([
    h('div', { class: 'body contrast' }, t('theme')),
    h('div', { class: 'chips gap-md' }, themes.map(([mode, label, name]) => chip(t(label), settings.theme === mode, () => app.update((s) => ({ ...s, theme: mode })), name))),
  ])

  // Accent color: theme default, presets, and the native color picker.
  const themeBrand = resolvePalette(settings.theme, { ...custom, accent: null }).isDark ? DARK.brand.hex() : LIGHT.brand.hex()
  const accentIsCustom = custom.accent !== null && !ACCENT_PRESETS.includes(custom.accent)
  const accentCard = card([
    h('div', { class: 'body contrast' }, t('accent_color')),
    h(
      'div',
      { class: 'swatches gap-sm' },
      swatch(themeBrand, custom.accent === null, () => app.customize((c) => ({ ...c, accent: null })), t('accent_default'), 'RotateCcw'),
      ACCENT_PRESETS.slice(1).map((color) => swatch(color, custom.accent === color, () => app.customize((c) => ({ ...c, accent: color })), color)),
      colorPicker(accentIsCustom ? custom.accent! : null, custom.accent ?? themeBrand, t('custom_color'), (color, commit) =>
        app.customize((c) => ({ ...c, accent: color }), { persist: commit, rerender: commit }),
      ),
    ),
  ])

  // Background photo.
  const photoInput = h('input', { type: 'file', accept: 'image/*', class: 'visually-hidden' }) as HTMLInputElement
  const photoError = h('p', { class: 'small error', role: 'alert', hidden: true }, t('background_failed'))
  photoInput.addEventListener('change', async () => {
    const file = photoInput.files?.[0]
    if (!file) return
    photoLabel.textContent = t('loading')
    try {
      const dataUrl = await importPhoto(file)
      if (!saveBackground(dataUrl)) throw new Error('storage full')
      app.customize((c) => ({ ...c, hasBackground: true }))
      await refreshBackground()
    } catch (error) {
      console.error(error)
      photoError.hidden = false
      photoLabel.textContent = t(custom.hasBackground ? 'change_photo' : 'choose_photo')
    }
  })
  const photoLabel = h('span', null, t(custom.hasBackground ? 'change_photo' : 'choose_photo'))
  const photoSliders = (enabled: boolean) => [
    slider(t('blur'), custom.blur, (v) => {
      app.customize((c) => ({ ...c, blur: v }), { persist: false, rerender: false })
      void refreshBackground()
    }, () => app.customize((c) => c, { rerender: false }), { disabled: !enabled }),
    slider(t('dim'), custom.dim / 0.85, (v) => app.customize((c) => ({ ...c, dim: v * 0.85 }), { persist: false, rerender: false }), () => app.customize((c) => c, { rerender: false }), { disabled: !enabled }),
    slider(
      t('panel_opacity'),
      (custom.panelOpacity - 0.3) / 0.7,
      (v) => app.customize((c) => ({ ...c, panelOpacity: 0.3 + v * 0.7 }), { persist: false, rerender: false }),
      () => app.customize((c) => c, { rerender: false }),
      { disabled: !enabled, valueText: (v) => `${Math.round((0.3 + v * 0.7) * 100)}%` },
    ),
  ]
  const photoCard = card([
    h('div', { class: 'body contrast' }, t('background_photo')),
    h('p', { class: 'small secondary' }, t('background_photo_desc')),
    h(
      'div',
      { class: 'row gap-row-sm gap-md' },
      h('label', { class: 'btn btn-base btn-lg grow' }, icon('Image', 18), photoLabel, photoInput),
      custom.hasBackground
        ? button([icon('Trash', 18), h('span', null, t('remove'))], async () => {
            saveBackground(null)
            app.customize((c) => ({ ...c, hasBackground: false }))
            await refreshBackground()
          }, { type: 'outlined', color: 'var(--red)' })
        : null,
    ),
    photoError,
    h('div', { class: 'stack gap-md' }, photoSliders(custom.hasBackground)),
  ])

  // Custom theme base color.
  const baseIsCustom = !THEME_BASE_PRESETS.includes(custom.themeBase)
  const customActive = settings.theme === 'custom'
  const customThemeCard = card([
    h('div', { class: 'body contrast' }, t('custom_theme')),
    h('p', { class: 'small secondary' }, t('custom_theme_desc')),
    h(
      'div',
      { class: 'swatches gap-sm' },
      THEME_BASE_PRESETS.map((color) =>
        swatch(color, customActive && custom.themeBase === color, () => app.update((s) => ({ ...s, theme: 'custom', custom: { ...s.custom, themeBase: color } })), color),
      ),
      colorPicker(customActive && baseIsCustom ? custom.themeBase : null, custom.themeBase, t('base_color'), (color, commit) =>
        app.update((s) => ({ ...s, theme: 'custom', custom: { ...s.custom, themeBase: color } }), { persist: commit, rerender: commit }),
      ),
    ),
  ])

  const shapes: [CellShape, Key, IconName][] = [
    ['square', 'shape_square', 'Square'],
    ['rounded', 'shape_rounded', 'Squircle'],
    ['circle', 'shape_circle', 'Circle'],
  ]
  const boardCard = card([
    h('div', { class: 'caption secondary' }, t('board')),
    h('div', { class: 'body contrast gap-sm' }, t('tile_shape')),
    h('div', { class: 'chips gap-sm' }, shapes.map(([shape, label, name]) => chip(t(label), custom.cellShape === shape, () => app.customize((c) => ({ ...c, cellShape: shape })), name))),
    h('hr', { class: 'divider' }),
    slider(t('wheel_size'), (custom.wheelScale - 0.7) / 0.3, (v) => app.customize((c) => ({ ...c, wheelScale: 0.7 + v * 0.3 }), { persist: false, rerender: false }), () => app.customize((c) => c, { rerender: false }), {
      valueText: (v) => `${Math.round((0.7 + v * 0.3) * 100)}%`,
    }),
    h('hr', { class: 'divider' }),
    settingRow(t('left_handed'), toggle(custom.leftHanded, (on) => app.customize((c) => ({ ...c, leftHanded: on })), t('left_handed')), t('left_handed_desc')),
    h('hr', { class: 'divider' }),
    settingRow(t('reduce_motion'), toggle(custom.reduceMotion, (on) => app.customize((c) => ({ ...c, reduceMotion: on })), t('reduce_motion')), t('reduce_motion_desc')),
  ])

  const reset = button([icon('RotateCcw', 20), h('span', null, t('reset_customization'))], async () => {
    saveBackground(null)
    app.customize(() => ({ ...DEFAULT_CUSTOMIZATION }))
    await refreshBackground()
  }, { size: 'xl', extraClass: 'full' })

  const gameplayCard = card([
    settingRow(t('sound'), toggle(settings.sound, (on) => app.update((s) => ({ ...s, sound: on }), { rerender: false }), t('sound'))),
    canVibrate ? h('hr', { class: 'divider' }) : null,
    canVibrate ? settingRow(t('haptics'), toggle(settings.haptics, (on) => app.update((s) => ({ ...s, haptics: on }), { rerender: false }), t('haptics'))) : null,
  ])

  return frame(
    [navButton('ArrowLeft', t('back'), () => app.navigate('home')), h('h1', { class: 'title grow' }, t('settings'))],
    [
      h(
        'div',
        { class: 'scroll' },
        h(
          'div',
          { class: 'column' },
          sectionTitle(t('language')),
          languageCard,
          sectionTitle(t('appearance')),
          themeCard,
          sectionTitle(t('customization')),
          h('div', { class: 'stack gap-sm' }, h('div', { class: 'caption secondary indent' }, t('preview')), themePreview()),
          accentCard,
          photoCard,
          customThemeCard,
          boardCard,
          reset,
          sectionTitle(t('gameplay')),
          gameplayCard,
          sectionTitle(t('safety')),
          safetyCard(),
          backupCard(),
          sectionTitle(t('about')),
          card([
            h('div', { class: 'row center gap-row' }, logo(44), h('div', null, h('div', { class: 'heading' }, 'Wordlink'), h('div', { class: 'small secondary' }, t('version', __APP_VERSION__)))),
            h('p', { class: 'body gap-md' }, t('about_text')),
            h('p', { class: 'small secondary gap-sm' }, t('credits')),
          ]),
        ),
      ),
    ],
  )
}

/**
 * A swatch that opens the platform color picker (on iOS, the system sheet).
 * The input covers the swatch so the tap reaches it directly, which Safari
 * requires for opening the picker.
 */
function colorPicker(selected: string | null, initial: string, label: string, onPick: (color: string, commit: boolean) => void): HTMLElement {
  const input = h('input', { type: 'color', 'aria-label': label }) as HTMLInputElement
  input.value = initial.toLowerCase().slice(0, 7)
  input.addEventListener('input', () => onPick(input.value.toUpperCase(), false))
  input.addEventListener('change', () => onPick(input.value.toUpperCase(), true))
  const look = swatch(selected ?? '#000000', selected !== null, () => undefined, label, 'Plus', selected === null)
  look.tabIndex = -1
  look.setAttribute('aria-hidden', 'true')
  return h('span', { class: 'color-picker' }, look, input)
}

/** A miniature board in the current theme, accent and tile shape. */
function themePreview(): HTMLElement {
  const tiles = ['W', 'O', 'R', 'D', '']
  return h(
    'div',
    { class: 'theme-preview' },
    h('div', { class: 'preview-bar' }, logo(18), h('span', { class: 'preview-title' }, 'Wordlink'), h('span', { class: 'coins small' }, icon('Coins', 14), String(app.profile.coins))),
    h(
      'div',
      { class: 'preview-body' },
      h('div', { class: 'preview-tiles' }, tiles.map((letter, i) => h('span', { class: `cell ${i < 3 ? 'solved' : i === 3 ? 'hinted' : 'hidden'}` }, h('span', null, i < 4 ? letter : '')))),
      h('div', { class: 'preview-wheel' }, ['L', 'I', 'N', 'K'].map((l, i) => h('span', { class: i === 0 ? 'on' : '', style: { '--i': i } }, l))),
    ),
  )
}

function safetyCard(): HTMLElement {
  const status = (ok: boolean, text: string) =>
    h('div', { class: `row gap-row-sm status ${ok ? 'ok' : 'warn'}` }, icon(ok ? 'Check' : 'X', 18), h('span', { class: 'small' }, text))
  const installed = isStandalone()
  const storage = h('div', null, storageWorks ? null : status(false, t('storage_failed')))
  if (storageWorks) {
    void requestPersistence().then((persistent) => storage.replaceChildren(status(persistent, t(persistent ? 'storage_persistent' : 'storage_best_effort'))))
  }
  return card([
    h('div', { class: 'stack gap-sm' }, status(installed, t(installed ? 'installed' : 'not_installed')), storage),
    installed ? null : h('div', { class: 'gap-md' }, installCard(false)),
  ], 'safety')
}

function backupCard(): HTMLElement {
  const note = h('div', { class: 'row gap-row-sm status ok gap-md', hidden: true }, icon('Check', 18), h('span', { class: 'label' }, t('restored')))
  const copyLabel = h('span', null, t('copy_code'))
  const copy = button([icon('Copy', 18), copyLabel], async () => {
    const code = app.backupCode()
    try {
      await navigator.clipboard.writeText(code)
      copyLabel.textContent = t('code_copied')
    } catch {
      // No clipboard permission: show the code so it can be copied by hand.
      openModal(h('div', { class: 'modal-content' }, h('textarea', { class: 'code-field', readonly: true, rows: 4 }, code)), () => undefined)
    }
  }, { extraClass: 'grow' })
  const share =
    typeof navigator.share === 'function'
      ? button([icon('Share', 18), h('span', null, t('share'))], () => void navigator.share({ title: 'Wordlink', text: app.backupCode() }).catch(() => undefined), { extraClass: 'grow' })
      : null
  const restore = button([icon('ClipboardPaste', 18), h('span', null, t('restore'))], () => openRestore(() => (note.hidden = false)), { extraClass: 'grow' })
  return card([
    h('div', { class: 'body contrast' }, t('backup')),
    h('p', { class: 'small secondary' }, t('backup_desc')),
    h('div', { class: 'row wrap gap-row-sm gap-md' }, copy, share, restore),
    note,
  ])
}

function openRestore(onRestored: () => void): void {
  const field = h('textarea', { class: 'code-field', rows: 4, autocapitalize: 'off', autocomplete: 'off', spellcheck: 'false', 'aria-label': t('restore_title') }) as HTMLTextAreaElement
  const error = h('p', { class: 'small error', role: 'alert', hidden: true }, t('invalid_code'))
  const submit = button([h('span', null, t('restore'))], () => {
    if (app.restore(field.value)) {
      close()
      onRestored()
    } else {
      error.hidden = false
      field.classList.add('invalid')
    }
  }, { type: 'colored', size: 'xl', extraClass: 'grow', disabled: true })
  field.addEventListener('input', () => {
    submit.disabled = field.value.trim() === ''
    error.hidden = true
    field.classList.remove('invalid')
  })
  const paste =
    typeof navigator.clipboard?.readText === 'function'
      ? button([icon('ClipboardPaste', 18), h('span', null, t('paste'))], async () => {
          try {
            field.value = (await navigator.clipboard.readText()).trim()
            field.dispatchEvent(new Event('input'))
          } catch {
            field.focus()
          }
        }, { type: 'outlined', extraClass: 'full' })
      : null
  const content = h(
    'div',
    { class: 'modal-content' },
    h('div', { class: 'row center gap-row' }, h('span', { class: 'brand' }, icon('ClipboardPaste', 22)), h('h2', { class: 'title grow' }, t('restore_title'))),
    h('p', { class: 'small secondary' }, t('restore_desc')),
    field,
    error,
    paste,
    h('div', { class: 'row gap-row-sm' }, button([h('span', null, t('cancel'))], () => close(), { size: 'xl', extraClass: 'grow' }), submit),
  )
  const close = openModal(content, () => undefined)
}
