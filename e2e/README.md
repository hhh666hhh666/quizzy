# 端到端测试（Playwright）

这个工程是 [《测试系统说明》](../docs/testing/系统说明.md) 里说的**第三层**：真浏览器 + 真后端 + 真库。

**为什么单独一个工程**：它是跨端的（PC / 未来的移动 H5），塞进任何一个前端工程都会让那个工程背起跨端职责。带上自己的 `package.json` 之后，两个前端工程的依赖树**一个字都不用动**——那条「前端保持零额外依赖」的形态因此没有被破坏（[ADR 0020](../docs/adr/0020-layered-test-system.md)）。

## 怎么跑

**前置：被测的全栈得先起来**（MySQL → 后端 → 前端）。本工程**不负责起服务**——启动方式写在 `pom.xml`、`package.json` 与 [根 README](../README.md) 里，本工程不再说第二遍（[ADR 0006](../docs/adr/0006-bare-array-import-contract.md)）。

默认连 `http://127.0.0.1:5173`，可用 `E2E_BASE_URL` 换。

```bash
npm ci
npm test              # 跑全部
npm run test:list     # 只列用例，不执行（验配置用）
```

### CI 上

`npx playwright install --with-deps chromium` 装浏览器，然后 `npm test`。一次性环境（`mysql:8` service + 后端进程 + 前端 preview）由 job 的步骤拉起，**不建 `.env` 文件**——测试配置直接注入 job 的 env。

⚠️ **前端 preview 必须显式 `--host 127.0.0.1`**：Vite 的 preview 默认 host 是 `localhost`，而 Linux runner 上 `localhost` 会解析到 IPv6 的 `::1` → 只监听 `::1`，于是 `curl 127.0.0.1` 每次「0 毫秒直接拒绝」，看起来像服务根本没起。本机 Windows 上 `localhost` 就是 `127.0.0.1`，**所以这个坑本机测不出来**——首次 CI 就这么红过一次。

### 本机

⚠️ 两条本机特有的限制：

1. **需要 Docker 起 MySQL**。本机 Docker Desktop 被策略拦着时会起不来，此时跑不了——**这不是测试的问题**。
2. **本机没下载 Playwright 的浏览器**（那条网络通道在本机不通）。所以要驱动**本机已装的 Chrome**：

```bash
E2E_CHANNEL=chrome npm test
```

CI 上留空即可（用 `npx playwright install` 装的那份 Chromium）。

**本机怎么一次把全栈起起来**（不要用 dev 库，另起一个一次性容器）：

```bash
docker run -d --name quizzy-e2e-mysql -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=quizzy -p 127.0.0.1:3307:3306 mysql:8
# 后端指向它（QUIZZY_JWT_SECRET 需 ≥32 字节）：
SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/quizzy?...' \
SPRING_DATASOURCE_USERNAME=root SPRING_DATASOURCE_PASSWORD=root \
QUIZZY_JWT_SECRET='<32 字节以上的随机串>' mvn spring-boot:run
# 前端：npm run build && node node_modules/vite/bin/vite.js preview --port 5173 --host 127.0.0.1
```

## 约定

- **workers 固定 1**：用例共享同一个库与同一个后端进程，并行会互相踩。要并行得先做数据隔离，那是另一个决定。
- **账号名带时间戳**：每次跑新建，避免与上次残留撞名。
- **断言优先语义定位**（`getByRole`），只在「同一页重复出现或文案会变」的元素上用 `data-testid`。
- **重复行用稳定类名钩子**（`.qb-row` / `.paper-row` / `.his-row` / `.wrong-row` / `.fav-row`）：
  同一页重复出现、文案会变的列表行，语义定位不可靠——这是这几处**唯一**的定位契约，
  改类名要全局搜（含 `support/helpers.ts`）。轻提示同理：`.toast--success` 等类名即断言钩子。
- **失败留证据**：`trace` 与截图（`trace: 'retain-on-failure'`）。不录视频——代价大而排查价值与 trace 重叠。
- **只跑桌面 Chromium**：跨浏览器矩阵与移动视口都是明确不做 / 后置。

## 加一条新用例

先看 [《测试系统说明》的「改动 → 补哪层」](../docs/testing/系统说明.md)：

- 新增 / 改接口 → 补的是**接口层**，不是这里；
- 前端页面 / 路由 / 交互 → **补这里**；
- 只改样式 / 文案 / 排版 → 不写新断言（几何断言若已覆盖该页面会自动生效）。

`specs/` 下的文件按业务链路编号（`00-` 冒烟、`01-` 排版几何、`02-` 核心链路…）。
⚠️ **不在这里列「现有哪些 spec、各几条」**——数量会变、清单必然漂（[ADR 0021](../docs/adr/0021-test-sync-discipline.md)）；
要看现有哪些，直接看 `specs/` 目录本身。
