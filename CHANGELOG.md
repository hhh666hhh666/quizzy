# 变更日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。

## 关于版本号（先读这段）

**版本号以 `git tag` 为准**，遵循 SemVer（`MAJOR.MINOR.PATCH`）。本仓库的 `tag` 就是真相源，下面每个 `## [x.y.z]` 段都对应一个真实存在、可以 `git checkout` 的 tag。

需要区分的四件事：

| 出处 | 是什么 | 说明 |
|------|--------|------|
| **`git tag`**（如 `v1.0.0`） | **版本号** | 唯一真相源，与下面每个版本段一一对应 |
| `README.md` 与 `docs/requirements/scope.md` 里的 **v1** | **产品代次** | 指「只做选择题的这一版」，不是版本号 |
| compose 里的镜像 tag | 镜像标识 | 写死的固定值，**不随代码变**，所以无法靠它回滚 |
| `pom.xml` / `package.json` 的包版本 | 各自的包版本 | Maven 坐标与 npm 包版本，与发布版本无关 |

镜像 tag 与包版本的统一管理方案未定，见 [docs/todo/2026-09-20-TODO-镜像分发.md](./docs/todo/2026-09-20-TODO-镜像分发.md)。

---

## [Unreleased]

新变更先堆在这里；打 tag 时整段移入新版本段并改名。

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
- 下一个版本发布时：把 `[Unreleased]` 里的内容固化成新的版本段 → 打附注 tag（`git tag -a vX.Y.Z -m "vX.Y.Z"`）→ **单独推送 tag**（`git push origin vX.Y.Z`，它不随普通 push 走）→ 更新底部两个比较链接。
- 许可证见 [LICENSE](./LICENSE)。

[Unreleased]: https://github.com/hhh666hhh666/quizzy/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/hhh666hhh666/quizzy/compare/ae273cb...v1.0.0
