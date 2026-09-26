---
description: 改代码必须同步改文档；移动/删除/重命名文件后必须全仓搜引用（含注释与脚本），提交前跑 scripts/check-doc-links.sh。
alwaysApply: true
---

# 改代码 = 改文档

## 单一信息源（ADR 0006，硬纪律）

能从代码、脚本、`.env.example`、Flyway 迁移、springdoc 推导出来的事实，文档只写**去哪看**，不复制内容——`DESIGN.md` 曾复制导入契约，随后与实现漂移。
**文档与代码冲突时以代码为准，并顺手把文档改对。**

## 改动 → 该去哪儿改

不确定就先读 `docs/README.md` 的文档地图表，别猜。

| 改了 | 去检查 |
|------|--------|
| 后端接口 / DTO / VO | `docs/design/API.md`（细节以 springdoc 为准） |
| 表结构 / 实体 / Flyway | `docs/design/数据模型.md` |
| 判分、抽题、答题流转 | `docs/design/判分与业务规则.md` |
| 导入导出格式 | `docs/design/导入导出.md` |
| 前端结构 / 路由 / 状态 | `docs/design/前端.md` |
| 部署、compose、`scripts/` | `docs/operations/deployment.md`、`runbook.md` |
| 配置、密钥、`.env` | `docs/operations/configuration.md`（ADR 0011） |
| 测试策略 | `docs/testing.md` |
| CI / 镜像构建 | `.github/workflows/ci.yml` + ADR 0010 / 0013 |
| 领域词汇 | `CONTEXT.md` |
| 本次迭代交付项 | `CHANGELOG.md` |

## 移动 / 删除 / 重命名文件（最容易漏，必须做）

1. 用 `git mv`，别手动重开文件。
2. 全仓搜旧名，**包括非 Markdown 文本**（注释、脚本 `echo`、Dockerfile、YAML、`.env.example`）：

   ```bash
   git grep -n '<旧名或旧路径>'
   ```

3. 改完跑 `bash scripts/check-doc-links.sh`。它查两类：md 相对链接、以及脚本/注释里提到的 `docs/ scripts/ .github/` 路径。CI 上有同名 job 会再验一遍。
4. md 里写**示例或虚构路径用行内代码** `` `foo/bar.md` ``，别写成 `[](foo/bar.md)`——脚本只查链接，不查行内代码。

## 收尾

- 结构性决策：结论进 `docs/adr/00NN-*.md`，过程进 `docs/decisions/`。
- 新的未决事项**不许**写在任何文档正文里 → `docs/todo/`（见 `todo-discipline` 规则）。
- 提交前跑 `bash scripts/check-doc-links.sh`，全绿再 commit。
