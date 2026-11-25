# 项目级 skill 清单（`.agents/skills/`）

**目的**：存档**为什么只装了这些、装到哪一步、哪部分被舍弃**——回答"当初为什么这么装"，避免以后重新纠结或误判成遗漏。

**读者**：改前端 UI 或使用项目级 skill 的人 / 未来的自己 / AI。

**真相源**：本文件。设计类 skill 的**用途与禁区**速查表在 [`.agents/rules/design-skills.md`](../../.agents/rules/design-skills.md)（每次会话自动加载，那里才是"开工前该看"的）；本文件是**档案**，记录动机与完整性——两处**允许重叠，但速查口径以规则为准**。

> ⚠️ `ADR 0031` 只记了"选定这五个 + 均为部分安装 + 不设 alwaysApply + impeccable 不装 hooks"这一层结论。**逐项的安装完整性与舍取原因只在本文**——上游核实过（`nextlevelbuilder/ui-ux-pro-max-skill` 的 README 与目录树），不是凭印象。

## 设计类 skill 一览

| # | Skill | 角色 | 安装完整性 | 舍弃了什么 | 舍弃原因 | 什么时候用 |
|---|---|---|---|---|---|---|
| 1 | **impeccable** | 方向 + 确定性门禁（设计总导演） | ⚠️ 部分安装（分两批） | ① `hooks/` **未装**；② 一度只装 guidance（`scripts/` 后补） | ① hooks 每次编辑自动跑探测，属"守护进程式"介入，把"运行期拉外部二进制"的风险面从"主动调"扩到"每次编辑"；② 分两批只是**落地顺序**，非取舍 | 定方向 / 整体重设计 / 抽设计系统（`shape`/`init`/`document`/`extract`），及 `critique`/`audit`/`polish` |
| 2 | **make-interfaces-feel-better** | 细节工艺 + 审查格式 | ✅ 整份（57K） | 无 | — | 细节工艺：同心圆角、阴影/描边取舍、悬停/按下态、图标描边、动效克制、字体平滑、tabular-nums |
| 3 | **web-design-guidelines** | 无障碍 / 合规审查 | ✅ 整份（4.0K 单文件） | 无 | — | 规范审查：`review my UI` / `check accessibility` / `audit design`。⚠️ 每次从 Vercel 源拉最新规则（**需联网**） |
| 4 | **shadcn-vue** | 组件用法（官方） | ✅ 整份（72K） | 无 | — | 用 shadcn-vue 组件：`add`/`search`/`docs`/preset/调试。⚠️ `user-invocable: false`（部分宿主只能 AI 自己按描述触发、@ 不到） |
| 5 | **ui-ux-pro-max** | 检索型资料库（查询台） | ⚠️ 部分安装：装 Basic 版运行时核心，未装分发层 | ① **`cli/` 安装器**；② **`templates/`**（CLI 生成各平台文件用）；③ **`gallery/` 展示**；④ **`--persist` 写盘行为** | ①④ 与"单一信息源"纪律冲突（`MASTER.md` 自称 Source of Truth，会和 `tokens.css` / `DESIGN.md` 打架）；②③ 属分发与展示层，与"只做检索"的定位无关 | **只当查询台**：查色板 / 字体搭配 / 风格(79) / UX 规则(119) / 图标 / 图表 / 栈实现。⚠️ **只查不写 —— 禁用 `--persist` / `--force`** |

**⚠️ 不列入"舍弃"的一项**：上游 **Premium（付费）版**含 `brand` / `slides` / `banner-design` / 自定义图标等扩展——**Basic（开源）版本就没有它们**，属"从未存在"，不是本仓的舍弃项（依据：上游 README 的 "Basic vs Premium Version Comparison"）。

## 同在 `.agents/skills/` 下的其他项目级 skill

除上述 5 个前端设计 skill 外，另收录 4 组（5 个目录）与本仓协作流程和题库生产直接绑定的 skill：

| Skill | 角色 | 什么时候用 |
|---|---|---|
| **wrap-up** | 收尾自检（原 `.codebuddy/commands/wrap-up.md`） | 每次改动收尾或输入 `/wrap-up` 时：捞未决事项、补文档、跑断链检查、更新工作台并提议 commit |
| **domain-modeling** | 领域术语表与 ADR 治理 | 讨论新概念、维护 `CONTEXT.md`、按三条标准克制决定是否新建 ADR |
| **grilling** / **grill-with-docs** | 决策树追问对齐（后者联动 `domain-modeling`） | 复杂需求或方案开工前，分轮追问消歧并同步沉淀术语与 ADR |
| **md-to-question-bank** | Markdown → Quizzy 题库 JSON 流水线 | 把 `question-bank/` 下的 Markdown 笔记切分、出题、溯源校验并合并成裸数组 JSON（对齐 [《导入导出》](./导入导出.md)） |

## 共同约定（全部项目级 skill 适用）

- **都不设 `alwaysApply`**：按描述按需激活，不常驻注入上下文。
- **统一放在项目级 `.agents/skills/`**（2026-11-25 由本机用户级 `~/.workbuddy/skills/` 迁入仓库）：Antigravity 自动发现，随仓库跨机器同步；不扫 `.agents/skills/` 的工具可按路径显式读取或在用户级保持同步。
- **设计类取舍理由**见 [ADR 0031](../adr/0031-pc-frontend-rebuild-shadcn-vue.md)。其中 `impeccable` 的 hooks **仍未装**（同卡 2026-10-07 注记）。

## 逐项细节

### 1. impeccable

| 组件 | 状态 | 说明 |
|---|---|---|
| `SKILL.md` | ✅ | v4.5.0 |
| `reference/*.md` | ✅ 45 份 | 含 `hooks.md`、`init.md`、`live*.md`、`craft-floor.md`、`degraded/` |
| `scripts/` | ⚠️ **先没装，后补** | 补后才有 `impeccable context` / `detect --json` 的确定性扫描 |
| 引擎二进制 | ⚠️ **运行期拉取** | 首次调用从 GitHub Releases 按版本锁拉 `0.1.11` → `~/.impeccable/bin/0.1.11/impeccable.exe`；**本仓唯一一处"运行期拉并执行外部二进制"** |
| `hooks/` | ❌ **未装** | 探测器只在被主动调用时跑 |

**回退方式**：删 `~/.impeccable` 与 skill 下的 `scripts/`，即回到"只用 guidance"。

### 5. ui-ux-pro-max

**装了（Basic 版运行时核心）**：`SKILL.md`、`scripts/`（`core` / `search` / `design_system` / `reasoning_contract` / `validate_data` + `tests/`）、`references/`、`data/`（全套 CSV/JSON，含 `stacks/` 22 个栈表）。

**未装（上游顶层，属分发层）**：`cli/` 安装器（`npm i -g ui-ux-pro-max-cli` → `uipro init --ai <平台>`）、`templates/`、`.claude/` / `.factory/`（作者本地开发用）。

**⚠️ 唯一硬约束**：`--persist` 会往**仓库根**写 `design-system/<slug>/MASTER.md`（自称 "Global Source of Truth"）+ `pages/` → 与本仓"设计真相源 = `tokens.css`"冲突。**禁用 `--persist` / `--force`**；`.gitignore` 已加 `design-system/` 兜底。注意：**不带 `--persist` 的 `--design-system` 只打印、可用**，被砍的只是"写盘副作用"。

**环境前提**：搜索脚本需 **Python 3.x**（`python` / `python3` / `py -3` 任一）。
