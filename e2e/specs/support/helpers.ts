import { expect, type Page } from '@playwright/test'

/**
 * 排版要盯的宽度：**桌面 / 平板（1280 / 768）**。
 *
 * <p>⚠️ **没有 375，是刻意的**，两个理由：
 *
 * <ol>
 *   <li>按 [ADR 0019](../../docs/adr/0019-mobile-clients-with-uniapp.md) 的多端架构，
 *       **移动端另有 quizzy-mobile 工程**（uni-app 出 H5 + 小程序），quizzy-web 只服务桌面与平板；
 *   <li>实测 quizzy-web 的 PC 布局**最小可用宽度约 501px**——侧边栏固定 200px，查询区与表格不折行，
 *       375px 下必然横向溢出。这不是 bug，是范围外。
 * </ol>
 *
 * <p>所以几何断言在 quizzy-web 上只验桌面与平板；375 归 quizzy-mobile 落地后再补。
 */
export const WIDTHS = [1280, 768] as const

/**
 * 注册一个全新账号，结束时停在已登录状态（`/questions`）。
 *
 * 账号名带时间戳：这个库是被多个用例共享的，不能假设干净（导入不幂等那条教训同理）。
 */
export async function registerNewUser(page: Page): Promise<string> {
  const username = `e2e_${Date.now()}${Math.floor(Math.random() * 1000)}`
  await page.goto('/')
  await expect(page).toHaveURL(/\/login$/)
  await page.getByRole('tab', { name: '注册' }).click()
  await page.getByTestId('register-username').fill(username)
  await page.getByTestId('register-password').fill('e2e-Passw0rd!')
  await page.getByTestId('register-submit').click()
  await expect(page).toHaveURL(/\/questions$/)
  return username
}

/**
 * 断言页面没有横向溢出，且（若存在）主内容区没有越出视口右边界。
 *
 * <p>只做这两类，是**刻意的**：
 *
 * <ul>
 *   <li><b>溢出</b>——整页出现横向滚动条。在任何宽度上都是 bug，几乎不会误报。
 *   <li><b>越界</b>——关键容器的右边界超出视口宽度。同样几乎不会误报。
 *   <li><b>裁切不做通用断言</b>：`el-table`、代码块、Markdown 渲染区**本来就可滚动**，
 *       拿「scrollWidth > clientWidth」去套必然误报。要验裁切请用
 *       {@link expectNotClipped} 显式点名元素——「哪些容器允许滚动」只有人知道，机器猜不出来。
 * </ul>
 */
export async function expectNoHorizontalOverflow(page: Page, containerSelector = '.el-main'): Promise<void> {
  const doc = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth
  }))
  const viewportWidth = page.viewportSize()?.width ?? doc.clientWidth

  expect(doc.scrollWidth, `页面在 ${viewportWidth}px 宽下不该横向溢出`)
    .toBeLessThanOrEqual(doc.clientWidth + 1)

  const container = page.locator(containerSelector)
  if ((await container.count()) === 0) {
    return
  }
  const box = await container.first().boundingBox()
  expect(box, `容器 ${containerSelector} 应当可见`).not.toBeNull()
  expect(box!.x + box!.width, `容器 ${containerSelector} 在 ${viewportWidth}px 宽下不应越出视口右边界`)
    .toBeLessThanOrEqual(viewportWidth + 1)
}

/**
 * 点名验「内容被裁掉」：只在调用方**明确知道该容器不允许滚动**时使用。
 *
 * 判据是「宽度被吃掉 **且** `overflow-x` 是 hidden」——后者是关键：
 * 可滚动容器（overflow auto/scroll）本来就该有滚动条，不算被裁。
 */
export async function expectNotClipped(page: Page, selector: string): Promise<void> {
  const clipped = await page.evaluate((sel) => {
    return Array.from(document.querySelectorAll(sel))
      .filter((el) => {
        const style = getComputedStyle(el)
        return el.scrollWidth > el.clientWidth + 1 && style.overflowX === 'hidden'
      })
      .map((el) => (el.textContent || '').trim().slice(0, 40))
  }, selector)

  expect(clipped, `${selector} 里不该有内容被裁掉`).toEqual([])
}
