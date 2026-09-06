# E2E 测试套件使用说明

本目录是三轮交付（Bug 修复 / 推荐系统 v2 / UI v2）沉淀的 API 级端到端测试，直接以 HTTP 调用真实后端 + 隔离 MySQL 库验证，无需浏览器。运行环境要求：Node 18+（验证用的 Node 22）。

## 脚本清单

| 脚本 | 覆盖范围 | 通过标准 |
|------|----------|----------|
| `api-test.mjs` | 全站 16 个模块 111 项断言：注册幂等/登录、歌曲浏览、收藏、播放历史、歌单（含隐私越权）、评论（原子计数+级联删除）、动态（可见性）、好友互接受、聊天（IDOR）、通知（IDOR）、分享（去重）、管理员、401 统一响应、推荐、用户资料、搜索历史 | 全部 PASS |
| `rec-test.mjs` | 推荐系统 v2 规格验收 20 项（docs/specs/推荐系统v2/01-需求规格.md §6）：响应结构/理由、硬过滤、去重、冷启动、通道命中、缓存与换一批、similar-songs 兼容、性能冒烟 | 全部 PASS |
| `ws-test.mjs` | WebSocket(STOMP) 鉴权 7 项：匿名拒连、合法连接、订阅自己频道收推送、订阅他人频道被拒、好友实时收发 | 全部 PASS |
| `external-test.mjs` | 曲库供应链（docs/specs/曲库供应链v1/）：外源搜索鉴权/参数校验、流代理 permitAll 与非法 trackId/fileId 拦截、音源列表（v1.3 起仅歌曲海聚合源）、歌曲海真实上游联测（搜索→歌词 LRC→stream 206/Range，网络可达时） | 全部 PASS |

## 运行前提

1. MySQL 8 可用（本机 root/1234 或自备），存在隔离测试库 `sheepmusic_e2e`：
   ```sql
   CREATE DATABASE IF NOT EXISTS sheepmusic_e2e DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
   ```
   首次建库可导入仓库根目录的 `backup_local.sql` 获得真实歌曲数据（也可空库启动，JPA 自动建表 + AdminInitializer 自动建管理员，但歌曲类用例需要数据）。
2. 后端在 **19000** 端口指向该测试库启动（见下方命令）。
3. `ws-test.mjs` 通过 `createRequire` 引用 `front/sheep-music/node_modules` 里的 `@stomp/stompjs` 与 `sockjs-client`，需先在 front 目录 `npm install`。

## 启动被测后端（Git Bash / Windows）

```bash
cd back/music-project
JAVA_HOME="C:\Users\29329\.jdks\corretto-1.8.0_462" \
PORT=19000 \
SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/sheepmusic_e2e?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai" \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=1234 \
ALIYUN_OSS_ACCESS_KEY_ID=dummy-key ALIYUN_OSS_ACCESS_KEY_SECRET=dummy-secret \
"/c/Users/29329/.m2/wrapper/dists/apache-maven-3.9.12-bin/5nmfsn99br87k5d4ajlekdq10k/apache-maven-3.9.12/bin/mvn" spring-boot:run -q -DskipTests
```

要点：
- 端口用 `PORT` 环境变量覆盖（本机 9000 常被 IDE 旧实例占用）。
- OSS 用占位凭证：上传类用例只验证权限拦截，不真传阿里云。
- Maven 用 `~/.m2/wrapper` 里自带的发行版（本机无全局 mvn），JDK 必须是 8（`~/.jdks/corretto-1.8.0_462`）。

## 运行测试

```bash
E2E_BASE=http://localhost:19000 node tests/e2e/api-test.mjs   # 111 项
E2E_BASE=http://localhost:19000 node tests/e2e/rec-test.mjs   # 20 项
E2E_BASE=http://localhost:19000 node tests/e2e/external-test.mjs # 曲库供应链（无密钥环境不访问外网）
node tests/e2e/ws-test.mjs                                     # 内部默认 19000，7 项
```

脚本可重复执行（用户名带时间戳、结束自动清理资源）。退出码非 0 即存在失败项。

## 已知环境坑

- 本机 shell 若配了 `http_proxy`，对 `localhost` 的 curl 需加 `--noproxy '*'`；对 `[::1]` 字面量代理不匹配会报 502。
- Vite dev 在本机只绑 IPv6 `[::1]`，浏览器走 `localhost` 不受影响。
- 访客自动化标签页会节流动画时钟，CSS 过渡看似"冻结"（getComputedStyle 停在旧值），用 `document.getAnimations().forEach(a => a.finish())` 强制完成后再读样式。
