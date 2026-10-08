<template>
  <div class="font-sans text-base text-ink">
    <div v-if="result" class="rounded-xl bg-surface p-6 shadow-sm" :class="loading ? 'pointer-events-none opacity-60' : ''">
      <h1 class="text-base font-medium">{{ result.title }} · 答题结果</h1>

      <!-- 指标带：蓝 tint 容器（v2 口径的大面积色调面），四项指标平铺 -->
      <div class="mt-4 grid grid-cols-2 gap-4 rounded-xl bg-tint-blue p-5 sm:grid-cols-4">
        <div>
          <div class="text-xs text-ink-muted">得分</div>
          <div class="mt-1 text-2xl font-semibold">{{ result.obtainedScore }}<span class="text-sm font-normal text-ink-muted"> / {{ result.totalScore }}</span></div>
        </div>
        <div>
          <div class="text-xs text-ink-muted">正确率</div>
          <div class="mt-1 text-2xl font-semibold">{{ result.accuracy }}<span class="text-sm font-normal text-ink-muted">%</span></div>
        </div>
        <div>
          <div class="text-xs text-ink-muted">答对</div>
          <div class="mt-1 text-2xl font-semibold">{{ result.correctCount }}<span class="text-sm font-normal text-ink-muted"> / {{ result.answeredCount }}</span></div>
        </div>
        <div>
          <div class="text-xs text-ink-muted">未作答</div>
          <div class="mt-1 text-2xl font-semibold">{{ result.unansweredCount }}</div>
        </div>
      </div>

      <p v-if="result.unansweredCount" class="mt-3 rounded-md bg-tint-amber/50 px-3 py-2 text-sm">
        还有 {{ result.unansweredCount }} 道题没有作答，正确率只统计已作答的题目。
      </p>

      <div v-for="item in result.items" :key="item.questionId" class="mt-5 border-t border-line-soft pt-5">
        <div class="flex flex-wrap items-center gap-2">
          <span class="font-semibold">第 {{ item.index + 1 }} 题</span>
          <TypeTag :type="item.type" />
          <span
            class="inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium"
            :class="item.correct ? 'bg-tint-green text-ink-green' : item.answered ? 'bg-danger-soft text-ink-red' : 'bg-surface-2 text-ink-muted'"
          >
            {{ item.correct ? '正确' : item.answered ? '错误' : '未作答' }}
          </span>
          <span class="text-xs text-ink-muted">{{ item.correct ? item.score : 0 }} / {{ item.score }} 分</span>
          <!--
            收藏星标。⚠️ 结果页上「这题答错了」这件事**已经自动进错题本了**，
            所以这里的星标只表达「我要留着自己再看」——两个集合的含义不同，别混。
          -->
          <FavoriteStar class="ml-auto" :question-id="item.questionId" :favorited="item.favorited" @change="onStarChange(item, $event)" />
        </div>

        <!-- 题干 + 选项 + 答案 + 解析 = 阅读面：近白块，不上色 -->
        <div class="reader mt-2 rounded-lg border border-line-soft bg-reader p-4">
          <MarkdownRenderer :source="item.stem" />
          <ul class="mt-2 flex flex-col gap-1">
            <li
              v-for="option in item.options"
              :key="option.label"
              class="flex items-start gap-2 rounded-md px-2 py-1.5"
              :class="optionClass(item, option.label)"
            >
              <span class="shrink-0 font-semibold" :class="optionLabelClass(item, option.label)">{{ option.label }}.</span>
              <span class="min-w-0 flex-1"><MarkdownRenderer :source="option.content" /></span>
            </li>
          </ul>
          <p class="mt-3 text-sm">
            你的答案：{{ item.userAnswers.length ? item.userAnswers.join(', ') : '未作答' }}
            · 正确答案：{{ item.correctAnswers.join(', ') }}
          </p>
          <div v-if="item.analysis" class="mt-3 border-t border-line-soft pt-3">
            <div class="text-xs font-medium text-ink-muted">解析</div>
            <div class="mt-1 text-sm">
              <MarkdownRenderer :source="item.analysis" />
            </div>
          </div>
        </div>
      </div>

      <div class="mt-6 flex gap-2">
        <Button variant="outline" @click="router.push('/history')">返回答题记录</Button>
        <Button @click="router.push('/wrong-book')">查看错题本</Button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FavoriteStar from '@/components/FavoriteStar.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import TypeTag from '@/components/TypeTag.vue'
import { Button } from '@/components/ui/button'
import { getSessionResult } from '@/api/quiz'
import type { QuizResultItemVO, SessionResultVO } from '@/types'

const route = useRoute()
const router = useRouter()
const result = ref<SessionResultVO | null>(null)
const loading = ref(false)

/** 行底色只做轻提示；正确/错误的语义由标号颜色与小标签承担（状态色不做大色块文字）。 */
function optionClass(item: QuizResultItemVO, label: string) {
  const isCorrect = item.correctAnswers.includes(label)
  const isPicked = item.userAnswers.includes(label)
  if (isCorrect) return 'bg-tint-green/50'
  if (isPicked) return 'bg-danger-soft/50'
  return 'hover:bg-surface-2/50'
}

function optionLabelClass(item: QuizResultItemVO, label: string) {
  const isCorrect = item.correctAnswers.includes(label)
  const isPicked = item.userAnswers.includes(label)
  if (isCorrect) return 'text-ink-green'
  if (isPicked) return 'text-ink-red'
  return ''
}

function onStarChange(item: QuizResultItemVO, favorited: boolean) {
  item.favorited = favorited
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
