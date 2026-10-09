---
description: 改 UI 前先看这条：五个外部设计 skill 的用途与禁区（装在用户级、按描述命中）。含 ui-ux-pro-max 禁用 --persist 的唯一硬约束。
alwaysApply: true
---

# 外部设计 skill 的使用纪律

五个 skill **装在用户级 `~/.workbuddy/skills/`、不在仓库里**——换机器不跟着走；它们**按描述命中、不是每次自动加载**，所以开工时**点名（或 @ 提及）最稳**。下表是**调度表的唯一位置**（`docs/design/前端.md` 只留文档域事实，不复述本表）。**逐项的角色、安装完整性与舍取原因**（档案性）见 [`docs/design/设计skill清单.md`](../../docs/design/设计skill清单.md)。

## 什么时候用哪个

| 什么时候 | 用哪个 | 备注 |
|---|---|---|
| 定方向 / 整体重设计 / 抽设计系统 | `impeccable` | 引擎首次调用会从 GitHub Releases 拉平台二进制（`~/.impeccable/bin/`） |
| 细节工艺：圆角、阴影、悬停态、图标、动效克制 | `make-interfaces-feel-better` | 整份安装 |
| 无障碍与规范审查：键盘、对比度、i18n、深色、网址状态 | `web-design-guidelines` | 每次从 Vercel 源拉最新规则，**需联网** |
| 查色板 / 字体搭配 / 风格资料 | `ui-ux-pro-max` | 见下方禁区 |
| 用 shadcn-vue 组件（`add` / `search` / preset） | `shadcn-vue`（官方） | ⚠️ `user-invocable: false`，**只能 AI 自己用，@ 不到** |

**一个都不设 `alwaysApply`**（不常驻注入）；`impeccable` **不装 hooks**（探测器只在被主动调用时跑）。理由见 [ADR 0031](../../docs/adr/0031-pc-frontend-rebuild-shadcn-vue.md)。

## ⚠️ 唯一硬约束：`ui-ux-pro-max` 只查不写

**禁用 `--persist` 与 `--force`。** 这个 skill 只当**查询台**（`--domain …`、以及不带 `--persist` 的 `--design-system` 都只打印、不写盘）。

`--persist` 会在**仓库根**落 `design-system/<slug>/MASTER.md`——它自称 "Global Source of Truth"，等于塞进**第三份设计真相**，与本仓铁律正面冲突：设计系统唯一真相源是 `quizzy-web/src/styles/tokens.css`（人类可读快照是根 `DESIGN.md`，**从实现派生**）。它不知道我们定的口味（冷中性 / 宽松 / L3 面色 / 正文近白 / 浮层实底），只会按关键词从自带语料里推一套行业惯例。

它的数据与引擎都在 skill 内部（纯本地、无网络），**保留的能力足以满足"查资料"这一用途**；被舍弃的 `cli/` 安装器、`templates/`、gallery 属分发与展示层，与本仓无关。`.gitignore` 已加 `design-system/` 兜底，防止手滑后被提交。

## 屏幕 / 工作流工具

Figma / Stitch 是**一次性参照**，**不是规格**：产物**不进仓库、不链文档、不当真相源**。理由同 ADR 0031。
