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

// 空卷：固定卷允许一道题都没有（先建卷、后加题）。但空卷**不能发起作答**，
// 所以列表里那个入口必须置灰——这一步只在界面上成立，接口层看不见。
test('空固定卷能保存，但「开始作答」是灰的', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const title = `E2E 空卷 ${Date.now()}`

  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.locator('.el-form-item', { hasText: '标题' }).locator('.el-input__inner').fill(title)
  // 一道题都没选也该能保存——拦在保存这一步就会让「先建卷、后加题」这条动线走不通
  await expect(dialog).toContainText('空卷可以先保存')
  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message--success')).toContainText('保存成功')

  const row = page.locator('.el-table__row', { hasText: title })
  await expect(row).toHaveCount(1)
  await expect(row.getByRole('button', { name: '开始作答' })).toBeDisabled()
})

// 内联建题：省掉「先去题库建、再回来选」那一趟。断言落在「建完自动进卷」上——
// 那正是这一步存在的理由。
test('在试卷抽屉里内联新建题目，保存后自动进卷', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 内联建题 ${stamp}`
  const title = `E2E 内联卷 ${stamp}`

  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()
  const paperDialog = page.locator('.el-dialog:visible').first()
  await paperDialog.locator('.el-form-item', { hasText: '标题' }).locator('.el-input__inner').fill(title)
  await paperDialog.getByRole('button', { name: '新建题目' }).click()

  // 建题表单是**另一层** dialog，嵌套在试卷抽屉里
  const questionDialog = page.locator('.el-dialog:visible').last()
  await questionDialog.locator('.el-form-item', { hasText: '题干' }).locator('textarea').fill(stem)
  const optionRows = questionDialog.locator('.option-row')
  await optionRows.nth(0).locator('.el-input__inner').fill('正确答案在这')
  await optionRows.nth(1).locator('.el-input__inner').fill('这是干扰项')
  await optionRows.nth(0).locator('.el-radio').click()
  await questionDialog.getByRole('button', { name: '保存' }).click()

  await expect(page.locator('.el-message--success')).toContainText('已新建并加入试卷')
  await expect(paperDialog).toContainText('共 1 道')

  await paperDialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message--success')).toContainText('保存成功')
  await expect(page.locator('.el-table__row', { hasText: title })).toHaveCount(1)
})

// 选题器：题一多，只靠「翻页找」根本找不到。关键词/筛选是这一步唯一的可用入口。
test('试卷选题器能按关键词筛题', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 筛选选题 ${stamp}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.getByRole('button', { name: '选择题库题目' }).click()

  const selector = page.locator('.el-dialog:visible').last()
  await selector.getByPlaceholder('按题干搜索').fill(stem)
  await selector.getByRole('button', { name: '查询' }).click()

  // 筛到只剩这一道——否则「按题干搜」只是摆设
  await expect(selector.locator('.el-table__row')).toHaveCount(1)
  const row = selector.locator('.el-table__row', { hasText: stem })
  await row.locator('.el-checkbox').first().click()
  // 选中数必须跟着走：跨页勾选是自己记账的，计数是它唯一的外在证据
  await expect(selector).toContainText('已选 1 道')

  await selector.getByRole('button', { name: '加入试卷' }).click()
  await expect(dialog).toContainText('共 1 道')
})
