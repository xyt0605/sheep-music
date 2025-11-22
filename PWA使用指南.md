# 📱 Sheep Music PWA 使用指南

## 什么是 PWA？

PWA (Progressive Web App) 是渐进式网页应用，可以让网页拥有接近原生 APP 的体验：
- ✅ 可以"安装"到手机桌面
- ✅ 支持离线使用
- ✅ 快速加载
- ✅ 沉浸式全屏体验
- ✅ 无需应用商店下载

## 🎯 已完成的 PWA 配置

### 1. **manifest.json** - PWA 配置文件
定义了应用的名称、图标、主题色等信息

### 2. **Service Worker** - 离线缓存
实现资源缓存，支持离线访问

### 3. **Meta 标签** - 移动端优化
添加了 iOS 支持、主题色等配置

## 📱 如何在手机上"安装"？

### **Android 手机 (Chrome/Edge)**
1. 用 Chrome 或 Edge 浏览器打开网站
2. 点击浏览器菜单 (三个点)
3. 选择"添加到主屏幕"或"安装应用"
4. 确认安装
5. 在桌面找到 Sheep Music 图标点击使用

### **iPhone (Safari)**
1. 用 Safari 浏览器打开网站
2. 点击底部分享按钮 (方框带箭头)
3. 向下滚动，选择"添加到主屏幕"
4. 编辑名称（可选），点击"添加"
5. 在桌面找到 Sheep Music 图标点击使用

## 🎨 需要准备的图标

你需要在 `public` 目录下放置以下图标：

### **必需图标：**
- `icon-192x192.png` - 192x192 像素
- `icon-512x512.png` - 512x512 像素

### **可选图标：**
- `favicon.ico` - 网站图标
- `apple-touch-icon.png` - iOS 专用图标

### 图标设计建议：
- 使用简洁的设计
- 背景色：深色 (#0a0a0f) 或主题色 (#667eea)
- 前景：白色或浅色图标/文字
- 保持一定的内边距，避免被裁切

## 🔧 Nginx 配置更新

为了让 PWA 正常工作，需要在 Nginx 中添加正确的 MIME 类型：

```nginx
# 在 server 块中添加
location ~* \.(json|webmanifest)$ {
    add_header Content-Type application/json;
    add_header Cache-Control "public, max-age=3600";
}

location /sw.js {
    add_header Content-Type application/javascript;
    add_header Cache-Control "no-cache, no-store, must-revalidate";
}
```

## 🚀 部署步骤

```bash
# 1. 准备图标文件
# 将 icon-192x192.png 和 icon-512x512.png 放到 public 目录

# 2. 重新构建
cd front/sheep-music
npm run build

# 3. 推送到 Git
git add .
git commit -m "feat: 添加 PWA 支持"
git push origin main

# 4. 服务器部署
cd /opt/sheep-music
git pull origin main
docker compose down
docker compose build --no-cache frontend
docker compose up -d
```

## ✨ PWA 带来的优势

### 1. **用户体验**
- 全屏沉浸式体验，无浏览器地址栏
- 更快的加载速度（缓存）
- 离线也能查看已缓存的内容

### 2. **安装便捷**
- 无需应用商店审核
- 用户访问即可安装
- 更新即时生效

### 3. **跨平台**
- Android、iOS、桌面都支持
- 一套代码，多端使用
- 维护成本低

## 📊 与原生 APP 对比

| 特性 | PWA | 原生 APP |
|-----|-----|---------|
| 开发成本 | 低 | 高 |
| 分发方式 | URL 直接访问 | 应用商店 |
| 更新速度 | 即时 | 需审核 |
| 存储空间 | 小 | 大 |
| 推送通知 | ✅ | ✅ |
| 离线使用 | ✅ | ✅ |
| 系统集成 | 部分 | 完整 |
| 性能 | 较好 | 最好 |

## 🔮 进一步优化建议

### 1. **添加离线页面**
创建 `public/offline.html`，在无网络时显示

### 2. **优化缓存策略**
- 静态资源：长期缓存
- API 数据：短期缓存或不缓存
- 音频/图片：按需缓存

### 3. **添加推送通知**
- 新歌推荐通知
- 喜欢的歌手发新歌提醒

### 4. **优化触摸体验**
- 滑动切歌
- 双击收藏
- 长按显示菜单

### 5. **性能监控**
使用 Lighthouse 检测 PWA 评分

## 🐛 常见问题

### Q: 为什么看不到"添加到主屏幕"？
A: 需要满足以下条件：
- HTTPS 连接
- 有效的 manifest.json
- 注册了 Service Worker

### Q: iOS Safari 不显示安装提示？
A: iOS Safari 不会主动提示，用户需要手动操作"添加到主屏幕"

### Q: 更新后用户看不到新版本？
A: Service Worker 需要刷新才会更新，可以提示用户刷新页面

## 📚 参考资料

- [PWA 官方文档](https://web.dev/progressive-web-apps/)
- [Service Worker API](https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API)
- [Web App Manifest](https://developer.mozilla.org/en-US/docs/Web/Manifest)
