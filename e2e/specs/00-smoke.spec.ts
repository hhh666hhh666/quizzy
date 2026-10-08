import { expect, test } from '@playwright/test'

// 冒烟：证明「前端 + 后端 + 数据库」这条链路真的能从头跑通一次。
//
// 它存在的意义不是覆盖率——是补上 CI 里那个**从没被填过的洞**：此前
// `mvn test` 不拉 Spring、镜像 job 只 `docker build`，后端在流水线里从未被真正启动过。
// 这条用例是那个洞的最小封口，也是后面所有 spec 的地基。
//
// ⚠️ 断言优先用语义定位（role），只在「同一页重复出现或文案会变」时才用
// data-testid——登录 / 注册是同一张卡的两个状态、字段同名，靠 testid 标明用例
// 当前在哪一态。这条取舍见 docs/testing/系统说明.md 与 ADR 0020。

test('冒烟：注册一个新账号，登录态能撑过刷新', async ({ page }) => {
  // 账号名唯一：每次跑都新建，避免与上次残留撞名（导入不幂等那条教训同样适用）。
  const stamp = `${Date.now()}${Math.floor(Math.random() * 1000)}`
  const username = `e2e_${stamp}`
  const password = 'e2e-Passw0rd!'

  await page.goto('/')
  // 未登录会被路由守卫踢到 /login。
  await expect(page).toHaveURL(/\/login$/)

  // 切到「注册」态：登录 / 注册是同一张卡的两个状态，注册是一条链接。
  // 文案是页面级的稳定标识，用 role 定位即可。
  await page.getByRole('button', { name: '注册' }).click()

  await page.getByTestId('register-username').fill(username)
  await page.getByTestId('register-password').fill(password)
  await page.getByTestId('register-submit').click()

  // 注册成功 → 跳到题库页，并出现侧边栏。
  await expect(page).toHaveURL(/\/questions$/)
  await expect(page.getByRole('menuitem', { name: '题库' })).toBeVisible()

  // 刷新一次：这一步才真正验证了「token 落 localStorage + 后端能凭 token 认出人」，
  // 而不只是「前端内存里的状态看着对」。
  await page.reload()
  await expect(page).toHaveURL(/\/questions$/)
  await expect(page.getByRole('menuitem', { name: '题库' })).toBeVisible()
})
