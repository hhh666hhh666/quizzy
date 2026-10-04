# 测试

本目录分三份文件，**别混着写**：

| 想看什么 | 去哪 |
|----------|------|
| 现在有哪些测试、怎么跑、盲区在哪、新测试放哪 | **本文件** —— 现状与索引 |
| 为什么这么分层、每层保证什么、加功能时怎么同步 | [系统说明](./系统说明.md) —— 设计意图 |
| 让 AI 当真实用户做探索测试的任务书 | [agent-exploration.md](./agent-exploration.md) |

**为什么分两份**：能从代码与 CI 查到的事实**只写一次**（本文件），理由**只写一次**（系统说明）。同一件事写两遍必然漂——那是 [ADR 0006](../adr/0006-bare-array-import-contract.md) 的教训；测试系统本身的取舍见 [ADR 0020](../adr/0020-layered-test-system.md)。

## 一句话现状

**后端有单元测试，只覆盖两处最容易出静默错误的逻辑；端到端层已落地；接口与服务集成层还没做。**

端到端跑在 **CI 的一次性环境**里（MySQL + 后端进程 + 前端 preview，跑完随 runner 销毁），工程在 [e2e/](../../e2e/README.md)。接口与服务集成层**设计已定、尚未落地**（设计见 [系统说明](./系统说明.md) 与 [ADR 0020](../adr/0020-layered-test-system.md)，落地顺序见 [todo](../todo/2026-10-04-TODO-测试系统落地.md)）。两个前端工程（PC 与移动端）仍然**没有单测、没有 lint**——端到端的依赖被隔离在独立工程里，前端工程的依赖树一个字都没动。

具体有哪些测试类，看目录本身（数量会变，文档不写数字）。

- [../../quizzy-server/src/test/java/com/quizzy/module/quiz/service/ScoreStrategyTest.java](../../quizzy-server/src/test/java/com/quizzy/module/quiz/service/ScoreStrategyTest.java) — 判分策略
- [../../quizzy-server/src/test/java/com/quizzy/module/question/service/QuestionImportServiceTest.java](../../quizzy-server/src/test/java/com/quizzy/module/question/service/QuestionImportServiceTest.java) — 导入校验

## 测试边界（刻意的选择）

- **只测判分与导入校验**：这两处的错误是静默的——多选题答案集合、判断题对错写错了不会报错，只会一直错下去。
- **全部是纯 JUnit 5**，不拉 Spring 上下文、不触发 Flyway、**不需要数据库**。
  这条约束是有意为之：它让 CI 上跑 `mvn test` 不需要起任何服务（见 [.github/workflows/ci.yml](../../.github/workflows/ci.yml) 的注释）。
  ⚠️ 加测试时**不要破坏这条约束**，否则 CI 会开始需要一个数据库。

## 本地怎么跑

后端：在 `quizzy-server` 下 `mvn -B -ntp test`（与 CI 同一条命令）。加了数据库与 Spring 的那两层**怎么在本机跑**，以 `quizzy-server/pom.xml` 的 profile 定义为准——本文件不复制那份清单。

前端：`quizzy-web` 与 `quizzy-mobile` 各自可用 `npm run typecheck` 与构建脚本（移动端是 `build:h5`）——**以各自 `package.json` 的 `scripts` 为准**，本文件不复制。

端到端：见 [e2e/README.md](../../e2e/README.md)——它需要先起全栈（MySQL → 后端 → 前端），怎么起看 [根 README](../../README.md) 与 `pom.xml`。

## CI 怎么跑

各 job 的定义就是清单，看 [.github/workflows/ci.yml](../../.github/workflows/ci.yml) 本身，**本文件不复述**（改了会立刻过期）。

只有一条要单独强调：**`vite build`（以及移动端的 `uni build`）走 esbuild 只剥离类型、不做检查，所以类型错误只会被 `typecheck` 这一步挡住。** 别以为「构建过了就是好的」。

移动端那条还多一步 `diff` 守卫：`quizzy-mobile/src/types/index.ts` 必须与 `quizzy-web/src/types/index.ts` **逐字一致**——移动端不引共享包（[ADR 0019](../adr/0019-mobile-clients-with-uniapp.md)），类型是逐份复制的镜像，这条断言把「两份必须一致」从约定变成可验证事实。

另外两个镜像 job 是**冒烟**：只在干净 Linux 上验证镜像能从零构建出来，不推任何 registry。它的价值在于 ADR 0009 / 0010 记的那类坑（构建镜像平台、宿主机 `node_modules` 污染容器）**只有在这种环境才会暴露**，本机因缓存命中永远发现不了。

## 已知盲区

只描述，不成清单——要转化为行动去 [todo/](../todo/)。

- 接口层与服务集成层**尚未落地**（方案已定）；
- 前端无单测、无 lint（暂缓，理由见 [系统说明](./系统说明.md)）；
- **排版的审美与层级零自动化**——不做视觉回归，也只有人眼能判断「协调 / 主次」；几何事实（溢出、越界、裁切）待端到端层落地后由断言覆盖；
- 历史作答不做题目快照（题目被改后，历史记录里显示的解析会跟着变）；
- 现有单元测试不覆盖 Web 层与数据库交互。

## 加新测试放哪

三层各有落点，**别混放**：

- **单元测试**：`quizzy-server/src/test/java/com/quizzy/module/<模块>/service/`
- **接口与服务集成测试**：同一棵 `src/test` 树，靠标签与 Maven profile 与单元测试分开（**默认 `mvn test` 仍只跑单元**）
- **端到端测试**：独立工程 [e2e/](../../e2e/README.md) 的 `specs/`

路径与标签的**权威定义在 `pom.xml` 与 `e2e/` 自身**，本文件只给形状。

⚠️ 保持「纯 JUnit 测试不引 Spring 上下文」这条约束。某个逻辑确实需要数据库才能测时，**按「改动 → 补哪层」的表**（在 [系统说明](./系统说明.md)）决定进哪一层，别顺手让 `mvn test` 背上起服务的成本——那是一次真正的取舍。
