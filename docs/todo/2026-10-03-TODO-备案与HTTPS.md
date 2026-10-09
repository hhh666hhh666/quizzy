# 待办：备案与 HTTPS

创建：2026-10-03 · 状态：待办 · 优先级：高

## ICP 备案与 HTTPS 切流已完成（2026-10-09），剩公安备案 + 小程序白名单

**已完成**（2026-10-09）：域名解析（`@` 与 `www` 均指向服务器）；宝塔把 `server_name` 切到主域名并申请 Let's Encrypt 证书（有效期至 2027-01-07，续期归宝塔计划任务）；开启「强制 HTTPS」（80 强制 301 到 443）。切流后远程验证全绿：HTTPS 首页 200、证书链 verify ok、HTTP 自动跳转、`/api/` 穿透到 Spring（未登录返回业务 JSON）。证书的申请与续期交给宿主宝塔（[ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md) Amendment 1），不自建 acme.sh；两阶段形态对照模板见 [deploy/nginx/quizzy-site.conf](../../deploy/nginx/quizzy-site.conf)。被否过的选项留档：先只上 IP + 自签证书——⛔ 小程序**不接受** IP，等于没解决。

⚠️ **已知小缺口**：证书 SAN 只覆盖主域名、未覆盖 `www`（手输 www 会撞证书告警页）。修补方式：宝塔站点 → 域名管理加 `www` → SSL 页勾上重新签一次。小程序「request 合法域名」只填主域名，**不受影响**。

**还没做的**：

- **公安备案**（ICP 通过后 **30 日内**，见 [ADR 0017](../adr/0017-public-signup-service.md)）——主人手动操作，唯一有 deadline 的项；办完本文件结案归档
- 小程序「request 合法域名」白名单 → 已移交 [`2026-10-03-TODO-移动端与小程序落地.md`](./2026-10-03-TODO-移动端与小程序落地.md)（跟着 AppID 与小程序编译目标一起做）
- `docs/operations/deployment.md` 补 H5 `/m/` 对外入口 → 移动端 TODO 的「H5 `/m/` 部署」条目已覆盖，不在此重复
