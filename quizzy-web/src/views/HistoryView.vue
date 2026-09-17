<template>
  <el-card>
    <template #header>
      <div class="header">
        <span>答题记录</span>
        <el-radio-group v-model="status" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="IN_PROGRESS">进行中</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
          <el-radio-button value="ABANDONED">已放弃</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column label="来源" width="100">
        <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'COMPLETED' ? 'success' : row.status === 'IN_PROGRESS' ? 'warning' : 'info'">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="120">
        <template #default="{ row }">{{ row.obtainedScore }} / {{ row.totalScore }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="170" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === 'IN_PROGRESS'" link type="primary" @click="router.push(`/quiz/${row.id}`)">
            继续作答
          </el-button>
          <el-button v-else link type="primary" @click="router.push(`/quiz/${row.id}/result`)">查看结果</el-button>
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
import { listSessions } from '@/api/quiz'
import type { SessionVO } from '@/types'

const router = useRouter()
const rows = ref<SessionVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const status = ref('')
const loading = ref(false)

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

onMounted(load)
</script>

<style scoped>
.header { display: flex; align-items: center; justify-content: space-between; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
