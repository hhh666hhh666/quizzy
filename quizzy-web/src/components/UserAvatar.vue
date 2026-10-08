<template>
  <img v-if="src" class="user-avatar" :src="src" :style="boxStyle" alt="" />
  <span v-else class="user-avatar user-avatar--generated" :style="boxStyle" aria-hidden="true">
    <svg viewBox="0 0 5 5" width="100%" height="100%" shape-rendering="crispEdges">
      <rect
        v-for="(cell, index) in art.cells"
        :key="index"
        :x="cell.x"
        :y="cell.y"
        width="1"
        height="1"
        :fill="foreground"
      />
    </svg>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { UserVO } from '@/types'
import { generatedAvatar } from '@/utils/avatar'

/**
 * 用户头像：有自定义头像就显示它，否则**当场算一张**（GitHub 风格的 5×5 方块）。
 *
 * 默认头像不入库，理由与取舍见 `docs/adr/0028`——它由用户 id 派生，每次算出来都一样。
 *
 * ⚠️ 这里的两组 hsl 是**唯一的硬编码颜色**，与前端主题纪律不冲突：
 * 它表达的是「这个用户的身份色」，而不是某个主题语义色。
 */
const props = withDefaults(defineProps<{ user?: UserVO | null; size?: number }>(), {
  user: null,
  size: 28
})

const src = computed(() => props.user?.avatar || '')

// 种子用 id：它不会变，所以同一个人每次生成的图案都一样。
const art = computed(() => generatedAvatar(String(props.user?.id ?? 'anonymous')))

const foreground = computed(() => `hsl(${art.value.hue} 55% 45%)`)

const boxStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`,
  // 圆框（2026-10-08 主人拍板）：与生成图的方块图案配合，由 overflow:hidden 裁成圆形。
  borderRadius: '50%',
  background: `hsl(${art.value.hue} 45% 90%)`
}))
</script>

<style scoped>
.user-avatar {
  display: inline-block;
  flex: none;
  object-fit: cover;
  overflow: hidden;
  vertical-align: middle;
}

.user-avatar--generated {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
</style>
