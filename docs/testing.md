# 测试

## 一句话现状

**后端有单元测试，只覆盖两处最容易出静默错误的逻辑；前端没有任何测试，也没有 lint。**

具体是哪两个测试类，看目录本身（数量会变，文档不写数字）：

- [../quizzy-server/src/test/java/com/quizzy/module/quiz/service/ScoreStrategyTest.java](../quizzy-server/src/test/java/com/quizzy/module/quiz/service/ScoreStrategyTest.java) — 判分策略
- [../quizzy-server/src/test/java/com/quizzy/module/question/service/QuestionImportServiceTest.java](../quizzy-server/src/test/java/com/quizzy/module/question/service/QuestionImportServiceTest.java) — 导入校验

## 测试边界（刻意的选择）

- **只测判分与导入校验**：这两处的错误是静默的——多选题答案集合、判断题对错写错了不会报错，只会一直错下去。
- **全部是纯 JUnit 5**，不拉 Spring 上下文、不触发 Flyway、**不需要数据库**。
  这条约束是有意为之：它让 CI 上跑 `mvn test` 不需要起任何服务（见 [.github/workflows/ci.yml](../.github/workflows/ci.yml) 的注释）。
  ⚠️ 加测试时**不要破坏这条约束**，否则 CI 会开始需要一个数据库。

## 本地怎么跑

后端：在 `quizzy-server` 下 `mvn -B -ntp test`（与 CI 同一条命令）。

前端：`quizzy-web` 下可用的是 `npm run typecheck` 与 `npm run build`——**以 `package.json` 的 `scripts` 为准**，本文件不复制那份清单。

## CI 怎么跑

四条并行检查，定义见 [.github/workflows/ci.yml](../.github/workflows/ci.yml)，本文件不复述。

只有一条要单独强调：**`vite build` 走 esbuild 只剥离类型、不做检查，所以类型错误只会被 `typecheck` 这一步挡住。** 别以为「构建过了就是好的」。

另外两个镜像 job 是**冒烟**：只在干净 Linux 上验证镜像能从零构建出来，不推任何 registry。它的价值在于 ADR 0009 / 0010 记的那类坑（构建镜像平台、宿主机 `node_modules` 污染容器）**只有在这种环境才会暴露**，本机因缓存命中永远发现不了。

## 前端为什么没有单测与 lint

目前 `package.json` 里**没有这两个 script**，CI 对前端的门禁只有类型检查与构建。这是刻意的选择——为了保持「零额外依赖、随手能跑」的个人项目形态，而不是遗漏。

## 已知盲区

- 没有端到端测试；
- 前端无单测、无 lint；
- 历史作答不做题目快照（题目被改后，历史记录里显示的解析会跟着变）；
- 单元测试不覆盖 Web 层与数据库交互（受上面的边界约束）。

盲区只做描述，**不在文档里开待办清单**——要转化为行动，去 [docs/todo/](./todo/) 新建一条（命名规则见 [todo/README.md](./todo/README.md)）。

## 加新测试放哪

路径约定：`quizzy-server/src/test/java/com/quizzy/module/<模块>/service/`。

保持「纯 JUnit、不引 Spring 上下文」这条约束。如果某个逻辑确实需要数据库才能测，先想清楚是否值得为它让 CI 背上起服务的成本——那是一次真正的取舍，够格的话写条 ADR。
