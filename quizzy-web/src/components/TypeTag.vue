<script setup lang="ts">
import { computed } from 'vue'
import type { QuestionType } from '@/types'

/**
 * 题型小标签（彩色）：tint 底 + 深字色（各自实测 ≥ 小字 AA，见 tokens.css 的 ink-* 组）。
 * 色相映射按主人 2026-10-08 的参考图：单选=蓝、多选=绿、判断=紫。
 */
const props = defineProps<{ type: string }>()

const CLASS_MAP: Record<QuestionType, string> = {
  SINGLE: 'bg-tint-blue text-ink-blue',
  MULTI: 'bg-tint-green text-ink-green',
  JUDGE: 'bg-tint-violet text-ink-violet'
}

const cls = computed(() => CLASS_MAP[props.type as QuestionType] ?? 'bg-surface-2 text-ink')
const label = computed(() => ({ SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[props.type] || props.type))
</script>

<template>
  <span class="inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium" :class="cls">{{ label }}</span>
</template>
