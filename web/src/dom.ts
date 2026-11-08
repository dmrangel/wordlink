import { Lucide, type IconName } from './icons'

type Child = Node | string | number | false | null | undefined | Child[]

type Props = {
  class?: string | false | null
  style?: Partial<Record<string, string | number>> | string
  dataset?: Record<string, string>
  [key: string]: unknown
}

/** Tiny element builder: h('button', { class: 'btn', onclick }, 'Play'). */
export function h<K extends keyof HTMLElementTagNameMap>(tag: K, props?: Props | null, ...children: Child[]): HTMLElementTagNameMap[K] {
  const el = document.createElement(tag)
  if (props) {
    for (const [key, value] of Object.entries(props)) {
      if (value === undefined || value === null || value === false) continue
      if (key === 'class') el.className = value as string
      else if (key === 'style') {
        if (typeof value === 'string') el.setAttribute('style', value)
        else for (const [prop, v] of Object.entries(value as Record<string, string | number>)) el.style.setProperty(prop, String(v))
      } else if (key === 'dataset') Object.assign(el.dataset, value)
      else if (key.startsWith('on') && typeof value === 'function') el.addEventListener(key.slice(2), value as EventListener)
      else if (value === true) el.setAttribute(key, '')
      else el.setAttribute(key, String(value))
    }
  }
  append(el, children)
  return el
}

export function append(parent: Node, children: Child[]): void {
  for (const child of children) {
    if (child === null || child === undefined || child === false) continue
    if (Array.isArray(child)) append(parent, child)
    else parent.appendChild(typeof child === 'object' ? child : document.createTextNode(String(child)))
  }
}

const SVG = 'http://www.w3.org/2000/svg'

/** A Lucide stroke icon; color follows `currentColor`. */
export function icon(name: IconName, size = 20, extraClass = ''): SVGSVGElement {
  const svg = document.createElementNS(SVG, 'svg')
  svg.setAttribute('viewBox', '0 0 24 24')
  svg.setAttribute('width', String(size))
  svg.setAttribute('height', String(size))
  svg.setAttribute('fill', 'none')
  svg.setAttribute('stroke', 'currentColor')
  svg.setAttribute('stroke-width', '2')
  svg.setAttribute('stroke-linecap', 'round')
  svg.setAttribute('stroke-linejoin', 'round')
  svg.setAttribute('aria-hidden', 'true')
  svg.setAttribute('class', `icon ${extraClass}`.trim())
  for (const d of Lucide[name]) {
    const path = document.createElementNS(SVG, 'path')
    path.setAttribute('d', d)
    svg.appendChild(path)
  }
  return svg
}

/** Restarts a CSS animation class on an element. */
export function replay(el: Element, cls: string): void {
  el.classList.remove(cls)
  void (el as HTMLElement).offsetWidth
  el.classList.add(cls)
}
