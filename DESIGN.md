---
name: Quizzy
description: 个人自学刷题工具——一套「安静的练习室」视觉系统
colors:
  # 主色
  practice-blue: "oklch(55% 0.18 262)"
  practice-blue-wash: "oklch(93% 0.045 262)"
  chalk-white: "oklch(100% 0 0)"
  # 面（冷调浅面，全部白名单内）
  desk-blue-pad: "oklch(96.5% 0.012 250)"
  paper: "oklch(99% 0.005 250)"
  slate-pad: "oklch(94% 0.02 250)"
  practice-sheet: "oklch(100% 0 0)"
  # 线
  pencil-line: "oklch(89% 0.015 250)"
  pencil-line-soft: "oklch(93% 0.01 250)"
  # 墨（四档字色）
  heavy-ink: "oklch(16% 0.02 265)"
  ink: "oklch(23% 0.025 265)"
  faded-ink: "oklch(48% 0.025 265)"
  pale-ink: "oklch(62% 0.02 265)"
  # 状态三对（本体 + 浅底）
  check-green: "oklch(55% 0.14 155)"
  check-green-soft: "oklch(94.5% 0.05 155)"
  warning-amber: "oklch(66% 0.15 75)"
  warning-amber-soft: "oklch(95% 0.05 75)"
  red-pen: "oklch(56% 0.19 25)"
  red-pen-soft: "oklch(95% 0.045 25)"
  # 墨系深档（tint 底上的彩字/图标，配五张便签用）
  inked-blue: "oklch(46% 0.15 262)"
  inked-green: "oklch(46% 0.13 155)"
  inked-violet: "oklch(46% 0.14 300)"
  inked-amber: "oklch(46% 0.12 75)"
  inked-red: "oklch(46% 0.16 25)"
  # 五张便签（受控色板：蓝 = 默认容器色，不许加第六张）
  note-blue: "oklch(94.5% 0.035 255)"
  note-cyan: "oklch(95% 0.035 205)"
  note-violet: "oklch(94.5% 0.035 300)"
  note-green: "oklch(95% 0.035 155)"
  note-amber: "oklch(96% 0.04 85)"
  # 收藏两枚身份色（非主题派生）
  bookmark-gold: "#EAC54F"
  night-watch-amber: "#F59E0B"
typography:
  display:
    fontFamily: "system-ui, -apple-system, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "28px"
    fontWeight: 600
    lineHeight: 1.3
  headline:
    fontFamily: "system-ui, -apple-system, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "18px"
    fontWeight: 600
    lineHeight: 1.6
  title:
    fontFamily: "system-ui, -apple-system, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: 1.75
  body:
    fontFamily: "system-ui, -apple-system, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.75
  label:
    fontFamily: "system-ui, -apple-system, Segoe UI, PingFang SC, Microsoft YaHei, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.55
rounded:
  xs: "5px"
  sm: "8px"
  md: "10px"
  lg: "14px"
  xl: "20px"
  xxl: "24px"
spacing:
  xs: "6px"
  sm: "8px"
  md: "16px"
  lg: "24px"
components:
  button-primary:
    backgroundColor: "{colors.practice-blue}"
    textColor: "{colors.chalk-white}"
    rounded: "{rounded.md}"
    height: "36px"
    padding: "0 10px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    height: "36px"
    padding: "0 10px"
  card:
    backgroundColor: "{colors.paper}"
    rounded: "{rounded.xl}"
    padding: "24px"
  input:
    backgroundColor: "{colors.practice-sheet}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    height: "36px"
  chip:
    backgroundColor: "{colors.note-blue}"
    textColor: "{colors.inked-blue}"
    rounded: "{rounded.sm}"
    padding: "2px 8px"
  nav-item-active:
    backgroundColor: "{colors.practice-blue-wash}"
    textColor: "{colors.inked-blue}"
    rounded: "{rounded.md}"
    height: "41px"
  dialog:
    backgroundColor: "{colors.practice-sheet}"
    rounded: "{rounded.xl}"
    padding: "24px"
  toast:
    backgroundColor: "{colors.paper}"
    rounded: "{rounded.lg}"
    padding: "12px"
  quiz-option:
    backgroundColor: "{colors.practice-sheet}"
    rounded: "{rounded.lg}"
    padding: "16px"
  quiz-option-selected:
    backgroundColor: "{colors.note-blue}"
    rounded: "{rounded.lg}"
    padding: "16px"
---

# Design System: Quizzy

## Overview

**Creative North Star: "安静的练习室" (The Quiet Practice Room)**

一个人，一摞题，反复练到会——这间房间里没有倒计时、没有排名、没有围观。整套视觉围绕这一句建成：**唯一该干净的地方是读文字的那一层**。页面底是一片冷调的浅蓝「桌垫」，卡片与浮层是近白的「练习纸」，两者之间的色差与留白承担全部结构——不靠粗线、不靠重色块。颜色被严格收在蓝灰轴（oklch 色相 250–265）上，彩度只出现在五张「便签」与两枚收藏身份色里。

语气是**克制而笃定**：少色、大留白、行高宽松（1.45–1.75）、动效快而少（100–200ms，且只表达状态）。界面不说教、不催促——判分反馈把事实（你的答案 / 正确答案）摆出来就退场。所有"浮起来"的东西（弹窗、下拉、提示）用同一种物理语义：实底练习纸 + 1px 铅笔线 + 遮罩压暗；唯一允许透光的是顶栏那一层轻玻璃。

**Key Characteristics:**
- 冷调低彩度的"桌垫 + 练习纸"双层底色，结构靠色差与留白
- 唯一近白不上色的一层：阅读面（题干 / 解析 / 浮层）
- 受控色板：五张便签（tint）+ 五枚墨系深档，不许随手扩充
- 对 / 错永远"颜色 + 文字"双通道，色盲与高对比模式下照读
- 圆角整体大一档（5→24px）的"宽松"口径；行高是宽松的主要杠杆
- 两套材质语言泾渭分明：铺底用"面"，浮层用"练习纸实底"，顶栏用轻玻璃——同类元素不许混

## Colors

调色板性格一句话：**冷、低彩度、受控**——除便签与收藏两色外全部围绕蓝灰轴，彩只做强调不做环境。

### Primary
- **练习蓝 Practice Blue** (oklch(55% 0.18 262)): 主按钮、链接、键盘焦点环、选中态文字。界面上唯一的高彩度常驻色，出现得越少越有力。
- **练习蓝·晕 Practice Blue Wash** (oklch(93% 0.045 262)): 选中与高亮的**底**——侧栏当前项、下拉悬停、选项卡选中。
- **粉笔白 Chalk White** (oklch(100% 0 0)): 练习蓝之上的文字。深色实底不用黑字，这是房间里唯一"写在黑板上"的时刻。

### Neutral
- **桌垫蓝 Desk-Blue Pad** (oklch(96.5% 0.012 250)): 页面底色。整个应用是一张桌垫。
- **纸 Paper** (oklch(99% 0.005 250)): 卡片 / 内容面。
- **垫板 Slate Pad** (oklch(94% 0.02 250)): 侧栏、表格表头、悬停底、交替带。
- **练习纸 Practice Sheet** (oklch(100% 0 0)): 阅读面——题干 / 解析 / 弹窗底。**系统里最重的一枚约定**。
- **铅笔线 Pencil Line** (oklch(89% 0.015 250)) / **淡铅笔线** (oklch(93% 0.01 250)): 1px 分隔。分界永远细，重量交给留白。
- 四档墨：**浓墨** (oklch(16% 0.02 265)) 底栏昵称等最强强调；**墨** (oklch(23% 0.025 265)) 正文；**灰墨** (oklch(48% 0.025 265)) 次要文字（小字一律用它）；**淡墨** (oklch(62% 0.02 265)) 只允许大字（≥19px 粗体）与纯装饰。

### 状态与便签
- 三对状态色：**对勾绿** oklch(55% 0.14 155) / **提醒琥珀** oklch(66% 0.15 75) / **红笔** oklch(56% 0.19 25)，各配一枚浅底（-soft）做容器面——浅底上的小字用墨色，不用状态色本体。
- 五枚**墨系深档**（inked-*，oklch 46% 档）：便签底上的彩色文字 / 图标。每枚都在对应便签底上 ≥4.5:1。
- 五张**便签**（note-*）：分区与强调块的受控色板。**蓝是默认容器色**；其余四色留给分区。
- 收藏两枚身份色：**书签金 #EAC54F**（星标 hover / 已收藏）、**守夜琥珀 #F59E0B**（侧栏收藏夹图标）。与语义色无关，不许互换。

### Named Rules
**The One Clean Layer Rule (一层净面).** 阅读面永远是纯白 practice-sheet——题干、解析、弹窗底**不许上色**。刷题时绝大多数时间盯的就是它，那是这间房间里唯一必须干净的地方。
**The No-Color-Only Rule (双通道).** 对 / 错不许只靠颜色——必须同时给出文字（你的答案 / 正确答案）或图标（对勾 / 叉）。
**The Five Notes Rule (五张便签).** tint 受控色板恰好五张；想加第六张先过 ADR 级讨论，并给它配一枚墨系深档。

## Typography

**Display / Body 同族**：系统栈 `system-ui, -apple-system, "Segoe UI", "PingFang SC", "Microsoft YaHei", "Noto Sans SC"`（不引入 Web 字体——国内加载会失败，也拖慢首屏；自托管中文子集是后续增强）。等宽：`ui-monospace, SFMono-Regular, "JetBrains Mono", Consolas`，代码块专用。

**Character:** 标题与正文同族、只差字重——整间房间只用一把嗓子。中文需要呼吸，所以**行高整体偏松**（1.45–1.75），字阶细（11/12/13/14/16/18/22/28），对比靠字重与颜色不靠体型。

### Hierarchy
- **Display** (600, 28px, 1.3): 结果页指标数字——全站最大字号，只在这里。
- **Headline** (600, 18px, 1.6): 品牌字标（Quizzy）与登录页。
- **Title** (500, 14px, 1.75): 页面与弹窗标题、分区小标题。
- **Body** (400, 14px, 1.75): 正文与阅读面主档。
- **Label** (500, 12px, 1.55): 标签、表头、辅助说明。13px（约 0.81rem，行高 1.65）是正文的次级档，用于表格与紧凑区。

### Named Rules
**The Loose Lines Rule (松行距).** 行高是"宽松"的主要杠杆：中文正文 ≥1.75，13px 档 ≥1.65，任何地方不得低于 1.45。

## Layout

- 应用壳：**固定 200px 侧栏**（垫板色）+ **56px 轻玻璃顶栏** + 可滚内容区（`overscroll` 桌面端抑制）。
- 内容密度两档：**表格页铺满**（题库 / 试卷 / 记录 / 收藏），**表单与阅读页收窄**（个人账户 720px 上限）。
- 节奏：间距只用四档——6 / 8 / 16 / 24px；卡片内边距 24px，弹窗头部与底栏条 16px 纵向。
- 表格：表头垫板色半透明带、行高由 10px 竖向内距撑开、行间 1px 淡铅笔线；数字列等宽数字（tabular-nums）。
- 响应式：PC 优先（主要使用场景是桌面）；弹窗在窄屏收窄至视口 −2rem，网格降列。

## Elevation & Depth

这个系统**不用重阴影做层次**——深度由色调分层表达（桌垫 → 纸 → 垫板 → 练习纸），阴影只做状态回应，且**用冷调色相**（oklch 30% 0.03 260，不用纯黑——"浮起来"但不脏）。浮层的物理语义统一为：遮罩压暗页面（`bg-black/10` + 轻微底色模糊）+ **近白练习纸实底** + 1px 焦点环描边。顶栏是唯一的例外：一层不模糊的轻玻璃（白色 .68），因为它背后确实有滚过的内容可透。

### Shadow Vocabulary
- **微影 `--shadow-xs`** (`0 1px 2px oklch(30% 0.03 260 / 0.05)`): 细分隔上的极轻浮起（如头像块）。
- **静影 `--shadow-sm`** (`0 2px 4px -1px / 0.07 + 0 1px 2px -1px / 0.05`): 卡片默认。
- **浮影 `--shadow-md`** (`0 8px 20px -6px / 0.12 + 0 2px 6px -2px / 0.07`): 悬停抬起与浮层。
- **玻璃影 `--glass-shadow`**: 为将来的模糊浮层预置，当前无使用者。

### Named Rules
**The Two Materials Rule (两套材质).** 铺底（页 / 卡 / 表 / 表单）用"面"，浮层用"练习纸实底 + 描边 + 遮罩"，顶栏用轻玻璃——**同类元素只准用一种语言**，混用会像两个人做的。
**The Glass Has Content Behind It Rule (玻璃要有可透之物).** 玻璃背后必须有滚动内容；浮在纯白上等于白付性能代价。玻璃上**不许放长正文**。

## Shapes

形状口径一句话：**整体比第一版大一档的宽松圆角**。标尺 xs 5 / sm 8 / md 10 / lg 14 / xl 20 / xxl 24：按钮与输入 md(10)、卡片 lg–xl(14–20)、弹窗 xl(20)、小标签与便签 sm(8)、玻璃浮层留 xxl(24)。边框永远 1px 铅笔线系；**大分界从不交给粗线**。唯一的圆形元素是用户头像（方块生成图的圆形裁剪）与状态圆点。

## Components

### Buttons
- **Shape:** 圆角 10px（md）；尺寸比通用组件库整体大一档（默认高 36px、图标钮 28/32/36）。
- **Primary:** 练习蓝底 + 粉笔白字，高 36px，横距 10px，字 13px/500。
- **Hover / Focus:** hover 降不透明度；焦点为 2px 练习蓝焦点环（`focus-visible`，鼠标点击不出现）；按下位移 1px。
- **Outline / Ghost / Link:** 描边款用于次级动作；link 款只用于"查看 / 编辑 / 移除"这类行内动作——**统一挂 `.link-button`**（hover 下划线 + 键盘焦点环）。
- **Destructive:** 红笔浅底 + 红笔字（软性危险），只有注销账号用实底危险钮。

### Chips（便签标签）
- **Style:** 便签底 + 墨系深档字，12px/500，圆角 8px。题型 / 难度 / 状态 / 来源全走这一件。
- **State:** 无交互态（是标签不是按钮）；状态对错同时带文字（双通道规则）。

### Cards / Containers
- **Corner Style:** 20px（xl）。
- **Background:** 纸（surface）；表达型页面（登录 / 结果）用便签蓝做大块容器。
- **Shadow Strategy:** 默认静影 sm；悬停才升到 md。
- **Internal Padding:** 24px；内部小节间距 16px。

### Inputs / Fields
- **Style:** 36px 高、圆角 10px、1px 铅笔线描边；卡片内字段用**练习纸白底**（与纸卡形成层次），列表页搜索框同理。
- **Focus:** 2px 练习蓝焦点环（focus-visible）+ 描边加亮。
- **Disabled / Error:** 禁用降透明；错误内联在字段下方（红笔色小字）。

### Navigation
- **Style:** 200px 垫板色侧栏；项高 41px、圆角 10px、14px 字。当前项 = 练习蓝·晕底 + 墨蓝字 + 图标同色；未选中悬停 = 纸色底。
- **Signatures:** 「收藏夹」图标恒为守夜琥珀；侧栏底部 = 圆框头像 + 昵称（浓墨）+「当前外观 / 关于」单行齐平；顶栏只留页面标题（13px/500 + 轻玻璃底）。
- **Mobile:** 移动端（uni-app）不搬侧栏，走底部标签栏 + 单列。

### Dialog
- **Shape:** 圆角 20px、练习纸实底、1px 焦点环描边；遮罩压暗 + 轻微模糊。
- **Anatomy:** 头部标题 14px/500 + 淡铅笔线分隔；内容区可滚；底栏为垫板色半透明条（取消 / 主行动）。
- **Behavior:** reka Dialog（portal 到 body）；Esc / 遮罩关闭；破坏性操作一律先过确认框（自建 MessageBox）。

### Quiz Option（签名件）
- **Shape:** 整行可点卡片——圆角 14px、练习纸白底、1px 铅笔线；选项字母加粗与正文同排。
- **State:** 选中 = 便签蓝底 + 练习蓝描边（原生 radio / checkbox 驱动，`:has(:checked)` 上色）；已作答锁定；判分反馈区（对勾绿-soft / 红笔-soft 底 + 深字色）带正确答案与解析。
- **Rule:** 点整行即选（label 即命中区），键盘原生可达。

### FavoriteStar（签名件）
- **Style:** 线性星 → hover / 已收藏变书签金 #EAC54F；短按收藏 / 取消，长按 500ms 打开收藏夹面板。
- **Feedback:** 右下角提示条带「修改收藏夹」/「撤销」动作按钮。

## Do's and Don'ts

### Do:
- **Do** 把阅读面（题干 / 解析 / 弹窗底）保持纯白 practice-sheet——它是系统里唯一"该干净"的层。
- **Do** 判分反馈走双通道：颜色 +「你的答案 / 正确答案」文字或对勾叉图标。
- **Do** tint 只用五张便签，且彩色小字配墨系深档（inked-*），本体色不上小字。
- **Do** 浮层统一"练习纸实底 + 1px 描边 + 遮罩"；同类元素只用一个材质语言。
- **Do** 行高宁松勿紧（正文 1.75），圆角按大一档标尺；分界用 1px 铅笔线 + 留白。
- **Do** 动效 100–200ms、只表达状态（悬停 / 进出场），并保留全局 `prefers-reduced-motion` 降级。

### Don't:
- **Don't** 做考试压力型视觉——倒计时、排名、红黑榜（与 ADR 0001「练习语义优先」直接冲突）。
- **Don't** 做花哨游戏化——徽章、连击、彩带庆祝、夸张弹跳动效。
- **Don't** 给阅读面上色，或让状态色本体出现在浅底小字上。
- **Don't** 添加第六张便签 / 第六种面，或把便签色当环境色大面积铺。
- **Don't** 把长正文放到玻璃上（对比度不可控）；玻璃背后没有内容可透时也不用玻璃。
- **Don't** 用纯黑阴影或粗边框做大分界；阴影只用冷调三档。
