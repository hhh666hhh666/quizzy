<script setup lang="ts">
/**
 * ICP 备案号脚注（2026-10-14 加）。
 *
 * 为什么抽成组件：和 PC 端的 `quizzy-web/src/components/BeianFooter.vue` 同一个理由——
 * 号码要在登录页与首页各出一处，分散硬编码后换号必然漏改一处。
 *
 * ⚠️ 号码**跨端做不到共享源码**（quizzy-web 与 quizzy-mobile 是两个独立的包），
 *    换号请全仓搜 `蜀ICP备` 一次改齐。
 *
 * 为什么用 `@click` 而不是 `<a href>`：uni-app 的小程序目标不认 `<a>`，也跳不进没配到
 * 「业务域名」白名单里的外部地址。这里按编译目标分流——H5 直接开页面，非 H5 退化为复制号码。
 * 当前只启用 H5 一个编译目标（见 `docs/design/移动端.md`），第二条分支是给将来留的。
 */
const BEIAN_NO = '蜀ICP备2026059456号-1'
const BEIAN_URL = 'https://beian.miit.gov.cn'

function onTap() {
  // #ifdef H5
  window.open(BEIAN_URL, '_blank', 'noopener')
  // #endif
  // #ifndef H5
  uni.setClipboardData({ data: BEIAN_NO })
  // #endif
}
</script>

<template>
  <text class="qz-beian" @click="onTap">{{ BEIAN_NO }}</text>
</template>

<style lang="scss" scoped>
.qz-beian {
  display: block;
  padding: 12rpx 0;
  text-align: center;
  font-size: $text-sm;
  // 有信息量的小字必须用 muted：uni.scss 的口径写明 subtle 达不到小字 AA，只给占位符 / 装饰
  color: $app-ink-muted;
}
</style>
