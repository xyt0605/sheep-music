const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false, // 禁用 ESLint 检查
  devServer: {
    port: 8001, // 前端服务器端口
    client: {
      overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: (error) => {
          // 忽略 ResizeObserver 错误
          const ignoreErrors = [
            'ResizeObserver loop completed with undelivered notifications.',
            'ResizeObserver loop limit exceeded'
          ]
          if (ignoreErrors.includes(error.message)) {
            return false
          }
          return true
        }
      }
    },
    proxy: {
      '/api': {
        target: 'http://localhost:9000', // 后端服务器地址
        changeOrigin: true,
        pathRewrite: {
          '^/api': '' // 将 /api 替换为空
        }
      }
    }
  }
})
