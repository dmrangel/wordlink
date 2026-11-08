// Service worker: precaches the whole app on install so it plays offline.
// dist/sw.js is generated from this file by vite.config.ts.
const VERSION = '__VERSION__'
const PRECACHE = __PRECACHE__
const CACHE = `wordlink-${VERSION}`

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key.startsWith('wordlink-') && key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  )
})

self.addEventListener('fetch', (event) => {
  const request = event.request
  if (request.method !== 'GET' || new URL(request.url).origin !== self.location.origin) return
  // Pages always resolve to the cached app shell; everything else is cache-first.
  // ignoreVary: module scripts carry an Origin header the precache requests did not,
  // and servers answer with Vary (Origin, Accept-Encoding), which would miss.
  const lookup = caches.match(request.mode === 'navigate' ? './' : request, { ignoreVary: true })
  event.respondWith(lookup.then((cached) => cached || fetch(request)))
})
