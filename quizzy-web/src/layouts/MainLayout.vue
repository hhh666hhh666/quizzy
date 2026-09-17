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
          <span>{{ store.user?.nickname || '未登录' }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const active = computed(() => '/' + (route.path.split('/')[1] || 'questions'))

onMounted(() => {
  if (!store.user) store.loadUser()
})

function onLogout() {
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #fff; border-right: 1px solid #ebeef5; }
.logo { padding: 18px 20px; font-size: 20px; font-weight: 600; color: #409eff; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ebeef5; }
.title { font-size: 16px; font-weight: 500; }
.user-area { display: flex; align-items: center; gap: 12px; }
</style>
