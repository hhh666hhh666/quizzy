import { expect, test } from '@playwright/test'
import { createFixedPaper, createQuestion, registerNewUser, startPaperQuiz } from './support/helpers'

// 错题本主链路：答错 → 进本 → 手动移出 → 不在本里。
//
// 错题本**不是一张独立的表**，而是从作答统计投影出来的状态（docs/design/数据模型.md）。
// 接口层验的是「投影对不对」，端到端验的是「人能不能真的走到那一步」——
// 包括结算之后数据才落定、以及列表里那条「移出」按钮真的能把它清掉。

test('错题本：答错进本，手动移出后消失', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 错题 ${stamp}`
  const title = `E2E 错题卷 ${stamp}`

  await createQuestion(page, { stem, options: ['这个是对的', '这个是错的'], correct: ['A'] })
  await createFixedPaper(page, title, [stem])

  await startPaperQuiz(page, title)

  // 故意选第二个选项（B），必定判错
  await page.locator('.option').nth(1).locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.el-alert')).toContainText('回答错误')

  await page.getByRole('button', { name: '结束并查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)

  // 进本了
  await page.goto('/wrong-book')
  const wrongRow = page.locator('.wrong-row', { hasText: stem })
  await expect(wrongRow, '答错的题应当出现在错题本里').toHaveCount(1)

  // 手动移出 → 不再出现
  await wrongRow.getByRole('button', { name: '移出' }).click()
  await expect(page.locator('.wrong-row', { hasText: stem }), '移出之后不该还在').toHaveCount(0)
})
