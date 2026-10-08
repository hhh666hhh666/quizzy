<template>
  <div class="font-sans text-base text-ink">
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-base font-medium">收藏夹</h1>
        <div class="flex items-center gap-2">
          <span class="text-xs text-ink-muted">将练习：{{ currentName }}</span>
          <Input v-model.number="count" type="number" min="1" max="100" aria-label="练习题数" class="h-8 w-20 bg-reader" />
          <Button size="sm" :disabled="starting" @click="onPractice">用收藏的题练习</Button>
        </div>
      </div>

      <div class="flex items-start gap-4">
        <!-- 左：夹列表。⚠️ `.folder-item` 是 e2e 的钩子，别改名（12 号 spec 在用） -->
        <aside class="w-[220px] shrink-0">
          <Button variant="outline" class="mb-2 w-full" @click="onCreateFolder">新建收藏夹</Button>
          <ul class="overflow-hidden rounded-lg border border-line-soft py-1">
            <li
              class="folder-item group flex cursor-pointer items-center gap-1.5 px-3 py-2 text-sm"
              :class="selected === null ? 'bg-brand-soft font-medium text-ink-blue' : 'text-ink hover:bg-surface'"
              @click="select(null)"
            >
              <span class="min-w-0 flex-1 truncate">全部收藏</span>
            </li>
            <li
              v-for="folder in folders"
              :key="folder.id"
              class="folder-item group flex cursor-pointer items-center gap-1.5 px-3 py-2 text-sm"
              :class="selected === folder.id ? 'bg-brand-soft font-medium text-ink-blue' : 'text-ink hover:bg-surface'"
              @click="select(folder.id)"
            >
              <span class="min-w-0 flex-1 truncate" :title="folder.name">
                {{ folder.name }}
                <span v-if="folder.isDefault" class="ml-1 rounded-sm bg-surface-2 px-1 py-0.5 text-2xs text-ink-muted">默认</span>
              </span>
              <span class="shrink-0 text-xs text-ink-muted">{{ folder.questionCount }}</span>
              <span class="hidden shrink-0 items-center gap-1.5 group-hover:flex group-focus-within:flex">
                <button type="button" class="text-xs text-brand hover:underline" @click.stop="onRenameFolder(folder)">改名</button>
                <button
                  v-if="!folder.isDefault"
                  type="button"
                  class="text-xs text-danger hover:underline"
                  @click.stop="onDeleteFolder(folder)"
                >
                  删除
                </button>
              </span>
            </li>
          </ul>
        </aside>

        <!-- 右：这个夹里的题 -->
        <div class="min-w-0 flex-1">
          <div class="overflow-x-auto rounded-lg border border-line-soft" :class="loading ? 'pointer-events-none opacity-60' : ''">
            <table class="w-full border-collapse text-sm">
              <thead>
                <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
                  <th class="px-3 py-2.5 font-medium">ID</th>
                  <th class="px-3 py-2.5 font-medium">题型</th>
                  <th class="px-3 py-2.5 font-medium">题干</th>
                  <th class="px-3 py-2.5 font-medium">分类</th>
                  <th class="px-3 py-2.5 font-medium">收藏时间</th>
                  <th class="px-3 py-2.5 text-center font-medium">收藏</th>
                  <th v-if="selected !== null" class="px-3 py-2.5 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in rows" :key="row.id" class="fav-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
                  <td class="px-3 py-2.5 text-ink-muted">{{ row.id }}</td>
                  <td class="px-3 py-2.5"><TypeTag :type="row.type" /></td>
                  <td class="max-w-0 px-3 py-2.5">
                    <span class="block truncate" :title="row.stem">{{ row.stem }}</span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2.5">{{ row.categoryName || '-' }}</td>
                  <td class="whitespace-nowrap px-3 py-2.5 text-ink-muted">{{ formatTime(row.favoritedAt) }}</td>
                  <td class="px-3 py-2.5 text-center">
                    <FavoriteStar :question-id="row.id" :favorited="row.favorited" @change="onStarChange" />
                  </td>
                  <td v-if="selected !== null" class="whitespace-nowrap px-3 py-2.5">
                    <button type="button" class="text-danger hover:underline" @click="onRemoveFromFolder(row)">移出此夹</button>
                  </td>
                </tr>
                <tr v-if="!rows.length">
                  <td :colspan="selected !== null ? 7 : 6" class="px-4 py-8 text-center text-sm leading-loose text-ink-muted">
                    这个收藏夹里还没有题。去「题库」用星标或「加入收藏夹」把题收进来吧。
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <TablePagination :total="total" :page="page" :size="size" @update:page="(p) => { page = p; loadQuestions() }" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import FavoriteStar from '@/components/FavoriteStar.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import TablePagination from '@/components/TablePagination.vue'
import TypeTag from '@/components/TypeTag.vue'
import {
  createFolder,
  deleteFolder,
  listFolders,
  pageFavoriteQuestions,
  practiceFavorites,
  removeFromFolder,
  renameFolder
} from '@/api/favorite'
import type { FavoriteFolderVO, QuestionListItemVO } from '@/types'

/**
 * 收藏夹页面：左边是夹、右边是这个夹里的题。
 *
 * 三条与状态有关的取值，改之前先看 `docs/adr/0030`：
 * - `selected === null` 表示**「全部收藏」**（跨夹、不重复计），它不是一个真实的夹；
 * - 夹列表的顺序由后端给（按「最近有新题进来」倒序），前端不再排一次；
 * - 右列的题也由后端按**最近收藏的排最前**给（`GET /api/favorites/questions`，不是题库列表接口），
 *   前端同样不再排一次——**别在这张表上加 `sortable`**，那只会排当前这一页，与服务端排序打架。
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
    const result = await pageFavoriteQuestions(selected.value, page.value, size.value)
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

/**
 * 收藏时间只做「变得好读」，**不做时区换算**：后端已经固定在 Asia/Shanghai 输出，
 * 这里把它变成 `2026-10-07 13:45`。顺手兼容两种写法——ISO 的 `T` 分隔与空格分隔。
 */
function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(loadAll)
</script>
