# 密码与密钥抽到 .env，application.yml 保留本机默认值

Status: accepted

数据库密码、库名、端口、数据目录和 JWT 密钥全部从项目根的 `.env` 读取，`.env` 不入库（模板是 `.env.example`）。同时 `application.yml` 里 datasource 与 jwt 写成 `${ENV:默认值}` 占位，并保留一套能直连本机 3306 的默认值——这样 dev 形态下在宿主机 `mvn spring-boot:run` 不需要 `.env` 也能跑，而容器里由 compose 注入真值覆盖。

## Considered Options

- **全部强制从环境读，不给默认值**：最安全，但本机开发每次都得先配齐 `.env`，少一个变量就启动失败。
- **继续把密码写在 yml / compose 里**：省事，但密码和密钥进了仓库，换个环境就得改受版本控制的文件。
- **占位符 + 本机默认值**（选定）：默认值只服务「宿主机开发」这一条路径，容器路径一律由 compose 显式注入，两条路径互不干扰。

## Consequences

- `application.yml` 里那串 `quizzy-default-jwt-secret-please-replace...` 看起来像疏漏，实际是刻意保留的本机兜底；把默认值删掉会让宿主机开发起不来。
- 默认值只是兜底。任何非本机环境都必须在 `.env` 里换成随机值（`python -c "import secrets;print(secrets.token_hex(32))"`）。
