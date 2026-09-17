-- 种子题库：全部为公开题（owner_id 为 NULL），供首次登录即有题可刷。
SET NAMES utf8mb4;

INSERT IGNORE INTO `category` (`name`, `sort`) VALUES
  ('Java 基础', 1), ('Java 并发', 2), ('JVM', 3),
  ('Spring', 4), ('MySQL', 5), ('Redis', 6), ('计算机网络', 7);

INSERT IGNORE INTO `tag` (`name`) VALUES
  ('易错'), ('高频'), ('集合'), ('多线程'), ('内存'), ('事务'), ('索引'), ('缓存');

INSERT INTO `question`
  (`type`, `stem`, `analysis`, `difficulty`, `answer`, `score`, `category_id`, `owner_id`)
VALUES
  ('SINGLE', '下列关于 `String`、`StringBuilder`、`StringBuffer` 的说法，正确的是？', '`String` 不可变；`StringBuilder` 非线程安全但性能更好；`StringBuffer` 的方法用 `synchronized` 修饰，是线程安全的。', 'EASY', 'B', 1, 1, NULL),
  ('SINGLE', '`HashMap` 在 JDK 8 中，当单个桶的链表长度超过多少且数组长度达到多少时会树化？', '链表长度超过 8 且数组容量不小于 64 时转红黑树；若容量不足 64 会优先扩容。', 'MEDIUM', 'C', 2, 1, NULL),
  ('MULTI', '下列哪些操作会导致 `ArrayList` 触发扩容？', '`add` 与 `addAll` 在容量不足时触发扩容，`ensureCapacity` 也可能触发；`remove` 与 `get` 不会扩容。', 'MEDIUM', 'A,C', 2, 1, NULL),
  ('JUDGE', '`==` 比较的是对象引用，`equals` 比较的是对象内容，因此重写 `equals` 时一定也要重写 `hashCode`。', '判断正确。`equals` 与 `hashCode` 需保持一致性，否则对象在哈希集合中会出现“存得进、取不出”的问题。', 'EASY', 'A', 1, 1, NULL),
  ('SINGLE', '接口中的方法默认修饰符是？', '接口方法默认是 `public abstract`，字段默认是 `public static final`。', 'EASY', 'B', 1, 1, NULL),
  ('MULTI', '关于 `finally` 块，下列说法正确的有？', '`finally` 一般在 `return` 前执行；`System.exit` 或线程被中断时不会执行；`finally` 中的 `return` 会覆盖 `try` 的返回值。', 'MEDIUM', 'A,B,D', 3, 1, NULL),

  ('SINGLE', '`volatile` 关键字不能保证下列哪一项？', '`volatile` 保证可见性与禁止指令重排序，但不保证复合操作的原子性（如 `i++`）。', 'MEDIUM', 'C', 2, 2, NULL),
  ('SINGLE', '`synchronized` 修饰静态方法时，锁住的是？', '静态方法锁的是当前类的 `Class` 对象，实例方法锁的是当前实例 `this`。', 'EASY', 'B', 1, 2, NULL),
  ('MULTI', '下列哪些方式可以实现线程间通信？', '`wait/notify` 基于监视器锁；`Condition` 配合 `Lock`；`CountDownLatch` 等同步工具也可以；`Thread.yield` 只是让出 CPU，不用于通信。', 'MEDIUM', 'A,B,C', 2, 2, NULL),
  ('SINGLE', '`ReentrantLock` 相比 `synchronized` 不具备下列哪项能力？', '`ReentrantLock` 支持公平锁、可中断 `lockInterruptibly`、尝试加锁 `tryLock`、多条件变量，但不能自动释放锁，必须手动 `unlock`。', 'MEDIUM', 'D', 2, 2, NULL),
  ('JUDGE', '线程池的 `corePoolSize` 允许设置为 0。', '判断正确。设置为 0 时任务会直接进入队列，队列满了才会创建非核心线程。', 'HARD', 'A', 2, 2, NULL),
  ('SINGLE', '`ThreadLocal` 产生内存泄漏的根本原因是？', '`ThreadLocalMap` 的 key 是弱引用、value 是强引用；key 被回收后 value 仍被线程持有，线程池复用线程时就会泄漏。', 'HARD', 'C', 3, 2, NULL),

  ('SINGLE', 'JVM 中哪块内存区域不会发生 `OutOfMemoryError`？', '程序计数器是唯一规范中没有规定 `OutOfMemoryError` 的区域。', 'MEDIUM', 'A', 2, 3, NULL),
  ('MULTI', '下列哪些对象可以作为 GC Roots？', 'GC Roots 包括栈帧中的本地变量、方法区静态属性与常量、JNI 引用等；被 `SoftReference` 包裹的对象本身不是根。', 'HARD', 'A,B,C', 3, 3, NULL),
  ('SINGLE', 'CMS 收集器使用的垃圾收集算法是？', 'CMS 采用标记-清除算法，因此会产生内存碎片。', 'MEDIUM', 'B', 2, 3, NULL),
  ('JUDGE', '对象在新生代经历 15 次 Minor GC 后一定会晋升到老年代。', '判断错误。除年龄阈值外还有动态年龄判定与大对象直接进老年代等规则，并不是“一定”。', 'HARD', 'B', 2, 3, NULL),

  ('SINGLE', 'Spring Bean 的默认作用域是？', '默认是 `singleton`，即每个容器中同名的 Bean 只有一个实例。', 'EASY', 'A', 1, 4, NULL),
  ('SINGLE', '`@Transactional` 在下列哪种情况下会失效？', '同类内部方法调用不会经过代理，因此事务注解失效。', 'MEDIUM', 'D', 2, 4, NULL),
  ('MULTI', '关于 Spring 循环依赖，下列说法正确的有？', 'Spring 通过三级缓存解决“单例 + setter/字段注入”的循环依赖；构造器注入与 prototype 作用域的循环依赖无法解决。', 'HARD', 'A,B', 3, 4, NULL),
  ('SINGLE', 'Spring MVC 中负责把请求分发到具体处理器方法的组件是？', '`DispatcherServlet` 是前端控制器，通过 `HandlerMapping` 找到处理器。', 'EASY', 'B', 1, 4, NULL),
  ('JUDGE', 'Spring Boot 的自动配置本质上是通过 `@Conditional` 系列注解按条件决定是否生效。', '判断正确。`spring.factories` 中声明的自动配置类由各种 `@ConditionalOnXxx` 控制生效条件。', 'MEDIUM', 'A', 2, 4, NULL),

  ('SINGLE', 'MySQL InnoDB 的默认隔离级别是？', 'InnoDB 默认是可重复读（REPEATABLE READ），并通过 Next-Key Lock 在一定程度上避免幻读。', 'MEDIUM', 'C', 2, 5, NULL),
  ('MULTI', '下列哪些情况会导致索引失效？', '对索引列做函数运算、隐式类型转换、以 `%` 开头的 `LIKE`、违反最左前缀都会让索引失效。', 'HARD', 'A,B,C,D', 3, 5, NULL),
  ('SINGLE', '关于联合索引 `(a, b, c)`，下列查询能完整用到三列的是？', '只有 `a=1 and b=2 and c=3` 满足最左前缀且连续，可以完整使用联合索引。', 'MEDIUM', 'D', 2, 5, NULL),
  ('JUDGE', 'InnoDB 的主键索引叶子节点存储的是整行数据。', '判断正确。InnoDB 的主键索引是聚簇索引，叶子节点保存完整行记录；二级索引叶子节点保存主键值。', 'EASY', 'A', 1, 5, NULL),
  ('SINGLE', '`explain` 结果中 `type` 为哪一项时性能最差？', '性能由好到差大致为 system > const > eq_ref > ref > range > index > ALL，`ALL` 表示全表扫描。', 'EASY', 'D', 1, 5, NULL),

  ('SINGLE', 'Redis 中下列哪种数据结构适合做排行榜？', '`zset` 有序集合支持按 score 排序，适合排行榜与延迟队列。', 'EASY', 'C', 1, 6, NULL),
  ('MULTI', '关于缓存穿透，下列哪些手段可以缓解？', '缓存空值、布隆过滤器、参数校验都能缓解穿透；缓存预热主要解决的是冷启动与雪崩问题。', 'MEDIUM', 'A,B,D', 2, 6, NULL),
  ('SINGLE', 'Redis 的 `SETNX` 常被用于实现？', '`SETNX`（set if not exist）配合过期时间可实现简易分布式锁。', 'MEDIUM', 'B', 2, 6, NULL),
  ('JUDGE', 'Redis 是单线程模型，因此所有命令都不会阻塞。', '判断错误。像 `KEYS`、大 key 删除、`FLUSHALL` 这类命令依然会长时间阻塞主线程。', 'MEDIUM', 'B', 2, 6, NULL),

  ('SINGLE', 'TCP 三次握手的第二步发送的报文标志位是？', '第二次握手由服务端回 SYN+ACK。', 'EASY', 'B', 1, 7, NULL),
  ('MULTI', '下列哪些属于 HTTP 状态码 4xx？', '400、401、404 属于客户端错误；500 与 502 属于服务端错误。', 'EASY', 'A,B,D', 1, 7, NULL),
  ('SINGLE', 'HTTPS 中用于协商对称密钥的阶段发生在？', 'TLS 握手阶段协商出会话密钥，后续应用数据用对称加密传输。', 'MEDIUM', 'C', 2, 7, NULL),
  ('JUDGE', 'TCP 的 TIME_WAIT 状态出现在主动关闭连接的一方。', '判断正确。主动关闭方发出最后一个 ACK 后进入 TIME_WAIT，等待 2MSL。', 'MEDIUM', 'A', 2, 7, NULL);

INSERT INTO `question_option` (`question_id`, `label`, `content`, `sort`) VALUES
  (1, 'A', 'String 是线程安全的可变字符串', 1), (1, 'B', 'String 不可变，StringBuilder 非线程安全，StringBuffer 线程安全', 2),
  (1, 'C', 'StringBuilder 用 synchronized 保证线程安全', 3), (1, 'D', '三者都不可变', 4),
  (2, 'A', '链表长度 8，数组 16', 1), (2, 'B', '链表长度 6，数组 64', 2),
  (2, 'C', '链表长度 8，数组 64', 3), (2, 'D', '链表长度 64，数组 8', 4),
  (3, 'A', 'add', 1), (3, 'B', 'remove', 2), (3, 'C', 'addAll', 3), (3, 'D', 'get', 4),
  (4, 'A', '正确', 1), (4, 'B', '错误', 2),
  (5, 'A', 'public static abstract', 1), (5, 'B', 'public abstract', 2),
  (5, 'C', 'protected abstract', 3), (5, 'D', '默认无修饰符', 4),
  (6, 'A', 'finally 中的代码通常在 return 之前执行', 1), (6, 'B', '调用 System.exit 后 finally 不会执行', 2),
  (6, 'C', 'finally 块可以与 try 块单独搭配，不需要 catch', 3), (6, 'D', 'finally 中的 return 会覆盖 try 中的返回值', 4),
  (7, 'A', '可见性', 1), (7, 'B', '禁止指令重排序', 2), (7, 'C', '原子性', 3), (7, 'D', '以上都不保证', 4),
  (8, 'A', '当前实例对象 this', 1), (8, 'B', '当前类的 Class 对象', 2),
  (8, 'C', '调用该方法的对象', 3), (8, 'D', '任意对象', 4),
  (9, 'A', 'wait / notify', 1), (9, 'B', 'Condition 的 await / signal', 2),
  (9, 'C', 'CountDownLatch', 3), (9, 'D', 'Thread.yield', 4),
  (10, 'A', '可实现公平锁', 1), (10, 'B', '支持可中断地获取锁', 2),
  (10, 'C', '支持多个条件变量', 3), (10, 'D', '可以自动释放锁', 4),
  (11, 'A', '正确', 1), (11, 'B', '错误', 2),
  (12, 'A', '线程池不会被回收', 1), (12, 'B', 'ThreadLocal 本身是强引用', 2),
  (12, 'C', 'ThreadLocalMap 的 key 是弱引用而 value 是强引用', 3), (12, 'D', 'Entry 继承了 WeakReference', 4),
  (13, 'A', '程序计数器', 1), (13, 'B', 'Java 虚拟机栈', 2), (13, 'C', '方法区', 3), (13, 'D', '堆', 4),
  (14, 'A', '栈帧中的本地变量引用的对象', 1), (14, 'B', '方法区中静态属性引用的对象', 2),
  (14, 'C', '本地方法栈中 JNI 引用的对象', 3), (14, 'D', '被软引用关联的对象', 4),
  (15, 'A', '标记-整理', 1), (15, 'B', '标记-清除', 2), (15, 'C', '复制算法', 3), (15, 'D', '分代收集', 4),
  (16, 'A', '正确', 1), (16, 'B', '错误', 2),
  (17, 'A', 'singleton', 1), (17, 'B', 'prototype', 2), (17, 'C', 'request', 3), (17, 'D', 'session', 4),
  (18, 'A', '标注在 public 方法上', 1), (18, 'B', '数据库引擎为 InnoDB', 2),
  (18, 'C', '事务方法被另一个 Bean 调用', 3), (18, 'D', '同类内部方法之间调用', 4),
  (19, 'A', '单例 Bean 的 setter 注入循环依赖可以被解决', 1), (19, 'B', '构造器注入的循环依赖无法被解决', 2),
  (19, 'C', 'prototype 作用域的循环依赖可以被解决', 3), (19, 'D', '任何循环依赖 Spring 都能解决', 4),
  (20, 'A', 'HandlerAdapter', 1), (20, 'B', 'DispatcherServlet', 2),
  (20, 'C', 'ViewResolver', 3), (20, 'D', 'HandlerInterceptor', 4),
  (21, 'A', '正确', 1), (21, 'B', '错误', 2),
  (22, 'A', '读未提交', 1), (22, 'B', '读已提交', 2), (22, 'C', '可重复读', 3), (22, 'D', '串行化', 4),
  (23, 'A', '对索引列使用函数', 1), (23, 'B', '查询条件发生隐式类型转换', 2),
  (23, 'C', 'LIKE 以 % 开头', 3), (23, 'D', '不满足联合索引最左前缀', 4),
  (24, 'A', 'where b=2 and c=3', 1), (24, 'B', 'where a=1 and c=3', 2),
  (24, 'C', 'where b=2 and a=1', 3), (24, 'D', 'where a=1 and b=2 and c=3', 4),
  (25, 'A', '正确', 1), (25, 'B', '错误', 2),
  (26, 'A', 'system', 1), (26, 'B', 'ref', 2), (26, 'C', 'range', 3), (26, 'D', 'ALL', 4),
  (27, 'A', 'list', 1), (27, 'B', 'hash', 2), (27, 'C', 'zset', 3), (27, 'D', 'set', 4),
  (28, 'A', '缓存空值', 1), (28, 'B', '布隆过滤器', 2), (28, 'C', '提高缓存过期时间', 3), (28, 'D', '请求参数校验', 4),
  (29, 'A', '消息队列', 1), (29, 'B', '分布式锁', 2), (29, 'C', '限流', 3), (29, 'D', '排行榜', 4),
  (30, 'A', '正确', 1), (30, 'B', '错误', 2),
  (31, 'A', 'SYN', 1), (31, 'B', 'SYN + ACK', 2), (31, 'C', 'ACK', 3), (31, 'D', 'FIN', 4),
  (32, 'A', '400', 1), (32, 'B', '401', 2), (32, 'C', '500', 3), (32, 'D', '404', 4),
  (33, 'A', 'DNS 解析阶段', 1), (33, 'B', 'TCP 三次握手阶段', 2), (33, 'C', 'TLS 握手阶段', 3), (33, 'D', '应用数据传输阶段', 4),
  (34, 'A', '正确', 1), (34, 'B', '错误', 2);

INSERT IGNORE INTO `question_tag` (`question_id`, `tag_id`) VALUES
  (1, 1), (2, 2), (2, 3), (3, 3), (4, 1), (6, 1),
  (7, 4), (7, 2), (8, 4), (9, 4), (10, 4), (12, 4), (12, 1),
  (13, 5), (14, 5), (15, 5), (16, 5), (16, 1),
  (18, 6), (18, 2), (19, 2), (21, 2),
  (23, 7), (24, 7), (25, 7), (26, 7), (22, 6),
  (27, 8), (28, 8), (29, 8), (30, 8), (30, 1);
