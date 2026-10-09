<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <!--
      ⚠️ 宽度必须连 `sm:max-w-*` 一起写。DialogContent 内置了 `sm:max-w-sm`（384px），
      它带 `sm:` 修饰符、与本地的 `max-w-3xl` 不同组，twMerge 不会合并掉它；
      而在 ≥640px 视口下媒体查询的优先级更高 → 只写 `max-w-3xl` 实际只有 384px 宽。
      宽高都改由 JS 内联 style 给（可拖动调整），所以这里不再写任何 max-w / max-h——
      `max-h-[85vh]` 会和「主人手动拖出来的高度」打架（拖高了也被 85vh 卡住）。
      溢出控制交给内部滚动区 + `.detail-body` 的滚动条样式。
    -->
    <DialogContent
      class="detail-shell flex flex-col gap-0 overflow-hidden p-0 sm:max-w-none"
      :style="shellStyle"
    >
      <DialogHeader class="shrink-0 border-b border-line-soft px-6 py-4">
        <DialogTitle class="text-base font-medium">题目详情</DialogTitle>
      </DialogHeader>

      <div
        v-if="detail"
        class="detail-body flex flex-1 flex-col gap-4 overflow-y-auto px-6 py-5"
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
      <div v-else class="flex flex-1 items-center justify-center text-sm text-ink-muted">加载中…</div>

      <div class="flex shrink-0 justify-end gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
        <Button variant="outline" @click="emit('update:visible', false)">关闭</Button>
        <Button v-if="detail?.editable" @click="onEdit">编辑此题</Button>
      </div>

      <!--
        三个拖拽热区（都不画任何线，靠光标提示；对照实测：右边缘曾经同时有
        15px 浏览器默认滚动条 + 一根 2px 手柄淡线，两条竖线贴着同一侧很脏）。
        `dir` 决定这条热区改哪个方向：x = 只调宽、y = 只调高、both = 右下角斜拖。
        双击任一热区复位默认尺寸。
      -->
      <div
        v-for="h in HANDLES"
        :key="h.dir"
        class="absolute z-10 hidden select-none sm:block"
        :class="h.cls"
        role="separator"
        :aria-orientation="h.dir === 'x' ? 'vertical' : 'horizontal'"
        :aria-label="h.label"
        :data-testid="`detail-resize-handle-${h.dir}`"
        @pointerdown="(e: PointerEvent) => startResize(e, h.dir)"
        @dblclick="resetSize"
      />
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

/* ---------- 可拖动调整尺寸 ---------- */
// 卡片是**水平 + 垂直双向居中**的（DialogContent 内置 top-1/2 left-1/2 + 位移 -50%），
// 所以拖任一边时是「两头一起张开」：尺寸变化 = 指针位移 × 2。
// 两个方向共用同一口径，手感一致（往右拖变宽、往下拖变高）。
const MIN_W = 480
const MIN_H = 320
const DEFAULT_W = 960
const DEFAULT_H = 640
const KEY_W = 'quizzy:detail-dialog-width'
const KEY_H = 'quizzy:detail-dialog-height'

/** 上限各留 32px 给遮罩，不至于顶到视口边。 */
const maxWidth = () => Math.max(MIN_W, window.innerWidth - 32)
const maxHeight = () => Math.max(MIN_H, window.innerHeight - 32)

const readDim = (key: string, def: number, min: number, max: number) => {
  const raw = Number(localStorage.getItem(key))
  if (!Number.isFinite(raw) || raw <= 0) return def
  return Math.min(Math.max(raw, min), max)
}

const width = ref(readDim(KEY_W, DEFAULT_W, MIN_W, maxWidth()))
const height = ref(readDim(KEY_H, DEFAULT_H, MIN_H, maxHeight()))

/** 拖动方向：x = 只调宽、y = 只调高、both = 右下角斜拖。 */
type Dir = 'x' | 'y' | 'both'
const resizing = ref<Dir | null>(null)

const HANDLES: { dir: Dir; cls: string; label: string }[] = [
  { dir: 'x', cls: 'inset-y-0 right-0 w-2 cursor-ew-resize', label: '拖动调整题目详情宽度' },
  { dir: 'y', cls: 'inset-x-0 bottom-0 h-2 cursor-ns-resize', label: '拖动调整题目详情高度' },
  { dir: 'both', cls: 'bottom-0 right-0 size-4 cursor-nwse-resize', label: '拖动调整题目详情宽高' },
]

// ⚠️ 高度写死（而不是 max-height）：主人选了「钉住给的高度」——
// 拖多少就是多少，内容不够高就留白，所见即所得。内容超出则在内部滚动。
const shellStyle = computed(() => ({
  width: `${width.value}px`,
  height: `${height.value}px`,
}))

let startX = 0
let startY = 0
let startW = 0
let startH = 0
// ⚠️ 必须自己记住「是哪个元素捕获了指针」。
// 早先写成 `e.currentTarget.releasePointerCapture()`，但 endResize 是绑在 **window** 上的，
// 事件冒到 window 时 `e.currentTarget === window`，而 window 没有这个方法 →
// 可选链 `.?.` 静默跳过 → **capture 永远没释放**。
// 后果：拖过手柄之后，浏览器把后续指针事件都投给那个手柄，
// 页面上其它元素的 :hover 不再更新（实测：拖完后滚动条拇指怎么也 hover 不出来）。
let captureEl: HTMLElement | null = null

function startResize(e: PointerEvent, dir: Dir) {
  if (e.button !== 0) return
  resizing.value = dir
  startX = e.clientX
  startY = e.clientY
  startW = width.value
  startH = height.value
  // 指针可能移出热区，用 pointer capture 保证后续 move 仍然回到这里
  captureEl = e.currentTarget as HTMLElement
  captureEl.setPointerCapture(e.pointerId)
  window.addEventListener('pointermove', onResizeMove)
  window.addEventListener('pointerup', endResize)
  window.addEventListener('pointercancel', endResize)
}

function onResizeMove(e: PointerEvent) {
  const dir = resizing.value
  if (!dir) return
  if (dir === 'x' || dir === 'both') {
    width.value = Math.round(Math.min(Math.max(startW + (e.clientX - startX) * 2, MIN_W), maxWidth()))
  }
  if (dir === 'y' || dir === 'both') {
    height.value = Math.round(Math.min(Math.max(startH + (e.clientY - startY) * 2, MIN_H), maxHeight()))
  }
}

function endResize(e: PointerEvent) {
  if (!resizing.value) return
  resizing.value = null
  // 释放捕获要用**当初捕获的那个元素**，不能用 e.currentTarget（那是 window）
  if (captureEl?.hasPointerCapture?.(e.pointerId)) captureEl.releasePointerCapture(e.pointerId)
  captureEl = null
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', endResize)
  window.removeEventListener('pointercancel', endResize)
  localStorage.setItem(KEY_W, String(width.value))
  localStorage.setItem(KEY_H, String(height.value))
}

/** 双击热区复位——拖歪了不用靠手拖回去。 */
function resetSize() {
  width.value = Math.min(DEFAULT_W, maxWidth())
  height.value = Math.min(DEFAULT_H, maxHeight())
  localStorage.setItem(KEY_W, String(width.value))
  localStorage.setItem(KEY_H, String(height.value))
}

/** 视口变小/变矮时把已存的尺寸收回来，避免卡片溢出。 */
function onWindowResize() {
  width.value = Math.min(width.value, maxWidth())
  height.value = Math.min(height.value, maxHeight())
}

watch(resizing, (dir) => {
  const cursor = dir === 'x' ? 'ew-resize' : dir === 'y' ? 'ns-resize' : dir ? 'nwse-resize' : ''
  document.body.style.cursor = cursor
  document.body.style.userSelect = dir ? 'none' : ''
  if (dir) window.addEventListener('resize', onWindowResize)
  else window.removeEventListener('resize', onWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('pointermove', onResizeMove)
  window.removeEventListener('pointerup', endResize)
  window.removeEventListener('pointercancel', endResize)
  window.removeEventListener('resize', onWindowResize)
  captureEl = null
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
</style>

<style>
/* ---------- 滚动条：不占位、平时隐形、悬停才浮出 ---------- */
/*
  ⚠️ 实测过：默认滚动条在 Windows/Chrome 上是 **15px 常驻灰条**，
  它和右边缘的拖拽热区贴在**同一条边**上，看起来就是「一条多余的竖线」。
  主人要求右边缘干净，所以改成 overlay 式细条：不显形，指针进入才浮出。

  ⚠️⚠️ 两个坑，都是实测出来的（.workbuddy/lab-scrollbar*.mjs）：

  1. **不能在 scoped 块里写 `:deep(::-webkit-scrollbar)`**——Vue 会编译成
     `.detail-body[data-v-x] ::-webkit-scrollbar`，中间那个**空格是后代选择器**，
     意思变成「.detail-body 的子孙元素的滚动条」；而滚动条伪元素必须**直接**挂在
     `.detail-body` 自己身上 → 一条都不生效（实测：拇指始终透明、且仍占 15px）。
     所以另开**全局** style 块，靠 `.detail-body` 类名限定范围。

  2. **`:hover` 不能挂在同一个元素上**：`.detail-body:hover::-webkit-scrollbar-thumb`
     在 Chrome 里**不生效**（实测三组写法全部失败）。必须把 hover 提到**祖先**上：
     `.detail-shell:hover .detail-body::-webkit-scrollbar-thumb`（实测生效，
     截图字节 154B → 208B）。
*/
.detail-body::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
/* 轨道透明 → 平时看不见；拇指也默认透明 → 完全隐形 */
.detail-body::-webkit-scrollbar-track {
  background: transparent;
}
.detail-body::-webkit-scrollbar-thumb {
  background-color: transparent;
  border-radius: 999px;
}
/* ⚠️ hover 挂在外壳（祖先）上，不是 .detail-body 自己——理由见上面第 2 条 */
.detail-shell:hover .detail-body::-webkit-scrollbar-thumb {
  background-color: var(--color-line);
}
.detail-shell .detail-body::-webkit-scrollbar-thumb:hover {
  background-color: var(--color-ink-subtle);
}

/*
  Firefox 走标准属性——**必须包在 `@supports not selector(::-webkit-scrollbar)` 里**。
  ⚠️⚠️ 实测（.workbuddy/lab-scrollbar3.mjs）：一旦无条件写 `scrollbar-width: thin`，
  Chrome 会**直接禁用**所有 `::-webkit-scrollbar-*` 自定义（组 x 悬停 149B→410B 生效，
  组 y 加上 scrollbar-width 后恒为 462B、完全失效）。标准属性与私有伪元素是互斥的，
  所以只能给「不支持 webkit 伪元素」的浏览器用。
*/
@supports not selector(::-webkit-scrollbar) {
  .detail-body {
    scrollbar-width: thin;
    scrollbar-color: transparent transparent;
  }
  .detail-shell:hover .detail-body {
    scrollbar-color: var(--color-line) transparent;
  }
}
</style>
