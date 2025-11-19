## 目标
- 增强分享能力：在歌单/歌曲/动态提供便捷分享入口并完善分享广场交互（收藏、浏览数）。
- 引入第三方 Emoji 选择器：为发布与评论输入框提供高质量表情选择与插入体验。

## 技术选择
- Emoji 库：`vue3-emoji-picker`
  - 原生支持 Vue 3 与 Vite，体积小，内置丰富表情分类与搜索。
  - 安装：`npm i vue3-emoji-picker` 并引入其样式 `import 'vue3-emoji-picker/dist/style.css'`。

## 组件与页面改造
### 新增组件
- `components/Social/EmojiPicker.vue`
  - 封装 `vue3-emoji-picker`，暴露 `onPick(emoji)` 回调，支持插入到任意 `el-input/textarea` 光标处。
  - 轻量弹出层（悬浮面板）与受控显示（props：visible、placement）。
- `components/Social/ShareComposer.vue`
  - 统一分享表单（标题、描述、Emoji、提交按钮）；用于歌单分享弹窗与动态分享入口。

### 页面改造
- `views/Social/Moments.vue`
  - 发布动态弹窗内添加 Emoji 按钮与选择器；在内联评论输入区域集成 Emoji 插入。
  - 利用现有 `publishMoment/commentMoment` 接口，保持后端无改动。
- `components/Social/CommentInput.vue`
  - 增加 Emoji 按钮与插槽用于挂载 `EmojiPicker`；实现将选中表情插入到 `v-model="content"` 的光标位置。
- `views/Social/ShareSquare.vue`
  - 分享卡片添加“收藏”按钮（`collectSharedPlaylist(shareId)`），收藏后乐观更新计数；浏览数统计调用参数修正为 `share.id`。
- 歌单相关（如 `views/PlaylistDetail.vue` 或歌单列表卡片）
  - 增加“分享”按钮，弹出 `ShareComposer`，调用 `sharePlaylist(data)` 发布到分享广场。

## 交互与校验
- 未登录时，分享/评论操作提示并跳转登录。
- 操作成功消息提示，并提供“查看分享广场”快捷入口。
- 计数采用乐观更新，失败回滚并提示。

## 开发步骤
1. 安装依赖并新增 `EmojiPicker.vue` 与 `ShareComposer.vue`。
2. 改造 `CommentInput.vue`、`Moments.vue`、`ShareSquare.vue`，在需要的输入框集成 Emoji 并完善交互。
3. 在歌单详情/列表入口添加“分享”按钮，打通分享流程。
4. 自测：
   - 发布动态/评论并插入 Emoji 显示正确。
   - 分享到广场后可见，浏览/收藏计数正常变更。

## 验收标准
- 用户可在发布与评论时选择并插入表情，显示正确。
- 歌单分享流程可用，分享广场展示条目并支持收藏、浏览统计。
- 未登录时有明确提示并引导登录。