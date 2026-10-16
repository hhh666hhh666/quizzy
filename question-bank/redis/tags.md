# Redis 题库标签词表

> **受控词表**。`plan.py` 切分时**只从这里挑**标签——挑的是在标题或正文中出现过的，标题里出现的优先。
> 新标签要先加进本表再用，否则它不会进 `tasks.json`、模型也就不会去用它。
>
> 两点说明：
> - **考频档位**（`高频` / `中频` / `低频`）**不在此表**——它由出题环节按原文信号单独标，口径见技能的 `references/authoring.md`。
> - 标签全库同名复用（quizzy 的标签是全局共享的），所以**别为同一概念造两种写法**（如 `repl backlog` 与 `replication backlog buffer` 只能留一个）。
> - 内容标签每题 ≤ 5 个；下表按文档的知识块排列（数据结构 → 线程模型 → 事务 → 持久化 → 淘汰过期 → 高可用 → 性能 → 场景），新增的放末尾即可。
>
> **重跑口径**（2026-10-16 首跑）：`plan.py --docs question-bank/redis --out .qbgen/redis --category redis --split-level 3 --min-chars 60`。
> 切片 id = 「文件名 + 标题」，**换切分粒度 = 换 id**，旧题会被当成新片再入一遍库；要改参数就得连 `json/` 一起重来。

- Redis数据类型
- SDS
- 链表
- 哈希表
- rehash
- 压缩列表
- listpack
- 整数集合
- 跳表
- Zset
- 线程模型
- 多线程
- 网络模型
- IO多路复用
- epoll
- 事务
- 原子性
- Lua
- 持久化
- RDB
- AOF
- 混合持久化
- 写时复制
- fsync
- 过期删除
- 内存淘汰
- LRU
- LFU
- 缓存一致性
- 主从复制
- 全量同步
- 增量同步
- replication buffer
- repl backlog
- 主从不一致
- 脑裂
- 哨兵
- 故障转移
- 分片集群
- 哈希槽
- MOVED
- 缓存雪崩
- 缓存击穿
- 缓存穿透
- 缓存预热
- Pipeline
- keys命令
- 大key
- 分布式锁
- 可重入锁
- 读写锁
- 看门狗
- Redisson
- 限流
- 滑动窗口
- 延迟队列
- 性能优化
