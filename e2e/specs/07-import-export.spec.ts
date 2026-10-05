import { readFileSync } from 'node:fs'
import { expect, test } from '@playwright/test'
import { createQuestion, registerNewUser } from './support/helpers'

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
  await dialog.getByRole('button', { name: '关闭' }).click()

  // 真的进库了
  await expect(page.locator('.el-table__row', { hasText: stem })).toHaveCount(1)
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
