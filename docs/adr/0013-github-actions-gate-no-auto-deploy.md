# CI 只做门禁，不做部署

Status: accepted

push 或 PR 到 master 时，GitHub 托管 runner 上跑四条并行门禁：后端 `mvn test`、前端 `npm ci` + `typecheck` + `build`、两个镜像各自在干净 Linux 上 `docker build` 冒烟。**没有任何 job 会把产物送到任何机器上去跑**。仓库里虽有完整的 compose 编排，但部署目标只有本机 Docker Desktop——`docs/decisions/docker-run-setup.md` 的 D3 已经定了「纯 localhost 自用，不规划局域网或公网」，没有第二台机器可以推。所以这里是**持续可交付**而不是持续部署：代码随时处于能上线的状态，上线动作由 `scripts/deploy.sh` 完成，触发它的永远是人。

## Considered Options

- **把 self-hosted runner 装在开发机上，让 CI 直接 `docker compose up`**：最贴近「push 完就更新」。但 GitHub 官方明确警告**不要**在 public 仓库上使用 self-hosted runner——任何能 fork 并开 PR 的人都能让 runner 在那台机器上执行任意代码，进而拿到 secrets、逃出 sandbox、访问本机网络。这台机器上还有题库、笔记和浏览器 session，全在同一个信任域里。收益只是省掉一次手动 `deploy.sh`，代价是把整台开发机暴露给互联网。不用。
- **买一台云服务器，云端 CI 构建后 SSH 过去部署**：这样才有真正的持续部署。但这是一个个人刷题工具，为一台常驻机器付月费，还要重新处理密钥、端口与镜像分发（`docs/todo/2026-09-20-TODO-镜像分发.md` 里那条待办就是卡在这里）。现在不做。
- **GitHub 托管 runner 做门禁 + 本机手动 `scripts/deploy.sh`**（选定）：运行环境与开发机物理隔离，零攻击面；公共仓库的托管 runner 分钟数免费。代价是本机更新仍需手动——但这个动作本来就该在眼皮下发生，项目里有 Flyway 迁移和题库数据，不该被一条无人值守的流水线自动改动。
- **完全不上 CI**：省一个文件和几个 secrets。但「这次改动到底能不能构建、能不能过类型检查」只能靠手跑，而两次改动之间隔几周就会忘记该跑哪几项。

## Consequences

- **CI 绿了 ≠ 本机已更新。** 部署这一步必须由人执行 `bash scripts/deploy.sh`。
- **CI 里不能 `docker compose up`。** 仓库里没有 `.env`，而 `${QUIZZY_JWT_SECRET:?…}` 是 0011 刻意设的必填语法，缺变量时 compose 在读配置阶段就退出——那会变成一个看起来像 CI 挂了的假失败。同理 `MYSQL_DATA_DIR` 的默认值 `E:/…` 在 Linux runner 上毫无意义。所以 CI 只验证「镜像能被构建出来」，不验证「服务能跑起来」。
- **四个 job 并行，冒烟不设 `needs`。** 它们回答的是「Dockerfile 在干净 Linux 上还能不能从零构建」——0009（alpine 缺 esbuild 二进制）和 0010（依赖源）记的那类坑**只在这种环境下暴露**，本机因为缓存命中永远发现不了。若让它们等前后端，一旦前后端先红，最需要验证的那个信号就被整个吞掉了。
- **Docker Hub 限流必须提前处理。** 两个 `docker build` 每次要拉 `maven:3.9.9-eclipse-temurin-21`、`eclipse-temurin:21-jre`、`node:22-slim`、`nginx:alpine` 四个基础镜像。匿名限额 100 次 / 6 小时**按共享出口 IP 计**，GitHub runner 的出口 IP 是共用的，很容易撞顶。所以配 `DOCKERHUB_USERNAME` / `DOCKERHUB_TOKEN` 两个 secrets 走 `docker/login-action`，提到 200 次 / 6 小时且按账号计。**没配 secrets 时 CI 照样能跑**，只是退回匿名限额；public 仓库的 fork PR 拿不到 secrets，同样自动降级。
- ⚠️ **但跳过条件不能写成 `if: ${{ secrets.X != '' }}`**——GitHub 不允许在 `if:` 里引用 `secrets`，会报 `Unrecognized named-value: 'secrets'` 并让**整个 workflow 文件作废、一个 job 都起不来**。首次落地就是这么踩到的，故障表现是"run 失败但 jobs 数为 0、run 的 name 退回成文件路径"，很容易误判成别的毛病。正确做法是在 job 级 `env` 里把它折算成一个普通布尔（`HAS_DOCKERHUB: ${{ secrets.A != '' && secrets.B != '' }}`），step 再判 `env.HAS_DOCKERHUB == 'true'`——这样密钥值本身不进 env，只传"有没有"。
- **两个 Dockerfile 的依赖源用 build-arg 参数化，这是对 0010 的一个显式例外。** 0010 说国内镜像源是**本机**的网络约束，「换到网络正常的机器上可以直接删掉」。这里不删而是改成 `ARG MAVEN_MIRROR=aliyun` / `ARG NPM_REGISTRY=https://registry.npmmirror.com`，因为本机还要继续用国内源，硬改会让本机构建立刻拉不动。默认值保持国内源（本机行为零变化），CI 用 `--build-arg` 切回官方源。`ARG` 必须声明在使用它的那个构建阶段内、且排在用到它的指令之前——写在第二个 `FROM` 之后取不到值。
- **`npm run typecheck` 此前从未被任何流程执行过**，首次纳入门禁时暴露出 7 个类型错误（`vite build` 走 esbuild 只剥离类型不做检查，一直是绿的）。这是存量债，已在本次一并修掉；其中 `QuickQuizView` 那个是真隐患——`types: string[]` 允许把任意字符串塞进抽题规则发给后端。**typecheck 现在是硬门禁，前端 `strict: true` 真正开始生效。**
- **`scripts/deploy.sh` 永不重建 mysql。** 用 `--no-deps` 确保 `depends_on` 不会把数据库拉进这次操作，且 mysql 只在「没跑起来」时被拉起一次。数据在宿主机 `MYSQL_DATA_DIR`，不在容器里（见 0007）。

相关：0010（构建期依赖源）、0011（`.env` 与 `:?` 必填）、0007（数据绑定挂载）、0009（构建镜像与 .dockerignore）。
