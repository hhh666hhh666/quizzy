import { expect, test } from '@playwright/test'
import { createQuestion, lastMessage, registerNewUser } from './support/helpers'

/**
 * 收藏夹的两条用户主路（模型见 docs/adr/0030）。
 *
 * ⚠️ **第一条是回归哨兵，别删**：「修改收藏夹」面板曾经**首次打开永远谎报「还没有收藏夹」**——
 * 它的加载挂在 `el-dialog` 的 `@open` 上，而 element-plus 只在 `modelValue` **变化**时才 emit 这个事件
 * （那个 watch 没有 `immediate`，`onMounted` 那条只 `open()` 不 emit）；星标又用 `v-if` 懒挂载面板，
 * 于是首次打开时 `modelValue` 一上来就是 `true`，事件根本不触发。
 *
 * <p>这类错误**只有端到端能挡**：接口层的数据全对，「还没有收藏夹」是**界面自己编的**。
 */

test('题库页收藏一道题后，打开「修改收藏夹」能列出收藏夹并显示当前归属', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stem = `E2E 收藏 ${Date.now()}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/questions')
  const row = page.locator('.el-table__row', { hasText: stem })
  await expect(row).toHaveCount(1)

  // 短按星标 = 收藏（默认收藏夹按需创建，见 ADR 0030）
  await row.locator('.favorite-star').click()
  const banner = page.locator('.el-notification').first()
  await expect(banner).toContainText('已加入')

  // 面板的明路是横幅上那个按钮（长按只是快捷键，桌面端几乎没有可发现性）
  await banner.getByRole('button', { name: '修改收藏夹' }).click()

  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toContainText('现在在')
  // 前面刚收藏过，默认夹必然存在——此时说「还没有收藏夹」就是谎报
  await expect(dialog).not.toContainText('还没有收藏夹')
  await expect(dialog.locator('.folder-row')).toHaveCount(1)
})

test('收藏夹页面：新建夹 → 题库批量加入 → 夹里能看到这道题', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `E2E 归夹 ${stamp}`
  const folder = `E2E 夹 ${stamp}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/favorites')
  await page.getByRole('button', { name: '新建收藏夹' }).click()
  const prompt = page.locator('.el-message-box')
  await prompt.locator('input').fill(folder)
  // ⚠️ 不按文案点「确定」：两个中文字的按钮可能被插空格，直接点主按钮更稳
  await prompt.locator('.el-message-box__btns .el-button--primary').click()
  await expect(page.locator('.folder-item', { hasText: folder })).toHaveCount(1)

  await page.goto('/questions')
  await page.locator('.el-table__row', { hasText: stem }).locator('.el-checkbox').first().click()
  await page.getByTestId('batch-add-to-favorite').click()
  const picker = page.locator('.el-dialog:visible').first()
  await picker.locator('.folder-row', { hasText: folder }).locator('.el-checkbox').click()
  await picker.locator('.el-dialog__footer .el-button--primary').click()
  await expect(lastMessage(page, 'success')).toContainText('加入')

  // 回收藏夹页面：选中那个夹，右列应当只有这一道题
  await page.goto('/favorites')
  await page.locator('.folder-item', { hasText: folder }).click()
  await expect(page.locator('.el-table__row', { hasText: stem })).toHaveCount(1)
})
