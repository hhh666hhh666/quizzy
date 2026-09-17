<template>
  <el-card v-if="result" v-loading="loading">
    <template #header>{{ result.title }} · 答题结果</template>

    <el-row :gutter="16" class="summary">
      <el-col :span="6">
        <el-statistic title="得分" :value="result.obtainedScore" :suffix="` / ${result.totalScore}`" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="正确率" :value="result.accuracy" :precision="0" suffix="%" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="答对" :value="result.correctCount" :suffix="` / ${result.answeredCount}`" />
      </el-col>
      <el-col :span="6">
        <el-statistic title="未作答" :value="result.unansweredCount" />
      </el-col>
    </el-row>

    <el-alert v-if="result.unansweredCount" type="warning" :closable="false" class="tip">
      还有 {{ result.unansweredCount }} 道题没有作答，正确率只统计已作答的题目。
    </el-alert>

    <div v-for="item in result.items" :key="item.questionId" class="item">
      <div class="item-head">
        <span class="index">第 {{ item.index + 1 }} 题</span>
        <el-tag size="small">{{ typeLabel(item.type) }}</el-tag>
        <el-tag size="small" :type="item.correct ? 'success' : item.answered ? 'danger' : 'info'">
          {{ item.correct ? '正确' : item.answered ? '错误' : '未作答' }}
        </el-tag>
        <span class="score">{{ item.correct ? item.score : 0 }} / {{ item.score }} 分</span>
      </div>
      <MarkdownRenderer :source="item.stem" />
      <ul class="option-list">
        <li
          v-for="option in item.options"
          :key="option.label"
          :class="optionClass(item, option.label)"
        >
          <span class="label">{{ option.label }}.</span>
          <MarkdownRenderer :source="option.content" />
        </li>
      </ul>
      <div class="answers">
        你的答案：{{ item.userAnswers.length ? item.userAnswers.join(', ') : '未作答' }}
        · 正确答案：{{ item.correctAnswers.join(', ') }}
      </div>
      <div v-if="item.analysis" class="analysis">
        <el-divider content-position="left">解析</el-divider>
        <MarkdownRenderer :source="item.analysis" />
      </div>
    </div>

    <div class="footer-actions">
      <el-button @click="router.push('/history')">返回答题记录</el-button>
      <el-button type="primary" @click="router.push('/wrong-book')">查看错题本</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { getSessionResult } from '@/api/quiz'
import type { QuizResultItemVO, SessionResultVO } from '@/types'

const route = useRoute()
const router = useRouter()
const result = ref<SessionResultVO | null>(null)
const loading = ref(false)

function optionClass(item: QuizResultItemVO, label: string) {
  const isCorrect = item.correctAnswers.includes(label)
  const isPicked = item.userAnswers.includes(label)
  if (isCorrect) return 'option correct'
  if (isPicked) return 'option wrong'
  return 'option'
}

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

onMounted(async () => {
  loading.value = true
  try {
    result.value = await getSessionResult(Number(route.params.id))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.summary { margin-bottom: 16px; }
.tip { margin-bottom: 12px; }
.item { padding: 14px 0; border-top: 1px solid #ebeef5; }
.item-head { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.index { font-weight: 600; }
.score { color: #909399; font-size: 12px; }
.option-list { list-style: none; padding: 0; margin: 8px 0; }
.option { padding: 4px 8px; border-radius: 4px; }
.option.correct { background: #f0f9eb; color: #67c23a; }
.option.wrong { background: #fef0f0; color: #f56c6c; }
.label { font-weight: 600; margin-right: 4px; }
.answers { font-size: 13px; color: #606266; }
.footer-actions { margin-top: 16px; display: flex; gap: 10px; }
</style>
