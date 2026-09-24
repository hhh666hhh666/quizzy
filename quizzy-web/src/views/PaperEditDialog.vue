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
        </el-form-item>
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

    <el-dialog v-model="selectorVisible" title="选择题库题目" width="720px" append-to-body>
      <el-table :data="candidates" v-loading="loadingCandidates" border max-height="360" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="45" />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button @click="selectorVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSelection">加入试卷</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { listCategories, pageQuestions } from '@/api/question'
import { getPaper, previewRule, savePaper } from '@/api/paper'
import type { PaperRuleDTO, QuestionListItemVO } from '@/types'

const props = defineProps<{ visible: boolean; paperId: number | null }>()
const emit = defineEmits(['update:visible', 'saved'])

const categories = ref<any[]>([])
const candidates = ref<QuestionListItemVO[]>([])
const selectedQuestions = ref<QuestionListItemVO[]>([])
const preview = ref<any[]>([])
const selectorVisible = ref(false)
const loadingCandidates = ref(false)
const previewing = ref(false)
const saving = ref(false)
const pendingSelection = ref<QuestionListItemVO[]>([])

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
      selectedQuestions.value = paper.questionIds?.length ? await loadQuestions(paper.questionIds) : []
    } else {
      form.value = { id: null, title: '', description: '', mode: 'FIXED', questionIds: [], rule: emptyRule() }
      selectedQuestions.value = []
    }
  },
  { immediate: true }
)

async function loadQuestions(ids: number[]) {
  const result = await pageQuestions({ page: 1, size: 100, scope: 'all' })
  return result.list.filter((item) => ids.includes(item.id))
}

async function openSelector() {
  selectorVisible.value = true
  loadingCandidates.value = true
  try {
    const result = await pageQuestions({ page: 1, size: 100, scope: 'all' })
    candidates.value = result.list
  } finally {
    loadingCandidates.value = false
  }
}

watch(selectorVisible, (v) => {
  if (v) openSelector()
})

function onSelectionChange(rows: QuestionListItemVO[]) {
  pendingSelection.value = rows
}

function confirmSelection() {
  for (const item of pendingSelection.value) {
    if (!form.value.questionIds.includes(item.id)) {
      form.value.questionIds.push(item.id)
      selectedQuestions.value.push(item)
    }
  }
  selectorVisible.value = false
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
  if (form.value.mode === 'FIXED' && !form.value.questionIds.length) {
    ElMessage.warning('固定卷需要至少选择一道题')
    return
  }
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
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
</style>
