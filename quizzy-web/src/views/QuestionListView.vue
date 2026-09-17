<template>
  <div>
    <el-card>
      <el-form :inline="true" :model="query">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="搜索题干" @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="query.type" clearable style="width: 120px">
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
            <el-option label="判断题" value="JUDGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" clearable style="width: 120px">
            <el-option label="简单" value="EASY" />
            <el-option label="中等" value="MEDIUM" />
            <el-option label="困难" value="HARD" />
          </el-select>
        </el-form-item>
        <el-form-item label="范围">
          <el-select v-model="query.scope" style="width: 120px">
            <el-option label="全部" value="all" />
            <el-option label="我的题库" value="mine" />
            <el-option label="公开题库" value="public" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="toolbar">
        <el-button type="primary" @click="onCreate">新建题目</el-button>
        <el-button @click="importVisible = true">批量导入</el-button>
        <el-button @click="onExport('excel')">导出 Excel</el-button>
        <el-button @click="onExport('json')">导出 JSON</el-button>
        <el-button @click="download(templatePath(), 'quizzy-template.xlsx')">下载导入模板</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template #default="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column label="难度" width="90">
          <template #default="{ row }">{{ difficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="70" />
        <el-table-column label="归属" width="100">
          <template #default="{ row }">{{ row.ownerId ? '我的' : '公开' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!row.editable" @click="onEdit(row)">编辑</el-button>
            <el-button link type="danger" :disabled="!row.editable" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        layout="total, sizes, prev, pager, next"
        :total="total"
        v-model:current-page="page"
        v-model:page-size="size"
        @change="load"
      />
    </el-card>

    <QuestionEditDialog v-model:visible="editVisible" :question-id="editingId" @saved="load" />
    <ImportDialog v-model:visible="importVisible" @done="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteQuestion, pageQuestions } from '@/api/question'
import { exportPath, templatePath } from '@/api/paper'
import request from '@/api/request'
import QuestionEditDialog from './QuestionEditDialog.vue'
import ImportDialog from './ImportDialog.vue'
import type { QuestionListItemVO, QuestionQuery } from '@/types'

const rows = ref<QuestionListItemVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const editVisible = ref(false)
const importVisible = ref(false)
const editingId = ref<number | null>(null)

const query = reactive<QuestionQuery>({ keyword: '', type: undefined, difficulty: undefined, scope: 'all' })

async function load() {
  loading.value = true
  try {
    const result = await pageQuestions({ ...query, page: page.value, size: size.value })
    rows.value = result.list
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.keyword = ''
  query.type = undefined
  query.difficulty = undefined
  query.scope = 'all'
  page.value = 1
  load()
}

function onCreate() {
  editingId.value = null
  editVisible.value = true
}

function onEdit(row: QuestionListItemVO) {
  editingId.value = row.id
  editVisible.value = true
}

async function onDelete(row: QuestionListItemVO) {
  await ElMessageBox.confirm('删除后不可恢复，确认删除？', '提示', { type: 'warning' })
  await deleteQuestion(row.id)
  ElMessage.success('已删除')
  load()
}

function onExport(format: string) {
  const stamp = new Date().toISOString().replace(/[-:T]/g, '').slice(0, 14)
  download(exportPath(format), `quizzy-questions-${stamp}.${format}`)
}

async function download(path: string, filename: string) {
  const response = await request.get(path, { responseType: 'blob' })
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  window.URL.revokeObjectURL(url)
}

function typeLabel(type: string) {
  return { SINGLE: '单选', MULTI: '多选', JUDGE: '判断' }[type] || type
}

function difficultyLabel(level: string) {
  return { EASY: '简单', MEDIUM: '中等', HARD: '困难' }[level] || level
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
