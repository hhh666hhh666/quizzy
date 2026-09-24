<template>
  <div class="markdown-body" v-html="rendered"></div>
</template>

<script setup lang="ts">
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

// 原来这里引用 md.utils.escapeHtml，而 md 正在初始化中——类型推断成环，
// 于是 md 被推成 any（TS7022 / TS7023）。抽成独立函数就把环断掉了。
// markdown-it 的 escapeHtml 不依赖实例，语义等价：只转义 & < > " 四个字符。
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
.markdown-body { line-height: 1.7; word-break: break-word; }
.markdown-body :deep(code) { background: #f5f7fa; padding: 2px 4px; border-radius: 3px; font-size: 13px; }
.markdown-body :deep(pre) { background: #f5f7fa; padding: 12px; border-radius: 6px; overflow-x: auto; }
.markdown-body :deep(p) { margin: 6px 0; }
</style>
