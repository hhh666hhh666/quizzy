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
.markdown-body :deep(code) { background: var(--el-fill-color-light); padding: 2px 4px; border-radius: 3px; font-size: 13px; }
.markdown-body :deep(pre) { background: var(--el-fill-color-light); padding: 12px; border-radius: 6px; overflow-x: auto; }
.markdown-body :deep(p) { margin: 6px 0; }
</style>

<style>
/* 代码高亮的深色配色。
   为什么不直接 import 一份 github-dark.css：那份与 github.css 的选择器完全同名（都是
   顶层的 .hljs / .hljs-keyword），谁后加载谁生效，于是浅色下也会变成深色配色。
   所以这里只在 html.dark 下覆盖同一批 token 类，色值取 GitHub 官方暗色方案。
   ⚠️ 必须是全局样式（不能 scoped）：否则选择器会带上组件属性选择器，够不到 .dark 前缀。 */
html.dark .markdown-body .hljs { background: #0d1117; color: #c9d1d9; }
html.dark .markdown-body .hljs-doctag,
html.dark .markdown-body .hljs-keyword,
html.dark .markdown-body .hljs-template-tag,
html.dark .markdown-body .hljs-template-variable,
html.dark .markdown-body .hljs-type,
html.dark .markdown-body .hljs-variable.language_ { color: #ff7b72; }
html.dark .markdown-body .hljs-title,
html.dark .markdown-body .hljs-title.class_,
html.dark .markdown-body .hljs-title.function_ { color: #d2a8ff; }
html.dark .markdown-body .hljs-attr,
html.dark .markdown-body .hljs-attribute,
html.dark .markdown-body .hljs-literal,
html.dark .markdown-body .hljs-meta,
html.dark .markdown-body .hljs-number,
html.dark .markdown-body .hljs-operator,
html.dark .markdown-body .hljs-selector-attr,
html.dark .markdown-body .hljs-selector-class,
html.dark .markdown-body .hljs-selector-id,
html.dark .markdown-body .hljs-variable { color: #79c0ff; }
html.dark .markdown-body .hljs-regexp,
html.dark .markdown-body .hljs-string,
html.dark .markdown-body .hljs-meta .hljs-string { color: #a5d6ff; }
html.dark .markdown-body .hljs-built_in,
html.dark .markdown-body .hljs-symbol { color: #ffa657; }
html.dark .markdown-body .hljs-code,
html.dark .markdown-body .hljs-comment,
html.dark .markdown-body .hljs-formula { color: #8b949e; }
html.dark .markdown-body .hljs-name,
html.dark .markdown-body .hljs-quote,
html.dark .markdown-body .hljs-selector-pseudo,
html.dark .markdown-body .hljs-selector-tag { color: #7ee787; }
html.dark .markdown-body .hljs-bullet { color: #f2cc60; }
html.dark .markdown-body .hljs-section { color: #1f6feb; font-weight: bold; }
html.dark .markdown-body .hljs-strong { font-weight: bold; }
html.dark .markdown-body .hljs-emphasis { font-style: italic; }
html.dark .markdown-body .hljs-addition { color: #aff5b4; background-color: #033a16; }
html.dark .markdown-body .hljs-deletion { color: #ffdcd7; background-color: #67060c; }
</style>
