<template>
  <!--
    ⚠️ 数据加载绑在**自己的 visible** 上（见下面的 watch），不要改挂到第三方组件的事件上——
    历史上 el-dialog 的 `@open` 在「首次挂载时 modelValue 已是 true」时不触发，导致首次打开
    永远谎报「还没有收藏夹」（ADR 0030 的回归哨兵测试就钉着这条）。现在的 reka Dialog
    默认走 portal，原来那段「必须 append-to-body，否则表格行会盖到面板上」的坑随组件一起退场。
  -->
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="max-w-md">
      <DialogHeader>
        <DialogTitle class="text-base font-medium">{{ title }}</DialogTitle>
      </DialogHeader>

      <p class="text-sm leading-relaxed text-ink">
        {{ hint }}
        <template v-if="mode === 'set' && defaultFolderName">
          一个都不勾 = 收进「{{ defaultFolderName }}」，想彻底取消收藏请点星标。
        </template>
      </p>

      <!--
        只读的一行「现在在」。
        ⚠️ 面板**不预勾**（ADR 0030：勾选要出于主人的主动表态），但**不预勾不等于对现状闭口不谈**——
        一道明明已收藏的题打开面板看到一片空白，看着就像在说谎。所以这里把事实摆出来，勾选仍是纯粹的主动动作。
      -->
      <div v-if="mode === 'set'" class="flex flex-wrap items-center gap-1.5 rounded-md bg-surface-2 px-3 py-2 text-sm">
        <span class="text-ink-muted">现在在</span>
        <template v-if="currentNames.length">
          <span v-for="name in currentNames" :key="name" class="rounded-sm bg-surface px-2 py-0.5 text-xs text-ink">{{ name }}</span>
        </template>
        <span v-else class="text-ink-muted">还没被收藏</span>
      </div>

      <div class="max-h-[300px] overflow-y-auto" :class="loading ? 'pointer-events-none opacity-60' : ''">
        <label
          v-for="folder in folders"
          :key="folder.id"
          class="folder-row flex cursor-pointer items-center gap-2 rounded-md px-1.5 py-1.5 hover:bg-surface-2"
        >
          <Checkbox
            :model-value="checked.includes(folder.id)"
            @update:model-value="(v: unknown) => toggleFolder(folder.id, v === true)"
          />
          <span>{{ folder.name }}</span>
          <span v-if="folder.isDefault" class="rounded-sm bg-surface-2 px-1.5 py-0.5 text-xs text-ink-muted">默认</span>
        </label>
        <p v-if="!loading && folders.length === 0" class="px-2 py-4 text-center text-sm text-ink-muted">还没有收藏夹</p>
      </div>

      <!--
        ⚠️ 这个入口要**一直在**，不能在空态时才出现：「想归类时才发现没有合适的夹」正是最需要它的一刻，
        而那一刻恰恰是**已经有别的夹**的时候（早先只在空态显示，等于把入口藏在了最不需要它的地方）。
      -->
      <Button variant="link" size="sm" class="self-start" @click="onCreateQuick">＋ 新建收藏夹</Button>

      <DialogFooter>
        <Button variant="outline" @click="emit('update:visible', false)">取消</Button>
        <Button :disabled="saving" @click="onConfirm">确定</Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import {
  addQuestionsToFolder,
  createFolder,
  getQuestionFolders,
  listFolders,
  setQuestionFolders
} from '@/api/favorite'
import type { FavoriteFolderVO } from '@/types'

/**
 * 收藏夹选择面板。**两种模式共用同一个面板**，差别只在「确定之后做什么」：
 *
 * - `set`（单题）：**覆盖**——勾谁这道题就属于谁，没勾的夹会被移出；
 * - `add`（批量）：**只加不减**——题库页批量条用，勾谁就加进谁，一道也不移出。
 *
 * ⚠️ 为什么批量不做成覆盖：批量下「没勾的夹」动辄涉及几十道题的归属，一次点击就能静默改掉一片，
 * 那不是主人按下按钮时以为会发生的事。两个入口的名字也不同（「修改」对「加入」）。
 *
 * 两条来自 `docs/adr/0030` 的固定行为：
 * 1. **不预勾当前状态**——列表永远从全空开始，让主人主动表态（但现状会以只读的一行「现在在」摆出来）；
 * 2. `set` 模式下**空选 = 留在默认收藏夹**，不是取消收藏（那样一次手滑就丢收藏）。
 */
const props = withDefaults(defineProps<{
  visible: boolean
  mode?: 'set' | 'add'
  questionId?: number
  questionIds?: number[]
}>(), { mode: 'set', questionId: undefined, questionIds: undefined })

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved'): void
}>()

const folders = ref<FavoriteFolderVO[]>([])
const checked = ref<number[]>([])
const currentFolderIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)

const title = computed(() => (props.mode === 'add' ? '加入收藏夹' : '修改收藏夹'))

/** 默认夹的**当前名字**。⚠️ 「默认收藏夹」这几个字不许写死进文案——它可以被改名（ADR 0030）。 */
const defaultFolderName = computed(() => folders.value.find((folder) => folder.isDefault)?.name)

const currentNames = computed(() =>
  currentFolderIds.value
    .map((id) => folders.value.find((folder) => folder.id === id)?.name)
    .filter((name): name is string => !!name)
)

const hint = computed(() =>
  props.mode === 'add'
    ? `勾选的收藏夹都会被加入（已有的会自动跳过，不会有重复）；共 ${props.questionIds?.length ?? 0} 道题。`
    : '勾谁就属于谁（没勾的会被移出）。'
)

/** 打开时加载（绑定自己的 visible，别依赖第三方组件的事件语义——见模板顶部注释）。 */
watch(() => props.visible, (visible) => {
  if (!visible) {
    return
  }
  checked.value = []
  void loadFolders()
}, { immediate: true })

function toggleFolder(id: number, isChecked: boolean) {
  checked.value = isChecked ? [...checked.value, id] : checked.value.filter((v) => v !== id)
}

async function loadFolders() {
  loading.value = true
  try {
    const [list, current] = await Promise.all([
      listFolders(),
      props.mode === 'set' && props.questionId != null
        ? getQuestionFolders(props.questionId)
        : Promise.resolve<number[]>([])
    ])
    folders.value = list
    currentFolderIds.value = current
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  saving.value = true
  try {
    if (props.mode === 'add') {
      const ids = props.questionIds ?? []
      if (!checked.value.length) {
        ElMessage.warning('请先勾一个收藏夹')
        return
      }
      for (const folderId of checked.value) {
        await addQuestionsToFolder(folderId, ids)
      }
      ElMessage.success(`已把 ${ids.length} 道题加入 ${checked.value.length} 个收藏夹`)
    } else if (props.questionId != null) {
      await setQuestionFolders(props.questionId, checked.value)
      ElMessage.success(checked.value.length ? '已改好所属收藏夹' : '已留在默认收藏夹')
    }
    emit('update:visible', false)
    emit('saved')
  } catch {
    // 失败提示已由 api/request 的 unwrap 统一弹出
  } finally {
    saving.value = false
  }
}

/** 面板里顺手建一个夹——「想归类时才发现没有合适的夹」正是最常发生的一刻。 */
async function onCreateQuick() {
  try {
    const { value } = await ElMessageBox.prompt('给新收藏夹起个名字', '新建收藏夹', {
      confirmButtonText: '新建',
      cancelButtonText: '取消',
      inputPlaceholder: '最多 20 个字',
      inputValidator: (input: string) => (input && input.trim() ? true : '名字不能为空')
    })
    const folder = await createFolder(value.trim())
    folders.value = [...folders.value, folder]
    checked.value = [...checked.value, folder.id]
  } catch {
    // 取消或失败都不影响面板
  }
}
</script>
