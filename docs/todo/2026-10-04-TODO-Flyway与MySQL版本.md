# 待办：Flyway 与 MySQL 版本

创建：2026-10-04 · 状态：待办 · 优先级：低

## Flyway 跑在它「已测试支持」之外的 MySQL 版本上

**现状**：prod 与测试都用 `mysql:8`，实测解析到 **8.4**；而迁移执行时 Flyway 每次都会打一条警告：

> Flyway upgrade recommended: MySQL 8.4 is newer than this version of Flyway and support has not been tested. The latest supported version of MySQL is 8.1.

**在哪看到的**：2026-10-04 接口层首次在 CI 上跑真容器时（`api` job 的日志）。本机与 CI 都会打。

**为什么现在定不下来**：迁移本身是**跑得通的**（V1 + V2 在 8.4 上成功，`release.yml` 的迁移重放也一直绿），所以这是「**供应商未做过验证**」而不是「不支持」。没有任何已知失效，而现在要处理它，就得动生产依赖。

**可选方向**：

- **不动，接受**——符合「迁移只做向后兼容」的既有纪律；真出问题会先在 `release.yml` 的迁移重放那一步暴露，**那一步在构建期、不碰线上**。
- **升级 Flyway** 到支持 8.4 的版本——消掉警告，但要评估 `flyway-core` / `flyway-mysql` 的版本兼容，并把迁移脚本重验一遍。
- **把 MySQL 钉到 `mysql:8.1`**——版本落回支持区间，但等于**降级生产数据库**。

**倾向第一档**：留到出现真实的不兼容症状再动。那时优先走第二档（升 Flyway），因为它不动数据库本身，而第三档要动数据。

⚠️ 无论走哪一档，都别忘了这条与 [ADR 0016](../adr/0016-cloud-deploy-with-release-pipeline.md) 的既有保险有关：**破坏性迁移不能靠回滚兜底**，所以「版本升不升」不改变那条纪律——它只是让「未验证的组合」缩小。
