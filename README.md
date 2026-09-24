# Quizzy 答题程序 v1

[![CI](https://github.com/hhh666hhh666/quizzy/actions/workflows/ci.yml/badge.svg)](https://github.com/hhh666hhh666/quizzy/actions/workflows/ci.yml)

个人自学刷题工具，v1 只支持**选择题**（单选 / 多选 / 判断）。练习语义：逐题作答、即时判分、马上看解析，答错的题进错题本，连续答对 3 次自动移出。

完整设计见 [DESIGN.md](./DESIGN.md)。

| 想看什么 | 去哪 |
|----------|------|
| 这个词在代码里到底指什么 | [CONTEXT.md](./CONTEXT.md)（术语表，纯词汇） |
| 为什么当初这么定 | [docs/adr/](./docs/adr/)（13 条：练习语义、进度落库、公开题、判分规则、导入容错、JSON 契约、容器编排、答案存储、构建镜像、依赖源、环境变量、健康检查、CI 只做门禁） |
| 整体设计与接口清单 | [DESIGN.md](./DESIGN.md) |

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Spring Boot 3.5 · Java 21 · MyBatis-Plus 3.5 · MySQL 8.4 · Flyway · jjwt · EasyExcel · MapStruct · springdoc-openapi |
| 前端 | Vue 3 · Vite 6 · TypeScript · Pinia · Element Plus · markdown-it + highlight.js |
| 运行 | Docker Compose（dev 只起数据库，prod 全容器） |

## 运行配置（.env）

两个 compose 文件都从项目根目录的 `.env` 读取变量，先准备一份：

```bash
cp .env.example .env
# 至少把 QUIZZY_JWT_SECRET 换成随机值：
# python -c "import secrets;print(secrets.token_hex(32))"
```

`.env` 已加进 `.gitignore`，不会入库。可调的变量：

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码，默认 `123456` |
| `MYSQL_DATABASE` | 库名，默认 `quizzy` |
| `MYSQL_PORT` | 宿主机映射端口，默认 `3306` |
| `MYSQL_DATA_DIR` | **数据落盘路径**，默认 `E:/develop/docker/mysql8/var/lib/mysql` |
| `QUIZZY_JWT_SECRET` | JWT 签名密钥，务必替换；**prod 模式下缺失会直接启动失败** |
| `QUIZZY_JWT_EXPIRE_DAYS` | 登录有效期，默认 7 天 |

dev 与 prod **共用同一个 `MYSQL_DATA_DIR`**，切换运行方式数据不会丢。

## 快速开始（开发模式）

```bash
# 1. 启动 MySQL 8.4（宿主机 3306，自动建库 quizzy）
docker compose -f docker-compose.dev.yml up -d

# 2. 启动后端（8080），Flyway 会自动建表并灌入种子题库
cd quizzy-server && mvn spring-boot:run

# 3. 启动前端（5173，/api 代理到 8080）
cd quizzy-web && npm install && npm run dev
```

打开 http://localhost:5173 注册一个账号即可开始。接口文档： http://localhost:8080/swagger-ui.html

> 若本机已有 MySQL 占用 3306，先停掉它，或改 `.env` 里的 `MYSQL_PORT`。

## 全容器运行（生产 / 演示）

```bash
# dev 只起了 mysql，容器名与 prod 冲突，先停掉（数据留在 E 盘，不会被删）
docker compose -f docker-compose.dev.yml down

docker compose -f docker-compose.prod.yml up -d --build
```

日常改完代码要重建时改用 `bash scripts/deploy.sh`，别再手敲上面这条，见下一节。

前端由 nginx 托管在 http://localhost（80），`/api` 反代到后端容器；MySQL 数据落在 `MYSQL_DATA_DIR` 指向的目录。

首次构建要分别拉取 Maven 与 npm 的全量依赖（已配阿里云 / npmmirror 镜像），大概几分钟；之后只改代码的话是增量编译。

> 后端 8080 也直接映射到了宿主机，方便开 swagger。纯内网自用没问题，不需要的话把 `docker-compose.prod.yml` 里 server 的 `ports` 段删掉即可。

### 排障：`--build` 时报拉不到基础镜像

如果看到 `failed to fetch oauth token ... auth.docker.io ... Bad Gateway`，说明本机连不上 Docker Hub。Maven 与 npm 依赖已经走国内源，但**基础镜像本身仍来自 Docker Hub**，需要单独处理。两种解法：

```bash
# 1. 一次性：通过镜像站拉取后打回官方标签（不改全局配置，不用重启 Docker）
for img in maven:3.9.9-eclipse-temurin-21 eclipse-temurin:21-jre node:22-slim nginx:alpine; do
  docker pull docker.m.daocloud.io/library/$img
  docker tag  docker.m.daocloud.io/library/$img $img
done

# 2. 一劳永逸：Docker Desktop → Settings → Docker Engine，加入后 Apply & Restart
#    "registry-mirrors": ["https://docker.m.daocloud.io"]
```

### 排障：dev 与 prod 的容器名冲突

两个 compose 里的 mysql 容器名都是 `quizzy-mysql`，不能同时运行。切换时先 `docker compose -f docker-compose.dev.yml down`——只删容器，数据在 `MYSQL_DATA_DIR` 里，不会丢。

## CI 与部署

**CI 只做门禁，不会把任何东西部署到任何机器上。** push 或 PR 到 master 时，GitHub 托管 runner 上跑四条并行检查：

| job | 检查什么 |
|------|----------|
| 后端 | `mvn test`（7 个单测，不需要数据库） |
| 前端 | `npm ci` + `npm run typecheck` + `npm run build` |
| 镜像 · 后端 | 干净 Linux 上 `docker build ./quizzy-server` |
| 镜像 · 前端 | 干净 Linux 上 `docker build ./quizzy-web` |

镜像那两条是**冒烟**：只在乎「能不能从零构建出来」，不推送到任何 registry。这是最有价值的一项——Dockerfile 的坑（构建镜像平台、宿主机 `node_modules` 污染容器）只有在这种环境下才会暴露，本机因为缓存命中永远发现不了。

为什么不做自动部署：部署目标只有本机 Docker Desktop，而本仓库是 **public** 的，把 self-hosted runner 装在开发机上等于让任何能开 PR 的人在那台机器上执行代码。详见 ADR 0013。

> **CI 绿了不代表本机已更新。** 上线这一步要自己跑：
>
> ```bash
> bash scripts/deploy.sh          # 前后端都重建
> bash scripts/deploy.sh server   # 只重建后端
> bash scripts/deploy.sh web      # 只重建前端
> ```
>
> 脚本会先校验 `.env` 与 `QUIZZY_JWT_SECRET`，然后重建指定服务、等健康检查通过、打印最终状态。
> **mysql 永不参与重建**，数据在宿主机 `MYSQL_DATA_DIR` 里。改完前端记得硬刷新（nginx 没设 Cache-Control）。

两个 docker job 会从 Docker Hub 拉基础镜像，匿名限额是 100 次 / 6 小时**按共享出口 IP 计**，GitHub runner 共用出口 IP 容易撞顶。可选的优化：在仓库 Settings → Secrets → Actions 里配 `DOCKERHUB_USERNAME` 和 `DOCKERHUB_TOKEN`（Docker Hub → Account Settings → Personal Access Tokens），限额翻倍且改为按账号计。**不配也能跑**，登录步骤会自动跳过，只是有匿名限流风险。

## 功能

- **题库**：题目增删改查，按关键词 / 题型 / 难度 / 分类 / 范围（全部 / 我的 / 公开）筛选
- **导入导出**：Excel（主）与 JSON（辅）双向导入导出，导入时**跳过错误行**并给出逐行错误报告
- **试卷**：固定卷（手动选题）与规则卷（按分类 / 题型 / 难度 / 题量抽题，支持排除近期已做）
- **答题**：逐题作答，提交后即时判分并展示正确答案与解析；可回退改答案，进度落库可断点续答
- **错题本**：答错自动入本，连续答对 3 次自动移出，支持手动移出与一键错题重练
- **记录**：历史会话列表，未完成的可以继续作答

## 判分规则

| 题型 | 规则 |
|------|------|
| 单选 / 判断 | 选中项与正确答案一致才得分 |
| 多选 | 必须与正确答案集合**完全一致**（顺序无关）才得分，少选、多选、错选均不得分 |

每题分值在建题时指定（默认 1 分），不做部分分、不做倒扣分。跳过未答的题不计入正确率分母，结算时会提示未作答数量。

## 导入模板

列顺序固定为：

```text
题型 | 题干 | 选项A | 选项B | 选项C | 选项D | 选项E | 选项F | 答案 | 解析 | 难度 | 分值 | 分类 | 标签
```

- 题型：`single` / `multi` / `judge`（也认 `单选` / `多选` / `判断题`）
- 答案：单个字母，多选用逗号分隔（`A,C`）
- 难度：`easy` / `medium` / `hard`；分类与标签不存在时自动创建
- 题干、选项、解析支持 Markdown

JSON 结构见 `QuestionImportDTO`，可先从页面导出一份 JSON 作为样例。也可以在题库页点「下载导入模板」拿到标准 Excel 模板。

## 数据模型

11 张表：`user`、`category`、`tag`、`question`、`question_option`、`question_tag`、`question_stat`、
`paper`、`paper_question`、`quiz_session`、`quiz_answer`。
题目 `owner_id` 为 `NULL` 表示公开题（所有人可读、只读），否则为创建者私有。
完整关系见 DESIGN.md 第 3 节。

## 已知取舍

- 历史作答**不做题目快照**：题目被修改后，历史记录里显示的解析会跟着变（题量与规模下可接受，需要时可在 `quiz_answer` 冗余题干字段）
- 种子题库 34 道（Java / 并发 / JVM / Spring / MySQL / Redis / 网络），更多题目建议用 Excel 批量导入
- 不引 Redis，答题进度直接落 MySQL
- 单元测试只覆盖判分策略与导入校验两处最容易出静默错误的逻辑
- 前端**没有单测也没有 lint**（`package.json` 里不存在这两个 script），CI 对前端的门禁只有类型检查与构建
