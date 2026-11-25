---
description: 服务器入口与部署凭据红线——本项目的服务器别名、档案出处（GitHub servers 仓库）、流水线耦合点与红线；上服务器 / 动部署凭据前先读。
alwaysApply: true
---

# 服务器入口与红线

**触发**：任何要 SSH 上线上服务器（排障 / 部署 / 查状态）、或要动 `DEPLOY_SSH_KEY` 这类部署凭据的时刻。

## 怎么连 & 查机器事实

- 本项目部署在 Host 别名 **`aliyun`** 上（`ssh aliyun`）。
- **机器级事实以 GitHub 上的服务器档案仓库为单一真相源**（不要假设本机存在同级目录，换机器可能没有）：
  - 仓库：[hhh666hhh666/servers](https://github.com/hhh666hhh666/servers)（`aliyun/` 目录）
  - `aliyun/README.md`：公网 IP、机器规格、端口分配总表、域名解析（灰云 / 橙云）
  - `aliyun/ssh访问.md`：直连与 Cloudflare 隧道两种接入配置、端口分工、断连排障
  - `aliyun/cloudflare隧道.md`：隧道维护与回滚
- 查阅方式：直接读 GitHub 页面或用 `gh api repos/hhh666hhh666/servers/contents/aliyun/<文件名>` 读取，**不在本仓库复制机器级事实**。

## 与本项目的耦合点

- **SSH 22 端口与 `DEPLOY_SSH_KEY`**：发布流水线（`.github/workflows/release.yml`）仍走 22 端口部署，凭据存在 GitHub Secrets（`DEPLOY_SSH_KEY` / `DEPLOY_SSH_KNOWN_HOSTS`）。**要关 22 端口或轮换部署密钥，必须先同步改本仓库流水线与 Secrets**。
- **应用部署与排障**：部署与回滚看 `docs/operations/deployment.md`，排障看 `docs/operations/runbook.md`，本文件不复述。

## Windows（git bash）下的两个通用坑

1. **权限**：ssh 可能以 "Permissions are too open" 拒绝从 Windows 侧读到的 pem → 先 `cp` 到 `/tmp` 再 `chmod 600`，用副本连。
2. **长连接**：端口转发隧道等长连接要在**主人本机的终端**里跑，别交给 AI 会话的后台任务（会被回收）。

## 红线

- 私钥**内容**不进文档、不进提交、不进消息；具体私钥路径以当前机器 `~/.ssh/config` 的 `IdentityFile` 为准，**不把某台开发机的盘符或绝对路径写进仓库**。
