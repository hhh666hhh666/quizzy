# Quizzy v1 设计导航

原设计文档已按主题拆分到 `docs/` 下。**本文件只作兼容入口，不要在这里新增内容**——既有链接能继续用，但真正的说明都在下面这些文件里。

| 要找什么 | 去哪 |
|----------|------|
| 产品定位、v1 范围、明确不做 | [需求范围](docs/requirements/scope.md) |
| 实体关系、可见性、生命周期 | [数据模型](docs/design/数据模型.md) |
| 判分、会话、错题本、抽题规则 | [判分与业务规则](docs/design/判分与业务规则.md) |
| Excel / JSON 导入导出契约 | [导入导出](docs/design/导入导出.md) |
| API 约定与运行时文档入口 | [API](docs/design/API.md) |
| 页面流、路由、状态边界 | [前端](docs/design/前端.md) |

完整文档地图见 [docs/README.md](docs/README.md)，每条决策的理由见 [docs/adr/](docs/adr/)。
