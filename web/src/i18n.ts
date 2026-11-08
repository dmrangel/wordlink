// Interface strings. The Android app keeps the same texts in res/values*/strings.xml;
// like there, the interface follows the game language picked in the app.

export type Lang = 'en' | 'pt'

const en = {
  play: 'Play',
  level_n: 'Level {0}',
  level_caption: 'Level',
  next_milestone: { one: '{0} level to the next milestone', other: '{0} levels to the next milestone' },
  language: 'Language',
  daily_puzzle: 'Daily puzzle',
  daily_new: 'A fresh 7-letter puzzle every day',
  daily_solved: 'Solved today, come back tomorrow',
  stats: 'Stats',
  stat_levels: 'Levels',
  stat_words: 'Words',
  stat_bonus: 'Bonus',
  stat_streak: 'Streak',
  loading: 'Loading…',
  load_failed: 'Could not load the word list. Check your connection and try again.',
  retry: 'Try again',

  settings: 'Settings',
  appearance: 'Appearance',
  theme: 'Theme',
  theme_dark: 'Dark',
  theme_oled: 'OLED',
  theme_light: 'Light',
  theme_system: 'System',
  theme_custom: 'Custom',
  interface_language_desc: 'Also changes the language of the whole app',
  customization: 'Customization',
  preview: 'Preview',
  accent_color: 'Accent color',
  accent_default: 'Theme default',
  custom_color: 'Custom color',
  background_photo: 'Background photo',
  background_photo_desc: 'Shown behind every screen. Nothing leaves your device.',
  background_failed: 'That image could not be used. Try a JPEG or PNG.',
  choose_photo: 'Choose photo',
  change_photo: 'Change',
  remove: 'Remove',
  blur: 'Blur',
  dim: 'Dim',
  panel_opacity: 'Panel opacity',
  custom_theme: 'Custom theme',
  custom_theme_desc: 'Pick a base color; surfaces, borders and text are derived from it, light or dark.',
  base_color: 'Base color',
  board: 'Board',
  tile_shape: 'Tile shape',
  shape_square: 'Square',
  shape_rounded: 'Rounded',
  shape_circle: 'Circle',
  wheel_size: 'Letter wheel size',
  left_handed: 'Left-handed layout',
  left_handed_desc: 'Puts hint and target on the left side of the wheel',
  reduce_motion: 'Reduce motion',
  reduce_motion_desc: 'Turns off confetti, shakes and letter pop-ins',
  reset_customization: 'Reset customization',
  gameplay: 'Gameplay',
  sound: 'Sound effects',
  haptics: 'Vibration',
  about: 'About',
  about_text: 'Free and open source. No ads, no accounts, no tracking. The visual design follows the Modrinth App design system; Wordlink is not affiliated with Modrinth.',
  credits: 'Word lists: ENABLE, SCOWL, FrequencyWords, fserb/pt-br and VERO. Font: Inter. Icons: Lucide.',
  version: 'Version {0}',

  backup: 'Progress backup',
  backup_desc: 'A code with your coins, levels and streak. Paste it on another device or in the Android app to carry on there.',
  copy_code: 'Copy code',
  code_copied: 'Copied',
  share: 'Share',
  restore: 'Restore',
  restore_title: 'Restore progress',
  restore_desc: 'Paste a backup code. It replaces the coins, levels and streak on this device.',
  paste: 'Paste',
  invalid_code: "That code isn't valid. Check that it was copied completely.",
  restored: 'Progress restored',

  safety: 'Keep your progress',
  install_title: 'Install Wordlink',
  install_ios: 'Tap {share} Share, then {add} Add to Home Screen. Safari can erase the progress of websites you leave unopened for 7 days; the installed app keeps it, and it works offline.',
  install_other: 'Install the app to play offline and keep your progress safe from browser cleanup.',
  install_button: 'Install',
  installed: 'Installed as an app',
  not_installed: 'Running in the browser',
  storage_persistent: 'Storage is protected from automatic cleanup',
  storage_best_effort: 'The browser may clear storage when space runs low; keep a backup code',
  storage_failed: 'This browser is not saving progress (private mode?). It will be lost when you close it.',
  dismiss: 'Dismiss',

  daily_title: 'Daily · {0}',
  shuffle: 'Shuffle',
  hint: 'Hint',
  target: 'Target',
  bonus: 'Bonus',
  back: 'Back',
  target_prompt: 'Tap a square to reveal it',
  not_enough_coins: 'Not enough coins',
  jar_reward: 'Bonus jar full! +{0} coins',
  bonus_title: 'Bonus words',
  bonus_rule_pt: 'Portuguese mode only counts singular words and verbs in the infinitive.',
  bonus_empty: "Find real words that aren't in the grid to fill the jar.",
  jar_progress: '{0} of {1} for +{2} coins',
  close: 'Close',
  cancel: 'Cancel',
  coins: 'Coins',

  level_complete: 'Level complete!',
  daily_complete: 'Daily puzzle solved!',
  next_level: 'Next level',
  home: 'Home',
  words_found: { one: '{0} word', other: '{0} words' },
  bonus_found: { one: '{0} bonus word', other: '{0} bonus words' },
  days: { one: '{0} day', other: '{0} days' },
}

type Strings = typeof en
type Plural = { one: string; other: string }
export type Key = { [K in keyof Strings]: Strings[K] extends string ? K : never }[keyof Strings]
export type PluralKey = { [K in keyof Strings]: Strings[K] extends Plural ? K : never }[keyof Strings]

const pt: Strings = {
  play: 'Jogar',
  level_n: 'Nível {0}',
  level_caption: 'Nível',
  next_milestone: { one: 'Falta {0} nível para o próximo marco', other: 'Faltam {0} níveis para o próximo marco' },
  language: 'Idioma',
  daily_puzzle: 'Desafio diário',
  daily_new: 'Um diagrama novo de 7 letras todo dia',
  daily_solved: 'Resolvido hoje, volte amanhã',
  stats: 'Estatísticas',
  stat_levels: 'Níveis',
  stat_words: 'Palavras',
  stat_bonus: 'Bônus',
  stat_streak: 'Sequência',
  loading: 'Carregando…',
  load_failed: 'Não foi possível carregar a lista de palavras. Confira a conexão e tente de novo.',
  retry: 'Tentar de novo',

  settings: 'Configurações',
  appearance: 'Aparência',
  theme: 'Tema',
  theme_dark: 'Escuro',
  theme_oled: 'OLED',
  theme_light: 'Claro',
  theme_system: 'Sistema',
  theme_custom: 'Personalizado',
  interface_language_desc: 'Também muda o idioma de todo o app',
  customization: 'Personalização',
  preview: 'Prévia',
  accent_color: 'Cor de destaque',
  accent_default: 'Padrão do tema',
  custom_color: 'Cor personalizada',
  background_photo: 'Foto de fundo',
  background_photo_desc: 'Aparece atrás de todas as telas. Nada sai do seu aparelho.',
  background_failed: 'Não foi possível usar essa imagem. Tente um JPEG ou PNG.',
  choose_photo: 'Escolher foto',
  change_photo: 'Trocar',
  remove: 'Remover',
  blur: 'Desfoque',
  dim: 'Escurecer',
  panel_opacity: 'Opacidade dos painéis',
  custom_theme: 'Tema personalizado',
  custom_theme_desc: 'Escolha uma cor base; superfícies, bordas e textos são derivados dela, claros ou escuros.',
  base_color: 'Cor base',
  board: 'Tabuleiro',
  tile_shape: 'Formato dos quadrados',
  shape_square: 'Quadrado',
  shape_rounded: 'Arredondado',
  shape_circle: 'Círculo',
  wheel_size: 'Tamanho da roda de letras',
  left_handed: 'Modo canhoto',
  left_handed_desc: 'Leva a dica e o alvo para o lado esquerdo da roda',
  reduce_motion: 'Reduzir animações',
  reduce_motion_desc: 'Desliga confetes, tremidas e a entrada animada das letras',
  reset_customization: 'Restaurar personalização',
  gameplay: 'Jogo',
  sound: 'Efeitos sonoros',
  haptics: 'Vibração',
  about: 'Sobre',
  about_text: 'Livre e de código aberto. Sem anúncios, sem contas, sem rastreamento. O visual segue o design system do Modrinth App; o Wordlink não tem afiliação com o Modrinth.',
  credits: 'Listas de palavras: ENABLE, SCOWL, FrequencyWords, fserb/pt-br e VERO. Fonte: Inter. Ícones: Lucide.',
  version: 'Versão {0}',

  backup: 'Backup do progresso',
  backup_desc: 'Um código com suas moedas, níveis e sequência. Cole em outro aparelho ou no app Android para continuar de lá.',
  copy_code: 'Copiar código',
  code_copied: 'Copiado',
  share: 'Compartilhar',
  restore: 'Restaurar',
  restore_title: 'Restaurar progresso',
  restore_desc: 'Cole um código de backup. Ele substitui as moedas, os níveis e a sequência deste aparelho.',
  paste: 'Colar',
  invalid_code: 'Esse código não é válido. Confira se ele foi copiado inteiro.',
  restored: 'Progresso restaurado',

  safety: 'Guarde seu progresso',
  install_title: 'Instale o Wordlink',
  install_ios: 'Toque em {share} Compartilhar e depois em {add} Adicionar à Tela de Início. O Safari pode apagar o progresso de sites que ficam 7 dias sem ser abertos; o app instalado guarda tudo e funciona offline.',
  install_other: 'Instale o app para jogar offline e proteger seu progresso da limpeza do navegador.',
  install_button: 'Instalar',
  installed: 'Instalado como app',
  not_installed: 'Rodando no navegador',
  storage_persistent: 'O armazenamento está protegido da limpeza automática',
  storage_best_effort: 'O navegador pode limpar o armazenamento se faltar espaço; guarde um código de backup',
  storage_failed: 'Este navegador não está salvando o progresso (modo privado?). Ele será perdido ao fechar.',
  dismiss: 'Dispensar',

  daily_title: 'Diário · {0}',
  shuffle: 'Embaralhar',
  hint: 'Dica',
  target: 'Alvo',
  bonus: 'Bônus',
  back: 'Voltar',
  target_prompt: 'Toque num quadrado para revelá-lo',
  not_enough_coins: 'Moedas insuficientes',
  jar_reward: 'Pote de bônus cheio! +{0} moedas',
  bonus_title: 'Palavras bônus',
  bonus_rule_pt: 'No português valem só palavras no singular e verbos no infinitivo (sem plurais nem conjugações).',
  bonus_empty: 'Encontre palavras reais que não estão no diagrama para encher o pote.',
  jar_progress: '{0} de {1} para +{2} moedas',
  close: 'Fechar',
  cancel: 'Cancelar',
  coins: 'Moedas',

  level_complete: 'Nível concluído!',
  daily_complete: 'Desafio diário resolvido!',
  next_level: 'Próximo nível',
  home: 'Início',
  words_found: { one: '{0} palavra', other: '{0} palavras' },
  bonus_found: { one: '{0} palavra bônus', other: '{0} palavras bônus' },
  days: { one: '{0} dia', other: '{0} dias' },
}

const tables: Record<Lang, Strings> = { en, pt }
let current: Lang = 'en'
let rules = new Intl.PluralRules('en')

export function setLang(lang: Lang): void {
  current = lang
  rules = new Intl.PluralRules(lang)
  document.documentElement.lang = lang === 'pt' ? 'pt-BR' : 'en'
}

export function lang(): Lang {
  return current
}

function format(text: string, args: (string | number)[]): string {
  return text.replace(/\{(\d)\}/g, (_, i) => String(args[Number(i)]))
}

export function t(key: Key, ...args: (string | number)[]): string {
  return format(tables[current][key] as string, args)
}

/** Uses the same CLDR plural rules as Android's quantity strings. */
export function plural(key: PluralKey, count: number): string {
  const forms = tables[current][key] as Plural
  return format(rules.select(count) === 'one' ? forms.one : forms.other, [count])
}
