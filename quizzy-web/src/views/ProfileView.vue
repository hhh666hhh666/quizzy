<template>
  <div class="profile">
    <el-card class="block">
      <template #header>资料</template>

      <div class="avatar-row">
        <UserAvatar :user="previewUser" :size="80" />
        <div class="avatar-side">
          <p class="hint">
            没设头像时用按用户 id 生成的方块图，同一个账号每次生成的都是同一张。
            图片会在上传前压到最长边 {{ MAX_EDGE }}px，所以随便选一张原图即可。
          </p>
          <div class="avatar-buttons">
            <input ref="fileInput" class="file-input" type="file" accept="image/*" @change="onPickFile" />
            <el-button :loading="uploading" @click="fileInput?.click()">选择图片</el-button>
            <el-button :disabled="!avatar" @click="onUseDefaultAvatar">用默认头像</el-button>
          </div>
        </div>
      </div>

      <el-form label-width="72px" @submit.prevent>
        <el-form-item label="昵称">
          <el-input v-model="nickname" maxlength="32" show-word-limit placeholder="昵称" />
        </el-form-item>
        <el-form-item label="登录名">
          <el-input :model-value="store.user?.username" disabled />
          <p class="hint">登录名不可修改——它和「以后可能改成邮箱作登录名」这件事绑在一起。</p>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="onSaveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="block">
      <template #header>安全</template>

      <el-form label-width="72px" @submit.prevent>
        <el-form-item label="原密码">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="6-64 位"
          />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPassword" @click="onChangePassword">修改密码</el-button>
          <span class="hint">改完密码，其他设备会被要求重新登录，这台设备不受影响。</span>
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="action-row">
        <div>
          <div class="action-title">退出所有设备</div>
          <p class="hint">包括这一台。用于「怀疑有别的设备拿着登录凭据」时。</p>
        </div>
        <el-button @click="onLogoutAll">退出所有设备</el-button>
      </div>

      <div class="action-row">
        <div>
          <div class="action-title danger">注销账号</div>
          <p class="hint">永久删除账号、全部题目、试卷与作答记录，且不可恢复。</p>
        </div>
        <el-button type="danger" @click="onDeleteAccount">注销账号</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserAvatar from '@/components/UserAvatar.vue'
import { changePassword, deleteAccount, logoutAllDevices, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { toAvatarDataUrl } from '@/utils/avatar'
import type { UserVO } from '@/types'

const MAX_EDGE = 256

const router = useRouter()
const store = useUserStore()

const fileInput = ref<HTMLInputElement | null>(null)
const nickname = ref('')
const avatar = ref<string | null>(null)
const uploading = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)
const deleting = ref(false)

const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

/** 预览要跟着「还没保存的改动」走，不能只看服务端已经存下的那份。 */
const previewUser = computed<UserVO | null>(() => {
  if (!store.user) return null
  return { ...store.user, avatar: avatar.value ?? undefined }
})

// 直接打开 /profile 时 store 可能还没拉过用户，自己兜一下。
onMounted(() => {
  if (!store.user) store.loadUser()
})

watch(
  () => store.user,
  (user) => {
    if (!user) return
    nickname.value = user.nickname
    avatar.value = user.avatar ?? null
  },
  { immediate: true }
)

function onUseDefaultAvatar() {
  avatar.value = null
}

async function onPickFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  // 先取文件再清空，否则连续选同一张图不会再触发 change。
  input.value = ''
  if (!file) return
  uploading.value = true
  try {
    avatar.value = await toAvatarDataUrl(file)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '图片处理失败')
  } finally {
    uploading.value = false
  }
}

async function onSaveProfile() {
  const trimmed = nickname.value.trim()
  if (!trimmed) {
    ElMessage.error('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const updated = await updateProfile(trimmed, avatar.value)
    store.setUser(updated)
    avatar.value = updated.avatar ?? null
    ElMessage.success('资料已保存')
  } catch {
    // 失败提示已由 api/request 的 unwrap 统一弹出，这里不重复报。
  } finally {
    savingProfile.value = false
  }
}

async function onChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword) {
    ElMessage.error('请输入原密码')
    return
  }
  if (newPassword.length < 6 || newPassword.length > 64) {
    ElMessage.error('新密码长度为 6-64 个字符')
    return
  }
  if (newPassword !== confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    const result = await changePassword(oldPassword, newPassword)
    // 后端只给当前设备换发新 token，本地要把凭据跟着换掉，否则下一次请求就 401。
    store.applyToken(result.token)
    store.setUser(result.user)
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    ElMessage.success('密码已修改，其他设备需要重新登录')
  } catch {
    // 同上：提示已统一弹出。
  } finally {
    changingPassword.value = false
  }
}

async function onLogoutAll() {
  try {
    await ElMessageBox.confirm('所有设备（包括这一台）都会被退出，之后需要重新登录。继续吗？', '退出所有设备', {
      confirmButtonText: '退出所有设备',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await logoutAllDevices()
  } catch {
    // 服务端很可能已经把人踢了，所以无论如何都回登录页。
  }
  store.logout()
  router.push('/login')
}

async function onDeleteAccount() {
  let password = ''
  try {
    const result = await ElMessageBox.prompt(
      '注销会永久删除账号、全部题目、试卷与作答记录，且无法撤销。请输入当前密码确认。',
      '注销账号',
      {
        confirmButtonText: '永久注销',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '当前密码',
        inputValidator: (value: string) => (value ? true : '请输入当前密码'),
        type: 'warning'
      }
    )
    password = result.value
  } catch {
    return
  }

  deleting.value = true
  try {
    await deleteAccount(password)
    store.logout()
    ElMessage.success('账号已注销')
    router.push('/login')
  } catch {
    // 密码不对等失败情形：提示已弹出，留在原页让他重试。
  } finally {
    deleting.value = false
  }
}
</script>

<style scoped>
.profile {
  max-width: 720px;
}

.block + .block {
  margin-top: 16px;
}

.avatar-row {
  display: flex;
  gap: 20px;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-side {
  flex: 1;
  min-width: 0;
}

.avatar-buttons {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.file-input {
  display: none;
}

.hint {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.avatar-side .hint {
  margin-top: 0;
}

.action-row {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
}

.action-row + .action-row {
  margin-top: 16px;
}

.action-title {
  font-size: 14px;
  font-weight: 500;
}

.action-title.danger {
  color: var(--el-color-danger);
}
</style>
