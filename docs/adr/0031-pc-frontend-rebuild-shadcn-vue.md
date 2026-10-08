# PC 前端重建：换用 shadcn-vue + Tailwind，并按需引入设计 skill

Status: accepted

PC 端（`quizzy-web`）**原地重建**：组件基座由 Element Plus 换成 **shadcn-vue（Reka UI + Tailwind CSS v4）**，并引入一组**外部设计 skill**。起因是把「尽可能精美、有设计感」当硬目标后，Element Plus 的默认外观与它锁死的 `--el-*` 主题体系成了设计上限——继续在它上面换肤，天花板太低。同时定下**深浅两档主题、默认浅色**（深色不追求观感）、**本轮不动信息架构**、设计工具（Figma / Stitch）只当**一次性参照**（不当规格）。

## Considered Options

- **保留 Element Plus，只做深度换肤**：改动最小，但设计上限被它的默认外观与 DOM 结构卡住，「尽可能精美」做不到。**被否**。
- **改用 Naive UI**：可主题化、开箱即用，但仍是外部依赖、DOM 不受控，且与设计类 skill 的 Tailwind 取向对不上。**被否**。
- **迁到 React 系（shadcn/ui）**：设计生态最成熟，但换框架是另一量级的风险与成本。**被否**。
- **原地重建为 shadcn-vue + Tailwind CSS v4**（选定）：组件源码拷进仓库、结构与样式全归自己，Tailwind + CSS 变量天然就是 token 层；也是官方 skill 与设计工具的共同取向。
- **只保留一套主题（浅色）**：曾被采纳又**反转**——理由是「浅 → 深日后要便宜」，而 shadcn-vue 本就带 `.dark` 变量块，出两套的增量很小。**被否**（改为深浅两档、默认浅色）。
- **设计工具（Figma / Stitch）当设计真相源**：要持续同步、且与仓库既有的单一信息源纪律冲突。**被否**（只当一次性参照）。
- **设计 skill 取 `frontend-design`**：与 Impeccable 同槽（审美观点）且是它的前身。**被否**。
- **设计 skill 取 `taste-skill`**：同槽，且带 GSAP 动效体系，与「纯 CSS、克制」的取向相抵。**被否**。
- **设计 skill 取 `brand-guidelines`**：与 Impeccable 的 `DESIGN.md` 争「设计真相源」。**被否**。
- **设计 skill 取 Impeccable + make-interfaces-feel-better + web-design-guidelines + ui-ux-pro-max + `unovue/shadcn-vue` 官方 skill**（选定，**均为部分安装**）：依次覆盖「方向 + 确定性门禁」「细节工艺 + 审查格式」「无障碍 / 合规审查」「检索型资料库」「组件用法」；**一个都不设 alwaysApply**（ADR 0014），Impeccable **不装 hooks**。

## Consequences

- **必须重写**：15 个 `views/*.vue`、3 个 `components/*.vue`、`main.ts` / `App.vue` / `layouts/MainLayout.vue`；`api/request.ts` 的错误提示要从 `ElMessage` 解耦。
- **原样保留**：`src/stores/`、`src/types/`、`src/utils/`、`src/router/`（已核实与组件库无关）。
- **新增依赖与构建步骤**：Tailwind CSS v4 + `@tailwindcss/vite`；npm 锁文件必须在**干净临时目录**生成（否则只写当前平台的原生可选依赖）。
- **组件源码进仓库**：升级即「重拉合并」，维护自担；同时 Radix/Reka 的 overlay 默认 portal 到 body，`el-dialog` 那类「弹窗被表格盖住」的坑在结构上不再出现。
- **设计文档落在仓库根**：`PRODUCT.md` / `DESIGN.md` 由 Impeccable 产出（`DESIGN.md` 是**派生快照**，源头是实现）；两者与 `scope.md` **允许重叠，冲突以 `scope.md` 为准**；`.impeccable/design.json` 忽略、`.impeccable/surfaces/*.md` 保留。
- **主题**：两档、默认浅色；深色**直接用 shadcn-vue 默认值、不追求观感**；`color-scheme` 按主题设置；token 一律语义命名。
- **E2E**：以现有 13 个 spec 为**行为规格**逐条移植（`.el-*` 选择器必换）；`10-theme.spec.ts` 保留并改为两档。
- **反噬到一条既有测试决策**：`docs/testing/系统说明.md` 里「暂缓组件测试」的理由之一是「无共享组件库」——shadcn-vue 的组件源码进仓库后**该理由不再成立**，需在重构落地后重新评估。
- **代码落地时**才同步的文档：`README.md` 技术栈表、`docs/design/前端.md`（整篇）、`docs/design/移动端.md` 里提到 PC 栈的那句、`CHANGELOG.md`。
- 属破坏性大改，按 ADR 0023 **单独发版**。

> ⚠️ **后续（2026-10-07）**：Impeccable 的 `scripts/` **已装**。它的启动器会在**首次调用**时从 GitHub Releases 按版本锁定拉取平台二进制（本机落在 `~/.impeccable/bin/0.1.11/impeccable.exe`，探针实测返回 `impeccable-engine 0.1.11`）——这是本仓库**唯一一处"运行期拉取并执行外部二进制"**，故在此记明来历。**hooks 仍未装**：探测器只在我们主动调用时才跑。回退方式：删 `~/.impeccable` 与该 skill 下的 `scripts/`，即回到只用 guidance。

> **Amendment 1（2026-10-08）：重建收官。** 三条里程碑全部兑现——① **Element Plus 彻底退场**（`8887fa6`，全站 reka-ui + 自建件，`<el-*>` 全仓清零）；② **`DESIGN.md` 已由 `impeccable document` 生成并入库**（`design.json` 边车按本文继续忽略）；③ 全站规范审查（Vercel Web Interface Guidelines）**整改完成**（键盘可达性、URL 状态铺开、theme-color/favicon、tabular-nums 等，详见 CHANGELOG 的 [Unreleased] 段）。与本文记录的两处现实差异，记录在案：
> - **主题最终是三档**（跟随系统 / 浅色 / 深色），比 Consequences 写的「两档」多一档；**深色色板本身尚未落地**（当前只切 `color-scheme` 与少数组件变体），属后续功能。
> - **浮层材质定为「练习纸实底 + 1px 描边 + 遮罩压暗」**而非模糊玻璃（弹窗里表单与正文占大头，实底保对比度）；轻玻璃只服务顶栏。tokens 注释与 `DESIGN.md` 均已按此对齐。
> Consequences 里「代码落地时同步的文档」清单（README / `前端.md` / `移动端.md` / CHANGELOG）**已全部结算**；「组件测试重评」也已按承诺兑现——结论见 [《测试系统说明》](../testing/系统说明.md)（继续暂缓，理由改写）。
