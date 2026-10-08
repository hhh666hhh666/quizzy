import { readFileSync } from 'node:fs'
import { expect, test } from '@playwright/test'
import { createQuestion, questionRow, registerNewUser } from './support/helpers'

/**
 * 导入与导出。
 *
 * <p>这两条补的都是「**跨模块 + 有副作用**」的链路：导入会把一批题写进库、
 * 导出会把库里的内容变成文件，接口层只能验响应，验不到「文件真的下来了 / 题真的进去了」。
 */

test('批量导入（JSON 粘贴）：题目入库、给出逐行报告、出现在列表里', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stem = `IT 导入的题 ${Date.now()}`
  const payload = JSON.stringify([
    {
      type: 'SINGLE',
      stem,
      options: [
        { label: 'A', content: '甲' },
        { label: 'B', content: '乙' }
      ],
      answer: ['A']
    }
  ])

  await page.goto('/questions')
  await page.getByRole('button', { name: '批量导入' }).click()

  const dialog = page.locator('.el-dialog:visible')
  // 默认停在 Excel 页签，切到 JSON 页签——粘贴文本比造文件稳
  await dialog.locator('.el-radio-button, .el-tabs__item', { hasText: 'JSON 导入' }).first().click()
  await dialog.locator('textarea').fill(payload)
  await dialog.getByRole('button', { name: '开始导入' }).click()

  // 逐行报告：共 N 条 / 成功 M 条
  await expect(dialog.locator('.report')).toContainText('共 1 条')
  // ⚠️ 必须限定在 footer 里：弹窗右上角那个 × 的 `aria-label` 在中文语言包下**也叫「关闭」**
  //    （英文时是 "Close"，所以早先不限定也能过），不限定就会同时命中两个元素、严格模式报错。
  //    要关弹窗请用 `.el-dialog__headerbtn`（按类找，不看文案），见 06 号用例的收尾写法。
  await dialog.locator('.el-dialog__footer').getByRole('button', { name: '关闭' }).click()

  // 真的进库了
  await expect(questionRow(page, stem)).toHaveCount(1)
})

test('批量导入并顺带建卷：卷出现在试卷列表、题就在里面', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stamp = String(Date.now())
  const stem = `IT 导入建卷的题 ${stamp}`
  const title = `ITimport${stamp}`
  const payload = JSON.stringify([
    {
      type: 'SINGLE',
      stem,
      options: [
        { label: 'A', content: '甲' },
        { label: 'B', content: '乙' }
      ],
      answer: ['A']
    }
  ])

  await page.goto('/questions')
  await page.getByRole('button', { name: '批量导入' }).click()

  const dialog = page.locator('.el-dialog:visible')
  await dialog.locator('.el-radio-button, .el-tabs__item', { hasText: 'JSON 导入' }).first().click()
  await dialog.locator('textarea').fill(payload)
  // 勾上才会建卷；标题是必填的
  await dialog.getByTestId('import-create-paper').click()
  await dialog.getByPlaceholder('试卷标题（必填）').fill(title)
  await dialog.getByRole('button', { name: '开始导入' }).click()

  await expect(dialog.locator('.report')).toContainText('共 1 条')
  // 回执里点名了卷名——证明后端真的建了，而不是前端自己编了一句
  await expect(dialog.locator('.paper-created')).toContainText(title)

  // 真的建出来了，而且题在里面
  await dialog.getByTestId('import-goto-papers').click()
  await expect(page).toHaveURL(/\/papers$/)
  const row = page.locator('.paper-row', { hasText: title })
  await expect(row).toHaveCount(1)
  await row.getByRole('button', { name: '编辑' }).click()
  const editDialog = page.getByRole('dialog', { name: '编辑试卷' })
  await expect(editDialog).toContainText('共 1 道')
  await expect(editDialog.locator('.picked-row', { hasText: stem })).toHaveCount(1)
})

test('导出 JSON：真的下载到文件，且内容里带得刚才那道题', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stem = `IT 待导出的题 ${Date.now()}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/questions')
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: '导出 JSON' }).click()
  ])

  expect(download.suggestedFilename()).toMatch(/^quizzy-questions-\d{14}\.json$/)

  // 光有文件名不够——内容里得有刚才那道题，才算真的导出来了
  const file = await download.path()
  expect(file).toBeTruthy()
  expect(readFileSync(file!, 'utf8')).toContain(stem)
})

test('导出 Excel：真的下载到 .xlsx 文件，且是一个合法的 xlsx', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 900 })
  await registerNewUser(page)

  const stem = `IT 待导出的题（Excel） ${Date.now()}`
  await createQuestion(page, { stem, options: ['甲', '乙'], correct: ['A'] })

  await page.goto('/questions')
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: '导出 Excel' }).click()
  ])

  // 文件名形如 quizzy-questions-<14 位时间戳>.xlsx。此前这里曾经是 .excel
  // （把「格式选择器的值」直接当扩展名），且时间戳是 UTC（比北京时间早 8 小时）。
  expect(download.suggestedFilename()).toMatch(/^quizzy-questions-\d{14}\.xlsx$/)

  // 后缀对了还不够：内容得是个真 zip（xlsx 就是 zip，头两字节是 PK），
  // 证明服务端真的走了 easyexcel 那条写路径，而不是回了一坨错误 JSON。
  const file = await download.path()
  expect(file).toBeTruthy()
  expect(readFileSync(file!).subarray(0, 2).toString('latin1')).toBe('PK')
})
