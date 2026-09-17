<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>Quizzy 答题</h2>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="form.username" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="form" label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="form.username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
            <el-form-item label="昵称（可留空）">
              <el-input v-model="form.nickname" />
            </el-form-item>
            <el-button type="primary" class="submit" :loading="loading" @click="onRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const store = useUserStore()
const tab = ref('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

async function onLogin() {
  loading.value = true
  try {
    await store.login(form.username, form.password)
    router.push('/questions')
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  loading.value = true
  try {
    await store.register(form.username, form.password, form.nickname)
    router.push('/questions')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; align-items: center; justify-content: center; height: 100%; }
.login-card { width: 380px; }
h2 { text-align: center; margin: 0 0 20px; color: #409eff; }
.submit { width: 100%; }
</style>
