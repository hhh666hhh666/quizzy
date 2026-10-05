import { expect, test, type Page } from '@playwright/test'
import { createQuestion, registerNewUser } from './support/helpers'

/**
 * 题库页的**筛选条件与网址同步**。
 *
 * <p>这条链路里只有端到端能验的东西：
 *
 * <ul>
 *   <li><b>默认值的实际生效</b>——「范围」默认是「我的题库」，接口层看不到界面默认值；
 *   <li><b>多选下拉真的选得中、参数真的发出去</b>——接口层验的是「参数对不对」，
 *       验不到「界面有没有把参数拼出来」（数组参数尤其容易拼错，见 `toQueryParams` 的注释）；
 *   <li><b>网址与界面是同一份状态</b>——刷新后条件还在，这是纯粹的浏览器行为。
 * </ul>
 */

/**
 * 点查询区那颗按钮。
 *
 * <p>⚠️ 用正则容忍中间的空格：Element Plus 的按钮在 `autoInsertSpace` 打开时会把两个中文字
 * 渲染成「查 询」（<a href="https://element-plus.org/zh-CN/component/button.html">按钮文档</a>）。
 * 当前工程没套 `el-config-provider`，所以实际渲染是「查询」，但写死不带空格的话，
 * 哪天有人引入全局配置就会莫名其妙地红在这条定位上。
 */
const searchButton = (page: Page) => page.getByRole('button', { name: /查\s*询/ })
const resetButton = (page: Page) => page.getByRole('button', { name: /重\s*置/ })

test('题库页默认范围是「我的题库」：空表给出引导，切到「全部」能看到种子题库', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  await page.goto('/questions')

  // 默认范围不是「全部」而是「我的题库」
  await expect(page.locator('.el-form-item', { hasText: '范围' }).locator('.el-select')).toContainText('我的题库')

  // 新账号名下确实没有题（种子题库全是公开题），表格是空的
  await expect(page.locator('.el-table__row')).toHaveCount(0)

  // 空表不该是「一片空白」——得告诉人题在哪儿。这是「默认值改成我的题库」这个决定的配套代价。
  await expect(page.locator('.el-table__empty-block')).toContainText('我的题库里还没有题目')
  await expect(page.locator('.el-table__empty-block')).toContainText('公开题库')

  // 把范围切到「全部」再查 → 种子题库出现
  await pickOption(page, '范围', '全部')
  await searchButton(page).click()
  await expect(page.locator('.el-table__row').first()).toBeVisible()
})

test('按分类筛选：只勾一个分类就只剩它，条件写进网址、刷新后还在、重置能清干净', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = Date.now()
  const categoryA = `E2E分类甲${stamp}`
  const categoryB = `E2E分类乙${stamp}`
  const stemA = `E2E筛选甲${stamp}`
  const stemB = `E2E筛选乙${stamp}`
  const stemNoCategory = `E2E筛选无分类${stamp}`

  await createQuestionInCategory(page, stemA, categoryA)
  await createQuestionInCategory(page, stemB, categoryB)
  // 第三道不带分类，用来验「未分类」那个独立开关
  await createQuestion(page, { stem: stemNoCategory, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/questions')
  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemB })).toHaveCount(1)

  // ---------- 勾一个分类 ----------
  await pickCategory(page, categoryA)
  await searchButton(page).click()

  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemB })).toHaveCount(0)
  await expect(page.locator('.el-table__row', { hasText: stemNoCategory })).toHaveCount(0)
  // 条件写进了网址（默认值不写，所以只该有 categoryIds）。
  // ⚠️ 用 toHaveURL 而不是 expect(page.url())：后者只取一次值，会撞上 router.replace 还没写完
  //    的那一瞬间；toHaveURL 是会自动重试的。
  await expect(page).toHaveURL(/categoryIds=/)
  expect(page.url()).not.toContain('scope=')

  // ---------- 刷新后条件还在 ----------
  await page.reload()
  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemB })).toHaveCount(0)

  // ---------- 点「全部分类（点此清空）」，把勾上的分类清掉 ----------
  // ⚠️ 这一步之后**必须**在下拉外面点一下（下面点的「查询」）才能再动别的下拉，
  //    理由见 pickOption 的注释——这是本次在 CI 上真实踩过的坑。
  await clearCategories(page)
  await searchButton(page).click()
  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemB })).toHaveCount(1)
  // 清空后网址也回到了干净状态
  await expect(page).toHaveURL(/\/questions$/)

  // ---------- 换成「未分类」 ----------
  await pickCategory(page, '未分类')
  await searchButton(page).click()
  await expect(page.locator('.el-table__row', { hasText: stemNoCategory })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(0)
  // 界面上的「未分类」在网址/接口上是一个独立开关，不是某个分类的 id
  await expect(page).toHaveURL(/uncategorized=true/)

  // ---------- 重置 ----------
  await resetButton(page).click()
  // 网址清干净（等于回到了默认状态），三道题又都看得见
  await expect(page).toHaveURL(/\/questions$/)
  await expect(page.locator('.el-table__row', { hasText: stemA })).toHaveCount(1)
  await expect(page.locator('.el-table__row', { hasText: stemB })).toHaveCount(1)
})

/**
 * 造一道挂在指定分类下的题。
 *
 * <p>分类下拉开了 `allow-create`，所以直接输入一个**还不存在**的名字 + 回车就能选中——
 * 后端会按名字把分类建出来。这比「先建分类再建题」少一步，而分类本来就没有独立的新建入口。
 */
async function createQuestionInCategory(page: Page, stem: string, category: string): Promise<void> {
  await page.goto('/questions')
  await page.getByRole('button', { name: '新建题目' }).click()

  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toBeVisible()

  await dialog.locator('.el-form-item', { hasText: '题干' }).locator('textarea').fill(stem)
  const rows = dialog.locator('.option-row')
  await rows.nth(0).locator('.el-input__inner').fill('甲')
  await rows.nth(1).locator('.el-input__inner').fill('乙')
  await rows.nth(0).locator('.el-radio').click()

  const select = dialog.locator('.el-form-item', { hasText: '分类' }).locator('.el-select')
  await select.click()
  await select.locator('input').fill(category)
  await page.keyboard.press('Enter')

  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message--success')).toContainText('保存成功')
  await expect(dialog).toBeHidden()
}

/**
 * 在查询区某个下拉里选中一项（按显示文字找）。
 *
 * ⚠️ **进来时那个下拉必须是关着的。** 下拉若已经开着，这一下点击会把它**关掉**，
 * 紧接着找选项就一直找不到——报的是「element is not visible」，看着像元素凭空消失，
 * 其实是被自己上一步关掉的。所以**两次下拉操作之间必须先在下拉外面点一下**（点「查询」最自然）。
 *
 * ⚠️ **收尾不要用 Esc。** 点过下拉内部那几行（比如「全部分类（点此清空）」）之后焦点不在输入框上，
 * Escape 不一定会被 select 收到——这条在 CI 上实测踩过：下拉一直开着，于是下一次点击把它关掉。
 */
async function pickOption(page: Page, formItemLabel: string, optionText: string): Promise<void> {
  const select = page.locator('.el-form-item', { hasText: formItemLabel }).locator('.el-select')
  await select.click()

  const option = page
    .locator('.el-select-dropdown:visible .el-select-dropdown__item', { hasText: optionText })
    .first()
  // 先等它可见再点：等不到的报错比 30 秒超时清楚得多
  await expect(option).toBeVisible()
  await option.click()
}

/** 在「分类」多选里勾一个分类（可以是「未分类」）。 */
async function pickCategory(page: Page, optionText: string): Promise<void> {
  await pickOption(page, '分类', optionText)
}

/** 点「分类」下拉顶部那行「全部分类（点此清空）」，把已勾的分类清掉。下拉会保持开着。 */
async function clearCategories(page: Page): Promise<void> {
  const select = page.locator('.el-form-item', { hasText: '分类' }).locator('.el-select')
  await select.click()
  // 点是那一行自己，不是它的外层容器——外层容器上没有点击处理
  await page.locator('.el-select-dropdown:visible .select-header').click()
}
