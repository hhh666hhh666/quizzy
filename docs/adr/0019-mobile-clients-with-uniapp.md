# 移动端与微信小程序用 uni-app 单工程，PC 端不动

Status: accepted

quizzy 原先是「PC Web（`quizzy-web`）+ 后端」两端形态。本次要让它能在手机上用，并且**小程序将来确定会做**，同时**PC 端与移动端是各自不同的页面风格**——所以不是把 PC 页面压窄就能了事。结论：新增移动端形态，用 **uni-app（Vue3）单工程**编译出 **H5 与微信小程序**两个目标，仓库新增顶层目录 `quizzy-mobile`，与 `quizzy-server` / `quizzy-web` 并列；**PC 端 `quizzy-web` 完全不动**。

移动端**只做「消费」**：刷题、快速练习、错题本、答题记录，加上一个**只读的试卷列表**与从已有试卷开始答题；「建设」（题库增删改查、组卷、导入导出）留在 PC。它只新增入口，不新增业务能力（见 [《需求范围》](../requirements/scope.md)）。

## Considered Options

- **原生小程序 + 独立 H5（两套代码）**：双份维护——同一批页面、同一套状态在两端各写一遍，改一处要改两处。
- **Taro**：同为跨端方案，但它是 React 技术栈，与现有 Vue3 的贴合度不如 uni-app，换来的是一整轮重学与重写。
- **响应式改造现有 `quizzy-web`**（一套 PC 代码压窄）：与「PC 与移动端各有各的风格」直接冲突——移动交互不是缩小版 PC，硬塞进同一工程会污染 PC 端，条件样式迅速腐烂。
- **monorepo 工具链**（pnpm workspace / turborepo）：当前真正能共享的只有「接口契约知识」与 types，抽出共享包的成本大于收益。
- **PWA（manifest + Service Worker）**：类 App 体验交给小程序，H5 不叠 PWA，避免为同一目标维护两套「安装」机制；且微信内置浏览器不支持 PWA 安装。
- **微信登录先行**（先把 `wx.login → code2session → 账号绑定` 做完再上移动端）：后端要加 AppID/Secret、绑定关系、账号合并，属新能力；而现有账号密码登录可直接复用、后端零改动。
- **把「移动端 / 小程序 / 客户端」收进 `CONTEXT.md`，或建 `CONTEXT-MAP.md`**：这些是通用技术词，不满足「本上下文独有的概念」这一门槛；本仓仍是单上下文。
- **移动端也做「建设」**（题库增删改查 / 组卷 / Excel 导入导出）：手机做批量建题反人性，且 Excel 导入是 multipart 上传、导出是流式附件下载，在小程序里体验很差甚至不可行。

## Consequences

- 仓库新增顶层 `quizzy-mobile`，与两个现有目录并列，**不引入 workspace 工具链**。
- 移动端只读消费，建设类功能留在 PC；移动端与 PC **共用同一后端与同一账号**。
- 登录先复用现有 `POST /api/auth/login`（无状态 Bearer JWT）；微信登录后置，见 [`docs/todo/2026-10-03-TODO-微信登录与账号绑定.md`](../todo/2026-10-03-TODO-微信登录与账号绑定.md)。
- H5 走**同域子路径 `/m/`**，因此**不新增 CORS**——后端 `quizzy.cors.allowed-origins` 默认为空、靠同源部署生效（见 [《配置》](../operations/configuration.md)）。
- **外部阻塞**：境内服务器上 H5 与小程序**都**要 ICP 备案 + HTTPS；小程序另需微信「request 合法域名」白名单（不能填 IP）。**备案是不可压缩的关键路径，只卡上线、不卡文档与工程搭建**（见 [`docs/todo/2026-10-03-TODO-备案与HTTPS.md`](../todo/2026-10-03-TODO-备案与HTTPS.md)）。
- `CONTEXT.md` 与 `CONTEXT-MAP.md` **均不动**。
- 交付顺序：**先跑通移动 H5**（登录 + 刷题闭环），小程序作为同工程第二编译目标紧随。
- 未来上线时，web 入口需同时服务 `/m/`，届时同步 `docs/operations/deployment.md`——属未来改动，现在不动。
- 移动端前端设计另立一份文档（[《移动端》](../design/移动端.md)），不并入 [《前端》](../design/前端.md)（后者收敛为 PC 端专属）。

## 补充（2026-10-07）：账户自助的客户端归属

账户自助（改密码 / 改昵称 / 改头像 / 注销账号）既不是上面列的「消费」，也不在「建设」那张清单（题库增删改查 / 组卷 / 导入导出）里——它是第三类，**本次只落 PC**。

理由：本卡的默认倾向是「建设类留 PC」；账户设置是低频操作、不是刷题消费；而移动端**尚未上线**（卡在 ICP 备案，见 [备案与 HTTPS](../todo/2026-10-03-TODO-备案与HTTPS.md)），此时为它铺 UI 等于给一个没有用户的入口写代码，还要背上两套 UI 长期同步的维护成本。移动端真正需要账户自助时再单议，届时要定的第一件事是「生成头像那段算法要不要从 PC 复制过去」。范围与取舍见 [ADR 0027](0027-token-version-invalidates-all-devices.md)。

相关：[ADR 0017](0017-public-signup-service.md)（公开服务与注册风控）、[ADR 0016](0016-cloud-deploy-with-release-pipeline.md)（部署形态与对外入口）、[ADR 0001](0001-practice-not-exam-semantics.md)（练习语义，移动端沿用「消费」定位）、[ADR 0006](0006-bare-array-import-contract.md)（单一信息源纪律）。
