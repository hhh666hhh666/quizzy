<template>
  <div class="font-sans text-base text-ink">
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <h1 class="text-base font-medium">快速练习：按条件随机抽题</h1>

      <div class="mt-5 flex max-w-[560px] flex-col gap-4">
        <div class="flex flex-col gap-1">
          <span class="text-xs text-ink-muted">分类</span>
          <Select :model-value="rule.categoryId ?? 'ALL'" @update:model-value="(v: unknown) => (rule.categoryId = v === 'ALL' ? null : (v as number))">
            <SelectTrigger class="w-[240px] bg-reader">
              <span>{{ rule.categoryId === null ? '不限' : (categories.find((c) => c.id === rule.categoryId)?.name || '不限') }}</span>
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">不限</SelectItem>
              <SelectItem v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-xs text-ink-muted">题型</span>
          <div class="flex gap-4">
            <label v-for="t in ['SINGLE', 'MULTI', 'JUDGE']" :key="t" class="flex cursor-pointer items-center gap-2 text-sm">
              <input v-model="rule.types" type="checkbox" :value="t" class="size-4 accent-brand" />
              {{ { SINGLE: '单选题', MULTI: '多选题', JUDGE: '判断题' }[t as QuestionType] }}
            </label>
          </div>
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-xs text-ink-muted">难度</span>
          <div class="flex gap-4">
            <label v-for="d in ['EASY', 'MEDIUM', 'HARD']" :key="d" class="flex cursor-pointer items-center gap-2 text-sm">
              <input v-model="rule.difficulties" type="checkbox" :value="d" class="size-4 accent-brand" />
              {{ { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[d as Difficulty] }}
            </label>
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label for="quick-count-inner" class="text-xs text-ink-muted">题量</label>
          <!-- ⚠️ data-testid 在外层包裹上：08/02 号 spec 以「testid 内找 input」定位（沿用旧约定） -->
          <div data-testid="quick-count">
            <Input id="quick-count-inner" v-model.number="rule.count" type="number" min="1" max="200" class="w-28 bg-reader" />
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label for="quick-exclude" class="text-xs text-ink-muted">排除近期</label>
          <div class="flex items-center gap-3">
            <Input
              id="quick-exclude"
              :model-value="rule.excludeRecentDays ?? ''"
              type="number"
              min="0"
              max="365"
              class="w-28 bg-reader"
              @update:model-value="(v: unknown) => (rule.excludeRecentDays = Number(v) || 0)"
            />
            <span class="text-xs text-ink-muted">排除最近 N 天已做过的题，0 表示不排除</span>
          </div>
        </div>

        <div>
          <Button :disabled="starting" @click="onStart">开始练习</Button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/lib/toast'
import { listCategories } from '@/api/question'
import { startQuiz } from '@/api/quiz'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import type { PaperRuleDTO, QuestionType, Difficulty } from '@/types'

const router = useRouter()
const categories = ref<any[]>([])
const starting = ref(false)

// 显式标成 PaperRuleDTO：否则 types/difficulties 会被推成 string[]，
// 能把任意字符串塞进去发给后端（TS2322 挡的就是这个）
const rule = reactive<PaperRuleDTO>({
  categoryId: null,
  tagIds: [],
  types: [],
  difficulties: [],
  count: 20,
  excludeRecentDays: 0
})

async function onStart() {
  starting.value = true
  try {
    const sessionId = await startQuiz({ sourceType: 'QUICK', rule })
    router.push(`/quiz/${sessionId}`)
  } catch (e: any) {
    toast.error(e.message || '没有符合要求的题目')
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
})
</script>
