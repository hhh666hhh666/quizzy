# Quizzy · 给 AI 的规矩

适用任何 AI 工具。**权威版本在 `.codebuddy/rules/`（两条规则总是自动加载），本文件是精简摘要；两边冲突以那边为准**，改那边就要改这里。

Spring Boot + Vue 的自用刷题工具，围绕「一道题反复练到会」。领域词看 `CONTEXT.md`，文档总索引看 `docs/README.md`。

## 1. 未决事项必须落盘 `docs/todo/`

出现「以后 / 回头 / 暂缓 / 先不 / 还没定 / 看情况 / 再说」任一词，或有选项没给推荐，就在本次回复结束前：

- 同主题已有文件 → 追加；没有 → 新建 `docs/todo/YYYY-MM-DD-TODO-<主题>.md`（日期 = 创建日期，改文件不改名）
- 头三行固定：`# 待办：<主题>` / 空行 / `创建：YYYY-MM-DD · 状态：待办 · 优先级：高|中|低`
- **必须在 `docs/todo/README.md` 索引表补/改一行**
- 已拍板的**不是** TODO：结论进 `docs/adr/00NN-*.md`（编号递增、只追加），过程进 `docs/decisions/`，并在「已定，不用再想」表加一行
- 任何文档正文里不许开第二份待办清单，一律指回 `docs/todo/`

## 2. 改代码 = 改文档

- 能从代码 / 脚本 / `.env.example` / Flyway / springdoc 查到的事实，文档只写「去哪看」，**不复制**（ADR 0006）；文档与代码冲突以代码为准，顺手改文档
- 改了接口 / 表结构 / 判分 / 导入导出 / 前端 / 部署脚本 → 对 `docs/design/`、`docs/operations/` 下对应文件（索引见 `docs/README.md`）
- **移动或删除文件**：用 `git mv`，然后 `git grep -n '<旧名>'` 全仓搜（含注释、脚本、YAML、Dockerfile）逐个改
- md 里写虚构路径用行内代码 `` `foo/bar.md` ``，别写成链接

## 3. 收尾（每次对话结束前）

1. 扫本次对话有没有「提到但没做」的事 → 按第 1 条落 TODO（在回复里客套一句不算完成）
2. `git status --porcelain` 列出改动 → 按第 2 条补文档与引用
3. 跑 `bash scripts/check-doc-links.sh`，修到全绿（CI 里有同名 job 会再验一次）
4. 回复末尾给出：改了哪些文件 / 新增或更新了哪些 TODO / 建议的 Conventional Commits 切分
