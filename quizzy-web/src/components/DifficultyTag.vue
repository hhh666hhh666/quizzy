<script setup lang="ts">
import { computed } from 'vue'
import type { Difficulty } from '@/types'

/**
 * 难度小标签（彩色）：tint 底 + 深字色（各自实测 ≥ 小字 AA，见 tokens.css 的 ink-* 组）。
 * 色相映射按主人 2026-10-08 的参考图：简单=绿、中等=琥珀、困难=红（danger-soft 底）。
 */
const props = defineProps<{ level: string }>()

const CLASS_MAP: Record<Difficulty, string> = {
  EASY: 'bg-tint-green text-ink-green',
  MEDIUM: 'bg-tint-amber text-ink-amber',
  HARD: 'bg-danger-soft text-ink-red'
}

const cls = computed(() => CLASS_MAP[props.level as Difficulty] ?? 'bg-surface-2 text-ink')
const label = computed(() => ({ EASY: '简单', MEDIUM: '中等', HARD: '困难' }[props.level] || props.level))
</script>

<template>
  <span class="inline-flex items-center whitespace-nowrap rounded-sm px-2 py-0.5 text-xs font-medium" :class="cls">{{ label }}</span>
</template>
