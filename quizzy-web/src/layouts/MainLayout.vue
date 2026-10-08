<template>
  <div class="flex h-full">
    <!-- 键盘用户的「跳到主要内容」：平时 sr-only，聚焦时才出现（规范审查后补） -->
    <a
      href="#main-content"
      class="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-[80] focus:rounded-md focus:bg-surface focus:px-3 focus:py-2 focus:text-sm focus:shadow-md focus-visible:ring-2 focus-visible:ring-focus"
    >
      跳到主要内容
    </a>
    <!-- 侧栏：实心有色面（surface-2）。⚠️ 它后面没有可透的内容，按「两层语言」的规矩②不用玻璃 -->
    <aside class="app-aside flex w-[200px] shrink-0 flex-col border-r border-line bg-surface-2 font-sans text-base text-ink">
      <!-- 产品标识：图标块 + 双行字标（2026-10-08 主人给的参考样式） -->
      <div class="flex items-center gap-2.5 px-4 pb-3 pt-5">
        <div class="grid size-9 shrink-0 place-items-center rounded-lg bg-brand shadow-sm">
          <ClipboardListIcon class="size-5 text-white" />
        </div>
        <div class="leading-tight">
          <div class="text-lg font-semibold tracking-tight text-brand">Quizzy</div>
          <div class="text-2xs font-medium tracking-[0.2em] text-ink-subtle">PRO STUDIO</div>
        </div>
      </div>
      <nav class="flex flex-col gap-1 px-3 py-2">
        <RouterLink
          v-for="item in NAV_ITEMS"
          :key="item.to"
          :to="item.to"
          class="no-underline flex items-center gap-2.5 rounded-md px-3 py-2 outline-none transition-colors focus-visible:ring-2 focus-visible:ring-focus"
          :class="isActive(item.to) ? 'bg-brand-soft font-medium text-ink-blue' : 'text-ink hover:bg-surface'"
        >
          <component :is="item.icon" class="size-4 shrink-0" :class="navIconClass(item, isActive(item.to))" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>

      <!-- 底部用户区（2026-10-08 按主人参考图从顶栏移来）：左「头像 + 昵称」，右「浅色 / 关于」，单行齐平 -->
      <div class="mt-auto border-t border-line-soft px-2.5 py-3">
        <div class="flex items-center justify-between gap-1.5">
          <!-- 头像与昵称都进设置页；不给整块加 hover 底色（主人：别出现白框），只让名字轻微变色 -->
          <RouterLink
            to="/profile"
            class="no-underline flex min-w-0 items-center gap-2 rounded-md py-1 outline-none focus-visible:ring-2 focus-visible:ring-focus"
          >
            <UserAvatar :user="store.user" :size="30" />
            <span class="truncate text-sm font-semibold text-ink-strong transition-colors hover:text-ink-muted">{{ store.user?.nickname || '未登录' }}</span>
          </RouterLink>

          <div class="flex shrink-0 items-center gap-0.5 text-xs text-ink-muted">
            <DropdownMenu>
              <DropdownMenuTrigger as-child>
                <button
                  type="button"
                  class="rounded-sm outline-none hover:text-brand focus-visible:ring-2 focus-visible:ring-focus"
                  :aria-label="`外观设置（当前：${modeLabel}）`"
                >
                  {{ modeLabel }}
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" side="top">
                <DropdownMenuItem v-for="opt in THEME_OPTIONS" :key="opt.value" @click="onTheme(opt.value)">
                  {{ themeStore.mode === opt.value ? '✓ ' : '' }}{{ opt.label }}
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            <span aria-hidden="true">/</span>
            <button
              type="button"
              class="rounded-sm outline-none hover:text-brand focus-visible:ring-2 focus-visible:ring-focus"
              @click="aboutVisible = true"
            >
              关于
            </button>
          </div>
        </div>
      </div>
    </aside>

    <!-- 右列：主内容从顶栏下面穿过——顶栏的玻璃才有东西可透 -->
    <div class="relative h-full min-w-0 flex-1">
      <main id="main-content" tabindex="-1" class="app-main absolute inset-0 overflow-y-auto pt-14">
        <div class="p-5">
          <router-view />
        </div>
      </main>

      <header class="app-header glass-bar absolute inset-x-0 top-0 z-10 flex h-14 items-center justify-between border-b px-4 font-sans text-base text-ink">
        <span class="text-sm font-medium">{{ route.meta.title || '' }}</span>
      </header>
    </div>

    <AboutDialog v-model:visible="aboutVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArchiveIcon,
  ClipboardListIcon,
  ClockIcon,
  FileTextIcon,
  StarIcon,
  TriangleAlertIcon,
  ZapIcon,
} from '@lucide/vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import UserAvatar from '@/components/UserAvatar.vue'
import AboutDialog from '@/views/AboutDialog.vue'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem
} from '@/components/ui/dropdown-menu'

const route = useRoute()
const store = useUserStore()
const themeStore = useThemeStore()

const aboutVisible = ref(false)

const NAV_ITEMS: { to: string; label: string; icon: Component; accent?: boolean }[] = [
  { to: '/questions', label: '题库', icon: ArchiveIcon },
  { to: '/papers', label: '试卷', icon: FileTextIcon },
  { to: '/quiz/quick', label: '快速练习', icon: ZapIcon },
  { to: '/wrong-book', label: '错题本', icon: TriangleAlertIcon },
  { to: '/favorites', label: '收藏夹', icon: StarIcon, accent: true },
  { to: '/history', label: '答题记录', icon: ClockIcon }
]

/** 侧栏高亮：当前路径等于该项、或在其子路径下（/quiz/quick 精确匹配；/questions 含子页）。 */
function isActive(to: string) {
  return route.path === to || route.path.startsWith(to + '/')
}

/** 导航图标的颜色：收藏夹的星标恒为琥珀 #F59E0B（主人指定色，token --color-amber）；其余随选中态走深蓝 / 灰。 */
function navIconClass(item: { accent?: boolean }, active: boolean) {
  if (item.accent) return 'fill-amber text-amber'
  return active ? 'text-ink-blue' : 'text-ink-muted'
}

// 外观切换放在侧栏底部（登录页没有侧栏，只能跟随已存的偏好）
const THEME_OPTIONS: { value: ThemeMode; label: string }[] = [
  { value: 'auto', label: '跟随系统' },
  { value: 'light', label: '浅色' },
  { value: 'dark', label: '深色' }
]

const modeLabel = computed(
  () => THEME_OPTIONS.find((opt) => opt.value === themeStore.mode)?.label || '跟随系统'
)

function onTheme(command: ThemeMode) {
  themeStore.setMode(command)
}

onMounted(() => {
  if (!store.user) store.loadUser()
})
</script>
