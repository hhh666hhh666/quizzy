# JSON 导入契约取裸数组

Status: accepted

`POST /api/questions/import/json` 收的是题目对象的**裸数组**，不带 `{"questions": [...]}` 外壳。裸数组的好处是文件即一屏题目、diff 干净、整段粘进导入框不用先剥一层壳；配套 skill `md-to-question-bank` 生成的题库文件也已经按裸数组产出。

## Considered Options

- **包装对象**（原 `DESIGN.md` 的 JSON Schema 最初写的是这个）：对未来加元信息友好，但对当前唯一的使用方式（人眼看、整段粘贴）只是噪音。
- **两者都收**：看似两全，实际是为一个内部工具维护两套解析路径，收益远小于成本。

## Consequences

- 原 `DESIGN.md` 的 JSON Schema 一节原本写的是包装对象，与实现不一致，已改为裸数组（该文档现已拆分，契约写在 [《导入导出》](../design/导入导出.md#json-契约)）。**文档与代码冲突时以代码为准，并同步改文档**，不要靠记忆区分。
- 将来要给导入文件加元信息（来源文档、生成时间、批次号）时，必须改成包装对象，并同时改 skill 的输出格式——那是一次破坏性变更，做之前先回来把这条 ADR 置为 superseded。
