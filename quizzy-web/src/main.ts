import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
// 深色变量表：只在 `html.dark` 下生效，不影响浅色默认值
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

const app = createApp(App)

app.use(createPinia())
// ⚠️ 不传 locale 的话 element-plus 用**英文**：分页会显示「Total 34」「10/page」，
// 整个中文界面里就这一处是洋文。这里只影响组件内置文案，业务文案不受影响。
app.use(ElementPlus, { locale: zhCn })
app.use(router)

// 挂载前先定下主题，免得首屏先渲染浅色再翻成深色
useThemeStore().init()

app.mount('#app')
