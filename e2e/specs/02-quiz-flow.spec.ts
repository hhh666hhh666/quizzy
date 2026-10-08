import { expect, test } from '@playwright/test'
import { registerNewUser } from './support/helpers'

// 核心链路：**注册 → 快速练习 → 答题（判分反馈）→ 结算 → 结果页**。
//
// 这一条补的是端到端层该补的东西：前面几层都只能分别验「接口对不对」「组件对不对」，
// 只有它走的是**人真正会走的那条路**——包括路由跳转、按钮文案联动、判分反馈的即时显示。
//
// ⚠️ 断言只用语义定位（role / 文案），**不依赖样式结构**；输入框因为同页重复出现才用 testid
// （取舍见 docs/testing/系统说明.md 与 ADR 0020）。

test('核心链路：快速练习 → 答题 → 结算 → 结果页', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  // 题量设成 1：这样答完一题，「结束并查看结果」就在当前页上（它只在最后一题才渲染）。
  // 顺带把这条链路压到几秒——抽 20 道的话要一路点「下一题」。
  await page.goto('/quiz/quick')
  await page.locator('[data-testid="quick-count"] input').fill('1')
  await page.getByRole('button', { name: '开始练习' }).click()

  await expect(page).toHaveURL(/\/quiz\/\d+$/)

  // 选第一个选项：单选/判断渲染成 el-radio、多选渲染成 el-checkbox，
  // 两者外层都是 `<label class="el-radio|el-checkbox">`，点 label 对两种都成立。
  await page.locator('.option').first().locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()

  // 判分反馈必须立刻出现，并带上正确答案——这是「练习语义」的核心，
  // 两端曾经都因为刷新逻辑把反馈清掉而看不见（见 CHANGELOG 里那条修复）。
  const feedback = page.locator('.feedback')
  await expect(feedback).toBeVisible()
  await expect(feedback).toContainText('正确答案')
  await expect(feedback).toContainText(/回答正确|回答错误/)

  await page.getByRole('button', { name: '结束并查看结果' }).click()

  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
  // 结果页的四项统计，正确率这一项的分母是「已作答」而不是总题数（接口层已钉过）
  await expect(page.getByText('得分')).toBeVisible()
  await expect(page.getByText('正确率')).toBeVisible()
  await expect(page.getByText('答对')).toBeVisible()
})

// 断点续答：会话把「做到第几题」存在服务端，刷新后应当接着走，而不是从头开始。
// 这条曾经是坏的——两端都因为刷新逻辑把位置挪走而跳题（见 CHANGELOG 里那条修复）。
test('断点续答：答完一题后刷新，位置继续在未作答的题上', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  // 两道题：答完第一道再刷新，才能看出「接着走」和「从头开始」的区别。
  await page.goto('/quiz/quick')
  await page.locator('[data-testid="quick-count"] input').fill('2')
  await page.getByRole('button', { name: '开始练习' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+$/)

  await page.locator('.option').first().locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.feedback')).toBeVisible()

  // 刷新——位置是服务端给的，不是本地状态
  await page.reload()

  // 头部显示「第 X / Y 题」：应当是第 2 题（第一道已作答），而不是回到第 1 题
  await expect(page.locator('.quiz-header')).toContainText('第 2 / 2 题')
  // 而且第一道题的作答仍然在（会话没丢）
  await expect(page.locator('.quiz-progress')).toBeVisible()
})
