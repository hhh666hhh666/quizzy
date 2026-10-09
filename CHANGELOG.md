# 变更日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 格式。

## 关于版本号（先读这段）

**版本号以 `git tag` 为准**，遵循 SemVer（`MAJOR.MINOR.PATCH`）。本仓库的 `tag` 就是真相源，下面每个 `## [x.y.z]` 段都对应一个真实存在、可以 `git checkout` 的 tag。

需要区分三件事：

| 出处 | 是什么 | 说明 |
|------|--------|------|
| **`git tag`**（如 `v1.1.0`） | **版本号** | 唯一真相源，与下面每个版本段一一对应 |
| compose 里的 `image:` | 镜像标识 | 只写 `${APP_VERSION}` 变量，**不再手写版本号**；镜像由 CI 推到 ACR，服务器按 tag 拉取 |
| `pom.xml` / `package.json` 的包版本 | 各自的包版本 | Maven 坐标与 npm 包版本，**与发布版本无关**（`pom.xml` 还刻意带 `-SNAPSHOT`） |

打 tag 时 `scripts/check-version.sh`（`release.yml` 的「版本」job）会比对**三处手写点**，
并断言 compose 用的是变量而不是写死的 tag。
设计与取舍见 [ADR 0015](./docs/adr/0015-version-number-governance.md)（含 Amendment 1、2）；镜像分发已定案，
见 [docs/todo/archive/2026-09-20-TODO-镜像分发.md](./docs/todo/archive/2026-09-20-TODO-镜像分发.md)。

> 「产品代次 v1」这个说法已于 2026-10-01 取消——它和版本号长得太像，要表达那个意思就直接写范围。
> 下面 `[1.0.0]` 段里出现的「答题程序 v1」是当时的历史措辞，保留不改。

---

## [Unreleased]

### Added

- **收藏夹**（[ADR 0030](./docs/adr/0030-favorites-as-named-folders.md)）：可以把题目收进**自己命名的多个收藏夹**，
  一道题可同时属于多个夹。
  - **「收藏」= 在至少一个收藏夹里**，没有「未分组」；每个用户天然有一个**默认收藏夹**（按需创建、不可删、可改名），
    点星标先落进它
  - 星标两种手势：**短按**收藏 / 取消收藏（取消后可**撤销**），**长按**打开「修改收藏夹」面板；
    收藏时会弹一条带「修改收藏夹」的横幅
  - 新增「收藏夹」页面（左夹右题，可新建 / 改名 / 删夹、把题移出某个夹、用某个夹的题开一次练习）；
    右列的题**按最近收藏的排最前**并显示收藏时间——它走 `GET /api/favorites/questions`，
    与题库页的收藏筛选（题目视角、按题目倒序）不是同一个端点，理由见 ADR 0030；
    面板会显示一行只读的「现在在：X」（**不预勾**，但不该对现状闭口不谈）
  - 题库页新增「收藏夹」筛选（含「全部收藏」）、批量「加入收藏夹」与行内星标；题目详情与答题结果页也有星标
  - 后端：Flyway `V4__question_favorites.sql`（`favorite_folder` + `favorite_folder_question` 两张表）；
    「来源」新增第四种 `FAVORITE`
  - **收藏夹可填简介与公开开关**（[ADR 0032](./docs/adr/0032-glass-overlays-and-folder-intro.md)）：
    夹新增 `intro`（≤200 字）与 `is_public` 两列（Flyway `V5__folder_intro_and_public.sql`）；
    建夹 / 编辑走同一个「收藏夹信息」面板，**三字段整体覆盖**（清空简介 = 写 null，
    后端因此必须用 `LambdaUpdateWrapper.set()`——全局 `update-strategy: not_null` 会让
    `updateById` 静默跳过 null 字段）。⚠️ `is_public` 目前**存而不用**：界面文案不许暗示「公开后别人能看」；
    默认收藏夹也可改这三样（仍不可删）。收藏夹页按 v3 构图重构：左侧蓝调面板**按内容自然高度**、
    夹行操作收进「⋯」菜单（编辑信息 / 删除；触屏常显、键盘可达），右列保持近白卡片

### Changed

- **PC 前端完成重建（[ADR 0031](./docs/adr/0031-pc-frontend-rebuild-shadcn-vue.md)）——Element Plus 彻底退场**：
  全站换成 reka-ui + shadcn-vue（组件源码入库）+ Tailwind CSS v4 + 自建组件（toast / 确认框 / 可创建下拉），
  视觉系统以 `quizzy-web/src/styles/tokens.css` 为唯一真相源，人类可读快照为仓根 `DESIGN.md`。
  随重建落地的行为变化：
  - 全站视觉换新（「安静的练习室」：冷调浅面 + 近白阅读面 + 受控五色便签板；取值与规则见 `DESIGN.md`）
  - 用户区从顶栏移到**侧栏底部**（头像与昵称点击都进账户页；「当前外观 / 关于」同排）；
    **「退出登录」移入账户页**安全卡（仅退本设备）
  - 提示 / 确认框改为自建（取消走返回值，不再靠异常控制流）；「分类」「标签」改为自建可创建下拉
  - 列表页的筛选与分页**全量进网址**（题库 / 答题记录 / 试卷 / 错题本；脏值忽略、默认值不写、`replace` 不污染后退）
  - 键盘可达性补齐：skip link、行内动作统一焦点环、可点元素全改真按钮；浏览器标签页标题跟随路由，
    新增 favicon 与随主题切换的 `theme-color`
  - 顶栏「外观」三档保留（跟随系统 / 浅色 / 深色；**深色色板待后续落地**）
  - **题目详情弹窗加宽、宽高都可拖**：默认 960 × 640（原 `max-w-3xl` 被组件内置的 `sm:max-w-sm` 压成 384px），
    右边缘 / 下边缘 / 右下角三处热区分别调宽、调高、同时调宽高（**尺寸变化 = 指针位移 × 2**，两头一起张开），
    下限 480 × 320、宽度上限贴视口，宽高存 `localStorage`、双击手柄复位；
    高度是**钉住**的（内容不够高就留白，WYSIWYG）；
    同时**右边缘的滚动条改为 overlay 式**——平时隐形、悬停才浮出（宽 10px，去掉原来可见的 15px 竖条），
    并修好选项字母与选项内容不在一条基线上的问题（markdown 首段的 `margin-top` 会把文字压低 6px）
- **`scope.md` 的「明确不做：图片上传与富文本编辑器」收窄为「题目内容里的图片上传与富文本编辑器」**——
  头像属于账号资料，不在那条边界的射程内（[ADR 0028](./docs/adr/0028-avatar-stored-in-database.md)）
- **登录态从「纯无状态 JWT」变成半状态**：JWT 里带一个 token 版本号，每个带 token 的请求都会与库里的值比对，
  因此**每个请求多一次查库**。这是换「改密码即全场下线」的代价，理由与被否的替代方案见
  [ADR 0027](./docs/adr/0027-token-version-invalidates-all-devices.md)
- **`CONTEXT.md` 的「收藏夹」不再是错题本的禁用别名**：它现在指自己的收藏功能（[ADR 0030](./docs/adr/0030-favorites-as-named-folders.md)）

### Fixed

- **账户页正在输入的昵称会被自己弹回去**（2026-10-09）：进资料页时表单会跟随 `store.user` 回填一次；
  若此时 `/auth/me` 的响应随后返回（`store.user` 换了新对象），回填会**再跑一次、把用户刚敲进去的内容冲掉**
  ——表现为「打开资料页、手快开始改昵称，几百毫秒后输入框自己变回原值」。修法是表单加上 `dirty` 位：
  用户动过（改昵称 / 换头像 / 用默认头像）之后就不再回填，保存成功时复位。
  本机 localhost 上这个窗口只有几十毫秒、很难撞到，回归用例因此在拦截层给 `/auth/me` 加了 800ms 延迟才稳定复现
  （反向验证过：旧代码必挂、新代码必过）。
- **「修改收藏夹」面板被题库表格的行穿透**（2026-10-07）：面板挂在表格单元格里的星标上，而彼时的组件库弹窗
  默认不挂到 body，遮罩与面板被困在表格内部那一层——表格的行会盖到面板上来。重建后弹窗（reka）结构上
  portal 到 body，此坑消失；端到端仍以 `expectDialogOnTop` 盯着层级（命中测试 + 根因两条判据）。
- **「修改收藏夹」面板首次打开会谎报「还没有收藏夹」**：它的加载曾挂在第三方弹窗的 `@open` 上——
  星标用 `v-if` 懒挂载面板，「挂载时就已经是打开态」的情况下那个事件不触发，于是每个星标**第一次**打开都
  加载不出收藏夹。现加载绑在组件自己的 `visible` 上，并有端到端回归哨兵（这类错误接口层测不出来：
  数据全对，是界面在说谎）。

## [1.5.0] - 2026-10-06

### Added

- **新增 CI 守卫 `scripts/check-todo-archive.sh`**（[ADR 0025](./docs/adr/0025-archive-resolved-todos.md)）：
  断言「已结案的待办都已归档、且每条都已登记」——主索引表的状态列必须恰是「待办」、`docs/todo/` 下的文件头
  必须写明「状态：待办」、`docs/todo/` 与 `archive/` 下每个文件都要登记在索引里。**认位置不认词**，
  因为旧版只查「已解决」，漏掉过一个把状态写成「已实现」的文件。
  挂进「文档 · 链接与引用自检」job，与 `check-doc-links.sh`、`check-module-tests.sh` 同列。
- **PC 端新增「跟随系统 / 浅色 / 深色」三档外观**（切换入口在顶栏用户区，登录页只能跟随已存的偏好）：
  走 Element Plus 官方的深色变量表（`html.dark` + `theme-chalk/dark/css-vars.css`），自有样式里的硬编码色值
  全部改写成 `var(--el-*)` 语义变量，于是明暗两档共用一套样式。代码块高亮的深色配色单独手写——
  `highlight.js` 的浅色版与暗色版选择器同名，两份同时引入会互相覆盖。机制与约定见 [《前端》](./docs/design/前端.md)。
- **「题集」需求落地为固定卷的三处补齐**（[ADR 0026](./docs/adr/0026-question-set-reuses-paper.md)）：原需求是「自建题集、
  往里加已有或未有的题」，盘下来现有**固定卷**就是那个东西，于是不新增实体，改为把它的短板补上——
  - 试卷抽屉的**选题器**重做：关键词 + 分类 / 题型 / 难度筛选、后端翻页，**跨页勾选不再丢**
    （此前只拉第一页前 100 条、没有搜索，题一多就找不到）；
  - 抽屉里可以**内联新建题目**并自动入卷，省掉「先去题库建、再回来选」那一趟；
  - 题库页可以**多选**后「**加入已有试卷**」（并入，已在卷中的自动忽略）或「用所选新建试卷」。
- **导入时可以顺带建一张固定卷**（[ADR 0026](./docs/adr/0026-question-set-reuses-paper.md)）：JSON 与 Excel 导入都接受可选的
  `paperTitle`（走**查询参数**，裸数组契约不变——见 [ADR 0006 Amendment 1](./docs/adr/0006-bare-array-import-contract.md)）。
  给了卷名就把**本次成功导入的题**装进一张新固定卷（一题都没成功则不建，避免造空卷），导入结果里回 `paperId`。

### Changed

- **已结案的待办移入 `docs/todo/archive/`**（[ADR 0025](./docs/adr/0025-archive-resolved-todos.md)）：
  `docs/todo/` 主索引表此前把「待办」与「已解决」混列，结案件数一多，「还剩哪些没定」就被稀释。
  现结案即归档——文件 `git mv` 进 `archive/`（文件名不改），主表只留未决项，已结案的列进新的「已归档」区。
  本次归档 6 份（镜像分发、术语表补录、云上入口与宝塔共存、答题页判分反馈、历史作答统计重复计数、文档体系）。
  ⚠️ 归档后文件深一层，文内 `../` 链接要退一级（写进 `docs/todo/archive/README.md`）。
- **取消会话工作台的「惰性归档」**（[ADR 0018](./docs/adr/0018-session-worklog-as-doc-class.md) Amendment 1）：
  该机制（把超 7 天未完成的工作台标 `已归档（超期）`）上线后从未触发，且它是「其他会话默认不读 `docs/worklog/`」
  的唯一例外；去掉后该目录变成**严格只写自己那份**。`docs/worklog/` 自此**没有归档概念**。
- **补上一条漏登记的孤儿待办**：`docs/todo/2026-10-04-TODO-共享分类缺少归属校验.md` 头部不合模板、
  状态写成「已实现」、既没归档也不在索引里（旧守卫只查「已解决」，所以没抓到）——已规范化并 `git mv` 进
  `archive/`，索引「已归档」区补一行；`check-todo-archive.sh` 同时改成「认位置不认词」以杜绝同类漏网。
- **固定卷不再要求「必须至少选一道题」**（[ADR 0026](./docs/adr/0026-question-set-reuses-paper.md)）：空卷可以先存下来、
  之后再往里加题；但空卷**不能发起作答**，试卷列表里那个入口会置灰并给出提示。
  ⚠️ 服务端本来就有兜底（抽到 0 题时以业务错误拒绝），所以置灰只是把话说在前面，不是新增的防线。

### Removed

- **取消 `docs/decisions/`（决策过程日志）这一文档类别**（[ADR 0024](./docs/adr/0024-drop-decisions-doc-class.md)）：
  它与 `docs/worklog/` 都是「过程」记录、职责重叠，而 ADR 卡里的 Considered Options 已经承载「被否选项」，
  于是整类删掉——今后**结论只归 `docs/adr/`、过程只归 `docs/worklog/`**。原有 10 份日志下线（正文留在 git 历史里），
  文档地图、目录树、3 条规则、`AGENTS.md`、贡献指南里的引用，以及往该目录写东西的本机 skill `grill-decisions`，一并清理。

### Fixed

- **v1.4.0 里新加的那条端到端用例在 CI 上是红的，已修**（红的是**用例自己**，不是产品；修了两轮）：

  ① 点过下拉内部那行「全部分类（点此清空）」之后用 Escape 收尾**收不掉**——焦点不在输入框上，
  select 收不到那个按键，下拉一直开着；紧接着再点该下拉反而把它**关掉**，于是找选项时报
  「element is not visible」，看着像元素凭空消失。改成「两次下拉操作之间先在下拉外面点一下」
  （点「查询」最自然）。

  ② 断言写错了对象：「未分类」在**网址**里是界面态的 `categoryIds=none`（哨兵值），
  而 `uncategorized=true` 是**接口**上的参数、只在发请求那一处翻译出来——第一版把两者混为一谈。

  用例文件同时改名 `08-` → `09-`（`08-history.spec.ts` 已占用那个序号），旧名全仓无引用。
  ⚠️ 更正 [1.4.0] 段那句「端到端 2 条」：那两条里**当时只有 1 条是绿的**。

### Security

- **组装固定卷时开始校验题目可见性**。此前固定卷**完全不校验**：只要猜到题号（id 是自增的），
  就能把**别人的私有题**加进自己的卷，再通过作答把题干与解析读出来。
  现在按「**只校验新加进来的题，已在卷里的放行**」处理——放行那一半是刻意的：否则
  「编辑一张含自己已软删题的卷」会突然保存不了。可见性规则见 [《数据模型》](./docs/design/数据模型.md)。

## [1.4.0] - 2026-10-05

「题库页筛选」这一版：题目可以按分类（多选，含「未分类」）与标签筛，筛选条件写进网址、刷新与后退都不丢，
导出也跟着筛选走；顺带把题库页的默认范围收成「我的题库」，并把「改分类名」那句又长又绕的提示语改直白。

⚠️ **本版与当天上午那批「一题一答」的改动一起发出，没有为破坏性变更单独发一版**——这是对
[ADR 0023](./docs/adr/0023-release-cadence-by-risk.md) 第 3 档的一次**有意例外**：本版唯一的破坏性变更
（分类筛选参数改成多选）只影响本仓库的前端，而前后端在**同一次 release** 里一起部署
（compose 用同一个 `${APP_VERSION}`），不存在「旧前端打新后端」的窗口；且 `[Unreleased]` 里还压着
当天上午那批修复，为一次参数改名再多走一轮部署，换来的是接近于零的可定位性收益。
理由与例外条件原记在决策过程日志（该文档类别已于 2026-10-06 取消，见 [ADR 0024](./docs/adr/0024-drop-decisions-doc-class.md)）。

### Changed

- **答题改成「一题一答」：已作答的题只能回看，不能再改答案**（[ADR 0022](./docs/adr/0022-answer-is-final.md)）。
  此前三处口径不一：前端提交后即禁用选项、按钮变「下一题」，后端却允许重复提交（以最后一次为准），
  文档与接口测试也还写着「可回退改答案」。现在后端会拒绝重复提交（业务码 1000），
  文档、接口测试与服务集成测试同步对齐，口径统一到前端早已实现的行为上。
  **跳过的题回头再答不受影响**；想重做仍可另开一次练习。
- **题库页的「范围」默认从「全部」改成「我的题库」**，并在表格为空时给一句引导（分两种：
  「我的题库里还没有题目…」与「没有符合当前筛选条件的题目…」）。
  ⚠️ 代价写在明处：种子题库全是公开题，所以**刚注册的账号**打开题库页会看到空表——
  这是刻意的默认，不是坏了。想立刻看到题，把「范围」切到「全部」或「公开题库」。
- **题库页支持按分类与标签筛选**：分类**可多选**（与标签同为「或」的关系——勾了的分类里任何一个命中即可），
  并单独列一个「未分类」开关；两个下拉列的都是**全部**分类与标签。筛选条件**写进网址**
  （`route.query`），刷新与浏览器的前进后退都能原样恢复；改条件后点「查询」才发起请求，
  「重置」回到默认状态并把网址清干净。
- ⚠️ **题目列表与导出接口的分类筛选参数是破坏性变更**：`categoryId`（单个）→ `categoryIds`
  （一个参数可重复传值）＋ 新增 `uncategorized`。旧的 `categoryId` **直接删掉、不保留兼容**。
  迁移方式：把 `categoryId=x` 换成 `categoryIds=x` 即可；要「未分类」用 `uncategorized=true`。
  之所以敢不留兼容：这个接口只有题库页一个调用方（移动端从未调用过它）。
- **导出跟随筛选条件**：`GET /api/questions/export` 收下与列表页**同一套**筛选参数，导出的是匹配的
  全部题目（**只跟随筛选、不跟随分页**）。副作用写在明处：默认范围是「我的题库」，于是
  「什么都不改直接点导出」导出的只是自己的题、不再含公开题——要连公开题一起导，先把「范围」切到「全部」。
- **「改分类名」的提示文案改成直白版**：「会把你在这个分类下的全部题目迁到新分类；别人的题目不受影响。」
  语义**一个字没改**——仍然只迁移自己的题目。旧文案（「只把你自己的题目迁到新名字下。别人的题目仍留在
  原分类，原分类也不会因此消失。」）说的是同一件事，只是又长又绕。

### Fixed

- **PC 答题页的单选题、判断题在宽屏下把选项横排成一行，没有按要求逐行纵排**（多选题一直是纵排，所以看起来像「只有单选 / 判断串了」）。
  根因是 Element Plus 的 `.el-radio-group` 出厂样式是 `display:inline-flex; flex-wrap:wrap`——横向弹性盒、空间不够才换行；
  而 `.el-checkbox-group` 是普通块级容器，多选才正常纵排。现在把答题页的两个 group 统一改回块级，并让控件撑满整行。
- **所有题型、答题页与结算页的选项字母「A.」与选项文字分了两行，没有齐平**。
  根因是选项文字由 `MarkdownRenderer` 渲染，其最外层是**块级** `div.markdown-body`，块级元素必然另起一行，
  把紧跟其前的行内标号单独留在上一行。现在把承接标号与正文的容器改成横向 flex（与题目详情弹窗、移动 H5 的结构一致），
  两者成为同一行的弹性项，长选项换行时第二行也仍然对齐在文字列内。
  另外，渲染器给段落加的 `margin: 6px 0` 会把正文首行再顶下去 6px，选项内**首尾段的外边距一并清零**，
  标号与正文首行的**基线**才真正重合（否则看着还是「内容低了一截」）。
- **同一道题重复提交会把作答统计重复累加、并把错题本标记冲掉**（改成一题一答后不会再产生）：
  以前「答错 → 点亮错题本 → 改成正确」会让标记被抹掉，「哪道题没掌握」的信号当场丢失。
  历史已产生的偏差无法回溯修正（覆盖没有留痕），记在 `docs/todo/archive/2026-10-05-TODO-历史作答统计重复计数.md`。

### Added

- **已作答的题回看时显示正确答案与解析**（PC 与移动端）：此前只有「刚提交那一刻」看得到，
  翻回已答的题只剩自己的选择——既不能改、又看不到答案，回看等于白看。
  两端同时加了一句「本题已作答，答案不可修改」的提示，行为保持一致。
- **题库页的筛选能力补上两层断言**：接口层 5 条（分类多选、未分类、两者组合下的「或」语义、
  「未分类」不会绕过其它筛选条件、导出跟随筛选且不泄漏别人的私有题），端到端 2 条
  （默认范围与空表引导、按分类筛选 + 网址同步 + 重置）。
  其中「括号守卫」那条用例**反向验过**：故意去掉拼 OR 时的那层括号，它会红。

## [1.3.2] - 2026-10-05

> 补丁版：修 v1.3.1 里「导出 Excel」的两个文件名问题（后缀、时区）。字节流一直是好的。

### Fixed

- **导出 Excel 下载下来的后缀是 `.excel`，Excel 双击打不开**：前端自己拼文件名时，
  把「格式选择器的值」（`excel`）直接当成扩展名用了——真实扩展名是 `xlsx`。
  **导出的字节流一直是对的**（服务端 `Content-Disposition` 写的也是 `.xlsx`），坏的只是文件名；
  「导出 JSON」那条因为 `format` 与真扩展名恰好一致（`json`），才一直没露馅。
- **导出文件名里的时间戳是 UTC**：`new Date().toISOString()` 比北京时间早 8 小时
  （11:36 导出的文件叫 `...033653`，看着像坏的）。改成固定按 `Asia/Shanghai` 生成。

### Added

- **端到端补一条「下载 Excel 导出」的守卫**（`07-import-export.spec.ts`）：断言文件名形如
  `quizzy-questions-<14 位时间戳>.xlsx`，且下载到的文件头两字节是 `PK`（真 zip）。
  这条路径此前**没有任何用例下载过**，正是上面那个后缀 bug 能长期漏网的原因。

## [1.3.1] - 2026-10-05

> 补丁版：`v1.3.0` **发出去的 Excel 导出是坏的**（下面第一条），这一版把它修好并把漏掉的测试补上。

### Fixed

- 🐛 **修好 Excel 导出（此前一直是坏的）**：POI 5.2.5 会调
  `ZipArchiveOutputStream.putArchiveEntry(ZipArchiveEntry)`，而 easyexcel 4.0.3 传递进来的
  commons-compress 是 1.24.0、没有这个方法 → 实际写 xlsx 时抛 `NoSuchMethodError`。
  把 commons-compress 钉到 1.26.0 后恢复正常。
  这个 bug 一直没被发现，是因为**没有任何测试会真的解析 Excel**——本轮先补了烟测才暴露出来。
- **清掉 4 条 Maven 的 medium**：`commons-compress` 1.26.0、`commons-lang3` 3.18.0、
  `log4j-api` 2.25.5、`poi` / `poi-ooxml` / `poi-ooxml-lite` 5.4.0（POI 三个构件必须一起钉，
  否则版本错配）。前两个**可达**——xlsx 上传是公开注册后任何人可用的入口（ADR 0017）。
  至此仓库的 open 告警只剩「清不掉」的两类（`vite` 被 uni-app 钉死、`braces` 上游无修复版本）。

### Added

- **Excel 烟测 `ExcelRoundTripTest`**：写一个小 xlsx 再读回来，走的是生产代码**同一条**
  easyexcel 路径，不碰数据库、留在单元层（`mvn test` 仍不需要 Docker）。
  它钉住的是「POI / commons-compress 这条链还能不能用」——此前**完全没覆盖**的一条路径，
  也是这次能抓出上面那个导出 bug 的原因。

## [1.3.0] - 2026-10-04

### Added

- **接口层（真 Spring + 真 MySQL）的机制就位**：`pom.xml` 声明 `maven-failsafe-plugin`、接口与集成测试
  定为 `*IT` 命名，于是 `mvn test` **依旧只跑纯单元测试、不需要 Docker**（`mvn verify` 才跑接口层）。
  库用 Testcontainers 起真 MySQL——不用 H2，因为迁移脚本是 MySQL 方言
  （[ADR 0020](./docs/adr/0020-layered-test-system.md) 及其 Amendment 2）。先用 auth 模块打通：
  统一信封、鉴权（**鉴权失败的 HTTP 状态仍是 200、401 落在信封里**这条反直觉约定也钉住了）、
  重复用户名、密码错、校验错误各一条。
- **接口层铺满 6 个模块，机械守卫打开**：auth / category / paper / question / quiz / wrongbook
  各自有了接口测试（契约 + **数据边界**），并加了 `scripts/check-module-tests.sh`
  挂在 CI 上断言「每个带 Controller 的模块都配了测试类」（[ADR 0021](./docs/adr/0021-test-sync-discipline.md) 及其 Amendment 1）。
  守卫本身**正反两向**验过：正向 6 个模块全 ok，反向临时造一个无测试的模块必须报 MISSING。
- **CI 加依赖安全扫描**（`deps` job，`dependency-review-action`）：把 PR 里**新增或变动**的
  依赖拿去比已知漏洞库，只卡 high 及以上、且只看新引入的——存量噪音不进门禁。
  ⚠️ **只覆盖 npm**（三份 `package-lock.json`）：Maven 不会自动进 GitHub 的 dependency graph，
  **后端仍是盲区**；这条边界写在 ci.yml 的注释与决策日志里，免得误以为「扫过了」。
- **依赖扫描抓出并修掉 5 条真实漏洞**（都是它自己报的，不是凭感觉升的）：
  **Tomcat 10.1.55 → 10.1.60**，清掉 3 条 **critical**（GHSA-gcx9-497g-6cp6 / 9xv2-5v5q-p794 /
  h3x4-894j-xpx5，均网络可达、无需权限）；**jackson-bom 2.21.4 → 2.21.7**，清掉 2 条 **high**（DoS）。
  只覆盖 `tomcat.version` / `jackson-bom.version` 两个属性，不整体升 Spring Boot。
  ⚠️ 踩到一坑：advisory 写的修复版本 **10.1.58 已被撤版**（Central 上没有），照抄会挂构建。
- **依赖扫描补上后端**：新增 `deps-submit` job（`maven-dependency-submission-action`），
  把 Maven 的**含传递依赖**的完整树提交成依赖快照，`deps` 排在它之后才能把后端也算进比对。
  push 上也跑——master 那条基线必须常新，否则 PR 上「base 一侧」是空的。
- **端到端补完《覆盖清单》最后三块**：**导入**（JSON 粘贴入库 + 逐行报告）、
  **导出**（真的下载到文件，且内容里有刚建的那道题——不只验文件名）、
  **答题记录**（列表 / 按状态筛 / 跳回结果页）。端到端 11 → **14 条**，本机全过。
- **分类的生命周期改为跟着题目走**（安全问题，方案见
  [`docs/todo/archive/2026-10-04-TODO-共享分类缺少归属校验.md`](./docs/todo/archive/2026-10-04-TODO-共享分类缺少归属校验.md)）：
  分类是全体共用的，而服务已对公网开放，此前**任何注册用户都能删掉共享分类**并清空所有人的题目引用。
  现在：**诞生**随「保存题目」按名字自动建（同名复用，撞名即合并）、**改名字**只迁移自己的题目、
  **消亡**由系统在无人引用时自动回收；**主动删除的端点已关闭**，分类接口只剩列表与改分类名。
  顺带修好两处：① 新建题目对话框里那个 `allow-create` 分类下拉此前是坏的（把新名字塞给 Long 类型的
  `categoryId`），现在会转成 `categoryName` 交给后端按名字解析，并加了「改分类名」入口；
  ② **规则卷引用的分类若已不存在，那条条件自动失效**——不再是整张卷一道题都抽不到。
- **服务集成层铺满 6 个模块**：在上一批试卷 / 题目之外，补上 `QuizServiceIT`（会话两表一致、
  **判错点亮错题本标记**、结算进终态）、`WrongBookServiceIT`（**手动移出只翻标记、不删统计**、
  错题重练抽本里的题、每人一本）、`CategoryServiceIT`（重名 409、删分类清空题目分类引用）。
  接口 + 集成总数 41 → **52 条**。
- **服务集成层首批落地（P3）**：新增 `PaperServiceIT`（固定卷两表一致、改卷不残留旧关系、别人的卷 404）
  与 `QuestionServiceIT`（校验失败不留半成品、删题时**题目软删但作答统计被硬删**）。
  这一层**直接调 service 并查表**，不经过 HTTP——因为「VO 可以由入参拼出来」，
  关系表是不是真的一致只有查表才知道；「删了重导会丢统计」这个代价也写成了断言，钉在明处。
- **端到端收掉 P2 最后三条尾巴**：**错题本主链路**（答错进本 → 手动移出 → 不在本里）、
  **多选题判分**（少选不得分、全选才得分，ADR 0004——单元层与接口层都验过但**没走过 UI**）、
  **裁切点名**（此前 `expectNotClipped` 是死代码，现在点名了登录卡片与侧边菜单）。
  顺带把「造题 / 组卷 / 从试卷发起作答」抽成公共助手，并让建题组卷那条用例**真正断言
  「刚建的那道题被选进了卷」**（原来只是随便勾第一道）。
- **端到端再补两条**：**建题 → 组固定卷 → 用这张卷作答**（跨题目 / 试卷 / 答题会话三个模块，
  只有端到端能串起来），以及**断点续答**（答完一题刷新，位置继续在未作答的题上——这条曾经是坏的）。
- **端到端补上核心链路与排版几何断言**：①一条主链路（注册 → 快速练习 → 答题 →
  **判分反馈即时显示** → 结算 → 结果页四项统计），②几何断言（页面不横向溢出、主内容区不越界，
  1280 / 768 两个宽度，覆盖登录页与主框架内五个页面）。
  **视口刻意不含 375**：按 [ADR 0019](./docs/adr/0019-mobile-clients-with-uniapp.md) 移动端另有
  quizzy-mobile，quizzy-web 实测 PC 布局最小约 501px；裁切（内容被裁掉）也**不做通用断言**——
  表格 / 代码块本来就可滚动，必然误报，改成了调用方点名的 `expectNotClipped`。
- **CI 新增 `api` job**（`mvn verify`；Testcontainers 自己起库，所以不需要 `services`）。
- **新增移动端工程 `quizzy-mobile`**（uni-app · Vue3 · TypeScript · Pinia · wot-design-uni），
  本轮只启用 H5 编译目标，落地「登录/注册 → 快速练习 → 答题 → 结果」一条主线。
  架构见 [ADR 0019](./docs/adr/0019-mobile-clients-with-uniapp.md)（PC 端不动、移动端只做「消费」）。
  小程序编译目标、`/m/` 部署产物与错题本/记录页留待后续。
- **CI 新增 `mobile` job**：类型检查 + `build:h5` + 一条 `diff` 守卫（移动端的 `types` 必须与 PC 端逐字一致）。
- **建立分层测试系统**：三层——单元（已有）、接口与服务集成（真 Spring + 真 MySQL，Testcontainers）、
  端到端（Playwright，**只在 CI 的一次性环境**跑）。两个前端工程的依赖树**不动**，端到端隔离在独立工程 `e2e/` 里。
  排版分三类：**几何事实**（溢出 / 越界 / 裁切）由端到端层的断言覆盖并进 CI，
  **审美与层级**靠 AI 探索粗筛 + 人眼，**视觉回归截图比对不做**。
  见 [ADR 0020](./docs/adr/0020-layered-test-system.md)（含 Amendment 1）与 [《测试系统说明》](./docs/testing/系统说明.md)，
  落地的分阶段计划见 [docs/todo/2026-10-04-TODO-测试系统落地.md](./docs/todo/2026-10-04-TODO-测试系统落地.md)。
- **端到端层已落地（P0）**：新增工程 [e2e/](./e2e/README.md)（Playwright，独立 `package.json`），
  并给 `ci.yml` 加了 `e2e` job——**这是后端第一次在流水线里被真正启动**（此前 `mvn test` 不拉 Spring、
  镜像 job 只 `docker build`）。一次性环境是 `mysql:8` service + 后端进程 + 前端 preview，跑完随 runner 销毁；
  测试配置**直接注入 job 的 env、不建 `.env` 文件**，顺带解开了「不敢 `compose up`」那个结。
- **登录页的输入框与注册按钮加了 `data-testid`**：登录与注册两个 Tab 同时挂在 DOM 上，
  「用户名」「密码」这类文案各重复一次，按文案定位会撞名——只给这类元素加，其余仍用语义定位。
- **新增 AI 探索测试任务书** `docs/testing/agent-exploration.md`：由人手动触发、**产出报告而非断言**，
  不进 CI（探索测试不可重复，不是测试）。任务书**自包含、平台无关**（写入能力自检，缺能力必须声明），
  报告写到 `.workbuddy/exploration/`（已被 gitignore，**不入库**）。
- **新增测试同步纪律**：改动 → 补哪层测试由一张映射表决定，测试与功能进**同一个 PR**；
  另计划一条 CI 机械守卫（每个 `module/*/controller` 至少有一个测试类，**尚未落地**，见 P1），
  见 [ADR 0021](./docs/adr/0021-test-sync-discipline.md)。

### Fixed

- **清掉 npm 侧能清的存量漏洞（10 个包）**：`adm-zip` / `qs` / `cookie` / `send` / `postcss` /
  `esbuild` / `postcss-selector-parser` / `@babel/core` / `@intlify/*`，以及 web 侧的 `vite` 6.4.3。
  直接依赖改版本号、传递依赖用 `overrides`；锁文件在**干净临时目录**里重新生成
  （在有 `node_modules` 的目录里 npm 只写当前平台的原生可选依赖，换到 Linux CI 上装不到）。
  仓库的 open 告警 **39 → 21**。剩下两类**清不掉**，已写明原因记在
  [`docs/todo/2026-10-04-TODO-依赖存量漏洞清理.md`](./docs/todo/2026-10-04-TODO-依赖存量漏洞清理.md)：
  ① mobile 的 `vite` 被 `@dcloudio/vite-plugin-uni` 的 peer **钉死在精确的 5.2.8**（要升得先升 uni-app）；
  ② `braces` 的公告范围是 `<= 3.0.3` 而**上游最新就是 3.0.3**（还没有修复版本）。

- **改正一处由荧自己造成的错误结论，并补上此前完全缺失的断言**：接口测试的注释与 todo 里曾写
  「抽题规则里没有题量」——**那是错的**。`PaperRuleDTO` 有 `count`（默认 20、夹在 1–200），
  `PaperService#selectQuestionsByRule` 会 `ORDER BY RAND() LIMIT count`：题量一直生效，语义是**上限**。
  误判来自荧用一条紧凑正则导出 DTO 字段，漏掉了带初始化值的 `private Integer count = 20;`。
  顺带补上**此前没有任何用例覆盖**的语义：传 `count: 3` 必须正好抽 3 道、传 `count: 0` 夹到 1 道
  ——在此之前 34 个接口用例没有一个碰得到它，谁删掉 `LIMIT count` 都会全绿通过。
- **两端答题页提交后都不再显示判分反馈**：`onSubmit()` 里 `load()` 会把当前题挪到「第一道未答题」，
  并在 `syncPicked()` 里把 `feedback` 清成 null——PC 端紧跟着的 `feedback.value = feedback.value`
  是自赋值，反馈永远不显示；移动端首版则是「题干是下一题、解析是上一题」，点「下一题」还会跳过一题。
  改为**记住当前题号、刷新会话后把位置与反馈放回去**，两端行为已对齐。

### Changed

- **`docs/testing.md` 迁为 `docs/testing/README.md`**：`docs/` 下新增 `testing/` 主题目录，
  按「现状 / 设计意图 / 探索任务书」拆成三份文件，全仓引用同步修正。
- **MySQL 从「不发布任何端口」改为**只绑宿主回环（`127.0.0.1:3306:3306`），
  好让本机能经 SSH 隧道直连——IDEA 的数据库工具与临时排障都靠这个稳定落点。
  **公网仍然够不着**：已实测宿主 `ss` 只显示回环地址、从公网探 3306 拒连。
  取舍见 [ADR 0016 Amendment 2](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)。
- **版本号手写点从两处回到三处**：新增 `quizzy-mobile/package.json`，
  `check-version.sh` 相应加两条校验（[ADR 0015 Amendment 2](./docs/adr/0015-version-number-governance.md)）。

---

## [1.2.0] - 2026-10-03

「搬到云上」这一版。prod 从本机 Docker Desktop 迁到阿里云轻量服务器，交付链路改成
「推 tag → CI 构建推 ACR → SSH 自动部署」，并顺手补掉了几处一直缺的安全与可靠性设置。

### Changed

- **prod 从本机搬到阿里云轻量服务器，交付链路改为「推 tag → CI 构建推 ACR → SSH 部署」**
  （[ADR 0016](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)，取代 ADR 0013）：
  - `docker-compose.prod.yml` 变为**云上编排**：镜像来自 ACR、不再有 `build:`、web 只绑 `127.0.0.1:8081`、
    MySQL 与后端都不发布端口、按 2C2G 用 `JAVA_TOOL_OPTIONS` 钉死 JVM 内存、三个服务统一加日志上限
  - 新增 `.github/workflows/release.yml`：`version` → `flyway-verify` → `build-push` → `deploy` 四个 job 串行；
    tag 触发从 `ci.yml` 移除，tag 的处置权统一收进它
  - 新增 `scripts/server-deploy.sh` / `server-rollback.sh`（在服务器上跑；部署前强制备份、mysql 永不重建）；
    **删除本机版 `scripts/deploy.sh`**——本机从此只剩开发形态
  - 新增 `deploy/`：宿主 nginx 站点模板与云端 `.env` 模板
- **HTTPS 与对外入口交给宿主上既有的 nginx（宝塔）**，不再自建 `acme.sh` + DNS-01
  （[ADR 0016 Amendment 1](./docs/adr/0016-cloud-deploy-with-release-pipeline.md)）
- **版本号手写点从三处减到两处**：compose 的 `image:` 改用 `${APP_VERSION}` 变量，
  `check-version.sh` 相应改成结构断言（[ADR 0015 Amendment 1](./docs/adr/0015-version-number-governance.md)）

### Added

- **迁移首次被 CI 验证**：新增的 `flyway-verify` job 在一次性 `mysql:8` 上按顺序重放 `db/migration`，
  并断言文件名合 Flyway 约定、版本号严格递增——此前 `mvn test` 完全不碰数据库

### Fixed

- **「改完前端必须硬刷新」这个老毛病消掉了**：容器 nginx 原先没有 `Cache-Control`，
  现在 `index.html` 明确不缓存、带内容 hash 的 `assets/` 长缓存
- **CORS 不再对任意来源开放**：原来是 `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 并存，
  现在默认**完全不注册 CORS**（前端与 `/api` 同源本就不需要），需要时用 `QUIZZY_CORS_ALLOWED_ORIGINS` 显式开
- **后端不再直接对公网暴露**：`8080` 端口映射删除，只经容器 nginx 暴露 `/api/`（Swagger 因此也不再对外）

---

## [1.1.0] - 2026-10-01

「关于」弹窗与版本号治理。这一版把版本号从「四个互不同步的值」收敛成「`git tag` 一个真相源 +
三处手写镜像 + 一道门禁」，并给它造了一条注入前端产物的通道。

### Added

- **「关于」弹窗**：顶栏可查当前版本号与构建信息。版本主行取自 `git tag`，副行只在 `HEAD` 领先 tag 时出现
- **版本号一致性门禁**：`scripts/check-version.sh` + CI 的「版本 · tag 与三处对齐」job，
  推 tag 时校验 `package.json` / `pom.xml` / compose 镜像 tag 与 tag 是否一致（[ADR 0015](./docs/adr/0015-version-number-governance.md)）

### Changed

- **版本号改由构建参数注入前后端产物**（此前前端根本没有版本号通道，后端则是写死的值）：
  `scripts/deploy.sh` 从 `git tag` 算出后经 compose 的 `build.args` 传入。swagger UI 上显示的版本
  也从原先写死的「产品代次」改为真实版本号
- **镜像 tag 跟随版本号**（`quizzy-server:1.1.0` / `quizzy-web:1.1.0`）。于是退回上一个已发版本
  可以复用本地镜像、不必重新构建；代价是同一版本号内的多次构建仍互相覆盖
- **取消「产品代次 v1」这个说法**：`README.md` 与 `docs/requirements/scope.md` 改为直白的范围描述，
  `CONTEXT.md` 收录「版本号」一词并注明该说法已取消

---

## [1.0.0] - 2026-09-26

第一个正式版本。从「答题程序 v1」首个提交一路到文档体系与 CI 门禁落地，`ae273cb` → `v1.0.0`。

### Added

- **答题程序 v1**：选择题（单选 / 多选 / 判断）的题库管理、固定卷与规则卷、逐题作答与即时判分、错题本、历史会话（[ae273cb](https://github.com/hhh666hhh666/quizzy/commit/ae273cb)）
- **题库列表页支持查看题目详情**（[9661bf0](https://github.com/hhh666hhh666/quizzy/commit/9661bf0)）
- **全容器 Docker 化**：dev / prod 双 compose，MySQL 数据绑定挂载到宿主机（[7c5a996](https://github.com/hhh666hhh666/quizzy/commit/7c5a996)，ADR 0007）
- **后端健康检查与数据库备份脚本**（[5008ace](https://github.com/hhh666hhh666/quizzy/commit/5008ace)，ADR 0012）
- **CI 门禁**：后端单测、前端类型检查与构建、两个镜像在干净 Linux 上冒烟构建（[d513539](https://github.com/hhh666hhh666/quizzy/commit/d513539)，ADR 0013）

### Changed

- 镜像构建改用 glibc 基础镜像、前后端补 `.dockerignore`、构建期依赖走国内镜像源（ADR 0009、ADR 0010）
- CI 用到的 actions 升到当前大版本（[8bb2c1b](https://github.com/hhh666hhh666/quizzy/commit/8bb2c1b)）
- 出题中间产物 `.qbgen/` 不再入库（[159c023](https://github.com/hhh666hhh666/quizzy/commit/159c023)）

### Security

- **公开的 JWT 默认密钥不再用于签名**（[2984719](https://github.com/hhh666hhh666/quizzy/commit/2984719)，[ADR 0011 Amendment 1](./docs/adr/0011-env-file-with-local-defaults.md)）
  仓库转为公开后，`application.yml` 里那串默认密钥成为全网可知的字符串，可被用来签发任意 `userId` 的 token。
  现在：默认值仍保留（宿主机裸跑不需要 `.env`），但一旦检测到用它，改为每次启动随机生成并打 WARN；prod compose 则直接把密钥设为必填。
  ⚠️ **任何在这个修复之前用默认密钥部署的实例，都必须轮换 `QUIZZY_JWT_SECRET` 并让所有用户重新登录。** 轮换方式见 [docs/operations/configuration.md](./docs/operations/configuration.md)。
  ⚠️ 仍未解决：`MYSQL_ROOT_PASSWORD` 的默认值同样已公开，见 ADR 0011 末尾。

### Documentation

- 术语表 `CONTEXT.md` 与 ADR 0001–0008（[af1f653](https://github.com/hhh666hhh666/quizzy/commit/af1f653)）
- ADR 0009–0012（[df31345](https://github.com/hhh666hhh666/quizzy/commit/df31345)）
- `docs/decisions/` 决策过程日志（[1541cd4](https://github.com/hhh666hhh666/quizzy/commit/1541cd4)）
- MySQL 题库笔记（[d1d9715](https://github.com/hhh666hhh666/quizzy/commit/d1d9715)）
- CI/CD 那轮的决策日志归档（[3b77ec8](https://github.com/hhh666hhh666/quizzy/commit/3b77ec8)）

---

## 维护约定

- 提交信息沿用 Conventional Commits（`feat:` / `fix:` / `docs:` / `chore:`），`git log` 就是归类依据。
- 变更记入 `[Unreleased]`；**用户可感知的破坏性变更必须在版本段里单独说明迁移方式**。
- 下一个版本发布时：把 `[Unreleased]` 里的内容固化成新的版本段 → 打附注 tag（`git tag -a vX.Y.Z -m "vX.Y.Z"`）→ **单独推送 tag**（`git push origin vX.Y.Z`，它不随普通 push 走）→ 更新底部两个比较链接 → 跑一遍 `bash scripts/check-version.sh vX.Y.Z` 应当全绿 → **建 GitHub Release（见下三条）**。
- **只推 tag，`/releases` 页会是空的**：那一页列的是 **Release 对象**，不是 tag。本项目 `v1.2.0`～`v1.5.0` 就这么漏了——
  页面上只剩 `v1.0.0` / `v1.1.0`，`Latest` 一直停在 `v1.1.0`，看着像一个月没发版（2026-10-06 补建了那 6 条）。
  建法：`gh release create vX.Y.Z --title vX.Y.Z --notes-file <正文>`；正文取本文件的对应版本段，**但段里的相对链接必须改写成指向该 tag 的绝对地址**
  （`](./docs/...` → `](https://github.com/hhh666hhh666/quizzy/blob/vX.Y.Z/docs/...`），否则在 Release 页上全是死链。
  ⚠️ 改写后**双向自检**：既要数「还有没有 `](./`」，也要数「`]后直接跟 https`」——后者能抓到「把 `](` 一并吃掉」的改写 bug（第一次就是这么错的）。
- 建 Release **不触发** `release.yml`（它只听 `push: tags`），所以补建历史版本不会二次部署；也正因为如此，**忘了建不会有任何人提醒**，只能靠这条约定兜着。
- 刻意**不把建 Release 做成自动化**：正文是给人看的说明书，手写比搬运本文件更用心，而发版是低频、需要人过目的动作。
- 许可证见 [LICENSE](./LICENSE)。

[Unreleased]: https://github.com/hhh666hhh666/quizzy/compare/v1.5.0...HEAD
[1.5.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.3.2...v1.4.0
[1.3.2]: https://github.com/hhh666hhh666/quizzy/compare/v1.3.1...v1.3.2
[1.3.1]: https://github.com/hhh666hhh666/quizzy/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/hhh666hhh666/quizzy/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/hhh666hhh666/quizzy/compare/ae273cb...v1.0.0
