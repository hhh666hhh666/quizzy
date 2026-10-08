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
  await dialog.locator('.el-form-item', { hasText: '题干' }).locator('textarea').fill(stem)
  const optionRows = dialog.locator('.option-row')
  await optionRows.nth(0).locator('.el-input__inner').fill('正确答案在这')
  await optionRows.nth(1).locator('.el-input__inner').fill('干扰项')
  await optionRows.nth(0).locator('.el-radio').click()

  // allow-create：直接输入一个新名字，回车即选中（不需要先有这个分类）
  const categorySelect = dialog.locator('.el-form-item', { hasText: '分类' }).locator('.el-select')
  await categorySelect.click()
  await categorySelect.locator('input').fill(categoryName)
  await page.keyboard.press('Enter')

  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(lastMessage(page, 'success')).toContainText('保存成功')

  // **诞生的证据**：再打开对话框，分类下拉里能看到这个新名字
  await expect(questionRow(page, stem)).toHaveCount(1)
  await openCategoryDropdown(page)
  await expect(page.locator('.el-select-dropdown:visible')).toContainText(categoryName)
  await closeDialog(page)

  // ---------- 2. 删掉唯一引用它的那道题 ----------
  await questionRow(page, stem)
    .getByRole('button', { name: '删除' })
    .click()
  // ⚠️ 不能按文案「确定」找按钮：Element Plus 会在两个中文字之间**自动插入空格**（渲染成「确 定」），
  //   所以按名字匹配不到。直接点确认框的主按钮——不受这个排版行为影响。
  await page.locator('.el-message-box__btns .el-button--primary').click()
  await expect(questionRow(page, stem)).toHaveCount(0)

  // **自动回收的证据**：没有人引用它了，下拉里不该再有
  await openCategoryDropdown(page)
  await expect(page.locator('.el-select-dropdown:visible')).not.toContainText(categoryName)
})

/** 打开「新建题目」对话框里的分类下拉。 */
async function openCategoryDropdown(page: import('@playwright/test').Page) {
  await page.getByRole('button', { name: '新建题目' }).click()
  const dialog = page.getByRole('dialog', { name: '新建题目' })
  await dialog.locator('.el-form-item', { hasText: '分类' }).locator('.el-select').click()
}

/**
 * 关掉当前打开的对话框。
 *
 * ⚠️ 不能只靠 Esc 连按两下：第一下关掉了分类下拉，但焦点还留在 EP 的 `el-select__input` 上，
 * 而 EP 对 Esc 做了 `preventDefault`——reka 弹窗的关闭逻辑见到「已被处理过的 Esc」就不关
 * （2026-10-08 在 CI 上实测到的）。所以先 Esc 收下拉，再点「取消」——确定性最高。
 */
async function closeDialog(page: import('@playwright/test').Page) {
  await page.keyboard.press('Escape')
  const dialog = page.getByRole('dialog', { name: '新建题目' })
  await dialog.getByRole('button', { name: '取消' }).click()
  await expect(dialog).toBeHidden()
}
