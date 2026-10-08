import { expect, test, type Page } from '@playwright/test'
import { createQuestion, lastMessage, questionRow, registerNewUser } from './support/helpers'

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

// 查询 / 重置按钮。正则容忍中间被插空格——EP 时代留下的习惯，新按钮没这个问题，容忍无害。
const searchButton = (page: Page) => page.getByRole('button', { name: /查\s*询/ })
const resetButton = (page: Page) => page.getByRole('button', { name: /重\s*置/ })

test('题库页默认范围是「我的题库」：空表给出引导，切到「全部」能看到种子题库', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  await page.goto('/questions')

  // 默认范围不是「全部」而是「我的题库」（单选下拉的触发钮直接显示当前项的文案）
  await expect(page.getByTestId('filter-scope').getByRole('combobox')).toContainText('我的题库')

  // 新账号名下确实没有题（种子题库全是公开题），表格是空的
  await expect(page.locator('.qb-row')).toHaveCount(0)

  // 空表不该是「一片空白」——得告诉人题在哪儿。这是「默认值改成我的题库」这个决定的配套代价。
  await expect(page.locator('.empty-hint')).toContainText('我的题库里还没有题目')
  await expect(page.locator('.empty-hint')).toContainText('公开题库')

  // 把范围切到「全部」再查 → 种子题库出现
  await pickOption(page, 'scope', '全部')
  await searchButton(page).click()
  await expect(page.locator('.qb-row').first()).toBeVisible()
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
  await expect(questionRow(page, stemA)).toHaveCount(1)
  await expect(questionRow(page, stemB)).toHaveCount(1)

  // ---------- 勾一个分类 ----------
  await pickCategory(page, categoryA)
  await searchButton(page).click()

  await expect(questionRow(page, stemA)).toHaveCount(1)
  await expect(questionRow(page, stemB)).toHaveCount(0)
  await expect(questionRow(page, stemNoCategory)).toHaveCount(0)
  // 条件写进了网址（默认值不写，所以只该有 categoryIds）。
  // ⚠️ 用 toHaveURL 而不是 expect(page.url())：后者只取一次值，会撞上 router.replace 还没写完
  //    的那一瞬间；toHaveURL 是会自动重试的。
  await expect(page).toHaveURL(/categoryIds=/)
  expect(page.url()).not.toContain('scope=')

  // ---------- 刷新后条件还在 ----------
  await page.reload()
  await expect(questionRow(page, stemA)).toHaveCount(1)
  await expect(questionRow(page, stemB)).toHaveCount(0)

  // ---------- 点「全部分类（点此清空）」，把勾上的分类清掉 ----------
  await clearCategories(page)
  await searchButton(page).click()
  await expect(questionRow(page, stemA)).toHaveCount(1)
  await expect(questionRow(page, stemB)).toHaveCount(1)
  // 清空后网址也回到了干净状态
  await expect(page).toHaveURL(/\/questions$/)

  // ---------- 换成「未分类」 ----------
  await pickCategory(page, '未分类')
  await searchButton(page).click()
  await expect(questionRow(page, stemNoCategory)).toHaveCount(1)
  await expect(questionRow(page, stemA)).toHaveCount(0)
  // ⚠️ 网址里是**界面态**的写法：`categoryIds=none`（哨兵值），不是 `uncategorized=true`。
  //    后者是**接口**上的参数，只在 apiQuery() 那一处翻译出来——两者别混，第一版就混错了。
  await expect(page).toHaveURL(/categoryIds=none/)

  // ---------- 重置 ----------
  await resetButton(page).click()
  // 网址清干净（等于回到了默认状态），三道题又都看得见
  await expect(page).toHaveURL(/\/questions$/)
  await expect(questionRow(page, stemA)).toHaveCount(1)
  await expect(questionRow(page, stemB)).toHaveCount(1)
})

/**
 * 造一道挂在指定分类下的题。
 *
 * <p>分类下拉开了 `allow-create`，所以直接输入一个**还不存在**的名字 + 回车就能选中——
 * 后端会按名字把分类建出来。这比「先建分类再建题」少一步，而分类本来就没有独立的新建入口。
 * （⚠️ 这个下拉在**新建题目对话框**里，对话框仍是 Element Plus——迁移按页推进，它还没轮到。）
 */
async function createQuestionInCategory(page: Page, stem: string, category: string): Promise<void> {
  await page.goto('/questions')
  await page.getByRole('button', { name: '新建题目' }).click()

  const dialog = page.getByRole('dialog', { name: '新建题目' })
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
  await expect(lastMessage(page, 'success')).toContainText('保存成功')
  await expect(dialog).toBeHidden()
}

/**
 * 打开单个下拉筛选（题型 / 难度 / 范围）并选中一项。
 *
 * <p>2026-10-08 起筛选下拉换成了 reka 的 Select：点触发钮 → 点选项，选中即收起。
 * Element Plus 时代那套「两次下拉之间必须先点空白处」「Esc 不一定收得掉」的坑，
 * 随旧控件一起退场了——新控件的开关行为是确定的，不需要任何舞蹈。
 */
async function pickOption(page: Page, filter: string, optionText: string): Promise<void> {
  await page.getByTestId(`filter-${filter}`).getByRole('combobox').click()
  const option = page.getByRole('option', { name: optionText })
  await expect(option).toBeVisible()
  await option.click()
}

/** 在「分类」多选面板里勾一个分类（可以是「未分类」）。面板保持开着，收尾用 Esc 收起。 */
async function pickCategory(page: Page, optionText: string): Promise<void> {
  await page.getByTestId('filter-category').getByRole('button').click()
  await page.getByRole('checkbox', { name: optionText }).click()
  await page.keyboard.press('Escape')
}

/** 点「分类」面板顶部那行「全部分类（点此清空）」，把已勾的分类清掉，然后 Esc 收起面板。 */
async function clearCategories(page: Page): Promise<void> {
  await page.getByTestId('filter-category').getByRole('button').click()
  await page.getByText('全部分类（点此清空）').click()
  await page.keyboard.press('Escape')
}
