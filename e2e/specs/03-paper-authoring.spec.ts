import { expect, test } from '@playwright/test'
import { createFixedPaper, createQuestion, registerNewUser, startPaperQuiz } from './support/helpers'

// 建题 → 组固定卷 → 用这张卷发起作答 → 答题 → 结算 → 结果页。
//
// 这条链路补的是「自己造数据」那一段：前面的主链路直接用种子题库开练，
// 而建题 / 选入试卷 / 从试卷发起作答这三步**只有端到端能串起来**——
// 它们各自都跨了模块（题目 → 试卷 → 答题会话），接口层只能分别验。
//
// ⚠️ 断言只用语义定位（role / 文案），不依赖样式结构。

test('建题 → 组固定卷 → 用这张卷作答', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 建题 ${stamp}`
  const title = `E2E 试卷 ${stamp}`

  await createQuestion(page, { stem, options: ['正确答案在这', '这个是干扰项'], correct: ['A'] })
  // 按题干把「刚建的那道」选进卷里——顺带证明了它真的建出来了
  await createFixedPaper(page, title, [stem])

  await startPaperQuiz(page, title)

  // 这张卷只有一道题，所以答完就能结算
  await page.locator('.option').first().locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.el-alert')).toContainText('正确答案')

  await page.getByRole('button', { name: '结束并查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
  await expect(page.getByText('正确率')).toBeVisible()
})
