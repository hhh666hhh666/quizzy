# 应急预案（Runbook）

出事时按**症状**查，不按章节读。

**命令分层**：默认只写**只读诊断命令**；会改变状态的动作一律只写「用哪个脚本 / 看哪篇文档的哪一节」——命令留在脚本里，那儿才是真相源，而且脚本会随 compose 一起改。

**前提**：prod 跑在阿里云轻量服务器上（`8.137.172.6`），本机只剩开发形态。下面除非明说「本机」，命令都在**服务器**上跑。

## 30 秒定性

先看容器还在不在，再决定往下读哪一节：

```bash
cd /srv/quizzy/compose/scripts
bash server-compose.sh ps
```

> **走包装脚本，别直接敲 `docker compose`**：compose 每次调用都会把整份文件插值一遍，而 `image:` 上有 `${APP_VERSION:?...}` 必填断言——缺了它**连 `ps` 都会退出**，报错长得像「compose 文件坏了」。包装脚本会自动从 `.current-version` 补上，顺带处理了 `--env-file` 的路径。

- **STATUS 列不是 `Up`**（没有 / `Exit` / 一直在 `Restarting`）→ [场景 · 容器起不来](#场景--容器起不来)
- **`Up` 但 health 列是 `unhealthy` 或 `starting` 很久** → [场景 · 后端 unhealthy 或接口 5xx](#场景--后端-unhealthy-或接口-5xx)
- **容器都健康，但站点打不开** → [场景 · 站点打不开](#场景--站点打不开)
- **页面能开但报数据库相关错误** → [场景 · 数据库连不上](#场景--数据库连不上)
- **本机 dev 形态（只起 MySQL、前后端在宿主机跑）出问题** → 回到 [../../README.md](../../README.md) 的快速开始章节，本文只覆盖线上。

## 症状速查表

| 症状 | 最可能的原因 | 跳到 |
|------|--------------|------|
| 部署 job 报 `manifest unknown` / `pull access denied` | ACR 上没有这个版本 tag（构建没成功，或 tag 名对不上） | [容器起不来](#场景--容器起不来) |
| 报容器名已存在 / `Conflict` | MySQL 那个容器还归 `docker run` 管、没有 compose 标签 | [容器起不来](#场景--容器起不来) |
| `failed to fetch oauth token ... auth.docker.io` | 服务器拉 `mysql:8` 时连不上 Docker Hub | [容器起不来](#场景--容器起不来) |
| 后端一直 `unhealthy` | 应用上下文没起来（配置、数据库、端口） | [后端 unhealthy](#场景--后端-unhealthy-或接口-5xx) |
| 页面 502 / 接口 5xx | 后端没就绪，或容器 nginx 拿不到后端 | [后端 unhealthy](#场景--后端-unhealthy-或接口-5xx) |
| 浏览器打开是宝塔的默认页 | 宿主 nginx 的站点没配好 / 没生效 | [站点打不开](#场景--站点打不开) |
| 登录失效 / token 报非法 | JWT 密钥换过 | [配置](./configuration.md) |
| 磁盘快满了 | 镜像堆积（日志已有上限，不会再无限涨） | [磁盘](#场景--磁盘) |

## 数据在哪、怎么确认它还在（动手之前先看这节）

**数据不在容器里。** 它在 `/srv/quizzy/.env` 的 `MYSQL_DATA_DIR` 指向的宿主机目录上（[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)）。

一眼确认它还在：

```bash
# 目录在不在、有多大（路径读 .env 的 MYSQL_DATA_DIR）
ls -la /srv/quizzy/mysql

# 库能不能查（只读）
docker exec quizzy-mysql mysql -uroot -p"$(grep '^MYSQL_ROOT_PASSWORD=' /srv/quizzy/.env | cut -d= -f2-)" -e 'select 1'
```

结论只有一句：**删除容器 ≠ 删除数据**。真正能毁掉数据的只有「手工删那个目录」和「`down -v`」，两者都在下面的红线清单里。

**顺带一条新能力**：MySQL 现在绑在宿主回环 `127.0.0.1:3306`（[ADR 0016 Amendment 2](../adr/0016-cloud-deploy-with-release-pipeline.md)），
所以**本机可以经 SSH 隧道直连**排障，不必再从容器里绕：

```bash
ssh -N -L 13306:127.0.0.1:3306 -i ~/.ssh/quizzy/workbuddy.pem root@8.137.172.6
# 另开一个终端：mysql -h127.0.0.1 -P13306 -uroot -p quizzy
```

⚠️ **本机的 `docker-compose.dev.yml` 也占着 `127.0.0.1:3306`** —— 没走隧道时会连到**本机的 dev 库**，
而两边表结构与种子题都一样，界面上完全看不出来。判断连的是哪一个：

```sql
select @@max_connections;
```

云端是 `50`（compose 显式设的），本机 dev 是默认的 `151`。

## 场景 · 容器起不来

按顺序排除，每一步都有明确判据：

**1. 镜像拉不到。** 分两类，症状不同：

- **应用镜像来自 ACR**（`crpi-*.cn-<地域>.personal.cr.aliyuncs.com`，**新个人版实例**格式）：报 `manifest unknown` 说明 ACR 上**没有这个版本 tag**——回头看 `release.yml` 的 `build-push` job 是不是红的，或 tag 名有没有对错（镜像 tag 不带 `v`）。
- **`mysql:8` 来自 Docker Hub**：报 `failed to fetch oauth token ... auth.docker.io` 就是这条路不通。服务器上已配好 `/etc/docker/daemon.json` 的 `registry-mirrors`，确认它还在；实在不行就用镜像站拉下来再打回官方 tag。

**2. MySQL 容器没有 compose 标签。** 它是先期用 `docker run` 起的，直接 `up -d` 会报容器名冲突。
处置见 [deployment.md](./deployment.md) 的「首次上线的一次性准备」第 3 步——**只 `docker rm` 容器**，数据在宿主机目录上不受影响。

**3. 都不是** —— 看日志尾部，重点看最后 100 行：

```bash
cd /srv/quizzy/compose/scripts
bash server-compose.sh logs --tail=100 <服务名>
```

## 场景 · 后端 unhealthy 或接口 5xx

先明确一件事：**healthcheck 探的是 springdoc 的 `/v3/api-docs`，且 unhealthy 不会让容器自己重启**——`restart: unless-stopped` 只在进程退出时生效（[ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)）。别指望 restart 救场，它只是个体温计。

- **一直是 `starting`**：还在等 MySQL。正常冷启动是十几秒量级；超过健康检查的等待上限就去看日志。
- **`unhealthy` 且稳定**：应用上下文没起来。看后端容器报错，最常见的是数据库连不上（见下节）或必填配置缺失。
- **后端 `healthy` 但接口 5xx**：去 [deployment.md](./deployment.md) 的「回滚」一节——版本引入的问题就回滚。
- **怀疑内存不够**（2C2G 的硬约束）：`free -m` 与 `docker stats --no-stream` 一起看；JVM 堆是 `docker-compose.prod.yml` 里 `JAVA_TOOL_OPTIONS` 的 `-Xmx512m`，吃紧时第一手段是降到 384m。

## 场景 · 站点打不开

从外往里一层层剥，每层一个判据：

1. **打开的是宝塔默认页 / 403** → 宿主 nginx 的站点没配好或没生效。站点配置在宝塔面板里（仓库 [deploy/nginx/quizzy-site.conf](../../deploy/nginx/quizzy-site.conf) 只是对照模板）。
2. **宿主 nginx 返回 502** → 它反代的目标 `127.0.0.1:8081` 没人听。查 web 容器在不在、`docker compose ps` 里它的 ports 是不是 `127.0.0.1:8081->80/tcp`。
3. **静态页能开但接口 502** → 容器 nginx 到后端的这一段断了，转上面「后端 unhealthy」。
4. **改了前端但页面没变**：**这不再是已知毛病**——容器 nginx 现在给 `index.html` 发了 `no-cache`、给带 hash 的 `assets/` 发了长缓存。所以如果还是旧页面，说明**镜像没换**（部署没跑到 / 版本没变），而不是浏览器缓存。

## 场景 · 数据库连不上

判断顺序固定：

1. mysql 容器在不在（`ps` 里有没有 `quizzy-mysql`）——不在就先拉起。
2. 数据目录在不在（上面那节的 `ls`）——不在才是真事故，此时**先不要重建**，先看 [backup.md](./backup.md) 能不能找到可用备份。
3. 后端报什么——`Access denied` 指向密码（`MYSQL_ROOT_PASSWORD`），`Unknown database` 指向库名（`MYSQL_DATABASE`）。两者都在 `/srv/quizzy/.env` 里。

**重建 mysql 是最后手段**，而且重建不会丢数据（数据在宿主机目录上）。但动它之前，先手工跑一次备份。

## 场景 · 磁盘

日志这一条**已经解决了**：三个服务都在 `docker-compose.prod.yml` 里限了 `max-size: 10m` / `max-file: 3`，不会无限涨。

现在会涨的是**镜像**——每次部署都拉一份新的。应急判断：

```bash
docker system df        # 镜像与容器占了多少
```

清理属于写操作，用 `docker image prune -a`（**先确认 ACR 上还有旧版本**，那是回滚落点，本地删了不影响回滚）。定期清理的动作还没挂调度，见 [docs/todo/2026-09-20-TODO-运行与运维.md](../todo/2026-09-20-TODO-运行与运维.md)。

## 场景 · 备份与恢复

**顺序只有一条：动数据之前先跑一次备份。** 怎么跑见 [backup.md](./backup.md)。

恢复步骤同样在 [backup.md](./backup.md)，并且**明确标注了尚未演练**——第一次用请务必在临时库上验证，别拿生产库当试验田。

## 升级与回滚

- **升级**：推一个 `v*` tag，见 [deployment.md](./deployment.md) 的上线路径。
- **回滚**：在服务器上跑 `scripts/server-rollback.sh <版本号>`。镜像按版本 tag 保留在 ACR，**不需要重新构建**。
- ⚠️ **回退应用 ≠ 回退数据库**：迁移只做向后兼容是纪律，破坏性变更不靠回滚兜底。

## 红线清单

- ❌ **不要 `docker compose down -v`**——`-v` 会删匿名卷，是这套部署里唯一可能真的把数据卷走的写法。
- ❌ **部署时不要重建或删除 mysql 容器**——`server-deploy.sh` 刻意用 `--no-deps` 绕开它（[ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md)）。
- ❌ **不要手工删 `MYSQL_DATA_DIR` 指向的目录**——那是数据本体。
- ❌ **不要给 unhealthy 容器加自动重启指望它自愈**——healthcheck 不触发重启（[ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)）。
- ❌ **不要在没备份的情况下改已经执行过的 Flyway 迁移**。
- ❌ **不要让 `QUIZZY_JWT_SECRET` 使用公开仓库里那串默认值**——理由与轮换方式见 [configuration.md](./configuration.md)。
- ❌ **不要在服务器上构建镜像**——2C2G 跑 Maven 必 OOM，构建一律在 CI。

## 抓现场

出问题时留这几样输出，事后复盘或问人时才有得看：

```bash
cd /srv/quizzy/compose/scripts
bash server-compose.sh ps
bash server-compose.sh logs --tail=100 <服务名>
docker inspect <容器名> --format '{{json .State}}'
```

如果是「部署脚本自己挂了」，不必手动抓——`server-deploy.sh` 的失败分支会自动打印日志尾部和回滚路径。
