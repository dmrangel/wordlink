// Modrinth design-system palettes and the player's customization, ported from
// app/.../ui/theme/Theme.kt and Customization.kt. Applied as CSS variables.

export type ThemeMode = 'dark' | 'oled' | 'light' | 'system' | 'custom'
export type CellShape = 'square' | 'rounded' | 'circle'

export const CELL_RADIUS: Record<CellShape, number> = { square: 0.1, rounded: 0.22, circle: 0.5 }

export interface Customization {
  /** Replaces the theme's brand color everywhere; null keeps the theme default. */
  accent: string | null
  /** Background color the Custom theme is derived from. */
  themeBase: string
  hasBackground: boolean
  blur: number
  dim: number
  panelOpacity: number
  cellShape: CellShape
  /** Letter wheel diameter relative to the space available for it. */
  wheelScale: number
  leftHanded: boolean
  reduceMotion: boolean
}

export const DEFAULT_CUSTOMIZATION: Customization = {
  accent: null,
  themeBase: '#12161F',
  hasBackground: false,
  blur: 0.35,
  dim: 0.35,
  panelOpacity: 0.78,
  cellShape: 'rounded',
  wheelScale: 1,
  leftHanded: false,
  reduceMotion: false,
}

export const ACCENT_PRESETS = ['#1BD96A', '#4F9CFF', '#C78AFF', '#FF6BCB', '#FF496E', '#FFA347', '#FACC15', '#2DD4BF']
export const THEME_BASE_PRESETS = ['#12161F', '#1A1322', '#0F1A14', '#1D1715', '#20232A', '#F2EEE6', '#E4ECF4']

/** sRGB color with components in 0..1. */
export class Color {
  constructor(readonly r: number, readonly g: number, readonly b: number, readonly a = 1) {}

  static hex(hex: string): Color {
    const v = hex.replace('#', '')
    const n = parseInt(v.length === 8 ? v.slice(2) : v, 16)
    const a = v.length === 8 ? parseInt(v.slice(0, 2), 16) / 255 : 1
    return new Color(((n >> 16) & 255) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255, a)
  }

  static hsl(h: number, s: number, l: number): Color {
    const c = (1 - Math.abs(2 * l - 1)) * s
    const x = c * (1 - Math.abs(((h / 60) % 2) - 1))
    const m = l - c / 2
    const [r, g, b] = h < 60 ? [c, x, 0] : h < 120 ? [x, c, 0] : h < 180 ? [0, c, x] : h < 240 ? [0, x, c] : h < 300 ? [x, 0, c] : [c, 0, x]
    return new Color(r + m, g + m, b + m)
  }

  alpha(a: number): Color {
    return new Color(this.r, this.g, this.b, a)
  }

  /** Relative luminance, as Compose's Color.luminance(). */
  luminance(): number {
    const lin = (c: number) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
    return 0.2126 * lin(this.r) + 0.7152 * lin(this.g) + 0.0722 * lin(this.b)
  }

  toHsl(): [number, number, number] {
    const max = Math.max(this.r, this.g, this.b)
    const min = Math.min(this.r, this.g, this.b)
    const l = (max + min) / 2
    const d = max - min
    if (d === 0) return [0, 0, l]
    const s = d / (1 - Math.abs(2 * l - 1))
    let h = max === this.r ? (((this.g - this.b) / d) % 6 + 6) % 6 : max === this.g ? (this.b - this.r) / d + 2 : (this.r - this.g) / d + 4
    h = (h * 60 + 360) % 360
    return [h, Math.min(1, Math.max(0, s)), l]
  }

  lerp(to: Color, f: number): Color {
    const mix = (a: number, b: number) => a + (b - a) * f
    return new Color(mix(this.r, to.r), mix(this.g, to.g), mix(this.b, to.b), mix(this.a, to.a))
  }

  css(): string {
    const c = (v: number) => Math.round(Math.min(1, Math.max(0, v)) * 255)
    return this.a >= 1 ? `rgb(${c(this.r)} ${c(this.g)} ${c(this.b)})` : `rgb(${c(this.r)} ${c(this.g)} ${c(this.b)} / ${+this.a.toFixed(3)})`
  }

  hex(): string {
    const c = (v: number) => Math.round(Math.min(1, Math.max(0, v)) * 255).toString(16).padStart(2, '0')
    return `#${c(this.r)}${c(this.g)}${c(this.b)}`.toUpperCase()
  }
}

export interface Palette {
  isDark: boolean
  surface1: Color
  surface2: Color
  surface3: Color
  surface4: Color
  surface5: Color
  contrast: Color
  base: Color
  secondary: Color
  brand: Color
  brandHighlight: Color
  red: Color
  orange: Color
  blue: Color
  purple: Color
  accentContrast: Color
  gradient: [Color, Color]
  press: Color
  frame: Color
  panel: Color
}

const hex = Color.hex

function palette(p: Omit<Palette, 'frame' | 'panel'>): Palette {
  return { ...p, frame: p.surface3, panel: p.surface1 }
}

export const DARK = palette({
  isDark: true,
  surface1: hex('#16181C'),
  surface2: hex('#1D1F23'),
  surface3: hex('#27292E'),
  surface4: hex('#34363C'),
  surface5: hex('#42444A'),
  contrast: hex('#FFFFFF'),
  base: hex('#B0BAC5'),
  secondary: hex('#96A2B0'),
  brand: hex('#1BD96A'),
  brandHighlight: hex('#401BD96A'),
  red: hex('#FF496E'),
  orange: hex('#FFA347'),
  blue: hex('#4F9CFF'),
  purple: hex('#C78AFF'),
  accentContrast: hex('#000000'),
  gradient: [hex('#131F17'), hex('#09110D')],
  press: hex('#1AFFFFFF'),
})

export const OLED = palette({
  ...DARK,
  surface1: hex('#000000'),
  surface2: hex('#09090A'),
  surface3: hex('#101013'),
  surface4: hex('#1B1B20'),
  surface5: hex('#25262B'),
  gradient: [hex('#80131F17'), hex('#9909120E')],
})

export const LIGHT = palette({
  isDark: false,
  surface1: hex('#EBEBEB'),
  surface2: hex('#F5F5F5'),
  surface3: hex('#F8F8F8'),
  surface4: hex('#FFFFFF'),
  surface5: hex('#DDDDDD'),
  contrast: hex('#1A202C'),
  base: hex('#2C2E31'),
  secondary: hex('#484D54'),
  brand: hex('#00AF5C'),
  brandHighlight: hex('#4000AF5C'),
  red: hex('#CB2245'),
  orange: hex('#E08325'),
  blue: hex('#1F68C0'),
  purple: hex('#8E32F3'),
  accentContrast: hex('#FFFFFF'),
  gradient: [hex('#1F24E15B'), hex('#2D44B68A')],
  press: hex('#14000000'),
})

/** Builds a full surface ramp from one color with Modrinth's lightness steps. */
export function customPalette(base: Color): Palette {
  const [h, s, l] = base.toHsl()
  const shade = (delta: number) => Color.hsl(h, s, Math.min(1, Math.max(0, l + delta)))
  if (base.luminance() < 0.35) {
    return {
      ...DARK,
      surface1: base,
      surface2: shade(0.027),
      surface3: shade(0.069),
      surface4: shade(0.122),
      surface5: shade(0.177),
      base: Color.hsl(h, Math.min(s, 0.2), 0.75),
      secondary: Color.hsl(h, Math.min(s, 0.16), 0.64),
      gradient: [shade(0.04).lerp(DARK.brand, 0.08), shade(-0.01)],
      frame: shade(0.069),
      panel: base,
    }
  }
  return {
    ...LIGHT,
    surface1: base,
    surface2: shade(0.04),
    surface3: shade(0.05),
    surface4: shade(0.08),
    surface5: shade(-0.055),
    contrast: Color.hsl(h, Math.min(s, 0.3), 0.13),
    base: Color.hsl(h, Math.min(s, 0.08), 0.18),
    secondary: Color.hsl(h, Math.min(s, 0.08), 0.3),
    gradient: [shade(0.04).lerp(LIGHT.brand, 0.12), shade(0.05)],
    frame: shade(0.05),
    panel: base,
  }
}

const systemDark = window.matchMedia('(prefers-color-scheme: dark)')

export function onSystemThemeChange(listener: () => void): void {
  systemDark.addEventListener('change', listener)
}

export function resolvePalette(mode: ThemeMode, custom: Customization): Palette {
  let p =
    mode === 'dark' ? DARK
    : mode === 'oled' ? OLED
    : mode === 'light' ? LIGHT
    : mode === 'system' ? (systemDark.matches ? DARK : LIGHT)
    : customPalette(hex(custom.themeBase))
  if (custom.accent) {
    const accent = hex(custom.accent)
    p = {
      ...p,
      brand: accent,
      brandHighlight: accent.alpha(0.25),
      accentContrast: accent.luminance() > 0.4 ? hex('#000000') : hex('#FFFFFF'),
      gradient: [p.surface2.lerp(accent, p.isDark ? 0.12 : 0.16), p.surface1.lerp(accent, 0.03)],
    }
  }
  if (custom.hasBackground) {
    p = { ...p, frame: p.surface3.alpha(custom.panelOpacity), panel: p.surface1.alpha(custom.panelOpacity) }
  }
  return p
}

export function applyPalette(p: Palette, custom: Customization): void {
  const vars: Record<string, string> = {
    '--surface-1': p.surface1.css(),
    '--surface-2': p.surface2.css(),
    '--surface-3': p.surface3.css(),
    '--surface-4': p.surface4.css(),
    '--surface-5': p.surface5.css(),
    '--contrast': p.contrast.css(),
    '--base': p.base.css(),
    '--secondary': p.secondary.css(),
    '--brand': p.brand.css(),
    '--brand-highlight': p.brandHighlight.css(),
    '--red': p.red.css(),
    '--orange': p.orange.css(),
    '--blue': p.blue.css(),
    '--purple': p.purple.css(),
    '--red-highlight': p.red.alpha(0.25).css(),
    '--orange-highlight': p.orange.alpha(0.25).css(),
    '--blue-highlight': p.blue.alpha(0.25).css(),
    '--purple-highlight': p.purple.alpha(0.25).css(),
    '--accent-contrast': p.accentContrast.css(),
    '--gradient-a': p.gradient[0].css(),
    '--gradient-b': p.gradient[1].css(),
    '--press': p.press.css(),
    '--frame': p.frame.css(),
    '--panel': p.panel.css(),
    '--cell-radius': String(CELL_RADIUS[custom.cellShape]),
    '--dim': String(custom.dim),
  }
  const root = document.documentElement
  for (const [key, value] of Object.entries(vars)) root.style.setProperty(key, value)
  root.style.colorScheme = p.isDark ? 'dark' : 'light'
  root.classList.toggle('reduce-motion', custom.reduceMotion)
  root.classList.toggle('has-background', custom.hasBackground)
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', p.surface3.alpha(1).hex())
}
