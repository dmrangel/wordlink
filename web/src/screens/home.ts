import { app } from '../app'
import { badge, button, card, coinsBadge, frame, iconTile, logo, navButton, progressBar, sectionTitle } from '../components'
import { h, icon } from '../dom'
import { plural, t } from '../i18n'
import type { IconName } from '../icons'
import { canPromptInstall, dismissInstall, isIos, promptInstall, shouldSuggestInstall } from '../install'
import { LANGUAGES } from '../store'

const MILESTONE = 10

export function homeScreen(): HTMLElement {
  const language = app.settings.language
  const level = app.level()
  const dailySolved = app.dailySolved()
  const streak = app.streak
  const inMilestone = (level - 1) % MILESTONE

  const hero = card(
    [
      h(
        'div',
        { class: 'row center' },
        h('div', { class: 'grow' }, h('div', { class: 'caption brand' }, t('level_caption')), h('div', { class: 'display' }, String(level))),
        badge(LANGUAGES.find((l) => l.code === language)!.nativeName, 'Languages'),
      ),
      h('div', { class: 'gap-md' }, progressBar(inMilestone / MILESTONE)),
      h('div', { class: 'small secondary gap-sm' }, plural('next_milestone', MILESTONE - inMilestone)),
      app.loadError ? h('div', { class: 'small error gap-sm', role: 'alert' }, t('load_failed')) : null,
      button(
        [icon('Play', 22), h('span', null, app.loading ? t('loading') : app.loadError ? t('retry') : t('play'))],
        () => void app.play(),
        { type: 'colored', size: 'xxl', extraClass: 'full gap-lg', disabled: app.loading },
      ),
    ],
    'hero',
  )

  const languages = h(
    'div',
    { class: 'two-col' },
    LANGUAGES.map((option) => {
      const selected = option.code === language
      return card(
        [
          h(
            'div',
            { class: 'row center' },
            h('span', { class: `lang-code${selected ? ' selected' : ''}` }, option.code.toUpperCase()),
            h('span', { class: 'grow' }),
            selected ? h('span', { class: 'brand' }, icon('Check')) : null,
          ),
          h('div', { class: 'heading gap-md ellipsis' }, option.nativeName),
          h('div', { class: 'small secondary' }, t('level_n', app.level(option.code))),
        ],
        `lang-card${selected ? ' selected' : ''}`,
        () => app.update((s) => ({ ...s, language: option.code })),
      )
    }),
  )

  const daily = card(
    [
      h(
        'div',
        { class: 'row center gap-row' },
        iconTile('CalendarDays', 'blue'),
        h('div', { class: 'grow' }, h('div', { class: 'heading' }, t('daily_puzzle')), h('div', { class: 'small secondary' }, t(dailySolved ? 'daily_solved' : 'daily_new'))),
        dailySolved ? iconTile('Check', 'brand', 36) : h('span', { class: 'secondary' }, icon('ChevronRight')),
      ),
      streak > 0 ? h('div', { class: 'gap-md' }, badge(plural('days', streak), 'Flame', 'orange')) : null,
    ],
    'daily',
    () => void app.play(true),
  )

  const completed = Object.values(app.profile.levels).reduce((sum, l) => sum + l - 1, 0)
  const stat = (name: IconName, tone: string, value: number, label: string) =>
    card([h('div', { class: 'row center gap-row' }, iconTile(name, tone, 40), h('div', { class: 'min0' }, h('div', { class: 'heading ellipsis' }, String(value)), h('div', { class: 'small secondary ellipsis' }, label)))])

  return frame(
    [
      h('span', { class: 'logo-slot' }, logo(36)),
      h('h1', { class: 'title grow' }, 'Wordlink'),
      coinsBadge(),
      navButton('Settings', t('settings'), () => app.navigate('settings')),
    ],
    [
      h(
        'div',
        { class: 'scroll' },
        h(
          'div',
          { class: 'column' },
          installCard(),
          hero,
          sectionTitle(t('language')),
          languages,
          daily,
          sectionTitle(t('stats')),
          h('div', { class: 'two-col' }, stat('Trophy', 'brand', completed, t('stat_levels')), stat('BookOpen', 'purple', app.profile.wordsFound, t('stat_words'))),
          h('div', { class: 'two-col' }, stat('Sparkles', 'blue', app.profile.bonusFound, t('stat_bonus')), stat('Flame', 'orange', streak, t('stat_streak'))),
        ),
      ),
    ],
  )
}

/** iOS: how to Add to Home Screen. Chrome/Android: the native install prompt. */
export function installCard(dismissible = true): HTMLElement | null {
  if (dismissible && !shouldSuggestInstall()) return null
  const body = isIos ? iosInstructions() : h('p', { class: 'small' }, t('install_other'))
  const el = card(
    [
      h(
        'div',
        { class: 'row center gap-row' },
        iconTile('SquarePlus', 'brand', 40),
        h('div', { class: 'heading grow' }, t('install_title')),
        dismissible
          ? button([icon('X', 18)], () => {
              dismissInstall()
              el.remove()
            }, { type: 'quiet', size: 'sm', label: t('dismiss'), extraClass: 'btn-circle' })
          : null,
      ),
      h('div', { class: 'gap-sm' }, body),
      !isIos && canPromptInstall()
        ? button([icon('SquarePlus', 18), h('span', null, t('install_button'))], () => void promptInstall().then(() => app.navigate(app.screen)), {
            type: 'colored',
            extraClass: 'full gap-md',
          })
        : null,
    ],
    'install-card',
  )
  return el
}

function iosInstructions(): HTMLElement {
  // "{share}" and "{add}" become the icons Safari shows for those actions.
  const p = h('p', { class: 'small' })
  for (const part of t('install_ios').split(/(\{share\}|\{add\})/)) {
    if (part === '{share}') p.appendChild(h('span', { class: 'inline-icon' }, icon('Share', 16)))
    else if (part === '{add}') p.appendChild(h('span', { class: 'inline-icon' }, icon('SquarePlus', 16)))
    else p.appendChild(document.createTextNode(part))
  }
  return p
}
