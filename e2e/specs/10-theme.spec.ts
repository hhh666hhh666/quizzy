import { expect, test } from '@playwright/test'
import { registerNewUser } from './support/helpers'

// 外观切换是新增的**交互**，按《测试系统说明》「前端页面 / 路由 / 交互 → 补一条端到端」落在这一条。
//
// 断言只认 `html` 上的 `dark` 类——那是 Element Plus 深色变量表的唯一选择器（见《前端》）。
// **刻意不验具体颜色**：色值属于设计细节，改配色不该让用例变红；布局是否被撑坏由 01-layout 的几何断言管。

test.describe('外观 · 浅色 / 深色', () => {
  test('未登录：跟随系统时，系统是深色页面就是深色', async ({ page }) => {
    await page.emulateMedia({ colorScheme: 'dark' })
    await page.goto('/login')
    await expect(page.locator('html')).toHaveClass(/dark/)

    // 系统转浅色，页面应当跟着转回来（auto 档的监听链路）
    await page.emulateMedia({ colorScheme: 'light' })
    await expect(page.locator('html')).not.toHaveClass(/dark/)
  })

  test('登录后：顶栏可切换三档，选择写进 localStorage 且刷新后仍生效', async ({ page }) => {
    await registerNewUser(page)
    const html = page.locator('html')
    await expect(html).not.toHaveClass(/dark/)

    await page.getByRole('button', { name: /外观/ }).click()
    await page.getByRole('menuitem', { name: /深色/ }).click()
    await expect(html).toHaveClass(/dark/)
    expect(await page.evaluate(() => localStorage.getItem('quizzy_theme'))).toBe('dark')

    // 刷新后仍深色：同时覆盖「持久化」与 index.html 里那段首屏防闪烁脚本
    await page.reload()
    await expect(html).toHaveClass(/dark/)

    await page.getByRole('button', { name: /外观/ }).click()
    await page.getByRole('menuitem', { name: /浅色/ }).click()
    await expect(html).not.toHaveClass(/dark/)
  })
})
