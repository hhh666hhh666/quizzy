<template>
  <div class="font-sans text-base text-ink">
    <div class="rounded-xl bg-surface p-6 shadow-sm">
      <Button @click="onCreate">新建试卷</Button>

      <div class="mt-4 overflow-x-auto rounded-lg border border-line-soft" :class="loading ? 'pointer-events-none opacity-60' : ''">
        <table class="w-full border-collapse text-sm">
          <thead>
            <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
              <th class="px-3 py-2.5 font-medium">ID</th>
              <th class="px-3 py-2.5 font-medium">标题</th>
              <th class="px-3 py-2.5 font-medium">模式</th>
              <th class="px-3 py-2.5 font-medium">题量</th>
              <th class="px-3 py-2.5 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id" class="paper-row border-b border-line-soft transition-colors hover:bg-surface-2/50">
              <td class="px-3 py-2.5 text-ink-muted">{{ row.id }}</td>
              <td class="max-w-0 px-3 py-2.5">
                <span class="block truncate" :title="row.title">{{ row.title }}</span>
              </td>
              <td class="whitespace-nowrap px-3 py-2.5">{{ row.mode === 'FIXED' ? '固定卷' : '规则卷' }}</td>
              <td class="px-3 py-2.5">{{ row.questionCount }}</td>
              <td class="whitespace-nowrap px-3 py-2.5">
                <!-- 空卷不能发起作答：置灰 + 悬浮说明（title 即可，这是一句话的事） -->
                <span :title="isEmptyPaper(row) ? '这张试卷还没有题目' : undefined">
                  <button
                    type="button"
                    class="text-brand hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                    :disabled="isEmptyPaper(row)"
                    @click="onStart(row.id)"
                  >
                    开始作答
                  </button>
                </span>
                <button type="button" class="ml-3 text-ink hover:underline" @click="onEdit(row)">编辑</button>
                <button type="button" class="ml-3 text-danger hover:underline" @click="onDelete(row)">删除</button>
              </td>
            </tr>
            <tr v-if="!rows.length">
              <td colspan="5" class="px-4 py-8 text-center text-sm leading-loose text-ink-muted">
                还没有试卷。点「新建试卷」组一张固定卷或规则卷。
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <TablePagination :total="total" :page="page" :size="size" @update:page="(p) => { page = p; load() }" />
    </div>

    <PaperEditDialog v-model:visible="editVisible" :paper-id="editingId" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { confirmBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import { deletePaper, pagePapers } from '@/api/paper'
import { startQuiz } from '@/api/quiz'
import { Button } from '@/components/ui/button'
import TablePagination from '@/components/TablePagination.vue'
import PaperEditDialog from './PaperEditDialog.vue'
import type { PaperVO } from '@/types'

const router = useRouter()
const rows = ref<PaperVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const editVisible = ref(false)
const editingId = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const result = await pagePapers(page.value, size.value)
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function onCreate() {
  editingId.value = null
  editVisible.value = true
}

function onEdit(row: PaperVO) {
  editingId.value = row.id
  editVisible.value = true
}

/**
 * 空卷：固定卷且一道题都没有。只有固定卷会「真的为空」——
 * 规则卷的 questionCount 是规则里写的题量，能不能抽到题要运行时才知道，不能拿它置灰。
 */
function isEmptyPaper(row: PaperVO) {
  return row.mode === 'FIXED' && !row.questionCount
}

async function onStart(paperId: number) {
  const sessionId = await startQuiz({ sourceType: 'PAPER', paperId })
  router.push(`/quiz/${sessionId}`)
}

async function onDelete(row: PaperVO) {
  const ok = await confirmBox({ title: '提示', message: '确认删除该试卷？', confirmText: '删除', danger: true })
  if (!ok) return
  await deletePaper(row.id)
  toast.success('已删除')
  load()
}

onMounted(load)
</script>
