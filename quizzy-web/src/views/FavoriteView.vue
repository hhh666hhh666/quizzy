<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>收藏夹</span>
        <div class="header-actions">
          <span class="hint-inline">将练习：{{ currentName }}</span>
          <el-input-number v-model="count" :min="1" :max="100" size="small" />
          <el-button type="primary" size="small" :loading="starting" @click="onPractice">
            用收藏的题练习
          </el-button>
        </div>
      </div>
    </template>

    <div class="body">
      <aside class="side">
        <el-button class="new-folder" size="small" @click="onCreateFolder">新建收藏夹</el-button>
        <ul class="folder-list">
          <li class="folder-item" :class="{ active: selected === null }" @click="select(null)">
            <span class="folder-name">全部收藏</span>
          </li>
          <li
            v-for="folder in folders"
            :key="folder.id"
            class="folder-item"
            :class="{ active: selected === folder.id }"
            @click="select(folder.id)"
          >
            <span class="folder-name" :title="folder.name">
              {{ folder.name }}
              <span v-if="folder.isDefault" class="tag">默认</span>
            </span>
            <span class="folder-count">{{ folder.questionCount }}</span>
            <span class="folder-actions">
              <el-button link type="primary" size="small" @click.stop="onRenameFolder(folder)">
                改名
              </el-button>
              <el-button
                v-if="!folder.isDefault"
                link
                type="danger"
                size="small"
                @click.stop="onDeleteFolder(folder)"
              >
                删除
              </el-button>
            </span>
          </li>
        </ul>
      </aside>

      <div class="main">
        <el-table :data="rows" v-loading="loading" border>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column label="题型" width="80">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" min-width="240" show-overflow-tooltip />
          <el-table-column label="分类" width="110">
            <template #default="{ row }">{{ row.categoryName || '-' }}</template>
          </el-table-column>
          <el-table-column label="收藏" width="60" align="center">
            <template #default="{ row }">
              <FavoriteStar :question-id="row.id" :favorited="row.favorited" @change="onStarChange" />
            </template>
          </el-table-column>
          <el-table-column v-if="selected !== null" label="操作" width="110">
            <template #default="{ row }">
              <el-button link type="danger" @click="onRemoveFromFolder(row)">移出此夹</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          class="pagination"
          layout="total, prev, pager, next"
          :total="total"
          v-model:current-page="page"
          v-model:page-size="size"
          @change="loadQuestions"
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FavoriteStar from '@/components/FavoriteStar.vue'
import {
  createFolder,
  deleteFolder,
  listFolders,
  practiceFavorites,
  removeFromFolder,
  renameFolder
} from '@/api/favorite'
import { pageQuestions } from '@/api/question'
import type { FavoriteFolderVO, QuestionListItemVO, QuestionQuery } from '@/types'

/**
 * 收藏夹页面：左边是夹、右边是这个夹里的题。
 *
 * 两条与状态有关的取值，改之前先看 `docs/adr/0030`：
 * - `selected === null` 表示**「全部收藏」**（跨夹、不重复计），它不是一个真实的夹；
 * - 夹列表的顺序由后端给（按「最近有新题进来」倒序），前端不再排一次。
 */
const router = useRouter()

const folders = ref<FavoriteFolderVO[]>([])
const selected = ref<number | null>(null)
const rows = ref<QuestionListItemVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const count = ref(20)
const loading = ref(false)
const starting = ref(false)

const currentName = computed(() => {
  if (selected.value === null) {
    return '全部收藏'
  }
  return folders.value.find((folder) => folder.id === selected.value)?.name || '这个收藏夹'
})

async function loadFolders() {
  folders.value = await listFolders()
  // 选中的夹可能刚被删掉，退回「全部收藏」，否则右边会一直空着且没有解释
  if (selected.value !== null && !folders.value.some((folder) => folder.id === selected.value)) {
    selected.value = null
  }
}

async function loadQuestions() {
  loading.value = true
  try {
    const query: QuestionQuery = { page: page.value, size: size.value }
    if (selected.value === null) {
      query.anyFavorite = true
    } else {
      query.favoriteFolderIds = [selected.value]
    }
    const result = await pageQuestions(query)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  await loadFolders()
  await loadQuestions()
}

function select(folderId: number | null) {
  if (selected.value === folderId) {
    return
  }
  selected.value = folderId
  page.value = 1
  loadQuestions()
}

async function onCreateFolder() {
  try {
    const { value } = await ElMessageBox.prompt('给新收藏夹起个名字', '新建收藏夹', {
      confirmButtonText: '新建',
      cancelButtonText: '取消',
      inputPlaceholder: '最多 20 个字',
      inputValidator: (input: string) => (input && input.trim() ? true : '名字不能为空')
    })
    await createFolder(value.trim())
    await loadFolders()
  } catch {
    // 取消或失败都不影响页面
  }
}

async function onRenameFolder(folder: FavoriteFolderVO) {
  try {
    const { value } = await ElMessageBox.prompt('改成什么名字？', '收藏夹改名', {
      confirmButtonText: '改名',
      cancelButtonText: '取消',
      inputValue: folder.name,
      inputValidator: (input: string) => (input && input.trim() ? true : '名字不能为空')
    })
    await renameFolder(folder.id, value.trim())
    await loadFolders()
  } catch {
    // 取消或失败都不影响页面
  }
}

async function onDeleteFolder(folder: FavoriteFolderVO) {
  try {
    await ElMessageBox.confirm(
      `删除「${folder.name}」后，里面的题会从它这里移出；` +
        '如果某道题不再属于任何收藏夹，它就不再是收藏了。继续吗？',
      '删除收藏夹',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await deleteFolder(folder.id)
  ElMessage.success('已删除收藏夹')
  await loadAll()
}

/**
 * 星标变了就整页重取。
 *
 * ⚠️ 不要改成「只改这一行」：取消收藏是从**所有**夹移出，这一行很可能已经不该出现在当前列表里
 * （尤其正在看某个具体夹时）——就地改状态会留下一个「明明不在这个夹里却还列着」的行。
 */
async function onStarChange() {
  await loadFolders()
  await loadQuestions()
}

async function onRemoveFromFolder(row: QuestionListItemVO) {
  if (selected.value === null) {
    return
  }
  await removeFromFolder(selected.value, row.id)
  ElMessage.success('已移出这个收藏夹')
  await onStarChange()
}

async function onPractice() {
  starting.value = true
  try {
    const sessionId = await practiceFavorites(selected.value, count.value)
    router.push(`/quiz/${sessionId}`)
  } catch (e: any) {
    ElMessage.error(e.message || '这个收藏夹里还没有题')
  } finally {
    starting.value = false
  }
}

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

onMounted(loadAll)
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hint-inline {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.side {
  flex: none;
  width: 220px;
}

.new-folder {
  width: 100%;
  margin-bottom: 8px;
}

.folder-list {
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
}

.folder-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.folder-item:last-child {
  border-bottom: none;
}

.folder-item:hover {
  background: var(--el-fill-color-light);
}

.folder-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.folder-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-count {
  flex: none;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 操作按钮平时让位给「名字 + 数量」，指到哪一行才出现 */
.folder-actions {
  flex: none;
  display: none;
}

.folder-item:hover .folder-actions {
  display: inline-flex;
}

.tag {
  margin-left: 4px;
  padding: 0 4px;
  font-size: 11px;
  border-radius: 3px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color);
}

.main {
  flex: 1;
  min-width: 0;
}

.pagination {
  margin-top: 12px;
  justify-content: flex-end;
}
</style>
