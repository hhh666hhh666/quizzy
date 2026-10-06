import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

/** 三档外观：跟随系统 / 浅色 / 深色。「没存过」等价于跟随系统。 */
export type ThemeMode = 'auto' | 'light' | 'dark'

/**
 * ⚠️ 这个 key 在 `index.html` 的首屏防闪烁脚本里**又写了一遍**——那边要在 CSS 生效前同步执行，
 * 拿不到本模块。改这里就必须一起改那边，否则会出现「刷新后闪一下浅色」。
 */
export const THEME_STORAGE_KEY = 'quizzy_theme'

const DARK_QUERY = '(prefers-color-scheme: dark)'

function readMode(): ThemeMode {
  const stored = localStorage.getItem(THEME_STORAGE_KEY)
  return stored === 'light' || stored === 'dark' || stored === 'auto' ? stored : 'auto'
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(readMode())
  // 系统当前是否深色。只有 auto 档才看它
  const systemDark = ref(window.matchMedia(DARK_QUERY).matches)

  const isDark = computed(() => (mode.value === 'auto' ? systemDark.value : mode.value === 'dark'))

  // 深色变量表的选择器就是 `html.dark`（Element Plus 约定），所以只动 <html> 上的类
  function apply() {
    document.documentElement.classList.toggle('dark', isDark.value)
  }

  function setMode(next: ThemeMode) {
    mode.value = next
    localStorage.setItem(THEME_STORAGE_KEY, next)
    apply()
  }

  /** 由 `main.ts` 在挂载前调一次：先定主题再渲染，避免首屏翻色 */
  function init() {
    apply()
    // 选「跟随系统」时，系统可能中途切换（如 macOS 的日落自动切换）
    window.matchMedia(DARK_QUERY).addEventListener('change', (event) => {
      systemDark.value = event.matches
      if (mode.value === 'auto') apply()
    })
  }

  return { mode, isDark, setMode, init }
})
