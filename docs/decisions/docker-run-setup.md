# 决策日志：quizzy 怎么在 Docker 上跑起来

## 2026-09-20 16:44 · 第 1 次 grill

起因是「我要在 docker 上运行」这一句话。开问前先摸了环境，查到两条事实：
`docker-compose.prod.yml` 三个服务齐全、架构层面是通的；但当时**直接 `up --build` 必失败**——
仓库里没有 `.dockerignore`，宿主机 159MB 的 `node_modules` 里只有 `@esbuild/win32-x64` 和
`rollup-win32-x64-*`，`COPY . .` 会把容器里刚装好的 Linux 依赖整层覆盖，`npm run build` 一定崩。

### D1 · 两套运行形态并存

- **决策**：同时保留两种形态——开发时只把 MySQL 放容器、前后端在宿主机跑；演示/验证时三容器全起。
- **因为**：采纳推荐（D 选项）。日常要改代码，全容器每改一行都要重建镜像，本地开发没法用；但 prod compose 已经写好，演示时一次 `up` 就能起来。
- **放弃**：C「容器里开发、源码挂载带热重载」——配置成本高，本机直接跑更简单。
- **来源**：Q1
- 相关：[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)

> **Q1 - 「在 Docker 上运行」是哪一种形态？**：A. 全容器（prod compose，三个容器，开 http://localhost）／B. 只把 MySQL 放容器，前后端仍在宿主机跑（等于现状不变）／C. 容器里开发：源码挂进去，带热重载／D. 前两者都要 —— dev 用 B，演示用 A
>
> ➡️ 推荐：A（或 D）。prod compose 已经写好，显然是奔着这个去的；但日常还要改代码的话 D 更实际，两边共用同一个数据卷就行。
>
> **你的回答**：D，并且追加要求「把 mysql 的数据卷挂载在 E:\develop\docker\mysql8\var\lib\mysql」

### D2 · MySQL 数据绑定挂载到 E 盘，dev 与 prod 共用同一份

- **决策**：数据目录绑定挂载到 `E:/develop/docker/mysql8/var/lib/mysql`，两个 compose 文件用同一个路径，不再用命名卷。
- **因为**：用户在 Q1 里主动指定。数据在宿主机磁盘上肉眼可见、随手能拷；两种运行方式切换时数据不丢。实测 MySQL 8.4 的 InnoDB 能在 Windows 绑定挂载上正常初始化。
- **放弃**：命名卷（Docker 推荐做法、性能最好）——数据藏在 Docker 内部路径里，出问题时要靠 `docker cp` 才能拿出来。
- **来源**：Q1（用户追加）
- 相关：[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)

### D3 · 只在自己本机跑，不对外

- **决策**：纯本机 Docker Desktop，`localhost` 自己访问，不规划局域网或公网。
- **因为**：采纳推荐。
- **来源**：Q2
- 后果：默认的 root 密码与后端 8080 端口映射得以保留；哪天要放到能被别人访问的环境，密钥和端口映射必须重来一遍。

> **Q2 - 跑在哪儿、谁来访问？**：A. 就本机 Docker Desktop，localhost 自己看／B. 局域网，同网段其他机器/手机也要能开／C. 云服务器，有公网 IP 甚至域名／D. 云服务器 + 域名 + HTTPS
>
> ➡️ 推荐：先按 A 说清楚。这题答案会直接决定后面几件事：C/D 的话，root/123456 的数据库密码和 application.yml 里那串默认 JWT 就必须换掉，而且后端 8080 不该直接对宿主机暴露；纯 A 的话这些可以先用默认值跑起来再说。
>
> **你的回答**：A

### D4 · 加 .dockerignore，并把 Maven / npm 换成国内源

- **决策**：前后端都加 `.dockerignore` 排除宿主机产物；Maven 走阿里云镜像、npm 走 npmmirror。
- **因为**：采纳推荐。且不选它构建根本过不去——开问前已查实宿主机 Windows 版 node_modules 会覆盖容器内 Linux 依赖。
- **放弃**：B「宿主机先构建，Dockerfile 只搬运产物」——看起来快，但把构建产物和 Windows 深度绑定，等于放弃了 Docker 最大的好处。
- **来源**：Q3
- 相关：[ADR 0009](../adr/0009-glibc-build-image-and-dockerignore.md)、[ADR 0010](../adr/0010-china-mirrors-for-build-deps.md)

> **Q3 - 镜像怎么构建？（解决 Windows node_modules 那个问题）**：A. 加 .dockerignore + 换国内源：忽略 node_modules/dist/target，容器里 maven 走阿里云镜像、npm 走 npmmirror。构建干净可复现，第一次大概要几分钟下依赖／B. 宿主机先构建，Dockerfile 只搬运产物：本机 mvn package / npm run build 已经有产物了，Dockerfile 直接 COPY。构建秒级完成，但镜像不再「自包含」，换台机器就得先本地构建／C. A + 打好的 jar/dist 提交进镜像仓库：CI 里构建、推镜像，运行处只 pull
>
> ➡️ 推荐：A。B 看起来快，但「本地构建产物」和 Windows 深度绑定，等于放弃了 Docker 最大的好处；你现在网络要下 maven 全量依赖 + npm 全量依赖，换源之后也就几分钟，只痛一次。
>
> **你的回答**：A

### D5 · 密码与密钥抽到 .env，不入库

- **决策**：数据库密码、库名、端口、数据目录、JWT 密钥全部从项目根 `.env` 读取，`.env` 进 `.gitignore`；模板是 `.env.example`；JWT 密钥现场随机生成。
- **因为**：采纳推荐。
- **放弃**：B「保持明文写在仓库」——换环境就得改受版本控制的文件；C「额外给 MySQL 建专用账号」——本机自用阶段不必，等真要上公网再说。
- **来源**：Q4
- 相关：[ADR 0011](../adr/0011-env-file-with-local-defaults.md)

> **Q4 - 密码和密钥怎么处理？**（当时 root/123456 和 application.yml 里那串 quizzy-default-jwt-secret-please-replace... 都是明文写在仓库文件里的）A. 抽到 .env（compose 读，.gitignore 掉），JWT secret 现场生成一个随机值 —— 本机自用也这么做，养成习惯／B. 保持现状，先把服务跑通，密钥的事留到真要上公网时再说／C. 抽 .env + 给 MySQL 建一个专用账号（不用 root），权限只给 quizzy 库
>
> ➡️ 推荐：A，如果是 C/D 场景则升级成 C。root 直连在任何多人可见的环境里都不合适，成本也就多写几行 compose。
>
> **你的回答**：A

---

## 2026-09-20 22:52 · 第 2 次 grill

第一轮落地并跑通后，就剩下的三个未决项继续问。中途用户追问了「healthcheck 是什么」「具体有什么用」，
我实测了当前容器状态并如实说了：在这套单机自用的场景下，启动顺序的收益有限（nginx 的 resolver 已兜住），
主要是买个可观测性。

### D1 · 备份先落项目内 .backup/，最终位置待定

- **决策**：备份暂时落在 `E:\storage\program\workbuddy\quizzy\.backup`，由 `.env` 的 `BACKUP_DIR` 控制；最终放在哪里以后再定，并写进待办。写了 `scripts/backup-mysql.sh`（导出后清理超过 `BACKUP_KEEP_DAYS` 天的旧文件，空文件判定失败）。
- **因为**：采纳推荐（加定时备份），但位置由用户指定为先放项目内、以后再说。
- **放弃**：暂时接受「与数据同在 E 盘」这个风险——已明确记录这只是复制不是备份。
- **来源**：Q1
- 相关：待办见 [docs/todo/2026-09-20-TODO-数据库备份.md](../todo/2026-09-20-TODO-数据库备份.md)

> **Q1 - 数据备份怎么做？**：E 盘数据目录现在没有任何保护。要不要加个定时 mysqldump（比如每天一份、留最近 7 天）？另外：要不要顺手在 E 盘留一份 latest？谁来做定时（Windows 任务计划程序 vs 这里的自动化定时任务）？
>
> ➡️ 推荐：加，脚本放仓库里。你的题库是唯一资产，容器可以重建，题不能。调度方式倾向 Windows 任务计划程序，因为备份不该依赖一个会关掉的东西。
>
> **你的回答**：「还是先放在 E:\storage\program\workbuddy\quizzy\.backup，具体要放哪以后再说，在项目目录下新增一个待办md写进去」

### D2 · 加后端 healthcheck，web 等它 healthy 才启动

- **决策**：给 server 加 healthcheck 探 `/v3/api-docs`，web 的 `depends_on` 改成 `condition: service_healthy`。
- **因为**：采纳推荐。项目没引 actuator，Spring Boot 本身没有 `/actuator/health`，而 `/v3/api-docs` 一定返回 200 且能证明上下文初始化完毕。冷启动实测 mysql → server → web 依次等 healthy，总 17 秒。
- **放弃**：引 actuator 换 `/actuator/health`——为一个探测端点加一个依赖不划算；bash `/dev/tcp` 探端口——只能证明 Tomcat 在监听。
- **来源**：Q2
- 相关：[ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)

> **Q2 - server / web 要不要加 healthcheck？**：现在 nginx 靠 resolver 127.0.0.11 兜住了启动时序，能用但脆弱。
>
> ➡️ 推荐：加后端 healthcheck，web 的 depends_on 改成 condition: service_healthy，成本很低。
>
> **你的回答**：「其他按你推荐的来」

### D3 · 镜像暂不推 registry

- **决策**：保持只在本地打 `quizzy-server:0.1.0` / `quizzy-web:0.1.0`，不推任何 registry。
- **因为**：采纳推荐。Q2 已定纯本机自用，推了也没有消费者。
- **来源**：Q3
- 相关：待办见 [docs/todo/2026-09-20-TODO-镜像分发.md](../todo/2026-09-20-TODO-镜像分发.md)

> **Q3 - 镜像 tag 与分发？**：现在是本地 quizzy-server:0.1.0 / quizzy-web:0.1.0，没推任何 registry。
>
> ➡️ 推荐：暂时不推，你 Q2 选了纯本机自用，推 registry 目前没有消费者。
>
> **你的回答**：「其他按你推荐的来」

### D4 · 未决事项拆成 TODO/ 目录，按主题分文件

- **决策**：根目录的 `TODO.md` 改成 `TODO/` 目录，文件名 `YYYY-MM-DD-TODO-<主题>.md`（日期记创建日期，改文件不改名），`README.md` 作索引并固化「已定不用再想」清单。
- **因为**：用户在会话中主动提出。按主题拆而不是按时间堆，是因为备份和镜像分发的紧迫程度差太远，混在一张清单里每次都得重读一遍。
- **来源**：会话中提出

> **后续状态（2026-09-26）**：目录后来从仓库根移到了 `docs/todo/`（理由：文档类内容集中管理），
> 用 `git mv` 搬的，历史保留。命名规则与「按主题分文件」的做法都没变，本条只有路径变了。

### 未决问题

- **备份最终放哪** — 卡在：用户说以后再说。本机 C 只剩 11G（96% 满），E 与数据同盘属同一故障域，只剩 D 盘（85G 空闲）合理。
- **谁触发定时备份** — 卡在：Windows 任务计划程序 vs 这里的自动化定时任务，二选一未定。脚本已写好但未挂调度。
- **后端 8080 是否继续暴露到宿主机** — 卡在：当前内网自用可接受，取决于以后会不会放到别人能访问的环境。
- **日志轮转** — 卡在：还没决定用 compose 的 `logging.options.max-size` 还是接 logrotate。
- **探测端点是否换成 actuator** — 卡在：等 `/v3/api-docs` 每次生成文档的开销真的碍眼再定。
- **镜像 tag 与分发** — 卡在：等出现第二台机器需要跑这套东西。

### 本次定下的叫法

- **dev 形态** = 只把 MySQL 放容器，前后端在宿主机跑（`mvn spring-boot:run` / `npm run dev`）
- **prod 形态** = 前端、后端、数据库全容器，一次 `docker compose up` 起来
- **故障域** = 备份与数据落在同一块盘时，那块盘挂了两边一起没，此时备份只是「复制」不是「备份」

> 这三个都是运维叫法，不是 quizzy 的领域概念（题目、会话、错题本那些）。按 CONTEXT.md 的定位它只收领域术语，
> 不建议把这几个塞进去。
