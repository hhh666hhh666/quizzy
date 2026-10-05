# 部署

## 分工：CI 管「能不能上线」，`release.yml` 管「上线」

| 流水线 | 触发 | 干什么 | 会改动运行中的服务吗 |
|--------|------|--------|----------------------|
| [ci.yml](../../.github/workflows/ci.yml) | push 到 master / PR | 后端单测、前端类型检查与构建、两个镜像在干净 Linux 上冒烟构建、文档链接自检 | **不会** |
| [release.yml](../../.github/workflows/release.yml) | 推 `v*` tag | 版本门禁 → 迁移重放验证 → 构建推 ACR → SSH 部署 | **会**——这是本项目唯一会改线上的一条 |

两个文件各跑各的 job 清单，本文件不复制那张表，改了会立刻过期。取舍见
[ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md)（它取代了「CI 只做门禁、上线永远由人跑脚本」的 [ADR 0013](../adr/0013-github-actions-gate-no-auto-deploy.md)）。

## 上线路径：推一个 tag

```bash
git tag v1.2.0 && git push origin v1.2.0
```

之后 `release.yml` 依次做四件事，任何一环红了就停在那里、不会继续往下推：

1. **版本门禁** —— `scripts/check-version.sh` 比对 `package.json` / `pom.xml` 与 tag，并断言 compose 用的是 `${APP_VERSION}` 变量（[ADR 0015](../adr/0015-version-number-governance.md) 及其 Amendment 1）
2. **迁移重放** —— 在一次性 `mysql:8` 上按顺序跑完 `db/migration`，并断言文件名合 Flyway 约定、版本号严格递增
3. **构建推 ACR** —— 两个镜像以 `:<版本号>`（不带 `v`）推到阿里云容器镜像服务
4. **部署** —— `scp` compose 与脚本上服务器，再执行 `scripts/server-deploy.sh`

**部署那一步做什么、顺序如何、失败怎么报**，写在 [scripts/server-deploy.sh](../../scripts/server-deploy.sh) 的头部注释与函数里——那儿是真相源。三条不该忘的性质：**mysql 永不重建**（`--no-deps`）、**`up -d` 之前先强制备份一次**、失败时打印容器日志尾部与回滚命令。

服务器上**不 clone 仓库**：每次由 CI 把 compose 与脚本送过去，保证「服务器上的编排 = 这个 tag 的编排」。

## 首次上线的一次性准备（只做一次）

前四条需要人在服务器侧或阿里云控制台操作，之后每次发版都不再需要。

1. **配 GitHub Secrets**：`ACR_USERNAME`（阿里云账号全名）、`ACR_PASSWORD`（ACR 固定密码，**不是 AccessKey**——个人版不支持 AccessKey 推送）、`DEPLOY_SSH_KEY`、`DEPLOY_SSH_KNOWN_HOSTS`（可选 `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN`，用来提升基础镜像的拉取配额）
2. **云端 `.env`**：按 [deploy/.env.cloud.example](../../deploy/.env.cloud.example) 填好，放到服务器 `/srv/quizzy/.env`（`chmod 600`）
3. **把 MySQL 交给 compose 管**：它是先期用 `docker run` 起的、**没有 compose 标签**，直接 `up -d` 会报容器名冲突。停掉并删除那个容器，让 compose 用**同一个绑定挂载目录**重建——数据在宿主机目录上，不受影响
4. **宿主 nginx 站点**：在宝塔面板建站并配反向代理到 `127.0.0.1:8081`。仓库里的 [deploy/nginx/quizzy-site.conf](../../deploy/nginx/quizzy-site.conf) 是**对照模板**，真正生效的那份归宝塔管。备案期间 `server_name` 填服务器 IP；备案通过后改填域名，并由宝塔申请 Let's Encrypt 证书

> ⚠️ 第 3 步的红线：**只 `docker rm` 容器**。永远不要 `docker compose down -v`，也永远不要删 `MYSQL_DATA_DIR` 指向的目录——那是数据本体。

## 上线前检查清单

1. CI 在 master 上是绿的（跑什么见 [../testing/README.md](../testing/README.md) 的 CI 一节）。
2. 本次改动涉及数据库结构 → 确认迁移是**向后兼容**的（加表 / 加可空列）。破坏性变更不能靠回滚兜底。
3. 迁移脚本已随本次改动进了 `db/migration`，且版本号严格递增——`release.yml` 的第二个 job 会验，但别指望它替人想清楚语义。
4. `CHANGELOG.md` 已把 `[Unreleased]` 固化成该版本段——版本门禁会验这一段的存在。
5. **节奏自检**（[ADR 0023](../adr/0023-release-cadence-by-risk.md)）：本次改动属于哪一档——无迁移（修 bug / 纯前端）可随时发；带迁移攒到一周 1～2 次、且已把迁移与代码拆开发；破坏性变更单独发一次、前后不夹别的。

## 上线后该看什么

`deploy` job 的日志里会打印容器状态与最终版本号；服务器上的权威状态是：

```bash
cd /srv/quizzy/compose/scripts
bash server-compose.sh ps
```

> 为什么绕一层脚本：compose 每次调用都会把整份文件插值一遍，而 `image:` 上有 `${APP_VERSION:?...}` 必填断言——缺了它**连 `ps` 都会退出**。包装脚本会自动从 `.current-version` 补上，顺带也处理了 `--env-file` 的路径。

期望：`quizzy-server` 的 health 是 `healthy`，`quizzy-web` 是 `Up`。不符合就转 [runbook.md](./runbook.md)。

> 前端现在带了正确的 `Cache-Control`（`index.html` 不缓存、带内容 hash 的 `assets/` 长缓存），**不再需要硬刷新**。

## 回滚

镜像按版本 tag 保留在 ACR，所以回滚**不需要重新构建**，在服务器上跑：

```bash
cd /srv/quizzy/compose/scripts
bash server-rollback.sh <要回退到的版本号>
```

它本质是把版本号交给 `server-deploy.sh` 重跑一遍，因此同样会先备份、先拉镜像、再等健康。上一个已上线版本记录在 `/srv/quizzy/compose/.current-version`，不传参数时会打印出来。

⚠️ **回退应用 ≠ 回退数据库。** ADR 0016 的纪律是「迁移只做向后兼容」，所以应用回滚到旧版本是安全的；但若某次迁移是破坏性的（删列、改类型），回滚应用并不能把数据变回来。**数据层面的问题不要靠回滚硬扛**，先看 [runbook.md](./runbook.md) 的数据一节。

## 本机不再跑 prod

`docker-compose.prod.yml` 是**云上编排**：镜像来自 ACR、没有 `build:`、web 只绑回环。本机跑不起来它，也不该跑——本机只剩开发形态（[docker-compose.dev.yml](../../docker-compose.dev.yml) + `mvn spring-boot:run` + `npm run dev`）。

原本的本机版部署脚本 `scripts/deploy.sh` 已删除（要找回历史版本用 `git show <旧提交>:scripts/deploy.sh`）。这是刻意的：目标是「从此不在本机上运行 prod」。

## 已知未决

- 应用层监控（现在只有阿里云云监控的基础告警）→ [docs/todo/2026-09-20-TODO-运行与运维.md](../todo/2026-09-20-TODO-运行与运维.md)
- 备份上传 OSS 与**恢复演练** → [docs/todo/2026-09-20-TODO-数据库备份.md](../todo/2026-09-20-TODO-数据库备份.md)
- 40G 盘上镜像会堆积，需要定期 `docker image prune` → 同上「运行与运维」那条
