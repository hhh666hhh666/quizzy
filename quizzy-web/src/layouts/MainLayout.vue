<template>
  <div class="flex h-full">
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
    </aside>

    <!-- 右列：主内容从顶栏下面穿过——顶栏的玻璃才有东西可透 -->
    <div class="relative h-full min-w-0 flex-1">
      <main class="app-main absolute inset-0 overflow-y-auto pt-14">
        <div class="p-5">
          <router-view />
        </div>
      </main>

      <header class="app-header glass-bar absolute inset-x-0 top-0 z-10 flex h-14 items-center justify-between border-b px-4 font-sans text-base text-ink">
        <span class="text-sm font-medium">{{ route.meta.title || '' }}</span>
        <div class="flex items-center gap-1">
          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <button
                type="button"
                class="flex items-center gap-2 rounded-md px-2 py-1 outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                <UserAvatar :user="store.user" :size="28" />
                <span>{{ store.user?.nickname || '未登录' }}</span>
              </button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" class="w-40">
              <DropdownMenuItem @click="router.push('/profile')">我的账户</DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem class="text-danger" @click="onLogout">退出登录</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <DropdownMenu>
            <DropdownMenuTrigger as-child>
              <Button variant="link">外观：{{ modeLabel }}</Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem v-for="opt in THEME_OPTIONS" :key="opt.value" @click="onTheme(opt.value)">
                {{ themeStore.mode === opt.value ? '✓ ' : '' }}{{ opt.label }}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button variant="link" @click="aboutVisible = true">关于</Button>
        </div>
      </header>
    </div>

    <AboutDialog v-model:visible="aboutVisible" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
  DropdownMenuItem,
  DropdownMenuSeparator
} from '@/components/ui/dropdown-menu'
import { Button } from '@/components/ui/button'

const route = useRoute()
const router = useRouter()
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

/** 导航图标的颜色：收藏夹的星标恒为琥珀（参考样式）；其余随选中态走深蓝 / 灰。 */
function navIconClass(item: { accent?: boolean }, active: boolean) {
  if (item.accent) return 'fill-warn text-warn'
  return active ? 'text-ink-blue' : 'text-ink-muted'
}

// 顶栏常驻，所以外观切换放在这里（登录页没有顶栏，只能跟随已存的偏好）
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

function onLogout() {
  store.logout()
  router.push('/login')
}
</script>
