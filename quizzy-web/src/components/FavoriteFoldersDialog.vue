<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="420px"
    @update:model-value="emit('update:visible', $event)"
    @open="onOpen"
  >
    <p class="hint">{{ hint }}</p>
    <div v-loading="loading">
      <el-checkbox-group v-model="checked">
        <div v-for="folder in folders" :key="folder.id" class="folder-row">
          <el-checkbox :value="folder.id">{{ folder.name }}</el-checkbox>
          <span v-if="folder.isDefault" class="tag">默认</span>
        </div>
      </el-checkbox-group>
      <el-empty v-if="!loading && folders.length === 0" description="还没有收藏夹" :image-size="60" />
      <el-button v-if="!loading && folders.length === 0" link type="primary" @click="onCreateQuick">
        新建一个收藏夹
      </el-button>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addQuestionsToFolder, createFolder, listFolders, setQuestionFolders } from '@/api/favorite'
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
 * 1. **不预勾当前状态**——列表永远从全空开始，让主人主动表态；
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
const loading = ref(false)
const saving = ref(false)

const title = computed(() => (props.mode === 'add' ? '加入收藏夹' : '修改收藏夹'))

const hint = computed(() =>
  props.mode === 'add'
    ? `勾选的收藏夹都会被加入（已有的会自动跳过，不会有重复）；共 ${props.questionIds?.length ?? 0} 道题。`
    : '勾选后这道题就属于这些收藏夹；一个都不勾 = 留在默认收藏夹（不是取消收藏）。'
)

async function onOpen() {
  checked.value = []
  loading.value = true
  try {
    folders.value = await listFolders()
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

<style scoped>
.hint {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.folder-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.tag {
  font-size: 12px;
  padding: 0 6px;
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}
</style>
