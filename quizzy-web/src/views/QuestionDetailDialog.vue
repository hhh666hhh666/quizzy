<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <!--
      ⚠️ 宽度必须连 `sm:max-w-*` 一起写。DialogContent 内置了 `sm:max-w-sm`（384px），
      它带 `sm:` 修饰符、与本地的 `max-w-3xl` 不同组，twMerge 不会合并掉它；
      而在 ≥640px 视口下媒体查询的优先级更高 → 只写 `max-w-3xl` 实际只有 384px 宽。
      宽度改由 JS 内联 style 给（可拖动调整），所以这里只用 max-w 兜底一个上限。
    -->
    <DialogContent
      class="flex max-h-[85vh] flex-col gap-0 overflow-hidden p-0 sm:max-w-none"
      :style="shellStyle"
    >
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
              <!-- 字母给固定宽度 + 与正文同 line-height：字母和正文首行才会落在同一条基线上 -->
              <span
                class="option-label shrink-0 font-semibold"
                :class="detail.answers.includes(option.label) ? 'text-ink-green' : ''"
              >{{ option.label }}</span>
              <div class="option-body min-w-0 flex-1"><MarkdownRenderer :source="option.content" /></div>
              <span v-if="detail.answers.includes(option.label)" class="shrink-0 text-xs leading-7 text-ink-green">正确答案</span>
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

      <!--
        拖拽调宽手柄：贴右边缘、竖条热区 12px（视觉上只有 2px 线）。
        「自由调节宽度」只做横向——纵向已有 max-h-[85vh] + 内部滚动，再给纵拖会两头打架。
      -->
      <div
        class="absolute inset-y-0 right-0 z-10 hidden w-3 cursor-ew-resize select-none sm:block"
        role="separator"
        aria-orientation="vertical"
        aria-label="拖动调整题目详情宽度"
        data-testid="detail-resize-handle"
        @pointerdown="startResize"
        @dblclick="resetWidth"
      >
        <span class="pointer-events-none absolute inset-y-2 right-1 w-0.5 rounded-full bg-line transition-colors" :class="resizing ? 'bg-brand' : ''" />
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
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

/* ---------- 可拖动调宽 ---------- */
// 宽度是「居中卡片」的宽度：拖动时按住的是右边缘，卡片中心不动，
// 所以宽度变化 = 指针位移 × 2。这样手感与直觉一致（往右拖两头同时张开）。
const MIN_W = 480
const DEFAULT_W = 960
const STORAGE_KEY = 'quizzy:detail-dialog-width'

/** 上限取视口宽 - 32px，给遮罩留边；低于 MIN_W 的视口直接钉在 MIN_W。 */
function maxWidth() {
  return Math.max(MIN_W, window.innerWidth - 32)
}

const width = ref(loadWidth())
const resizing = ref(false)

function loadWidth(): number {
  const raw = Number(localStorage.getItem(STORAGE_KEY))
  if (!Number.isFinite(raw) || raw <= 0) return DEFAULT_W
  return Math.min(Math.max(raw, MIN_W), maxWidth())
}

const shellStyle = computed(() => ({ width: `${width.value}px` }))

let startX = 0
let startW = 0

function startResize(e: PointerEvent) {
  if (e.button !== 0) return
  resizing.value = true
  startX = e.clientX
  startW = width.value
  // 指针可能移出手柄，用 pointer capture 保证后续 move 仍然回到这里
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
  window.addEventListener('pointermove', onResizeMove)
  window.addEventListener('pointerup', endResize)
  window.addEventListener('pointercancel', endResize)
}

function onResizeMove(e: PointerEvent) {
  if (!resizing.value) return
  const next = startW + (e.clientX - startX) * 2
  width.value = Math.round(Math.min(Math.max(next, MIN_W), maxWidth()))
}

function endResize(e: PointerEvent) {
  if (!resizing.value) return
  resizing.value = false
  ;(e.currentTarget as HTMLElement)?.releasePointerCapture?.(e.pointerId)
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', endResize)
  window.removeEventListener('pointercancel', endResize)
  localStorage.setItem(STORAGE_KEY, String(width.value))
}

/** 双击手柄复位——拖歪了不用靠手拖回去。 */
function resetWidth() {
  width.value = Math.min(DEFAULT_W, maxWidth())
  localStorage.setItem(STORAGE_KEY, String(width.value))
}

/** 视口变窄时把已存的宽度收回来，避免卡片横向溢出。 */
function onWindowResize() {
  width.value = Math.min(width.value, maxWidth())
}

watch(resizing, (on) => {
  document.body.style.cursor = on ? 'ew-resize' : ''
  document.body.style.userSelect = on ? 'none' : ''
  if (on) window.addEventListener('resize', onWindowResize)
  else window.removeEventListener('resize', onWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', endResize)
  window.removeEventListener('pointercancel', endResize)
  window.removeEventListener('resize', onWindowResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
})

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

<style scoped>
/* 选项字母与正文首行对齐。
   ⚠️ 病根在 MarkdownRenderer：它的 `.markdown-body :deep(p)` 有 `margin: 6px 0`，
   而子组件用的是 scoped style，父组件这边的选择器够不到那层 `p`。
   实测（.workbuddy/measure-detail.mjs）：字母文本顶边 8px、正文文本顶边 15px，差 7px。
   解法是把行内 markdown 的首尾段距抹掉，并给字母一个与正文同 line-height 的盒子。

   line-height 取 1.75（= markdown-body 的 1.7 × 14px ≈ 24px 行盒，取整成 28/16 便于对齐）。 */
.option-label,
.option-body :deep(.markdown-body) {
  line-height: 1.75rem;
}

/* 字母列宽固定 1.25rem（够放 A–Z 与多选组合），保证正文起点在各行对齐 */
.option-label {
  width: 1.25rem;
}

/* 压掉 markdown 首尾段距：首段的 6px 上边距就是那 7px 错位的来源 */
.option-body :deep(.markdown-body p:first-child) {
  margin-top: 0;
}
.option-body :deep(.markdown-body p:last-child) {
  margin-bottom: 0;
}

/* 手柄拖动时给整个卡片一个明确的可拖拽提示（浏览器会带光标，这里补个视觉反馈） */
[data-testid='detail-resize-handle']:hover span,
[data-testid='detail-resize-handle']:focus-visible span {
  background: var(--color-brand);
}
</style>
