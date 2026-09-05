// Sheep Music E2E API 测试（Node 22+，global fetch）
const BASE = process.env.E2E_BASE || 'http://localhost:9000'
let pass = 0, fail = 0
const failures = []

const ok = (name, cond, extra = '') => {
  if (cond) { pass++; console.log(`  PASS  ${name}`) }
  else { fail++; failures.push(name); console.log(`  FAIL  ${name}  ${extra}`) }
}

const api = async (method, path, { token, body, raw } = {}) => {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  let payload
  if (body instanceof FormData) { payload = body }
  else if (body !== undefined) { headers['Content-Type'] = 'application/json'; payload = JSON.stringify(body) }
  const res = await fetch(BASE + path, { method, headers, body: payload })
  if (raw) return { status: res.status, json: null }
  let json = null
  try { json = await res.json() } catch (_) {}
  return { status: res.status, json }
}
const code = (r) => r.json?.code
const data = (r) => r.json?.data
const msg = (r) => r.json?.message || ''

const register = async (username) =>
  api('POST', '/auth/register', { body: { username, password: 'test123456', nickname: username + '昵称' } })
const login = async (username, password = 'test123456') =>
  api('POST', '/auth/login', { body: { username, password } })

// ============ 1. 认证与注册幂等 ============
console.log('\n[1] 认证 / 注册幂等')
const RUN = Date.now().toString().slice(-7)
const NAME1 = `e2e_u1_${RUN}`, NAME2 = `e2e_u2_${RUN}`, NAME3 = `e2e_u3_${RUN}`
let r = await register(NAME1)
ok('注册新用户成功', code(r) === 200, msg(r))
ok('注册响应不泄漏 password 字段', data(r) && data(r).password === undefined && data(r).username === NAME1)

r = await register(NAME1)
ok('重复注册用户名被拒（幂等提示）', code(r) !== 200 && /已存在/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
r = await register(NAME2)
ok('注册第二个用户成功', code(r) === 200, msg(r))
r = await register(NAME3)
ok('注册第三个用户成功', code(r) === 200, msg(r))

r = await login(NAME1)
ok('登录 e2e_user1 成功', code(r) === 200 && !!data(r)?.token, msg(r))
const U1 = data(r)?.token
const u1id = data(r)?.userInfo?.id
r = await login(NAME2)
const U2 = data(r)?.token; const u2id = data(r)?.userInfo?.id
ok('登录 e2e_user2 成功', code(r) === 200 && !!U2, msg(r))
r = await login(NAME3)
const U3 = data(r)?.token; const u3id = data(r)?.userInfo?.id
ok('登录 e2e_user3 成功', code(r) === 200 && !!U3, msg(r))
ok('登录 userInfo 不含 password', data(r)?.userInfo?.password === undefined)
r = await login(NAME1, 'wrong-password')
ok('错误密码登录被拒', code(r) !== 200, msg(r))

// ============ 2. 浏览歌曲（公开/需登录） ============
console.log('\n[2] 歌曲浏览')
r = await api('GET', '/music/hot?page=0&size=10', { token: U1 })
if (!(code(r) === 200 && data(r)?.content?.length > 0)) { console.log('  ABORT: 热门歌曲不可用，后续用例无意义'); process.exit(1) }
ok('热门歌曲列表（登录后）', true)
const hotSong = data(r)?.content?.[0]
ok('热门歌曲含标题/歌手', !!hotSong?.title)
r = await api('GET', '/music/new?page=0&size=5', { token: U1 })
ok('新歌速递列表', code(r) === 200 && data(r)?.content?.length > 0, msg(r))
r = await api('GET', '/music/search?keyword=%E7%88%B1&page=0&size=5', { token: U1 })
ok('搜索歌曲', code(r) === 200 && data(r) !== undefined, msg(r))
r = await api('GET', `/music/song/${hotSong.id}`, { token: U1 })
ok('歌曲详情', code(r) === 200 && data(r)?.id === hotSong.id, msg(r))
r = await api('POST', `/music/play/${hotSong.id}`, { token: U1 })
ok('记录歌曲播放', code(r) === 200, msg(r))
r = await api('GET', '/music/artists?page=0&size=5', { token: U1 })
ok('歌手列表', code(r) === 200, msg(r))
const artistId = data(r)?.content?.[0]?.id
r = await api('GET', `/music/artist/${artistId}/songs`, { token: U1 })
ok('按歌手查歌曲', code(r) === 200, msg(r))
r = await api('GET', `/music/song/${hotSong.id}/lyric`, { token: U1 })
ok('获取歌词', code(r) === 200, msg(r))

// ============ 3. 收藏 ============
console.log('\n[3] 收藏')
r = await api('POST', `/api/user/favorite/${hotSong.id}`, { token: U1 })
ok('收藏歌曲', code(r) === 200, msg(r))
r = await api('GET', `/api/user/favorite/check/${hotSong.id}`, { token: U1 })
ok('检查收藏状态=已收藏', data(r)?.isFavorite === true, JSON.stringify(data(r)))
r = await api('POST', '/api/user/favorite/batch-check', { token: U1, body: [hotSong.id] })
ok('批量检查收藏', code(r) === 200 && data(r) !== undefined, msg(r))
r = await api('GET', '/api/user/favorite/list?page=0&size=10', { token: U1 })
ok('收藏列表包含该歌', code(r) === 200 && data(r)?.content?.some(f => f.song?.id === hotSong.id), msg(r))
r = await api('GET', '/api/user/favorite/count', { token: U1 })
ok('收藏计数=1', data(r)?.count === 1, JSON.stringify(data(r)))

// ============ 4. 播放历史 ============
console.log('\n[4] 播放历史')
r = await api('POST', '/api/user/play-history', { token: U1, body: { songId: hotSong.id } })
ok('记录播放历史', code(r) === 200, msg(r))
r = await api('GET', '/api/user/play-history/list?page=0&size=10', { token: U1 })
ok('播放历史包含该歌', code(r) === 200 && data(r)?.content?.some(h => h.song?.id === hotSong.id), msg(r))
r = await api('GET', '/api/user/play-history/count', { token: U1 })
ok('播放历史计数', code(r) === 200 && data(r)?.count >= 1, JSON.stringify(data(r)))

// ============ 5. 歌单（含私有可见性） ============
console.log('\n[5] 歌单')
r = await api('POST', '/api/playlist/create', { token: U1, body: { name: 'E2E私有歌单', description: '测试', isPublic: false } })
ok('创建私有歌单', code(r) === 200 && !!data(r)?.id, msg(r))
const plId = data(r)?.id
r = await api('POST', `/api/playlist/${plId}/songs/${hotSong.id}`, { token: U1 })
ok('添加歌曲到歌单', code(r) === 200, msg(r))
r = await api('GET', `/api/playlist/${plId}`, { token: U1 })
ok('属主可看私有歌单', code(r) === 200 && data(r)?.id === plId, msg(r))
r = await api('GET', `/api/playlist/${plId}`, { token: U2 })
ok('他人访问私有歌单被拒', code(r) !== 200 && /无权查看/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', `/api/playlist/${plId}/songs`, { token: U2 })
ok('他人访问私有歌单歌曲被拒', code(r) !== 200 && /无权查看/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
r = await api('POST', `/api/playlist/${plId}/play`, { token: U2 })
ok('歌单播放计数接口', code(r) === 200, msg(r))
r = await api('POST', `/api/playlist/${plId}/visibility?isPublic=true`, { token: U1 })
ok('设为公开', code(r) === 200, msg(r))
r = await api('GET', `/api/playlist/${plId}`, { token: U2 })
ok('公开后他人可看', code(r) === 200, msg(r))
r = await api('GET', '/api/playlist/square?page=0&size=20', { token: U2 })
ok('歌单广场包含该歌单', code(r) === 200 && data(r)?.content?.some(p => p.id === plId), msg(r))
r = await api('GET', '/api/playlist/my?page=0&size=10', { token: U1 })
ok('我的歌单列表', code(r) === 200 && data(r)?.content?.some(p => p.id === plId), msg(r))
r = await api('PUT', `/api/playlist/${plId}`, { token: U2, body: { name: '黑客改名' } })
ok('他人改歌单被拒', code(r) !== 200 && /无权限/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)

// ============ 6. 评论（点赞原子性 + 级联删除） ============
console.log('\n[6] 评论')
r = await api('POST', '/comment', { token: U1, body: { songId: hotSong.id, content: 'E2E_ROOT_CASCADE 根评论', rating: 5 } })
ok('发表评论', code(r) === 200 && !!data(r)?.id, msg(r))
const rootCommentId = data(r)?.id
r = await api('POST', '/comment', { token: U2, body: { songId: hotSong.id, content: 'E2E回复', parentId: rootCommentId } })
ok('回复评论', code(r) === 200, msg(r))
const replyId = data(r)?.id
r = await api('GET', `/comment/${rootCommentId}/replies`, { token: U1 })
ok('查询回复列表=1条', code(r) === 200 && Array.isArray(data(r)) && data(r).length === 1, JSON.stringify(data(r)))
r = await api('POST', `/comment/${rootCommentId}/like`, { token: U2 })
ok('u2点赞评论', code(r) === 200 && data(r) === true, msg(r))
r = await api('GET', `/comment/${rootCommentId}/liked`, { token: U2 })
ok('点赞状态查询', data(r)?.data === true || data(r) === true, JSON.stringify(data(r)))
r = await api('GET', `/comment/song/${hotSong.id}?page=0&size=10`, { token: U1 })
const rc = data(r)?.content?.find(c => c.id === rootCommentId)
ok('评论点赞数=1（原子更新）', rc?.likeCount === 1, JSON.stringify(rc?.likeCount))
r = await api('POST', `/comment/${rootCommentId}/like`, { token: U2 })
ok('重复点赞变为取消', data(r) === false, msg(r))
r = await api('GET', `/comment/song/${hotSong.id}?page=0&size=10`, { token: U1 })
ok('取消后点赞数=0', data(r)?.content?.find(c => c.id === rootCommentId)?.likeCount === 0)
r = await api('POST', `/comment/${rootCommentId}/like`, { token: U2 })
r = await api('DELETE', `/comment/${rootCommentId}`, { token: U2 })
ok('非本人删除评论被拒', code(r) !== 200 && /无权/.test(msg(r)), msg(r))
r = await api('DELETE', `/comment/${rootCommentId}`, { token: U1 })
ok('本人删除根评论', code(r) === 200, msg(r))
r = await api('GET', '/comment/count/' + hotSong.id, { token: U1 })
ok('删除后根评论计数=0（级联删了根）', data(r) === 0, JSON.stringify(data(r)))
// 级联删除的 DB 校验（回复与点赞是否一并删除）放在脚本后用 SQL 验证，回复ID记入输出
console.log(`  INFO  cascade-check ids: root=${rootCommentId} reply=${replyId} song=${hotSong.id}`)

// ============ 7. 动态（可见性） ============
console.log('\n[7] 动态')
r = await api('POST', '/moment', { token: U1, body: { type: 'text', content: 'E2E私密动态X1', visibility: 'private' } })
ok('发布私密动态', code(r) === 200 && !!data(r)?.id, msg(r))
const privMomentId = data(r)?.id
r = await api('POST', '/moment', { token: U1, body: { type: 'text', content: 'E2E公开动态X2', visibility: 'public' } })
const pubMomentId = data(r)?.id
ok('发布公开动态', code(r) === 200 && !!pubMomentId, msg(r))
r = await api('GET', `/moment/user/${u1id}?page=0&size=20`, { token: U1 })
ok('本人可见自己的私密动态', code(r) === 200 && data(r)?.content?.some(m => m.id === privMomentId), msg(r))
r = await api('GET', `/moment/user/${u1id}?page=0&size=20`, { token: U2 })
ok('他人不可见私密动态（可见性修复）', code(r) === 200 && !data(r)?.content?.some(m => m.id === privMomentId), msg(r))
r = await api('GET', `/moment/user/${u1id}?page=0&size=20`, { token: U2 })
ok('他人可见公开动态', data(r)?.content?.some(m => m.id === pubMomentId), msg(r))
r = await api('GET', '/moment/public?page=0&size=20', { token: U2 })
ok('公开广场含该动态', code(r) === 200 && data(r)?.content?.some(m => m.id === pubMomentId), msg(r))
r = await api('POST', `/moment/${pubMomentId}/like`, { token: U2 })
ok('u2点赞动态', code(r) === 200 && data(r) === true, msg(r))
r = await api('GET', '/moment/public?page=0&size=20', { token: U2 })
ok('动态点赞数=1', data(r)?.content?.find(m => m.id === pubMomentId)?.likeCount === 1)
r = await api('POST', '/moment/comment', { token: U2, body: { momentId: pubMomentId, content: 'E2E动态评论' } })
ok('u2评论动态', code(r) === 200, msg(r))
r = await api('GET', `/moment/${pubMomentId}/comments?page=0&size=10`, { token: U2 })
ok('动态评论数=1', data(r)?.content?.length === 1 || data(r)?.totalElements === 1, JSON.stringify(data(r)?.totalElements))
r = await api('GET', `/moment/${pubMomentId}/liked`, { token: U2 })
ok('动态点赞状态查询', code(r) === 200, msg(r))

// ============ 8. 好友 + 聊天 ============
console.log('\n[8] 好友与聊天')
r = await api('GET', '/friend/search?keyword=e2e_u', { token: U2 })
ok('搜索用户返回列表', code(r) === 200 && data(r)?.length >= 2, msg(r))
ok('搜索结果不含 password 字段', Array.isArray(data(r)) && data(r).every(u => u.password === undefined))
r = await api('POST', '/friend/request', { token: U1, body: { friendId: u2id, remark: '加个好友' } })
ok('u1 发好友请求', code(r) === 200, msg(r))
r = await api('POST', '/friend/request', { token: U2, body: { friendId: u1id, remark: '我也加你' } })
ok('u2 反向发好友请求（互发场景）', code(r) === 200, msg(r))
r = await api('GET', '/friend/requests', { token: U2 })
const reqFromU1 = data(r)?.find(x => x.userId === u1id && x.status === 'pending')
ok('u2 能看到 u1 的请求', !!reqFromU1, JSON.stringify(data(r)?.length))
if (!reqFromU1) { console.log('  ABORT: 好友请求不可见，后续用例无意义'); process.exit(1) }
r = await api('POST', `/friend/accept/${reqFromU1.id}`, { token: U2 })
ok('u2 接受互发请求不再500（M1修复）', code(r) === 200, msg(r))
r = await api('GET', '/friend/check/' + u1id, { token: U2 })
ok('双向好友关系生效', data(r)?.isFriend === true || data(r) === true, JSON.stringify(data(r)))
r = await api('GET', '/friend/list', { token: U1 })
ok('好友列表包含对方', code(r) === 200 && data(r)?.some(f => f.friendId === u2id), msg(r))

r = await api('POST', '/chat/send', { token: U1, body: { receiverId: u2id, type: 'text', content: 'E2E你好' } })
ok('u1 发送聊天消息', code(r) === 200 && !!data(r)?.id, msg(r))
const chatMsgId = data(r)?.id
r = await api('GET', `/chat/history/${u1id}?page=0&size=20`, { token: U2 })
ok('u2 查聊天记录', code(r) === 200 && data(r)?.content?.some(m => m.id === chatMsgId), msg(r))
r = await api('GET', '/chat/unread-count', { token: U2 })
const unreadBefore = data(r)
ok('u2 未读数=1', unreadBefore === 1, JSON.stringify(unreadBefore))
r = await api('PUT', '/chat/read/batch', { token: U3, body: [chatMsgId] })
ok('u3 批量已读他人消息（静默不生效）', r.status === 200, msg(r))
r = await api('GET', '/chat/unread-count', { token: U2 })
ok('u2 未读数仍=1（IDOR修复）', data(r) === 1, JSON.stringify(data(r)))
r = await api('PUT', `/chat/read/${chatMsgId}`, { token: U3 })
ok('u3 单条已读他人消息被拒', code(r) !== 200 && /无权/.test(msg(r)), msg(r))
r = await api('POST', `/chat/recall/${chatMsgId}`, { token: U3 })
ok('u3 撤回他人消息被拒', code(r) !== 200 && /撤回自己/.test(msg(r)), msg(r))
r = await api('PUT', `/chat/read/all/${u1id}`, { token: U2 })
ok('u2 清空来自 u1 的未读', code(r) === 200, msg(r))
r = await api('GET', '/chat/unread-count', { token: U2 })
ok('u2 未读数=0', data(r) === 0, JSON.stringify(data(r)))
r = await api('GET', '/chat/conversations', { token: U1 })
ok('会话列表存在', code(r) === 200 && data(r)?.length >= 1, msg(r))

// ============ 9. 通知 ============
console.log('\n[9] 通知')
r = await api('GET', '/notification/unread-count', { token: U1 })
const unreadNotif = data(r)
ok('u1 有未读通知（点赞/好友请求等）', unreadNotif >= 1, JSON.stringify(unreadNotif))
r = await api('GET', '/notification/list?page=0&size=20', { token: U1 })
const notif = data(r)?.content?.[0]
ok('u1 通知列表非空', !!notif, msg(r))
r = await api('PUT', `/notification/read/${notif.id}`, { token: U2 })
ok('u2 已读他人通知被拒（IDOR修复）', code(r) !== 200 && /无权/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
r = await api('PUT', '/notification/read/batch', { token: U2, body: data(r2 => r2) ?? [] })
r = await api('PUT', '/notification/read/batch', { token: U2, body: [notif.id] })
r = await api('GET', '/notification/unread-count', { token: U1 })
ok('u2 批量已读他人通知不生效', data(r) === unreadNotif, JSON.stringify(data(r)))
r = await api('PUT', `/notification/read/${notif.id}`, { token: U1 })
ok('本人已读自己的通知', code(r) === 200, msg(r))

// ============ 10. 分享 ============
console.log('\n[10] 分享')
r = await api('POST', '/share/song', { token: U1, body: { songId: hotSong.id, type: 'song', description: 'E2E分享测试' } })
ok('分享歌曲', code(r) === 200 && !!data(r)?.id, msg(r))
const shareId = data(r)?.id
r = await api('POST', `/share/song/${shareId}/like`, { token: U2 })
ok('u2 点赞分享', code(r) === 200, msg(r))
r = await api('POST', `/share/song/${shareId}/like`, { token: U2 })
ok('重复点赞被拒（去重修复）', code(r) !== 200 && /已点赞/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', `/share/song/${shareId}/view`, { token: U2 })
r = await api('POST', `/share/song/${shareId}/view`, { token: U2 })
ok('分享浏览计数', code(r) === 200, msg(r))
r = await api('GET', '/share/songs?page=0&size=10', { token: U2 })
ok('分享广场包含该分享', code(r) === 200 && data(r)?.content?.some(s => s.id === shareId), msg(r))
r = await api('DELETE', `/share/song/${shareId}`, { token: U2 })
ok('非本人删除分享被拒', code(r) !== 200, msg(r))

// ============ 11. 管理员 ============
console.log('\n[11] 管理员')
r = await api('GET', '/admin/song/list?page=0&size=5', { token: U1, raw: true })
ok('非管理员访问后台=HTTP 403', r.status === 403, `status=${r.status}`)
r = await login('admin', '123456')
const ADMIN = data(r)?.token
ok('admin 登录（AdminInitializer）', code(r) === 200 && data(r)?.userInfo?.role === 'admin', msg(r))
r = await api('GET', '/admin/song/list?page=0&size=5', { token: ADMIN })
ok('管理员歌曲列表', code(r) === 200 && data(r)?.content?.length > 0, msg(r))
r = await api('GET', `/admin/song/${hotSong.id}`, { token: ADMIN })
const origArtists = (data(r)?.artists || []).map(a => a.id)
r = await api('PUT', `/admin/song/${hotSong.id}`, { token: ADMIN, body: { title: hotSong.title, url: hotSong.url, artistIds: origArtists } })
ok('更新歌曲不传status不报错（M8修复）', code(r) === 200, `code=${code(r)} msg=${msg(r)}`)
r = await api('GET', `/admin/song/${hotSong.id}`, { token: ADMIN })
ok('歌曲状态保持为1', data(r)?.status === 1, JSON.stringify(data(r)?.status))

const fd = new FormData()
fd.append('file', new Blob([new Uint8Array([0x49, 0x44, 0x33])], { type: 'audio/mpeg' }), 'test.mp3')
r = await api('POST', '/upload/music', { token: U1, body: fd })
ok('普通用户上传音频被拒（权限修复）', code(r) !== 200 && /无权限/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)
const fd2 = new FormData()
fd2.append('file', new Blob([new Uint8Array([0x89, 0x50])], { type: 'image/png' }), 'noext')
r = await api('POST', '/upload/avatar', { token: U1, body: fd2 })
ok('无扩展名头像被拒（不再崩溃）', code(r) !== 200 && /不支持的图片格式/.test(msg(r)), `code=${code(r)} msg=${msg(r)}`)

// ============ 12. 401 统一响应 ============
console.log('\n[12] 未认证统一 401')
r = await api('GET', '/api/user/favorite/list', { raw: true })
ok('无token访问=HTTP 401（entry point修复）', r.status === 401, `status=${r.status}`)
r = await api('GET', '/music/hot?page=0&size=1', { raw: true })
ok('需登录接口无token=401', r.status === 401, `status=${r.status}`)
r = await api('GET', '/api/user/search-history/hot', { raw: true })
ok('热门搜索公开接口可访问', r.status === 200, `status=${r.status}`)

// ============ 13. 推荐 ============
console.log('\n[13] 推荐系统')
r = await api('GET', '/music/recommend/personalized?limit=5', { token: U1 })
ok('个性化推荐', code(r) === 200 && Array.isArray(data(r)), msg(r))
ok('推荐不含下架歌曲', Array.isArray(data(r)) && data(r).every(i => i.song ? i.song.status === 1 : i.status === 1))
r = await api('GET', '/music/recommend/guess-you-like?limit=5', { token: U1 })
ok('猜你喜欢', code(r) === 200 && Array.isArray(data(r)), msg(r))
r = await api('GET', `/music/recommend/similar-songs/${hotSong.id}?limit=3`, { token: U1 })
ok('相似歌曲', code(r) === 200 && Array.isArray(data(r)), msg(r))

// ============ 14. 用户资料 ============
console.log('\n[14] 用户资料')
r = await api('GET', '/user/info', { token: U1 })
ok('获取个人信息', code(r) === 200 && data(r)?.id === u1id, msg(r))
r = await api('PUT', '/user/update', { token: U1, body: { nickname: '端到端一号', signature: 'E2E签名' } })
ok('更新个人资料', code(r) === 200 && data(r)?.nickname === '端到端一号', msg(r))
r = await api('PUT', '/user/password', { token: U3, body: { oldPassword: 'test123456', newPassword: 'newpass456' } })
ok('u3 修改密码', code(r) === 200, msg(r))
r = await login(NAME3, 'newpass456')
ok('新密码可登录', code(r) === 200, msg(r))
r = await login(NAME3, 'test123456')
ok('旧密码已失效', code(r) !== 200, msg(r))

// ============ 15. 搜索历史 ============
console.log('\n[15] 搜索历史')
r = await api('POST', '/api/user/search-history', { token: U1, body: { keyword: 'E2E关键词' } })
ok('保存搜索历史', code(r) === 200, msg(r))
r = await api('GET', '/api/user/search-history?page=0&size=10', { token: U1 })
ok('搜索历史列表', code(r) === 200, msg(r))

// ============ 16. 清理测试资源（删除歌单/动态/收藏恢复原状） ============
console.log('\n[16] 资源清理')
await api('DELETE', `/api/playlist/${plId}`, { token: U1 })
await api('DELETE', `/moment/${pubMomentId}`, { token: U1 })
await api('DELETE', `/moment/${privMomentId}`, { token: U1 })
await api('DELETE', `/api/user/favorite/${hotSong.id}`, { token: U1 })
r = await api('GET', '/api/playlist/my?page=0&size=10', { token: U1 })
ok('测试歌单已清理', code(r) === 200 && !data(r)?.content?.some(p => p.id === plId))
console.log('\n========================================')
console.log(`通过 ${pass} 项，失败 ${fail} 项`)
if (failures.length) { console.log('失败项:'); failures.forEach(f => console.log('  - ' + f)) }
process.exit(fail ? 1 : 0)
