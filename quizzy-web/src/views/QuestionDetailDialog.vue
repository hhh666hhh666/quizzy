<template>
  <el-dialog
    :model-value="visible"
    title="题目详情"
    width="720px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <div v-loading="loading">
      <template v-if="detail">
        <div class="meta">
          <el-tag size="small">{{ typeLabel(detail.type) }}</el-tag>
          <el-tag size="small" :type="difficultyTag(detail.difficulty)">{{ difficultyLabel(detail.difficulty) }}</el-tag>
          <el-tag size="small" type="info">{{ detail.score }} 分</el-tag>
          <el-tag size="small" type="info">{{ detail.categoryName || '未分类' }}</el-tag>
          <el-tag size="small" :type="detail.ownerId ? 'success' : 'warning'">
            {{ detail.ownerId ? '我的题' : '公开题（只读）' }}
          </el-tag>
          <el-tag v-if="detail.inWrongBook" size="small" type="danger">在错题本</el-tag>
        </div>

        <div v-if="detail.tags?.length" class="meta">
          <el-tag v-for="t in detail.tags" :key="t.id" size="small" effect="plain" type="info">
            {{ t.name }}
          </el-tag>
        </div>

        <div class="block">
          <div class="block-title">题干</div>
          <MarkdownRenderer :source="detail.stem" />
        </div>

        <div class="block">
          <div class="block-title">选项</div>
          <div
            v-for="option in detail.options"
            :key="option.label"
            class="option-row"
            :class="{ correct: detail.answers.includes(option.label) }"
          >
            <span class="label">{{ option.label }}</span>
            <div class="content"><MarkdownRenderer :source="option.content" /></div>
            <span v-if="detail.answers.includes(option.label)" class="flag">正确答案</span>
          </div>
        </div>

        <div class="block">
          <div class="block-title">
            正确答案
            <span class="answer">{{ detail.answers.join('、') }}</span>
          </div>
        </div>

        <div class="block">
          <div class="block-title">解析</div>
          <MarkdownRenderer v-if="detail.analysis" :source="detail.analysis" />
          <span v-else class="empty">暂无解析</span>
        </div>

        <div class="block stat">
          <span>作答次数：<b>{{ detail.answerCount }}</b></span>
          <span>答对次数：<b>{{ detail.correctCount }}</b></span>
          <span>
            正确率：
            <b :class="rateClass">
              {{ detail.answerCount > 0 ? Math.round((detail.correctCount / detail.answerCount) * 100) + '%' : '—' }}
            </b>
          </span>
        </div>
      </template>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
      <el-button v-if="detail?.editable" type="primary" @click="onEdit">编辑此题</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getQuestion } from '@/api/question'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import type { QuestionVO } from '@/types'

const props = defineProps<{ visible: boolean; questionId: number | null }>()
const emit = defineEmits(['update:visible', 'edit'])

const detail = ref<QuestionVO | null>(null)
const loading = ref(false)

const rateClass = computed(() => {
  const d = detail.value
  if (!d || d.answerCount === 0) return ''
  return d.correctCount / d.answerCount >= 0.6 ? 'rate-high' : 'rate-low'
})

watch(
  () => [props.visible, props.questionId],
  async () => {
    if (!props.visible || !props.questionId) return
    loading.value = true
    try {
      detail.value = await getQuestion(props.questionId)
    } finally {
      loading.value = false
    }
  },
  { immediate: true }
)

function onEdit() {
  emit('edit', props.questionId)
  emit('update:visible', false)
}

function typeLabel(type: string) {
  return { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题' }[type] || type
}

function difficultyLabel(level: string) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[level] || level
}

function difficultyTag(level: string) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[level] || 'info'
}
</script>

<style scoped>
.meta { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.block { margin-bottom: 16px; }
.block-title { font-weight: 600; margin-bottom: 6px; font-size: 14px; }
.answer { color: #67c23a; font-weight: 700; margin-left: 4px; }
.option-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
}
.option-row.correct { background: #f0f9eb; }
.option-row .label { width: 18px; font-weight: 600; flex-shrink: 0; line-height: 1.7; }
.option-row .content { flex: 1; min-width: 0; }
.option-row .flag { color: #67c23a; font-size: 12px; flex-shrink: 0; line-height: 1.7; }
.empty { color: #909399; font-size: 13px; }
.stat { display: flex; gap: 20px; font-size: 13px; color: #606266; }
.rate-high { color: #67c23a; }
.rate-low { color: #f56c6c; }
</style>
