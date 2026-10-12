---
description: 要 SSH 上线上服务器时：密钥位置（本机事实）、连接别名、Windows 权限坑与「什么能写进仓库」的红线。
alwaysApply: true
---

# 本机 SSH 与服务器入口

**触发**：任何要 SSH 上线上服务器（排障 / 部署 / 查状态）的时刻。这里记的是**这台机器的事实**——换机器、或密钥再搬家时，本文件与 `C:\Users\lzh\.ssh\config`、`docs/operations/runbook.md` 的隧道示例要一起改（2026-10-12 迁过一次：`~/.ssh/quizzy/` → `~/.ssh/agent/`）。

## 怎么连

```bash
ssh aliyun
```

别名在 `C:\Users\lzh\.ssh\config`：`HostName 8.137.172.6`、`User root`、`IdentityFile` 指向私钥。

- **私钥位置**：`C:\Users\lzh\.ssh\agent\workbuddy.pem`。需要显式写路径时用 `~/.ssh/agent/workbuddy.pem` 的写法（git bash 的 `~` 即 `C:\Users\lzh`），**不要写反斜杠形式**。
- ⚠️ CI 部署用的是**另一把**密钥（GitHub Secrets 的 `DEPLOY_SSH_KEY`，见 `.github/workflows/release.yml`），与本文件无关。

## Windows（git bash）下的两个坑

1. **权限**：ssh 可能以 "Permissions are too open" 拒绝从 Windows 侧拷来的 pem → 先 `cp` 到 `/tmp` 再 `chmod 600`，用副本连。
2. **长连接**：隧道之类的长连接要在**主人本机的终端**里跑，别交给 AI 会话的后台任务（会被回收）。

## 红线

- pem 的**内容**不进文档、不进提交、不进消息；仓库里能出现的只有**路径**。
- 部署与回滚看 `docs/operations/deployment.md`，排障看 `docs/operations/runbook.md`——本文件只管「怎么连上去」。
