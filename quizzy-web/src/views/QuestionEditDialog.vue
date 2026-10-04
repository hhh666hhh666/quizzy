<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑题目' : '新建题目'"
    width="720px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <el-form :model="form" label-width="90px">
      <el-form-item label="题型">
        <el-radio-group v-model="form.type" @change="onTypeChange">
          <el-radio-button value="SINGLE">单选题</el-radio-button>
          <el-radio-button value="MULTI">多选题</el-radio-button>
          <el-radio-button value="JUDGE">判断题</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="题干">
        <el-input v-model="form.stem" type="textarea" :rows="3" placeholder="支持 Markdown，代码块请用 ``` 包裹" />
      </el-form-item>

      <el-form-item label="选项">
        <div class="options">
          <div v-for="(option, index) in form.options" :key="index" class="option-row">
            <span class="label">{{ option.label }}</span>
            <el-input
              v-model="option.content"
              :disabled="form.type === 'JUDGE'"
              :placeholder="`选项 ${option.label} 的内容`"
            />
            <el-checkbox
              v-if="form.type === 'MULTI'"
              :value="option.label"
              :model-value="form.answers.includes(option.label)"
              @change="(checked: boolean | string | number) => toggleAnswer(option.label, checked)"
            >
              正确项
            </el-checkbox>
            <el-radio
              v-else
              :value="option.label"
              :model-value="form.answers[0] || ''"
              @change="() => (form.answers = [option.label])"
            >
              正确项
            </el-radio>
            <el-button
              link
              type="danger"
              :disabled="form.type === 'JUDGE' || form.options.length <= 2"
              @click="removeOption(index)"
            >
              删除
            </el-button>
          </div>
        </div>
        <el-button
          link
          type="primary"
          :disabled="form.type === 'JUDGE' || form.options.length >= 6"
          @click="addOption"
        >
          + 增加选项
        </el-button>
      </el-form-item>

      <el-form-item label="解析">
        <el-input v-model="form.analysis" type="textarea" :rows="3" placeholder="支持 Markdown" />
      </el-form-item>

      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="难度">
            <el-select v-model="form.difficulty">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="分值">
            <el-input-number v-model="form.score" :min="1" :max="100" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
      <el-form-item label="分类">
        <el-select v-model="form.categoryId" clearable filterable allow-create default-first-option>
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <!-- 只在选中了既有分类时才出现：新建的名字还没有 id，谈不上改名 -->
        <el-button
          v-if="typeof form.categoryId === 'number'"
          link
          type="primary"
          class="rename-hint"
          @click="onRenameCategory"
        >
          改分类名
        </el-button>
      </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="标签">
        <el-select v-model="tagNames" multiple filterable allow-create default-first-option placeholder="输入后回车新建">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.name" />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:visible', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getQuestion, saveQuestion } from '@/api/question'
import { listCategories, listTags, moveCategory } from '@/api/question'
import type { QuestionSaveDTO } from '@/types'

const props = defineProps<{ visible: boolean; questionId: number | null }>()
const emit = defineEmits(['update:visible', 'saved'])

const categories = ref<any[]>([])
const tags = ref<any[]>([])
const tagNames = ref<string[]>([])
const saving = ref(false)

const emptyForm = (): QuestionSaveDTO => ({
  id: null,
  type: 'SINGLE',
  stem: '',
  analysis: '',
  difficulty: 'MEDIUM',
  score: 1,
  categoryId: null,
  answers: [],
  options: [
    { label: 'A', content: '' },
    { label: 'B', content: '' }
  ],
  tags: []
})

const form = ref<QuestionSaveDTO>(emptyForm())

watch(
  () => [props.visible, props.questionId],
  async () => {
    if (!props.visible) return
    categories.value = await listCategories()
    tags.value = await listTags()
    if (props.questionId) {
      const detail = await getQuestion(props.questionId)
      form.value = {
        id: detail.id,
        type: detail.type,
        stem: detail.stem,
        analysis: detail.analysis || '',
        difficulty: detail.difficulty,
        score: detail.score,
        categoryId: detail.categoryId ?? null,
        answers: [...detail.answers],
        options: detail.options.map((o) => ({ label: o.label, content: o.content })),
        tags: []
      }
      tagNames.value = detail.tags.map((t) => t.name)
    } else {
      form.value = emptyForm()
      tagNames.value = []
    }
  },
  { immediate: true }
)

function onTypeChange() {
  if (form.value.type === 'JUDGE') {
    form.value.options = [
      { label: 'A', content: '正确' },
      { label: 'B', content: '错误' }
    ]
  }
  form.value.answers = []
}

function addOption() {
  const next = String.fromCharCode(65 + form.value.options.length)
  form.value.options.push({ label: next, content: '' })
}

function removeOption(index: number) {
  form.value.options.splice(index, 1)
  form.value.answers = form.value.answers.filter((a) => form.value.options.some((o) => o.label === a))
}

function toggleAnswer(label: string, checked: any) {
  const set = new Set(form.value.answers)
  if (checked) set.add(label)
  else set.delete(label)
  form.value.answers = [...set].sort()
}

/**
 * 改分类名。
 *
 * ⚠️ 语义不是「原地改名」——分类是共享的，原地改会把**所有人**的题目都换名字。
 * 后端做的是「只把我的题迁到目标分类；旧分类若因此无人引用则自动删」。
 */
async function onRenameCategory() {
  const current = categories.value.find((c: any) => c.id === form.value.categoryId)
  try {
    const { value } = await ElMessageBox.prompt(
      '只把你自己的题目迁到新名字下。别人的题目仍留在原分类，原分类也不会因此消失。',
      '改分类名',
      {
        inputValue: current?.name ?? '',
        inputValidator: (v: string) => (v && v.trim().length > 0 ? true : '名称不能为空')
      }
    )
    const moved = await moveCategory(Number(form.value.categoryId), String(value).trim())
    categories.value = await listCategories()
    form.value.categoryId = moved.id
    ElMessage.success('已改分类名')
  } catch (e: any) {
    // 点取消也会被 reject，别当成错误提示
    if (e !== 'cancel') {
      ElMessage.error(e?.message || '改分类名失败')
    }
  }
}

async function onSave() {
  if (!form.value.stem.trim()) {
    ElMessage.warning('题干不能为空')
    return
  }
  if (form.value.answers.length === 0) {
    ElMessage.warning('请指定正确答案')
    return
  }
  saving.value = true
  try {
    const payload: any = { ...form.value, tags: tagNames.value }
    // 分类下拉开了 allow-create，而 v-model 绑的是 Long 类型的 id——
    // 于是「输入一个新名字」得到的是**字符串**。把它转成 categoryName 交给后端
    // 按名字解析（同名复用、否则新建），id 位置留空。
    // 这样分类与题目在同一个请求里一起落库，不会出现「只建了分类、题目没建成」。
    if (typeof payload.categoryId === 'string') {
      payload.categoryName = payload.categoryId
      payload.categoryId = null
    }
    await saveQuestion(payload)
    ElMessage.success('保存成功')
    emit('saved')
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.options { width: 100%; }
.option-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.label { width: 20px; font-weight: 600; }
.rename-hint { margin-left: 10px; }
</style>
