# 应急预案（Runbook）

出事时按**症状**查，不按章节读。

**命令分层**：默认只写**只读诊断命令**；会改变状态的动作一律只写「用哪个脚本 / 看哪篇文档的哪一节」——命令留在脚本里，那儿才是真相源，而且脚本会随 compose 一起改。

**唯一的例外**是「容器根本起不来」那两节：那时的故障发生在脚本能跑之前，跳去别处查反而更慢，所以把处置命令直接写在这里，并标注真相源（见 [场景 · 容器起不来](#场景--容器起不来)）。

## 30 秒定性

先看容器还在不在，再决定往下读哪一节：

```bash
docker compose -f docker-compose.prod.yml ps
```

- **STATUS 列不是 `Up`**（没有 / `Exit` / 一直在 `Restarting`）→ [场景 · 容器起不来](#场景--容器起不来)
- **`Up` 但 health 列是 `unhealthy` 或 `starting` 很久** → [场景 · 后端 unhealthy 或接口 5xx](#场景--后端-unhealthy-或接口-5xx)
- **容器都健康，但页面打不开 / 改了没生效** → [场景 · 前端打不开或改了没生效](#场景--前端打不开或改了没生效)
- **页面能开但报数据库相关错误** → [场景 · 数据库连不上](#场景--数据库连不上)
- **dev 形态（只起 MySQL、前后端在宿主机跑）出问题** → 回到 [../../README.md](../../README.md) 的快速开始章节，本文只覆盖容器形态。

## 症状速查表

| 症状 | 最可能的原因 | 跳到 |
|------|--------------|------|
| `docker compose` 一敲就报错退出，看起来像 Docker 坏了 | `.env` 缺失或 `QUIZZY_JWT_SECRET` 为空（compose 在读配置阶段就退出） | [容器起不来](#场景--容器起不来) |
| `--build` 时报 `failed to fetch oauth token ... auth.docker.io` | 本机连不上 Docker Hub | [容器起不来](#场景--容器起不来) |
| 报容器名已存在 / `Conflict` | dev 与 prod 的 mysql 容器同名，不能同时跑 | [容器起不来](#场景--容器起不来) |
| 后端一直 `unhealthy` | 应用上下文没起来（配置、数据库、端口） | [后端 unhealthy](#场景--后端-unhealthy-或接口-5xx) |
| 页面 502 / 接口 5xx | 后端没就绪或 nginx 反代拿不到后端 | [后端 unhealthy](#场景--后端-unhealthy-或接口-5xx) |
| 打开 `http://localhost` 是旧页面 | nginx 没设 Cache-Control，浏览器缓存 | [前端](#场景--前端打不开或改了没生效) |
| 登录失效 / token 报非法 | JWT 密钥换过，或仍在用已泄露的默认值 | [配置](./configuration.md) |
| E 盘快满了 | 容器日志无轮转 | [磁盘与日志](#场景--磁盘与日志) |

## 数据在哪、怎么确认它还在（动手之前先看这节）

**数据不在容器里。** 它在 `.env` 的 `MYSQL_DATA_DIR` 指向的宿主机目录上，dev 与 prod 共用同一份（[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)）。

一眼确认它还在：

```bash
# 目录在不在、有多大（路径读 .env 的 MYSQL_DATA_DIR）
ls -la "$MYSQL_DATA_DIR"

# 库能不能查（只读）
docker exec quizzy-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e 'select 1'
```

结论只有一句：**删除容器 ≠ 删除数据**。真正能毁掉数据的只有「手工删那个目录」和「`down -v`」，两者都在下面的红线清单里。

## 场景 · 容器起不来

按顺序排除，每一步都有明确判据：

**1. `.env` 缺失或密钥为空**——表现最像「Docker 坏了」，其实 compose 在读配置阶段就退出了，`ps` 里一个容器都没有。
判据：直接报变量未设置，或提示 `QUIZZY_JWT_SECRET` 必填。
处置：见 [configuration.md](./configuration.md) 的「密钥」一节。若是要上线，直接用 [deployment.md](./deployment.md) 的上线前检查清单走一遍。

**2. 两个形态的 mysql 容器同名**——dev 与 prod 的 compose 里 mysql 容器名相同，不能同时运行。
判据：报 `Conflict`，说容器名已存在。
处置：把 dev 那个停掉。**只删容器，数据在宿主机目录里不会丢**：

```bash
docker compose -f docker-compose.dev.yml down
```

**3. 基础镜像拉不到**——Maven 与 npm 依赖已走国内源，但**基础镜像本身来自 Docker Hub**，是另一条路。
判据：`failed to fetch oauth token ... auth.docker.io ... Bad Gateway`。

#### 一次性处理：从镜像站拉取后补回官方 tag

不改全局配置、不用重启 Docker：

```bash
for img in maven:3.9.9-eclipse-temurin-21 eclipse-temurin:21-jre node:22-slim nginx:alpine; do
  docker pull docker.m.daocloud.io/library/$img
  docker tag  docker.m.daocloud.io/library/$img $img
done
```

#### 长期处理：配 registry mirror

Docker Desktop → Settings → Docker Engine，加入下面这段后 Apply & Restart：

```json
"registry-mirrors": ["https://docker.m.daocloud.io"]
```

⚠️ **这两段是本文档里唯一复制的写操作命令**，因为故障发生在任何脚本能跑之前。它们的真相源是 `quizzy-server/Dockerfile` 与 `quizzy-web/Dockerfile` 里的 `FROM`——**基础镜像变了要同步改这里**；镜像站是否可用也以当时为准。

**4. 都不是**——看日志尾部，重点看最后 20 行：

```bash
docker compose -f docker-compose.prod.yml logs --tail=100 <服务名>
```

## 场景 · 后端 unhealthy 或接口 5xx

先明确一件事：**healthcheck 探的是 springdoc 的 `/v3/api-docs`，且 unhealthy 不会让容器自己重启**——`restart: unless-stopped` 只在进程退出时生效（[ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)）。别指望 restart 救场，它只是个体温计。

- **一直是 `starting`**：还在等依赖（通常是数据库）。正常冷启动是十几秒量级；超过健康检查的等待上限就去看日志。
- **`unhealthy` 且稳定**：应用上下文没起来。按上一节的日志命令看后端容器的报错，最常见的是数据库连不上（见下节）或必填配置缺失。
- **页面 502 但后端 `healthy`**：nginx 侧的问题多发生在后端刚重启的那几秒；刷新一次仍不行再看 web 容器日志。

## 场景 · 前端打不开或改了没生效

- **打不开**：先确认 web 容器在 `ps` 里是 `Up`（nginx 没有 healthcheck，看 `State.Status`）。再看它监听的端口有没有被别的进程占了——端口定义见 `docker-compose.prod.yml`。
- **改了没生效**：`nginx.conf` 没设 `Cache-Control`，**必须硬刷新**。`deploy.sh` 跑完也会打印这条提示。

## 场景 · 数据库连不上

判断顺序固定：

1. mysql 容器在不在（`ps` 里有没有 `quizzy-mysql`）——不在就先拉起，走 [deployment.md](./deployment.md) 的上线路径。
2. 数据目录在不在（上一节的 `ls`）——不在才是真事故，此时**先不要重建**，先看 [backup.md](./backup.md) 能不能找到可用备份。
3. 后端报什么——`Access denied` 指向密码（`MYSQL_ROOT_PASSWORD`），`Unknown database` 指向库名（`MYSQL_DATABASE`）。两者都在 `.env` 里。

**重建 mysql 是最后手段**，而且重建不会丢数据（数据在宿主机目录上）。但动它之前，先手工跑一次备份，见 [backup.md](./backup.md)。

## 场景 · 磁盘与日志

容器日志**没有做轮转**，Docker 默认的 json-file driver 不限制大小，长期跑会一直涨。这是已登记的未决项，方案与盘位分析见 [docs/todo/2026-09-20-TODO-运行与运维.md](../todo/2026-09-20-TODO-运行与运维.md)，本文不讨论怎么改。

应急判断用这条：

```bash
docker system df        # 镜像与容器占了多少
```

## 场景 · 备份与恢复

**顺序只有一条：动数据之前先跑一次备份。** 怎么跑见 [backup.md](./backup.md)。

恢复步骤同样在 [backup.md](./backup.md)，并且**明确标注了尚未演练**——第一次用请务必在临时库上验证，别拿生产库当试验田。

## 升级与回滚

- 上线：`bash scripts/deploy.sh`（详见 [deployment.md](./deployment.md)）。
- **回滚不能靠镜像 tag 回退**——tag 是固定值，重建时被覆盖了。回滚只能回退代码版本再跑一次 `deploy.sh`，具体路径见 `deploy.sh` 失败时打印的「怎么回滚」一节。

## 红线清单

- ❌ **不要 `docker compose down -v`**——`-v` 会删匿名卷，是这套部署里唯一可能真的把数据卷走的写法。
- ❌ **部署时不要重建或删除 mysql 容器**——`deploy.sh` 刻意用 `--no-deps` 绕开它（[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)）。
  （dev 与 prod 切换时 `docker compose ... down` 停掉它是**允许的**，见上文；禁止的是在部署流程里重建它。）
- ❌ **不要手工删 `MYSQL_DATA_DIR` 指向的目录**——那是数据本体。
- ❌ **不要给 unhealthy 容器加自动重启指望它自愈**——healthcheck 不触发重启（[ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)）。
- ❌ **不要在没备份的情况下改已经执行过的 Flyway 迁移**。
- ❌ **不要让 `QUIZZY_JWT_SECRET` 使用公开仓库里那串默认值**——理由与轮换方式见 [configuration.md](./configuration.md)。

## 抓现场

出问题时留这几样输出，事后复盘或问人时才有得看：

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs --tail=100 <服务名>
docker inspect <容器名> --format '{{json .State}}'
```

如果是「部署脚本自己挂了」，不必手动抓——`deploy.sh` 的失败分支会自动打印日志尾部和回滚路径。
