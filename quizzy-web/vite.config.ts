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
  // Tailwind CSS v4：样式入口分两套（`styles/app.css` / `styles/preview.css`，见 tokens.css 文件头）。
  // 主应用入口**过渡期不引 preflight / base**——未迁移的 Element Plus 页面不吃全局重置；
  // 只有用到 Tailwind 的文件才会被插件处理，逐页迁移期间其余页面不受影响。
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
  build: {
    rollupOptions: {
      // 两个入口：index（主应用）+ preview（设计系统预览页）。
      // preview 只挂 token 层，用来肉眼看设计系统；主应用不受它影响。
      input: {
        index: fileURLToPath(new URL('./index.html', import.meta.url)),
        preview: fileURLToPath(new URL('./preview.html', import.meta.url))
      }
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
