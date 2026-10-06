import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 深色变量表：只在 `html.dark` 下生效，不影响浅色默认值
import 'element-plus/theme-chalk/dark/css-vars.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

const app = createApp(App)

app.use(createPinia())
app.use(ElementPlus)
app.use(router)

// 挂载前先定下主题，免得首屏先渲染浅色再翻成深色
useThemeStore().init()

app.mount('#app')
