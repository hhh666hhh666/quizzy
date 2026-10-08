<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="flex max-h-[85vh] max-w-3xl flex-col gap-0 overflow-hidden p-0">
      <DialogHeader class="border-b border-line-soft px-6 py-4">
        <DialogTitle class="text-base font-medium">题目详情</DialogTitle>
      </DialogHeader>

      <div
        v-if="detail"
        class="flex flex-col gap-4 overflow-y-auto px-6 py-5"
        :class="loading ? 'pointer-events-none opacity-60' : ''"
      >
        <div class="flex flex-wrap items-center gap-2">
          <TypeTag :type="detail.type" />
          <DifficultyTag :level="detail.difficulty" />
          <span class="inline-flex items-center rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-ink-muted">{{ detail.score }} 分</span>
          <span class="inline-flex items-center rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-ink-muted">{{ detail.categoryName || '未分类' }}</span>
          <span
            class="inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium"
            :class="detail.ownerId ? 'bg-tint-green text-ink-green' : 'bg-tint-amber text-ink-amber'"
          >
            {{ detail.ownerId ? '我的题' : '公开题（只读）' }}
          </span>
          <span v-if="detail.inWrongBook" class="inline-flex items-center rounded-sm bg-danger-soft px-2 py-0.5 text-xs font-medium text-ink-red">
            在错题本
          </span>
          <FavoriteStar class="ml-auto" :question-id="detail.id" :favorited="detail.favorited" @change="onStarChange" />
        </div>

        <div v-if="detail.tags?.length" class="flex flex-wrap gap-1.5">
          <span v-for="t in detail.tags" :key="t.id" class="rounded-sm bg-surface-2 px-2 py-0.5 text-xs text-ink-muted">{{ t.name }}</span>
        </div>

        <div>
          <div class="mb-1.5 text-sm font-semibold">题干</div>
          <div class="rounded-lg border border-line-soft bg-reader p-4">
            <MarkdownRenderer :source="detail.stem" />
          </div>
        </div>

        <div>
          <div class="mb-1.5 text-sm font-semibold">选项</div>
          <div class="flex flex-col gap-1">
            <div
              v-for="option in detail.options"
              :key="option.label"
              class="option-row flex items-start gap-2 rounded-md px-2 py-1.5"
              :class="detail.answers.includes(option.label) ? 'bg-tint-green/50' : ''"
            >
              <span class="shrink-0 font-semibold" :class="detail.answers.includes(option.label) ? 'text-ink-green' : ''">{{ option.label }}</span>
              <div class="min-w-0 flex-1"><MarkdownRenderer :source="option.content" /></div>
              <span v-if="detail.answers.includes(option.label)" class="shrink-0 text-xs text-ink-green">正确答案</span>
            </div>
          </div>
        </div>

        <div class="text-sm font-semibold">
          正确答案 <span class="ml-1 font-bold text-ink-green">{{ detail.answers.join('、') }}</span>
        </div>

        <div>
          <div class="mb-1.5 text-sm font-semibold">解析</div>
          <MarkdownRenderer v-if="detail.analysis" :source="detail.analysis" />
          <span v-else class="text-sm text-ink-muted">暂无解析</span>
        </div>

        <div class="flex flex-wrap gap-5 text-sm">
          <span>作答次数：<b>{{ detail.answerCount }}</b></span>
          <span>答对次数：<b>{{ detail.correctCount }}</b></span>
          <span>
            正确率：
            <b :class="rateClass">
              {{ detail.answerCount > 0 ? Math.round((detail.correctCount / detail.answerCount) * 100) + '%' : '—' }}
            </b>
          </span>
        </div>
      </div>
      <div v-else class="px-6 py-10 text-center text-sm text-ink-muted">加载中…</div>

      <div class="flex justify-end gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
        <Button variant="outline" @click="emit('update:visible', false)">关闭</Button>
        <Button v-if="detail?.editable" @click="onEdit">编辑此题</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getQuestion } from '@/api/question'
import DifficultyTag from '@/components/DifficultyTag.vue'
import FavoriteStar from '@/components/FavoriteStar.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import TypeTag from '@/components/TypeTag.vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import type { QuestionVO } from '@/types'

const props = defineProps<{ visible: boolean; questionId: number | null }>()
const emit = defineEmits(['update:visible', 'edit'])

const detail = ref<QuestionVO | null>(null)
const loading = ref(false)

const rateClass = computed(() => {
  const d = detail.value
  if (!d || d.answerCount === 0) return ''
  return d.correctCount / d.answerCount >= 0.6 ? 'text-ink-green' : 'text-ink-red'
})

// ⚠️ source 返回稳定字符串：返回新数组会被 Vue 按引用判定变化，组件每次重渲染都会重取详情
watch(
  () => (props.visible && props.questionId ? `open:${props.questionId}` : 'closed'),
  async (key) => {
    if (key === 'closed') return
    loading.value = true
    try {
      detail.value = await getQuestion(Number(props.questionId))
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

/** 星标只同步这一份详情，不回头刷列表——列表页自己有星标，它那行会各自更新。 */
function onStarChange(favorited: boolean) {
  if (detail.value) {
    detail.value.favorited = favorited
  }
}
</script>
