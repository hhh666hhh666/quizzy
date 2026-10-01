# 部署

## 分工：CI 只做门禁，上线由人触发

**CI 绿了不等于本机已更新。** CI 跑的是验证，不会把任何产物投放到任何机器；上线这一步永远由人执行。理由见 [ADR 0013](../adr/0013-github-actions-gate-no-auto-deploy.md)。

CI 具体跑哪几条检查，看 [.github/workflows/ci.yml](../../.github/workflows/ci.yml)——本文件不复制那张表，改了会立刻过期。

## 两种运行形态

| 形态 | 内容 | 用在哪 |
|------|------|--------|
| dev | 只把 MySQL 放容器，前后端在宿主机跑 | 日常改代码 |
| prod | 前端、后端、数据库全容器 | 演示 / 实际自用 |

两者**不能同时运行**（mysql 容器名相同），切换方式见 [../../README.md](../../README.md) 的运行章节。数据共用同一份，切换不丢。

## 唯一的上线路径：`scripts/deploy.sh`

日常改完代码要更新容器，**用脚本，不要手敲 compose 命令**。它保证的是这几件事（细节与顺序写在脚本头部注释里，那儿才是真相源）：

- 校验 `.env` 存在、`QUIZZY_JWT_SECRET` 已设置且非空；
- **算出要注入的版本号与构建信息**（从 `git tag` 推），见下节；
- **mysql 永不参与重建**——用 `--no-deps` 绕开 `depends_on`，它最多在「没跑起来」时被拉起一次；
- 等健康检查通过，超时就打印日志尾部并给出回滚路径；
- 探一次真实端口（只告警，不中断）。

用法只有三种，脚本头部注释里写得很清楚：`bash scripts/deploy.sh [web|server|all]`。

> ⚠️ **绕过脚本直接 `docker compose up` 也能把服务跑起来，但版本号会变成 `dev`。**
> 那四个 `APP_*` 变量是脚本算出来再 `export` 给 compose 的，不在 `.env` 里（`.env` 是密钥与
> 本机配置的地盘，见 [ADR 0011](../adr/0011-env-file-with-local-defaults.md)）。
> 另外绕过脚本就绕过了它全部的前置校验与健康检查，只是恰好不会立刻报错而已。

## 版本号从哪来

「关于」弹窗显示的主行是 `git tag` 的字面值，副行只在 `HEAD` 领先 tag 时出现。**真相源是 `git tag`**，
三处手写点（`package.json` / `pom.xml` / compose 的 `image` tag）是它的镜像，打 tag 时由 CI 校验，
设计与取舍见 [ADR 0015](../adr/0015-version-number-governance.md)。

日常只需要记住两件事：

- **打 tag 前**先跑 `bash scripts/check-version.sh <tag>` 或直接看 CI 的「版本 · tag 与三处对齐」job；
  在非 tag 提交上这个脚本会自动跳过（HEAD 领先 tag 是正常状态，硬比必然误报）。
- **改了版本号要同步 compose 的 `image:` 字面量**。两者对不上时 `deploy.sh` 会告警——因为那会把
  上一个版本的镜像覆盖掉，悄悄毁掉回滚能力。

## 上线前检查清单

1. `.env` 存在，且 `QUIZZY_JWT_SECRET` 是**随机值**——默认值已随公开仓库泄露，见 [configuration.md](./configuration.md) 与 [ADR 0011 Amendment 1](../adr/0011-env-file-with-local-defaults.md)。
2. 本地 CI 是绿的（跑什么见 [../../docs/testing.md](../testing.md) 的 CI 一节）。
3. 本次改动涉及数据库结构 → **先跑一次备份**，见 [backup.md](./backup.md)。
4. 若本次改了 Dockerfile 或依赖源，本机构建可能命中缓存掩盖问题；真正的验证是 CI 那两个镜像冒烟 job。

## 上线后该看什么

脚本跑完会自己打印最终状态与访问地址。两种期望终态：

- `docker compose ps` 里 server 的 health 列是 `healthy`；
- 前端能打开（**记得硬刷新**，nginx 没设 Cache-Control）。

不符合就转 [runbook.md](./runbook.md)。

## 回滚

分两种，取决于要退回哪里：

- **退回上一个已发版本**（最常见）——可以**复用本地镜像，不必重新构建**。镜像 tag 跟随版本号
  （[ADR 0015](../adr/0015-version-number-governance.md)），所以 `--build` 只会重打当前版本号那一份，
  别的版本号的镜像还在本地。具体命令在 `deploy.sh` 失败时打印的「怎么回滚」一节里。
- **退回其它任意提交**——需要回退代码再跑一次 `deploy.sh` 重新构建。

⚠️ 保证的粒度是**「每个已发版本保有一份镜像」，不是「每个提交一份」**：同一版本号内的多次构建
仍会互相覆盖。所以回滚的可靠落点是 tag，不是随便一个 commit。

⚠️ **这条保证是从 v1.1.0 起才成立的。** v1.0.0 发布时镜像 tag 还是旧方案的写死值 `0.1.0`
（与 tag 名对不上），那两个镜像已在上线 v1.1.0 后清掉。所以**当前本地镜像只有一个回滚落点：`1.1.0`**；
真要退回 v1.0.0 只能 `git checkout v1.0.0` 后重新跑 `deploy.sh` 构建。

判断原则：能用回滚解决的（新代码引入的问题）就回滚；数据层面的问题不要靠回滚硬扛，先看 [runbook.md](./runbook.md) 的数据一节。

## 已知未决

- 后端端口是否继续暴露到宿主机 → [docs/todo/2026-09-20-TODO-运行与运维.md](../todo/2026-09-20-TODO-运行与运维.md)
- 镜像 tag 与分发方式 → [docs/todo/2026-09-20-TODO-镜像分发.md](../todo/2026-09-20-TODO-镜像分发.md)
