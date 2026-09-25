# 配置

## 配置的三层

1. **`.env`**（不入库，模板是 `.env.example`）→ 供两个 compose 文件读取；
2. **compose** 把变量注入容器 → 覆盖后端配置；
3. **`application.yml`** 里 datasource 与 jwt 写成 `${ENV:默认值}` 占位。

为什么 yml 里保留默认值：让 dev 形态下在宿主机 `mvn spring-boot:run` **不需要 `.env` 也能跑**（[ADR 0011](../adr/0011-env-file-with-local-defaults.md)）。容器路径一律由 compose 显式注入真值，两条路径互不干扰。

## 变量清单在哪

**完整清单与默认值见 [.env.example](../../.env.example)**，它自带注释，是唯一真相源。**本文件不复制那张表**——复制出来的第二份一定会漂移。

本节只回答 `.env.example` 回答不了的问题：**改了某个东西，要重启什么。**

## 改配置后的生效范围

这是本文档的核心内容：

| 改了什么 | 需要做什么 | 说明 |
|----------|------------|------|
| `.env` 里的变量 | 只需 recreate 对应容器 | 走 `scripts/deploy.sh`（见 [deployment.md](./deployment.md)），**不要重建 mysql** |
| `application.yml` | **必须重新 build 镜像** | 它已被打进镜像，只 recreate 不会生效 |
| compose 的结构（`ports` / `volumes` / `depends_on`） | recreate 对应容器 | 同上走 deploy.sh |
| `.dockerignore` / Dockerfile | 重新 build 镜像 | 构建上下文变了 |
| Flyway 迁移脚本 | 应用启动自动执行 | ⚠️ 改**已执行过**的迁移前先备份，见 [backup.md](./backup.md) |

`scripts/deploy.sh` 只做「recreate + 等健康」；要重新 build 镜像也是它（`--build`），不需要分开记。

## 密钥：JWT 默认密钥已因仓库公开而失效

[ADR 0011 Amendment 1](../adr/0011-env-file-with-local-defaults.md) 记录了全过程，要点：

- `application.yml` 里那串默认密钥随公开仓库成为全网可知的字符串，**可被用来签发任意 `userId` 的 token**。
- 现在的处理：默认值仍保留（宿主机裸跑需要它），但一旦检测到用它，改为每次启动随机生成并打 WARN——所以宿主机重启后需要重新登录，这是有意的。
- prod 侧把 `QUIZZY_JWT_SECRET` 设成了 compose 必填项，缺了直接报错退出。

**轮换后所有已登录用户都需要重新登录**（token 签名变了）。任何在修复之前部署过的实例都要轮换一次，见 [../../CHANGELOG.md](../../CHANGELOG.md) 的 Security 段。

⚠️ 仍未解决：`MYSQL_ROOT_PASSWORD` 的默认值同样已公开，且现有数据卷是用它初始化的，改它需要重建或改密——见 ADR 0011 末尾「仍未解决」。

## 数据目录 `MYSQL_DATA_DIR`

它决定数据落在宿主机哪个目录，dev 与 prod **共用同一份**（[ADR 0007](../adr/0007-dev-prod-compose-with-bind-mount.md)）。

⚠️ **改它不是「换配置」，是换库**——新路径下一次启动会初始化出一个空库。除非你清楚自己在做什么，否则别动它。

## 备份相关变量

`BACKUP_DIR` 与保留策略相关变量同样在 `.env.example` 里，用途见 [backup.md](./backup.md)。
