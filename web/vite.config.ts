import { createHash } from 'node:crypto'
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { join, relative } from 'node:path'
import { defineConfig, type Plugin } from 'vite'

const core = fileURLToPath(new URL('../core/build/dist/js/productionLibrary/wordlink-core.mjs', import.meta.url))

/**
 * Writes dist/sw.js from src/sw.js with the list of every built file and a
 * version hash of their contents, so the service worker precaches the whole
 * app (dictionaries and sounds included) and a new build replaces the cache.
 */
function serviceWorker(): Plugin {
  let outDir = 'dist'
  return {
    name: 'wordlink-service-worker',
    apply: 'build',
    configResolved(config) {
      outDir = config.build.outDir
    },
    closeBundle() {
      const files: string[] = []
      const walk = (dir: string) => {
        for (const name of readdirSync(dir)) {
          const path = join(dir, name)
          if (statSync(path).isDirectory()) walk(path)
          else if (!name.endsWith('.map') && name !== 'sw.js') files.push(relative(outDir, path).split('\\').join('/'))
        }
      }
      walk(outDir)
      files.sort()
      const template = readFileSync(fileURLToPath(new URL('src/sw.js', import.meta.url)), 'utf8')
      const hash = createHash('sha256').update(template)
      for (const file of files) hash.update(file).update(readFileSync(join(outDir, file)))
      const version = hash.digest('hex').slice(0, 12)
      const precache = ['./', ...files.filter((f) => f !== 'index.html')]
      writeFileSync(
        join(outDir, 'sw.js'),
        template.replace('__VERSION__', version).replace('__PRECACHE__', JSON.stringify(precache)),
      )
    },
  }
}

export default defineConfig({
  // Relative paths: GitHub Pages serves the app from /<repo>/.
  base: './',
  define: {
    __APP_VERSION__: JSON.stringify(JSON.parse(readFileSync(fileURLToPath(new URL('package.json', import.meta.url)), 'utf8')).version),
  },
  resolve: {
    alias: { 'wordlink-core': core },
  },
  build: {
    target: ['safari16.4', 'chrome111', 'firefox115'],
    assetsInlineLimit: 0,
  },
  plugins: [serviceWorker()],
})
