<template>
  <span
    class="favorite-star"
    :class="{ 'is-favorited': favorited, 'is-holding': holding }"
    :title="title"
    :aria-label="title"
    role="button"
    tabindex="0"
    @mousedown="onPressStart"
    @mouseup="onPressEnd"
    @mouseleave="onPressEnd"
    @click="onClick"
    @keydown.enter.prevent="onClick"
    @contextmenu.prevent
  >
    <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
      <path
        d="M12 3.1l2.73 5.53 6.1.89-4.41 4.3 1.04 6.08L12 17.03l-5.46 2.87 1.04-6.08-4.41-4.3 6.1-.89z"
        :fill="favorited ? 'currentColor' : 'none'"
        stroke="currentColor"
        stroke-width="1.6"
        stroke-linejoin="round"
      />
    </svg>

    <!-- ⚠️ 只在第一次用到时才挂载，免得一页十行就挂着十个对话框 -->
    <FavoriteFoldersDialog
      v-if="dialogMounted"
      v-model:visible="dialogVisible"
      :question-id="questionId"
      @saved="emit('change', true)"
    />
  </span>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import FavoriteFoldersDialog from './FavoriteFoldersDialog.vue'
import { toast } from '@/lib/toast'
import { favorite, setQuestionFolders, unfavorite } from '@/api/favorite'
import type { FavoriteFolderVO } from '@/types'

/**
 * 收藏星标。两种手势、三处提示，取舍见 `docs/adr/0030`：
 *
 * - **短按** = 收藏（落到默认收藏夹）/ 取消收藏（从**所有**夹移出）；
 * - **长按**（约 500ms）= 打开「修改收藏夹」面板；
 * - 收藏后弹一条带「修改收藏夹」的横幅、取消后弹一条带「撤销」的横幅。
 *
 * ⚠️ 长按在桌面浏览器里**几乎没有可发现性**（这正是它被当成快捷键而不是唯一入口的原因），
 * 所以横幅上的按钮才是明路。改这个组件时别把横幅去掉、只留长按。
 */
const HOLD_MS = 500

const props = defineProps<{ questionId: number; favorited: boolean }>()
const emit = defineEmits<{ (e: 'change', favorited: boolean): void }>()

const holding = ref(false)
const busy = ref(false)
const dialogMounted = ref(false)
const dialogVisible = ref(false)
let holdTimer: number | null = null
let longPressed = false

const title = computed(() =>
  props.favorited ? '已收藏：短按取消收藏，长按改收藏夹' : '短按收藏，长按改收藏夹'
)

function onPressStart() {
  if (busy.value) {
    return
  }
  longPressed = false
  holding.value = true
  holdTimer = window.setTimeout(() => {
    holdTimer = null
    holding.value = false
    // 标记一下：这次是长按，等一下那个 click 事件要忽略掉
    longPressed = true
    openDialog()
  }, HOLD_MS)
}

function onPressEnd() {
  holding.value = false
  if (holdTimer !== null) {
    window.clearTimeout(holdTimer)
    holdTimer = null
  }
}

function openDialog() {
  dialogMounted.value = true
  dialogVisible.value = true
}

async function onClick() {
  // 长按已经处理过这一下了。不拦的话，「改收藏夹」会顺带把收藏取消掉——
  // 这是把两个动作压在同一个元素上最容易踩的坑。
  if (longPressed) {
    longPressed = false
    return
  }
  if (busy.value) {
    return
  }
  busy.value = true
  try {
    if (props.favorited) {
      const removed = await unfavorite(props.questionId)
      emit('change', false)
      notifyUnfavorited(removed)
    } else {
      const folder = await favorite(props.questionId)
      emit('change', true)
      notifyFavorited(folder)
    }
  } catch {
    // 失败提示已由 api/request 的 unwrap 统一弹出
  } finally {
    busy.value = false
  }
}

/**
 * 收藏后的短横幅。
 *
 * ⚠️ 夹名必须用**接口返回的那个**：默认收藏夹可以被改名，界面上任何地方都不许写死这几个字。
 */
function notifyFavorited(folder: FavoriteFolderVO) {
  toast.success(`已加入「${folder.name}」`, {
    action: { label: '修改收藏夹', onClick: () => openDialog() }
  })
}

/**
 * 取消收藏后的短横幅，带一次**撤销**。
 *
 * 撤销就是「把刚才那批夹原样设回去」——借的是覆盖式设置那个接口，**后端不需要「撤销」接口**。
 */
function notifyUnfavorited(removedFolderIds: number[]) {
  if (removedFolderIds.length === 0) {
    return
  }
  toast.info(`已从 ${removedFolderIds.length} 个收藏夹移出`, {
    action: {
      label: '撤销',
      onClick: async (close) => {
        try {
          await setQuestionFolders(props.questionId, removedFolderIds)
          emit('change', true)
          close()
        } catch {
          // 失败提示已由 unwrap 统一弹出
        }
      }
    }
  })
}
</script>

<style scoped>
.favorite-star {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  outline: none;
  color: var(--color-ink-subtle);
  /* 长按不该把同一行里的题干选中 */
  user-select: none;
  transition: color 0.15s, transform 0.15s;
}

.favorite-star:hover,
.favorite-star.is-favorited {
  /* 收藏星标的颜色（主人 2026-10-08 指定）：金黄 --color-star，不再是 EP warning */
  color: var(--color-star);
}

.favorite-star.is-holding {
  transform: scale(1.2);
}
</style>
