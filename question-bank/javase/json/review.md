# 题库复核报告

- 生成时间：2026-10-12 14:53
- 题库总数：**109**
- 源文件：E:\storage\program\workbuddy\quizzy\question-bank\javase\json\.full.json（含溯源信息）；导入用：`all.json`（或 `by-category/<分类>.json`）

## 题型 / 难度 / 考频分布

| 维度 | 分布 |
| --- | --- |
| 题型 | 单选 51、判断 33、多选 25 |
| 难度 | 中等 67、简单 41、困难 1 |
| 考频 | 中频 76、高频 30、低频 3 |
| 分类 | javase 109 |

## 单节题数

- 有题目的切片 43 个，平均 2.5 题/节
- 没有单节超过 5 题的切片

## 章节覆盖

- 切片总数 43，产出题目的切片 43，覆盖率 100%

## 被跳过的章节（4）

- `md\JavaSe.md` → (开头)：空节（去掉标题/链接/星号后没有正文）
- `md\JavaSe.md` → Java的动态代理如何实现？：空节（去掉标题/链接/星号后没有正文）
- `md\JavaSe.md` → jdk8新特性：空节（去掉标题/链接/星号后没有正文）
- `md\JavaSe.md` → 集合：空节（去掉标题/链接/星号后没有正文）

## 校验结果

- 总题数 109：通过 109、带警告 0、失败 0
- 考频：高频 30、中频 76、低频 3；高频占比 28%

## 下一步

1. 打开 `preview.html` 抽样核对答案与原文出处；
2. 需要保留的手改题，在 `.full.json` 里给它加 `"_locked": true`；
3. 确认后把 `all.json` 内容贴进前端导入框，或用：
   `curl -X POST http://localhost:8080/api/questions/import/json -H "Content-Type: application/json" -H "Authorization: Bearer <token>" --data-binary @all.json`
