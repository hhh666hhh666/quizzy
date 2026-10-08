import { expect, test } from '@playwright/test'
import { lastMessage, questionRow, registerNewUser } from './support/helpers'

/**
 * 分类在**界面上**的完整生命周期：随题目诞生 → 无人引用时自动消失。
 *
 * <p>顺带验一件**此前是坏的事**：新建题目对话框里的分类下拉开了 `allow-create`，
 * 但 `v-model` 绑的是 Long 类型的 `categoryId`——输入一个新名字会直接失败。
 * 现在它会把新名字交给后端按名字解析（同名复用、否则新建）。
 *
 * <p>服务层与接口层都已经验过这套生命周期（`CategoryServiceIT` / `CategoryApiIT`），
 * 这里补的是最后一段：**人在界面上真的走得通**。
 */
test('分类随题目诞生：输入新名字即建出，删掉唯一的题后自动消失', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const categoryName = `IT 界面分类 ${stamp}`
  const stem = `IT 带新分类的题 ${stamp}`

  // ---------- 1. 建题时输入一个还不存在的分类名 ----------
  await page.goto('/questions')
  await page.getByRole('button', { name: '新建题目' }).click()

  const dialog = page.getByRole('dialog', { name: '新建题目' })
  await dialog.getByLabel('题干').fill(stem)
  const optionRows = dialog.locator('.option-row')
  await optionRows.nth(0).getByRole('textbox').fill('正确答案在这')
  await optionRows.nth(1).getByRole('textbox').fill('干扰项')
  await optionRows.nth(0).getByRole('radio').click()

  // allow-create：直接输入一个新名字，回车即选中（不需要先有这个分类）
  const categorySelect = dialog.getByLabel('分类')
  await categorySelect.click()
  await categorySelect.fill(categoryName)
  await page.keyboard.press('Enter')

  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(lastMessage(page, 'success')).toContainText('保存成功')

  // **诞生的证据**：再打开对话框，分类下拉里能看到这个新名字
  await expect(questionRow(page, stem)).toHaveCount(1)
  await openCategoryDropdown(page)
  await expect(page.locator('.create-select-panel:visible')).toContainText(categoryName)
  await closeDialog(page)

  // ---------- 2. 删掉唯一引用它的那道题 ----------
  await questionRow(page, stem)
    .getByRole('button', { name: '删除' })
    .click()
  // 自建确认框：按标题找到它，再点「删除」
  await page.getByRole('dialog', { name: '提示' }).getByRole('button', { name: '删除' }).click()
  await expect(questionRow(page, stem)).toHaveCount(0)

  // **自动回收的证据**：没有人引用它了，下拉里不该再有
  await openCategoryDropdown(page)
  await expect(page.locator('.create-select-panel:visible')).not.toContainText(categoryName)
})

/** 打开「新建题目」对话框里的分类下拉。 */
async function openCategoryDropdown(page: import('@playwright/test').Page) {
  await page.getByRole('button', { name: '新建题目' }).click()
  const dialog = page.getByRole('dialog', { name: '新建题目' })
  await dialog.getByLabel('分类').click()
}

/**
 * 关掉当前打开的对话框。
 *
 * 直接点「取消」——面板（分类下拉）会在 mousedown 的外点判断里自行收起，比和键盘事件较劲稳得多
 * （历史上被 EP 的 Esc `preventDefault` 坑过一次）。
 */
async function closeDialog(page: import('@playwright/test').Page) {
  const dialog = page.getByRole('dialog', { name: '新建题目' })
  await dialog.getByRole('button', { name: '取消' }).click()
  await expect(dialog).toBeHidden()
}
