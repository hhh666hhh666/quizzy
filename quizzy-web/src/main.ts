import { createApp } from 'vue'
import { createPinia } from 'pinia'
// 我们的设计系统入口（完整引入：Element Plus 已退场，preflight 的收益开始兑现——
// 见 src/styles/app.css 的文件头与 docs/adr/0031 的迁移里程碑）
import './styles/app.css'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

const app = createApp(App)

app.use(createPinia())
app.use(router)

// 挂载前先定下主题，免得首屏先渲染浅色再翻成深色
useThemeStore().init()

app.mount('#app')
