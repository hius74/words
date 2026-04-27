const CACHE_NAME = 'words-app-cache-v1';
const ASSETS_TO_CACHE = [
    '/',
    '/favicon.svg',
    '/index.css',
    '/index.html',
    '/index.js',
    '/settings.css',
    '/settings.html',
    '/settings.js',
];

const sw = self as unknown as ServiceWorkerGlobalScope;

// Install Event: Save assets to cache
sw.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(ASSETS_TO_CACHE);
        })
    );
});

// Fetch Event: Implement Cache-First Strategy
sw.addEventListener('fetch', (event) => {
    event.respondWith(
        caches.match(event.request).then((response) => {
            // Return cached file OR fetch from network if not in cache
            return response || fetch(event.request);
        })
    );
});