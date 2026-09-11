// AI 厂商预设（系统设置 v1 从 DjDrawer 提取共享）：任何 OpenAI 兼容端点均可（选"自定义"后手填）
// DjDrawer 与 管理后台-系统设置 共用，新增厂商只改这里
export const PROVIDER_PRESETS = [
  { key: 'zhipu', label: '智谱 BigModel', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', models: ['glm-4.6', 'glm-4.5-air', 'glm-4.5-flash', 'glm-4-plus'], hint: 'glm-4.6 为当前旗舰；flash 免费但当前响应慢' },
  { key: 'deepseek', label: 'DeepSeek', baseUrl: 'https://api.deepseek.com', models: ['deepseek-chat', 'deepseek-reasoner'], hint: '两个别名自动跟随 DeepSeek 最新版；reasoner 思考耗大量 token' },
  { key: 'moonshot', label: '月之暗面 Kimi', baseUrl: 'https://api.moonshot.cn/v1', models: ['kimi-latest', 'kimi-k2-turbo-preview', 'kimi-k2-0905-preview', 'moonshot-v1-8k'] },
  { key: 'qwen', label: '阿里通义千问', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', models: ['qwen3-max', 'qwen-plus-latest', 'qwen-turbo-latest', 'qwen-max'] },
  { key: 'doubao', label: '字节豆包（火山方舟）', baseUrl: 'https://ark.cn-beijing.volces.com/api/v3', models: ['（填你的接入点 ID）'], hint: '模型填方舟控制台的接入点 ID' },
  { key: 'siliconflow', label: '硅基流动（聚合）', baseUrl: 'https://api.siliconflow.cn/v1', models: ['deepseek-ai/DeepSeek-V3.1', 'Qwen/Qwen3-235B-A22B', 'moonshotai/Kimi-K2-Instruct'] },
  { key: 'openai', label: 'OpenAI', baseUrl: 'https://api.openai.com/v1', models: ['gpt-5', 'gpt-5-mini', 'gpt-4.1', 'gpt-4o'] },
  { key: 'gemini', label: 'Google Gemini', baseUrl: 'https://generativelanguage.googleapis.com/v1beta/openai', models: ['gemini-2.5-pro', 'gemini-2.5-flash', 'gemini-2.0-flash'] },
  { key: 'ollama', label: 'Ollama 本地（零成本）', baseUrl: 'http://localhost:11434/v1', models: ['qwen3:8b', 'deepseek-r1:8b', 'gpt-oss:20b', 'llama3.1:8b'], hint: '需本机已装 Ollama 并拉取模型；或用 ollama pull 拉新模型' },
  { key: 'custom', label: '自定义', baseUrl: '', models: [] }
]

export const findProviderPreset = (key) => PROVIDER_PRESETS.find(p => p.key === key)

/** 按已保存 baseUrl 反查预设 key（匹配不到视为自定义） */
export const providerKeyOfBaseUrl = (baseUrl) => {
  const match = PROVIDER_PRESETS.find(p => p.baseUrl && p.baseUrl === baseUrl)
  return match ? match.key : (baseUrl ? 'custom' : 'zhipu')
}
