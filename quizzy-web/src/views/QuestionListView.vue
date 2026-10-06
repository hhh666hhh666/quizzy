<template>
  <div>
    <el-card>
      <el-form :inline="true" :model="state">
        <el-form-item label="关键词">
          <el-input v-model="state.keyword" clearable placeholder="搜索题干" @keyup.enter="applySearch" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="state.type" clearable style="width: 120px">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="state.difficulty" clearable style="width: 120px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="state.scope" style="width: 120px">
            <el-option label="全部" value="all" />
            <el-option label="我的题库" value="mine" />
            <el-option label="公开题库" value="public" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="state.categoryIds"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部分类"
            style="width: 200px"
          >
            <!--
              「全部分类」不做成一个可勾的选项，而是一行**清空动作**：多选项之间若要互斥
              （勾「全部」就自动取消别的），界面就得自己发明一套规则去解释谁赢，
              而那种状态最容易用出「看着没筛、题却少了」。
            -->
            <template #header>
              <div class="select-header" @click="clearCategories">全部分类（点此清空）</div>
            </template>
            <el-option label="未分类" :value="UNCATEGORIZED" />
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="state.tagIds"
            multiple
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="全部标签"
            style="width: 200px"
          >
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applySearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="onCreate">新建题目</el-button>
        <el-button @click="importVisible = true">批量导入</el-button>
        <el-button @click="onExport('excel')">导出 Excel</el-button>
        <el-button @click="onExport('json')">导出 JSON</el-button>
        <el-button @click="download(templatePath(), 'quizzy-template.xlsx')">下载导入模板</el-button>
      </div>

      <!--
        批量操作条：只在选了题之后出现。
        ⚠️ 「已选」是**跨页**的——勾选状态自己记账（见脚本里的 selectedIds），翻页或改筛选都
        不会把之前勾的冲掉，所以这里必须把数量显式写出来，否则翻到第二页时会以为第一页白勾了。
      -->
      <div v-if="selectedCount" class="batch-bar">
        <span class="batch-count">已选 {{ selectedCount }} 道（可跨页）</span>
        <el-button size="small" type="primary" data-testid="batch-add-to-paper" @click="openAddToPaper">加入试卷</el-button>
        <el-button size="small" data-testid="batch-new-paper" @click="onCreatePaperFromSelection">用所选新建试卷</el-button>
        <el-button size="small" link @click="clearSelection">清空选择</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column width="46">
          <template #header>
            <el-checkbox
              :model-value="pageAllSelected"
              :indeterminate="pageSomeSelected"
              @change="(v: any) => togglePage(!!v)"
            />
          </template>
          <template #default="{ row }">
            <el-checkbox :model-value="selectedIds.has(row.id)" @change="(v: any) => toggleRow(row, !!v)" />
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="归属" width="100">
          <template #default="{ row }">{{ row.ownerId ? '我的' : '公开' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" :disabled="!row.editable" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!row.editable" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <!--
          空表提示。默认范围是「我的题库」，而种子题库全是公开题，所以刚注册的人一进来
          必然看到空表——不解释一句的话，那看起来就像「题都丢了」或者导入失败。
        -->
        <template #empty>
          <div class="empty-hint">
            <template v-if="noFilterButMine">
              我的题库里还没有题目。可以点「新建题目」自己建，或者把上面的「范围」切到
              <b>全部</b> / <b>公开题库</b>，那里有系统自带的题。
            </template>
            <template v-else> 没有符合当前筛选条件的题目。试试放宽条件，或点「重置」回到默认状态。 </template>
          </div>
        </template>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:current-page="state.page"
        v-model:page-size="state.size"
        @change="applyPage"
      />
    </el-card>

    <QuestionDetailDialog
      v-model:visible="detailVisible"
      :question-id="viewingId"
      @edit="onEditFromDetail"
    />
    <QuestionEditDialog v-model:visible="editVisible" :question-id="editingId" @saved="load" />
    <ImportDialog v-model:visible="importVisible" @done="load" />

    <!-- 「加入已有试卷」：只列固定卷——规则卷没有题目列表，加不进去（后端也会拒） -->
    <el-dialog v-model="addToPaperVisible" title="加入试卷" width="460px">
      <p class="add-hint">只能加入<b>固定卷</b>；已在卷中的题会被自动忽略，不会重复。</p>
      <el-select v-model="targetPaperId" placeholder="选择一张固定卷" class="add-select" data-testid="batch-paper-select">
        <el-option
          v-for="p in fixedPapers"
          :key="p.id"
          :label="`${p.title}（${p.questionCount} 道）`"
          :value="p.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="addToPaperVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!targetPaperId"
          :loading="adding"
          data-testid="batch-add-confirm"
          @click="onAddToPaper"
        >
          加入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { LocationQuery } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteQuestion, listCategories, listTags, pageQuestions, toQueryParams } from '@/api/question'
import { appendPaperQuestions, exportPath, pagePapers, savePaper, templatePath } from '@/api/paper'
import request from '@/api/request'
import QuestionDetailDialog from './QuestionDetailDialog.vue'
import QuestionEditDialog from './QuestionEditDialog.vue'
import ImportDialog from './ImportDialog.vue'
import type { Difficulty, QuestionListItemVO, QuestionQuery, QuestionType, TagVO } from '@/types'

/**
 * 分类下拉里「未分类」这一项的值。
 *
 * 分类 id 一律是数字，所以用这个字符串当哨兵不会撞上真实分类。它在上传给后端之前会被
 * 翻译成 `uncategorized=true`——接口上「未分类」不是一个真实的分类 id，而是一个独立的开关。
 */
const UNCATEGORIZED = 'none'
type CategoryChoice = number | typeof UNCATEGORIZED

/** 页面的全部状态，**也就是写进网址的那一份**——两边同源，不存在「网址上有、界面上没有」的字段。 */
interface PageState {
  keyword: string
  type?: QuestionType
  difficulty?: Difficulty
  scope: string
  categoryIds: CategoryChoice[]
  tagIds: number[]
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
const detailVisible = ref(false)
const editVisible = ref(false)
const importVisible = ref(false)
const viewingId = ref<number | null>(null)
const editingId = ref<number | null>(null)

// ---------- 批量选择与批量操作 ----------
//
// ⚠️ 勾选状态**自己记账**，不用 el-table 的 `type="selection"`：它的选中态在数据换了就重置，
// 跨页勾选要靠 `reserve-selection` 才留得住，而「`selection-change` 到底带不带保留行」不够直观——
// 一旦不带，翻到第二页再操作就会**静默丢掉第一页的选择**。同一取舍见 `PaperEditDialog` 的选题器。
const selectedIds = ref(new Set<number>())
const selectedCount = computed(() => selectedIds.value.size)
const pageAllSelected = computed(
  () => rows.value.length > 0 && rows.value.every((row) => selectedIds.value.has(row.id))
)
const pageSomeSelected = computed(
  () => !pageAllSelected.value && rows.value.some((row) => selectedIds.value.has(row.id))
)
const addToPaperVisible = ref(false)
const fixedPapers = ref<{ id: number; title: string; questionCount: number }[]>([])
const targetPaperId = ref<number | null>(null)
const adding = ref(false)

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
    ElMessage.warning('还没有固定卷。先用「用所选新建试卷」建一张吧。')
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
      ElMessage.success(`已加入 ${result.added} 道，该卷现有 ${result.total} 道`)
    } else {
      ElMessage.info(`所选题目都已在卷中，该卷现有 ${result.total} 道`)
    }
    addToPaperVisible.value = false
    clearSelection()
  } finally {
    adding.value = false
  }
}

async function onCreatePaperFromSelection() {
  try {
    const { value } = await ElMessageBox.prompt('新试卷的标题', '用所选题目新建试卷', {
      inputValidator: (v: string) => (v && v.trim().length > 0 ? true : '标题不能为空')
    })
    await savePaper({ title: String(value).trim(), mode: 'FIXED', questionIds: [...selectedIds.value] })
    ElMessage.success(`已新建试卷（${selectedCount.value} 道题）`)
    clearSelection()
  } catch (e: any) {
    // 点取消也会 reject，别当成错误提示
    if (e !== 'cancel') ElMessage.error(e?.message || '新建试卷失败')
  }
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
    !state.tagIds.length
)

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
    tagIds: state.tagIds
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
    page: 1,
    size: DEFAULT_SIZE
  } satisfies PageState)
  writeStateToRoute()
  load()
}

/** 分类下拉顶部那行「全部分类」。 */
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
  await ElMessageBox.confirm('删除后不可恢复，确认删除？', '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
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

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

function difficultyLabel(level: string) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[level] || level
}

onMounted(async () => {
  // 顺序有讲究：先把网址里的条件读进来，再取下拉框的候选值，最后才查列表
  Object.assign(state, readStateFromRoute())
  const [categoryList, tagList] = await Promise.all([listCategories(), listTags()])
  categories.value = categoryList
  tags.value = tagList
  await load()
})
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-fill-color-light);
}
.batch-count { color: var(--el-text-color-regular); font-size: 13px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.add-hint { margin: 0 0 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.add-select { width: 100%; }
.empty-hint { padding: 18px 12px; color: var(--el-text-color-secondary); line-height: 1.8; }
.select-header {
  padding: 3px 12px 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  cursor: pointer;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.select-header:hover { color: var(--el-color-primary); }
</style>
