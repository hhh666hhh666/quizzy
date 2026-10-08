<template>
  <Dialog :open="visible" @update:open="(v: boolean) => emit('update:visible', v)">
    <DialogContent class="flex max-h-[85vh] max-w-2xl flex-col gap-0 overflow-hidden p-0">
      <DialogHeader class="border-b border-line-soft px-6 py-4">
        <DialogTitle class="text-base font-medium">批量导入题目</DialogTitle>
      </DialogHeader>

      <div class="flex flex-col gap-4 overflow-y-auto px-6 py-5">
        <!-- 页签：分段器（真按钮） -->
        <div class="flex gap-1 self-start rounded-md bg-surface-2 p-1" role="group" aria-label="导入方式">
          <button
            v-for="opt in TABS"
            :key="opt.value"
            type="button"
            class="rounded-sm px-3 py-1 text-sm transition-colors"
            :class="tab === opt.value ? 'bg-surface font-medium text-ink shadow-sm' : 'text-ink-muted hover:text-ink'"
            @click="tab = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>

        <template v-if="tab === 'excel'">
          <p class="rounded-md bg-tint-blue/60 px-3 py-2 text-sm">
            列顺序：题型 | 题干 | 选项A-F | 答案 | 解析 | 难度 | 分值 | 分类 | 标签
          </p>
          <button
            type="button"
            class="flex w-full cursor-pointer flex-col items-center justify-center gap-1 rounded-lg border border-dashed border-line bg-reader px-4 py-8 text-sm text-ink-muted transition-colors hover:border-brand hover:text-ink focus-visible:ring-2 focus-visible:ring-focus"
            @click="fileInput?.click()"
            @dragover.prevent
            @drop.prevent="onDrop"
          >
            <input ref="fileInput" class="hidden" type="file" accept=".xlsx,.xls" @change="onPick" />
            <span v-if="file" class="text-ink">已选择：{{ file.name }}</span>
            <span v-else>把文件拖到这里，或点击选择（.xlsx / .xls）</span>
          </button>
        </template>
        <template v-else>
          <Textarea
            v-model="jsonText"
            :rows="12"
            class="bg-reader font-mono text-xs"
            placeholder='[{"type":"single","stem":"...","options":[{"label":"A","content":"..."}],"answer":["A"]}]'
          />
        </template>

        <!--
          顺带建卷。**默认不勾**：不勾就还是老行为（只导入题目），要建卷才显式勾上并起个名字。
          后端把「有没有卷名」当作建不建卷的开关，所以这里的标题只在勾上后才必填。
        -->
        <div class="flex flex-wrap items-center gap-3">
          <label class="flex cursor-pointer items-center gap-2 text-sm">
            <input v-model="createPaper" data-testid="import-create-paper" type="checkbox" class="size-4 accent-brand" />
            同时把本次导入的题装进一张新试卷
          </label>
          <Input
            v-model="paperTitle"
            :disabled="!createPaper"
            placeholder="试卷标题（必填）…"
            data-testid="import-paper-title"
            class="w-[240px] bg-reader"
          />
        </div>

        <div v-if="result" class="report flex flex-col gap-3">
          <p>
            共 {{ result.total }} 条，成功
            <span class="font-medium text-ink-green">{{ result.successCount }}</span>
            条，失败
            <span class="font-medium text-ink-red">{{ result.failed.length }}</span>
            条。失败的行会被跳过，其余正常入库。
          </p>
          <p v-if="result.paperId" class="paper-created">
            已把成功的题装进新试卷「{{ paperTitle }}」。
            <button type="button" class="link-button text-brand hover:underline" data-testid="import-goto-papers" @click="goPapers">
              去试卷页看看
            </button>
          </p>
          <div v-if="result.failed.length" class="max-h-60 overflow-y-auto rounded-lg border border-line-soft">
            <table class="w-full border-collapse text-sm">
              <thead>
                <tr class="border-b border-line bg-surface-2/60 text-left text-xs text-ink-muted">
                  <th class="px-3 py-2 font-medium">行号</th>
                  <th class="px-3 py-2 font-medium">题干</th>
                  <th class="px-3 py-2 font-medium">错误原因</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in result.failed" :key="index" class="border-b border-line-soft last:border-b-0">
                  <td class="px-3 py-2 text-ink-muted">{{ item.row }}</td>
                  <td class="max-w-0 px-3 py-2"><span class="block truncate" :title="item.stem">{{ item.stem }}</span></td>
                  <td class="px-3 py-2">{{ item.reason }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="flex justify-end gap-2 border-t border-line-soft bg-surface-2/50 px-6 py-3">
        <Button variant="outline" @click="emit('update:visible', false)">关闭</Button>
        <Button :disabled="loading" @click="onImport">开始导入</Button>
      </div>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/lib/toast'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { importExcel, importJson } from '@/api/paper'
import type { ImportResultVO } from '@/types'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible', 'done'])

const router = useRouter()

const TABS: { value: string; label: string }[] = [
  { value: 'excel', label: 'Excel 导入' },
  { value: 'json', label: 'JSON 导入' }
]

const tab = ref('excel')
const jsonText = ref('')
const file = ref<File | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const loading = ref(false)
const result = ref<ImportResultVO | null>(null)
const createPaper = ref(false)
const paperTitle = ref('')

function acceptFile(picked: File | undefined) {
  if (!picked) return
  file.value = picked
  result.value = null
}

function onPick(event: Event) {
  const input = event.target as HTMLInputElement
  acceptFile(input.files?.[0])
  // 先取文件再清空，否则连续选同一个文件不会再触发 change
  input.value = ''
}

function onDrop(event: DragEvent) {
  acceptFile(event.dataTransfer?.files?.[0])
}

async function onImport() {
  const title = createPaper.value ? paperTitle.value.trim() : undefined
  if (createPaper.value && !title) {
    toast.warning('请填写试卷标题')
    return
  }
  loading.value = true
  try {
    if (tab.value === 'excel') {
      if (!file.value) {
        toast.warning('请先选择文件')
        return
      }
      result.value = await importExcel(file.value, title)
    } else {
      if (!jsonText.value.trim()) {
        toast.warning('请填写 JSON 内容')
        return
      }
      result.value = await importJson(JSON.parse(jsonText.value), title)
    }
    emit('done')
  } catch (e: any) {
    toast.error('导入失败：' + (e.message || '内容格式不正确'))
  } finally {
    loading.value = false
  }
}

function goPapers() {
  emit('update:visible', false)
  router.push('/papers')
}
</script>
