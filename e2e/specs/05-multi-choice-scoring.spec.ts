import { expect, test } from '@playwright/test'
import { createFixedPaper, createQuestion, registerNewUser, startPaperQuiz } from './support/helpers'

// 多选题「必须与正确答案完全一致才得分，少选同样不得分」（ADR 0004）。
//
// 这条规则在**单元层**（ScoreStrategy）与**接口层**（判分响应）都验过，
// 但都没走过 UI：这里补的是「人在页面上少选一个，真的会被判错」——
// 两道题同一张卷，第一道少选、第二道全选，一次跑完把两边都验了。

test('多选题：少选不得分，全选才得分（ADR 0004）', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const first = `E2E 多选甲 ${stamp}`
  const second = `E2E 多选乙 ${stamp}`
  const title = `E2E 多选卷 ${stamp}`

  // 两道结构相同的多选题，正确答案都是 A + B
  await createQuestion(page, { stem: first, type: 'MULTI', options: ['甲', '乙', '丙'], correct: ['A', 'B'] })
  await createQuestion(page, { stem: second, type: 'MULTI', options: ['甲', '乙', '丙'], correct: ['A', 'B'] })
  await createFixedPaper(page, title, [first, second])

  await startPaperQuiz(page, title)

  // 第 1 题：只选 A（少选一个）→ 判错
  await page.locator('.option').nth(0).locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.el-alert')).toContainText('回答错误')

  // 第 2 题：A + B 全选 → 判对
  await page.getByRole('button', { name: '下一题' }).click()
  await page.locator('.option').nth(0).locator('label').first().click()
  await page.locator('.option').nth(1).locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.el-alert')).toContainText('回答正确')

  await page.getByRole('button', { name: '结束并查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
  await expect(page.getByText('答对')).toBeVisible()
})
