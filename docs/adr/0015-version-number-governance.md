# 版本号以 `git tag` 为准，且只经构建参数注入

Status: accepted

要做到「关于」里显示当前版本号，先得回答「当前版本号是哪个数」。一查发现这个仓库里有四个互不同步的值：`git tag` 是 `v1.0.0`，`package.json` 是 `0.1.0`，`pom.xml` 是 `0.1.0-SNAPSHOT`，两个 compose 的 `image` tag 也是写死的 `0.1.0`；`OpenApiConfig` 里那个 `.version("v1")` 更是个错值——它是**产品代次**，却占着 API 版本的位置。而且**没有任何通道能把版本号送进前端**：`vite.config.ts` 没有 `define`，两个 Dockerfile 里唯一的 `ARG` 都是换源用的，后端既没有 actuator 也没有版本接口。

所以这一轮一并定下三件事。① 沿用 `CHANGELOG.md` 已有的公约：**`git tag` 是唯一真相源**，另外三处是它的手写镜像，靠 `scripts/check-version.sh` 校验；这个校验**只在推 tag 时跑**（`ci.yml` 的 `version` job，其余 job 在 tag 上被 `if` 挡掉）。② 版本号走**构建参数**注入两端产物——`scripts/deploy.sh` 算好 → compose 的 `build.args` → Dockerfile → 前端 `vite define` 成 `__APP_*__` 常量、后端进 `ENV QUIZZY_VERSION` 再由 `quizzy.version` 读取。**不新增任何接口**。③ 镜像 tag 跟随版本号（`quizzy-web:1.1.0`），并取消「v1 产品代次」的说法——「关于」弹窗显示的主行是 tag 字面（`v1.1.0`），副行只在 `HEAD` 领先 tag 时出现（`+4 提交 · 0d25d67`）。

## Considered Options

- **前端开机读 `package.json` 的版本**：零参数零接口。但仓库自己已经声明「包版本与发布版本无关」，等于在界面上公开一个假版本号；而且它是构建期常量，`vite build` 之后 `package.json` 根本不在产物里。
- **后端加一个 `GET /api/meta`，前端运行时拉**：唯一的好处是前后端版本能分开显示，且不需要重新构建前端就能换版本号。但代价是多一个接口、一次启动时的请求，还要给后端单独造一条注入通道——而 nginx 只发静态产物，本可以完全避开运行时依赖。
- **前端放一个常量文件手写版本号**：最省事。但会多出一个「改版本号要记得改这里」的地点，正是 `CHANGELOG.md` 那张表想消灭的东西。
- **引入根目录 `VERSION` 文件作为单一来源，脚本回写三处**：最彻底。但要让 Maven 从外部读版本得用 CI-friendly 的 `${revision}`，会牵动 `pom.xml` 的坐标与 `mvn install` 语义；收益不抵复杂度。
- **镜像 tag 里带 commit（`1.1.0-0d25d67`）**：同一版本号下多次构建各留一份，回滚粒度最细。但 compose 的 `image:` 必须是**字面量**，tag 里带 commit 就只能改成 `${QUIZZY_VERSION}` 变量替换，而 `.env` 已按 ADR 0011 划给密钥与本机配置。且每次构建都留一份镜像，磁盘会持续增长。
- **顺手把「产品代次 v1」正典化，写进 `CONTEXT.md`**：保留这个概念并给它一个正式定义。但「产品代次」只是为了解释一个命名意外而存在的；删掉意外比给意外上户口便宜。

## Consequences

- **校验放在「打 tag 的那一刻」，不是「一直相等」。** 两个 tag 之间 `HEAD` 必然领先好几个提交，那时三处包版本仍写着上一个版本号，是**正常**状态。硬比必然误报，而误报会让人养成「红着也照合」的习惯（同 ADR 0014 的取舍）。所以 `scripts/check-version.sh` 在非 tag 提交上直接跳过并以 0 退出，可以安全地挂在 CI 与本地自检里。
- **`pom.xml` 保留 `-SNAPSHOT`，比对时剥掉后缀。** `-SNAPSHOT` 的含义正是「这个坐标还没发布过」；若写死成 `1.1.0`，两次发版之间的 `mvn install` 就会用未发布代码覆盖 `~/.m2` 里的正式坐标。校验脚本为此多剥一次后缀。
- **回滚能力变了，但没到「任意提交」。** 镜像 tag 跟版本号走之后，`--build` 只会重打当前版本号那一份，别的版本号的镜像还在，所以**退回上一个已发版本可以复用本地镜像、不必重新构建**（构建失败也会因此少一类）。但同一版本号内的多次构建仍然互相覆盖——保证是「每个已发版本保有一份镜像」，不是「每个提交一份」。`scripts/deploy.sh` 的回滚提示已按此改写。
- **compose 的 `image:` 是字面量，所以「改了 tag 忘了同步 compose」会静默毁掉回滚能力**：新代码会被打上旧版本号的 tag，把上一份镜像覆盖掉。`check-version.sh` 会把这种情况判红，`deploy.sh` 在重建前也会告警（只告警不中断——克隆下来还没打过 tag 的仓库会误报）。
- **`APP_*` 四个变量不进 `.env`**，由 `deploy.sh` 计算后 `export` 给 compose。所以**绕过 `deploy.sh` 直接 `docker compose up` 的部署会拿到 `dev`**，这个坑写在了部署文档里。
- **`git` 只在宿主机上有**：两个 `.dockerignore` 都排除 `.git/`，容器里跑不了 `git describe`。这是版本号必须在构建前算好、只能经 build-arg 传入的根本原因。
- **前端 Dockerfile 的 `ARG` 位置是性能约束**：必须排在 `npm ci` 那一层之后、`npm run build` 之前。ARG 值一变，它之后的层缓存全失效——放到前面会让每次发版都白重装一遍依赖。
- **后端 Dockerfile 的运行时阶段要重新声明一次 `ARG`**：`ARG` 的作用域限于声明它的那个 stage，构建阶段那个出了 stage 就不可见。
- **`npm run dev` 下显示 `dev`**：宿主机直跑拿不到 build-arg，这是刻意的显式退化，不允许留空——空值在界面上看不出来。
- **`OpenApiConfig` 的 `.version()` 从写死的 `"v1"` 改为注入值**，swagger UI 上显示的版本因此变成真实版本号。
- ⚠️ **`actions/checkout` 默认是 `fetch-depth: 1` 的浅克隆，拿不到 tag**。`version` job 是本仓库唯一需要 `fetch-depth: 0` 的地方；其余 job 不需要 tag，保持默认即可。
- **推 tag 会触发 CI 了**（此前 `ci.yml` 只监听 `push: branches: [master]`，推 tag 完全不触发）。为了保住「tag 推送不白烧 runner 与 Docker Hub 匿名拉取配额」这个性质，四个重 job 各加了一条 `if: "!startsWith(github.ref, 'refs/tags/')"`——tag 指向的提交在 master 上已经完整验过一遍，再跑一遍是纯浪费。

相关：0011（`.env` 的职责边界）、0013（CI 只做门禁）、0014（误报比漏报更贵的取舍）、0006（单一信息源纪律）。

## Amendment 1（2026-10-02）：compose 的 `image:` 不再是字面量，手写点从三处减到两处

上面「镜像 tag 必须是字面量」这条**在搬到云上之后不再成立**。云上的形态是「CI 构建推 ACR → 服务器拉取」（[ADR 0016](0016-cloud-deploy-with-release-pipeline.md)），compose 的 `image:` 变成 `${ACR_REGISTRY}/${ACR_NAMESPACE}/quizzy-<svc>:${APP_VERSION}`：

- **手写点 3 → 2**：只剩 `quizzy-web/package.json` 与 `quizzy-server/pom.xml`。compose 那一处**量变了**——从「比对值」变成「比形状」。
- **`scripts/check-version.sh` 相应改造**：原来那两条按字面量提版本的 `sed` 会取空值而误报 FAIL；现在改成**结构断言**「`image:` 必须以 `.../quizzy-<svc>:${APP_VERSION}` 结尾」，防的是有人把它改回写死值——那会毁掉「版本 tag 即回滚落点」。
- **「改了 tag 忘了同步 compose 会静默毁掉回滚能力」这个风险随之消失**：镜像在 ACR 里、每个版本各留一份，compose 不再持有版本号。`deploy.sh` 里那条按字面量 grep 的一致性告警一并消失（该脚本已被 `scripts/server-deploy.sh` 取代并删除）。
- ⚠️ **新增一处不对称**：git tag 带 `v`（`v1.1.0`），镜像 tag 不带（`1.1.0`）。compose **不支持** `${VAR#v}` 这类字符串截断，所以剥前缀只能发生在传参之前——由 `release.yml` 的 `version` job 与 `server-deploy.sh` 各自完成，`check-version.sh` 的结构断言兜住这个形状。
- 上面 Consequences 里「`deploy.sh` 算好 → compose 的 `build.args`」这条注入链，现在改由 `.github/workflows/release.yml` 的 build-args 承担；`APP_*` 仍然不进 `.env`。
- 第 31 条「推 tag 会触发 CI、四个重 job 各加一条 `if` 挡掉」**不再成立**：tag 触发已从 `ci.yml` 移除，tag 的全部处置权收进 `release.yml`，那四条 `if` 随之删除。
