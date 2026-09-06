// 小屋 DJ E2E（docs/specs/agent-v1/，P3 BYOK：按用户配置密钥）
// 流程：鉴权 → 未配置降级（确定性，不依赖外网）→ 配置密钥（AGENT_API_KEY 环境变量作为真实密钥）
//       → 连接测试 → 真实 LLM 两轮（事件协议 + 防幻觉字段 + 会话记忆 AC-15）→ 清除配置回未配置态
// LLM 输出非确定性：断言聚焦事件协议与数据形态，不断言具体歌名。
const BASE = process.env.E2E_BASE || 'http://localhost:19000'
const REAL_KEY = process.env.AGENT_API_KEY || ''
const HAS_KEY = !!REAL_KEY
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

const readSse = async (path, token, body, budgetMs = 180000) => {
  const events = []
  const ctrl = new AbortController()
  const timer = setTimeout(() => ctrl.abort(), budgetMs)
  try {
    const res = await fetch(BASE + path, {
      method: 'POST',
      signal: ctrl.signal,
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body)
    })
    if (!res.ok || !res.body) return { events, httpStatus: res.status }
    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buf = ''
    outer: for (;;) {
      const { value, done } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      let sep
      while ((sep = buf.indexOf('\n\n')) >= 0) {
        const chunk = buf.slice(0, sep)
        buf = buf.slice(sep + 2)
        let event = 'message'
        const dataLines = []
        for (const line of chunk.split('\n')) {
          if (line.startsWith('event:')) event = line.slice(6).trim()
          else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
        }
        if (!dataLines.length) continue
        let data = null
        try { data = JSON.parse(dataLines.join('\n')) } catch (_) { data = { raw: dataLines.join('\n') } }
        events.push({ event, data })
        if (event === 'done' || event === 'error') break outer
      }
    }
  } catch (e) {
    return { events, error: String(e).slice(0, 120) }
  } finally {
    clearTimeout(timer)
  }
  return { events }
}

// ============ AC-1 鉴权 ============
console.log('\n[1] 鉴权（AC-1）')
let r = await api('POST', '/agent/dj/stream', { body: { query: '下雨天' } })
ok('未登录调用返回 401', r.status === 401, `status=${r.status}`)
r = await api('GET', '/agent/config')
ok('未登录查询配置返回 401', r.status === 401, `status=${r.status}`)

// 登录
const RUN = Date.now().toString().slice(-7)
const NAME = `e2e_dj_${RUN}`
r = await api('POST', '/auth/register', { body: { username: NAME, password: 'test123456', nickname: 'dj昵称' } })
ok('注册测试用户', r.json?.code === 200, r.json?.message || '')
r = await api('POST', '/auth/login', { body: { username: NAME, password: 'test123456' } })
const U = r.json?.data?.token
ok('登录测试用户', r.json?.code === 200 && !!U)

// ============ AC-17 未配置密钥降级（确定性，不依赖外网） ============
console.log('\n[2] 未配置密钥降级（AC-17）')
r = await api('GET', '/agent/config', { token: U })
ok('新用户 configured=false', code(r) === 200 && data(r)?.configured === false, JSON.stringify(data(r)).slice(0, 100))
const { events: preEvents } = await readSse('/agent/dj/stream', U, { query: '来点适合下雨天听的歌' })
const preErr = preEvents.find(e => e.event === 'error')
ok('未配置：error 事件含"还没有配置密钥"', !!preErr && /还没有配置密钥/.test(preErr.data?.message || ''), preErr?.data?.message)
ok('未配置：不产出 song_card（不允许幻觉兜底）', !preEvents.some(e => e.event === 'song_card'))

// ============ AC-18/19 配置密钥 + 连接测试 ============
console.log('\n[3] 配置密钥（AC-18/19）')
if (!HAS_KEY) {
  console.log('  SKIP  未提供 AGENT_API_KEY，跳过配置与真实 LLM 断言')
  console.log(`\n========== 结果: ${pass} pass / ${fail} fail ==========`)
  if (failures.length) { console.log('失败项:\n - ' + failures.join('\n - ')); process.exit(1) }
  process.exit(0)
}
r = await api('POST', '/agent/config', { token: U, body: { apiKey: REAL_KEY, model: process.env.AGENT_MODEL || 'glm-4.5-air' } })
ok('保存配置成功', code(r) === 200 && data(r)?.configured === true, `code=${code(r)} msg=${msg(r)}`)
ok('key 脱敏返回（不回传明文）', !!data(r)?.apiKeyMasked && !(JSON.stringify(data(r)).includes(REAL_KEY)))
r = await api('GET', '/agent/config', { token: U })
ok('GET 配置：configured=true + model 正确', data(r)?.configured === true && !!data(r)?.model, JSON.stringify(data(r)).slice(0, 120))

console.log('\n[4] 连接测试（AC-19）')
r = await api('POST', '/agent/config/test', { token: U, body: {} })
ok('测试连接 ok=true（用已保存配置）', code(r) === 200 && data(r)?.ok === true, JSON.stringify(data(r)).slice(0, 140))
r = await api('POST', '/agent/config/test', { token: U, body: { apiKey: 'invalid-key-for-test' } })
ok('错误密钥测试 ok=false', code(r) === 200 && data(r)?.ok === false, JSON.stringify(data(r)).slice(0, 140))

// 实时模型列表（有已保存配置即可）
r = await api('GET', '/agent/config/models?baseUrl=https%3A%2F%2Fopen.bigmodel.cn%2Fapi%2Fpaas%2Fv4', { token: U })
ok('模型列表接口返回（supported 判断正确）', code(r) === 200 && typeof data(r)?.supported === 'boolean', JSON.stringify(data(r)).slice(0, 120))
if (data(r)?.supported) {
  ok('智谱实时模型列表非空且含 glm 系', data(r).models.length > 0 && data(r).models.some(m => m.startsWith('glm')), JSON.stringify(data(r).models.slice(0, 5)))
}

// ============ AC-2/3 真实 DJ 全流程 ============
console.log('\n[5] DJ 全流程（AC-2/3，真实 LLM）')
const { events, error: sseErr } = await readSse('/agent/dj/stream', U, { query: '来点适合下雨天听的歌' })
if (sseErr) console.log('  SSE 错误:', sseErr)
const counts = events.reduce((m, e) => { m[e.event] = (m[e.event] || 0) + 1; return m }, {})
console.log('  事件统计:', JSON.stringify(counts))
const errEv = events.find(e => e.event === 'error')
ok('无 error 事件', !errEv, errEv?.data?.message || '')
ok('出现 stage 事件（多 Agent 阶段播报）', (counts.stage || 0) >= 3, `stage=${counts.stage || 0}`)
ok('出现 ReAct 过程事件', (counts.thought || 0) > 0 && (counts.action || 0) > 0)
ok('出现流式串场词（text_delta）', (counts.text_delta || 0) > 0)
ok('以 done 事件收尾', (counts.done || 0) >= 1)
const sessEv = events.find(e => e.event === 'session')
ok('返回 session 事件', !!sessEv && !!sessEv.data?.sessionId)
const SESSION_ID = sessEv?.data?.sessionId

const cards = events.filter(e => e.event === 'song_card').map(e => e.data)
ok('产出 song_card（≥1 张，结构合法）', cards.length >= 1, `cards=${cards.length}`)
if (cards.length) {
  ok('卡片标题/歌手非空', cards.every(c => c.title && c.artist))
  ok('本地卡片带 songId、外源卡片带 streamUrl（防幻觉）',
    cards.every(c => c.source === 'local' ? !!c.songId
      : /\/music\/external\/stream\?source=gequhai&trackId=\d+$/.test(c.streamUrl || '')))
  const reasonCards = cards.filter(c => c.reason)
  console.log(`  NOTE  逐首 reason 覆盖 ${reasonCards.length}/${cards.length}`)
}

// ============ AC-15 会话记忆 ============
console.log('\n[6] 会话记忆（AC-15，真实 LLM）')
if (cards.length === 0) {
  console.log('  SKIP  首轮无卡片，跳过会话断言')
} else {
  const r2 = await readSse('/agent/dj/stream', U, { query: '换成周杰伦的，多来几首', sessionId: SESSION_ID })
  const c2 = r2.events.reduce((m, e) => { m[e.event] = (m[e.event] || 0) + 1; return m }, {})
  console.log('  事件统计:', JSON.stringify(c2))
  const err2 = r2.events.find(e => e.event === 'error')
  ok('第二轮无 error 事件', !err2, err2?.data?.message || '')
  const cards2 = r2.events.filter(e => e.event === 'song_card').map(e => e.data)
  ok('第二轮产出卡片', cards2.length >= 1, `cards=${cards2.length}`)
  const jj = cards2.filter(c => `${c.title}${c.artist}`.includes('周杰伦'))
  ok('第二轮命中周杰伦（会话上下文生效）', jj.length >= 1, `周杰伦 ${jj.length}/${cards2.length}`)
  const sess2 = r2.events.find(e => e.event === 'session')
  ok('第二轮返回同一 sessionId', sess2?.data?.sessionId === SESSION_ID, sess2?.data?.sessionId)
}

// ============ 清理：回到未配置态（幂等可重跑） ============
console.log('\n[清理]')
r = await api('DELETE', '/agent/config', { token: U })
ok('清除配置成功', code(r) === 200 && data(r)?.configured === false)
r = await api('GET', '/agent/config', { token: U })
ok('清除后 configured=false', data(r)?.configured === false)

console.log(`\n========== 结果: ${pass} pass / ${fail} fail ==========`)
if (failures.length) { console.log('失败项:\n - ' + failures.join('\n - ')); process.exit(1) }
