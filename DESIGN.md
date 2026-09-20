# Quizzy 答题程序 v1 — 设计文档

> 状态：**待确认**。本文是需求澄清（4 轮 20 个决策点）后的产物，确认后进入编码。

---

## 1. 决策记录

| # | 决策点 | 结论 |
|---|--------|------|
| 1 | 产品定位 | 个人自学刷题，练习语义（即时对错 + 解析 + 错题本），非考试语义 |
| 2 | 用户体系 | 轻量登录：账号密码 + JWT；用户可自建题库；不做独立后台管理页与 RBAC |
| 3 | 题型 | 单选 SINGLE / 多选 MULTI / 判断 JUDGE 三类 |
| 4 | 技术栈 | Spring Boot 3.5 + Java 21 + MyBatis-Plus + MySQL 8.4 + Vue3 + Vite + Pinia + Element Plus |
| 5 | 首期范围 | 题库管理、组卷、答题判分、结果页与错题本、批量导入导出（Excel + JSON） |
| 6 | 答题交互 | 逐题模式，作答后即时判分并展示正确答案与解析 |
| 7 | 计分 | 每题带自定义分值（默认 1 分）；多选题必须全对才得分，无部分分、无倒扣 |
| 8 | 题目归属 | 公共 + 私有混合：`owner_id IS NULL` = 公开题（只读），否则为私有题 |
| 9 | 组卷模型 | 会话式快速练习与持久化试卷并存 |
| 10 | 导入导出 | Excel（主）+ JSON（辅）；跳过错误行，返回逐行详细错误报告 |
| 11 | 答题进度 | 可回退修改已答题目；每题作答即时落库，刷新/关机可续答；不引 Redis，状态存 MySQL |
| 12 | 试卷形态 | 试卷分 `FIXED`（固定题目列表）与 `RULE`（组卷规则，每次现抽）两种模式 |
| 13 | 题目组织 | 平级分类 + 多标签，均全局共享（同名复用，先到先得） |
| 14 | 错题本 | 记录统计字段；答错入本，连续答对 ≥ 3 次自动移出，支持手动移出 |
| 15 | 内容形态 | 题干/选项/解析为 Markdown 文本，前端渲染（含代码高亮）；不做图片上传与富文本编辑器 |
| 16 | 接口契约 | 统一响应体 `{code, message, data, timestamp}`，HTTP 恒 200；分页统一 `PageResult<T>`；springdoc-openapi 生成 Swagger |
| 17 | 认证细节 | BCrypt + JWT 有效期 7 天 + 无刷新；token 存 localStorage + Pinia 持久化；除注册登录外全部接口需鉴权 |
| 18 | 后端组织 | controller / service / mapper 三层 + entity/dto/vo 分包 + Lombok + MapStruct + validation + 全局异常处理 |
| 19 | 前端组织 | TypeScript + Vue3 组合式 API + Vite + Pinia + Element Plus + Axios 封装 + 路由守卫 |
| 20 | 运行环境 | Docker Compose 双 profile：dev 仅起 MySQL（本地热重载开发），prod 全容器（MySQL + 后端 + nginx） |
| 20b | 单元测试 | 仅覆盖判分策略与导入校验两处 |

### 1.1 v1 明确不做

填空题 / 问答 / 编程题；限时考试、防作弊、监考；统计看板与掌握度分析；后台用户管理与角色权限；图片上传与富文本编辑器；Redis、消息队列、多级缓存；Refresh Token 机制；Excel 之外的导入格式。

---

## 2. 技术选型

**后端**：Spring Boot 3.5.x · Java 21 · MyBatis-Plus 3.5.x · MySQL 8.4 · Flyway · springdoc-openapi · Lombok · MapStruct · jjwt · EasyExcel · Jackson

**前端**：Vue 3.5 · Vite 6 · TypeScript · Pinia（含持久化） · Element Plus · Vue Router 4 · Axios · markdown-it + highlight.js

**运行**：Docker Compose（dev / prod 双 profile）

---

## 3. 数据模型

### 3.1 表清单

| 表 | 作用 |
|----|------|
| `user` | 用户 |
| `category` | 分类（平级，全局共享） |
| `tag` | 标签（全局共享，去重） |
| `question` | 题目 |
| `question_option` | 题目选项 |
| `question_tag` | 题目-标签关联 |
| `question_stat` | 用户 × 题目的作答统计（支撑错题本） |
| `paper` | 试卷（固定卷 / 规则卷） |
| `paper_question` | 固定卷的题目列表 |
| `quiz_session` | 一次答题会话 |
| `quiz_answer` | 会话内每题的作答与判分结果 |

### 3.2 字段设计

```text
user
  id BIGINT PK
  username VARCHAR(64) UNIQUE
  password_hash VARCHAR(100)
  nickname VARCHAR(64)
  create_time DATETIME
  update_time DATETIME

category
  id BIGINT PK
  name VARCHAR(64) UNIQUE
  sort INT
  create_time DATETIME

tag
  id BIGINT PK
  name VARCHAR(64) UNIQUE
  create_time DATETIME

question
  id BIGINT PK
  type VARCHAR(16)              -- SINGLE | MULTI | JUDGE
  stem TEXT                     -- Markdown
  analysis TEXT                 -- Markdown
  difficulty VARCHAR(16)        -- EASY | MEDIUM | HARD
  score INT DEFAULT 1
  category_id BIGINT
  owner_id BIGINT NULL          -- NULL = 公开题（只读）
  deleted TINYINT DEFAULT 0     -- 逻辑删除
  create_time DATETIME
  update_time DATETIME
  INDEX idx_owner_deleted (owner_id, deleted)
  INDEX idx_category (category_id)

question_option
  id BIGINT PK
  question_id BIGINT
  label VARCHAR(4)              -- A ~ F
  content VARCHAR(500)          -- Markdown
  sort INT
  INDEX idx_question (question_id)

question_tag
  question_id BIGINT
  tag_id BIGINT
  PK (question_id, tag_id)

question_stat
  id BIGINT PK
  user_id BIGINT
  question_id BIGINT
  answer_count INT DEFAULT 0
  correct_count INT DEFAULT 0
  consecutive_correct INT DEFAULT 0
  last_correct TINYINT          -- 最近一次是否正确
  in_wrong_book TINYINT DEFAULT 0
  last_answer_time DATETIME
  update_time DATETIME
  UNIQUE uk_user_question (user_id, question_id)
  INDEX idx_wrong (user_id, in_wrong_book, last_answer_time)

paper
  id BIGINT PK
  title VARCHAR(128)
  description VARCHAR(500)
  mode VARCHAR(16)              -- FIXED | RULE
  rule_json JSON NULL           -- 仅 RULE 模式
  question_count INT
  owner_id BIGINT
  create_time DATETIME
  update_time DATETIME

paper_question
  id BIGINT PK
  paper_id BIGINT
  question_id BIGINT
  sort INT
  UNIQUE uk_paper_question (paper_id, question_id)

quiz_session
  id BIGINT PK
  user_id BIGINT
  paper_id BIGINT NULL          -- 快速练习 / 错题练习为 NULL
  source_type VARCHAR(16)       -- PAPER | QUICK | WRONG_BOOK
  title VARCHAR(128)
  question_count INT
  current_index INT DEFAULT 0
  total_score INT
  obtained_score INT DEFAULT 0
  status VARCHAR(16)            -- IN_PROGRESS | COMPLETED | ABANDONED
  start_time DATETIME
  finish_time DATETIME
  INDEX idx_user_status (user_id, status)

quiz_answer
  id BIGINT PK
  session_id BIGINT
  question_id BIGINT
  user_answer VARCHAR(64)       -- "A" 或 "A,C"
  is_correct TINYINT
  score INT DEFAULT 0           -- 本题实得分
  sort INT
  answered_at DATETIME
  UNIQUE uk_session_question (session_id, question_id)
```

### 3.3 关键规则

**可见性**：查询题目时条件为 `deleted = 0 AND (owner_id IS NULL OR owner_id = :userId)`。公开题对所有登录用户可见但只读（编辑/删除接口校验 `owner_id = 当前用户`）。

**逻辑删除**：题目采用逻辑删除（`deleted = 1`），历史作答记录仍引用该题，不做题目快照。

**错题本**：

- 答错 → `in_wrong_book = 1`，`consecutive_correct = 0`
- 答对 → `consecutive_correct++`；若 `>= 3` → `in_wrong_book = 0`
- 手动移出 → `in_wrong_book = 0`，`consecutive_correct = 3`
- 错题本查询：`in_wrong_book = 1` 按 `last_answer_time DESC` 排序

**试卷规则 `rule_json` 结构**：

```json
{
  "categoryId": 1,
  "tagIds": [2, 3],
  "types": ["SINGLE", "MULTI"],
  "difficulties": ["EASY", "MEDIUM"],
  "count": 20,
  "excludeRecentDays": 7
}
```

---

## 4. 判分策略

`ScoreStrategy` 按题型分派，返回 `{isCorrect, score}`：

| 题型 | 规则 |
|------|------|
| SINGLE | 选中项 == 正确答案 → `score`，否则 0 |
| JUDGE | 同单选（选项固定为"正确 / 错误"） |
| MULTI | 选中集合 == 正确答案集合（顺序无关）→ `score`，否则 0；无部分分、无倒扣 |

**未作答**：不计分、不计入正确率分母；结束时提示"还有 N 题未作答"。

**会话得分**：`obtained_score = Σ quiz_answer.score`；正确率 = 正确题数 / 已答题数。

---

## 5. API 清单

统一响应体：`{code, message, data, timestamp}`，`code = 0` 表示成功。
分页入参 `page` / `size`，返回 `PageResult<T> {list, total, page, size}`。

### 认证 `/api/auth`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register` | 注册，返回 token + 用户信息 |
| POST | `/login` | 登录 |
| GET | `/me` | 当前用户 |

### 分类与标签

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/categories` | 分类列表 |
| POST / PUT / DELETE | `/api/categories[/{id}]` | 分类维护 |
| GET | `/api/tags` | 标签列表 |
| POST | `/api/tags` | 新建标签（同名复用） |

### 题目 `/api/questions`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `?page&size&keyword&type&difficulty&categoryId&tagIds&scope&onlyWrong` | 分页筛选（`scope`: mine / public / all） |
| GET | `/{id}` | 题目详情（含选项、答案、解析） |
| POST | | 新建题目 |
| PUT | `/{id}` | 修改（校验 owner） |
| DELETE | `/{id}` | 逻辑删除（校验 owner） |
| POST | `/import/json` | JSON 批量导入 |
| POST | `/import/excel` | Excel 导入（multipart） |
| GET | `/export?format=json\|excel&scope&ids` | 导出 |
| GET | `/template/excel` | 下载导入模板 |

### 试卷 `/api/papers`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `?page&size` | 试卷列表 |
| GET | `/{id}` | 详情（含题目列表或规则） |
| POST | | 新建（FIXED 传 `questionIds`，RULE 传 `rule`） |
| PUT | `/{id}` | 修改 |
| DELETE | `/{id}` | 删除 |
| POST | `/{id}/preview` | 规则卷预览抽题（不落库） |

### 答题 `/api/quiz`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/start` | 发起会话 `{sourceType, paperId?, rule?}`，返回 sessionId 与题目列表（**不含答案与解析**） |
| GET | `/sessions/{id}` | 会话详情 + 题目 + 已作答（续答用） |
| POST | `/sessions/{id}/answer` | 提交单题答案，返回 `{isCorrect, correctAnswer, analysis, score}` |
| POST | `/sessions/{id}/finish` | 结束并结算 |
| POST | `/sessions/{id}/abandon` | 放弃会话 |
| GET | `/sessions?page&size&status` | 历史会话列表 |
| GET | `/sessions/{id}/result` | 结果详情（逐题回顾） |

### 错题本 `/api/wrong-book`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `?page&size` | 错题列表 |
| DELETE | `/{questionId}` | 手动移出错题本 |
| POST | `/practice` | 发起错题练习，返回 sessionId |

---

## 6. 前端页面

| 路由 | 页面 | 要点 |
|------|------|------|
| `/login` `/register` | 登录 / 注册 | 表单校验，token 持久化 |
| `/questions` | 题库列表 | 表格 + 多维筛选 + 批量删除 + 导入 / 导出入口 |
| （抽屉） | 题目编辑 | 题型切换、选项动态增删、答案选择、解析、难度、分值、分类、标签 |
| （对话框） | 导入结果报告 | 成功 / 失败计数 + 错误行表格 + 下载错误报告 |
| `/papers` | 试卷列表 | 固定卷 / 规则卷统一展示 |
| （抽屉） | 试卷编辑 | 模式切换：选题抽屉（FIXED）或规则表单（RULE）+ 抽题预览 |
| `/quiz/:sessionId` | 答题页 | 题号进度、逐题作答、即时反馈解析、上一题 / 下一题、结束答题 |
| `/quiz/:sessionId/result` | 结果页 | 得分、正确率、逐题回顾、错题提示 |
| `/history` | 答题记录 | 历史会话列表，未完成的可继续 |
| `/wrong-book` | 错题本 | 错题列表 + 一键错题练习 |

**公共组件**：`MarkdownRenderer`、`QuestionCard`、`AnswerFeedback`、`ImportDialog`、`ImportReportTable`、`QuestionSelector`、`RuleForm`。
**Store**：`user`（token / 用户信息，持久化）、`quiz`（当前会话状态）。

---

## 7. 工程目录

```text
quizzy/
├── quizzy-server/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/quizzy/
│       │   ├── QuizzyApplication.java
│       │   ├── common/          Result, PageResult, ResultCode, BusinessException, GlobalExceptionHandler
│       │   ├── config/          WebMvcConfig, MybatisPlusConfig, CorsConfig, OpenApiConfig
│       │   ├── security/        JwtUtil, JwtInterceptor, UserContext, CurrentUser 注解
│       │   └── module/
│       │       ├── auth/
│       │       ├── category/    category + tag
│       │       ├── question/    entity/dto/vo/mapper/service/controller + importer/exporter
│       │       ├── paper/
│       │       ├── quiz/        session/answer + ScoreStrategy
│       │       └── wrongbook/
│       └── resources/
│           ├── application.yml
│           └── db/migration/    V1__init.sql, V2__seed_questions.sql
├── quizzy-web/
│   ├── package.json  vite.config.ts  tsconfig.json  index.html
│   ├── Dockerfile  nginx.conf
│   └── src/  api/ router/ stores/ layouts/ views/ components/ types/
├── docker-compose.dev.yml
├── docker-compose.prod.yml
└── README.md
```

---

## 8. 导入导出规格

### 8.1 Excel 模板列

| 列 | 说明 | 示例 |
|----|------|------|
| 题型 | single / multi / judge | multi |
| 题干 | Markdown | `关于 volatile，下列说法正确的是：` |
| 选项A ~ 选项F | 至少 2 个非空 | |
| 答案 | 单选一个字母；多选逗号分隔 | `A,C` |
| 解析 | Markdown | |
| 难度 | easy / medium / hard | medium |
| 分值 | 正整数 | 2 |
| 分类 | 不存在则自动创建 | Java 并发 |
| 标签 | 多个用逗号分隔 | `volatile,易错` |

### 8.2 JSON Schema

导入接口 `POST /api/questions/import/json` 收的是题目对象的**裸数组**，不带 `{"questions": [...]}` 外壳
（本文件早期版本写成了包装对象，与实现不一致，已按实现更正；决策理由见 `docs/adr/0006-bare-array-import-contract.md`）。

```json
[
  {
    "type": "MULTI",
    "stem": "关于 volatile ...",
    "options": [{ "label": "A", "content": "..." }, { "label": "B", "content": "..." }],
    "answer": ["A", "C"],
    "analysis": "...",
    "difficulty": "MEDIUM",
    "score": 2,
    "category": "Java 并发",
    "tags": ["volatile", "易错"]
  }
]
```

### 8.3 校验与失败处理

**校验规则**：题干非空；选项至少 2 个且内容非空、`label` 不重复；答案必须是已声明的选项 label，单选与判断题只能有 1 个；分值为正整数；分类与标签自动创建（同名复用）。

**失败处理**：跳过错误行，成功的入库，返回 `{total, successCount, failed: [{row, stem, reason}]}`；前端用表格展示错误行并提供错误报告下载。

---

## 9. 运行方式

```bash
# 开发：只起 MySQL（容器端口 3306 → 宿主机 3306，本机原 MySQL 需先停用）
docker compose -f docker-compose.dev.yml up -d

# 后端（或 IDE 直接运行 QuizzyApplication）
cd quizzy-server && mvn spring-boot:run      # 8080

# 前端
cd quizzy-web && npm install && npm run dev  # 5173，/api 代理到 8080

# 生产 / 演示：全容器
docker compose -f docker-compose.prod.yml up -d --build
```

Flyway 在应用启动时自动建表与灌种子数据，无需手动执行 SQL。

---

## 10. 遗留假设（需确认）

1. 题目删除为逻辑删除，历史作答**不做题目快照**——题目后续被修改时，历史记录里显示的解析会跟着变。
2. 公开题（`owner_id IS NULL`）所有人只读，不可编辑 / 删除。
3. 选项最多支持 A–F（6 个）；判断题固定 2 个选项（正确 / 错误），UI 渲染为对 / 错。
4. 快速练习与规则卷共用同一套 `rule` 结构；错题练习按 `last_answer_time` 倒序取前 N 条。
5. 跳过未答的题不计入正确率分母，结束时提示未作答数量。
6. 种子题库实际落地 34 道（Java 基础 / 并发 / JVM / Spring / MySQL / Redis / 网络），`owner_id = NULL`；更多题目应走 Excel 批量导入，而不是继续往迁移脚本里堆。
7. 端口：后端 8080、前端 5173、MySQL 容器映射宿主机 **3306**（本机原 MySQL 已停用）。
8. JWT 有效期 7 天，无刷新机制，登出即前端清除 token。
9. 单元测试仅覆盖判分策略与导入校验。

### 10.1 实现期修正

- 原表设计遗漏了「正确答案」的存储位置，已给 `question` 增加 `answer VARCHAR(64)`（逗号分隔的 label，如 `A,C`），与 `quiz_answer.user_answer` 同构，判分时直接比对。
- 用户自建题目统一为**私有**（`owner_id = 当前用户`），公开题只由种子数据产生，页面不提供「设为公开」入口。

---

## 11. 实施顺序

| 里程碑 | 内容 |
|--------|------|
| M1 | 后端骨架 + Flyway 建表 + 统一响应 / 异常处理 / JWT；前端脚手架 + 登录注册 |
| M2 | 分类标签 + 题目 CRUD + 列表筛选 |
| M3 | Excel / JSON 导入导出 + 错误报告 + 模板下载 |
| M4 | 试卷：固定卷 + 规则卷 + 抽题预览 |
| M5 | 答题会话、逐题作答、即时判分、回退续答、结果页 |
| M6 | 错题本 + 历史记录 + 种子题库 + README + compose 文件 |
