<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="onCreate">新建试卷</el-button>
    </div>
    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column label="模式" width="90">
        <template #default="{ row }">{{ row.mode === 'FIXED' ? '固定卷' : '规则卷' }}</template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题量" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="onStart(row.id)">开始作答</el-button>
          <el-button link @click="onEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pagination"
      layout="total, prev, pager, next"
      :total="total"
      v-model:current-page="page"
      v-model:page-size="size"
      @change="load"
    />
    <PaperEditDialog v-model:visible="editVisible" :paper-id="editingId" @saved="load" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deletePaper, pagePapers } from '@/api/paper'
import { startQuiz } from '@/api/quiz'
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

async function onStart(paperId: number) {
  const sessionId = await startQuiz({ sourceType: 'PAPER', paperId })
  router.push(`/quiz/${sessionId}`)
}

async function onDelete(row: PaperVO) {
  await ElMessageBox.confirm('确认删除该试卷？', '提示', { type: 'warning' })
  await deletePaper(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
