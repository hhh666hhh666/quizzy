import { expect, test } from '@playwright/test'
import { registerNewUser } from './support/helpers'

// 答题记录页：做完一次练习后，记录要出现在列表里、能按状态筛、并能跳回结果页。

/** 做一次只含一道题的快速练习并结算。 */
async function finishOneQuiz(page: import('@playwright/test').Page) {
  await page.goto('/quiz/quick')
  await page.locator('[data-testid="quick-count"] input').fill('1')
  await page.getByRole('button', { name: '开始练习' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+$/)

  await page.locator('.option').first().locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await page.getByRole('button', { name: '结束并查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
}

test('答题记录：结算后出现在列表，能按状态筛，并能跳回结果页', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  await finishOneQuiz(page)

  await page.goto('/history')
  const row = page.locator('.el-table__row').first()
  await expect(row).toContainText('快速练习')
  await expect(row).toContainText('已完成')

  // 按状态筛：⚠️ 不能按 role 找——el-radio-button 拿不到可访问名（与题型下拉同理），
  // 直接点它外层。
  await page.locator('.el-radio-button', { hasText: '已完成' }).click()
  await expect(page.locator('.el-table__row')).not.toHaveCount(0)

  await page.locator('.el-radio-button', { hasText: '已放弃' }).click()
  await expect(page.locator('.el-table__row')).toHaveCount(0)

  // 从记录页能跳回结果页
  await page.locator('.el-radio-button', { hasText: '已完成' }).click()
  await page.locator('.el-table__row').first().getByRole('button', { name: '查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
})
