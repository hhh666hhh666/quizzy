# 部署目标改为阿里云 ECS，CI 从门禁升级为构建与自动部署

Status: accepted · 取代 [ADR 0013](0013-github-actions-gate-no-auto-deploy.md)

运行位置从开发机搬到一台阿里云 ECS（中国大陆 / Alibaba Cloud Linux 4 / x86_64 / 2C2G / 包年包月），推 tag 后由 GitHub Actions 构建两个镜像推往阿里云 ACR，再 SSH 到 ECS 拉取与重启。**这是本项目第一次让无人值守的流水线改动运行中的服务**——ADR 0013 当时反对的正是这一点，所以那条顾虑必须在这里被正面回答，而不是绕过。触发这次改变的是「公开注册」（见 [ADR 0017](0017-public-signup-service.md)）：一个对公网开放的站点不能跑在一台会休眠的开发机上。

## Considered Options

- **服务器上构建，手动部署**（`compose up --build`，即 0013 那条路径的直译）：改动最小，而且两个 Dockerfile 的默认依赖源（Maven 走阿里云镜像、npm 走 npmmirror，[ADR 0010](0010-china-mirrors-for-build-deps.md)）在**大陆**服务器上正好是对的——CI 因为跑在境外才反过来传官方源。但 **2C2G 上跑一次 Maven 构建会直接 OOM**。内存不是一个可以调优的参数，而是这套方案的否决项；「CI 构建」因此从更优选择变成唯一可行选择。
- **CI 构建推 GHCR，服务器 pull**：CI 侧零配置、推送最快。但大陆 ECS 从 `ghcr.io` 拉取常慢且不稳，等于把不确定性放在了最需要确定的那一步。
- **本机构建后 `docker save` / `scp` / `docker load`**：不需要 registry 账号，构建最快（本地缓存是热的）。但本机仍留在部署路径上，每次要传几百 MB，与「从此不在本机上运行」的初衷相反。
- **CI 构建推 ACR + tag 自动部署**（选定）：ECS / ACR / OSS 同地域，服务器拉取走内网，快且免流量费；CI 与服务器之间只有 SSH 一跳。

## Consequences

- **「CI 绿了 ≠ 本机已更新」不再成立**，取而代之的是「推 tag 即上线」。人工动作从「执行 `deploy.sh`」前移到「决定要不要打 tag」——这是个更早、也更需要想清楚的决策点。
- ⚠️ **Flyway 迁移会在无人值守时自动执行。** 0013 的原话是「项目里有 Flyway 迁移和题库数据，不该被一条无人值守的流水线自动改动」。接受它，但配三道保险：
  1. 部署脚本在 `up -d` 之前**强制跑一次备份**；
  2. CI 新增 `flyway-verify` job，在临时 `mysql:8` 上把 `db/migration` 完整跑一遍——此前 `mvn test` 完全不碰数据库，**迁移从未被验证过**；
  3. 纪律：迁移只做向后兼容的加表 / 加可空列，破坏性变更不走自动部署。
  **第三道是靠人守的**；它一破，前两道只能证明「语法能跑」，不能证明「数据安全」。
- **`docker-compose.prod.yml` 的语义变了**：从「本机 prod 编排」变成「云上编排」——镜像来自 ACR、数据目录是 Linux 路径、MySQL 不再映射端口、web 加 443。本机只剩 dev 形态（[ADR 0007](0007-dev-prod-compose-with-bind-mount.md) 的双形态拆分因此不再是「两种运行方式」，而是「两个环境」）。
- **回滚变强了。** 上一个方案里「本地每个已发版本留一份镜像」的保证在 v1.0.0 → v1.1.0 之间已经破过一次（本地实际只剩 `1.1.0` 一个落点）；ACR 保留每个版本，回滚就是拉旧 tag，不再依赖某台机器上恰好还留着镜像。
- **8080 端口映射被删掉。** 顺带解决了 Swagger 的暴露问题，而且是白捡的：`nginx.conf` 本来就只转发 `/api/` 与静态资源，`/v3/api-docs` 与 `/swagger-ui.html` 会落进 SPA 回退，**从来没有对外暴露过**，只有直连宿主机 8080 才看得到。所以「关掉 springdoc」既不必要，也就不会发生「关掉 Swagger 连带打断 healthcheck」的连锁——[ADR 0012](0012-healthcheck-probes-api-docs.md) 的探测端点保持不变。
- **版本号治理（[ADR 0015](0015-version-number-governance.md)）要同步调**：compose 的 `image:` 从字面量 `quizzy-server:1.1.0` 变成 `${ACR_REGISTRY}/quizzy-server:${APP_VERSION}`，`scripts/check-version.sh` 的校验正则跟着改。
- **`.env` 分裂成两份语义**：本机 dev 一份（Windows 数据路径），云上一份（Linux 路径 + ACR 地址）。`.env.example` 只有一份就不够了。
- **服务器上不 clone 仓库**：CI 每次把 `docker-compose.prod.yml` 一起送过去，保证编排文件与 tag 一致——否则「服务器上的 compose 是哪个版本」会变成一个新的漂移源。

相关：[ADR 0007](0007-dev-prod-compose-with-bind-mount.md)（双形态与绑定挂载）、[ADR 0010](0010-china-mirrors-for-build-deps.md)（依赖源）、[ADR 0012](0012-healthcheck-probes-api-docs.md)（探测端点）、[ADR 0015](0015-version-number-governance.md)（版本号）、[ADR 0017](0017-public-signup-service.md)（公开注册，本次改变的触发原因）。

## Amendment 1（2026-10-02）：入口改由宿主 nginx 承担，证书不再自建

上线前实测那台轻量服务器（`8.137.172.6`）时发现两件与正文假设不符的事：

1. **它上面装着并运行着宝塔面板 11.2.0**，宝塔的 nginx 占着 `80` 与 `888`，面板自身在 `7891`。
   而正文写的形态是「web 容器自己 hold `80:80` 与 `443:443`」——**直接冲突**。
2. 正文担心的「大陆拉不动 Docker Hub 基础镜像」**不成立**：那台机的 `/etc/docker/daemon.json` 早就配好了
   `registry-mirrors`，`hello-world` 一次通过。（另：系统实际是 Alibaba Cloud Linux **3** 而非 4，
   且 `docker-ce` + `compose plugin` 早已装好并开机自启，正文里"要装 moby、要建 swap"那两步都不需要。）

**改动只有两处，其余全部不变：**

- **对外入口交给宿主上既有的 nginx**。quizzy 的 web 容器只绑 `127.0.0.1:8081:80`，由宿主 nginx 反代过去，
  不与既有环境抢端口。代价是多一层 nginx，且站点配置归宝塔管、位于仓库之外，成为新的「仓库外配置漂移源」。
- **HTTPS 不再自建**。原计划用 `acme.sh` + DNS-01 签 Let's Encrypt、并把一个仅 DNS 权限的 RAM AccessKey
  放在服务器上；宝塔自带 Let's Encrypt 申请与自动续期，于是**整条自建链路连同那个 AccessKey 的代价一起消失**。
  上面 Considered Options 里「DNS-01 唯一的代价是服务器上要存一个 DNS AccessKey」这句随之作废——
  这是换成宿主 nginx 之后顺带拿到的净收益，不是额外开销。

**决策过程**：`docs/todo/2026-10-02-TODO-云上入口与宝塔共存.md`（已解决）记录了三个选项与取舍依据。
