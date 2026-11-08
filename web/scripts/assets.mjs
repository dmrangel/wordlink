// Copies the files web/ shares with the Android app into public/ (git-ignored),
// so app/src/main stays the only copy of the dictionaries and sounds.
import { cpSync, mkdirSync, readdirSync, rmSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const web = join(dirname(fileURLToPath(import.meta.url)), '..')
const main = join(web, '..', 'app', 'src', 'main')

const copies = [
  [join(main, 'assets', 'dict'), join(web, 'public', 'dict'), '.txt'],
  [join(main, 'res', 'raw'), join(web, 'public', 'sounds'), '.wav'],
  [join(main, 'assets', 'licenses'), join(web, 'public', 'licenses'), '.txt'],
]
for (const [from, to, extension] of copies) {
  rmSync(to, { recursive: true, force: true })
  mkdirSync(to, { recursive: true })
  for (const name of readdirSync(from).filter((n) => n.endsWith(extension))) cpSync(join(from, name), join(to, name))
}
console.log('assets: dictionaries, sounds and licenses copied to public/')
