# 变更日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。

## 关于版本号（先读这段）

**本仓库目前没有任何 git tag**，下面各版本段是按提交历史回溯出来的「叙事版本」，不是可以 `git checkout` 的 tag。首个正式 tag 还没打。

另外，项目里现在有三处「版本」互不同源，知道它们不一致比假装一致更重要：

| 出处 | 写的版本 | 说明 |
|------|----------|------|
| 根目录 `README.md` 标题 | v1 | 产品代次，指「选择题那一版」 |
| 两个 compose 文件里的镜像 tag | 固定值（见 `docker-compose.prod.yml`） | 镜像标识，**不随代码变**，所以无法靠它回滚 |
| `quizzy-web/package.json` | 另一个固定值 | 前端包版本，与后端无关 |

统一方案未定，见 [TODO/2026-09-20-TODO-镜像分发.md](./TODO/2026-09-20-TODO-镜像分发.md)。

---

## [Unreleased]

新变更先堆在这里；打 tag 时整段移入新版本段并改名。

---

## [1.0.0] - 2026-09-25（叙事版本，未打 tag）

覆盖 2026-09-17 至 2026-09-25 共 15 个提交，从 v1 首个提交到 CI 门禁落地。

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
- 打第一个 tag 时要做三件事：把 `[Unreleased]` 固化成版本段、决定版本号并与上表三处对齐、把下面的比较链接换成 tag 比较链接。
- 许可证见 [LICENSE](./LICENSE)。

<!-- 版本号确定后，把下面两行换成 tag 比较链接 -->
[Unreleased]: https://github.com/hhh666hhh666/quizzy/compare/3b77ec8...HEAD
[1.0.0]: https://github.com/hhh666hhh666/quizzy/compare/ae273cb...3b77ec8
