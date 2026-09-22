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

## Amendment 1（2026-09-22）：仓库公开后，默认值不得再用于签名

**背景**：仓库已推为 GitHub 公开仓库，上面这串默认值随之成为全网可知的字符串。
任何人拿它部署却不配 `.env`，就能用这把公开的钥匙签发任意 `userId` 的 token。
原本「默认值仅影响本机开发」的前提不再成立——它现在是一条通往真实攻击的路径。

**决策**：保留 yml 里的默认值（继续满足「宿主机裸跑不需要 `.env`」），但让它**无法被用于签名**：

1. `JwtUtil` 检测到密钥等于该默认值时，改为**每次启动随机生成**并打 WARN 日志。
   安全性兜住，代价只是宿主机重启后需重新登录——开发路径依旧可用，只是不再「稳定」。
2. `docker-compose.prod.yml` 改用 `${QUIZZY_JWT_SECRET:?...}`，`.env` 缺失时 compose 直接报错退出，
   杜绝「带着默认密钥上线」。

**未采纳**：直接删掉默认值（会让宿主机开发起不来，违反本 ADR 的原始决策）；
改成启动抛异常（同上，且失败模式更硬）。

**仍未解决**：`MYSQL_ROOT_PASSWORD` 的默认值 `123456` 同样已公开，
且现有数据卷就是用这个密码初始化的，改它需要重建/改密，另行处理。
