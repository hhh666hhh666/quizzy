<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑试卷' : '新建试卷'"
    width="760px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <el-form :model="form" label-width="80px">
      <el-form-item label="标题">
        <el-input v-model="form.title" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="模式">
        <el-radio-group v-model="form.mode">
          <el-radio-button value="FIXED">固定卷</el-radio-button>
          <el-radio-button value="RULE">规则卷</el-radio-button>
        </el-radio-group>
        <span class="hint">
          {{ form.mode === 'FIXED' ? '题目固定，每次作答都是同一批题' : '每次作答按规则现场抽题' }}
        </span>
      </el-form-item>

      <template v-if="form.mode === 'FIXED'">
        <el-form-item label="已选题目">
          <span>共 {{ form.questionIds.length }} 道</span>
          <el-button link type="primary" @click="selectorVisible = true">选择题库题目</el-button>
          <el-button link type="primary" @click="createQuestionVisible = true">新建题目</el-button>
        </el-form-item>
        <el-alert
          v-if="!form.questionIds.length"
          class="empty-hint"
          type="info"
          :closable="false"
          show-icon
          title="空卷可以先保存，之后再往里加题；空卷不能发起作答。"
        />
        <el-table :data="selectedQuestions" max-height="220" border>
          <el-table-column label="题型" width="80">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" show-overflow-tooltip />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="danger" @click="removeQuestion(row.id)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-else>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.rule.categoryId" clearable>
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题量">
              <el-input-number v-model="form.rule.count" :min="1" :max="200" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="题型">
          <el-checkbox-group v-model="form.rule.types">
            <el-checkbox value="SINGLE" label="单选题" />
            <el-checkbox value="MULTI" label="多选题" />
            <el-checkbox value="JUDGE" label="判断题" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="难度">
          <el-checkbox-group v-model="form.rule.difficulties">
            <el-checkbox value="EASY" label="简单" />
            <el-checkbox value="MEDIUM" label="中等" />
            <el-checkbox value="HARD" label="困难" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="排除近期">
          <el-input-number v-model="form.rule.excludeRecentDays" :min="0" :max="365" />
          <span class="hint">排除最近 N 天已作答过的题目，0 表示不排除</span>
        </el-form-item>
        <el-button link type="primary" :loading="previewing" @click="onPreview">预览抽题结果</el-button>
        <el-table v-if="preview.length" :data="preview" max-height="220" border>
          <el-table-column label="题型" width="80">
            <template #default="{ row }">{{ typeLabel(row.type) }}</template>
          </el-table-column>
          <el-table-column prop="stem" label="题干" show-overflow-tooltip />
        </el-table>
      </template>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>

    <el-dialog v-model="selectorVisible" title="选择题库题目" width="880px" append-to-body>
      <div class="selector-filters">
        <el-input
          v-model="selectorQuery.keyword"
          placeholder="按题干搜索"
          clearable
          class="f-keyword"
          @keyup.enter="searchCandidates"
        />
        <el-select v-model="selectorQuery.categoryIds" multiple collapse-tags clearable placeholder="分类" class="f-cate">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="selectorQuery.type" clearable placeholder="题型" class="f-type">
          <el-option label="单选题" value="SINGLE" />
          <el-option label="多选题" value="MULTI" />
          <el-option label="判断题" value="JUDGE" />
        </el-select>
        <el-select v-model="selectorQuery.difficulty" clearable placeholder="难度" class="f-diff">
          <el-option label="简单" value="EASY" />
          <el-option label="中等" value="MEDIUM" />
          <el-option label="困难" value="HARD" />
        </el-select>
        <el-button type="primary" @click="searchCandidates">查询</el-button>
        <el-button @click="resetSelectorFilters">重置</el-button>
      </div>

      <el-table :data="candidates" v-loading="loadingCandidates" border max-height="360">
        <el-table-column width="50">
          <template #header>
            <el-checkbox
              :model-value="pageAllSelected"
              :indeterminate="pageSomeSelected"
              @change="(v: any) => togglePage(!!v)"
            />
          </template>
          <template #default="{ row }">
            <el-checkbox :model-value="selectedMap.has(row.id)" @change="(v: any) => toggleCandidate(row, !!v)" />
          </template>
        </el-table-column>
        <el-table-column label="题型" width="80">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="form.questionIds.includes(row.id)" size="small" type="info">已在卷中</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="selector-pagination"
        layout="total, prev, pager, next"
        :total="candidateTotal"
        v-model:current-page="selectorQuery.page"
        v-model:page-size="selectorQuery.size"
        @change="loadCandidates"
      />

      <template #footer>
        <span class="selector-count">已选 {{ selectedMap.size }} 道</span>
        <el-button @click="selectorVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!selectedMap.size" @click="confirmSelection">加入试卷</el-button>
      </template>
    </el-dialog>

    <!-- 内联新建：保存后自动加进当前卷，不必先跳去题库建完再回来选 -->
    <QuestionEditDialog v-model:visible="createQuestionVisible" :question-id="null" @saved="onQuestionCreated" />
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestion, listCategories, pageQuestions } from '@/api/question'
import { getPaper, previewRule, savePaper } from '@/api/paper'
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

const emptyRule = (): PaperRuleDTO => ({
  categoryId: null,
  tagIds: [],
  types: [],
  difficulties: [],
  count: 20,
  excludeRecentDays: 0
})

const form = ref<any>({ id: null, title: '', description: '', mode: 'FIXED', questionIds: [], rule: emptyRule() })

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

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}
</script>

<style scoped>
.hint { margin-left: 12px; color: var(--el-text-color-secondary); font-size: 12px; }
.empty-hint { margin-bottom: 12px; }
.selector-filters { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.f-keyword { width: 200px; }
.f-cate { width: 190px; }
.f-type, .f-diff { width: 110px; }
.selector-pagination { margin-top: 12px; justify-content: flex-end; }
.selector-count { margin-right: auto; color: var(--el-text-color-secondary); font-size: 13px; }
</style>
