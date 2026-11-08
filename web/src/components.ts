import { app } from './app'
import { h, icon } from './dom'
import { t } from './i18n'
import type { IconName } from './icons'
import { Color } from './theme'

// Modrinth-styled building blocks, mirroring app/.../ui/components/.

type ButtonType = 'base' | 'colored' | 'quiet' | 'outlined'

export function button(
  content: (Node | string)[],
  onclick: (e: MouseEvent) => void,
  { type = 'base' as ButtonType, size = 'lg', extraClass = '', disabled = false, label = '', color = '' } = {},
): HTMLButtonElement {
  return h(
    'button',
    {
      class: `btn btn-${type} btn-${size} ${extraClass}`,
      type: 'button',
      disabled,
      'aria-label': label || undefined,
      style: color ? { '--btn-color': color } : undefined,
      onclick,
    },
    content,
  )
}

export function navButton(name: IconName, label: string, onclick: () => void): HTMLButtonElement {
  return button([icon(name, 22)], onclick, { type: 'quiet', size: 'nav', label, extraClass: 'btn-circle' })
}

export function card(children: (Node | string | false | null)[], extraClass = '', onclick?: () => void): HTMLElement {
  if (onclick) return h('button', { class: `card card-button ${extraClass}`, type: 'button', onclick }, children)
  return h('div', { class: `card ${extraClass}` }, children)
}

export function chip(text: string, selected: boolean, onclick: () => void, iconName?: IconName): HTMLButtonElement {
  return h(
    'button',
    { class: `chip${selected ? ' selected' : ''}`, type: 'button', 'aria-pressed': String(selected), onclick },
    iconName ? icon(iconName, 16) : null,
    text,
  )
}

export function badge(text: string, iconName?: IconName, tone = ''): HTMLElement {
  return h('span', { class: `badge ${tone}` }, iconName ? icon(iconName, 16) : null, text)
}

export function iconTile(name: IconName, tone: string, size = 44): HTMLElement {
  return h('span', { class: `icon-tile ${tone}`, style: { '--size': `${size}px` } }, icon(name, Math.round(size * 0.5)))
}

export function sectionTitle(text: string): HTMLElement {
  return h('h2', { class: 'section-title' }, text)
}

export function progressBar(value: number, tone = ''): HTMLElement {
  return h('div', { class: `progress ${tone}` }, h('div', { class: 'progress-fill', style: { width: `${Math.round(value * 100)}%` } }))
}

export function toggle(checked: boolean, onchange: (on: boolean) => void, label: string): HTMLElement {
  const input = h('input', { type: 'checkbox', role: 'switch', 'aria-label': label, class: 'toggle' }) as HTMLInputElement
  input.checked = checked
  input.addEventListener('change', () => onchange(input.checked))
  return input
}

export function settingRow(title: string, trailing: Node, description?: string): HTMLElement {
  return h(
    'label',
    { class: 'setting-row' },
    h('span', { class: 'setting-text' }, h('span', { class: 'setting-title' }, title), description ? h('span', { class: 'small secondary' }, description) : null),
    trailing,
  )
}

/** Range input from 0 to 1; onInput fires while dragging, onCommit when released. */
export function slider(
  label: string,
  value: number,
  onInput: (v: number) => void,
  onCommit: () => void,
  { disabled = false, valueText }: { disabled?: boolean; valueText?: (v: number) => string } = {},
): HTMLElement {
  const text = (v: number) => (valueText ? valueText(v) : `${Math.round(v * 100)}%`)
  const output = h('span', { class: 'slider-value' }, text(value))
  const input = h('input', { type: 'range', min: '0', max: '1', step: '0.01', class: 'slider', disabled, 'aria-label': label }) as HTMLInputElement
  input.value = String(value)
  const paint = () => input.style.setProperty('--fill', `${Number(input.value) * 100}%`)
  paint()
  input.addEventListener('input', () => {
    paint()
    output.textContent = text(Number(input.value))
    onInput(Number(input.value))
  })
  input.addEventListener('change', onCommit)
  return h('div', { class: `slider-row${disabled ? ' disabled' : ''}` }, h('div', { class: 'slider-label' }, h('span', null, label), output), input)
}

export function coinsBadge(): HTMLElement {
  return h('span', { class: 'coins', 'aria-label': t('coins') }, icon('Coins', 18), h('span', { class: 'coins-value' }, String(app.profile.coins)))
}

/** Brand tile with a "W" traced through five linked letters, like the launcher icon. */
export function logo(size = 36): SVGSVGElement {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  svg.setAttribute('viewBox', '0 0 100 100')
  svg.setAttribute('width', String(size))
  svg.setAttribute('height', String(size))
  svg.setAttribute('class', 'logo')
  svg.setAttribute('aria-hidden', 'true')
  svg.innerHTML =
    '<rect width="100" height="100" rx="28" fill="var(--brand)"/>' +
    '<g fill="var(--accent-contrast)" stroke="var(--accent-contrast)" stroke-width="7.5" stroke-linecap="round" stroke-linejoin="round">' +
    '<polyline fill="none" points="22,32 36,70 50,42 64,70 78,32"/>' +
    '<circle cx="22" cy="32" r="3.75"/><circle cx="36" cy="70" r="3.75"/><circle cx="50" cy="42" r="3.75"/>' +
    '<circle cx="64" cy="70" r="3.75"/><circle cx="78" cy="32" r="3.75"/></g>'
  return svg
}

/** The app frame: top bar on the frame color, content in a rounded panel. */
export function frame(topBar: (Node | null)[], content: Node[]): HTMLElement {
  return h('div', { class: 'frame' }, h('header', { class: 'top-bar' }, topBar), h('main', { class: 'panel' }, content))
}

/** Dimmed backdrop with a centered card, like Modrinth's NewModal. Returns a close function. */
export function openModal(content: HTMLElement, onDismiss: (() => void) | null): () => void {
  const backdrop = h('div', { class: 'modal-backdrop' }, h('div', { class: 'modal-card', role: 'dialog', 'aria-modal': 'true' }, content))
  let closed = false
  const close = () => {
    if (closed) return
    closed = true
    document.removeEventListener('keydown', onKey)
    backdrop.classList.add('closing')
    setTimeout(() => backdrop.remove(), 160)
  }
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape' && onDismiss) {
      close()
      onDismiss()
    }
  }
  if (onDismiss) {
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) {
        close()
        onDismiss()
      }
    })
    document.addEventListener('keydown', onKey)
  }
  document.getElementById('app')!.appendChild(backdrop)
  return close
}

export function swatch(color: string, selected: boolean, onclick: () => void, label: string, iconName?: IconName, rainbow = false): HTMLButtonElement {
  return h(
    'button',
    {
      class: `swatch${selected ? ' selected' : ''}${rainbow ? ' rainbow' : ''}`,
      type: 'button',
      style: rainbow ? undefined : { '--swatch': color, '--swatch-ink': Color.hex(color).luminance() > 0.4 ? '#000' : '#fff' },
      'aria-label': label,
      'aria-pressed': String(selected),
      onclick,
    },
    selected && !iconName ? icon('Check', 18) : iconName ? icon(iconName, 18) : null,
  )
}
