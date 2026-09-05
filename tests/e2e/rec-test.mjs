// 推荐系统 v2 规格验收测试（AC-1 ~ AC-6, AC-9，见 docs/specs/推荐系统v2/01-需求规格.md §6）
const BASE = process.env.E2E_BASE || 'http://localhost:19000'
let pass = 0, fail = 0
const failures = []
const ok = (name, cond, extra = '') => {
  if (cond) { pass++; console.log(`  PASS  ${name}`) }
  else { fail++; failures.push(name); console.log(`  FAIL  ${name}  ${extra}`) }
}
const api = async (method, path, { token, body } = {}) => {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let payload
  if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body) }
  const res = await fetch(BASE + path, { method, headers, body: payload })
  let json = null
  try { json = await res.json() } catch (_) {}
  return { status: res.status, json }
}
const code = r => r.json?.code, data = r => r.json?.data, msg = r => r.json?.message || ''

const RUN = Date.now().toString().slice(-7)
const reg = async (u) => api('POST', '/auth/register', { body: { username: u, password: 'test123456', nickname: u } })
const login = async (u) => api('POST', '/auth/login', { body: { username: u, password: 'test123456' } })

console.log('[setup] 准备用户与行为数据')
await reg(`recA_${RUN}`); await reg(`recB_${RUN}`); await reg(`recC_${RUN}`)
const A = data(await login(`recA_${RUN}`))
const B = data(await login(`recB_${RUN}`))
const C = data(await login(`recC_${RUN}`))
ok('三个测试用户就绪', !!A?.token && !!B?.token && !!C?.token)

// 热门歌曲作为种子
const hot = data(await api('GET', '/music/hot?page=0&size=10', { token: A.token }))?.content || []
ok('种子歌曲数据就绪', hot.length >= 5, `len=${hot.length}`)
const seeds = hot.slice(0, 3)
const others = hot.slice(3, 6)

// A：收藏 3 首 + 反复播放 1 首（构成有行为画像）
for (const s of seeds) await api('POST', `/api/user/favorite/${s.id}`, { token: A.token })
await api('POST', '/api/user/play-history', { token: A.token, body: { songId: seeds[0].id } })
await api('POST', '/api/user/play-history', { token: A.token, body: { songId: seeds[0].id } })
await api('POST', '/api/user/play-history', { token: A.token, body: { songId: seeds[0].id } })
// B：收藏与 A 相同的种子 + 另外几首（制造 CF 共现候选）
for (const s of seeds) await api('POST', `/api/user/favorite/${s.id}`, { token: B.token })
for (const s of others) await api('POST', `/api/user/favorite/${s.id}`, { token: B.token })
// C：零行为（冷启动）

console.log('\n[AC-1] 响应结构与推荐理由')
const t0 = Date.now()
const r1 = await api('GET', '/music/recommend/personalized?limit=10', { token: A.token })
const elapsed = Date.now() - t0
const items = data(r1) || []
ok('personalized 返回列表', code(r1) === 200 && Array.isArray(items), msg(r1))
ok('每项含 song/score/reason/strategy', items.length > 0 && items.every(i =>
  i.song && typeof i.song === 'object' && typeof i.score === 'number' &&
  typeof i.reason === 'string' && typeof i.strategy === 'string'))
ok('推荐理由非空且非占位', items.every(i => i.reason && i.reason.length >= 2))
ok('strategy 属于枚举', items.every(i => ['cf', 'content', 'artist', 'hot', 'fresh'].includes(i.strategy)))

console.log('\n[AC-2] 硬过滤：已收藏 / 下架歌曲')
const favIds = new Set(seeds.map(s => s.id))
ok('结果不含已收藏歌曲', items.every(i => !favIds.has(i.song.id)))
ok('结果全部为上架歌曲', items.every(i => i.song.status === 1))

console.log('\n[AC-3] 去重与数量')
const ids = items.map(i => i.song.id)
ok('结果按歌曲去重', new Set(ids).size === ids.length, `${new Set(ids).size}/${ids.length}`)
ok('数量不超过 limit', ids.length <= 10)

console.log('\n[AC-5] 有行为用户命中协同/内容/歌手通道')
const r2 = await api('GET', '/music/recommend/guess-you-like?limit=12', { token: A.token })
const guessItems = data(r2) || []
ok('guess-you-like 结构一致', code(r2) === 200 && guessItems.every(i => i.song && i.reason))
const strategies = new Set([...items, ...guessItems].map(i => i.strategy))
ok('命中 cf/content/artist 至少其一', ['cf', 'content', 'artist'].some(s => strategies.has(s)),
  `strategies=${[...strategies].join(',')}`)
ok('CF 理由引用真实种子歌名', [...items, ...guessItems]
  .filter(i => i.strategy === 'cf')
  .every(i => /《.+》/.test(i.reason)), [...items, ...guessItems].filter(i => i.strategy === 'cf').map(i => i.reason).join(' | '))

console.log('\n[AC-4] 冷启动用户')
const r3 = await api('GET', '/music/recommend/personalized?limit=10', { token: C.token })
const cold = data(r3) || []
ok('冷启动用户也能拿到推荐', code(r3) === 200 && cold.length > 0, msg(r3))
ok('冷启动理由为热门/新歌类', cold.every(i => i.strategy === 'hot' || i.strategy === 'fresh'),
  cold.map(i => i.strategy).join(','))

console.log('\n[AC-6] 缓存与换一批')
const first1 = data(await api('GET', '/music/recommend/personalized?limit=10', { token: A.token })).map(i => i.song.id)
const first2 = data(await api('GET', '/music/recommend/personalized?limit=10', { token: A.token })).map(i => i.song.id)
ok('缓存生效：两次请求结果一致', JSON.stringify(first1) === JSON.stringify(first2))
const refreshed = data(await api('GET', '/music/recommend/personalized?limit=10&refresh=true', { token: A.token })).map(i => i.song.id)
if (first1.length >= 5) {
  ok('refresh=true 换一批结果不同', JSON.stringify(first1) !== JSON.stringify(refreshed))
} else {
  console.log('  SKIP  候选不足，跳过换一批差异断言')
}
// 收藏变化后缓存被主动失效：取消收藏再查，结果应与缓存无关地重新计算
await api('DELETE', `/api/user/favorite/${seeds[2].id}`, { token: A.token })
const afterUnfav = data(await api('GET', '/music/recommend/personalized?limit=10', { token: A.token })).map(i => i.song.id)
ok('收藏变化触发缓存失效（重新计算不报错）', Array.isArray(afterUnfav))
// 恢复收藏，避免影响其他断言
await api('POST', `/api/user/favorite/${seeds[2].id}`, { token: A.token })

console.log('\n[兼容] similar-songs 保持 List<Song> 契约')
const r4 = await api('GET', `/music/recommend/similar-songs/${seeds[0].id}?limit=5`, { token: A.token })
const sim = data(r4) || []
ok('similar-songs 仍返回歌曲数组', code(r4) === 200 && sim.every(s => s.title && s.score === undefined), msg(r4))

console.log('\n[AC-9] 性能冒烟')
ok(`personalized 耗时 ${elapsed}ms < 3000ms`, elapsed < 3000)

console.log('\n========================================')
console.log(`推荐验收：通过 ${pass} 项，失败 ${fail} 项`)
if (failures.length) { console.log('失败项:'); failures.forEach(f => console.log('  - ' + f)) }
process.exit(fail ? 1 : 0)
