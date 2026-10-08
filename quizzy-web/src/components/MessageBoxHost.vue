<template>
  <Dialog :open="boxRequest !== null" @update:open="(v: boolean) => { if (!v) settleCancel() }">
    <DialogContent v-if="boxRequest" class="max-w-sm">
      <DialogHeader>
        <DialogTitle class="text-base font-medium">{{ boxRequest.opts.title }}</DialogTitle>
      </DialogHeader>

      <p class="text-sm leading-relaxed">{{ boxRequest.opts.message }}</p>

      <div v-if="boxRequest.kind === 'prompt'" class="flex flex-col gap-1">
        <Input
          ref="inputRef"
          v-model="text"
          :id="'box-input'"
          :type="boxRequest.opts.inputType || 'text'"
          :placeholder="boxRequest.opts.placeholder"
          class="bg-reader"
          @keyup.enter="onConfirm"
        />
        <p v-if="error" class="text-xs text-ink-red">{{ error }}</p>
      </div>

      <DialogFooter>
        <Button variant="outline" @click="settleCancel">{{ boxRequest.opts.cancelText || '取消' }}</Button>
        <Button :class="boxRequest.opts.danger ? 'bg-danger text-white hover:opacity-90' : ''" @click="onConfirm">
          {{ boxRequest.opts.confirmText || '确定' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { boxRequest } from '@/lib/box'

const text = ref('')
const error = ref('')
const inputRef = ref<{ $el?: HTMLInputElement } | null>(null)

// 每次请求进来重置输入、聚焦——prompt 打开即可打字
watch(boxRequest, async (req) => {
  if (!req) return
  text.value = req.kind === 'prompt' ? (req.opts.defaultValue ?? '') : ''
  error.value = ''
  await nextTick()
  inputRef.value?.$el?.focus?.()
})

function onConfirm() {
  const req = boxRequest.value
  if (!req) return
  if (req.kind === 'prompt') {
    const value = text.value
    const valid = req.opts.validator?.(value)
    if (valid !== undefined && valid !== true) {
      error.value = valid
      return
    }
    const resolve = req.resolve
    boxRequest.value = null
    resolve(value.trim())
  } else {
    const resolve = req.resolve
    boxRequest.value = null
    resolve(true)
  }
}

function settleCancel() {
  const req = boxRequest.value
  if (!req) return
  boxRequest.value = null
  // 按 kind 分支收窄后再 resolve：把 resolve 提取到判断之前会被 TS 收成 never
  if (req.kind === 'prompt') {
    req.resolve(null)
  } else {
    req.resolve(false)
  }
}
</script>
