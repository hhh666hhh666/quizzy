# API

**目的**：告诉你**去哪看当前真实的接口**，以及那些跨接口通行、但从单个 endpoint 上看不出来的约定。

**读者**：要对接接口或改 Controller 的人。

**真相源**：**接口清单以运行时生成的文档和各 Controller 为准**。本文档刻意不维护 endpoint 表格——手写清单一旦和实现漂移，比没有文档更糟（前车之鉴见 [ADR 0006](../adr/0006-bare-array-import-contract.md)）。

---

## 怎么看当前 API

后端跑起来之后有两个入口，它们是**实时生成**的，永远和实现同步：

| 入口 | 用途 |
|------|------|
| Swagger UI（`/swagger-ui.html`） | 人看的交互文档，可以就地发请求 |
| `/v3/api-docs` | 机器可读的 OpenAPI JSON，用于生成客户端或做契约比对 |

想知道「现在到底有哪些接口、参数长什么样」，看这两个，不要看任何静态文档（包括这一份）。

顺带一提：容器的健康检查端点探的就是 `/v3/api-docs`，理由见 [ADR 0012](../adr/0012-healthcheck-probes-api-docs.md)。

## 接口按领域划分

后端按业务模块分包，每个模块一个 Controller，接口路径与模块对应：

| 领域 | 入口 |
|------|------|
| 认证 | `module/auth` 的 Controller |
| 分类与标签 | `module/category` 的 Controller |
| 题目与导入导出 | `module/question` 的 Controller |
| 试卷 | `module/paper` 的 Controller |
| 答题会话 | `module/quiz` 的 Controller |
| 错题本 | `module/wrongbook` 的 Controller |

## 通用约定

这些是跨接口通行的规则，不会在每个接口上重复说明：

- **统一响应体**：所有接口都包一层信封（`code` / `message` / `data` / `timestamp`），`code` 为零表示成功。结构定义在 `Result`
- **分页**：入参统一是 `page` / `size`，返回统一是 `PageResult<T>`（含列表与总数）。结构定义在 `PageResult`
- **异常**：业务异常走 `BusinessException`，由全局异常处理器转成统一信封，不需要调用方解异常
- **认证**：除注册登录外全部接口需要 JWT，token 从请求头带

精确字段名与类型看上面的 Java 类型定义，不在这里抄。

## 认证与数据边界

JWT 的签发与校验、有效期、以及「当前用户」怎么注入，见 `JwtUtil` / `JwtInterceptor` 与相关配置。

⚠️ **登录态是「半状态」的**：JWT 里带一个 token 版本号，**每次请求都会与库里的值比对**，不一致即 401。所以改密码、或点「退出所有设备」能让所有旧 token **立即**失效，而不是等它 7 天自然过期；查不到用户（已被注销）时同样 401。代价是每个请求多一次查库。见 [ADR 0027](../adr/0027-token-version-invalidates-all-devices.md)。

账户自助（改密码 / 改昵称 / 改头像 / 注销账号）同属 `module/auth`。两条容易踩的约定：

- ⚠️ 头像走 **JSON 里的 data URL，不是 multipart 上传**——后端没有接收图片文件的通道，只接收已编码好的字符串。只收 jpeg / png / webp，按文件头判定，拒绝 SVG（[ADR 0028](../adr/0028-avatar-stored-in-database.md)）。
- ⚠️ 「改登录名」这个接口**不存在**，是刻意不做，不是漏了（见 [《需求范围》](../requirements/scope.md) 的「用户」一条）。

## 收藏夹

收藏夹挂在 `module/favorite` 的 Controller 下。三条跨接口的约定：

- ⚠️ **「收藏」不是独立状态**——一道题要么在某个夹里、要么根本没被收藏。所以「取消收藏」是从**所有**夹
  移出，而不是只从一个夹移出（见 [《数据模型》](数据模型.md)）。
- ⚠️ **题库列表的收藏筛选是两个参数**：`anyFavorite`（在任意夹里）与 `favoriteFolderIds`（按具体夹）。
  前者为真时**忽略**后者——两者是包含关系，前端不必自己算「谁赢」，后端也不报错。
- ⚠️ **收藏夹页面右列不单独开接口**：它走 `GET /api/questions?favoriteFolderIds=…`，与题库页共用同一套
  筛选与字段组装，这样「收藏筛选」只有一份实现。

**数据边界不靠接口参数保证**：可见性规则（公开题与私有题谁能看谁能改）在服务端按归属判定，详见 [《数据模型》](数据模型.md)。换句话说，前端把筛选条件藏起来不构成权限控制。

## 接口变更规则

改接口时按这个顺序，顺序反了就会出现另一份真相源：

1. 先改 Controller / DTO / VO 与注解
2. 跑起来用 `/v3/api-docs` 验证输出
3. 需要的话更新本文档里的**约定性说明**（但不要新增 endpoint 清单）
4. 破坏性变更必须在 [`CHANGELOG.md`](../../CHANGELOG.md) 里写明迁移方式
