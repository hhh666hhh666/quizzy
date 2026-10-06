import { expect, test } from '@playwright/test'
import { createQuestion, lastMessage, registerNewUser } from './support/helpers'

/**
 * 题库页的批量操作：多选 → 「加入已有试卷」/「用所选新建试卷」。
 *
 * <p>为什么这条只能端到端验：它跨了「题库 → 试卷」两个模块，而**用户看得见的那条路**正是这条。
 * 接口层能验 `POST /api/papers/{id}/questions` 的去重与越权，验不到
 * 「勾了两道题、点一下，卷里真的多了这两道」。
 *
 * <p>⚠️ 断言消息一律走 `lastMessage()`：`el-message` 是**堆叠**的，前一步的「保存成功」可能还没消失，
 * 而多元素下 `toContainText` 是**严格模式报错**（不是「任一匹配」）——第一版就是因此红了 4 条。
 */

test('题库页多选 → 用所选题目新建试卷', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stemA = `E2E 批量甲 ${stamp}`
  const stemB = `E2E 批量乙 ${stamp}`
  await createQuestion(page, { stem: stemA, options: ['甲', '乙'], correct: ['A'] })
  await createQuestion(page, { stem: stemB, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/questions')
  // 默认范围是「我的题库」，刚建的两道就在第一页
  for (const stem of [stemA, stemB]) {
    const row = page.locator('.el-table__row', { hasText: stem })
    await expect(row, `我的题库里应当有「${stem}」`).toHaveCount(1)
    await row.locator('.el-checkbox').first().click()
  }
  await expect(page.locator('.batch-bar')).toContainText('已选 2 道')

  const title = `E2E 批量卷 ${stamp}`
  await page.getByTestId('batch-new-paper').click()
  const box = page.locator('.el-message-box')
  await box.locator('input').fill(title)
  // ⚠️ 不按文案点「确定」：两个中文字的按钮可能被插空格。直接点主按钮。
  await box.locator('.el-message-box__btns .el-button--primary').click()

  await expect(lastMessage(page, 'success')).toContainText('已新建试卷')
  // 批量条该收起来：选中的题已经用完，留着会让人以为还得再来一次
  await expect(page.locator('.batch-bar')).toHaveCount(0)

  await page.goto('/papers')
  const paperRow = page.locator('.el-table__row', { hasText: title })
  await expect(paperRow).toHaveCount(1)
  await paperRow.getByRole('button', { name: '编辑' }).click()
  await expect(page.locator('.el-dialog:visible').first()).toContainText('共 2 道')
})

test('题库页多选 → 加入已有试卷；重复加入的题被忽略', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 批量加入 ${stamp}`
  const title = `E2E 目标卷 ${stamp}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  // 先建一张空卷当目标——顺带又走了一遍「空卷可保存」
  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()
  const dialog = page.locator('.el-dialog:visible').first()
  await dialog.locator('.el-form-item', { hasText: '标题' }).locator('.el-input__inner').fill(title)
  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(lastMessage(page, 'success')).toContainText('保存成功')

  await page.goto('/questions')
  await page.locator('.el-table__row', { hasText: stem }).locator('.el-checkbox').first().click()
  await page.getByTestId('batch-add-to-paper').click()
  await page.getByTestId('batch-paper-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: title }).click()
  await page.getByTestId('batch-add-confirm').click()
  await expect(lastMessage(page, 'success')).toContainText('已加入 1 道')

  // 再把**同一道题**加一次：应当被忽略，而不是把卷变成两条重复关系
  await page.locator('.el-table__row', { hasText: stem }).locator('.el-checkbox').first().click()
  await page.getByTestId('batch-add-to-paper').click()
  await page.getByTestId('batch-paper-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: title }).click()
  await page.getByTestId('batch-add-confirm').click()
  await expect(lastMessage(page, 'info')).toContainText('都已在卷中')

  await page.goto('/papers')
  await page.locator('.el-table__row', { hasText: title }).getByRole('button', { name: '编辑' }).click()
  // 加两次也还是一道
  await expect(page.locator('.el-dialog:visible').first()).toContainText('共 1 道')
})
