<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="flex max-h-[85vh] max-w-3xl flex-col gap-0 overflow-hidden p-0">
      <DialogHeader class="border-b border-line-soft px-6 py-4">
        <DialogTitle class="text-base font-medium">{{ form.id ? '编辑试卷' : '新建试卷' }}</DialogTitle>
      </DialogHeader>

      <div class="flex flex-col gap-4 overflow-y-auto px-6 py-5">
        <div class="flex flex-col gap-1">
          <label for="paper-title" class="text-xs text-ink-muted">标题</label>
          <Input id="paper-title" v-model="form.title" class="bg-reader" />
        </div>

        <div class="flex flex-col gap-1">
          <label for="paper-description" class="text-xs text-ink-muted">说明</label>
          <Textarea id="paper-description" v-model="form.description" :rows="2" class="bg-reader" />
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-xs text-ink-muted">模式</span>
          <div class="flex gap-1 self-start rounded-md bg-surface-2 p-1">
            <button
              v-for="m in MODES"
              :key="m.value"
              type="button"
              class="rounded-sm px-3 py-1 text-sm transition-colors"
              :class="form.mode === m.value ? 'bg-surface font-medium text-ink shadow-sm' : 'text-ink-muted hover:text-ink'"
              @click="form.mode = m.value"
            >
              {{ m.label }}
            </button>
          </div>
          <span class="text-xs text-ink-muted">
            {{ form.mode === 'FIXED' ? '题目固定，每次作答都是同一批题' : '每次作答按规则现场抽题' }}
          </span>
        </div>

        <template v-if="form.mode === 'FIXED'">
          <div class="flex items-center gap-3">
            <span class="text-sm">已选题目：共 {{ form.questionIds.length }} 道</span>
            <Button variant="link" size="sm" @click="selectorVisible = true">选择题库题目</Button>
            <Button variant="link" size="sm" @click="createQuestionVisible = true">新建题目</Button>
          </div>

          <p
            v-if="!form.questionIds.length"
            class="rounded-md bg-tint-amber/50 px-3 py-2 text-sm"
          >
            空卷可以先保存，之后再往里加题；空卷不能发起作答。
          </p>

          <div v-if="selectedQuestions.length" class="overflow-hidden rounded-lg border border-line-soft">
            <table class="w-full border-collapse text-sm">
              <tbody>
                <tr v-for="q in selectedQuestions" :key="q.id" class="picked-row border-b border-line-soft last:border-b-0">
                  <td class="px-3 py-2"><TypeTag :type="q.type" /></td>
                  <td class="max-w-0 px-3 py-2">
                    <span class="block truncate" :title="q.stem">{{ q.stem }}</span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2 text-right">
                    <button type="button" class="text-danger hover:underline" @click="removeQuestion(q.id)">移除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <template v-else>
          <div class="flex flex-col gap-1">
            <span class="text-xs text-ink-muted">分类</span>
            <Select :model-value="form.rule.categoryId ?? 'ALL'" @update:model-value="(v: unknown) => (form.rule.categoryId = v === 'ALL' ? null : (v as number))">
              <SelectTrigger class="w-[220px] bg-reader">
                <span>{{ ruleCategoryLabel }}</span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">不限</SelectItem>
                <SelectItem v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex flex-col gap-1">
            <label for="rule-count" class="text-xs text-ink-muted">题量</label>
            <Input id="rule-count" v-model.number="form.rule.count" type="number" min="1" max="200" class="w-28 bg-reader" />
          </div>

          <div class="flex flex-col gap-1.5">
            <span class="text-xs text-ink-muted">题型</span>
            <div class="flex gap-4">
              <label v-for="t in ['SINGLE', 'MULTI', 'JUDGE']" :key="t" class="flex cursor-pointer items-center gap-2 text-sm">
                <input v-model="form.rule.types" type="checkbox" :value="t" class="size-4 accent-brand" />
                {{ { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题' }[t as QuestionType] }}
              </label>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <span class="text-xs text-ink-muted">难度</span>
            <div class="flex gap-4">
              <label v-for="d in ['EASY', 'MEDIUM', 'HARD']" :key="d" class="flex cursor-pointer items-center gap-2 text-sm">
                <input v-model="form.rule.difficulties" type="checkbox" :value="d" class="size-4 accent-brand" />
                {{ { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d as Difficulty] }}
              </label>
            </div>
          </div>

          <div class="flex flex-col gap-1">
            <label for="rule-exclude" class="text-xs text-ink-muted">排除近期</label>
            <div class="flex items-center gap-3">
              <Input id="rule-exclude" v-model.number="form.rule.excludeRecentDays" type="number" min="0" max="365" class="w-28 bg-reader" />
              <span class="text-xs text-ink-muted">排除最近 N 天已作答过的题目，0 表示不排除</span>
            </div>
          </div>

          <Button variant="link" size="sm" class="self-start" :disabled="previewing" @click="onPreview">预览抽题结果</Button>
          <div v-if="preview.length" class="overflow-hidden rounded-lg border border-line-soft">
            <table class="w-full border-collapse text-sm">
              <tbody>
                <tr v-for="q in preview" :key="q.id" class="border-b border-line-soft last:border-b-0">
                  <td class="px-3 py-2"><TypeTag :type="q.type" /></td>
                  <td class="max-w-0 px-3 py-2">
                    <span class="block truncate" :title="q.stem">{{ q.stem }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

        <div class="flex justify-end gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
          <Button variant="outline" @click="emit('update:visible', false)">取消</Button>
          <Button :disabled="saving" @click="onSave">保存</Button>
        </div>

        <!-- 内联新建：保存后自动加进当前卷，不必先跳去题库建完再回来选。
             ⚠️ 必须留在 DialogContent 子树内：reka 的焦点陷阱会把「逃出焦点域」的输入框
             焦点夺回（fill/type 全部落空）——EP 对话框渲染在这棵子树里才拿得到焦点。
             （el-overlay 是 fixed + z-index 2002，视觉与点击仍在 reka 层之上。） -->
        <QuestionEditDialog v-model:visible="createQuestionVisible" :question-id="null" @saved="onQuestionCreated" />
      </DialogContent>

    <!-- ── 选题器（嵌套对话框）───────────────────────────────────────────── -->
    <Dialog :open="selectorVisible" @update:open="(v: boolean) => (selectorVisible = v)">
      <DialogContent class="flex max-h-[85vh] max-w-4xl flex-col gap-0 overflow-hidden p-0">
        <DialogHeader class="border-b border-line-soft px-6 py-4">
          <DialogTitle class="text-base font-medium">选择题库题目</DialogTitle>
        </DialogHeader>

        <div class="flex flex-col gap-4 overflow-y-auto px-6 py-4">
          <div class="flex flex-wrap items-center gap-2">
            <Input
              v-model="selectorQuery.keyword"
              placeholder="按题干搜索"
              class="h-9 w-48 bg-reader"
              @keyup.enter="searchCandidates"
            />
            <Popover>
              <PopoverTrigger as-child>
                <button
                  type="button"
                  class="flex h-9 w-44 items-center justify-between rounded-md border border-line bg-reader px-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-focus"
                >
                  <span :class="selectorQuery.categoryIds.length ? '' : 'text-ink-subtle'">{{ selectorCategoryLabel }}</span>
                  <ChevronDownIcon class="size-4 opacity-50" />
                </button>
              </PopoverTrigger>
              <PopoverContent align="start" class="w-56 p-0">
                <div class="max-h-60 overflow-y-auto py-1">
                  <label
                    v-for="c in categories"
                    :key="c.id"
                    class="flex cursor-pointer items-center gap-2 px-3 py-1.5 text-sm hover:bg-surface-2"
                  >
                    <Checkbox
                      :model-value="selectorQuery.categoryIds.includes(c.id)"
                      @update:model-value="(v: unknown) => toggleSelectorCategory(c.id, v === true)"
                    />
                    {{ c.name }}
                  </label>
                </div>
              </PopoverContent>
            </Popover>
            <Select :model-value="selectorQuery.type || 'ALL'" @update:model-value="(v: unknown) => (selectorQuery.type = v === 'ALL' ? '' : (v as QuestionType))">
              <SelectTrigger class="h-9 w-[130px] bg-reader">
                <span>{{ selectorQuery.type ? { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题' }[selectorQuery.type as QuestionType] : '题型' }}</span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">不限</SelectItem>
                <SelectItem value="SINGLE">单选题</SelectItem>
                <SelectItem value="MULTI">多选题</SelectItem>
                <SelectItem value="JUDGE">判断题</SelectItem>
              </SelectContent>
            </Select>
            <Select
              :model-value="selectorQuery.difficulty || 'ALL'"
              @update:model-value="(v: unknown) => (selectorQuery.difficulty = v === 'ALL' ? '' : (v as Difficulty))"
            >
              <SelectTrigger class="h-9 w-[130px] bg-reader">
                <span>{{ selectorQuery.difficulty ? { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[selectorQuery.difficulty as Difficulty] : '难度' }}</span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">不限</SelectItem>
                <SelectItem value="EASY">简单</SelectItem>
                <SelectItem value="MEDIUM">中等</SelectItem>
                <SelectItem value="HARD">困难</SelectItem>
              </SelectContent>
            </Select>
            <Button size="sm" @click="searchCandidates">查询</Button>
            <Button size="sm" variant="outline" @click="resetSelectorFilters">重置</Button>
          </div>

          <div class="overflow-x-auto rounded-lg border border-line-soft" :class="loadingCandidates ? 'pointer-events-none opacity-60' : ''">
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
                  <th class="px-3 py-2.5 font-medium">题型</th>
                  <th class="px-3 py-2.5 font-medium">题干</th>
                  <th class="px-3 py-2.5 font-medium">状态</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in candidates" :key="row.id" class="selector-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
                  <td class="px-3 py-2.5">
                    <Checkbox
                      :model-value="selectedMap.has(row.id)"
                      :aria-label="`选择第 ${row.id} 题`"
                      @update:model-value="(v: unknown) => toggleCandidate(row, v === true)"
                    />
                  </td>
                  <td class="px-3 py-2.5"><TypeTag :type="row.type" /></td>
                  <td class="max-w-0 px-3 py-2.5">
                    <span class="block truncate" :title="row.stem">{{ row.stem }}</span>
                  </td>
                  <td class="whitespace-nowrap px-3 py-2.5">
                    <span
                      v-if="form.questionIds.includes(row.id)"
                      class="inline-flex items-center rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-ink-muted"
                    >
                      已在卷中
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <TablePagination
            :total="candidateTotal"
            :page="selectorQuery.page"
            :size="selectorQuery.size"
            @update:page="(p) => { selectorQuery.page = p; loadCandidates() }"
          />
        </div>

        <div class="flex items-center justify-between gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
          <span class="text-sm text-ink-muted">已选 {{ selectedMap.size }} 道</span>
          <div class="flex gap-2">
            <Button variant="outline" @click="selectorVisible = false">取消</Button>
            <Button :disabled="!selectedMap.size" @click="confirmSelection">加入试卷</Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChevronDownIcon } from '@lucide/vue'
import { getQuestion, listCategories, pageQuestions } from '@/api/question'
import { getPaper, previewRule, savePaper } from '@/api/paper'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import { Input } from '@/components/ui/input'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import TablePagination from '@/components/TablePagination.vue'
import TypeTag from '@/components/TypeTag.vue'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import QuestionEditDialog from './QuestionEditDialog.vue'
import type { Difficulty, PaperRuleDTO, QuestionListItemVO, QuestionQuery, QuestionType } from '@/types'

const props = defineProps<{ visible: boolean; paperId: number | null }>()
const emit = defineEmits(['update:visible', 'saved'])

const categories = ref<any[]>([])
const candidates = ref<QuestionListItemVO[]>([])
const selectedQuestions = ref<QuestionListItemVO[]>([])
const preview = ref<any[]>([])
const selectorVisible = ref(false)
const createQuestionVisible = ref(false)
const loadingCandidates = ref(false)
const previewing = ref(false)
const saving = ref(false)
const candidateTotal = ref(0)

/**
 * 选题器里「勾了哪些」**自己记账**，不用 el-table 的 selection。
 *
 * 理由：table 的选中状态在**数据换了就重置**，跨页勾选要靠 `reserve-selection` 才留得住，
 * 而它的 `selection-change` 到底带不带「保留的那些行」不那么直观——一旦不带，
 * 用户在第二页点「加入试卷」就会**静默丢掉第一页的选择**。用显式 checkbox + 自己的 Map，
 * 翻页前后行为完全确定，也不依赖组件内部实现。
 */
const selectedMap = ref(new Map<number, QuestionListItemVO>())
const pageAllSelected = computed(
  () => candidates.value.length > 0 && candidates.value.every((row) => selectedMap.value.has(row.id))
)
const pageSomeSelected = computed(
  () => !pageAllSelected.value && candidates.value.some((row) => selectedMap.value.has(row.id))
)

const selectorQuery = ref({
  keyword: '',
  categoryIds: [] as number[],
  type: '' as QuestionType | '',
  difficulty: '' as Difficulty | '',
  page: 1,
  size: 20
})

const selectorCategoryLabel = computed(() =>
  selectorQuery.value.categoryIds.length
    ? `已选 ${selectorQuery.value.categoryIds.length} 个分类`
    : '分类'
)

function toggleSelectorCategory(id: number, checked: boolean) {
  selectorQuery.value.categoryIds = checked
    ? [...selectorQuery.value.categoryIds, id]
    : selectorQuery.value.categoryIds.filter((v) => v !== id)
}

const emptyRule = (): PaperRuleDTO => ({
  categoryId: null,
  tagIds: [],
  types: [],
  difficulties: [],
  count: 20,
  excludeRecentDays: 0
})

const form = ref<any>({ id: null, title: '', description: '', mode: 'FIXED', questionIds: [], rule: emptyRule() })

const MODES: { value: string; label: string }[] = [
  { value: 'FIXED', label: '固定卷' },
  { value: 'RULE', label: '规则卷' }
]

const ruleCategoryLabel = computed(() => {
  if (form.value.rule.categoryId === null) return '不限'
  return categories.value.find((c) => c.id === form.value.rule.categoryId)?.name || '不限'
})

watch(
  () => [props.visible, props.paperId],
  async () => {
    if (!props.visible) return
    categories.value = await listCategories()
    preview.value = []
    if (props.paperId) {
      const paper = await getPaper(props.paperId)
      form.value = {
        id: paper.id,
        title: paper.title,
        description: paper.description || '',
        mode: paper.mode,
        questionIds: [...(paper.questionIds || [])],
        rule: paper.rule || emptyRule()
      }
      selectedQuestions.value = await loadQuestions(form.value.questionIds)
    } else {
      form.value = { id: null, title: '', description: '', mode: 'FIXED', questionIds: [], rule: emptyRule() }
      selectedQuestions.value = []
    }
  },
  { immediate: true }
)

/**
 * 按 id 把题目取回来（用于回显「已选题目」表格）。
 *
 * ⚠️ 不能只查**第一页**再 `filter`：卷里若引用了较早的题，它们根本不在第一页里，
 * 于是「共 189 道」下面只列出几行——看着像题目丢了。这里按页翻找，直到找齐或翻完为止。
 */
async function loadQuestions(ids: number[]) {
  if (!ids.length) return []
  const wanted = new Set(ids)
  const found = new Map<number, QuestionListItemVO>()
  const size = 100
  // 上限只是兜底，避免题库很大时无限翻页；正常卷几页内就找齐
  for (let page = 1; page <= 50 && wanted.size; page++) {
    const result = await pageQuestions({ page, size, scope: 'all' })
    for (const item of result.list) {
      if (wanted.delete(item.id)) found.set(item.id, item)
    }
    if (result.list.length < size) break
  }
  // 按卷里原本的顺序回显，别用查出顺序
  return ids.map((id) => found.get(id)).filter((item): item is QuestionListItemVO => !!item)
}

watch(selectorVisible, (v) => {
  if (!v) return
  // 每次打开都是一次全新的选择；已在卷中的题用「状态」列标出来，而不是预先勾上
  selectedMap.value = new Map()
  selectorQuery.value.page = 1
  loadCandidates()
})

async function loadCandidates() {
  loadingCandidates.value = true
  try {
    const query: QuestionQuery = {
      scope: 'all',
      page: selectorQuery.value.page,
      size: selectorQuery.value.size
    }
    if (selectorQuery.value.keyword) query.keyword = selectorQuery.value.keyword
    if (selectorQuery.value.categoryIds.length) query.categoryIds = selectorQuery.value.categoryIds
    if (selectorQuery.value.type) query.type = selectorQuery.value.type
    if (selectorQuery.value.difficulty) query.difficulty = selectorQuery.value.difficulty
    const result = await pageQuestions(query)
    candidates.value = result.list
    candidateTotal.value = result.total
  } finally {
    loadingCandidates.value = false
  }
}

function searchCandidates() {
  selectorQuery.value.page = 1
  loadCandidates()
}

function resetSelectorFilters() {
  selectorQuery.value.keyword = ''
  selectorQuery.value.categoryIds = []
  selectorQuery.value.type = ''
  selectorQuery.value.difficulty = ''
  searchCandidates()
}

function toggleCandidate(row: QuestionListItemVO, checked: boolean) {
  if (checked) selectedMap.value.set(row.id, row)
  else selectedMap.value.delete(row.id)
}

function togglePage(checked: boolean) {
  for (const row of candidates.value) {
    if (checked) selectedMap.value.set(row.id, row)
    else selectedMap.value.delete(row.id)
  }
}

function confirmSelection() {
  let added = 0
  let ignored = 0
  for (const item of selectedMap.value.values()) {
    if (form.value.questionIds.includes(item.id)) {
      ignored++
      continue
    }
    form.value.questionIds.push(item.id)
    selectedQuestions.value.push(item)
    added++
  }
  if (!added) {
    ElMessage.warning('所选题目都已在卷中')
  } else if (ignored) {
    ElMessage.success(`加入 ${added} 道（${ignored} 道已在卷中，已跳过）`)
  } else {
    ElMessage.success(`加入 ${added} 道`)
  }
  selectorVisible.value = false
}

/** 内联新建题目：`QuestionEditDialog` 保存后会回传新题的 id，这里顺手加进卷。 */
async function onQuestionCreated(id: unknown) {
  if (typeof id !== 'number') return
  if (form.value.questionIds.includes(id)) return
  const detail = await getQuestion(id)
  form.value.questionIds.push(id)
  selectedQuestions.value.push(detail)
  createQuestionVisible.value = false
  ElMessage.success('已新建并加入试卷')
}

function removeQuestion(id: number) {
  form.value.questionIds = form.value.questionIds.filter((qid: number) => qid !== id)
  selectedQuestions.value = selectedQuestions.value.filter((q: QuestionListItemVO) => q.id !== id)
}

async function onPreview() {
  previewing.value = true
  try {
    preview.value = await previewRule(form.value.rule)
    if (!preview.value.length) ElMessage.warning('没有符合规则的题目')
  } finally {
    previewing.value = false
  }
}

async function onSave() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写试卷标题')
    return
  }
  // ⚠️ 固定卷**允许没有题目**（先建卷、后加题，见 ADR 0026）：空卷只是不能发起作答，
  //    保存这一步不再拦。
  saving.value = true
  try {
    await savePaper(form.value)
    ElMessage.success('保存成功')
    emit('saved')
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>
