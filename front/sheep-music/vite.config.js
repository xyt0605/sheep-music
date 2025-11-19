import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue({
      template: {
        compilerOptions: {
          isCustomElement: (tag) => tag === 'emoji-picker'
        }
      }
    })
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
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
      '/api': {
        target: 'http://localhost:9000',
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

