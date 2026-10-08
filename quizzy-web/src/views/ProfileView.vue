<template>
  <div class="max-w-[720px] font-sans text-base text-ink">
    <!-- 资料 -->
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <h1 class="text-base font-medium">资料</h1>

      <div class="mt-4 flex items-center gap-5">
        <UserAvatar :user="previewUser" :size="80" />
        <div class="min-w-0 flex-1">
          <p class="text-xs leading-relaxed text-ink-muted">
            没设头像时用按用户 id 生成的方块图，同一个账号每次生成的都是同一张。
            图片会在上传前压到最长边 {{ MAX_EDGE }}px，所以随便选一张原图即可。
          </p>
          <div class="mt-3 flex gap-2">
            <input ref="fileInput" class="hidden" type="file" accept="image/*" @change="onPickFile" />
            <Button variant="outline" :disabled="uploading" @click="fileInput?.click()">选择图片</Button>
            <Button variant="outline" :disabled="!avatar" @click="onUseDefaultAvatar">用默认头像</Button>
          </div>
        </div>
      </div>

      <div class="mt-5 flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label for="profile-nickname" class="text-xs text-ink-muted">昵称</label>
          <Input id="profile-nickname" v-model="nickname" maxlength="32" placeholder="昵称" class="bg-reader" />
        </div>
        <div class="flex flex-col gap-1">
          <label for="profile-username" class="text-xs text-ink-muted">登录名</label>
          <Input id="profile-username" :model-value="store.user?.username" disabled class="bg-surface-2" />
          <p class="text-xs leading-relaxed text-ink-muted">
            登录名不可修改——它和「以后可能改成邮箱作登录名」这件事绑在一起。
          </p>
        </div>
        <div>
          <Button :disabled="savingProfile" @click="onSaveProfile">保存资料</Button>
        </div>
      </div>
    </div>

    <!-- 安全 -->
    <div class="mt-4 rounded-xl bg-surface p-6 shadow-sm">
      <h1 class="text-base font-medium">安全</h1>

      <div class="mt-4 flex max-w-[460px] flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label for="pw-old" class="text-xs text-ink-muted">原密码</label>
          <Input
            id="pw-old"
            v-model="passwordForm.oldPassword"
            type="password"
            autocomplete="current-password"
            class="bg-reader"
          />
        </div>
        <div class="flex flex-col gap-1">
          <label for="pw-new" class="text-xs text-ink-muted">新密码</label>
          <Input
            id="pw-new"
            v-model="passwordForm.newPassword"
            type="password"
            autocomplete="new-password"
            placeholder="6-64 位"
            class="bg-reader"
          />
        </div>
        <div class="flex flex-col gap-1">
          <label for="pw-confirm" class="text-xs text-ink-muted">确认新密码</label>
          <Input
            id="pw-confirm"
            v-model="passwordForm.confirmPassword"
            type="password"
            autocomplete="new-password"
            class="bg-reader"
          />
        </div>
        <div class="flex items-center gap-3">
          <Button :disabled="changingPassword" @click="onChangePassword">修改密码</Button>
          <span class="text-xs text-ink-muted">改完密码，其他设备会被要求重新登录，这台设备不受影响。</span>
        </div>
      </div>

      <div class="my-5 border-t border-line-soft" />

      <!-- 退出登录（本设备）：原来挂在侧栏用户下拉里，2026-10-08 用户区改纯链接后挪到设置页 -->
      <div class="flex items-center justify-between gap-4">
        <div>
          <div class="text-sm font-medium">退出登录</div>
          <p class="mt-0.5 text-xs leading-relaxed text-ink-muted">仅退出这台设备，其他设备保持登录。</p>
        </div>
        <Button variant="outline" @click="onLogout">退出登录</Button>
      </div>

      <div class="mt-4 flex items-center justify-between gap-4">
        <div>
          <div class="text-sm font-medium">退出所有设备</div>
          <p class="mt-0.5 text-xs leading-relaxed text-ink-muted">包括这一台。用于「怀疑有别的设备拿着登录凭据」时。</p>
        </div>
        <Button variant="outline" @click="onLogoutAll">退出所有设备</Button>
      </div>

      <div class="mt-4 flex items-center justify-between gap-4 border-t border-line-soft pt-4">
        <div>
          <div class="text-sm font-medium text-ink-red">注销账号</div>
          <p class="mt-0.5 text-xs leading-relaxed text-ink-muted">永久删除账号、全部题目、试卷与作答记录，且不可恢复。</p>
        </div>
        <Button variant="destructive" :disabled="deleting" @click="onDeleteAccount">注销账号</Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { confirmBox, promptBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import UserAvatar from '@/components/UserAvatar.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
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
    toast.error(error instanceof Error ? error.message : '图片处理失败')
  } finally {
    uploading.value = false
  }
}

async function onSaveProfile() {
  const trimmed = nickname.value.trim()
  if (!trimmed) {
    toast.error('昵称不能为空')
    return
  }
  savingProfile.value = true
  try {
    const updated = await updateProfile(trimmed, avatar.value)
    store.setUser(updated)
    avatar.value = updated.avatar ?? null
    toast.success('资料已保存')
  } catch {
    // 失败提示已由 api/request 的 unwrap 统一弹出，这里不重复报。
  } finally {
    savingProfile.value = false
  }
}

async function onChangePassword() {
  const { oldPassword, newPassword, confirmPassword } = passwordForm.value
  if (!oldPassword) {
    toast.error('请输入原密码')
    return
  }
  if (newPassword.length < 6 || newPassword.length > 64) {
    toast.error('新密码长度为 6-64 个字符')
    return
  }
  if (newPassword !== confirmPassword) {
    toast.error('两次输入的新密码不一致')
    return
  }
  changingPassword.value = true
  try {
    const result = await changePassword(oldPassword, newPassword)
    // 后端只给当前设备换发新 token，本地要把凭据跟着换掉，否则下一次请求就 401。
    store.applyToken(result.token)
    store.setUser(result.user)
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    toast.success('密码已修改，其他设备需要重新登录')
  } catch {
    // 同上：提示已统一弹出。
  } finally {
    changingPassword.value = false
  }
}

async function onLogout() {
  store.logout()
  router.push('/login')
}

async function onLogoutAll() {
  const ok = await confirmBox({
    title: '退出所有设备',
    message: '所有设备（包括这一台）都会被退出，之后需要重新登录。继续吗？',
    confirmText: '退出所有设备',
    danger: true
  })
  if (!ok) return
  try {
    await logoutAllDevices()
  } catch {
    // 服务端很可能已经把人踢了，所以无论如何都回登录页。
  }
  store.logout()
  router.push('/login')
}

async function onDeleteAccount() {
  const password = await promptBox({
    title: '注销账号',
    message: '注销会永久删除账号、全部题目、试卷与作答记录，且无法撤销。请输入当前密码确认。',
    confirmText: '永久注销',
    inputType: 'password',
    placeholder: '当前密码',
    validator: (v) => (v ? true : '请输入当前密码'),
    danger: true
  })
  if (password === null) return

  deleting.value = true
  try {
    await deleteAccount(password)
    store.logout()
    toast.success('账号已注销')
    router.push('/login')
  } catch {
    // 密码不对等失败情形：提示已弹出，留在原页让他重试。
  } finally {
    deleting.value = false
  }
}
</script>
