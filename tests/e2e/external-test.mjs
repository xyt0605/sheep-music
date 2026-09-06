// 曲库供应链 E2E（docs/specs/曲库供应链v1/，v1.3 起仅保留歌曲海聚合源）
// 匿名/参数校验类断言不访问外网；gequhai 搜索/歌词/音频为真实上游联测（网络可达时执行）。
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
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  let json = null
  try { json = await res.json() } catch (_) {}
  return { status: res.status, json }
}
const code = (r) => r.json?.code
const data = (r) => r.json?.data
const msg = (r) => r.json?.message || ''

// ============ AC-1 未登录 401 ============
console.log('\n[1] 鉴权（AC-1）')
let r = await api('GET', '/music/external/search?source=gequhai&keyword=love')
ok('未登录搜索外源返回 401', r.status === 401 && code(r) === 401, `status=${r.status} code=${code(r)}`)

// 登录一个测试用户
const RUN = Date.now().toString().slice(-7)
const NAME = `e2e_ext_${RUN}`
r = await api('POST', '/auth/register', { body: { username: NAME, password: 'test123456', nickname: 'ext昵称' } })
ok('注册测试用户', code(r) === 200, msg(r))
r = await api('POST', '/auth/login', { body: { username: NAME, password: 'test123456' } })
const U = data(r)?.token
ok('登录测试用户', code(r) === 200 && !!U, msg(r))

// ============ AC-3/AC-4 参数校验 ============
console.log('\n[2] 参数校验（AC-3/AC-4）')
r = await api('GET', '/music/external/search?source=gequhai', { token: U })
ok('缺 keyword 返回业务 400', code(r) === 400, `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', '/music/external/search?source=gequhai&keyword=%20%20', { token: U })
ok('空白 keyword 返回业务 400', code(r) === 400, `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', '/music/external/search?source=nonexist&keyword=love', { token: U })
ok('未知音源返回业务 400', code(r) === 400 && /不支持的音乐来源/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)

// ============ AC-5/AC-6 流代理（免登录 + 参数校验，不发外网请求） ============
console.log('\n[3] 流代理（AC-5/AC-6）')
r = await api('GET', '/music/external/stream?source=gequhai&trackId=abc')
ok('匿名访问 stream 不是 401（permitAll 生效）', r.status !== 401, `status=${r.status}`)
ok('非数字 trackId 返回 400', r.status === 400, `status=${r.status}`)
r = await api('GET', '/music/external/stream?source=nonexist&trackId=123')
ok('未知音源 stream 返回 400', r.status === 400, `status=${r.status}`)
r = await api('GET', '/music/external/stream?source=gequhai&trackId=1&fileId=../etc/passwd')
ok('fileId 路径穿越返回 400', r.status === 400, `status=${r.status}`)

// ============ AC-11 音源列表接口 ============
console.log('\n[4] 音源列表（AC-11）')
r = await api('GET', '/music/external/sources')
ok('未登录获取音源列表返回 401', r.status === 401, `status=${r.status}`)
r = await api('GET', '/music/external/sources', { token: U })
const srcList = data(r) || []
ok('音源列表 code=200 且仅歌曲海一个源',
  code(r) === 200 && Array.isArray(srcList) && srcList.length === 1 && srcList[0]?.source === 'gequhai',
  `sources=${srcList.map(s => s.source).join(',')}`)
ok('歌曲海源 enabled=true 且 openLicense=false（聚合试听标识）',
  srcList[0]?.enabled === true && srcList[0]?.openLicense === false)

// ============ AC-13 歌曲海聚合源（网络可达时真实联测） ============
console.log('\n[5] 歌曲海聚合源实测（AC-13，网络可达时）')
r = await api('GET', '/music/external/search?source=gequhai&keyword=' + encodeURIComponent('周杰伦') + '&page=0&size=10', { token: U })
ok('gequhai 搜索 code=200 且 enabled=true', code(r) === 200 && data(r)?.enabled === true, `code=${code(r)} msg=${msg(r)}`)
const gqItems = data(r)?.items || []
if (gqItems.length > 0) {
  const g1 = gqItems[0]
  ok('gequhai 字段齐全（标题/歌手）', !!g1.title && !!g1.artist, JSON.stringify(g1).slice(0, 120))
  ok('gequhai streamUrl 为同源代理且 trackId 为数字',
    /\/music\/external\/stream\?source=gequhai&trackId=\d+$/.test(g1.streamUrl || ''), `streamUrl=${g1?.streamUrl}`)
  const lrc = await api('GET', `/music/external/lyric?source=gequhai&trackId=${g1.sourceTrackId}`, { token: U })
  ok('gequhai 歌词返回 LRC 文本', code(lrc) === 200 && /\[\d{2}:\d{2}/.test(data(lrc) || ''), `len=${(data(lrc) || '').length}`)
  const gres = await fetch(BASE + g1.streamUrl.replace(/^\/api/, ''), { headers: { Range: 'bytes=0-1023' } })
  ok('gequhai 流代理真实音频 206/200', gres.status === 206 || gres.status === 200, `status=${gres.status}`)
  ok('gequhai Content-Type 为音频', /audio\//.test(gres.headers.get('content-type') || ''), gres.headers.get('content-type'))
  await gres.body?.cancel().catch(() => {})
} else {
  console.log('  SKIP  上游无结果或网络不可达（message=' + (data(r)?.message || '无') + '），跳过联测断言')
}

// ============ 清理 ============
console.log('\n[清理]')
// 该用户只调过搜索，无资源残留（用户本身留在测试库，与其他套件同策略）

console.log(`\n========== 结果: ${pass} pass / ${fail} fail ==========`)
if (failures.length) { console.log('失败项:\n - ' + failures.join('\n - ')); process.exit(1) }
