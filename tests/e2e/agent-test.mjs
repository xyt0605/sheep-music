// 小屋 DJ E2E（docs/specs/agent-v1/）
// 后端未配 AGENT_API_KEY：断言优雅降级（error 事件含配置指引）；
// 配置了密钥：真实 LLM 全流程冒烟（stage/thought/text_delta/song_card/done）。
// LLM 输出具有非确定性，断言聚焦事件协议与数据形态，不断言具体歌名。
const BASE = process.env.E2E_BASE || 'http://localhost:19000'
const HAS_KEY = !!process.env.AGENT_API_KEY
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

// SSE 读取（fetch 流式），返回 {events: [{event, data}], error}
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

// 登录
const RUN = Date.now().toString().slice(-7)
const NAME = `e2e_dj_${RUN}`
r = await api('POST', '/auth/register', { body: { username: NAME, password: 'test123456', nickname: 'dj昵称' } })
ok('注册测试用户', r.json?.code === 200, r.json?.message || '')
r = await api('POST', '/auth/login', { body: { username: NAME, password: 'test123456' } })
const U = r.json?.data?.token
ok('登录测试用户', r.json?.code === 200 && !!U)

// ============ AC-1 降级 / AC-2/3 真实流程 ============
console.log(`\n[2] DJ 对话（${HAS_KEY ? '真实 LLM 全流程' : '无密钥降级'}）`)
const { events } = await readSse('/agent/dj/stream', U, { query: '来点适合下雨天听的歌' })
const byEvent = Object.fromEntries(events.map(e => [e.event])).constructor === Object
const counts = events.reduce((m, e) => { m[e.event] = (m[e.event] || 0) + 1; return m }, {})
console.log('  事件统计:', JSON.stringify(counts))

if (!HAS_KEY) {
  const errEv = events.find(e => e.event === 'error')
  ok('无密钥：返回 error 事件（优雅降级）', !!errEv && /AGENT_API_KEY|大模型/.test(errEv.data?.message || ''), errEv?.data?.message)
} else {
  const errEv = events.find(e => e.event === 'error')
  ok('无 error 事件（有密钥时应完整执行）', !errEv, errEv?.data?.message || '')
  ok('出现 stage 事件（多 Agent 阶段播报）', (counts.stage || 0) >= 3, `stage=${counts.stage || 0}`)
  ok('出现 ReAct 过程事件（thought/action/observation）',
    (counts.thought || 0) > 0 && (counts.action || 0) > 0 && (counts.observation || 0) > 0)
  ok('出现流式串场词（text_delta）', (counts.text_delta || 0) > 0)
  ok('以 done 事件收尾', (counts.done || 0) >= 1)

  const cards = events.filter(e => e.event === 'song_card').map(e => e.data)
  // LLM 与上游网站的联合非确定性：协议断言为 ≥1 张结构合法卡片；≥3 为理想体验（打印观察）
  ok('产出 song_card（≥1 张，结构合法）', cards.length >= 1, `cards=${cards.length}`)
  if (cards.length < 3) console.log(`  NOTE  本轮仅 ${cards.length} 张卡片（上游波动属正常，理想 ≥3）`)
  if (cards.length) {
    ok('卡片标题/歌手非空', cards.every(c => c.title && c.artist))
    ok('本地卡片带 songId、外源卡片带 streamUrl（防幻觉：字段来自工具数据）',
      cards.every(c => c.source === 'local' ? !!c.songId
        : /\/music\/external\/stream\?source=gequhai&trackId=\d+$/.test(c.streamUrl || '')))
    ok('存在歌曲海来源卡片（联网检索生效）', cards.some(c => c.source === 'gequhai'))

    // 真实可播性抽查：部分歌曲海曲目是网盘专属（无在线音源，上游 502），尝试前 3 张外源卡
    const webCards = cards.filter(c => c.source === 'gequhai')
    if (webCards.length) {
      let playable = false, lastStatus = 0
      for (const webCard of webCards.slice(0, 3)) {
        const res = await fetch(BASE + webCard.streamUrl.replace(/^\/api/, ''), { headers: { Range: 'bytes=0-1023' } })
        lastStatus = res.status
        if (res.status === 206 || res.status === 200) { playable = true; await res.body?.cancel().catch(() => {}); break }
      }
      ok('外源卡片音频流可播（206/200，前 3 张外源卡至少一张可播）', playable, `lastStatus=${lastStatus}`)
    }
  }
}

console.log(`\n========== 结果: ${pass} pass / ${fail} fail ==========`)
if (failures.length) { console.log('失败项:\n - ' + failures.join('\n - ')); process.exit(1) }
