<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

// 启动时不做跳转：登录态由各页 onShow 的 ensureLogin() 兜底，
// 因为 onLaunch 里做 reLaunch 有时序问题。
onLaunch(() => {})
</script>

<style lang="scss">
page {
  background-color: $app-bg;
  color: $app-text;
  font-size: 28rpx;
  line-height: 1.6;
}
</style>
