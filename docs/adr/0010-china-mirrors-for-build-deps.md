# 构建期依赖走国内镜像源

Status: accepted

`quizzy-server/docker-maven-settings.xml` 把 Maven 中央仓指向阿里云，`quizzy-web/Dockerfile` 里 `npm ci` 带 `--registry=https://registry.npmmirror.com`。这两处是**环境约束，不是项目偏好**：本机连不上 Docker Hub，`auth.docker.io` 直接返回 Bad Gateway，容器里按默认配置解析依赖必然超时失败。（2026-09-24 更正：这句的后半段不成立，见文末 Amendment 1。）

基础镜像同样受影响。解决办法是在宿主机上 `docker pull docker.m.daocloud.io/library/<img>` 再 `docker tag` 回官方名——这样不必改 Docker Desktop 的全局配置、也不用重启 Docker，BuildKit 之后能直接用本地镜像，不再去撞 Hub。

## Consequences

- 换到网络正常的机器上，这两处可以直接删掉；`docker-maven-settings.xml` 只在容器内被 `COPY` 到 `/root/.m2/`，不影响宿主机自己的 Maven 配置。
- 一劳永逸的做法是 Docker Desktop → Settings → Docker Engine 加 `"registry-mirrors": ["https://docker.m.daocloud.io"]`，配好之后上面那个 pull + tag 的步骤就不再需要。
- 以后新增基础镜像时记得先用同样的方式拉，否则构建会卡在 `load metadata` 报 Bad Gateway——这个报错完全看不出是网络问题。

## Amendment 1（2026-09-24）：官方源实测能通，国内源从「唯一解」降级为「更快的默认值」

**背景**：上面第一段把 Docker Hub 不通的结论外推到了包管理器源——「容器里按默认配置解析依赖必然超时失败」。今天补测之后，**这句的后半段不成立**。

**实测（2026-09-24，本机，宿主机直连）**：

| 源 | 结果 | 耗时 |
|---|---|---|
| `repo1.maven.org`（Maven 官方中央仓） | 200 | ~1.8s |
| `maven.aliyun.com`（Maven 国内镜像） | 200 | ~0.9s |
| `registry.npmjs.org`（npm 官方源） | 200 | ~5.9s |
| `registry.npmmirror.com`（npm 国内镜像） | 200 | ~1.3s |

**哪些是实测、哪些没测**：

- **实测**：四个源在宿主机上都返回 200。npm 官方源另有**容器内**证据——`docker build --build-arg NPM_REGISTRY=https://registry.npmjs.org ./quizzy-web` 一路跑到 `vite build`，依赖全部拉下来了。
- **没测**：`repo1.maven.org` 在**容器内**的表现。后端用 `MAVEN_MIRROR=central` 的整轮构建被提前中断，没跑完，所以表里那个 200 只是宿主机直连，不等于容器里也顺。
- **没变**：Docker Hub / `auth.docker.io` 在本机返回 Bad Gateway。这是**基础镜像**的 registry，本 ADR 第二段（pull + tag 换名）依然是硬约束，不受本次修正影响。

**结论怎么变**：

- 「官方源必然超时失败」不成立：官方源**能通，只是慢**（npm 官方比 npmmirror 慢约 4 倍，Maven 官方比阿里云慢约 2 倍）。
- 因此这两处不再是**环境约束**，而是**速度与稳定性上的偏好**。换到网络正常的机器上删掉它们，理由不需要「那边能连官方源」，只需要接受慢一点。
- 第二段（基础镜像走 Docker Hub）不受影响，仍是环境约束。

**决策：默认值仍然保持国内源**，理由从「不加就建不了」换成「加了更快更稳」，同时把它参数化以便被覆盖：

- `quizzy-server/Dockerfile` 新增 `ARG MAVEN_MIRROR=aliyun`，非 `aliyun` 时删掉容器里的 settings.xml，走官方中央仓。
- `quizzy-web/Dockerfile` 新增 `ARG NPM_REGISTRY=https://registry.npmmirror.com`，`npm ci --registry="$NPM_REGISTRY"`。
- CI 在境外 runner 上分别传 `MAVEN_MIRROR=central` 与 `NPM_REGISTRY=https://registry.npmjs.org`（ADR 0013），本机行为零变化。

**未采纳**：把默认值改成官方源。本机实测国内源快 2–6 倍，而境外 CI 已经能用 build-arg 覆盖，改默认值只会让本机变慢，换不到任何收益。

**仍未解决**：`repo1.maven.org` 在容器内的实际表现还没测完。补一次 `docker build --build-arg MAVEN_MIRROR=central ./quizzy-server` 跑到底就能定论。
