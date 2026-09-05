import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'emoji-picker'
        }
      }
    }),
    // Element Plus 按需引入：模板里的 <el-xxx> 自动按文件引入组件与样式，
    // 配合 main.js 移除全量 import，主包体积从 ~1.6MB 降到几百 KB
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 一些旧包在浏览器中仍引用 Node 的 global
  // 提供别名以避免 "global is not defined" 错误
  define: {
    global: 'window'
  },
  build: {
    outDir: 'dist',
    assetsDir: 'assets',
    minify: 'esbuild',
    sourcemap: false
  },
  server: {
    port: 8001,
    proxy: {
      // 与生产 Nginx 保持一致：播放器会把 OSS 地址改写为 /api/oss/*。
      // 必须放在普通 /api 代理之前，否则媒体请求会被转发到 Spring Boot 并返回 401/404。
      '/api/oss': {
        target: 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path.replace(/^\/api\/oss/, '')
      },
      '/api': {
        // 本机 9000 被占用时可用 VITE_PROXY_TARGET 指到其他后端端口
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:9000',
        changeOrigin: true,
        ws: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      '/oss': {
        target: 'https://sheepmusic.oss-cn-hangzhou.aliyuncs.com',
        changeOrigin: true,
        secure: true,
        rewrite: (path) => path.replace(/^\/oss/, '')
      }
    }
  }
})

