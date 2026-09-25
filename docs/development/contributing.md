# 贡献指南

## 这个项目的协作现实

单人开发、个人自用工具、仓库 public。

欢迎开 issue。要提 PR 的话**建议先开 issue 讨论**——这里很多决策是本机环境约束驱动的（Docker Hub 不通、依赖走国内源、数据绑在 E 盘），外人从代码里看不出来，容易白做。

## 分支与合并

`master` 是唯一常驻分支。改动走**短命分支 → PR → squash 合并 → 删分支**。

先例：CI 那轮用的是 `ci/gate` 分支，验绿后再 squash 进 master 并删除（该分支已不存在，过程见 [../decisions/ci-gate-setup.md](../decisions/ci-gate-setup.md)）。

直接 push master 也可以，但走 PR 有两个实际好处：能在合并前看到整体 diff，以及留一份可回看的记录。

## 提交信息

沿用 **Conventional Commits**：`feat:` / `fix:` / `docs:` / `chore:` …

现有提交全是这个风格，`git log` 就是样例库。两条容易混的：

- `docs:` —— 改文档**内容**（ADR、README、本目录）；
- `chore:` —— 工具与工程杂项（.gitignore、换行、依赖版本）。

## 提 PR 前本地要做什么

1. 跑一遍 [../testing.md](../testing.md) 里列的本地命令（后端 `mvn test`、前端 `typecheck` + `build`）。
2. 可选：在本机构建两个镜像做冒烟——但本机命中缓存会掩盖问题，真正的验证是 CI 那两个 job。

## CI 会拦什么

见 [../testing.md](../testing.md) 的 CI 一节与 [../adr/0013](../adr/0013-github-actions-gate-no-auto-deploy.md)。

⚠️ **CI 绿了不等于线上已更新。** 上线要自己跑 `scripts/deploy.sh`，见 [../operations/deployment.md](../operations/deployment.md)。

## 文档纪律（本项目特有，最重要的一节）

- **改代码同时改文档**；文档与代码冲突时**以代码为准**，并顺手把文档改对。
  真实教训记录在 [../adr/0006](../adr/0006-bare-array-import-contract.md)：`DESIGN.md` 曾把导入契约写成包装对象而与实现漂移。
- **能从代码、脚本、`.env.example`、Flyway 或 springdoc 运行时得到的事实，文档只写「去哪看」，不复制一份。**
- 新决策怎么落：
  - **结论**进 `docs/adr/`，格式照 [0006](../adr/0006-bare-array-import-contract.md) 抄（Status / Considered Options / Consequences），编号递增，**只追加不改**；
  - **过程**（问答、被否选项、当时为什么纠结）留在 `docs/decisions/`；
  - **未决事项**进 `TODO/`，命名规则见 [../../TODO/README.md](../../TODO/README.md)。
- 两者区别见 [../README.md](../README.md)。

## 环境小坑

- **`*.sh` 必须 LF 换行**：CRLF 会让 bash 报 `bad interpreter`。`.gitattributes` 已强制，但编辑器可能改回去，提交前留意 `git diff` 有没有整文件变动。
- **`.env` 不入库**，模板是 `.env.example`；`QUIZZY_JWT_SECRET` 必须是随机值，理由见 [../operations/configuration.md](../operations/configuration.md)。
- 本机连不上 Docker Hub，基础镜像要靠镜像站打 tag；依赖源（Maven / npm）走国内源是**环境约束而非项目偏好**，见 [../adr/0010](../adr/0010-china-mirrors-for-build-deps.md)。

## 关于本文件的位置

GitHub 只自动识别**仓库根目录**的 `CONTRIBUTING.md`（会在 PR 页面显示提示条）。本文件放在 `docs/development/` 下不会触发那个提示；是否要在根目录建一个转发页，与 `DESIGN.md` 的转发页策略一起决定。
