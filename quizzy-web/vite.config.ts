import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// 版本与构建信息在构建期注入（docker build --build-arg，由 .github/workflows/release.yml 计算）。
// 取不到就是「在宿主机直跑」（npm run dev / 本地 npm run build），显式退化成 dev，
// 不留空字符串——空值在界面上看不出来，dev 至少能一眼看出不是正式构建。
// ⚠️ 这几个值只在构建期存在，不进任何配置文件，也不经接口传输。
const APP_VERSION = process.env.APP_VERSION ?? 'dev'
const APP_AHEAD = process.env.APP_AHEAD ?? '0'
const APP_COMMIT = process.env.APP_COMMIT ?? 'unknown'
const APP_BUILD_TIME = process.env.APP_BUILD_TIME ?? 'unknown'

export default defineConfig({
  // Tailwind CSS v4：对现有页面**惰性**——只有 `@import "tailwindcss"` 的文件才会被它处理，
  // 所以在迁移完成前，它不影响任何既有样式。
  plugins: [vue(), tailwindcss()],
  define: {
    __APP_VERSION__: JSON.stringify(APP_VERSION),
    __APP_AHEAD__: JSON.stringify(APP_AHEAD),
    __APP_COMMIT__: JSON.stringify(APP_COMMIT),
    __APP_BUILD_TIME__: JSON.stringify(APP_BUILD_TIME)
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
