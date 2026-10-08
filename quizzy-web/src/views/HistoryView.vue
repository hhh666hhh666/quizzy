<template>
  <div class="font-sans text-base text-ink">
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-base font-medium">答题记录</h1>
        <!-- 状态分段器：三档互斥、当前项反白（铺底语言，不上玻璃） -->
        <div class="flex gap-1 rounded-md bg-surface-2 p-1" role="group" aria-label="按状态筛选">
          <button
            v-for="opt in STATUS_OPTIONS"
            :key="opt.value"
            type="button"
            class="rounded-sm px-3 py-1 text-sm transition-colors"
            :class="status === opt.value ? 'bg-surface font-medium text-ink shadow-sm' : 'text-ink-muted hover:text-ink'"
            @click="status = opt.value; load()"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="overflow-x-auto rounded-lg border border-line-soft" :class="loading ? 'pointer-events-none opacity-60' : ''">
        <table class="w-full border-collapse text-sm">
          <thead>
            <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
              <th class="px-3 py-2.5 font-medium">ID</th>
              <th class="px-3 py-2.5 font-medium">标题</th>
              <th class="px-3 py-2.5 font-medium">来源</th>
              <th class="px-3 py-2.5 font-medium">状态</th>
              <th class="px-3 py-2.5 font-medium">得分</th>
              <th class="px-3 py-2.5 font-medium">开始时间</th>
              <th class="px-3 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="his-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
              <td class="px-3 py-2.5 text-ink-muted">{{ row.id }}</td>
              <td class="max-w-0 px-3 py-2.5">
                <span class="block truncate" :title="row.title">{{ row.title }}</span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ sourceLabel(row.sourceType) }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <span class="inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium" :class="STATUS_TAG_CLASS[row.status] || 'bg-surface-2 text-ink-muted'">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ row.obtainedScore }} / {{ row.totalScore }}</td>
              <td class="whitespace-nowrap px-3 py-2.5 text-ink-muted">{{ formatTime(row.startTime) }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <button
                  v-if="row.status === 'IN_PROGRESS'"
                  type="button"
                  class="text-brand hover:underline"
                  @click="router.push(`/quiz/${row.id}`)"
                >
                  继续作答
                </button>
                <button v-else type="button" class="text-brand hover:underline" @click="router.push(`/quiz/${row.id}/result`)">
                  查看结果
                </button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="7" class="px-4 py-8 text-center text-sm leading-loose text-ink-muted">
                还没有答题记录——去「快速练习」或「题库」开一场吧。
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <TablePagination :total="total" :page="page" :size="size" @update:page="(p) => { page = p; load() }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listSessions } from '@/api/quiz'
import TablePagination from '@/components/TablePagination.vue'
import type { SessionVO } from '@/types'

const router = useRouter()
const rows = ref<SessionVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const loading = ref(false)

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: '全部' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'ABANDONED', label: '已放弃' }
]

/** 状态标签：已完成=绿、进行中=琥珀、已放弃=中性灰（tint 底 + 深字色，见 tokens.css）。 */
const STATUS_TAG_CLASS: Record<string, string> = {
  COMPLETED: 'bg-tint-green text-ink-green',
  IN_PROGRESS: 'bg-tint-amber text-ink-amber',
  ABANDONED: 'bg-surface-2 text-ink-muted'
}

async function load() {
  loading.value = true
  try {
    const result = await listSessions(page.value, size.value, status.value || undefined)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function sourceLabel(type: string) {
  return { PAPER: '试卷', QUICK: '快速练习', WRONG_BOOK: '错题重练' }[type] || type
}

function statusLabel(status: string) {
  return { IN_PROGRESS: '进行中', COMPLETED: '已完成', ABANDONED: '已放弃' }[status] || status
}

/** 与收藏时间的展示同款约定：后端固定 Asia/Shanghai 输出，前端只做「变得好读」。 */
function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(load)
</script>
