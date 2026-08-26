// Service Worker for PWA
// 版本号变更会在 activate 阶段清掉所有旧缓存，发布出现异常时可用它强制刷新客户端
const CACHE_NAME = 'sheep-music-v2'
const urlsToCache = [
  '/',
  '/index.html',
  '/manifest.json'
]

// 安装 Service Worker
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Opened cache')
        return cache.addAll(urlsToCache)
      })
  )
  self.skipWaiting()
})

// 激活 Service Worker
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName)
            return caches.delete(cacheName)
          }
        })
      )
    })
  )
  self.clients.claim()
})

// 是否为页面导航请求（HTML 文档本身，而非页面引用的资源）
const isNavigationRequest = (request) => {
  if (request.mode === 'navigate') return true
  const accept = request.headers.get('accept')
  return !!accept && accept.includes('text/html')
}

// 拦截请求
self.addEventListener('fetch', (event) => {
  const request = event.request

  // 非 GET 请求（登录、上传等）不介入，交给浏览器默认处理
  if (request.method !== 'GET') return

  // 导航请求走 network-first。
  // 若对 index.html 也用缓存优先，它会被永久钉住：里面引用的是旧的带 hash
  // 的资源名，新版本上线后，所有访问过站点的浏览器都会一直停在旧版本。
  if (isNavigationRequest(request)) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response && response.status === 200 && response.type === 'basic') {
            const responseToCache = response.clone()
            caches.open(CACHE_NAME).then((cache) => {
              cache.put(request, responseToCache)
            })
          }
          return response
        })
        // 离线兜底：先找该地址的缓存，再退回 SPA 外壳
        .catch(() => caches.match(request).then((cached) => cached || caches.match('/index.html')))
    )
    return
  }

  // 静态资源走 cache-first：构建产物文件名自带 hash，内容变了 URL 也会变，
  // 不存在拿到陈旧内容的问题。
  event.respondWith(
    caches.match(request)
      .then((response) => {
        // 缓存命中，返回缓存
        if (response) {
          return response
        }

        // 网络请求
        return fetch(request).then((response) => {
          // 检查是否是有效响应
          if (!response || response.status !== 200 || response.type !== 'basic') {
            return response
          }

          // 克隆响应
          const responseToCache = response.clone()

          caches.open(CACHE_NAME).then((cache) => {
            cache.put(request, responseToCache)
          })

          return response
        })
      })
      .catch(() => {
        // 离线时显示离线页面（可选）
        return caches.match('/offline.html')
      })
  )
})

// 监听消息
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting()
  }
})
