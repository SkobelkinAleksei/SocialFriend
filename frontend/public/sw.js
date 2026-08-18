const SW_VERSION = '5';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

// Слушатель нужен, чтобы Chrome считал это приложением.
// Запросы не перехватываем: иначе с ярлыка «Домой» ломается вход и весь сайт (ERR_FAILED).
self.addEventListener('fetch', () => {});

self.addEventListener('push', (event) => {
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    if (windows.some((client) => 'focused' in client && client.focused)) {
      return;
    }
    let data = { title: 'На районе', body: 'На районе', url: '/' };
    try {
      if (event.data) {
        data = { ...data, ...event.data.json() };
      }
    } catch {
      /* ignore */
    }
    await self.registration.showNotification(data.title || 'На районе', {
      body: data.body || 'На районе',
      icon: '/icons/icon-192.png',
      badge: '/icons/icon-192.png',
      data: { url: data.url || '/' },
    });
  })());
});

self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = (event.notification.data && event.notification.data.url) || '/';
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clients) => {
      for (const client of clients) {
        if ('focus' in client) {
          client.focus();
          if ('navigate' in client) client.navigate(url);
          return;
        }
      }
      if (self.clients.openWindow) {
        return self.clients.openWindow(url);
      }
    })
  );
});
