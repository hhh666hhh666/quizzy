<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

// 启动时不做跳转：登录态由各页 onShow 的 ensureLogin() 兜底，
// 因为 onLaunch 里做 reLaunch 有时序问题。
onLaunch(() => {})
</script>

<style lang="scss">
page {
  background-color: $app-canvas;
  color: $app-ink;
  font-size: $text-md;
  line-height: 1.75;
}

/* ── 通用件（2026-10-09 随色板统一抽出来，页面别再各写一份）── */

.qz-page {
  padding: 24rpx 24rpx 48rpx;
}

.qz-card {
  background: $app-surface;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
  padding: 28rpx;
  margin-bottom: 24rpx;
}

/* 阅读面：题干 / 解析这类要读长文字的块，用近白 */
.qz-reader {
  background: $app-reader;
  border-radius: $radius-lg;
  box-shadow: $shadow-sm;
}

.qz-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  border-radius: $radius-md;
  font-size: $text-lg;
  background: $app-surface;
  color: $app-ink;
  border: 1rpx solid $app-line;
  box-sizing: border-box;
}

/* 主按钮：全站唯一强调色，一屏最多一个 */
.qz-btn--primary {
  background: $app-brand;
  color: #fff;
  border-color: $app-brand;
  font-weight: 500;
}

.qz-btn--ghost {
  background: transparent;
  border-color: $app-line;
  color: $app-ink-muted;
}

.qz-btn--danger {
  background: transparent;
  border-color: $app-danger;
  color: $app-danger;
}

.qz-btn--block {
  width: 100%;
}

.qz-btn--disabled {
  opacity: 0.45;
}

.qz-tag {
  display: inline-flex;
  align-items: center;
  height: 40rpx;
  padding: 0 16rpx;
  border-radius: $radius-xs;
  background: $app-surface-2;
  color: $app-ink-muted;
  font-size: $text-sm;
  line-height: 1;
}

.qz-tag--brand {
  background: $app-brand-soft;
  color: $app-ink-blue;
}

.qz-tag--ok {
  background: $app-ok-soft;
  color: $app-ink-green;
}

.qz-tag--danger {
  background: $app-danger-soft;
  color: $app-danger;
}

.qz-tag--warn {
  background: $app-warn-soft;
  color: $app-warn;
}

.qz-muted {
  color: $app-ink-muted;
}

.qz-empty {
  padding: 120rpx 0;
  text-align: center;
  color: $app-ink-muted;
  font-size: $text-md;
}

.qz-loading {
  padding: 120rpx 0;
  text-align: center;
  color: $app-ink-muted;
}
</style>
