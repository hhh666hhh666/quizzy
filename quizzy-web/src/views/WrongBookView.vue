<template>
  <div class="font-sans text-base text-ink">
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="text-base font-medium">错题本</h1>
          <p class="mt-0.5 text-xs text-ink-muted">连续答对 3 次会自动移出</p>
        </div>
        <div class="flex items-center gap-2">
          <Input
            v-model.number="count"
            type="number"
            min="1"
            max="100"
            aria-label="练习题数"
            class="h-8 w-20 bg-reader"
          />
          <Button size="sm" :disabled="starting" @click="onPractice">开始错题练习</Button>
        </div>
      </div>

      <div class="overflow-x-auto rounded-lg border border-line-soft" :class="loading ? 'pointer-events-none opacity-60' : ''">
        <table class="w-full border-collapse text-sm">
          <thead>
            <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
              <th class="px-3 py-2.5 font-medium">ID</th>
              <th class="px-3 py-2.5 font-medium">题型</th>
              <th class="px-3 py-2.5 font-medium">题干</th>
              <th class="px-3 py-2.5 font-medium">分类</th>
              <th class="px-3 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="wrong-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
              <td class="px-3 py-2.5 text-ink-muted">{{ row.id }}</td>
              <td class="px-3 py-2.5"><TypeTag :type="row.type" /></td>
              <td class="max-w-0 px-3 py-2.5">
                <span class="block truncate" :title="row.stem">{{ row.stem }}</span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ row.categoryName || '-' }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <button type="button" class="text-danger hover:underline" @click="onRemove(row)">移出</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="5" class="px-4 py-8 text-center text-sm leading-loose text-ink-muted">
                错题本还是空的——答错的题会自动进到这里来。
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
import { toast } from '@/lib/toast'
import { pageWrongBook, practiceWrongBook, removeFromWrongBook } from '@/api/quiz'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import TablePagination from '@/components/TablePagination.vue'
import TypeTag from '@/components/TypeTag.vue'

const route = useRoute()
const router = useRouter()
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const count = ref(20)
const loading = ref(false)
const starting = ref(false)

// ---------- 界面状态 ↔ 网址（与题库页同款约定）----------
const DEFAULT_SIZE = 10

function pick(v: unknown): string | undefined {
  const one = Array.isArray(v) ? v[0] : v
  return typeof one === 'string' && one !== '' ? one : undefined
}

function readStateFromRoute() {
  const q = route.query
  const p = Number.parseInt(pick(q.page) ?? '', 10)
  const z = Number.parseInt(pick(q.size) ?? '', 10)
  page.value = Number.isFinite(p) && p > 1 ? p : 1
  size.value = Number.isFinite(z) && z > 0 && z <= 100 ? z : DEFAULT_SIZE
}

function writeStateToRoute() {
  const query: Record<string, string> = {}
  if (page.value > 1) query.page = String(page.value)
  if (size.value !== DEFAULT_SIZE) query.size = String(size.value)
  router.replace({ path: '/wrong-book', query })
}

async function load() {
  loading.value = true
  try {
    const result = await pageWrongBook(page.value, size.value)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function onPractice() {
  starting.value = true
  try {
    const sessionId = await practiceWrongBook(count.value)
    router.push(`/quiz/${sessionId}`)
  } catch (e: any) {
    toast.error(e.message || '错题本暂时是空的')
  } finally {
    starting.value = false
  }
}

async function onRemove(row: any) {
  await removeFromWrongBook(row.id)
  toast.success('已移出错题本')
  load()
}

onMounted(() => {
  readStateFromRoute()
  void load()
})
</script>
