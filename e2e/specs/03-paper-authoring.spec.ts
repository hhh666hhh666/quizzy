import { expect, test } from '@playwright/test'
import { registerNewUser } from './support/helpers'

// 建题 → 组卷（固定卷）→ 用这张卷发起作答 → 答题 → 结算 → 结果页。
//
// 这条链路补的是「自己造数据」那一段：前面的主链路直接用种子题库开练，
// 而建题 / 选入试卷 / 从试卷发起作答这三步**只有端到端能串起来**——
// 它们各自都跨了模块（题目 → 试卷 → 答题会话），接口层只能分别验。
//
// ⚠️ 断言只用语义定位（role / 文案），不依赖样式结构；表单里的输入框因为
// Element Plus 的 label 不与 input 关联（没有 for），统一走「先定位表单项、再取其输入框」。

test('建题 → 组固定卷 → 用这张卷作答', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 建题 ${stamp}`
  const title = `E2E 试卷 ${stamp}`

  // ---------- 1. 建题 ----------
  await page.goto('/questions')
  await page.getByRole('button', { name: '新建题目' }).click()

  const questionDialog = page.locator('.el-dialog:visible')
  await expect(questionDialog).toBeVisible()
  await questionDialog.locator('.el-form-item', { hasText: '题干' }).locator('textarea').fill(stem)

  // 默认题型是单选，自带 A / B 两个选项；把 A 填内容并标记为正确项。
  const optionRows = questionDialog.locator('.option-row')
  await optionRows.nth(0).locator('.el-input__inner').fill('正确答案在这')
  await optionRows.nth(1).locator('.el-input__inner').fill('这个是干扰项')
  await optionRows.nth(0).locator('.el-radio').click()

  await questionDialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message--success')).toContainText('保存成功')
  await expect(questionDialog).toBeHidden()

  // ---------- 2. 组卷（固定卷，选入一道题） ----------
  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()

  const paperDialog = page.locator('.el-dialog:visible').first()
  await expect(paperDialog).toBeVisible()
  await paperDialog.locator('.el-form-item', { hasText: '标题' }).locator('.el-input__inner').fill(title)
  // 模式默认就是固定卷，不点它——顺带把「默认值也能用」当成一个隐性断言。

  await paperDialog.getByRole('button', { name: '选择题库题目' }).click()
  const selector = page.locator('.el-dialog:visible').last()
  await expect(selector.locator('.el-table__row').first()).toBeVisible()
  // 只勾一道：这样后面「结束并查看结果」就在当前页上（它只在最后一题才渲染）。
  await selector.locator('.el-table__row').first().locator('.el-checkbox').first().click()
  await selector.getByRole('button', { name: '加入试卷' }).click()

  await paperDialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message--success')).toContainText('保存成功')
  await expect(paperDialog).toBeHidden()

  // ---------- 3. 用这张卷作答 ----------
  const paperRow = page.locator('.el-table__row', { hasText: title })
  await expect(paperRow).toHaveCount(1)
  await paperRow.getByRole('button', { name: '开始作答' }).click()

  await expect(page).toHaveURL(/\/quiz\/\d+$/)
  // 这张卷只有一道题，所以答完就能结算
  await page.locator('.option').first().locator('label').first().click()
  await page.getByRole('button', { name: '提交本题' }).click()
  await expect(page.locator('.el-alert')).toContainText('正确答案')

  await page.getByRole('button', { name: '结束并查看结果' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+\/result$/)
  await expect(page.getByText('正确率')).toBeVisible()
})
