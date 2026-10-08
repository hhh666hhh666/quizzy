<template>
  <router-view />
  <Toaster />
  <MessageBoxHost />
</template>

<script setup lang="ts">
import MessageBoxHost from '@/components/MessageBoxHost.vue'
import Toaster from '@/components/Toaster.vue'
</script>

<style>
/* 全局底座：字体与满高。页面底色 / 文字色由 base.css 的 body 规则接管（
   Element Plus 退场后，`--el-*` 的桥接已删除）。 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: var(--font-sans);
}
</style>
