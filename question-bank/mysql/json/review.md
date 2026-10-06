# 题库复核报告

- 生成时间：2026-10-06 16:17
- 题库总数：**189**
- 源文件：E:\storage\program\workbuddy\quizzy\question-bank\mysql\json\.full.json（含溯源信息）；导入用：`all.json`（或 `by-category/<分类>.json`）

## 题型 / 难度 / 考频分布

| 维度 | 分布 |
| --- | --- |
| 题型 | 单选 125、判断 34、多选 30 |
| 难度 | 中等 99、简单 78、困难 12 |
| 考频 | 中频 141、高频 36、低频 12 |
| 分类 | mysql 189 |

## 单节题数

- 有题目的切片 53 个，平均 3.6 题/节
- 没有单节超过 5 题的切片

## 章节覆盖

- 切片总数 53，产出题目的切片 51，覆盖率 96%

## 被跳过的章节（13）

- `MySQL.md` → (全文)：丢弃了 4 个 <details> 折叠块（语雀导出的重复答案）
- `mysql\md\MySQL.md` → 概念：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → MySQL的DDL和DML分别是什么含义？：信息密度不足（非空白字符 74 < 80 且无代码/表格）
- `mysql\md\MySQL.md` → 结构和事务：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 索引：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 日志：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 锁：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 架构：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 调优：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → count(*)的优化：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 主从复制：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 分库分表：空节（去掉标题/链接/星号后没有正文）
- `mysql\md\MySQL.md` → 其他：空节（去掉标题/链接/星号后没有正文）

## 校验结果

- 总题数 189：通过 189、带警告 0、失败 0
- 考频：高频 36、中频 141、低频 12；高频占比 19%

## 下一步

1. 打开 `preview.html` 抽样核对答案与原文出处；
2. 需要保留的手改题，在 `.full.json` 里给它加 `"_locked": true`；
3. 确认后把 `all.json` 内容贴进前端导入框，或用：
   `curl -X POST http://localhost:8080/api/questions/import/json -H "Content-Type: application/json" -H "Authorization: Bearer <token>" --data-binary @all.json`
