# 文档索引

这份索引只回答一个问题：**东西在哪**。内容本身一律留在各自的文件里，这里不复述。

## 文档地图

「真相源」一列是这份文档的**事实凭据在哪**——冲突时以真相源为准，并顺手修文档。
写「本文件」表示这份文档自己就是源头，没有别处可查。

| 路径 | 用途 | 读者 | 真相源 |
|------|------|------|--------|
| [../README.md](../README.md) | 5 分钟上手：跑起来、.env、CI 与部署、已知取舍 | 所有人 | 代码与脚本 |
| [../CONTEXT.md](../CONTEXT.md) | 领域术语表（题目 / 会话 / 错题本…纯词汇，不含实现） | 读代码的人 | 本文件 |
| [../DESIGN.md](../DESIGN.md) | v1 设计与接口清单（大而全，待拆分） | 改代码的人 | **代码**（⚠️ 与代码冲突以代码为准） |
| [../CHANGELOG.md](../CHANGELOG.md) | 版本变更与安全修复记录 | 所有人 | `git log` |
| [../LICENSE](../LICENSE) | MIT 许可证 | 想复用代码的人 | 本文件 |
| [adr/](./adr/) | **决策结论**卡片（Status / Considered Options / Consequences） | 查「为什么这么定」的人 | ADR 自身 |
| [decisions/](./decisions/) | **决策过程**日志（grill 问答原文、被否选项、未决问题） | 查「当时考虑过 X 吗」的人 | 日志自身 |
| [operations/runbook.md](./operations/runbook.md) | 应急预案：按症状查处置步骤 | 出事时的自己 | 脚本与 compose |
| [operations/deployment.md](./operations/deployment.md) | 上线路径、上线前检查、回滚 | 要上线的人 | `scripts/deploy.sh` |
| [operations/configuration.md](./operations/configuration.md) | 配置从哪来、改了要重启什么 | 换机器 / 改配置的人 | `.env.example` |
| [operations/backup.md](./operations/backup.md) | 备份现状、手动跑一次、恢复（未演练） | 担心数据丢的人 | `scripts/backup-mysql.sh` |
| [testing.md](./testing.md) | 测了什么、故意不测什么、新测试放哪 | 改代码前想确认安全网的人 | `src/test`、`package.json` |
| [development/contributing.md](./development/contributing.md) | 分支 / 提交 / PR 约定与文档纪律 | 要提交代码的人 | `git log` |
| [../TODO/](../TODO/) | **未决事项唯一入口** | 想知道「还有什么没定」的人 | 各待办文件 |

## ADR 与 decisions 的区别

这两个目录都在 `docs/` 下，但不是一回事：

- **`adr/`** 是**结论**。一条决策一张卡，写完不再改；要推翻就新增一条并标注取代了谁。
  查「**为什么**这么定」看这里。
- **`decisions/`** 是**过程**。保留 grill 问答原文，包括被放弃的选项和当时的纠结。
  查「当时**有没有考虑过**某个方案」看这里。

ADR 是 decisions 的沉淀：过程记完，够格的结论升级成 ADR（收录标准见 `decisions/records-and-glossary.md` 的 D5）。

## 未决事项只有一个入口：`TODO/`

规则：**任何「未定 / 待定 / 以后再说」只能存在于 `TODO/`**，docs 里一律链接过去，不许在文档中间开第二份待办清单。

命名与索引规则见 [TODO/README.md](../TODO/README.md)。那个文件里还有一张「已定，不用再想」的表——那些已经拍板并写进 ADR，列出来是为了防止以后重新纠结一遍。

## 单一信息源纪律（本项目的真实教训）

**`DESIGN.md` 8.2 节曾把 JSON 导入契约写成 `{"questions":[...]}` 包装对象，而实现收的是裸数组**——文档复制了事实，然后和实现漂移了。修正过程记录在 [ADR 0006](./adr/0006-bare-array-import-contract.md)。

由此定下两条：

1. **能从代码、脚本、`.env.example`、Flyway 迁移脚本或 springdoc 运行时得到的事实，文档只写「去哪看」，不复制一份。**
   典型反例：把 `.env.example` 的变量表抄进文档、把 `deploy.sh` 的步骤抄进文档、把种子题数抄进文档。
2. **文档与代码冲突时以代码为准**，并且顺手把文档改对——不是等下次。

## 常见入口

| 你想做的事 | 从哪开始 |
|------------|----------|
| 在一台新机器上跑起来 | [../README.md](../README.md) |
| 知道某个词在代码里指什么 | [../CONTEXT.md](../CONTEXT.md) |
| 排障（容器起不来 / 打不开 / 连不上库） | [operations/runbook.md](./operations/runbook.md) |
| 改代码前摸清设计与接口 | [../DESIGN.md](../DESIGN.md) + 对应 ADR |
| 上线 / 回滚 | [operations/deployment.md](./operations/deployment.md) |
| 想知道还有什么没定 | [../TODO/](../TODO/) |
