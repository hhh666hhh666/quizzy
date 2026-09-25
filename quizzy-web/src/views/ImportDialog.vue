<template>
  <el-dialog :model-value="visible" title="批量导入题目" width="640px" @update:model-value="(v: boolean) => emit('update:visible', v)">
    <el-tabs v-model="tab">
      <el-tab-pane label="Excel 导入" name="excel">
        <el-alert type="info" :closable="false" title="列顺序：题型 | 题干 | 选项A-F | 答案 | 解析 | 难度 | 分值 | 分类 | 标签" />
        <el-upload
          class="upload"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="onFileChange"
        >
          <div class="upload-tip">把文件拖到这里，或点击选择</div>
        </el-upload>
      </el-tab-pane>
      <el-tab-pane label="JSON 导入" name="json">
        <el-input v-model="jsonText" type="textarea" :rows="12" placeholder='[{"type":"single","stem":"...","options":[{"label":"A","content":"..."}],"answer":["A"]}]' />
      </el-tab-pane>
    </el-tabs>

    <div v-if="result" class="report">
      <p>
        共 {{ result.total }} 条，成功
        <el-text type="success">{{ result.successCount }}</el-text>
        条，失败
        <el-text type="danger">{{ result.failed.length }}</el-text>
        条。失败的行会被跳过，其余正常入库。
      </p>
      <el-table v-if="result.failed.length" :data="result.failed" max-height="240" border>
        <el-table-column prop="row" label="行号" width="70" />
        <el-table-column prop="stem" label="题干" show-overflow-tooltip />
        <el-table-column prop="reason" label="错误原因" min-width="200" />
      </el-table>
    </div>

    <template #footer>
      <el-button @click="emit('update:visible', false)">关闭</el-button>
      <el-button type="primary" :loading="loading" @click="onImport">开始导入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { importExcel, importJson } from '@/api/paper'
import type { ImportResultVO } from '@/types'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'done'])

const tab = ref('excel')
const jsonText = ref('')
const file = ref<File | null>(null)
const loading = ref(false)
const result = ref<ImportResultVO | null>(null)

function onFileChange(uploadFile: any) {
  file.value = uploadFile.raw as File
  result.value = null
}

async function onImport() {
  loading.value = true
  try {
    if (tab.value === 'excel') {
      if (!file.value) {
        ElMessage.warning('请先选择文件')
        return
      }
      result.value = await importExcel(file.value)
    } else {
      if (!jsonText.value.trim()) {
        ElMessage.warning('请填写 JSON 内容')
        return
      }
      result.value = await importJson(JSON.parse(jsonText.value))
    }
    emit('done')
  } catch (e: any) {
    ElMessage.error('导入失败：' + (e.message || '内容格式不正确'))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.upload { margin-top: 12px; }
.upload-tip { padding: 20px; color: #909399; }
.report { margin-top: 14px; }
</style>
