<script setup lang="ts">
/**
 * ICP 备案号脚注（2026-10-14 加）。
 *
 * 为什么抽成组件：备案号是「一句话贴好几屏」的东西——各处硬编码之后换号必然漏改一处。
 * 号码与链接在这里定义一次，登录页 / 侧栏 / 关于弹窗都引用本组件。
 *
 * 为什么字色用 ink-muted 而不是更淡的 ink-subtle：备案号是**有信息量的小字**，
 * tokens.css 的口径写死了 ink-subtle 只用于占位符 / 装饰（它在浅面上只有 3.0–3.6:1，
 * 过不了小字 AA），有信息量的小字一律走 muted。
 *
 * ⚠️ 换号要改三处（跨入口做不到共享源码），改的时候全仓搜 `蜀ICP备` 一次：
 *   ① 本文件  ② `quizzy-web/index.html` 的 noscript 兜底  ③ `quizzy-web/preview.html` 页脚
 */
const BEIAN_NO = '蜀ICP备2026059456号-1'

/** 工信部备案管理系统：备案号必须能点过去核验（合规要求） */
const BEIAN_URL = 'https://beian.miit.gov.cn'
</script>

<template>
  <p class="text-xs leading-normal text-ink-muted">
    <a
      class="link-button hover:text-brand hover:underline"
      :href="BEIAN_URL"
      target="_blank"
      rel="noopener noreferrer"
      data-testid="beian-number"
    >{{ BEIAN_NO }}</a>
  </p>
</template>
