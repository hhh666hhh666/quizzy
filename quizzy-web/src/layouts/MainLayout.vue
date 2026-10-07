<template>
  <el-container class="layout">
    <el-aside width="200px" class="aside">
      <div class="logo">Quizzy</div>
      <el-menu :default-active="active" router>
        <el-menu-item index="/questions">题库</el-menu-item>
        <el-menu-item index="/papers">试卷</el-menu-item>
        <el-menu-item index="/quiz/quick">快速练习</el-menu-item>
        <el-menu-item index="/wrong-book">错题本</el-menu-item>
        <el-menu-item index="/history">答题记录</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ route.meta.title || '' }}</span>
        <div class="user-area">
          <el-dropdown trigger="click" @command="onUserCommand">
            <span class="user-trigger">
              <UserAvatar :user="store.user" :size="28" />
              <span>{{ store.user?.nickname || '未登录' }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">我的账户</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click" @command="onTheme">
            <el-button link type="primary">外观：{{ modeLabel }}</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="opt in THEME_OPTIONS" :key="opt.value" :command="opt.value">
                  {{ themeStore.mode === opt.value ? '✓ ' : '' }}{{ opt.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button link type="primary" @click="aboutVisible = true">关于</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <AboutDialog v-model:visible="aboutVisible" />
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import UserAvatar from '@/components/UserAvatar.vue'
import AboutDialog from '@/views/AboutDialog.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const themeStore = useThemeStore()

const aboutVisible = ref(false)

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

const active = computed(() => '/' + (route.path.split('/')[1] || 'questions'))

onMounted(() => {
  if (!store.user) store.loadUser()
})

function onLogout() {
  store.logout()
  router.push('/login')
}

/** 顶栏用户区的下拉：资料入口与登出都收在这儿，避免顶栏平铺太多按钮。 */
function onUserCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    onLogout()
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: var(--el-bg-color); border-right: 1px solid var(--el-border-color-lighter); }
.logo { padding: 18px 20px; font-size: 20px; font-weight: 600; color: var(--el-color-primary); }
.header { display: flex; align-items: center; justify-content: space-between; background: var(--el-bg-color); border-bottom: 1px solid var(--el-border-color-lighter); }
.title { font-size: 16px; font-weight: 500; }
.user-area { display: flex; align-items: center; gap: 12px; }
.user-trigger { display: inline-flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
</style>
