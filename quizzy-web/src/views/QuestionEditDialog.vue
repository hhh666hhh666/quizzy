<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <!-- ⚠️ 宽度必须连 `sm:max-w-*` 一起写：DialogContent 内置 `sm:max-w-sm`，只写无前缀
         `max-w-3xl` 与之不同断点、twMerge 不合并，≥640px 视口下媒体查询赢 → 实际只有 384px。 -->
    <DialogContent class="flex max-h-[85vh] flex-col gap-0 overflow-hidden p-0 sm:max-w-3xl">
      <DialogHeader class="border-b border-line-soft px-6 py-4">
        <DialogTitle class="text-base font-medium">{{ form.id ? '编辑题目' : '新建题目' }}</DialogTitle>
      </DialogHeader>

      <div class="flex flex-col gap-4 overflow-y-auto px-6 py-5">
        <div class="flex flex-col gap-1.5">
          <span class="text-xs text-ink-muted">题型</span>
          <div class="flex gap-1 self-start rounded-md bg-surface-2 p-1" role="group" aria-label="题型">
            <button
              v-for="opt in TYPE_OPTIONS"
              :key="opt.value"
              type="button"
              class="rounded-sm px-3 py-1 text-sm transition-colors"
              :class="form.type === opt.value ? 'bg-surface font-medium text-ink shadow-sm' : 'text-ink-muted hover:text-ink'"
              @click="setType(opt.value)"
            >
              {{ opt.label }}
            </button>
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label for="q-stem" class="text-xs text-ink-muted">题干</label>
          <Textarea id="q-stem" v-model="form.stem" :rows="4" placeholder="支持 Markdown，代码块请用 ``` 包裹" class="bg-reader" />
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-xs text-ink-muted">选项</span>
          <div v-for="(option, index) in form.options" :key="index" class="option-row flex items-center gap-2.5">
            <span class="label w-5 shrink-0 font-semibold">{{ option.label }}</span>
            <Input
              v-model="option.content"
              :disabled="form.type === 'JUDGE'"
              :placeholder="`选项 ${option.label} 的内容`"
              class="flex-1 bg-reader"
            />
            <label v-if="form.type === 'MULTI'" class="flex shrink-0 cursor-pointer items-center gap-1.5 text-sm">
              <input
                type="checkbox"
                :checked="form.answers.includes(option.label)"
                class="size-4 accent-brand"
                @change="toggleAnswer(option.label, ($event.target as HTMLInputElement).checked)"
              />
              正确项
            </label>
            <label v-else class="flex shrink-0 cursor-pointer items-center gap-1.5 text-sm">
              <input
                type="radio"
                name="q-answer"
                :checked="form.answers[0] === option.label"
                class="size-4 accent-brand"
                @change="form.answers = [option.label]"
              />
              正确项
            </label>
            <button
              type="button"
              class="link-button shrink-0 text-sm text-danger hover:underline disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="form.type === 'JUDGE' || form.options.length <= 2"
              @click="removeOption(index)"
            >
              删除
            </button>
          </div>
          <button
            type="button"
            class="link-button self-start text-sm text-brand hover:underline disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="form.type === 'JUDGE' || form.options.length >= 6"
            @click="addOption"
          >
            + 增加选项
          </button>
        </div>

        <div class="flex flex-col gap-1">
          <label for="q-analysis" class="text-xs text-ink-muted">解析</label>
          <Textarea id="q-analysis" v-model="form.analysis" :rows="4" placeholder="支持 Markdown" class="bg-reader" />
        </div>

        <div class="grid grid-cols-3 gap-3">
          <div class="flex flex-col gap-1">
            <label for="q-difficulty" class="text-xs text-ink-muted">难度</label>
            <Select :model-value="form.difficulty" @update:model-value="(v: unknown) => (form.difficulty = v as any)">
              <SelectTrigger id="q-difficulty" class="bg-reader">
                <span>{{ DIFFICULTY_LABELS[form.difficulty] }}</span>
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="EASY">简单</SelectItem>
                <SelectItem value="MEDIUM">中等</SelectItem>
                <SelectItem value="HARD">困难</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div class="flex flex-col gap-1">
            <label for="q-score" class="text-xs text-ink-muted">分值</label>
            <Input
              id="q-score"
              :model-value="form.score"
              type="number"
              min="1"
              max="100"
              class="bg-reader"
              @update:model-value="(v: unknown) => (form.score = Number(v) || 1)"
            />
          </div>
          <div class="flex flex-col gap-1">
            <label for="q-category" class="text-xs text-ink-muted">分类</label>
            <CreatableSelect
              input-id="q-category"
              :model-value="form.categoryId"
              :options="categoryOptions"
              placeholder="输入可新建分类…"
              @update:model-value="(v: any) => ((form as any).categoryId = v)"
            />
            <!-- 只在选中了既有分类时才出现：新建的名字还没有 id，谈不上改名 -->
            <button
              v-if="typeof form.categoryId === 'number'"
              type="button"
              class="link-button self-start text-xs text-brand hover:underline"
              @click="onRenameCategory"
            >
              改分类名
            </button>
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label for="q-tags" class="text-xs text-ink-muted">标签</label>
          <CreatableSelect
            input-id="q-tags"
            multiple
            :model-value="tagNames"
            :options="tagOptions"
            placeholder="输入后回车新建…"
            @update:model-value="(v: any) => (tagNames = v as string[])"
          />
        </div>
      </div>

      <div class="flex justify-end gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
        <Button variant="outline" @click="emit('update:visible', false)">取消</Button>
        <Button :disabled="saving" @click="onSave">保存</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getQuestion, saveQuestion } from '@/api/question'
import { listCategories, listTags, moveCategory } from '@/api/question'
import CreatableSelect from '@/components/CreatableSelect.vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { promptBox } from '@/lib/box'
import { toast } from '@/lib/toast'
import type { QuestionSaveDTO, QuestionType } from '@/types'

const props = defineProps<{ visible: boolean; questionId: number | null }>()
const emit = defineEmits(['update:visible', 'saved'])

const categories = ref<any[]>([])
const tags = ref<any[]>([])
const tagNames = ref<string[]>([])
const saving = ref(false)

const TYPE_OPTIONS: { value: QuestionType; label: string }[] = [
  { value: 'SINGLE', label: '单选题' },
  { value: 'MULTI', label: '多选题' },
  { value: 'JUDGE', label: '判断题' }
]

const DIFFICULTY_LABELS: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }

const categoryOptions = computed(() => categories.value.map((c: any) => ({ label: c.name, value: c.id as number })))
const tagOptions = computed(() => tags.value.map((t: any) => ({ label: t.name, value: t.name as string })))

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

/**
 * 打开时初始化表单。
 *
 * ⚠️ 这里有两个坑，都是真实踩过的：
 * 1. getter 每次返回**新数组**，Vue 按引用比较 → 组件每次重渲染都会触发本 watcher——
 *    不加守卫的话，打开后用户刚打的字会被「异步请求完成后的重置」反复抹掉。
 *    用 `initializedFor` 记住「这次打开已经初始化过」，同一次打开只走一遍。
 * 2. 重置发生在**两个候选请求之后**（await listCategories/listTags），窗口期内
 *    「打开 → 打字 → 被清」的竞争真实存在（直驱脚本与真实快速打字都能踩到）。
 *    所以**先同步重置表单**，候选列表异步随后到。
 * ⚠️ 守卫记账：关闭时也要把 key 记成 'closed'——否则「关→再开」的 key 与上次相同，
 *    会被误判成「已初始化」而跳过 listCategories，下拉永远显示旧数据（真实踩过）。
 */
let initializedFor: string | null = null
watch(
  () => (props.visible ? `open:${props.questionId ?? 'new'}` : 'closed'),
  async (key) => {
    if (initializedFor === key) return
    initializedFor = key
    if (key === 'closed') return
    // 先同步清空（渲染前就绪），候选与详情异步随后——别让「打开后打的字」被请求完成后的重置抹掉
    form.value = emptyForm()
    tagNames.value = []
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
    }
  },
  { immediate: true }
)

function setType(type: QuestionType) {
  if (form.value.type === type) return
  form.value.type = type
  onTypeChange()
}

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

function toggleAnswer(label: string, checked: boolean) {
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
 *
 * ⚠️ 下面的提示文案必须和后端行为**说同一件事**。曾经想把它改成「该选项会修改该分类下
 * 所有题目的分类」——那句话描述的是「连别人的题一起迁」，与后端不符，等于在界面上写假话；
 * 2026-10-05 的结论是**只改文案、不改行为**，于是写成直白版的「全部题目（我的）」。
 */
async function onRenameCategory() {
  const current = categories.value.find((c: any) => c.id === form.value.categoryId)
  const name = await promptBox({
    title: '改分类名',
    message: '会把你在这个分类下的全部题目迁到新分类；别人的题目不受影响。',
    defaultValue: current?.name ?? '',
    confirmText: '改名',
    validator: (v) => (v.trim().length > 0 ? true : '名称不能为空')
  })
  if (name === null) return
  const moved = await moveCategory(Number(form.value.categoryId), name)
  categories.value = await listCategories()
  form.value.categoryId = moved.id
  toast.success('已改分类名')
}

async function onSave() {
  if (!form.value.stem.trim()) {
    toast.warning('题干不能为空')
    return
  }
  if (form.value.answers.length === 0) {
    toast.warning('请指定正确答案')
    return
  }
  saving.value = true
  try {
    const payload: any = { ...form.value, tags: tagNames.value }
    // 分类下拉支持「输入新名字」——v-model 拿到的可能是**字符串**。把它转成 categoryName
    // 交给后端按名字解析（同名复用、否则新建），id 位置留空。
    // 这样分类与题目在同一个请求里一起落库，不会出现「只建了分类、题目没建成」。
    if (typeof payload.categoryId === 'string') {
      payload.categoryName = payload.categoryId
      payload.categoryId = null
    }
    // 回传新题的 id：从试卷编辑抽屉里内联建题时，父组件要靠它把题加进卷
    const savedId = await saveQuestion(payload)
    toast.success('保存成功')
    emit('saved', savedId)
    emit('update:visible', false)
  } finally {
    saving.value = false
  }
}
</script>
