# 需求范围

**目的**：说清 quizzy 到底做哪些事、明确不做哪些事，以及这些边界当初为什么这么定。

**读者**：要判断某个改动属于范围内还是范围内的人（多数时候就是未来的自己）。

**真相源**：范围是本文档；**为什么**这样定见各条 [ADR](../adr/)；决策过程见 [决策日志](../decisions/)；未决事项只在 [`docs/todo/`](../todo/)。

---

## 产品定位

quizzy 是一个**以「一道题反复练到会」为核心的刷题工具**。它原先只给自己用（纯本机 `localhost`）；2026-10-01 决定**改为对公网开放、任何人注册即可使用**（迁移进行中，见 [ADR 0017](../adr/0017-public-signup-service.md)）。

它采用**练习语义**而非考试语义：逐题作答、提交后即时看到对错与解析、答错的题进错题本反复练。这条定位决定了后面几乎所有取舍——凡是只服务于「防人」的功能（限时、防作弊、监考、成绩排名）都不在范围内，理由见 [ADR 0001](../adr/0001-practice-not-exam-semantics.md)。

**「开放」只改了「谁来用」，没有改「练习语义」。** 它带来的是运行与合规义务——域名、ICP 备案、HTTPS、注册风控、备份责任——不是新的业务能力。

## 运行形态

**目标形态**（2026-10-01 定，迁移进行中）：跑在阿里云轻量应用服务器上，推 `git tag` 后由 CI 构建镜像并经 ACR 自动部署，本机只剩开发形态。对外入口由宿主上既有的 nginx 承担（那台机器上跑着宝塔面板），证书走宝塔申请与续期。见 [ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md) 及其 Amendment 1。

⚠️ **迁移完成前，实际形态仍是本机 Docker Desktop**——跑法以 [../operations/deployment.md](../operations/deployment.md) 为准，那份文档在行为真正改变之前不会提前改。

**客户端形态**（2026-10-03 定）：PC Web（`quizzy-web`）+ 移动 H5 + 微信小程序，三者**共用同一后端与账号**。移动端**只做「消费」**（刷题、快速练习、错题本、答题记录，以及只读试卷列表 + 从已有试卷答题），「建设」（题库增删改查、组卷、导入导出）留在 PC。它只新增入口，**不新增业务能力**。见 [ADR 0019](../adr/0019-mobile-clients-with-uniapp.md)。

## 范围

按业务能力划分，不按技术分层：

- **题库**：题目的增删改查，支持按关键词、题型、难度、分类、归属范围筛选
- **导入导出**：Excel 与 JSON 双向；导入时跳过错误行并给出逐行错误报告
- **组卷**：固定卷（手动选题）与规则卷（按条件现场抽题）
- **答题**：逐题作答、即时判分与解析、可回退改答案、进度落库因而可断点续答
- **错题本**：答错自动入本，练熟后自动移出，也支持手动移出与一键错题重练
- **作答记录**：历史会话列表，未完成的可以继续作答
- **用户**：注册后即可自建题库。公开注册带图形验证码与按 IP 限流，不做后台管理与角色权限（[ADR 0017](../adr/0017-public-signup-service.md)）

具体实现边界见 [《判分与业务规则》](../design/判分与业务规则.md) 与 [《数据模型》](../design/数据模型.md)。

## 明确不做

这些是**确定的边界**，不是在排期的待办：

- 填空题 / 问答题 / 编程题——当前只有选择题（单选、多选、判断）
- 限时考试、防作弊、监考、成绩排名
- 统计看板与掌握度分析
- 后台用户管理与角色权限（RBAC）
- 图片上传与富文本编辑器
- Redis、消息队列、多级缓存
- Refresh Token 机制
- Excel 与 JSON 之外的导入格式

**一个例外**（2026-10-01 因改为公开服务而开）：需要能**封禁滥用账号、删除违规内容**。
这仍然不是上面那条「后台用户管理与角色权限（RBAC）」——它是一个布尔字段级的最小开关，
不是权限体系。理由见 [ADR 0017](../adr/0017-public-signup-service.md)。

## 已定范围与约束

这些结论已经拍板，相关 ADR 记着理由。**别重新纠结，也别在此基础上擅自改动**：

| 约束 | 出处 |
|------|------|
| 练习语义优先于考试语义 | [ADR 0001](../adr/0001-practice-not-exam-semantics.md) |
| 答题进度直接落 MySQL，不引 Redis | [ADR 0002](../adr/0002-answer-progress-in-mysql.md) |
| 题目归属用「公开题 / 我的题目」表达 | [ADR 0003](../adr/0003-public-questions-by-empty-owner.md) |
| 多选题必须全选对才得分，无部分分 | [ADR 0004](../adr/0004-multi-choice-all-or-nothing.md) |
| 导入跳过错误行，成功的照常入库 | [ADR 0005](../adr/0005-import-skips-bad-rows.md) |
| JSON 导入契约是裸数组 | [ADR 0006](../adr/0006-bare-array-import-contract.md) |
| 运行形态与数据挂载方式 | [ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md) |
| 正确答案存题目上，不存选项上 | [ADR 0008](../adr/0008-correct-answer-on-question.md) |
| 构建镜像与环境依赖源的处理 | [ADR 0009](../adr/0009-glibc-build-image-and-dockerignore.md)、[ADR 0010](../adr/0010-china-mirrors-for-build-deps.md) |
| 密钥走 `.env`，本地默认值刻意保留 | [ADR 0011](../adr/0011-env-file-with-local-defaults.md) |
| 健康检查探哪些端点 | [ADR 0012](../adr/0012-healthcheck-probes-api-docs.md) |
| ~~CI 只做门禁，不做自动部署~~ → **改为 CI 构建镜像推 ACR、推 tag 即自动部署** | [ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md)（取代 [ADR 0013](../adr/0013-github-actions-gate-no-auto-deploy.md)） |
| 跑在阿里云 ECS；本机只剩开发形态 | [ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md) |
| 对公网开放、任何人注册即可使用 | [ADR 0017](../adr/0017-public-signup-service.md) |
| 版本号以 `git tag` 为准，只经构建参数注入 | [ADR 0015](../adr/0015-version-number-governance.md) |
| 移动端 / 小程序只是新增客户端形态，不新增业务能力；建设类功能留在 PC | [ADR 0019](../adr/0019-mobile-clients-with-uniapp.md) |

## 术语与决策入口

- **词指什么**：见 [`CONTEXT.md`](../../CONTEXT.md)，那里是纯术语表，不含实现
- **为什么这么定**：见 [`docs/adr/`](../adr/)——结论卡片，写只追加
- **当时考虑过什么**：见 [`docs/decisions/`](../decisions/)——过程日志，含被放弃的选项
- **还有什么没定**：见 [`docs/todo/`](../todo/)——未决事项的唯一入口
