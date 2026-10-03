import { defineConfig } from 'vite'
import uniPlugin from '@dcloudio/vite-plugin-uni'
import { fileURLToPath, URL } from 'node:url'

// ⚠️ @dcloudio/vite-plugin-uni 是 CJS 包，工厂函数挂在 exports.default 上（带 __esModule）。
// 本项目 package.json 有 "type": "module"，配置文件以原生 ESM 加载，此时默认导入拿到的是
// module.exports 整个对象而不是那个函数，uni() 会直接报 "uni is not a function"。
// 这里做一次归一化，两条加载路径（原生 ESM / esbuild 打包成 CJS）都成立。
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const uni = (typeof uniPlugin === 'function'
  ? uniPlugin
  : (uniPlugin as unknown as { default: () => unknown }).default) as unknown as () => any

// 与 quizzy-web 同样的构建期注入口径（值来自 docker build --build-arg，源头是 git tag）。
// 本轮移动端没有 Dockerfile / release 注入通道，取不到就退化成 dev/0/unknown/unknown，
// 且暂时不在任何页面里使用——保留管线是为了下一轮接上时零改动。
const APP_VERSION = process.env.APP_VERSION ?? 'dev'
const APP_AHEAD = process.env.APP_AHEAD ?? '0'
const APP_COMMIT = process.env.APP_COMMIT ?? 'unknown'
const APP_BUILD_TIME = process.env.APP_BUILD_TIME ?? 'unknown'

export default defineConfig({
  plugins: [uni()],
  css: {
    preprocessorOptions: {
      scss: {
        // wot-design-uni 的 scss 用了 @import 与 sass 全局内置函数（nth/unquote 等），
        // 在 Dart Sass 1.80+ 上是 deprecation warning。库不归我们改，静音掉以保持日志干净；
        // 这三类是「库的历史写法」，新写的样式不受影响。
        silenceDeprecations: ['legacy-js-api', 'import', 'global-builtin', 'if-function']
      }
    }
  },
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
    // 5174 避开 quizzy-web 的 5173
    port: 5174,
    // host: true 让同网段的手机能直接打开，用于真机预览
    host: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
