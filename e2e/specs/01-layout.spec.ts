import { expect, test } from '@playwright/test'
import { WIDTHS, expectNoHorizontalOverflow, registerNewUser } from './support/helpers'

// 排版的**几何事实**由断言覆盖（ADR 0020 Amendment 1）：只改样式/文案时不用写新用例，
// 改坏了这里自己会红。三个宽度来自《测试系统说明》「排版怎么测」。
//
// ⚠️ 这里只验「溢出」与「越界」两类。裁切（内容被裁掉）不做通用断言——
//    el-table / 代码块 / Markdown 渲染区本来就可滚动，通用规则必然误报。

const PAGES_WITH_LAYOUT = ['/questions', '/papers', '/quiz/quick', '/wrong-book', '/history']

test.describe('排版 · 几何事实', () => {
  for (const width of WIDTHS) {
    test(`${width}px：登录页不横向溢出`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 })
      await page.goto('/login')
      await expect(page.getByRole('tab', { name: '注册' })).toBeVisible()
      await expectNoHorizontalOverflow(page, '.login-card')
    })

    test(`${width}px：主框架内的页面不横向溢出、主内容区不越界`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 })
      await registerNewUser(page)
      for (const path of PAGES_WITH_LAYOUT) {
        await page.goto(path)
        await expect(page.locator('.el-main')).toBeVisible()
        await expectNoHorizontalOverflow(page, '.el-main')
      }
    })
  }
})
