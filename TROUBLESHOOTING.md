# 分享功能故障排查指南

## 问题1: `Cannot read properties of undefined (reading '0')`

### 原因
ShareDialog组件在初始化时，shareData可能为undefined或空对象。

### 解决方案 ✅
已修复：
1. 为`shareData` prop添加了默认值
2. 在模板中使用可选链操作符 `?.`
3. 添加了默认值处理

### 验证修复
重新启动前端开发服务器：
```bash
cd front/sheep-music
npm run dev
```

---

## 问题2: `ws proxy socket error: ECONNRESET`

### 原因
WebSocket代理连接错误，通常是因为后端服务未启动或连接中断。

### 解决步骤

#### 1. 检查后端服务是否启动
```bash
# 检查端口9000是否被占用
netstat -ano | findstr :9000
```

#### 2. 启动后端服务
```bash
cd back/music-project
mvn spring-boot:run
# 或者在IDE中运行主类
```

#### 3. 验证后端服务
访问: http://localhost:9000/swagger-ui.html
确认Swagger文档可以正常访问

#### 4. 检查数据库连接
确保MySQL数据库已启动并且配置正确：
- 数据库名: sheep_music
- 端口: 3306
- 用户名和密码在 `application.yml` 中配置

#### 5. 创建数据库表
首次运行时，需要确保数据库表已创建。新增的表：
- `tb_song_share` - 歌曲分享表

Spring Boot会自动创建表（如果配置了`spring.jpa.hibernate.ddl-auto=update`）

---

## 完整启动流程

### 1. 启动MySQL数据库
确保MySQL服务正在运行

### 2. 启动后端服务
```bash
cd back/music-project
mvn clean install
mvn spring-boot:run
```

等待看到类似输出：
```
Started SheepMusicApplication in X.XXX seconds
```

### 3. 启动前端服务
```bash
cd front/sheep-music
npm install  # 首次运行
npm run dev
```

访问: http://localhost:8001

---

## 测试分享功能

### 测试歌曲分享

1. **从播放器分享**
   - 播放任意歌曲
   - 点击播放器右侧的"分享"按钮（Share图标）
   - 填写分享描述
   - 点击"分享"按钮

2. **从歌单详情页分享**
   - 进入任意歌单详情页
   - 在歌曲列表中，点击歌曲操作栏的"分享"按钮
   - 填写分享描述
   - 点击"分享"按钮

### 测试歌单分享

1. 进入自己创建的歌单详情页
2. 点击"分享到广场"按钮
3. 填写分享描述
4. 点击"分享"按钮

### 验证分享成功

1. 打开浏览器开发者工具（F12）
2. 查看Network标签
3. 应该看到POST请求到 `/api/share/song` 或 `/api/share/playlist`
4. 响应状态码应为200
5. 页面显示"分享成功，已发布到分享广场"

---

## 常见错误及解决方案

### 错误: "请先登录"
**原因**: 未登录或token过期
**解决**: 重新登录系统

### 错误: "歌曲信息无效"
**原因**: 当前没有播放歌曲或歌曲数据不完整
**解决**: 确保有歌曲正在播放

### 错误: "分享数据无效"
**原因**: shareData.id为空
**解决**: 检查歌曲或歌单是否有有效的ID

### 错误: 401 Unauthorized
**原因**: JWT token无效或过期
**解决**: 
1. 清除浏览器localStorage
2. 重新登录

### 错误: 500 Internal Server Error
**原因**: 后端服务异常
**解决**:
1. 查看后端控制台日志
2. 检查数据库连接
3. 确认数据库表已创建

---

## 调试技巧

### 前端调试
```javascript
// 在ShareDialog.vue的handleShare方法中添加
console.log('分享数据:', {
  type: props.type,
  shareData: props.shareData,
  description: form.value.description
})
```

### 后端调试
在ShareController中添加日志：
```java
@PostMapping("/song")
public Result<SongShare> shareSong(@Valid @RequestBody ShareRequest request,
                                   HttpServletRequest httpRequest) {
    System.out.println("收到歌曲分享请求: " + request);
    // ... 其他代码
}
```

### 数据库验证
```sql
-- 查看歌曲分享记录
SELECT * FROM tb_song_share ORDER BY create_time DESC LIMIT 10;

-- 查看歌单分享记录
SELECT * FROM tb_playlist_share ORDER BY create_time DESC LIMIT 10;
```

---

## 性能优化建议

1. **添加分享缓存**: 使用Redis缓存热门分享
2. **分页加载**: 分享广场使用虚拟滚动
3. **图片懒加载**: 分享列表中的封面图片懒加载
4. **防抖处理**: 分享按钮添加防抖，避免重复提交

---

## 联系支持

如果问题仍然存在，请提供：
1. 浏览器控制台完整错误信息
2. 后端控制台日志
3. 操作步骤截图
