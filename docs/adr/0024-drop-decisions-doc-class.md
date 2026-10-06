# 取消 docs/decisions 这一文档类别

Status: accepted

`docs/` 下原有的 `docs/decisions/`（**决策过程**日志：保留 grill 问答原文、被否选项、当时的纠结）整类删除，10 份日志随之下线。此后**结论只进 `docs/adr/`**（被否选项写进同一张卡的 Considered Options），**过程只归 `docs/worklog/`**（会话工作台）。

## Considered Options

- **保留目录，只删掉往它写东西的那个 skill**：零改动、引用零风险。**被否**：会留下一类**没有任何工具与规则去写**的目录——它只在有人碰巧想起来时被动更新，是「躺在仓库里但没人维护」的欠债，比没有更糟。
- **把 decisions 的内容并进 `docs/worklog/`**：看似延续「过程」这一类。**被否**：worklog 明确定义为「只对创建它的那次会话有意义、其他会话默认不读」，把长期凭据搬进去等于塞进一次性文件。
- **保留目录但声明不再新增**：给历史留个位置。**被否**：等于允许一份必然持续漂移的文档活着，与 [ADR 0006](0006-bare-array-import-contract.md) 的单一信息源纪律冲突。
- **整类取消**（选定）。

## Consequences

- **两条理由**：① **两类「过程」文档职责重叠**——[ADR 0018](0018-session-worklog-as-doc-class.md) 建 `docs/worklog/` 的起因就是「既有纪律只覆盖结论，没有一类管过程」，worklog 落地后 decisions 与它的定位就撞了，且二者都是按时间累积；② **decisions 相对 ADR 多出来的只有「问答原文」**，而被否选项本就写在 ADR 卡的 Considered Options 里，原文那部分正是 worklog 的活。
- **它是单点工具链的产物**：往该目录写东西的只有一个本机用户级 skill `grill-decisions`，该 skill 已一并删除。没有别的自动或人工入口，留存即死水。
- **查「当时考虑过什么」的口径变了**：不再是「去 decisions 翻原文」，而是「看对应 ADR 卡的 Considered Options」。因此**以后写 ADR 必须把被否选项写进卡里**，别再指望另存一处——这条已同步进 `.codebuddy/rules/docs-sync.md`、`.codebuddy/rules/todo-discipline.md`、`AGENTS.md` 与 `docs/development/contributing.md`。
- **被删的 10 份日志正文只在 git 历史里**：要找回用 `git log --diff-filter=D -- docs/decisions`。其中**没有被任何 ADR 覆盖**的内容（例如 `records-and-glossary.md` 的 D5「ADR 收录标准」）就此失去常驻入口，要不要回捞已记到 `docs/todo/`。
- **历史引用一律按「只追加不改」处理**：ADR 0013 / 0017 / 0018 / 0023 正文里的 `docs/decisions/...` 路径保留原文，各篇追加一行后续注指向本文；已发布段落的 CHANGELOG 与 worklog 流水账不回改。**唯一例外是会成为死链的相对链接**——那 7 处已就地改掉，因为 `scripts/check-doc-links.sh` 会拦、CI 有同名 job。
- `docs/README.md` 的文档地图与「决策只有 adr 记」一节、`docs/目录结构.md` 的目录树已同步更新，`docs/decisions/` 不再出现在任何索引里。

相关：[ADR 0006](0006-bare-array-import-contract.md)（单一信息源）、[ADR 0018](0018-session-worklog-as-doc-class.md)（worklog 立为过程类文档，本文收窄了它旁边那一类）、[ADR 0014](0014-ai-discipline-in-repo.md)（AI 纪律入仓）。
