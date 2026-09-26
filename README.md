# Quizzy 答题程序 v1

[![CI](https://github.com/hhh666hhh666/quizzy/actions/workflows/ci.yml/badge.svg)](https://github.com/hhh666hhh666/quizzy/actions/workflows/ci.yml)

个人自学刷题工具，v1 只支持**选择题**（单选 / 多选 / 判断）。练习语义：逐题作答、即时判分、马上看解析，答错的题进错题本，连续答对若干次自动移出。

| 想看什么 | 去哪 |
|----------|------|
| 全部文档的索引 | [docs/README.md](./docs/README.md) |
| 这个词在代码里到底指什么 | [CONTEXT.md](./CONTEXT.md)（术语表，纯词汇） |
| 为什么当初这么定 | [docs/adr/](./docs/adr/)（结论卡片，编号递增） |
| v1 范围与明确不做 | [需求范围](./docs/requirements/scope.md) |
| 设计与业务规则 | [docs/design/](./docs/design/)（数据模型 / 判分 / 导入导出 / API / 前端） |
| 部署、配置、排障 | [docs/operations/](./docs/operations/) |

## 功能

- **题库**：题目增删改查，按关键词 / 题型 / 难度 / 分类 / 归属范围筛选
- **导入导出**：Excel（主）与 JSON（辅）双向，导入跳过错误行并给出逐行错误报告
- **试卷**：固定卷（手动选题）与规则卷（按条件现场抽题，支持排除近期已做）
- **答题**：逐题作答、即时判分与解析，可回退改答案，进度落库因而可断点续答
- **错题本**：答错自动入本，练熟自动移出，支持手动移出与一键错题重练
- **记录**：历史会话列表，未完成的可以继续作答

判分与错题的规则细节见 [判分与业务规则](./docs/design/判分与业务规则.md)，导入文件的格式见 [导入导出](./docs/design/导入导出.md)。

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Spring Boot 3.5 · Java 21 · MyBatis-Plus · MySQL 8.4 · Flyway · springdoc-openapi |
| 前端 | Vue 3 · Vite · TypeScript · Pinia · Element Plus |
| 运行 | Docker Compose（dev 只起数据库，prod 全容器） |

确切版本以 `pom.xml`、`package.json` 与两个 `Dockerfile` 为准。

## 快速开始（开发模式）

```bash
# 1. 启动 MySQL（应用启动时 Flyway 自动建表并灌入种子题库）
docker compose -f docker-compose.dev.yml up -d

# 2. 启动后端
cd quizzy-server && mvn spring-boot:run

# 3. 启动前端（/api 代理到后端）
cd quizzy-web && npm install && npm run dev
```

打开 http://localhost:5173 注册一个账号即可开始；接口文档在 http://localhost:8080/swagger-ui.html 。

> 若本机已有 MySQL 占用了默认端口，先停掉它，或改 `.env` 里的 `MYSQL_PORT`。

## 全容器运行（生产 / 演示）

```bash
# dev 与 prod 的 mysql 容器同名，先停掉 dev（数据在宿主机目录，不会被删）
docker compose -f docker-compose.dev.yml down

docker compose -f docker-compose.prod.yml up -d --build
```

前端由 nginx 托管在 http://localhost 。**日常改完代码要更新容器，用 `bash scripts/deploy.sh`，别手敲上面这条**——见 [部署](./docs/operations/deployment.md)。

## 配置

两个 compose 都从项目根目录的 `.env` 读取变量，先 `cp .env.example .env`。完整变量清单与默认值见 [`.env.example`](./.env.example)。三个必须留意的值：

| 变量 | 为什么要注意 |
|------|--------------|
| `QUIZZY_JWT_SECRET` | 必须是随机值——仓库公开后那串默认值已不能再用于签名；prod 模式下缺失会直接启动失败 |
| `MYSQL_DATA_DIR` | 数据落盘位置，dev 与 prod 共用同一份；**改它等于换库，不是换配置** |
| `MYSQL_ROOT_PASSWORD` | 现有数据卷是用默认值初始化的，改它需要重建或改密 |

配置从哪里来、改了要重启什么，见 [配置说明](./docs/operations/configuration.md)。

## 排障与运维

容器起不来 / 页面打不开 / 连不上库 → [应急预案](./docs/operations/runbook.md)；上线与回滚 → [部署](./docs/operations/deployment.md)；备份与恢复（尚未演练）→ [备份](./docs/operations/backup.md)。

## CI 与部署

**CI 只做质量门禁**，不会把任何产物投放到任何机器上：后端单测、前端类型检查与构建、两个镜像在干净 Linux 上冒烟构建。

所以 **CI 绿了不等于本机已更新**——上线要自己跑 `bash scripts/deploy.sh`。理由见 [ADR 0013](./docs/adr/0013-github-actions-gate-no-auto-deploy.md)，测试范围与盲区见 [测试说明](./docs/testing.md)。

本项目采用 [MIT](./LICENSE) 许可证。
