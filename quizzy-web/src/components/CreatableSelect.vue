<template>
  <div ref="rootRef" class="relative">
    <label v-if="false" :for="inputId" class="hidden" />
    <div
      class="flex min-h-9 w-full flex-wrap items-center gap-1.5 rounded-md border border-line bg-reader px-2 py-1 transition-colors focus-within:ring-2 focus-within:ring-focus"
      @click="openPanel"
    >
      <template v-if="multiple">
        <span
          v-for="tag in (modelValue as string[])"
          :key="tag"
          class="inline-flex items-center gap-1 rounded-sm bg-surface-2 px-1.5 py-0.5 text-xs text-ink"
        >
          {{ tag }}
          <button type="button" class="text-ink-subtle hover:text-ink" :aria-label="`移除 ${tag}`" @click.stop="removeTag(tag)">×</button>
        </span>
      </template>
      <input
        ref="inputRef"
        v-model="query"
        :id="inputId"
        :placeholder="inputPlaceholder"
        class="min-w-[80px] flex-1 border-0 bg-transparent text-sm text-ink outline-none placeholder:text-ink-subtle"
        @focus="open = true"
        @keydown.enter.prevent="onEnter"
        @keydown.backspace="onBackspace"
        @keydown.esc="onEsc"
      />
    </div>

    <div
      v-if="open"
      class="create-select-panel absolute z-20 mt-1 max-h-56 w-full overflow-y-auto rounded-lg border border-line bg-surface py-1 shadow-md"
    >
      <button
        v-for="opt in filtered"
        :key="String(opt.value)"
        type="button"
        class="flex w-full items-center gap-2 px-3 py-1.5 text-left text-sm hover:bg-surface-2"
        :class="isSelected(opt.value) ? 'font-medium text-ink-blue' : 'text-ink'"
        @click="pick(opt.value)"
      >
        <span class="min-w-0 flex-1 truncate">{{ opt.label }}</span>
        <span v-if="isSelected(opt.value)" class="shrink-0 text-xs">✓</span>
      </button>
      <button
        v-if="canCreate"
        type="button"
        class="flex w-full items-center px-3 py-1.5 text-left text-sm text-brand hover:bg-surface-2"
        @click="pick(query.trim())"
      >
        新建「{{ query.trim() }}」
      </button>
      <p v-if="!filtered.length && !canCreate" class="px-3 py-2 text-sm text-ink-muted">没有可选项</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 可创建的下拉选择（替代 Element Plus el-select 的 `filterable + allow-create`）。
 *
 * 两种形态共用一个组件：
 * - 单选（`multiple=false`，如「分类」）：选中值可以是列表里的（number id），也可以是
 *   **当场输入的新名字**（string）——新建题目那条链路靠这个把 categoryName 传给后端；
 * - 多选（`multiple=true`，如「标签」）：选中值是一串字符串，回车即加。
 *
 * ⚠️ 面板类名 `create-select-panel` 是 e2e 的钩子（06 号用它断言「新分类出现在下拉里」）。
 */
const props = withDefaults(defineProps<{
  modelValue: string | number | null | string[]
  options: { label: string; value: string | number }[]
  inputId?: string
  multiple?: boolean
  placeholder?: string
}>(), { inputId: undefined, multiple: false, placeholder: '' })

const emit = defineEmits<{ (e: 'update:modelValue', value: any): void }>()

const rootRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const query = ref('')
const open = ref(false)

/** 单选时把已选值当占位符显示（输入框本身保持空，方便直接打字筛选/新建）。 */
const inputPlaceholder = computed(() => {
  if (props.multiple) {
    const tags = props.modelValue as string[]
    return tags.length ? '' : props.placeholder
  }
  const selected = props.options.find((opt) => opt.value === props.modelValue)
  return selected ? selected.label : props.placeholder
})

const filtered = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return props.options
  return props.options.filter((opt) => opt.label.toLowerCase().includes(q))
})

/** 输入了内容、且没有完全一样的既有项时，才提供「新建」。 */
const canCreate = computed(
  () => !!query.value.trim() && !props.options.some((opt) => opt.label === query.value.trim())
)

function isSelected(value: string | number) {
  return props.multiple
    ? (props.modelValue as string[]).includes(String(value))
    : props.modelValue === value
}

function openPanel() {
  open.value = true
  inputRef.value?.focus()
}

function pick(value: string | number) {
  if (props.multiple) {
    const tags = props.modelValue as string[]
    const next = String(value)
    emit('update:modelValue', tags.includes(next) ? tags.filter((t) => t !== next) : [...tags, next])
    query.value = ''
  } else {
    emit('update:modelValue', value)
    query.value = ''
    open.value = false
  }
}

function removeTag(tag: string) {
  emit('update:modelValue', (props.modelValue as string[]).filter((t) => t !== tag))
}

/** 回车：优先命中唯一/第一个筛选项；没有匹配且可新建，就新建。 */
function onEnter() {
  const q = query.value.trim()
  if (!q) return
  const exact = props.options.find((opt) => opt.label === q)
  if (exact) {
    pick(exact.value)
    return
  }
  if (filtered.value.length) {
    pick(filtered.value[0].value)
    return
  }
  if (canCreate.value) {
    pick(q)
  }
}

function onBackspace() {
  if (props.multiple && !query.value) {
    const tags = props.modelValue as string[]
    if (tags.length) removeTag(tags[tags.length - 1])
  }
}

/** 面板开着时 Esc 只收面板（拦下不冒泡，免得把外层弹窗一起关掉）；没开就正常冒泡。 */
function onEsc(event: KeyboardEvent) {
  if (open.value) {
    event.stopPropagation()
    open.value = false
  }
}

function onDocumentClick(event: MouseEvent) {
  if (rootRef.value && !rootRef.value.contains(event.target as Node)) {
    open.value = false
  }
}

onMounted(() => document.addEventListener('mousedown', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('mousedown', onDocumentClick))
</script>
