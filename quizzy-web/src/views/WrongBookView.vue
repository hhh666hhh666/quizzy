<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>错题本（连续答对 3 次会自动移出）</span>
        <div>
          <el-input-number v-model="count" :min="1" :max="100" size="small" />
          <el-button type="primary" size="small" :loading="starting" @click="onPractice">开始错题练习</el-button>
        </div>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="题型" width="80">
        <template #default="{ row }">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="danger" @click="onRemove(row)">移出</el-button>
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
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageWrongBook, practiceWrongBook, removeFromWrongBook } from '@/api/quiz'

const router = useRouter()
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const count = ref(20)
const loading = ref(false)
const starting = ref(false)

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
    ElMessage.error(e.message || '错题本暂时是空的')
  } finally {
    starting.value = false
  }
}

async function onRemove(row: any) {
  await removeFromWrongBook(row.id)
  ElMessage.success('已移出错题本')
  load()
}

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

onMounted(load)
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
