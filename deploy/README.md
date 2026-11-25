# deploy/

放在这台服务器上、**但不由应用容器持有**的部署侧配置模板。真正的操作步骤与顺序见
[部署文档](../docs/operations/deployment.md)，这里不复制。机器级事实（主机规格、端口总表、域名解析与 SSH / 隧道接入）
统一看 GitHub 仓库 [hhh666hhh666/servers](https://github.com/hhh666hhh666/servers) 的 `aliyun/` 目录（不假设本机有克隆副本）。

- `nginx/quizzy-site.conf` —— 宿主（宝塔）nginx 的站点反代模板：终止 TLS，并把站点转到 `127.0.0.1:8081`。
  ⚠️ 真正生效的那份在服务器宝塔面板里，本文件是**对照模板**。
- `.env.cloud.example` —— 云端 `.env` 模板（复制到服务器 `/srv/quizzy/.env`）。

为什么放在这里而不是 `docs/`：它们是**要放到服务器上执行的文件**，不是讲述性文档；
`docs/` 只写「怎么用、去哪儿看」。
