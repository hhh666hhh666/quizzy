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
            @click="selectStatus(opt.value)"
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
              <td class="px-3 py-2.5 tabular-nums text-ink-muted">{{ row.id }}</td>
              <td class="max-w-0 px-3 py-2.5">
                <span class="block truncate" :title="row.title">{{ row.title }}</span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ sourceLabel(row.sourceType) }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <span class="inline-flex items-center rounded-sm px-2 py-0.5 text-xs font-medium" :class="STATUS_TAG_CLASS[row.status] || 'bg-surface-2 text-ink-muted'">
                  {{ statusLabel(row.status) }}
                </span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5 tabular-nums">{{ row.obtainedScore }} / {{ row.totalScore }}</td>
              <td class="whitespace-nowrap px-3 py-2.5 text-ink-muted">{{ formatTime(row.startTime) }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <button
                  v-if="row.status === 'IN_PROGRESS'"
                  type="button"
                  class="link-button text-brand hover:underline"
                  @click="router.push(`/quiz/${row.id}`)"
                >
                  继续作答
                </button>
                <button v-else type="button" class="link-button text-brand hover:underline" @click="router.push(`/quiz/${row.id}/result`)">
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

      <TablePagination :total="total" :page="page" :size="size" @update:page="(p) => { page = p; writeStateToRoute(); load() }" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listSessions } from '@/api/quiz'
import TablePagination from '@/components/TablePagination.vue'
import type { SessionVO } from '@/types'

const route = useRoute()
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

/** 时间展示：后端固定 Asia/Shanghai 下发（无时区后缀 → 按本地解析），用 Intl 格式化。 */
function formatTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
    .format(date)
    .replace(/\//g, '-')
}

// ---------- 界面状态 ↔ 网址（与题库页同款约定：脏值忽略、默认值不写进网址）----------

const DEFAULT_SIZE = 10
const STATUS_VALUES = ['', 'IN_PROGRESS', 'COMPLETED', 'ABANDONED']

function pick(v: unknown): string | undefined {
  const one = Array.isArray(v) ? v[0] : v
  return typeof one === 'string' && one !== '' ? one : undefined
}

function readStateFromRoute() {
  const q = route.query
  const s = pick(q.status)
  const p = Number.parseInt(pick(q.page) ?? '', 10)
  const z = Number.parseInt(pick(q.size) ?? '', 10)
  status.value = STATUS_VALUES.includes(s ?? '') ? (s ?? '') : ''
  page.value = Number.isFinite(p) && p > 1 ? p : 1
  size.value = Number.isFinite(z) && z > 0 && z <= 100 ? z : DEFAULT_SIZE
}

/** 写回网址用 replace：翻页 / 筛选若走 push 会把后退历史淹掉。 */
function writeStateToRoute() {
  const query: Record<string, string> = {}
  if (status.value) query.status = status.value
  if (page.value > 1) query.page = String(page.value)
  if (size.value !== DEFAULT_SIZE) query.size = String(size.value)
  router.replace({ path: '/history', query })
}

function selectStatus(next: string) {
  status.value = next
  page.value = 1
  writeStateToRoute()
  load()
}

onMounted(() => {
  readStateFromRoute()
  void load()
})
</script>
