import { app, HINT_COST, JAR_REWARD, JAR_SIZE, TARGET_COST, type Change, type Completion } from '../app'
import { badge, button, coinsBadge, frame, iconTile, navButton, openModal, progressBar } from '../components'
import { h, icon, replay } from '../dom'
import { colOf, rowOf } from '../engine'
import { fx } from '../fx'
import { lang, plural, t } from '../i18n'
import type { IconName } from '../icons'
import type { GameSession, WordResult } from '../session'
import { dateOf, LANGUAGES } from '../store'
import { LetterWheel } from './wheel'

const GAP_RATIO = 0.12
const MAX_CELL = 58
const BUTTON = 52
const PREVIEW = 56

/** Builds the game screen; returns the element and a cleanup function. */
export function gameScreen(session: GameSession): { el: HTMLElement; dispose: () => void } {
  const level = session.level
  const custom = app.settings.custom
  const id = level.id
  const title =
    id.kind === 'campaign'
      ? t('level_n', id.number)
      : t('daily_title', new Intl.DateTimeFormat(lang() === 'pt' ? 'pt-BR' : 'en', { dateStyle: 'medium' }).format(dateOf(id.epochDay)))

  const coins = coinsBadge()
  const grid = h('div', { class: 'grid' })
  const gridArea = h('div', { class: 'grid-area' }, grid)
  const preview = h('div', { class: 'preview' })
  const previewArea = h('div', { class: 'preview-area' }, preview)
  const controls = h('div', { class: `controls${custom.leftHanded ? ' left-handed' : ''}` })
  const side = h('div', { class: 'side' }, previewArea, controls)
  const toastHost = h('div', { class: 'toast-host', 'aria-live': 'polite' })
  const body = h('div', { class: 'game' }, gridArea, side, toastHost)

  let current = ''
  let closeModal: (() => void) | null = null
  let completionShown = false
  const timers = new Set<ReturnType<typeof setTimeout>>()
  const later = (ms: number, fn: () => void) => {
    const timer = setTimeout(() => {
      timers.delete(timer)
      fn()
    }, ms)
    timers.add(timer)
  }

  // ----- Crossword grid -----
  type CellState = 'hidden' | 'hinted' | 'solved'
  const cells = new Map<number, { el: HTMLElement; state: CellState }>()
  for (const [position, letter] of level.letterAt) {
    const el = h('div', { class: 'cell hidden', dataset: { cell: String(position) } }, h('span', null, letter.toUpperCase()))
    cells.set(position, { el, state: 'hidden' })
    grid.appendChild(el)
  }

  function stateOf(position: number, solved: Set<number>): CellState {
    return solved.has(position) ? 'solved' : session.hinted.has(position) ? 'hinted' : 'hidden'
  }

  function updateGrid(animate: boolean): void {
    const solved = session.solvedCells
    for (const [position, cell] of cells) {
      const state = stateOf(position, solved)
      if (state !== cell.state) {
        const delay = animate && !custom.reduceMotion ? (session.revealDelay.get(position) ?? 0) * 55 : 0
        cell.el.style.setProperty('--delay', `${delay}ms`)
        cell.el.classList.remove(cell.state)
        cell.el.classList.add(state)
        if (animate && cell.state === 'hidden' && !custom.reduceMotion) replay(cell.el, 'pop')
        cell.state = state
      }
      cell.el.classList.toggle('targetable', session.targeting && state === 'hidden')
    }
    grid.classList.toggle('targeting', session.targeting)
  }

  function layoutGrid(): void {
    const width = gridArea.clientWidth
    const height = gridArea.clientHeight
    if (!width || !height) return
    const byWidth = width / (level.cols + (level.cols - 1) * GAP_RATIO)
    const byHeight = height / (level.rows + (level.rows - 1) * GAP_RATIO)
    const size = Math.floor(Math.min(byWidth, byHeight, MAX_CELL))
    const step = size * (1 + GAP_RATIO)
    grid.style.width = `${step * (level.cols - 1) + size}px`
    grid.style.height = `${step * (level.rows - 1) + size}px`
    grid.style.setProperty('--cell', `${size}px`)
    for (const [position, cell] of cells) {
      cell.el.style.transform = `translate(${step * colOf(position)}px, ${step * rowOf(position)}px)`
    }
  }

  grid.addEventListener('click', (e) => {
    if (!session.targeting) return
    const target = (e.target as HTMLElement).closest<HTMLElement>('.cell')
    if (!target) return
    session.target(Number(target.dataset.cell), TARGET_COST)
    afterReveal()
  })

  // ----- Word preview -----
  let feedback: { text: string; result: WordResult } | null = null
  let feedbackId = 0

  function updatePreview(): void {
    const verdict = current ? null : feedback
    let text = ''
    let tone = ''
    if (current) {
      text = current.toUpperCase()
      tone = 'typing'
    } else if (verdict) {
      text = verdict.text.toUpperCase()
      tone = verdict.result === 'found' ? 'found' : verdict.result === 'bonus' ? 'bonus' : verdict.result === 'invalid' ? 'invalid' : 'repeat'
    } else if (session.targeting) {
      text = t('target_prompt')
      tone = 'prompt'
    }
    preview.className = `preview ${tone}${text ? ' visible' : ''}`
    preview.replaceChildren(verdict?.result === 'bonus' ? icon('Sparkles', 18) : '', h('span', null, text))
  }

  function showFeedback(word: string, result: WordResult): void {
    feedback = { text: session.display(word), result }
    const mine = ++feedbackId
    updatePreview()
    if (result === 'invalid' && !custom.reduceMotion) replay(preview, 'shake')
    later(900, () => {
      if (feedbackId === mine) {
        feedback = null
        updatePreview()
      }
    })
  }

  // ----- Wheel and buttons -----
  const wheel = new LetterWheel(
    level.letters,
    session.order,
    {
      onChange: (word) => {
        current = word
        updatePreview()
      },
      onLetter: (count) => fx.letter(count),
      onSubmit: (word) => {
        current = ''
        const result = session.submit(word)
        if (word.length >= 2 || result !== 'invalid') showFeedback(word, result)
        else updatePreview()
        if (result === 'found') {
          fx.word()
          updateGrid(true)
        } else if (result === 'bonus') {
          fx.bonus()
          replay(bonusButton, 'bump')
          updateJar()
        } else if (result === 'repeat') {
          fx.tick()
          if (!custom.reduceMotion) for (const c of level.cellsOf.get(word)!) replay(cells.get(c)!.el, 'pulse')
        } else if (result === 'bonusRepeat') fx.tick()
        else if (word.length >= 2) fx.error()
      },
    },
    custom.reduceMotion,
  )

  const roundAction = (name: IconName, label: string, tone: string, onclick: () => void, tag?: HTMLElement) =>
    h('div', { class: 'action' }, button([icon(name, 24)], onclick, { size: 'round', label, extraClass: `btn-circle tone-${tone}` }), tag ?? null)
  const costTag = (cost: number) => h('span', { class: 'cost-tag' }, icon('Coins', 11), String(cost))
  const jarTag = h('span', { class: 'count-tag' })

  const shuffleAction = roundAction('Shuffle', t('shuffle'), 'base', () => {
    session.shuffle()
    wheel.setOrder(session.order)
    fx.shuffle()
  })
  const bonusAction = roundAction('Sparkles', t('bonus'), 'blue', () => showBonus(), jarTag)
  const bonusButton = bonusAction.querySelector('button')!
  const hintAction = roundAction('Lightbulb', t('hint'), 'orange', () => {
    session.hint(HINT_COST)
    afterReveal()
  }, costTag(HINT_COST))
  const targetAction = roundAction('Target', t('target'), 'red', () => {
    session.targeting = !session.targeting
    refreshTargeting()
  }, costTag(TARGET_COST))
  const targetButton = targetAction.querySelector('button')!

  controls.append(
    wheel.canvas,
    h('div', { class: 'action-col play-side' }, shuffleAction, bonusAction),
    h('div', { class: 'action-col help-side' }, hintAction, targetAction),
  )

  function updateJar(): void {
    jarTag.textContent = String(app.profile.jar)
    jarTag.hidden = app.profile.jar === 0
  }

  function refreshTargeting(): void {
    targetButton.classList.toggle('active', session.targeting)
    wheel.setEnabled(!session.targeting && !session.isSolved)
    updateGrid(false)
    updatePreview()
  }

  function afterReveal(): void {
    updateGrid(true)
    refreshTargeting()
  }

  function layout(): void {
    const width = body.clientWidth
    const height = body.clientHeight
    if (!width || !height) return
    const landscape = width > height
    body.classList.toggle('landscape', landscape)
    if (!landscape) {
      const wheelArea = Math.min(width * 0.86, height * 0.44)
      side.style.height = `${PREVIEW + wheelArea}px`
    } else side.style.height = ''
    // Read sizes after the class change has applied.
    const cw = controls.clientWidth
    const ch = controls.clientHeight - 8
    const diameter = Math.max(80, Math.min(ch, cw - BUTTON * 2 + 24) * custom.wheelScale)
    wheel.resize(Math.floor(diameter))
    layoutGrid()
  }

  // ----- Overlays -----
  function showBonus(): void {
    const words = [...session.bonus].map((w) => session.display(w)).sort((a, b) => a.localeCompare(b, lang()))
    const content = h(
      'div',
      { class: 'modal-content' },
      h('div', { class: 'row center gap-row' }, iconTile('Sparkles', 'blue'), h('h2', { class: 'title grow' }, t('bonus_title'))),
      progressBar(app.profile.jar / JAR_SIZE, 'blue'),
      h('p', { class: 'small secondary' }, t('jar_progress', app.profile.jar, JAR_SIZE, JAR_REWARD)),
      id.language === 'pt' ? h('p', { class: 'small secondary row gap-row-sm' }, icon('BookOpen', 16), h('span', null, t('bonus_rule_pt'))) : null,
      words.length === 0 ? h('p', null, t('bonus_empty')) : h('div', { class: 'word-list' }, words.map((w) => badge(w.toUpperCase(), undefined, 'contrast'))),
      button([h('span', null, t('close'))], () => closeAll(), { size: 'xl', extraClass: 'full' }),
    )
    closeModal = openModal(content, () => (closeModal = null))
  }

  function closeAll(): void {
    closeModal?.()
    closeModal = null
  }

  function showCompletion(completion: Completion): void {
    if (completionShown) return
    completionShown = true
    fx.complete()
    wheel.setEnabled(false)
    later(650, () => {
      if (!custom.reduceMotion) confetti(body)
      const next = completion.daily
        ? null
        : button([h('span', null, t('next_level')), icon('ChevronRight')], () => {
            closeAll()
            void app.play()
          }, { type: 'colored', size: 'xl', extraClass: 'full' })
      const content = h(
        'div',
        { class: 'modal-content center-text' },
        iconTile('Trophy', 'brand', 64),
        h('h2', { class: 'title' }, t(completion.daily ? 'daily_complete' : 'level_complete')),
        h(
          'div',
          { class: 'row wrap center-row gap-row-sm' },
          badge(plural('words_found', completion.words), 'Check', 'brand'),
          completion.bonus > 0 ? badge(plural('bonus_found', completion.bonus), 'Sparkles', 'blue') : null,
        ),
        completion.reward > 0 ? h('div', null, badge(`+${completion.reward}`, 'Coins', 'orange')) : null,
        h(
          'div',
          { class: 'stack gap-sm' },
          next,
          button([icon('House'), h('span', null, t('home'))], () => {
            closeAll()
            app.navigate('home')
          }, { type: next ? 'base' : 'colored', size: 'xl', extraClass: 'full' }),
        ),
      )
      closeAll()
      closeModal = openModal(content, null)
    })
  }

  function showToast(): void {
    const toast = app.toast
    if (!toast) {
      toastHost.firstElementChild?.classList.add('leaving')
      return
    }
    const [name, tone, text]: [IconName, string, string] =
      toast.kind === 'jar' ? ['Coins', 'orange', t('jar_reward', toast.amount)] : ['X', 'red', t('not_enough_coins')]
    toastHost.replaceChildren(h('div', { class: 'toast' }, h('span', { class: `tone-${tone}` }, icon(name, 18)), h('span', null, text)))
    later(2000, () => app.dismissToast(toast.id))
  }

  const unsubscribe = app.subscribe((change: Change) => {
    if (change === 'profile') {
      coins.querySelector('.coins-value')!.textContent = String(app.profile.coins)
      updateJar()
    } else if (change === 'toast') showToast()
    else if (change === 'completion' && app.completion?.key === level.key) showCompletion(app.completion)
  })

  const observer = new ResizeObserver(() => layout())
  observer.observe(body)

  updateGrid(false)
  updateJar()
  updatePreview()
  if (session.isSolved) wheel.setEnabled(false)
  if (app.completion?.key === level.key) showCompletion(app.completion)

  const languageName = LANGUAGES.find((l) => l.code === id.language)!.nativeName
  const el = frame(
    [
      navButton('ArrowLeft', t('back'), () => app.navigate('home')),
      h('div', { class: 'grow min0 title-block' }, h('h1', { class: 'heading ellipsis' }, title), h('div', { class: 'small secondary' }, languageName)),
      coins,
    ],
    [body],
  )

  return {
    el,
    dispose: () => {
      unsubscribe()
      observer.disconnect()
      wheel.destroy()
      timers.forEach(clearTimeout)
      closeAll()
    },
  }
}

/** A short burst of confetti on one canvas, drawn from a single clock. */
function confetti(host: HTMLElement): void {
  const canvas = h('canvas', { class: 'confetti', 'aria-hidden': 'true' })
  host.appendChild(canvas)
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const width = host.clientWidth
  const height = host.clientHeight
  canvas.width = width * dpr
  canvas.height = height * dpr
  const ctx = canvas.getContext('2d')!
  ctx.scale(dpr, dpr)
  const style = getComputedStyle(document.documentElement)
  const palette = ['--brand', '--blue', '--purple', '--orange', '--red'].map((v) => style.getPropertyValue(v).trim())
  const particles = Array.from({ length: 80 }, () => ({
    angle: -Math.PI / 2 + (Math.random() - 0.5) * Math.PI * 0.9,
    speed: 0.55 + Math.random() * 0.65,
    spin: (Math.random() - 0.5) * 1080,
    color: palette[Math.floor(Math.random() * palette.length)],
    size: 6 + Math.random() * 6,
    drift: (Math.random() - 0.5) * 0.3,
  }))
  const start = performance.now()
  const origin = { x: width / 2, y: height * 0.42 }
  const reach = height * 0.9
  const step = (now: number) => {
    const t = (now - start) / 2200
    ctx.clearRect(0, 0, width, height)
    if (t >= 1 || !canvas.isConnected) {
      canvas.remove()
      return
    }
    ctx.globalAlpha = Math.max(0, 1 - t * t)
    for (const p of particles) {
      const x = origin.x + (Math.cos(p.angle) * p.speed + p.drift * t) * reach * t
      const y = origin.y + Math.sin(p.angle) * p.speed * reach * t + 1.1 * reach * t * t
      ctx.save()
      ctx.translate(x, y)
      ctx.rotate((p.spin * t * Math.PI) / 180)
      ctx.fillStyle = p.color
      ctx.beginPath()
      ctx.roundRect(-p.size / 2, -p.size / 4, p.size, p.size / 2, p.size / 6)
      ctx.fill()
      ctx.restore()
    }
    requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}
