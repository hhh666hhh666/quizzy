<template>
  <!--
    「收藏夹信息」面板：新建与编辑共用（`mode` 只决定标题与底按钮的字）。
    三条字段（名称 / 公开 / 简介）**永远整体提交**——后端按覆盖写回，不发某字段等于清掉它，
    这正是「能把简介清空」的实现方式（见 `api/favorite.ts` 的 FavoriteFolderSavePayload 注释）。

    ⚠️ 浮层材质是**玻璃**（`.glass`），与 ADR 0032 一致；ADR 0031 Amendment 1 的「浮层一律实底」已被它推翻。
    玻璃上不许放长正文（tokens.css），这里只有字段与短标签，合规。

    ⚠️ 宽度写 `sm:max-w-md`：`DialogContent` 内置 `sm:max-w-sm`（384px）带 `sm:` 修饰符，
    与 `max-w-*` 不同组、`twMerge` 合并不掉，必须显式覆盖（见 QuestionDetailDialog 的坑位注释）。
  -->
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="glass gap-5 border-0 p-5 sm:max-w-md">
      <DialogHeader>
        <DialogTitle class="text-base font-medium">{{ isEdit ? '收藏夹信息' : '新建收藏夹' }}</DialogTitle>
      </DialogHeader>

      <!-- 名称 -->
      <div class="flex flex-col gap-1.5">
        <label for="folder-info-name" class="text-sm text-ink">
          名称<span class="ml-0.5 text-danger">*</span>
        </label>
        <div class="relative">
          <Input
            id="folder-info-name"
            ref="nameInput"
            v-model="name"
            maxlength="20"
            class="bg-reader pr-14"
            :placeholder="'给收藏夹起个名字'"
            @keydown.enter.prevent="onSave"
          />
          <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs tabular-nums text-ink-muted">
            {{ name.length }} / 20
          </span>
        </div>
      </div>

      <!-- 是否公开 -->
      <div class="flex flex-col gap-1.5">
        <span class="text-sm text-ink">公开</span>
        <div class="flex items-center gap-2">
          <Switch v-model="isPublic" aria-label="是否公开这个收藏夹" />
          <span class="text-xs text-ink-muted">{{ isPublic ? '公开' : '仅自己可见' }}</span>
        </div>
        <!--
          ⚠️ 这句是**免责说明**不是功能说明：`is_public` 目前存而不用（ADR 0032），
          别写成「别人可以看到你的收藏夹」——那是现在还没有的行为。
        -->
        <p class="text-xs leading-relaxed text-ink-subtle">公开与否目前只是记录，暂不影响谁能看到它。</p>
      </div>

      <!-- 简介 -->
      <div class="flex flex-col gap-1.5">
        <label for="folder-info-intro" class="text-sm text-ink">简介</label>
        <div class="relative">
          <Textarea
            id="folder-info-intro"
            v-model="intro"
            maxlength="200"
            rows="3"
            class="min-h-[76px] resize-none bg-reader pb-6"
            placeholder="可以简单描述下你的收藏夹"
          />
          <span class="pointer-events-none absolute bottom-2 right-3 text-xs tabular-nums text-ink-muted">
            {{ intro.length }}/200
          </span>
        </div>
      </div>

      <DialogFooter class="border-0 bg-transparent p-0">
        <Button variant="outline" @click="emit('update:visible', false)">取消</Button>
        <Button :disabled="saving || !name.trim()" @click="onSave">{{ isEdit ? '保存' : '创建' }}</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { toast } from '@/lib/toast'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Switch } from '@/components/ui/switch'
import { Textarea } from '@/components/ui/textarea'
import { createFolder, updateFolder } from '@/api/favorite'
import type { FavoriteFolderVO } from '@/types'

/**
 * 新建 / 编辑收藏夹的玻璃面板。
 *
 * 传 `folder` = 编辑（保存走 `PUT`），不传 = 新建（保存走 `POST`）。
 * 默认收藏夹**也可以**从这里改（它不可删，但可改名 / 改简介）——后端不拦，前端也别拦。
 */
const props = defineProps<{
  visible: boolean
  folder?: FavoriteFolderVO | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const name = ref('')
const intro = ref('')
const isPublic = ref(false)
const saving = ref(false)
const nameInput = ref<InstanceType<typeof Input> | null>(null)

const isEdit = computed(() => !!props.folder)

/**
 * 每次打开时**用传进来的夹把表单复位**。
 *
 * ⚠️ 绑自己的 `visible`（不是第三方组件的 open 事件）——历史上 el-dialog 的 `@open` 在
 * 「首次挂载时 modelValue 已是 true」时不触发，会让首次打开永远读到空表单（ADR 0030 的回归哨兵）。
 */
watch(() => props.visible, (visible) => {
  if (!visible) {
    return
  }
  name.value = props.folder?.name ?? ''
  intro.value = props.folder?.intro ?? ''
  isPublic.value = props.folder?.isPublic ?? false
  void nextTick(() => nameInput.value?.$el?.focus?.())
}, { immediate: true })

async function onSave() {
  const trimmed = name.value.trim()
  if (!trimmed) {
    toast.warning('名字不能为空')
    return
  }
  saving.value = true
  try {
    if (props.folder) {
      await updateFolder(props.folder.id, { name: trimmed, intro: intro.value.trim(), isPublic: isPublic.value })
      toast.success('已保存')
    } else {
      await createFolder(trimmed, intro.value.trim(), isPublic.value)
      toast.success('已新建收藏夹')
    }
    emit('update:visible', false)
    emit('saved')
  } catch {
    // 失败提示由 api/request 的 unwrap 统一弹出
  } finally {
    saving.value = false
  }
}
</script>
