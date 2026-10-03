<template>
  <view class="markdown-body" v-html="rendered"></view>
</template>

<script setup lang="ts">
// ⚠️ 本组件是 Markdown 渲染的**唯一边界**。
// 现在用 markdown-it + highlight.js + v-html —— 这三样都依赖 DOM，**只在 H5 可用**。
// 将来上微信小程序时，只替换本文件内部实现（如 mp-html / rich-text），调用方一行不改。
import { computed } from 'vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import 'highlight.js/styles/github.css'

const props = defineProps<{ source: string }>()

// 只注册刷题场景常见语言，避免把 highlight.js 全量打进包里
;[java, javascript, typescript, python, sql, bash, json, xml].forEach((lang) => {
  hljs.registerLanguage(lang.name || 'unknown', lang)
})

const escapeHtml = (s: string): string =>
  s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight(code: string, lang: string): string {
    if (lang && hljs.getLanguage(lang)) {
      return `<pre class="hljs"><code>${hljs.highlight(code, { language: lang }).value}</code></pre>`
    }
    return `<pre class="hljs"><code>${escapeHtml(code)}</code></pre>`
  }
})

const rendered = computed(() => md.render(props.source || ''))
</script>

<style scoped>
.markdown-body {
  line-height: 1.7;
  word-break: break-word;
}
</style>
