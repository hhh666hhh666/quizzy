<script setup lang="ts">
import { computed } from 'vue'
import { Button } from '@/components/ui/button'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'

/**
 * 迁移期表格共用的分页条：共 N 条（+ 每页条数）+ 上一页 / 第 X / Y 页 / 下一页。
 *
 * 刻意做薄：只发 `update:page` / `update:size`，翻页后取不取数、要不要把越界页码
 * 退回第一页，都是调用方自己的事（题库页的 load 就自带越界回退）。
 * 深色模式、玻璃一概不沾——它是铺底语言的一部分。
 */
const props = withDefaults(
  defineProps<{ total: number; page: number; size: number; sizes?: boolean }>(),
  { sizes: false }
)
const emit = defineEmits<{ (e: 'update:page', v: number): void; (e: 'update:size', v: number): void }>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

function go(p: number) {
  if (p < 1 || p > pageCount.value || p === props.page) return
  emit('update:page', p)
}
</script>

<template>
  <div class="mt-4 flex items-center justify-end gap-3">
    <span class="text-sm text-ink-muted">共 {{ total }} 条</span>
    <Select v-if="sizes" :model-value="String(size)" @update:model-value="(v: unknown) => emit('update:size', Number(v))">
      <SelectTrigger class="h-8 w-[100px]">
        <span>{{ size }}条/页</span>
      </SelectTrigger>
      <SelectContent>
        <SelectItem v-for="s in [10, 20, 50]" :key="s" :value="String(s)">{{ s }}条/页</SelectItem>
      </SelectContent>
    </Select>
    <div class="flex items-center gap-1">
      <Button variant="outline" size="icon-sm" aria-label="上一页" :disabled="page <= 1" @click="go(page - 1)">‹</Button>
      <span class="px-2 text-sm text-ink-muted">第 {{ page }} / {{ pageCount }} 页</span>
      <Button variant="outline" size="icon-sm" aria-label="下一页" :disabled="page >= pageCount" @click="go(page + 1)">›</Button>
    </div>
  </div>
</template>
