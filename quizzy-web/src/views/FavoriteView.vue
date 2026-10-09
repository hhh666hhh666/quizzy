<template>
  <div class="font-sans text-base text-ink">
    <!--
      两块面 + 两块面之间留一条**页面底色**的缝（v3 构图）：
      左 = 蓝调面板（**按内容自然高度**，不铺满），右 = 近白卡片（吃满剩余高度）。
      ⚠️ 别用 items-stretch——那样左栏会被拉到与右卡等高，正是要避免的。
    -->
    <div class="flex items-start gap-6">
      <!--
        左：夹列表。
        ⚠️ `.folder-item` 是 e2e 的钩子，别改名（12 号 spec 在用）。
        ⚠️ 面板背景是 `bg-tint-blue`（品牌浅蓝面），**不是** `bg-surface`——它是「有色区块」，与右卡等地位。
      -->
      <aside class="w-[240px] shrink-0 rounded-xl bg-tint-blue p-3">
        <div class="mb-2 flex items-center justify-between gap-2 px-1">
          <h1 class="text-sm font-medium">收藏夹</h1>
          <!-- ⚠️ 新建入口是「静的次要控件」，**不是**实色主按钮：一页只有一个主色实底（右上的「用收藏的题练习」）。 -->
          <Button variant="outline" size="xs" class="bg-surface/70" @click="onCreateFolder">
            <PlusIcon class="size-3" />
            新建收藏夹
          </Button>
        </div>

        <ul class="flex flex-col gap-0.5">
          <li
            v-for="item in folderItems"
            :key="item.key"
            class="folder-item group relative flex cursor-pointer items-center gap-1.5 rounded-md px-2.5 py-2 text-sm"
            :class="
              selected === item.id
                ? 'bg-surface font-medium text-ink-blue shadow-sm before:absolute before:inset-y-1.5 before:left-0 before:w-[3px] before:rounded-full before:bg-brand'
                : 'text-ink hover:bg-surface/50'
            "
            @click="select(item.id)"
          >
            <span class="min-w-0 flex-1 truncate" :title="item.name">
              {{ item.name }}
              <span
                v-if="item.isDefault"
                class="ml-1 rounded-sm bg-surface/70 px-1 py-0.5 text-2xs text-ink-muted"
              >默认</span>
            </span>
            <span class="shrink-0 text-xs tabular-nums text-ink-muted">{{ item.questionCount }}</span>

            <!--
              ⚠️ ⋯ 触发键**不能只在 hover 时才出现**：只有鼠标的人 hover 得到，键盘与触屏的用户够不着。
              做法 = 常驻 DOM（保证可聚焦、可被读屏读到），但**平时透明度为 0**，hover / focus-within / 触摸
              （`@media (hover: none)` 下由 CSS 强制常显）才浮现。`全部收藏`不是真实的夹，不给 ⋯。
            -->
            <DropdownMenu v-if="item.id !== null">
              <DropdownMenuTrigger as-child>
                <button
                  type="button"
                  class="fav-more shrink-0 rounded-sm p-0.5 text-ink-subtle opacity-0 transition-opacity hover:text-ink focus-visible:opacity-100 focus-visible:ring-2 focus-visible:ring-focus group-hover:opacity-100"
                  :aria-label="`${item.name} 的操作`"
                  @click.stop
                >
                  <MoreHorizontalIcon class="size-4" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" class="w-32">
                <DropdownMenuItem @click="onEditFolder(item.raw!)">编辑信息</DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem variant="destructive" @click="onDeleteFolder(item.raw!)">删除</DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </li>
        </ul>

        <!-- 一个夹都没有时的空态：面板还是要给「新建」这个出口，否则整块是死的 -->
        <p v-if="!folders.length" class="px-1 py-2 text-xs leading-relaxed text-ink-muted">
          还没有自己的收藏夹。点上面的「新建收藏夹」建一个吧。
        </p>
      </aside>

      <!-- 右：这个夹里的题。这块吃满剩余宽度与高度，与左栏的「自然高度」形成对比 -->
      <div class="flex min-h-[calc(100vh-9.5rem)] min-w-0 flex-1 flex-col rounded-xl bg-surface p-5 shadow-sm">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-sm font-medium">{{ currentName }}</h2>
          <div class="flex items-center gap-2">
            <span class="text-xs text-ink-muted">将练习：{{ currentName }}</span>
            <Input v-model.number="count" type="number" min="1" max="100" aria-label="练习题数" class="h-8 w-20 bg-reader" />
            <Button size="sm" :disabled="starting" @click="onPractice">用收藏的题练习</Button>
          </div>
        </div>

        <div
          class="overflow-x-auto rounded-lg border border-line-soft"
          :class="loading ? 'pointer-events-none opacity-60' : ''"
        >
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
              <tr
                v-for="row in rows"
                :key="row.id"
                class="fav-row border-b border-line-soft transition-colors hover:bg-surface-2/50"
              >
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
                  <button type="button" class="link-button text-danger hover:underline" @click="onRemoveFromFolder(row)">
                    移出此夹
                  </button>
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

        <TablePagination
          :total="total"
          :page="page"
          :size="size"
          class="mt-auto pt-4"
          @update:page="(p: number) => { page = p; loadQuestions() }"
        />
      </div>
    </div>

    <!-- 收藏夹信息面板（新建 / 编辑共用，玻璃浮层） -->
    <FolderInfoDialog v-model:visible="infoVisible" :folder="editingFolder" @saved="onFolderSaved" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { MoreHorizontalIcon, PlusIcon } from '@lucide/vue'
import { confirmBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import FavoriteStar from '@/components/FavoriteStar.vue'
import FolderInfoDialog from '@/components/FolderInfoDialog.vue'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import TablePagination from '@/components/TablePagination.vue'
import TypeTag from '@/components/TypeTag.vue'
import { deleteFolder, listFolders, pageFavoriteQuestions, practiceFavorites, removeFromFolder } from '@/api/favorite'
import type { FavoriteFolderVO, QuestionListItemVO } from '@/types'

/**
 * 收藏夹页面（v3 构图）：左边是蓝调面板里的夹、右边是近白卡片里这个夹的题。
 *
 * 三条与状态有关的取值，改之前先看 `docs/adr/0030`：
 * - `selected === null` 表示**「全部收藏」**（跨夹、不重复计），它不是一个真实的夹；
 * - 夹列表的顺序由后端给（按「最近有新题进来」倒序），前端不再排一次；
 * - 右列的题也由后端按**最近收藏的排最前**给（`GET /api/favorites/questions`，不是题库列表接口），
 *   前端同样不再排一次——**别在这张表上加 `sortable`**，那只会排当前这一页，与服务端排序打架。
 *
 * 浮层材质见 `docs/adr/0032`：这里是**玻璃**（.glass），不是实底。
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
const infoVisible = ref(false)
const editingFolder = ref<FavoriteFolderVO | null>(null)

/**
 * 左栏渲染用的行。第一行是**虚拟的**「全部收藏」（`id === null`），后面才是真实的夹——
 * 前端不再手拼三处 `folder.id !== null` 判断，统一在这里摊平成一份。
 * `raw` 携带原始夹对象，供 ⋯ 菜单用（「全部收藏」没有 raw，也不画 ⋯）。
 */
const folderItems = computed(() =>
  [
    { key: 'all', id: null as number | null, name: '全部收藏', questionCount: allCount.value, isDefault: false, raw: null as FavoriteFolderVO | null },
    ...folders.value.map((folder) => ({
      key: `f-${folder.id}`,
      id: folder.id as number | null,
      name: folder.name,
      questionCount: folder.questionCount,
      isDefault: folder.isDefault,
      raw: folder
    }))
  ]
)

/**
 * 「全部收藏」的计数。
 *
 * ⚠️ 它是各夹计数之和（后端给的是「每个夹里有多少题」），**不是**去重后的总数——
 * 一道题可以同时躺在两个夹里。后端没有提供「去重总数」的端点，这里就先如实标注成合计。
 * 真要精确值，得后端加字段，别在前端拍一个数出来。
 */
const allCount = computed(() => folders.value.reduce((sum, folder) => sum + (folder.questionCount ?? 0), 0))

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

/** 新建：打开空白的信息面板（`editingFolder` 置空 = 面板走 POST 分支）。 */
function onCreateFolder() {
  editingFolder.value = null
  infoVisible.value = true
}

/** 编辑：把整个夹交给面板去回填三个字段。默认夹也走这条——它不可删但可以改。 */
function onEditFolder(folder: FavoriteFolderVO) {
  editingFolder.value = folder
  infoVisible.value = true
}

async function onFolderSaved() {
  await loadFolders()
}

async function onDeleteFolder(folder: FavoriteFolderVO) {
  const ok = await confirmBox({
    title: '删除收藏夹',
    message:
      `删除「${folder.name}」后，里面的题会从它这里移出；` +
      '如果某道题不再属于任何收藏夹，它就不再是收藏了。继续吗？',
    confirmText: '删除',
    danger: true
  })
  if (!ok) return
  await deleteFolder(folder.id)
  toast.success('已删除收藏夹')
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
  toast.success('已移出这个收藏夹')
  await onStarChange()
}

async function onPractice() {
  starting.value = true
  try {
    const sessionId = await practiceFavorites(selected.value, count.value)
    router.push(`/quiz/${sessionId}`)
  } catch (e: any) {
    toast.error(e.message || '这个收藏夹里还没有题')
  } finally {
    starting.value = false
  }
}

/**
 * 收藏时间只做「变得好读」，**不做时区换算**：后端已经固定在 Asia/Shanghai 输出，
 * 这里把它变成 `2026-10-07 13:45`。顺手兼容两种写法——ISO 的 `T` 分隔与空格分隔。
 */
function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
    .format(date)
    .replace(/\//g, '-')
}

onMounted(loadAll)
</script>

<style scoped>
/*
  ⚠️ 触屏（无 hover 能力）上，⋯ 按钮必须**常显**：只靠 `group-hover:opacity-100` 的话，
  触屏设备根本不会触发 hover，按钮就永远隐身、功能够不着。
  用 `@media (hover: none)` 而不是 `any-pointer`，语义更直接（「这台设备没有真正的悬停」）。
*/
@media (hover: none) {
  .fav-more {
    opacity: 1;
  }
}
</style>
