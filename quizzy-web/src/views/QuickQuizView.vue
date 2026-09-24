<template>
  <el-card>
    <template #header>快速练习：按条件随机抽题</template>
    <el-form :model="rule" label-width="100px" style="max-width: 560px">
      <el-form-item label="分类">
        <el-select v-model="rule.categoryId" clearable>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题型">
        <el-checkbox-group v-model="rule.types">
          <el-checkbox value="SINGLE" label="单选题" />
          <el-checkbox value="MULTI" label="多选题" />
          <el-checkbox value="JUDGE" label="判断题" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="难度">
        <el-checkbox-group v-model="rule.difficulties">
          <el-checkbox value="EASY" label="简单" />
          <el-checkbox value="MEDIUM" label="中等" />
          <el-checkbox value="HARD" label="困难" />
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="题量">
        <el-input-number v-model="rule.count" :min="1" :max="200" />
      </el-form-item>
      <el-form-item label="排除近期">
        <el-input-number v-model="rule.excludeRecentDays" :min="0" :max="365" />
        <span class="hint">排除最近 N 天已做过的题，0 表示不排除</span>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="starting" @click="onStart">开始练习</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listCategories } from '@/api/question'
import { startQuiz } from '@/api/quiz'
import type { PaperRuleDTO } from '@/types'

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
    ElMessage.error(e.message || '没有符合要求的题目')
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  categories.value = await listCategories()
})
</script>

<style scoped>
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
</style>
