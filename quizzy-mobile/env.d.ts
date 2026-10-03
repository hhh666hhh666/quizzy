/// <reference types="vite/client" />

// 与 quizzy-web 同名同义：构建期注入的版本与构建信息，值在 vite.config.ts 的 define 里生成。
// 本轮移动端没有注入通道，直跑时是 dev / 0 / unknown / unknown。
declare const __APP_VERSION__: string
declare const __APP_AHEAD__: string
declare const __APP_COMMIT__: string
declare const __APP_BUILD_TIME__: string

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
