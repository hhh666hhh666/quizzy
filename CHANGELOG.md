# 变更日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。

## 关于版本号（先读这段）

**版本号以 `git tag` 为准**，遵循 SemVer（`MAJOR.MINOR.PATCH`）。本仓库的 `tag` 就是真相源，下面每个 `## [x.y.z]` 段都对应一个真实存在、可以 `git checkout` 的 tag。

需要区分三件事：

| 出处 | 是什么 | 说明 |
|------|--------|------|
| **`git tag`**（如 `v1.1.0`） | **版本号** | 唯一真相源，与下面每个版本段一一对应 |
| compose 里的 `image:` | 镜像标识 | 只写 `${APP_VERSION}` 变量，**不再手写版本号**；镜像由 CI 推到 ACR，服务器按 tag 拉取 |
| `pom.xml` / `package.json` 的包版本 | 各自的包版本 | Maven 坐标与 npm 包版本，**与发布版本无关**（`pom.xml` 还刻意带 `-SNAPSHOT`） |

打 tag 时 `scripts/check-version.sh`（`release.yml` 的「版本」job）会比对**三处手写点**，
并断言 compose 用的是变量而不是写死的 tag。
设计与取舍见 [ADR 0015](./docs/adr/0015-version-number-governance.md)（含 Amendment 1、2）；镜像分发已定案，
见 [docs/todo/2026-09-20-TODO-镜像分发.md](./docs/todo/2026-09-20-TODO-镜像分发.md)。

> 「产品代次 v1」这个说法已于 2026-10-01 取消——它和版本号长得太像，要表达那个意思就直接写范围。
> 下面 `[1.0.0]` 段里出现的「答题程序 v1」是当时的历史措辞，保留不改。

---

## [Unreleased]

新变更先堆在这里；打 tag 时整段移入新版本段并改名。

### Added

- **接口层（真 Spring + 真 MySQL）的机制就位**：`pom.xml` 声明 `maven-failsafe-plugin`、接口与集成测试
  定为 `*IT` 命名，于是 `mvn test` **依旧只跑纯单元测试、不需要 Docker**（`mvn verify` 才跑接口层）。
  库用 Testcontainers 起真 MySQL——不用 H2，因为迁移脚本是 MySQL 方言
  （[ADR 0020](./docs/adr/0020-layered-test-system.md) 及其 Amendment 2）。先用 auth 模块打通：
  统一信封、鉴权（**鉴权失败的 HTTP 状态仍是 200、401 落在信封里**这条反直觉约定也钉住了）、
  重复用户名、密码错、校验错误各一条。
- **接口层铺满 6 个模块，机械守卫打开**：auth / category / paper / question / quiz / wrongbook
  各自有了接口测试（契约 + **数据边界**），并加了 `scripts/check-module-tests.sh`
  挂在 CI 上断言「每个带 Controller 的模块都配了测试类」（[ADR 0021](./docs/adr/0021-test-sync-discipline.md) 及其 Amendment 1）。
  守卫本身**正反两向**验过：正向 6 个模块全 ok，反向临时造一个无测试的模块必须报 MISSING。
- **端到端收掉 P2 最后三条尾巴**：**错题本主链路**（答错进本 → 手动移出 → 不在本里）、
  **多选题判分**（少选不得分、全选才得分，ADR 0004——单元层与接口层都验过但**没走过 UI**）、
  **裁切点名**（此前 `expectNotClipped` 是死代码，现在点名了登录卡片与侧边菜单）。
  顺带把「造题 / 组卷 / 从试卷发起作答」抽成公共助手，并让建题组卷那条用例**真正断言
  「刚建的那道题被选进了卷」**（原来只是随便勾第一道）。
- **端到端再补两条**：**建题 → 组固定卷 → 用这张卷作答**（跨题目 / 试卷 / 答题会话三个模块，
  只有端到端能串起来），以及**断点续答**（答完一题刷新，位置继续在未作答的题上——这条曾经是坏的）。
- **端到端补上核心链路与排版几何断言**：①一条主链路（注册 → 快速练习 → 答题 →
  **判分反馈即时显示** → 结算 → 结果页四项统计），②几何断言（页面不横向溢出、主内容区不越界，
  1280 / 768 两个宽度，覆盖登录页与主框架内五个页面）。
  **视口刻意不含 375**：按 [ADR 0019](./docs/adr/0019-mobile-clients-with-uniapp.md) 移动端另有
  quizzy-mobile，quizzy-web 实测 PC 布局最小约 501px；裁切（内容被裁掉）也**不做通用断言**——
  表格 / 代码块本来就可滚动，必然误报，改成了调用方点名的 `expectNotClipped`。
- **CI 新增 `api` job**（`mvn verify`；Testcontainers 自己起库，所以不需要 `services`）。
- **新增移动端工程 `quizzy-mobile`**（uni-app · Vue3 · TypeScript · Pinia · wot-design-uni），
  本轮只启用 H5 编译目标，落地「登录/注册 → 快速练习 → 答题 → 结果」一条主线。
  架构见 [ADR 0019](./docs/adr/0019-mobile-clients-with-uniapp.md)（PC 端不动、移动端只做「消费」）。
  小程序编译目标、`/m/` 部署产物与错题本/记录页留待后续。
- **CI 新增 `mobile` job**：类型检查 + `build:h5` + 一条 `diff` 守卫（移动端的 `types` 必须与 PC 端逐字一致）。
- **建立分层测试系统**：三层——单元（已有）、接口与服务集成（真 Spring + 真 MySQL，Testcontainers）、
  端到端（Playwright，**只在 CI 的一次性环境**跑）。两个前端工程的依赖树**不动**，端到端隔离在独立工程 `e2e/` 里。
  排版分三类：**几何事实**（溢出 / 越界 / 裁切）由端到端层的断言覆盖并进 CI，
  **审美与层级**靠 AI 探索粗筛 + 人眼，**视觉回归截图比对不做**。
  见 [ADR 0020](./docs/adr/0020-layered-test-system.md)（含 Amendment 1）与 [《测试系统说明》](./docs/testing/系统说明.md)，
  落地的分阶段计划见 [docs/todo/2026-10-04-TODO-测试系统落地.md](./docs/todo/2026-10-04-TODO-测试系统落地.md)。
- **端到端层已落地（P0）**：新增工程 [e2e/](./e2e/README.md)（Playwright，独立 `package.json`），
  并给 `ci.yml` 加了 `e2e` job——**这是后端第一次在流水线里被真正启动**（此前 `mvn test` 不拉 Spring、
  镜像 job 只 `docker build`）。一次性环境是 `mysql:8` service + 后端进程 + 前端 preview，跑完随 runner 销毁；
  测试配置**直接注入 job 的 env、不建 `.env` 文件**，顺带解开了「不敢 `compose up`」那个结。
- **登录页的输入框与注册按钮加了 `data-testid`**：登录与注册两个 Tab 同时挂在 DOM 上，
  「用户名」「密码」这类文案各重复一次，按文案定位会撞名——只给这类元素加，其余仍用语义定位。
- **新增 AI 探索测试任务书** `docs/testing/agent-exploration.md`：由人手动触发、**产出报告而非断言**，
  不进 CI（探索测试不可重复，不是测试）。任务书**自包含、平台无关**（写入能力自检，缺能力必须声明），
  报告写到 `.workbuddy/exploration/`（已被 gitignore，**不入库**）。
- **新增测试同步纪律**：改动 → 补哪层测试由一张映射表决定，测试与功能进**同一个 PR**；
  另计划一条 CI 机械守卫（每个 `module/*/controller` 至少有一个测试类，**尚未落地**，见 P1），
  见 [ADR 0021](./docs/adr/0021-test-sync-discipline.md)。

### Fixed

- **改正一处由荧自己造成的错误结论，并补上此前完全缺失的断言**：接口测试的注释与 todo 里曾写
  「抽题规则里没有题量」——**那是错的**。`PaperRuleDTO` 有 `count`（默认 20、夹在 1–200），
  `PaperService#selectQuestionsByRule` 会 `ORDER BY RAND() LIMIT count`：题量一直生效，语义是**上限**。
  误判来自荧用一条紧凑正则导出 DTO 字段，漏掉了带初始化值的 `private Integer count = 20;`。
  顺带补上**此前没有任何用例覆盖**的语义：传 `count: 3` 必须正好抽 3 道、传 `count: 0` 夹到 1 道
  ——在此之前 34 个接口用例没有一个碰得到它，谁删掉 `LIMIT count` 都会全绿通过。
- **两端答题页提交后都不再显示判分反馈**：`onSubmit()` 里 `load()` 会把当前题挪到「第一道未答题」，
  并在 `syncPicked()` 里把 `feedback` 清成 null——PC 端紧跟着的 `feedback.value = feedback.value`
  是自赋值，反馈永远不显示；移动端首版则是「题干是下一题、解析是上一题」，点「下一题」还会跳过一题。
  改为**记住当前题号、刷新会话后把位置与反馈放回去**，两端行为已对齐。

### Changed

- **`docs/testing.md` 迁为 `docs/testing/README.md`**：`docs/` 下新增 `testing/` 主题目录，
  按「现状 / 设计意图 / 探索任务书」拆成三份文件，全仓引用同步修正。
- **MySQL 从「不发布任何端口」改为**只绑宿主回环（`127.0.0.1:3306:3306`），
  好让本机能经 SSH 隧道直连——IDEA 的数据库工具与临时排障都靠这个稳定落点。
  **公网仍然够不着**：已实测宿主 `ss` 只显示回环地址、从公网探 3306 拒连。
  取舍见 [ADR 0016 Amendment 2](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)。
- **版本号手写点从两处回到三处**：新增 `quizzy-mobile/package.json`，
  `check-version.sh` 相应加两条校验（[ADR 0015 Amendment 2](./docs/adr/0015-version-number-governance.md)）。

---

## [1.2.0] - 2026-10-03

「搬到云上」这一版。prod 从本机 Docker Desktop 迁到阿里云轻量服务器，交付链路改成
「推 tag → CI 构建推 ACR → SSH 自动部署」，并顺手补掉了几处一直缺的安全与可靠性设置。

### Changed

- **prod 从本机搬到阿里云轻量服务器，交付链路改为「推 tag → CI 构建推 ACR → SSH 部署」**
  （[ADR 0016](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)，取代 ADR 0013）：
  - `docker-compose.prod.yml` 变为**云上编排**：镜像来自 ACR、不再有 `build:`、web 只绑 `127.0.0.1:8081`、
    MySQL 与后端都不发布端口、按 2C2G 用 `JAVA_TOOL_OPTIONS` 钉死 JVM 内存、三个服务统一加日志上限
  - 新增 `.github/workflows/release.yml`：`version` → `flyway-verify` → `build-push` → `deploy` 四个 job 串行；
    tag 触发从 `ci.yml` 移除，tag 的处置权统一收进它
  - 新增 `scripts/server-deploy.sh` / `server-rollback.sh`（在服务器上跑；部署前强制备份、mysql 永不重建）；
    **删除本机版 `scripts/deploy.sh`**——本机从此只剩开发形态
  - 新增 `deploy/`：宿主 nginx 站点模板与云端 `.env` 模板
- **HTTPS 与对外入口交给宿主上既有的 nginx（宝塔）**，不再自建 `acme.sh` + DNS-01
  （[ADR 0016 Amendment 1](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)）
- **版本号手写点从三处减到两处**：compose 的 `image:` 改用 `${APP_VERSION}` 变量，
  `check-version.sh` 相应改成结构断言（[ADR 0015 Amendment 1](./docs/adr/0015-version-number-governance.md)）

### Added

- **迁移首次被 CI 验证**：新增的 `flyway-verify` job 在一次性 `mysql:8` 上按顺序重放 `db/migration`，
  并断言文件名合 Flyway 约定、版本号严格递增——此前 `mvn test` 完全不碰数据库

### Fixed

- **「改完前端必须硬刷新」这个老毛病消掉了**：容器 nginx 原先没有 `Cache-Control`，
  现在 `index.html` 明确不缓存、带内容 hash 的 `assets/` 长缓存
- **CORS 不再对任意来源开放**：原来是 `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 并存，
  现在默认**完全不注册 CORS**（前端与 `/api` 同源本就不需要），需要时用 `QUIZZY_CORS_ALLOWED_ORIGINS` 显式开
- **后端不再直接对公网暴露**：`8080` 端口映射删除，只经容器 nginx 暴露 `/api/`（Swagger 因此也不再对外）

---

## [1.1.0] - 2026-10-01

「关于」弹窗与版本号治理。这一版把版本号从「四个互不同步的值」收敛成「`git tag` 一个真相源 +
三处手写镜像 + 一道门禁」，并给它造了一条注入前端产物的通道。

### Added

- **「关于」弹窗**：顶栏可查当前版本号与构建信息。版本主行取自 `git tag`，副行只在 `HEAD` 领先 tag 时出现
- **版本号一致性门禁**：`scripts/check-version.sh` + CI 的「版本 · tag 与三处对齐」job，
  推 tag 时校验 `package.json` / `pom.xml` / compose 镜像 tag 与 tag 是否一致（[ADR 0015](./docs/adr/0015-version-number-governance.md)）

### Changed

- **版本号改由构建参数注入前后端产物**（此前前端根本没有版本号通道，后端则是写死的值）：
  `scripts/deploy.sh` 从 `git tag` 算出后经 compose 的 `build.args` 传入。swagger UI 上显示的版本
  也从原先写死的「产品代次」改为真实版本号
- **镜像 tag 跟随版本号**（`quizzy-server:1.1.0` / `quizzy-web:1.1.0`）。于是退回上一个已发版本
  可以复用本地镜像、不必重新构建；代价是同一版本号内的多次构建仍互相覆盖
- **取消「产品代次 v1」这个说法**：`README.md` 与 `docs/requirements/scope.md` 改为直白的范围描述，
  `CONTEXT.md` 收录「版本号」一词并注明该说法已取消

---

## [1.0.0] - 2026-09-26

第一个正式版本。从「答题程序 v1」首个提交一路到文档体系与 CI 门禁落地，`ae273cb` → `v1.0.0`。

### Added

- **答题程序 v1**：选择题（单选 / 多选 / 判断）的题库管理、固定卷与规则卷、逐题作答与即时判分、错题本、历史会话（[ae273cb](https://github.com/hhh666hhh666/quizzy/commit/ae273cb)）
- **题库列表页支持查看题目详情**（[9661bf0](https://github.com/hhh666hhh666/quizzy/commit/9661bf0)）
- **全容器 Docker 化**：dev / prod 双 compose，MySQL 数据绑定挂载到宿主机（[7c5a996](https://github.com/hhh666hhh666/quizzy/commit/7c5a996)，ADR 0007）
- **后端健康检查与数据库备份脚本**（[5008ace](https://github.com/hhh666hhh666/quizzy/commit/5008ace)，ADR 0012）
- **CI 门禁**：后端单测、前端类型检查与构建、两个镜像在干净 Linux 上冒烟构建（[d513539](https://github.com/hhh666hhh666/quizzy/commit/d513539)，ADR 0013）

### Changed

- 镜像构建改用 glibc 基础镜像、前后端补 `.dockerignore`、构建期依赖走国内镜像源（ADR 0009、ADR 0010）
- CI 用到的 actions 升到当前大版本（[8bb2c1b](https://github.com/hhh666hhh666/quizzy/commit/8bb2c1b)）
- 出题中间产物 `.qbgen/` 不再入库（[159c023](https://github.com/hhh666hhh666/quizzy/commit/159c023)）

### Security

- **公开的 JWT 默认密钥不再用于签名**（[2984719](https://github.com/hhh666hhh666/quizzy/commit/2984719)，[ADR 0011 Amendment 1](./docs/adr/0011-env-file-with-local-defaults.md)）
  仓库转为公开后，`application.yml` 里那串默认密钥成为全网可知的字符串，可被用来签发任意 `userId` 的 token。
  现在：默认值仍保留（宿主机裸跑不需要 `.env`），但一旦检测到用它，改为每次启动随机生成并打 WARN；prod compose 则直接把密钥设为必填。
  ⚠️ **任何在这个修复之前用默认密钥部署的实例，都必须轮换 `QUIZZY_JWT_SECRET` 并让所有用户重新登录。** 轮换方式见 [docs/operations/configuration.md](./docs/operations/configuration.md)。
  ⚠️ 仍未解决：`MYSQL_ROOT_PASSWORD` 的默认值同样已公开，见 ADR 0011 末尾。

### Documentation

- 术语表 `CONTEXT.md` 与 ADR 0001–0008（[af1f653](https://github.com/hhh666hhh666/quizzy/commit/af1f653)）
- ADR 0009–0012（[df31345](https://github.com/hhh666hhh666/quizzy/commit/df31345)）
- `docs/decisions/` 决策过程日志（[1541cd4](https://github.com/hhh666hhh666/quizzy/commit/1541cd4)）
- MySQL 题库笔记（[d1d9715](https://github.com/hhh666hhh666/quizzy/commit/d1d9715)）
- CI/CD 那轮的决策日志归档（[3b77ec8](https://github.com/hhh666hhh666/quizzy/commit/3b77ec8)）

---

## 维护约定

- 提交信息沿用 Conventional Commits（`feat:` / `fix:` / `docs:` / `chore:`），`git log` 就是归类依据。
- 变更记入 `[Unreleased]`；**用户可感知的破坏性变更必须在版本段里单独说明迁移方式**。
- 下一个版本发布时：把 `[Unreleased]` 里的内容固化成新的版本段 → 打附注 tag（`git tag -a vX.Y.Z -m "vX.Y.Z"`）→ **单独推送 tag**（`git push origin vX.Y.Z`，它不随普通 push 走）→ 更新底部两个比较链接 → 跑一遍 `bash scripts/check-version.sh vX.Y.Z` 应当全绿。
- 许可证见 [LICENSE](./LICENSE)。

[Unreleased]: https://github.com/hhh666hhh666/quizzy/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hhh666hhh666/quizzy/compare/ae273cb...v1.0.0
