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
  // 登录 / 注册同卡两态：点「注册」链接切过去（role 定位，文案是页面级稳定标识）
  await page.getByRole('button', { name: '注册' }).click()
  await page.getByTestId('register-username').fill(username)
  await page.getByTestId('register-password').fill('e2e-Passw0rd!')
  await page.getByTestId('register-submit').click()
  await expect(page).toHaveURL(/\/questions$/)
  return username
}

/**
 * 题库列表的一行（2026-10-08 起表格自绘，行类名是 `.qb-row`）。
 *
 * <p>按题干（或任意文本）找行——各 spec 里「表格里该有 / 不该有某题」的断言都走这里，
 * 免得选择器散落一份份的。
 */
export function questionRow(page: Page, text: string) {
  return page.locator('.qb-row', { hasText: text })
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
export async function expectNoHorizontalOverflow(page: Page, containerSelector = '.app-main'): Promise<void> {
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
 * 用「新建题目」对话框造一道题。
 *
 * <p>为什么端到端用例要自己造题：错题本、多选判分这些用例**必须**知道正确答案是什么，
 * 而种子题库里的题是随机的。自己造才知道「选哪个是错的」。
 *
 * <p>⚠️ Element Plus 的 label 不与 input 关联（没有 `for`），所以统一走
 * 「先定位表单项、再取其输入框」；选项行靠 `.label` 里的字母认。
 */
export async function createQuestion(
  page: Page,
  opts: {
    stem: string
    options: string[]
    correct: string[]
    type?: 'SINGLE' | 'MULTI' | 'JUDGE'
  }
): Promise<void> {
  const type = opts.type ?? 'SINGLE'

  await page.goto('/questions')
  await page.getByRole('button', { name: '新建题目' }).click()

  const dialog = page.locator('.el-dialog:visible')
  await expect(dialog).toBeVisible()

  // 默认题型就是单选，只在不是单选时才点——顺带把「默认值能用」当成隐性断言。
  // ⚠️ el-radio-button 的 <input> 拿不到可访问名（用 getByRole('radio') 定不到），
  //    直接点它外面那层 label，与「点选项 label」的做法一致。
  if (type !== 'SINGLE') {
    await dialog.locator('.el-radio-button', { hasText: type === 'MULTI' ? '多选题' : '判断题' }).click()
  }

  await dialog.locator('.el-form-item', { hasText: '题干' }).locator('textarea').fill(opts.stem)

  // 对话框默认只给两个选项，不够就点「+ 增加选项」
  const rows = dialog.locator('.option-row')
  for (let i = await rows.count(); i < opts.options.length; i++) {
    await dialog.getByRole('button', { name: '+ 增加选项' }).click()
  }
  for (let i = 0; i < opts.options.length; i++) {
    await rows.nth(i).locator('.el-input__inner').fill(opts.options[i])
  }

  // 标记正确项：单选/判断渲染成 el-radio，多选渲染成 el-checkbox
  const marker = type === 'MULTI' ? '.el-checkbox' : '.el-radio'
  const rowCount = await rows.count()
  for (let i = 0; i < rowCount; i++) {
    const letter = (await rows.nth(i).locator('.label').innerText()).trim()
    if (opts.correct.includes(letter)) {
      await rows.nth(i).locator(marker).click()
    }
  }

  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(lastMessage(page, 'success')).toContainText('保存成功')
  await expect(dialog).toBeHidden()
}

/**
 * 用「新建试卷」对话框组一张**固定卷**，把指定题干的题选进去。
 *
 * 固定卷而不是规则卷：题是**确定的**，用例才能知道「该选哪个才对」。
 */
export async function createFixedPaper(page: Page, title: string, pickStems: string[]): Promise<void> {
  await page.goto('/papers')
  await page.getByRole('button', { name: '新建试卷' }).click()

  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toBeVisible()
  await dialog.locator('.el-form-item', { hasText: '标题' }).locator('.el-input__inner').fill(title)
  // 模式默认就是固定卷，不点它

  await dialog.getByRole('button', { name: '选择题库题目' }).click()
  const selector = page.locator('.el-dialog:visible').last()
  for (const stem of pickStems) {
    const row = selector.locator('.el-table__row', { hasText: stem })
    await expect(row, `题库里应当能找到「${stem}」`).toHaveCount(1)
    await row.locator('.el-checkbox').first().click()
  }
  await selector.getByRole('button', { name: '加入试卷' }).click()

  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(lastMessage(page, 'success')).toContainText('保存成功')
  await expect(dialog).toBeHidden()
}

/**
 * 断言「最近一条」消息提示，取 `.last()`。
 *
 * <p>⚠️ 这个 `.last()` 不是洁癖，是**必需**：`el-message` 是**堆叠**的——上一步的提示
 * 3 秒后才消失；而 Playwright 的 `toContainText` 在定位到**多个元素**时是**严格模式报错**
 * （不是「任一匹配」）。2026-10-06 就栽在这上面：选题器「加入试卷」多了一条成功提示，
 * 于是 `createFixedPaper` 里那句「点保存后断言保存成功」撞上两条堆叠消息，
 * **4 条本来无关的用例一起红了**。
 *
 * <p>所以**所有**消息断言都走这个函数，别再手写 `.locator('.el-message--x')`。
 */
export function lastMessage(page: Page, type: 'success' | 'info' | 'warning' | 'error') {
  return page.locator(`.el-message--${type}`).last()
}

/** 从试卷列表里找到这张卷，点「开始作答」，并等到答题页。 */
export async function startPaperQuiz(page: Page, paperTitle: string): Promise<void> {
  await page.goto('/papers')
  const row = page.locator('.el-table__row', { hasText: paperTitle })
  await expect(row).toHaveCount(1)
  await row.getByRole('button', { name: '开始作答' }).click()
  await expect(page).toHaveURL(/\/quiz\/\d+$/)
}

/**
 * 断言弹窗**真的浮在最上面**，不会被表格的行穿透。
 *
 * <p>为什么需要它：弹窗的层级不是天然就对的。把 `el-dialog` 写在「被表格 / 滚动容器包住」的位置
 * （2026-10-07 那次是挂在表格单元格里的星标上），它的遮罩与弹窗会被困在那一层，
 * 现象是**表格的行盖到弹窗上来**——肉眼一眼看得出，但**几何断言完全测不出来**
 * （它们只量位置尺寸，而位置尺寸都是对的）。
 *
 * <p>两条判据**都要过**，各管一段：
 * <ol>
 *   <li><b>根因</b>：遮罩不能落在 `.el-table` / `.el-scrollbar` 内部——中了就说明这个弹窗没写
 *       `append-to-body`，早晚要被穿透。这条是主力，稳定且换布局也不飘。</li>
 *   <li><b>现象</b>：身上撒九个点做命中测试，最上面都得是弹窗自己。
 *       ⚠️ 它没那么可靠：穿透只发生在「表格真的盖到弹窗」的那片区域上，表格不够高时一个点都碰不到
 *       （第一版就是因此假的绿）。留着是当兜底——它抓到的是**用户真正看到的那一层**。</li>
 * </ol>
 *
 * <p>⚠️ 只测**当前可见**的那个弹窗（取最后一个）：`el-dialog` 打开过就会留在 DOM 里，
 * 用 `querySelector` 会拿到先前打开过的那个隐藏弹窗。
 */
export async function expectDialogOnTop(page: Page, dialogSelector = '.el-dialog'): Promise<void> {
  const problems = await page.evaluate((sel) => {
    const visible = Array.from(document.querySelectorAll(sel)).filter((el) => {
      const r = el.getBoundingClientRect()
      return r.width > 0 && r.height > 0
    })
    const dialog = visible[visible.length - 1]
    if (!dialog) {
      return ['找不到可见的 ' + sel]
    }
    const problems: string[] = []

    const overlay = dialog.closest('.el-overlay')
    const trapped = overlay?.closest('.el-table, .el-scrollbar')
    if (trapped) {
      problems.push(
        `弹窗的遮罩被渲染在 ${trapped.tagName}.${String(trapped.className).slice(0, 32)} 里` +
          '——给 el-dialog 加 append-to-body，否则表格的行会盖到弹窗上'
      )
    }

    const r = dialog.getBoundingClientRect()
    const xs = [r.left + 24, r.left + r.width / 2, r.right - 24]
    const ys = [r.top + 24, r.top + r.height / 2, r.bottom - 12]
    for (const y of ys) {
      for (const x of xs) {
        const hit = document.elementFromPoint(x, y)
        const ok = hit != null && (hit === dialog || dialog.contains(hit))
        if (!ok) {
          problems.push(
            `(${Math.round(x)}, ${Math.round(y)}) 命中的是 ${
              hit ? hit.tagName + '.' + String(hit.className).slice(0, 40) : 'null'
            }`
          )
        }
      }
    }
    return problems
  }, dialogSelector)

  expect(problems, '弹窗必须盖住它下方的一切（表格的行、卡片…）').toEqual([])
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
