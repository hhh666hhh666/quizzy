<template>
  <div class="font-sans text-base text-ink">
    <h1 class="sr-only">题库</h1>
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <!-- ── 筛选区 ─────────────────────────────────────────────────────────── -->
      <div class="flex flex-wrap items-end gap-x-4 gap-y-3">
        <div class="flex flex-col gap-1" data-testid="filter-keyword">
          <label for="filter-keyword-input" class="text-xs text-ink-muted">关键词</label>
          <Input
            id="filter-keyword-input"
            v-model="state.keyword"
            placeholder="搜索题干…"
            class="h-9 w-44 bg-reader"
            @keyup.enter="applySearch"
          />
        </div>

        <div class="flex flex-col gap-1" data-testid="filter-type">
          <span class="text-xs text-ink-muted">题型</span>
          <Select :model-value="state.type ?? 'ALL'" @update:model-value="(v: unknown) => (state.type = v === 'ALL' ? undefined : (v as QuestionType))">
            <SelectTrigger class="h-9 w-[130px] bg-reader">
              <span>{{ typeFilterLabel }}</span>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">不限</SelectItem>
              <SelectItem value="SINGLE">单选题</SelectItem>
              <SelectItem value="MULTI">多选题</SelectItem>
              <SelectItem value="JUDGE">判断题</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex flex-col gap-1" data-testid="filter-difficulty">
          <span class="text-xs text-ink-muted">难度</span>
          <Select
            :model-value="state.difficulty ?? 'ALL'"
            @update:model-value="(v: unknown) => (state.difficulty = v === 'ALL' ? undefined : (v as Difficulty))"
          >
            <SelectTrigger class="h-9 w-[130px] bg-reader">
              <span>{{ difficultyFilterLabel }}</span>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">不限</SelectItem>
              <SelectItem value="EASY">简单</SelectItem>
              <SelectItem value="MEDIUM">中等</SelectItem>
              <SelectItem value="HARD">困难</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex flex-col gap-1" data-testid="filter-scope">
          <span class="text-xs text-ink-muted">范围</span>
          <Select :model-value="state.scope" @update:model-value="(v: unknown) => (state.scope = v as string)">
            <SelectTrigger class="h-9 w-[130px] bg-reader">
              <span>{{ scopeFilterLabel }}</span>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部</SelectItem>
              <SelectItem value="mine">我的题库</SelectItem>
              <SelectItem value="public">公开题库</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <!--
          「全部分类」不做成一个可勾的选项，而是一行**清空动作**（面板顶部那行）：多选项之间若要互斥
          （勾「全部」就自动取消别的），界面就得自己发明一套规则去解释谁赢，而那种状态最容易用出
          「看着没筛、题却少了」。
        -->
        <div class="flex flex-col gap-1" data-testid="filter-category">
          <span class="text-xs text-ink-muted">分类</span>
          <Popover>
            <PopoverTrigger as-child>
              <button
                type="button"
                class="flex h-9 w-[200px] items-center justify-between rounded-md border border-line bg-reader px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                <span :class="state.categoryIds.length ? '' : 'text-ink-subtle'">{{ categoryTriggerLabel }}</span>
                <ChevronDownIcon class="size-4 opacity-50" />
              </button>
            </PopoverTrigger>
            <PopoverContent align="start" class="w-56 p-0">
              <button
                type="button"
                class="link-button block w-full border-b border-line-soft px-3 py-2 text-left text-xs text-ink-muted hover:text-brand"
                @click="clearCategories"
              >
                全部分类（点此清空）
              </button>
              <div class="max-h-60 overflow-y-auto py-1">
                <label
                  v-for="opt in categoryOptions"
                  :key="String(opt.value)"
                  class="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm hover:bg-surface-2"
                >
                  <Checkbox :model-value="state.categoryIds.includes(opt.value)" @update:model-value="(v: unknown) => toggleCategory(opt.value, v === true)" />
                  {{ opt.label }}
                </label>
              </div>
            </PopoverContent>
          </Popover>
        </div>

        <div class="flex flex-col gap-1" data-testid="filter-tag">
          <span class="text-xs text-ink-muted">标签</span>
          <Popover>
            <PopoverTrigger as-child>
              <button
                type="button"
                class="flex h-9 w-[200px] items-center justify-between rounded-md border border-line bg-reader px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                <span :class="state.tagIds.length ? '' : 'text-ink-subtle'">{{ tagTriggerLabel }}</span>
                <ChevronDownIcon class="size-4 opacity-50" />
              </button>
            </PopoverTrigger>
            <PopoverContent align="start" class="w-56 p-0">
              <div class="max-h-60 overflow-y-auto py-1">
                <label
                  v-for="opt in tagOptions"
                  :key="opt.value"
                  class="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm hover:bg-surface-2"
                >
                  <Checkbox :model-value="state.tagIds.includes(opt.value)" @update:model-value="(v: unknown) => toggleTag(opt.value, v === true)" />
                  {{ opt.label }}
                </label>
              </div>
            </PopoverContent>
          </Popover>
        </div>

        <!--
          收藏夹筛选。⚠️ 与上面的分类不同，这里**把「全部收藏」做成一个可勾的选项**而不是顶部动作：
          后端的语义是「在任意夹里」**或**「在这些夹里」，两者是包含关系、不存在谁赢谁输，
          所以界面不需要发明任何互斥规则。分类那边做成顶部动作，是因为「全部分类」与具体分类
          之间的互斥规则不好解释——两处取舍不同，别照搬。
        -->
        <div class="flex flex-col gap-1" data-testid="filter-favorite">
          <span class="text-xs text-ink-muted">收藏夹</span>
          <Popover>
            <PopoverTrigger as-child>
              <button
                type="button"
                class="flex h-9 w-[200px] items-center justify-between rounded-md border border-line bg-reader px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-focus"
              >
                <span :class="state.favoriteChoices.length ? '' : 'text-ink-subtle'">{{ favoriteTriggerLabel }}</span>
                <ChevronDownIcon class="size-4 opacity-50" />
              </button>
            </PopoverTrigger>
            <PopoverContent align="start" class="w-56 p-0">
              <div class="max-h-60 overflow-y-auto py-1">
                <label
                  v-for="opt in favoriteOptions"
                  :key="String(opt.value)"
                  class="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm hover:bg-surface-2"
                >
                  <Checkbox
                    :model-value="state.favoriteChoices.includes(opt.value)"
                    @update:model-value="(v: unknown) => toggleFavorite(opt.value, v === true)"
                  />
                  {{ opt.label }}
                </label>
              </div>
            </PopoverContent>
          </Popover>
        </div>

        <div class="flex gap-2">
          <Button @click="applySearch">查询</Button>
          <Button variant="outline" @click="onReset">重置</Button>
        </div>
      </div>

      <!-- ── 动作区 ─────────────────────────────────────────────────────────── -->
      <div class="mb-3 mt-5 flex flex-wrap gap-2">
        <Button @click="onCreate"><PlusIcon class="size-4" />新建题目</Button>
        <Button variant="outline" @click="importVisible = true"><UploadIcon class="size-4" />批量导入</Button>
        <Button variant="outline" @click="onExport('excel')"><DownloadIcon class="size-4 text-ok" />导出 Excel</Button>
        <Button variant="outline" @click="onExport('json')"><DownloadIcon class="size-4 text-brand" />导出 JSON</Button>
        <Button variant="outline" @click="download(templatePath(), 'quizzy-template.xlsx')"><FileDownIcon class="size-4" />下载导入模板</Button>
      </div>

      <!--
        批量操作条：只在选了题之后出现。
        ⚠️ 「已选」是**跨页**的——勾选状态自己记账（见脚本里的 selectedIds），翻页或改筛选都
        不会把之前勾的冲掉，所以这里必须把数量显式写出来，否则翻到第二页时会以为第一页白勾了。
      -->
      <div v-if="selectedCount" class="batch-bar mb-3 flex flex-wrap items-center gap-2 rounded-md border border-line bg-brand-soft/40 px-3 py-2">
        <span class="text-sm">已选 {{ selectedCount }} 道（可跨页）</span>
        <Button size="sm" data-testid="batch-add-to-paper" @click="openAddToPaper">加入试卷</Button>
        <Button size="sm" variant="outline" data-testid="batch-new-paper" @click="onCreatePaperFromSelection">用所选新建试卷</Button>
        <Button size="sm" variant="outline" data-testid="batch-add-to-favorite" @click="addToFavoriteVisible = true">加入收藏夹</Button>
        <Button size="sm" variant="link" @click="clearSelection">清空选择</Button>
      </div>

      <!--
        题目列表。⚠️ 表格本体**不铺玻璃、不上深色**：题干是要逐字读的东西，行面就是卡片自己的
        近白面（surface），只以分隔线与 hover 区分行。
      -->
      <div class="overflow-x-auto rounded-lg border border-line-soft" :class="loading ? 'pointer-events-none opacity-60' : ''">
        <table class="w-full border-collapse text-sm">
          <thead>
            <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
              <th class="w-12 px-3 py-2.5">
                <Checkbox
                  :model-value="pageAllSelected ? true : pageSomeSelected ? 'indeterminate' : false"
                  aria-label="全选本页"
                  @update:model-value="(v: unknown) => togglePage(v === true)"
                />
              </th>
              <th class="px-3 py-2.5 font-medium">ID</th>
              <th class="px-3 py-2.5 font-medium">题型</th>
              <th class="px-3 py-2.5 font-medium">题干</th>
              <th class="px-3 py-2.5 font-medium">分类</th>
              <th class="px-3 py-2.5 font-medium">难度</th>
              <th class="px-3 py-2.5 font-medium">分值</th>
              <th class="px-3 py-2.5 font-medium">归属</th>
              <th class="px-3 py-2.5 text-center font-medium">收藏</th>
              <th class="px-3 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="qb-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
              <td class="px-3 py-2.5">
                <Checkbox
                  :model-value="selectedIds.has(row.id)"
                  :aria-label="`选择第 ${row.id} 题`"
                  @update:model-value="(v: unknown) => toggleRow(row, v === true)"
                />
              </td>
              <td class="px-3 py-2.5 text-ink-muted">{{ row.id }}</td>
              <td class="px-3 py-2.5"><TypeTag :type="row.type" /></td>
              <td class="max-w-0 px-3 py-2.5">
                <span class="block truncate" :title="row.stem">{{ row.stem }}</span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ row.categoryName || '未分类' }}</td>
              <td class="whitespace-nowrap px-3 py-2.5"><DifficultyTag :level="row.difficulty" /></td>
              <td class="px-3 py-2.5">{{ row.score }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ row.ownerId ? '我的' : '公开' }}</td>
              <td class="px-3 py-2.5 text-center">
                <FavoriteStar :question-id="row.id" :favorited="row.favorited" @change="onStarChange(row, $event)" />
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <button type="button" class="link-button text-brand hover:underline" @click="onView(row)">查看</button>
                <button type="button" class="link-button ml-2 text-brand hover:underline disabled:cursor-not-allowed disabled:opacity-50" :disabled="!row.editable" @click="onEdit(row)">
                  编辑
                </button>
                <button
                  type="button"
                  class="link-button ml-2 text-danger hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                  :disabled="!row.editable"
                  @click="onDelete(row)"
                >
                  删除
                </button>
              </td>
            </tr>

            <!--
              空表提示。默认范围是「我的题库」，而种子题库全是公开题，所以刚注册的人一进来
              必然看到空表——不解释一句的话，那看起来就像「题都丢了」或者导入失败。
            -->
            <tr v-if="!rows.length">
              <td colspan="10">
                <div class="empty-hint px-4 py-8 text-center text-sm leading-loose text-ink-muted">
                  <template v-if="noFilterButMine">
                    我的题库里还没有题目。可以点「新建题目」自己建，或者把上面的「范围」切到
                    <b>全部</b> / <b>公开题库</b>，那里有系统自带的题。
                  </template>
                  <template v-else> 没有符合当前筛选条件的题目。试试放宽条件，或点「重置」回到默认状态。 </template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ── 分页 ───────────────────────────────────────────────────────────── -->
      <TablePagination
        :total="total"
        :page="state.page"
        :size="state.size"
        sizes
        @update:page="(p) => { state.page = p; applyPage() }"
        @update:size="(s) => { state.size = s; applyPage() }"
      />
    </div>

    <QuestionDetailDialog v-model:visible="detailVisible" :question-id="viewingId" @edit="onEditFromDetail" />
    <QuestionEditDialog v-model:visible="editVisible" :question-id="editingId" @saved="load" />
    <ImportDialog v-model:visible="importVisible" @done="load" />

    <!-- 批量加入收藏夹：**只加不减**，与单题的「修改收藏夹」（覆盖）刻意不同（见 ADR 0030） -->
    <FavoriteFoldersDialog
      v-model:visible="addToFavoriteVisible"
      mode="add"
      :question-ids="selectedIdList"
      @saved="onBatchFavoriteSaved"
    />

    <!-- 「加入已有试卷」：只列固定卷——规则卷没有题目列表，加不进去（后端也会拒） -->
    <Dialog :open="addToPaperVisible" @update:open="(v: boolean) => (addToPaperVisible = v)">
      <DialogContent class="sm:max-w-md">
        <DialogHeader>
          <DialogTitle class="text-base font-medium">加入试卷</DialogTitle>
        </DialogHeader>
        <p class="text-sm leading-relaxed">只能加入<b>固定卷</b>；已在卷中的题会被自动忽略，不会重复。</p>
        <Select
          :model-value="targetPaperId === null ? undefined : String(targetPaperId)"
          @update:model-value="(v: unknown) => (targetPaperId = Number(v))"
        >
          <SelectTrigger class="w-full bg-reader" data-testid="batch-paper-select">
            <span>{{ targetPaperLabel }}</span>
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="p in fixedPapers" :key="p.id" :value="String(p.id)">{{ p.title }}（{{ p.questionCount }} 道）</SelectItem>
          </SelectContent>
        </Select>
        <DialogFooter>
          <Button variant="outline" @click="addToPaperVisible = false">取消</Button>
          <Button :disabled="!targetPaperId" data-testid="batch-add-confirm" @click="onAddToPaper">加入</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQuery } from 'vue-router'
import { confirmBox, promptBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import { ChevronDownIcon, DownloadIcon, FileDownIcon, PlusIcon, UploadIcon } from '@lucide/vue'
import { deleteQuestion, listCategories, listTags, pageQuestions, toQueryParams } from '@/api/question'
import { appendPaperQuestions, exportPath, pagePapers, savePaper, templatePath } from '@/api/paper'
import { listFolders } from '@/api/favorite'
import request from '@/api/request'
import FavoriteStar from '@/components/FavoriteStar.vue'
import FavoriteFoldersDialog from '@/components/FavoriteFoldersDialog.vue'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import DifficultyTag from '@/components/DifficultyTag.vue'
import TablePagination from '@/components/TablePagination.vue'
import TypeTag from '@/components/TypeTag.vue'
import QuestionDetailDialog from './QuestionDetailDialog.vue'
import QuestionEditDialog from './QuestionEditDialog.vue'
import ImportDialog from './ImportDialog.vue'
import type { Difficulty, FavoriteFolderVO, QuestionListItemVO, QuestionQuery, QuestionType, TagVO } from '@/types'

/**
 * 分类下拉里「未分类」这一项的值。
 *
 * 分类 id 一律是数字，所以用这个字符串当哨兵不会撞上真实分类。它在上传给后端之前会被
 * 翻译成 `uncategorized=true`——接口上「未分类」不是一个真实的分类 id，而是一个独立的开关。
 */
const UNCATEGORIZED = 'none'
type CategoryChoice = number | typeof UNCATEGORIZED

/**
 * 收藏夹下拉里「全部收藏」这一项的值。
 *
 * 收藏夹 id 一律是数字，所以这个字符串当哨兵不会撞上真实的夹。与分类的「未分类」不同，它**是一个可勾的选项**
 * （理由见模板里那段注释）；上送后端时被翻成 `anyFavorite=true`。
 */
const ALL_FAVORITES = 'all'
type FavoriteChoice = number | typeof ALL_FAVORITES

/** 页面的全部状态，**也就是写进网址的那一份**——两边同源，不存在「网址上有、界面上没有」的字段。 */
interface PageState {
  keyword: string
  type?: QuestionType
  difficulty?: Difficulty
  scope: string
  categoryIds: CategoryChoice[]
  tagIds: number[]
  favoriteChoices: FavoriteChoice[]
  page: number
  size: number
}

const DEFAULT_SCOPE = 'mine'
const DEFAULT_SIZE = 10

const TYPES: QuestionType[] = ['SINGLE', 'MULTI', 'JUDGE']
const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD']
const SCOPES = ['all', 'mine', 'public']

const route = useRoute()
const router = useRouter()

const rows = ref<QuestionListItemVO[]>([])
const total = ref(0)
const loading = ref(false)
const categories = ref<{ id: number; name: string }[]>([])
const tags = ref<TagVO[]>([])
const favoriteFolders = ref<FavoriteFolderVO[]>([])
const detailVisible = ref(false)
const editVisible = ref(false)
const importVisible = ref(false)
const viewingId = ref<number | null>(null)
const editingId = ref<number | null>(null)

// ---------- 批量选择与批量操作 ----------
//
// ⚠️ 勾选状态**自己记账**：原来用 el-table 的 selection 是因为「选中态随数据重置」的坑，
// 现在表格是自绘的、勾选本来就在行上，但记账方式不变——跨页勾选翻页 / 改筛选都不会丢。
const selectedIds = ref(new Set<number>())
const selectedCount = computed(() => selectedIds.value.size)
/** 给「加入收藏夹」面板用：批量接口要的是一份 id 数组，不是 Set。 */
const selectedIdList = computed(() => [...selectedIds.value])
const pageAllSelected = computed(
  () => rows.value.length > 0 && rows.value.every((row) => selectedIds.value.has(row.id))
)
const pageSomeSelected = computed(
  () => !pageAllSelected.value && rows.value.some((row) => selectedIds.value.has(row.id))
)
const addToPaperVisible = ref(false)
const addToFavoriteVisible = ref(false)
const fixedPapers = ref<{ id: number; title: string; questionCount: number }[]>([])
const targetPaperId = ref<number | null>(null)
const adding = ref(false)

/** 加入试卷弹窗的触发钮文案（reka Select 的受控显示）。 */
const targetPaperLabel = computed(() => {
  const paper = fixedPapers.value.find((p) => p.id === targetPaperId.value)
  return paper ? `${paper.title}（${paper.questionCount} 道）` : '选择一张固定卷'
})

function toggleRow(row: QuestionListItemVO, checked: boolean) {
  if (checked) selectedIds.value.add(row.id)
  else selectedIds.value.delete(row.id)
}

function togglePage(checked: boolean) {
  for (const row of rows.value) {
    if (checked) selectedIds.value.add(row.id)
    else selectedIds.value.delete(row.id)
  }
}

function clearSelection() {
  selectedIds.value = new Set()
  targetPaperId.value = null
}

async function openAddToPaper() {
  targetPaperId.value = null
  // 只取固定卷：规则卷没有题目列表，「加入」对它没有意义（后端也会以业务错误拒绝）
  fixedPapers.value = (await pagePapers(1, 100)).list.filter((paper) => paper.mode === 'FIXED')
  if (!fixedPapers.value.length) {
    toast.warning('还没有固定卷。先用「用所选新建试卷」建一张吧。')
    return
  }
  addToPaperVisible.value = true
}

async function onAddToPaper() {
  if (!targetPaperId.value) return
  adding.value = true
  try {
    const result = await appendPaperQuestions(targetPaperId.value, [...selectedIds.value])
    if (result.added) {
      toast.success(`已加入 ${result.added} 道，该卷现有 ${result.total} 道`)
    } else {
      toast.info(`所选题目都已在卷中，该卷现有 ${result.total} 道`)
    }
    addToPaperVisible.value = false
    clearSelection()
  } finally {
    adding.value = false
  }
}

/**
 * 星标变了：**只改这一行**，不重取列表。
 *
 * 与收藏夹页面刻意不同——那一页本身就是「按收藏筛选」的视图，收藏状态一变、这一行很可能已经
 * 不属于当前结果集；而题库页的筛选条件与收藏无关，行不会因此消失，就地更新既准又省一次请求。
 */
function onStarChange(row: QuestionListItemVO, favorited: boolean) {
  row.favorited = favorited
}

/** 批量加入收藏夹之后得重取一次：这一页里好几行的星标都可能跟着亮了。 */
function onBatchFavoriteSaved() {
  clearSelection()
  load()
}

async function onCreatePaperFromSelection() {
  const title = await promptBox({
    title: '用所选题目新建试卷',
    message: '新试卷的标题',
    validator: (v) => (v && v.trim().length > 0 ? true : '标题不能为空')
  })
  if (title === null) return
  await savePaper({ title: title.trim(), mode: 'FIXED', questionIds: [...selectedIds.value] })
  toast.success(`已新建试卷（${selectedCount.value} 道题）`)
  clearSelection()
}

/**
 * 范围默认是「我的题库」，不是「全部」。
 *
 * ⚠️ 代价写在明处：种子题库全是公开题，所以刚注册的人打开这一页会看到空表。这一条靠下面
 * 那段空表提示来兜，而不是把默认值改回「全部」——那等于默认糊一堆不是自己的题在眼前。
 */
const state = reactive<PageState>({
  keyword: '',
  type: undefined,
  difficulty: undefined,
  scope: DEFAULT_SCOPE,
  categoryIds: [],
  tagIds: [],
  favoriteChoices: [],
  page: 1,
  size: DEFAULT_SIZE
})

/** 只动了默认范围、别的条件一个都没动——用来决定空表时该说哪句话。 */
const noFilterButMine = computed(
  () =>
    state.scope === DEFAULT_SCOPE &&
    !state.keyword &&
    !state.type &&
    !state.difficulty &&
    !state.categoryIds.length &&
    !state.tagIds.length &&
    !state.favoriteChoices.length
)

// ---------- 多选筛选的选项与触发钮文案 ----------

const categoryOptions = computed(() => [
  { label: '未分类', value: UNCATEGORIZED as CategoryChoice },
  ...categories.value.map((c) => ({ label: c.name, value: c.id as CategoryChoice }))
])
const tagOptions = computed(() => tags.value.map((t) => ({ label: t.name, value: t.id })))
const favoriteOptions = computed(() => [
  { label: '全部收藏', value: ALL_FAVORITES as FavoriteChoice },
  ...favoriteFolders.value.map((f) => ({ label: f.name, value: f.id as FavoriteChoice }))
])

function toggleCategory(value: CategoryChoice, checked: boolean) {
  state.categoryIds = checked
    ? [...state.categoryIds, value]
    : state.categoryIds.filter((v) => v !== value)
}

function toggleTag(id: number, checked: boolean) {
  state.tagIds = checked ? [...state.tagIds, id] : state.tagIds.filter((v) => v !== id)
}

function toggleFavorite(value: FavoriteChoice, checked: boolean) {
  state.favoriteChoices = checked
    ? [...state.favoriteChoices, value]
    : state.favoriteChoices.filter((v) => v !== value)
}

/** 多选触发钮的文案：没选显示占位；选一个显示名字；多个显示「XX 等 N 项」。 */
function multiTriggerLabel(
  choices: (string | number)[],
  options: { label: string; value: string | number }[],
  placeholder: string
) {
  if (!choices.length) return placeholder
  const first = options.find((o) => o.value === choices[0])
  const firstLabel = first ? first.label : String(choices[0])
  return choices.length === 1 ? firstLabel : `${firstLabel} 等 ${choices.length} 项`
}

const categoryTriggerLabel = computed(() => multiTriggerLabel(state.categoryIds, categoryOptions.value, '全部分类'))
const tagTriggerLabel = computed(() => multiTriggerLabel(state.tagIds, tagOptions.value, '全部标签'))
const favoriteTriggerLabel = computed(() => multiTriggerLabel(state.favoriteChoices, favoriteOptions.value, '全部收藏夹'))

// ---------- 单选筛选的触发钮文案 ----------

const FILTER_TYPE_LABELS: Record<QuestionType, string> = { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题' }
const FILTER_DIFFICULTY_LABELS: Record<Difficulty, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
const SCOPE_LABELS: Record<string, string> = { all: '全部', mine: '我的题库', public: '公开题库' }

const typeFilterLabel = computed(() => (state.type ? FILTER_TYPE_LABELS[state.type] : '不限'))
const difficultyFilterLabel = computed(() => (state.difficulty ? FILTER_DIFFICULTY_LABELS[state.difficulty] : '不限'))
const scopeFilterLabel = computed(() => SCOPE_LABELS[state.scope] ?? '我的题库')

// ---------- 题型 / 难度标签（彩色小标签：tint 底 + 深字色，2026-10-08 主人拍板） ----------
// 色相映射按主人给的参考图：单选=蓝、多选=绿、判断=紫；简单=绿、中等=琥珀、困难=红。
// 标签本体抽成了 TypeTag / DifficultyTag 两个组件（错题本 / 收藏夹 / 记录页共用）。

// ---------- 状态 ↔ 网址 ----------

function firstOf(value: unknown): string | undefined {
  if (Array.isArray(value)) return value.length ? String(value[0]) : undefined
  return value == null ? undefined : String(value)
}

function listOf(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String)
  // 手写网址时常常写成 `tagIds=1,2`，两种写法都认，免得链接到了别人手里就失效
  if (typeof value === 'string' && value.length) return value.split(',')
  return []
}

/** 把网址上的分类值翻成界面值；认不出来的（手改网址留下的脏值）直接丢掉。 */
function toCategoryChoices(values: string[]): CategoryChoice[] {
  const choices: CategoryChoice[] = []
  for (const value of values) {
    if (value === UNCATEGORIZED) {
      choices.push(UNCATEGORIZED)
      continue
    }
    const id = Number(value)
    if (Number.isFinite(id)) choices.push(id)
  }
  return choices
}

function toIds(values: string[]): number[] {
  return values.map((v) => Number(v)).filter((v) => Number.isFinite(v))
}

/** 把网址上的收藏夹值翻成界面值；认不出来的一律丢掉（与分类同款防御，别让脏值打到后端）。 */
function toFavoriteChoices(values: string[]): FavoriteChoice[] {
  const choices: FavoriteChoice[] = []
  for (const value of values) {
    if (value === ALL_FAVORITES) {
      choices.push(ALL_FAVORITES)
      continue
    }
    const id = Number(value)
    if (Number.isFinite(id)) choices.push(id)
  }
  return choices
}

/**
 * 从网址读出页面状态。
 *
 * 枚举值一律**校验后才接受**：网址是可以手改的，塞一个 `type=FOO` 进去会让后端在枚举转换
 * 那一步直接 400，而主人看到的只是一个没头没脑的报错。脏值统统忽略、退回默认。
 */
function readStateFromRoute(): PageState {
  const q = route.query
  const type = firstOf(q.type)
  const difficulty = firstOf(q.difficulty)
  const scope = firstOf(q.scope)
  const page = Number.parseInt(firstOf(q.page) ?? '', 10)
  const size = Number.parseInt(firstOf(q.size) ?? '', 10)
  return {
    keyword: firstOf(q.keyword) ?? '',
    type: TYPES.includes(type as QuestionType) ? (type as QuestionType) : undefined,
    difficulty: DIFFICULTIES.includes(difficulty as Difficulty) ? (difficulty as Difficulty) : undefined,
    scope: SCOPES.includes(scope ?? '') ? (scope as string) : DEFAULT_SCOPE,
    categoryIds: toCategoryChoices(listOf(q.categoryIds)),
    tagIds: toIds(listOf(q.tagIds)),
    favoriteChoices: toFavoriteChoices(listOf(q.favoriteFolderIds)),
    page: Number.isFinite(page) && page > 1 ? page : 1,
    size: Number.isFinite(size) && size > 0 ? size : DEFAULT_SIZE
  }
}

/**
 * 把页面状态写回网址。
 *
 * ⚠️ 用 `replace` 而不是 `push`：否则每改一次筛选就多一条历史记录，主人按一次「后退」
 * 只会退回上一个下拉选项、而不是上一张页面——那是添乱。
 *
 * 与默认值相同的项**不写进网址**，于是「什么都没筛」的网址就是干净的 `/questions`。
 */
function writeStateToRoute() {
  const query: LocationQuery = {}
  if (state.keyword) query.keyword = state.keyword
  if (state.type) query.type = state.type
  if (state.difficulty) query.difficulty = state.difficulty
  if (state.scope !== DEFAULT_SCOPE) query.scope = state.scope
  if (state.categoryIds.length) query.categoryIds = state.categoryIds.map(String)
  if (state.tagIds.length) query.tagIds = state.tagIds.map(String)
  if (state.favoriteChoices.length) query.favoriteFolderIds = state.favoriteChoices.map(String)
  if (state.page > 1) query.page = String(state.page)
  if (state.size !== DEFAULT_SIZE) query.size = String(state.size)
  router.replace({ path: '/questions', query })
}

// ---------- 状态 → 后端参数 ----------

/**
 * 把界面状态翻成后端认的查询参数。
 *
 * 「未分类」的表达方式只在这一个函数里出现（界面用哨兵 `none`，接口用 `uncategorized=true`），
 * 别处一律不碰这个细节——不然它迟早会散到两三个地方去，然后互相对不上。
 */
function apiQuery(): QuestionQuery {
  return {
    keyword: state.keyword || undefined,
    type: state.type,
    difficulty: state.difficulty,
    scope: state.scope,
    categoryIds: state.categoryIds.filter((v): v is number => v !== UNCATEGORIZED),
    uncategorized: state.categoryIds.includes(UNCATEGORIZED),
    tagIds: state.tagIds,
    // 「全部收藏」勾了就置 anyFavorite：后端那边它**覆盖**具体夹（包含关系），
    // 所以这里不必把两者合起来算，界面也就不用解释「谁赢」。
    anyFavorite: state.favoriteChoices.includes(ALL_FAVORITES),
    favoriteFolderIds: state.favoriteChoices.filter((v): v is number => v !== ALL_FAVORITES)
  }
}

// ---------- 数据加载 ----------

async function load() {
  loading.value = true
  try {
    const result = await pageQuestions({ ...apiQuery(), page: state.page, size: state.size })
    // 页码越界（多半是手改网址留下的）→ 退回第 1 页再取一次，别让主人停在一张空白页上
    if (result.list.length === 0 && result.total > 0 && state.page > 1) {
      state.page = 1
      writeStateToRoute()
      const first = await pageQuestions({ ...apiQuery(), page: 1, size: state.size })
      rows.value = first.list
      total.value = first.total
      return
    }
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** 「查询」：条件变了就该从第 1 页重新看，并把新条件写进网址。 */
function applySearch() {
  state.page = 1
  writeStateToRoute()
  load()
}

/** 翻页 / 改每页条数：条件不变，只把页码同步进网址。 */
function applyPage() {
  writeStateToRoute()
  load()
}

function onReset() {
  Object.assign(state, {
    keyword: '',
    type: undefined,
    difficulty: undefined,
    scope: DEFAULT_SCOPE,
    categoryIds: [],
    tagIds: [],
    favoriteChoices: [],
    page: 1,
    size: DEFAULT_SIZE
  } satisfies PageState)
  writeStateToRoute()
  load()
}

/** 分类面板顶部那行「全部分类」。 */
function clearCategories() {
  state.categoryIds = []
}

function onCreate() {
  editingId.value = null
  editVisible.value = true
}

function onView(row: QuestionListItemVO) {
  viewingId.value = row.id
  detailVisible.value = true
}

function onEdit(row: QuestionListItemVO) {
  editingId.value = row.id
  editVisible.value = true
}

function onEditFromDetail(id: number) {
  editingId.value = id
  editVisible.value = true
}

async function onDelete(row: QuestionListItemVO) {
  const ok = await confirmBox({ title: '提示', message: '删除后不可恢复，确认删除？', confirmText: '删除', danger: true })
  if (!ok) return
  await deleteQuestion(row.id)
  toast.success('已删除')
  // 条件与页码都不动，只把当前这一页重新取一遍（页码若因此越界，load 会自己退回第 1 页）
  load()
}

/**
 * 导出文件名里的时间戳，**用北京时间**。
 *
 * 此前是 `new Date().toISOString()` —— 那是 **UTC**，比北京时间早 8 小时，
 * 于是 11:36 导出的文件叫 `...033653`，看着像坏的。
 * 这里固定 `Asia/Shanghai` 而不是用本机时区：「导出出来的文件叫什么」不该随设备时区变。
 */
function beijingStamp() {
  // sv-SE 的本地化格式恰好是 ISO 风格（YYYY-MM-DD HH:mm:ss），去掉分隔符即 14 位
  return new Date()
    .toLocaleString('sv-SE', { timeZone: 'Asia/Shanghai' })
    .replace(/[-: ]/g, '')
}

/**
 * 导出**跟随筛选条件**，但**不跟随分页**：筛完点导出，拿到的是「筛出来的那一批全部」。
 *
 * ⚠️ 所以这里传的是 `apiQuery()`（不含 page / size）而不是带页码的那一份。
 * 另外注意默认范围是「我的题库」——「什么都不改直接点导出」导出的就是自己的题，不再含公开题；
 * 想把公开题一起导出去，先把「范围」切到「全部」。
 */
function onExport(format: string) {
  // ⚠️ format 是「语义格式」（excel / json），**不是**文件扩展名：
  // 直接拿它拼后缀会得到 .excel（Excel 打不开）。Excel 的真实扩展名是 xlsx。
  const ext = format === 'json' ? 'json' : 'xlsx'
  download(exportPath(format, toQueryParams(apiQuery())), `quizzy-questions-${beijingStamp()}.${ext}`)
}

async function download(path: string, filename: string) {
  const response = await request.get(path, { responseType: 'blob' })
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

onMounted(async () => {
  // 顺序有讲究：先把网址里的条件读进来，再取下拉框的候选值，最后才查列表
  Object.assign(state, readStateFromRoute())
  const [categoryList, tagList, folderList] = await Promise.all([listCategories(), listTags(), listFolders()])
  categories.value = categoryList
  tags.value = tagList
  favoriteFolders.value = folderList
  await load()
})
</script>
