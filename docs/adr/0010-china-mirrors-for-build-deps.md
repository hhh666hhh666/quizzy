# 构建期依赖走国内镜像源

Status: accepted

`quizzy-server/docker-maven-settings.xml` 把 Maven 中央仓指向阿里云，`quizzy-web/Dockerfile` 里 `npm ci` 带 `--registry=https://registry.npmmirror.com`。这两处是**环境约束，不是项目偏好**：本机连不上 Docker Hub，`auth.docker.io` 直接返回 Bad Gateway，容器里按默认配置解析依赖必然超时失败。

基础镜像同样受影响。解决办法是在宿主机上 `docker pull docker.m.daocloud.io/library/<img>` 再 `docker tag` 回官方名——这样不必改 Docker Desktop 的全局配置、也不用重启 Docker，BuildKit 之后能直接用本地镜像，不再去撞 Hub。

## Consequences

- 换到网络正常的机器上，这两处可以直接删掉；`docker-maven-settings.xml` 只在容器内被 `COPY` 到 `/root/.m2/`，不影响宿主机自己的 Maven 配置。
- 一劳永逸的做法是 Docker Desktop → Settings → Docker Engine 加 `"registry-mirrors": ["https://docker.m.daocloud.io"]`，配好之后上面那个 pull + tag 的步骤就不再需要。
- 以后新增基础镜像时记得先用同样的方式拉，否则构建会卡在 `load metadata` 报 Bad Gateway——这个报错完全看不出是网络问题。
