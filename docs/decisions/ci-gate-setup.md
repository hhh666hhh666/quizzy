# 决策日志：quizzy 的 CI/CD 怎么落地

## 2026-09-23 19:39 · 第 1 次 grill（跨到 09-24 收尾，09-25 合并上线）

起因是「我想做 ci/cd」这一句话。开问前先摸了仓库，两条事实决定了整棵决策树的形状：

1. **这个项目没有部署目标机器**。`docs/decisions/docker-run-setup.md` 的 D3 早已定下「纯本机
   Docker Desktop，localhost 自用」，全仓库零个 IP、零个部署域名、无 SSH/rsync/部署脚本。
2. **仓库是 public 的**，而 GitHub 官方明确警告不要在 public 仓库上用 self-hosted runner
   （OWASP 说 never）。

这两条合起来意味着：**CD 那一半在本项目里不成立**，"持续部署"没有落点。整轮 grill 实际上是在
重新定义"做 ci/cd"在这里该是什么意思。

相关：[ADR 0013](../adr/0013-github-actions-gate-no-auto-deploy.md)

### D1 · CI 只做门禁，不做自动部署

- **决策**：CI 只做质量门禁，不会把任何产物送到任何机器上去跑；上线由人执行 `bash scripts/deploy.sh`。
- **因为**：采纳推荐。没有远端目标机器，且 public 仓库上装 self-hosted runner 等于让任何能
  fork 开 PR 的人在开发机执行代码——那台机器上还有题库、笔记和浏览器 session，全在同一信任域。
- **放弃**：① self-hosted runner 装开发机做真自动部署；② 买云服务器做真持续部署（个人刷题
  项目不值当，还要重新处理密钥与端口）；③ 完全不上 CI。
- **来源**：Q1
- 相关：[ADR 0013](../adr/0013-github-actions-gate-no-auto-deploy.md)

> **Q1 - CD 的落点：到底"交付"到哪儿？**：这是根决策，其余都挂在它下面。你目前**没有任何部署目标机器**——唯一环境是你本机的 Windows + Docker Desktop。
>
> - **A. 保持本机，不做自动部署**。CI 只当质量门禁，上线仍由你触发。CD 这一半按"**持续可交付**"理解：代码随时能一键上线，但不上自动。
> - **B. 本机 + self-hosted runner 做真自动部署**。⚠️ 技术上可以用 `on: push: branches: [master]` 挡住 fork PR（fork 触发的是 `pull_request` 事件），但代价是**永久放弃 PR 触发**，且哪天你手滑加一个 `pull_request_target`，就是把你家 Windows 对全网开放。官方措辞 "almost never"。
> - **C. 新增一台常驻远端**（云服务器／家里另一台机器／NAS）。要花钱、要定机型、还要重新处理密钥与端口。
> - **D. 暂不定义 CD，写进 `docs/todo/` 挂起**。仓库有先例——`docs/todo/2026-09-20-TODO-镜像分发.md` 就是这么处理的。
>
> ➡️ **推荐 A**。C 为一个个人刷题项目买服务器，性价比不成立；B 在 public 仓库上是明确的反模式；D 太消极——你每天都已经在手工敲那条 compose 命令，把它固化下来几乎零成本，而且它正好就是"持续交付"在当前拓扑下的诚实形态。
>
> **你的回答**：按你推荐的来

### D2 · 门禁内容：单测 + 类型检查 + 构建 + 两个镜像冒烟

- **决策**：四条检查——后端 `mvn test`、前端 `npm ci` + `typecheck` + `build`、两个镜像各自在
  干净 Linux 上 `docker build` 冒烟。**只构建，不推任何 registry**。
- **因为**：采纳推荐。冒烟是最值钱的一项：ADR 0009（alpine 缺 esbuild 二进制）和 ADR 0010
  （依赖源）记的那类坑**只在干净环境重建时暴露**，本机因为缓存命中永远发现不了。
- **放弃**：① 只做最小门禁（不含 docker build）；② 外加推镜像到 GHCR（目前没有消费者，
  `docs/todo/2026-09-20-TODO-镜像分发.md` 已明说推了没人拉）。
- **来源**：Q2

> **Q2 - CI 门禁跑到哪一步？**：你现在的 Dockerfile **从不跑 `typecheck`**（只跑 `build`），后端 7 个单测也从来没被自动跑过，而且 Dockerfile 至今只在你的 Windows Docker Desktop 上验证过。
>
> - **A. 最小门禁**：`mvn test` + `npm ci` + `npm run typecheck` + `npm run build`。不碰 Docker。
> - **B. A + 在干净 Linux 上 `docker build` 两个镜像做冒烟**。这一步的价值特别具体：ADR 0009/0010 记的两个坑**只有在干净环境重建时才会暴露**。
> - **C. B + 推镜像到 GHCR**。⚠️ 目前**没有消费者**。
>
> ➡️ **推荐 B**。A 少了最值钱的那一项验证；C 推了没人拉，白占存储。
>
> **你的回答**：按你推荐的来

### D3 · Docker Hub 用 PAT 登录破限流，未配 secrets 时优雅跳过

- **决策**：配 `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` 走 `docker/login-action`；secrets 未配
  或 fork PR 拿不到 secrets 时整步跳过，构建照跑。
- **因为**：采纳推荐。匿名限额 100 次/6 小时**按共享出口 IP 计**，GitHub 托管 runner 共用出口
  IP 必撞顶；认证后 200 次/6 小时且按账号计。一个 secret 换两倍配额。
- **放弃**：① runner 上配 registry mirror；② 把 `FROM` 改成 `mirror.gcr.io`（会把镜像源写进
  受版本控制的 Dockerfile，与 ADR 0010「国内源是本机约束不是项目偏好」冲突）。
- **来源**：Q3
- ⚠️ **实现时踩到的坑**：`secrets` 上下文**不能出现在 `if:` 里**（step 级、job 级都不行），会报
  `Unrecognized named-value: 'secrets'` 并让整个 workflow 文件作废、一个 job 都起不来。
  最终写法是 job 级 `env` 折算布尔：`HAS_DOCKERHUB: ${{ secrets.A != '' && secrets.B != '' }}`，
  step 再判 `env.HAS_DOCKERHUB == 'true'`。密钥值本身不进 env，只传"有没有"。

> **Q3 - Docker Hub 限流怎么破？**：CI 要拉 `maven:3.9.9-eclipse-temurin-21`（很大）、`eclipse-temurin:21-jre`、`node:22-slim`、`nginx:alpine`，匿名 100 pulls/6h 按共享 IP 计，撞限流是迟早的事。
>
> - **A. 配 Docker Hub PAT + `docker/login-action`**：免费账号提到 200/6h，且限额跟**你的账号**走，不跟共享 IP 走。
> - **B. runner 上配 registry mirror**（`mirror.gcr.io`）：不动 Dockerfile，但每个 job 都要配一次。
> - **C. 把 `FROM` 改成 `mirror.gcr.io/library/...`**：一劳永逸，但会把镜像源**写进 Dockerfile**，与 ADR 0010 的判断冲突。
> - **D. 不做 docker build**（= Q2 选 A），绕开整个问题。
>
> ➡️ **推荐 A**。最省事，且不污染受版本控制的 Dockerfile。
>
> **你的回答**：按你推荐的来

### D4 · 固化 scripts/deploy.sh，永不重建 mysql

- **决策**：把上线固化成 `scripts/deploy.sh [web|server|all]`，校验 `.env` 与 JWT → mysql 只在
  没跑时拉起一次 → `compose up -d --build --no-deps` → 等健康检查 → 探端口（只告警）。
- **因为**：采纳推荐。这是 CD 在当前拓扑下唯一诚实的形态；`--no-deps` 确保 `depends_on` 不会把
  数据库拉进这次操作，数据在宿主机 `MYSQL_DATA_DIR`，重建 mysql 只是白冒风险。
- **放弃**：不固化、继续手敲 compose 命令。
- **来源**：Q4
- 相关：[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)

> **Q4 - 本机这一侧：把上线固化成 `scripts/deploy.sh` 吗？**：你现在敲的是 `docker compose -f docker-compose.prod.yml up -d --build --force-recreate web`。
>
> - **A. 固化成脚本**：先校验 `.env` 齐全（缺 `QUIZZY_JWT_SECRET` 直接退出）→ build → recreate 指定服务（**永不碰 mysql**）→ 等 healthcheck 通过 → 打印 `docker compose ps`，失败给明确提示。
> - **B. 不固化**，继续手敲。
>
> ➡️ **推荐 A**。它顺手把 `docs/todo/2026-09-20-TODO-运行与运维.md` 里那段"改完代码怎么更新到 prod 容器"的口口相传操作变成可执行的事实。
>
> **你的回答**：按你推荐的来

### D5 · 两个 Dockerfile 的依赖源用 build-arg 参数化

- **决策**：server 加 `ARG MAVEN_MIRROR=aliyun`（非 aliyun 时删掉容器里的 settings.xml），
  web 加 `ARG NPM_REGISTRY=https://registry.npmmirror.com`。**默认值保持国内源，本机行为零变化**，
  CI 用 `--build-arg` 切回官方源。
- **因为**：采纳推荐。ADR 0010 的国内源是**本机**约束，境外 runner 直连官方源更快更稳；
  硬改会让本机构建立刻拉不动，所以参数化而不是删掉。
- **放弃**：① 不动 Dockerfile、CI 照用国内源（境外访问可能慢/偶发失败，CI 会变 flaky）；
  ② CI 里干脆不 docker build（丢掉最值钱的那项验证）。
- **来源**：Q5
- 相关：[ADR 0010 Amendment 1](../adr/0010-china-mirrors-for-build-deps.md)

> **Q5 - CI 依赖源**：你两个 Dockerfile 把国内源写死了（Maven 走 aliyun、npm 走 npmmirror），这是 ADR 0010 记的本机环境约束。但 CI 跑在 GitHub 的境外 runner 上，直连官方源其实更快更稳——怎么处理这个冲突？
>
> - **A. 参数化，CI 切回官方源**：给两个 Dockerfile 加 build-arg。本机默认值一字不改，CI 传参用 Maven Central / registry.npmjs.org。
> - **B. 不动 Dockerfile，CI 照用国内源**：最省事。风险是境外 runner 访问 aliyun / npmmirror 可能慢或偶发失败，CI 会变成时红时绿的 flaky 门禁。
> - **C. CI 里干脆不 docker build**：退化成只跑 mvn test + npm typecheck + npm build，绕开源的问题。
>
> ➡️ **推荐 A**。
>
> **你的回答**：参数化，CI 切回官方源（推荐）

### D6 · 触发：push master + PR 到 master

- **决策**：`on: push: branches: [master]` + `on: pull_request: branches: [master]`。
- **因为**：采纳推荐。云 runner 不涉及 self-hosted 的安全问题，多开一个触发零成本；现在没开过
  PR，但以后想改有风险的东西时，PR 上先跑一遍是白捡的。
- **放弃**：只 push master（以后用 PR 时要记得回来加一行）；push + 每周定时（能抓依赖腐化，
  但当前没那个需要）。
- **来源**：Q6

> **Q6 - 触发时机**：你目前只有 master 一个分支、单人开发、没有 PR 流程。
>
> - **A. push master + PR 到 master**
> - **B. 只在 push 到 master 时跑**
> - **C. push master + 每周定时跑一次**
>
> ➡️ **推荐 A**。
>
> **你的回答**：push master + PR 到 master（推荐）

### D7 · 7 个前端类型错误先修再上 CI

- **决策**：先把 `npm run typecheck` 的 7 个存量错误修掉，再把它纳入门禁。
- **因为**：采纳推荐。typecheck 当时是红的（exit 2），不修则 CI 首次 push 必红；且其中
  `QuickQuizView` 那个是真隐患——`types: string[]` 允许把任意字符串塞进抽题规则发给后端。
- **放弃**：① typecheck 单独拆成 `continue-on-error` 的 job（CI 不红但 `strict: true` 继续是
  摆设，黄色警告最容易被无视）；② CI 第一版不跑 typecheck。
- **来源**：Q7

> **Q7 - 类型债**：typecheck 现在就是红的（7 个错）。把它加进 CI 的话，第一次 push 必然失败。怎么处理这 7 个错？
>
> - **A. 先修再上 CI**：我一次性修完再配 CI，推上去就是绿的。改动都很小；其中 QuickQuizView 那个是真隐患——现在能把任意字符串塞进 rule.types 发给后端。代价是这次改动会碰到 5 个业务源文件，不只是加 CI 配置。
> - **B. 先上 CI，typecheck 单独降级**：拆成独立 job 加 continue-on-error，CI 不会红。好处是只动 CI 配置、不碰业务代码。代价是 strict 继续形同虚设，黄色警告最容易被无视掉。
> - **C. CI 第一版不跑 typecheck**：等于放弃类型检查这一项。
>
> ➡️ **推荐 A**。
>
> **你的回答**：先修再上 CI（推荐）

### D8 · 首次验证走 ci/gate 分支 + PR，不在 master 上留红记录

- **决策**：两个 commit 都提交在 `ci/gate` 分支上，推上去后开 PR 触发 CI；验绿了再 squash
  合进 master，删分支。
- **因为**：采纳推荐。即使 CI 红了也只红在分支上，master 不留红记录、README 的 badge 不会变红。
- **放弃**：① 直接在 master 提交并推送（CI 立刻触发但失败就落在主干上，badge 会红到下一个能过的
  commit）；② 只提交到本地不推送。
- **来源**：Q8

> **Q8 - 提交与推送怎么走？**（当前有 10 个改动文件 + 3 个新文件未提交，分两个 commit：一个是修类型错误，一个是 CI 本身）
>
> - **A. 建 ci/gate 分支 + 推 + 开 PR**：即使 CI 红了，也只红在这个分支上，master 不留红记录，README 的 badge 也不会红。验绿了再 squash 合进 master。
> - **B. 直接在 master 提交并推送**：CI 立刻触发，验起来最快。但一旦有哪一步挂了，红的记录就落在 master 上了。
> - **C. 只提交到本地，不推送**
>
> ➡️ **推荐 A**。
>
> **你的回答**：建 ci/gate 分支 + 推 + 开 PR（推荐）

### D9 · ADR 0010 的措辞一起修，放同一个 PR

- **决策**：给 ADR 0010 追加 Amendment 1，纠正「官方源必然超时失败」这句不实表述。
- **因为**：采纳推荐。实测四个源本机全通（`repo1.maven.org` 200/1.8s、`registry.npmjs.org`
  200/5.9s），那句是把 Docker Hub 不通外推到了包管理器源。放在一起少一次往返。
- **放弃**：先不改（留到以后）；先补测 `repo1.maven.org` 再决定。
- **来源**：Q9
- 相关：[ADR 0010 Amendment 1](../adr/0010-china-mirrors-for-build-deps.md)

> **Q9 - ADR 0010 措辞**：顺带把 ADR 0010 那句「解析依赖必然超时失败」的措辞改准吗？（今天实测发现 registry.npmjs.org 在本机其实是通的，那句是外推不是实测）
>
> - **A. 一起改，同一个 PR**：改成基于实测的说法：Docker Hub（auth.docker.io）确认不通，官方包管理器源实际能通但慢。加一条 Amendment 记下今天的实测结果。
> - **B. 先不改，单独再说**
> - **C. 先补测再决定**
>
> ➡️ **推荐 A**。
>
> **你的回答**：一起改，同一个 PR（推荐）

### D10 · 本机验证只验镜像构建，不动运行中的容器

- **决策**：`docker build` 两个镜像（默认路径各一次 + 传 `--build-arg` 看 RUN 指令回显就停），
  不跑 `scripts/deploy.sh`、不推 GitHub。
- **因为**：采纳推荐。不带 `-t` 的 `docker build` 只产 dangling 镜像，不会覆写 `:0.1.0` tag，
  运行中的三个容器全程不受影响。
- **放弃**：真跑一遍 deploy.sh（会 recreate web/server，有短暂中断）；推到 GitHub 验真实 CI
  （当时 `gh` 没装、也没有凭据）。
- **来源**：Q10、Q11

> **Q10 - 验证范围**：这次要验证到哪一步？（注意：不带 -t 的 docker build 不会覆写 quizzy-server:0.1.0 / quizzy-web:0.1.0 这两个 tag）
> - **A. 只验证镜像构建**／**B. 再加真跑一遍 deploy.sh**／**C. 再推到 GitHub 验真实 CI**
>
> ➡️ **推荐 A**。**你的回答**：只验证镜像构建（推荐）
>
> **Q11 - build-arg 验法**：传 build-arg 的那次构建，本机注定会失败（npmjs.org / Maven Central 在本机拉不动）。
> - **A. 看 RUN 回显就停**：用 `--progress=plain` 让 BuildKit 把每条 RUN 的实际命令打出来，看到 `RUN npm ci --registry=https://registry.npmjs.org` 就说明变量确实被替换进去了，然后直接终止。
> - **B. 完整跑到失败**／**C. 跳过，CI 上再验**
>
> ➡️ **推荐 A**。**你的回答**：看 RUN 回显就停（推荐）

---

### 未决问题

- `repo1.maven.org` 在**容器内**的可达性还没测完 — 卡在：上次 `docker build --build-arg
  MAVEN_MIRROR=central` 被提前中断，表里那个 200 只是宿主机直连。补一次完整构建即可定论。
- Docker Hub secrets 配不配 — 卡在：等用户决定。不配 CI 也能跑（登录步骤自动跳过），只是匿名
  限流风险仍在。
- `ubuntu-latest` 将于 2026-10-19 迁移到 Ubuntu 26 — 卡在：迁移尚未发生。要不要提前把 `runs-on`
  钉成 `ubuntu-24.04` 来规避，未定（目前倾向不动，等迁移那天看一眼）。
- 前端要不要加单测 / lint — 卡在：目前刻意不做，已写进 README「已知取舍」（**2026-09-26 注**：README 后来重写了，这条取舍现在记在 [../../docs/testing.md](../../docs/testing.md)）；CI 对前端的门禁只有
  类型检查与构建。
- 镜像分发（registry 与 tag）— 卡在：仍暂缓，等出现第二台需要跑这套东西的机器再定
  （见 `docs/todo/2026-09-20-TODO-镜像分发.md`）。

### 本次定下的叫法

- **门禁** = CI 里只做验证、不把产物投放到任何机器的那一半（本项目只做这个）。
- **冒烟构建** = 在干净环境 `docker build`，只验证镜像能从零构建出来，**不推任何 registry**。
- **部署（本项目）** = 用新镜像 recreate `web` / `server` 两个容器；**不是**把代码搬到另一台机器。
- **持续可交付 vs 持续部署** = 前者是「代码随时能上线、触发由人」，后者是「连发布也自动」。
  本项目属于前者，因为根本没有第二台机器可以自动发布过去。
