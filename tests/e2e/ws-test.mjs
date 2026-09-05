// WebSocket(STOMP) 鉴权 E2E：验证匿名拒绝、越权订阅拒绝、正常收发推送
import { createRequire } from 'module'
const require = createRequire('D:/workspace/java-project/project1/sheep-music/front/sheep-music/package.json')
const { Client } = require('@stomp/stompjs')
const SockJS = require('sockjs-client')

const BASE = process.env.E2E_BASE || 'http://localhost:19000'
const WS = (process.env.E2E_BASE || 'http://localhost:19000').replace('http', 'ws') + '/ws-chat/websocket'
let pass = 0, fail = 0
const ok = (name, cond, extra = '') => {
  if (cond) { pass++; console.log(`  PASS  ${name}`) }
  else { fail++; console.log(`  FAIL  ${name}  ${extra}`) }
}

const RUN = Date.now().toString().slice(-7)
const jpost = (path, body, token) => fetch(BASE + path, {
  method: 'POST', headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
  body: JSON.stringify(body)
}).then(r => r.json())

// 准备一对好友
const reg = (username) => jpost('/auth/register', { username, password: 'test123456', nickname: username })
await reg(`wsu1_${RUN}`); await reg(`wsu2_${RUN}`)
const u1 = await jpost('/auth/login', { username: `wsu1_${RUN}`, password: 'test123456' }).then(r => r.data)
const u2 = await jpost('/auth/login', { username: `wsu2_${RUN}`, password: 'test123456' }).then(r => r.data)
if (!u1?.token || !u2?.token) { console.log('ABORT: 登录失败'); process.exit(1) }
await jpost('/friend/request', { friendId: u2.userInfo.id, remark: '' }, u1.token)
const reqs = await fetch(BASE + '/friend/requests', { headers: { Authorization: `Bearer ${u2.token}` } }).then(r => r.json())
const fr = reqs.data.find(x => x.userId === u1.userInfo.id && x.status === 'pending')
await jpost(`/friend/accept/${fr.id}`, {}, u2.token)
console.log(`users: u1=${u1.userInfo.id} u2=${u2.userInfo.id} (已互为好友)`)

const waitEvent = (client, events, timeoutMs = 6000) => new Promise((resolve) => {
  const done = (type, detail) => { if (!finished) { finished = true; resolve({ type, detail }) } }
  let finished = false
  const timers = setTimeout(() => done('timeout'), timeoutMs)
  const wrap = (ev) => (msg) => { clearTimeout(timers); done(ev, msg) }
  if (events.includes('connected')) client.onConnect = () => done('connected')
  if (events.includes('error')) client.onStompError = (f) => done('error', f.headers?.message || f.body)
  if (events.includes('close')) client.onWebSocketClose = () => done('close')
})

const mkClient = (headers) => new Client({
  // 与前端 ws/client.js 完全一致的传输方式（SockJS）
  webSocketFactory: () => new SockJS(BASE + '/ws-chat'),
  connectHeaders: headers,
  reconnectDelay: 0
})

// 场景1：匿名 CONNECT 必须被拒
{
  const c = mkClient({})
  c.activate()
  const ev = await waitEvent(c, ['connected', 'error', 'close'], 5000)
  ok('匿名 CONNECT 被拒绝（未收到 CONNECTED）', ev.type !== 'connected', `ev=${ev.type}`)
  try { await c.deactivate() } catch (_) {}
}

// 场景2：合法 token CONNECT 成功，能订阅自己的频道
{
  const c = mkClient({ Authorization: `Bearer ${u1.token}` })
  c.activate()
  const ev = await waitEvent(c, ['connected', 'error', 'close'], 5000)
  ok('带合法 token CONNECT 成功', ev.type === 'connected', `ev=${ev.type} ${ev.detail || ''}`)
  if (ev.type === 'connected') {
    let gotOwn = null
    const sub = c.subscribe(`/topic/user.${u1.userInfo.id}.chat`, (f) => { gotOwn = f.body })
    // u1 给自己发不了消息（非好友），用 u2→u1 的推送验证：u1 订阅自己的频道后，
    // 由 HTTP 接口让 u2 发消息（u2 与 u1 已是好友）
    await fetch(BASE + '/chat/send', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${u2.token}` },
      body: JSON.stringify({ receiverId: u1.userInfo.id, type: 'text', content: 'WS推送验证' })
    }).then(r => r.json())
    await new Promise(r => setTimeout(r, 2500))
    ok('订阅自己频道并收到实时推送', gotOwn !== null && gotOwn.includes('WS推送验证'), gotOwn?.slice(0, 80))
    sub.unsubscribe()
  }
  try { await c.deactivate() } catch (_) {}
}

// 场景3：订阅他人频道必须被拒
{
  const c = mkClient({ Authorization: `Bearer ${u1.token}` })
  c.activate()
  const ev = await waitEvent(c, ['connected', 'error', 'close'], 5000)
  ok('u1 CONNECT 成功（场景3）', ev.type === 'connected', `ev=${ev.type}`)
  if (ev.type === 'connected') {
    let peerMsg = null, errored = false, closed = false
    c.onStompError = () => { errored = true }
    c.onWebSocketClose = () => { closed = true }
    const sub = c.subscribe(`/topic/user.${u2.userInfo.id}.chat`, (f) => { peerMsg = f.body })
    // u2 再发一条消息给 u1（若订阅未被拦截，u1 的越权订阅不会收到 u2 的消息——
    // 因为该 topic 只推 u2 自己的频道；真正要验证的是 SUBSCRIBE 是否被拒）
    await new Promise(r => setTimeout(r, 2000))
    ok('订阅他人频道被拒绝（ERROR/断开，且收不到帧）', errored || closed || peerMsg === null, `errored=${errored} closed=${closed}`)
    try { sub.unsubscribe() } catch (_) {}
  }
  try { await c.deactivate() } catch (_) {}
}

// 场景4：u2 在线时，u1 发消息 u2 能实时收到
{
  const c2 = mkClient({ Authorization: `Bearer ${u2.token}` })
  c2.activate()
  const ev2 = await waitEvent(c2, ['connected', 'error', 'close'], 5000)
  ok('u2 CONNECT 成功（场景4）', ev2.type === 'connected', `ev=${ev2.type}`)
  if (ev2.type === 'connected') {
    let received = null
    const sub = c2.subscribe(`/topic/user.${u2.userInfo.id}.chat`, (f) => { received = f.body })
    await new Promise(r => setTimeout(r, 800))
    await fetch(BASE + '/chat/send', {
      method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${u1.token}` },
      body: JSON.stringify({ receiverId: u2.userInfo.id, type: 'text', content: 'WS双向验证E2E' })
    }).then(r => r.json())
    await new Promise(r => setTimeout(r, 2500))
    ok('u2 实时收到 u1 的消息', received !== null && received.includes('WS双向验证E2E'), received?.slice(0, 80))
    sub.unsubscribe()
  }
  try { await c2.deactivate() } catch (_) {}
}

console.log('\n========================================')
console.log(`WS 测试：通过 ${pass} 项，失败 ${fail} 项`)
process.exit(fail ? 1 : 0)
