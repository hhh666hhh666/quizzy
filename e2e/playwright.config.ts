import { defineConfig, devices } from '@playwright/test'

// 端到端测试的配置。设计取舍见 docs/testing/系统说明.md，落地顺序见
// docs/todo/2026-10-04-TODO-测试系统落地.md。
//
// 三条刻意的选择，改之前先读：
//
// 1. **不在这里起被测系统**（不用 webServer）。CI 上 MySQL / 后端 / 前端各自是
//    独立的步骤（见 .github/workflows/ci.yml 的 e2e job），配置只负责「连上去跑」。
//    这样测试配置里不会出现第二份「怎么启动服务」的说法——那是 pom.xml 与
//    package.json 的事（ADR 0006）。
// 2. **视口只跑桌面 Chromium**。跨浏览器矩阵与移动视口都是明确不做 / 后置，
//    见 ADR 0020 与其 Amendment 1。
// 3. **workers 固定 1**。用例之间共享同一个数据库与同一个后端进程，
//    并行会互相踩（注册出的账号、题目都会串）。要并行得先做数据隔离，
//    那是另一个决定。

const BASE_URL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173'

// 本机没下载浏览器时，用 E2E_CHANNEL=chrome 驱动已装的 Chrome。
// CI 上留空 = 用 `npx playwright install` 装的那份 Chromium。
const CHANNEL = process.env.E2E_CHANNEL || undefined

export default defineConfig({
  testDir: './specs',
  timeout: 30_000,
  expect: { timeout: 5_000 },
  fullyParallel: false,
  workers: 1,
  // 重试只给 1 次：真失败的用例重试一次仍会失败，而偶发抖动值得被容忍。
  retries: process.env.CI ? 1 : 0,
  forbidOnly: !!process.env.CI,
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['list']],
  use: {
    baseURL: BASE_URL,
    channel: CHANNEL,
    // 失败留证据：trace 可回放整条操作，截图看当时的页面。
    // 不启用 `video`——录视频要么全跑要么不跑，代价大而排查价值与 trace 重叠。
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure'
  },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
})
