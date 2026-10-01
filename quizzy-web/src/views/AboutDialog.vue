<template>
  <el-dialog
    :model-value="visible"
    title="关于"
    width="440px"
    @update:model-value="(v: boolean) => emit('update:visible', v)"
  >
    <div class="about">
      <div class="product">Quizzy</div>
      <p class="tagline">个人自学刷题工具，围绕「一道题反复练到会」组织。</p>

      <div class="version">
        <span class="version-main">{{ version }}</span>
        <!-- 只在领先 tag 时出现：提醒你跑的这份代码里还有未发版的东西 -->
        <span v-if="ahead > 0" class="version-sub">+{{ ahead }} 提交 · {{ commit }}</span>
      </div>

      <el-descriptions :column="1" size="small" border class="meta">
        <el-descriptions-item label="构建时间">{{ buildTimeText }}</el-descriptions-item>
        <el-descriptions-item label="仓库">
          <el-link type="primary" :href="REPO_URL" target="_blank" rel="noopener">{{ REPO_URL }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="许可证">MIT</el-descriptions-item>
      </el-descriptions>
    </div>

    <template #footer>
      <el-button type="primary" @click="emit('update:visible', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineProps<{ visible: boolean }>()
const emit = defineEmits(['update:visible'])

const REPO_URL = 'https://github.com/hhh666hhh666/quizzy'

// 这几个是构建期注入的全局常量（声明见 src/env.d.ts）。不能直接在模板里用：
// <script setup> 的模板只认 setup 作用域内的绑定，未识别的标识符会被解析成 _ctx.*，
// 拿到 undefined。所以先在这里接成普通常量。
const version = __APP_VERSION__
const commit = __APP_COMMIT__
const ahead = Number(__APP_AHEAD__) || 0

// 未注入时（宿主机直跑）buildTime 是 unknown，直接显示会很怪
const buildTimeText = computed(() =>
  __APP_BUILD_TIME__ === 'unknown' ? '未注入（本地开发环境）' : __APP_BUILD_TIME__
)
</script>

<style scoped>
.about { text-align: center; }
.product { font-size: 22px; font-weight: 600; color: #409eff; }
.tagline { margin: 6px 0 0; color: #909399; font-size: 13px; }
.version { margin: 16px 0 18px; }
.version-main { font-size: 20px; font-weight: 600; color: #303133; }
.version-sub { display: block; margin-top: 4px; color: #909399; font-size: 12px; }
.meta { text-align: left; }
</style>
