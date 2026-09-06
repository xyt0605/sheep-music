// 曲库供应链 v1 E2E（docs/specs/曲库供应链v1/01-需求规格.md §6 AC-1~6）
// 无 JAMENDO_CLIENT_ID 环境：验证降级/鉴权/参数校验（不访问外网）；
// 配置了 JAMENDO_CLIENT_ID：追加真实上游联测（AC-10 冒烟）。
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
let r = await api('GET', '/music/external/search?source=jamendo&keyword=love')
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
r = await api('GET', '/music/external/search?source=jamendo', { token: U })
ok('缺 keyword 返回业务 400', code(r) === 400, `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', '/music/external/search?source=jamendo&keyword=%20%20', { token: U })
ok('空白 keyword 返回业务 400', code(r) === 400, `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', '/music/external/search?source=nonexist&keyword=love', { token: U })
ok('未知音源返回业务 400', code(r) === 400 && /不支持的音乐来源/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)

// ============ AC-2 未配置密钥优雅降级 ============
console.log('\n[3] 音源降级（AC-2）')
r = await api('GET', '/music/external/search?source=jamendo&keyword=love&page=0&size=20', { token: U })
if (process.env.JAMENDO_CLIENT_ID) {
  ok('已配置密钥：搜索 code=200', code(r) === 200, `code=${code(r)} msg=${msg(r)}`)
  ok('已配置密钥：enabled=true', data(r)?.enabled === true)
  ok('已配置密钥：返回结果列表', Array.isArray(data(r)?.items), `total=${data(r)?.total}`)
  const first = data(r)?.items?.[0]
  ok('已配置密钥：字段齐全（streamUrl 为同源代理）',
    !!first && typeof first.sourceTrackId !== 'undefined'
    && /\/music\/external\/stream\?source=jamendo&trackId=\d+$/.test(first.streamUrl || ''),
    `streamUrl=${first?.streamUrl}`)
  ok('已配置密钥：携带授权信息', !!first?.licenseName || !!first?.licenseUrl)
} else {
  ok('未配置密钥：code=200 不报错', code(r) === 200, `code=${code(r)} msg=${msg(r)}`)
  ok('未配置密钥：enabled=false', data(r)?.enabled === false, `enabled=${data(r)?.enabled}`)
  ok('未配置密钥：items 为空数组', Array.isArray(data(r)?.items) && data(r).items.length === 0)
  ok('未配置密钥：message 含配置指引', /JAMENDO_CLIENT_ID/.test(msg(r) + (data(r)?.message || '')), `msg=${msg(r)} data.message=${data(r)?.message}`)
  ok('响应结构含 source/sourceLabel', data(r)?.source === 'jamendo' && !!data(r)?.sourceLabel)
}

// ============ AC-5/AC-6 流代理（免登录 + 参数校验，不发外网请求） ============
console.log('\n[4] 流代理（AC-5/AC-6）')
r = await api('GET', '/music/external/stream?source=jamendo&trackId=abc')
ok('匿名访问 stream 不是 401（permitAll 生效）', r.status !== 401, `status=${r.status}`)
ok('非数字 trackId 返回 400', r.status === 400, `status=${r.status}`)
r = await api('GET', '/music/external/stream?source=nonexist&trackId=123')
ok('未知音源 stream 返回 400', r.status === 400, `status=${r.status}`)

// ============ 可选：真实上游联测（需外网 + 密钥，AC-10 冒烟） ============
if (process.env.JAMENDO_CLIENT_ID && data(await api('GET', '/music/external/search?source=jamendo&keyword=love', { token: U }))?.items?.length > 0) {
  console.log('\n[5] 真实上游联测（AC-10 冒烟）')
  const first = data(await api('GET', '/music/external/search?source=jamendo&keyword=love', { token: U })).items[0]
  const res = await fetch(BASE + first.streamUrl.replace(/^\/api/, ''), { headers: { Range: 'bytes=0-1023' } })
  ok('stream 真实音频 206/200', res.status === 206 || res.status === 200, `status=${res.status}`)
  ok('stream Content-Type 为音频', /audio\//.test(res.headers.get('content-type') || ''), res.headers.get('content-type'))
  ok('stream 支持 Range（Content-Range 存在）', res.status === 206 ? !!res.headers.get('content-range') : true)
  await res.body?.cancel().catch(() => {})
}

// ============ 清理 ============
console.log('\n[清理]')
// 该用户只调过搜索，无资源残留（用户本身留在测试库，与其他套件同策略）

console.log(`\n========== 结果: ${pass} pass / ${fail} fail ==========`)
if (failures.length) { console.log('失败项:\n - ' + failures.join('\n - ')); process.exit(1) }
