<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753378538969-50790e17-ccd6-4c97-b6b0-110c67ac8348.png)

# -----------------数据结构-----------------
# Redis常见的数据类型
| **<font style="color:rgb(0, 0, 0);">数据类型</font>** | **<font style="color:rgb(0, 0, 0);">适用场景</font>** | **<font style="color:rgb(0, 0, 0);">底层结构</font>** |
| --- | --- | --- |
| <font style="color:rgb(0, 0, 0);">String</font> | <font style="color:rgb(0, 0, 0);">缓存、计数器</font> | <font style="color:rgb(0, 0, 0);">SDS(简单动态字符串)</font> |
| <font style="color:rgb(0, 0, 0);">List</font> | <font style="color:rgb(0, 0, 0);">队列、栈</font> | <font style="color:rgb(0, 0, 0);">QuickList（ziplist双向链表）</font> |
| <font style="color:rgb(0, 0, 0);">Hash</font> | <font style="color:rgb(0, 0, 0);">对象存储</font> | <font style="color:rgb(0, 0, 0);">ziplist（小数据）或哈希表</font> |
| <font style="color:rgb(0, 0, 0);">Set</font> | <font style="color:rgb(0, 0, 0);">唯一元素集合（标签）</font> | <font style="color:rgb(0, 0, 0);">IntSet（整数集合）或哈希表</font> |
| <font style="color:rgb(0, 0, 0);">ZSet</font> | <font style="color:rgb(0, 0, 0);">排行榜、带权重队列</font> | <font style="color:rgb(0, 0, 0);">跳表（范围查询）+哈希表（快速查找分数）</font> |


+ String：存储文本、数字或二进制数据。
+ List：一个有序的字符串集合，底层是双向链表，支持从两端插入和删除元素。
+ Set：无序且元素唯一的字符串集合，底层是哈希表, 支持交并差等集合运算。
+ Hash：一种键值对集合, 类似java中的map, 底层是哈希表。
+ Sorted Set(Zset)：按分数排序且元素唯一的集合, 在底层实现结合了跳表和哈希表, 支持按分数范围获取元素。
+ Bitmap(位图): 是一串连续的二进制数组（0和1）, 可以用来进行签到统计, 一年的签到也只需要365个bit位, 可以节省很多空间
+ HyperLogLog: 是一种用于「统计基数」的集合数据类型，基数统计就是指统计一个集合中不重复的元素个数。HyperLogLog 是统计规则是基于概率完成的，不是非常准确，标准误算率是 0.81%, 但是耗费的内存空间非常小，适合统计 UV（独立访客）  
+ GEO: 用于存储地理位置信息，并对存储的信息进行操作。本质使用了Sorted Set
+ Stream: Redis 专门为消息队列设计的数据类型。

# Redis数据结构有那些？
1. 简单动态字符串（SDS, Simple Dynamic Strings)
2. 双向链表
3. 字典（Dict）
4. 压缩列表（Ziplist）
5. 整数集合（IntSet）
6. 跳表（Skiplist）

## 简单动态字符串（SDS, Simple Dynamic Strings)
Redis的字符串类型不是使用传统的C语言字符串，而是基于SDS实现的，这就是Redis数据结构的第一个特色。SDS是一个可以保存文本或二进制数据的抽象类型，它**记录了字符串的长度**，而且还**避免了C字符串的缓冲区溢出问题**，并且可以**更高效地进行长度计算和字符串追加操作**。

SDS是Redis处理字符串数据的基础，也是用于实现Redis的String类型键值对。

SDS 在原本字符数组之上，增加了三个元数据：len、alloc、flags, 主要是为了解决 C 语言字符串的缺陷, len用来记录字符串的长度, 这样获取字符串长度的时间复杂度就只有O(1), alloc就是分配给字符数组的空间长度, 在修改字符串时可以判断新的len是否大于alloc, 来决定是否进行扩容, flags用来表明结构体的类型, 表明字符串的大小类型(长度上限)

C 语言字符串的缺陷: 

+ C 语言的字符串长度获取 strlen 函数，需要通过遍历的方式来统计字符串长度，时间复杂度是 O（N）。
+ 字符串不能存'\0'
+ 字符串进行追加时容易造成缓冲区溢出

> C 语言的字符串标准库提供的字符串操作函数，大多数（比如 strcat 追加字符串函数）都是不安全的，因为这些函数把缓冲区大小是否满足操作需求的工作交由开发者来保证，程序内部并不会判断缓冲区大小是否足够用，当发生了缓冲区溢出就有可能造成程序异常结束。
>

## 双向链表	
双端链表是一个由节点组成的序列，每个节点都有指向前一个节点和后一个节点的指针，可以从两端快速添加或删除节点。

它用来实现Redis的List类型，支持在列表的两端快速插入或删除元素。

## 字典（Dict）
Redis中的字典类似于哈希表或Java中的HashMap，它是一个由键值对组成的数据结构。Redis的字典使用散列（Hashing）来快速定位键。

字典用于实现Redis的Hash类型和内部的键空间，键空间指的就是所有的键都存储在一个字典中。

### 哈希表是怎么扩容的？
redis使用了两个哈希表, 比如有哈希表1, 哈希表2, 一开始的数据都在哈希表1上, 哈希表2并没有被分配空间, 在负载因子满足一定条件时就会触发rehash, 给哈希表2分配哈希表1两倍的空间, 然后逐步将哈希表1的数据迁移到哈希表2, 此时如果插入新的数据就会插入到哈希表2,  查找一个 key 的值的话，先会在「哈希表 1」 里面进行查找，如果没找到，就会继续到「哈希表 2」 里面进行查找, 最后迁移完成后哈希表1的空间就会被释放, 哈希表2就变成哈希表1, 然后再创建一个空白的哈希表2, 为下一次rehash做准备

触发 rehash 操作的条件，主要有两个：

+ 当负载因子大于等于 1 ，并且 Redis 没有在执行 bgsave 命令或者 bgrewiteaof 命令，也就是没有执行 RDB 快照或没有进行 AOF 重写的时候，就会进行 rehash 操作。
+ 当负载因子大于等于 5 时，此时说明哈希冲突非常严重了，不管有没有有在执行 RDB 快照或 AOF 重写，都会强制进行 rehash 操作。

## 压缩列表（Ziplist）
压缩列表是Redis为了节省空间而设计的紧凑数据结构，它把多个元素序列化存储在单个字符串中。元素可以是小整数或短字符串。

如果List、Hash或Sorted Set类型的数据项较小并且数量较少时，Redis就会选择使用压缩列表来存储，目的就是优化内存使用。

ZipList是一个压缩的数据结构，它的每个元素都是连续存储的，因此内存的使用非常紧凑。**与其他数据结构相比，ZipList在小规模数据存储时显著减少了内存占用。**

但是ZipList也有缺点，正因为他是紧凑的线性结构，所以如果在 ZipList 中查找一个元素时，可能需要遍历整个列表，同理插入和删除操作也是线性的。**所以ZipList的插入、删除和查找操作的时间复杂度通常是 O(N)。相对来说是比较慢的。**

## listpack
**listpack** 是 Redis 在 5.0 版本为替代 ziplist(压缩列表) 设计的数据结构，解决了Ziplist极端情况下连锁更新的问题, 主要用于小型的 list、hash 和 zset 的底层存储。它是一块连续内存区域，内部存储多个元素，结构简单、压缩率高，支持正向和反向遍历。  

Ziplist中有一个字段用于记录前一个entry的大小，如果前一个entry太大，这个字段占用的大小也会变大，进而导致当前的entry变大，导致后面所有的entry都跟着变，而listpack采用的记录当前entry的大小而不是前一个entry的大小，那么，在ListPack中新增或者删除元素，只会影响到这个元素自己，而不需要其他的元素进行级联更新了。也就解决了级联更新的问题了。

> 压缩列表用连续内存存储，每个节点记录前驱长度，插入或删除中间节点时，后续节点偏移和前驱长度字段都可能变化，导致必须连续更新多个节点，产生连锁更新问题。listpack 没有压缩列表中记录前一个节点长度的字段了，listpack 只记录当前节点的长度，当我们向 listpack 加入一个新元素的时候，不会影响其他节点的长度字段的变化，从而避免了压缩列表的连锁更新问题。
>

## 整数集合（IntSet）
整数集合是一种简单的数组结构，仅用来存储整数值，并且可以根据存储的整数类型（如16位、32位或64位）动态调整，这样就可以节省空间。

整数集合用于存储Set类型的键，当集合中只包含整数值并且元素数量较少时。

## 跳表（Skiplist）
跳跃表是Redis自主设计的可以快速访问的有序数据结构，每个节点都有多个指向其他节点的指针，这些指针构成了多个层次，所以跳跃表可以在O(log n)时间复杂度内完成查找、插入和删除操作。

跳跃表可以用来实现Redis的Sorted Set类型，也就是有序集合，它可以快速按范围查询和按分数排序。

### Zset 底层是怎么实现的？
如果有序集合的元素个数小于 128 个，并且每个元素的值小于 64 字节时，Redis 会使用压缩列表作为 Zset 类型的底层数据结构；否则使用跳表作为底层数据结构

**<font style="color:rgb(48, 79, 254);">在 Redis 7.0 中，压缩列表数据结构已经废弃了，交由 listpack 数据结构来实现了</font>**

### 跳表是怎么实现的？
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1751724641321-cedb2927-ee11-4a96-93be-df0c7b1cdb15.png)

跳表由多层链表构成, 跳表的每个节点存储了元素值, 分数值, 后向指针和一个前向指针level数组, level数组里存了前向指针和跨度, 在跳表中查询元素的时候可以跳过中间的部分元素, 从而达到快速定位的效果

### 跳表是怎么设置层高的？
跳表在创建节点时候，会生成范围为[0-1]的一个随机数，如果这个随机数小于 0.25（相当于概率 25%），那么层数就增加 1 层，然后继续生成下一个随机数，直到随机数的结果大于 0.25 结束，最终确定该节点的层数。这样就可以做点节点的层级每增加一层, 跨度就会更高

### Redis为什么使用跳表而不是用B+树?
+ 跳表所需要去维护的内存大小比树节点更加小, 它不需要去维护那么多的节点信息
+ 跳表能够实现跟树结构一样的范围检索, 排序这样的功能
+ 跳表的代码更加简单
+ b+树优势是层级低可以减少磁盘io, 而redis数据是存在内存中的, 不需要这个优点

# ----------------线程模型------------------
# Redis为什么快？
+ Redis 的**数据存储在内存中**, 数据在内存中的读写速度是远大于在磁盘中的, 而且**采用了高效的数据结构**
+ Redis 采用**单线程模型**，这样可以避免多线程之间的竞争和上下文切换带来的性能损耗。
+  Redis 通过采用** I/O 多路复用机制**来高效处理大量客户端的 Socket 请求。所谓 I/O 多路复用，是指使用一个线程同时监听多个 I/O 流，常见的实现包括 `select` `poll`、和`epoll`。在 Redis 的单线程模型下，该机制使得内核可以同时监听多个监听 Socket 和已连接 Socket，一旦某个 Socket 上有连接请求或数据到达，内核就会通知 Redis 线程进行处理。这样，Redis 就能在单线程的前提下，高效地应对大量并发的客户端请求。  
+ Redis处理Socket请求时，在单线程情况下，只能依次处理每一个socket，如果正在处理的socket恰好未就绪（数据不可读或不可写），线程就会被阻塞，所有其它客户端socket都必须等待，性能就会很差，为了解决这个问题，Redis就使用到了IO多路复用，在单线程的情况下去监听多个socket，在某个socket可读可写的时候收到通知，从而避免无效的等待，它的底层就是使用了操作系统的IO多路复用机制，操作系统是提供了三种IO多路复用机制，分别是select、poll、epoll，**select**的机制是用户空间会将需要监听的fd保存到一个数组，因为是数组，能监听的fd是有限的，一般个1024个，这个数组会拷贝给内核空间，一旦有就绪的事件，这个数组又会拷贝回用户空间，并且他不知道里面具体哪个就绪了还要去全部轮询一遍，效率很低，**poll**的话只是将数组改成了链表仅仅解决了监听的FD集合数量有限的问题，epoll是使用红黑树去存你想要监听的fd，使用链表来存就绪的fd，这样就可以只将就绪的fd复制回去，用户空间就可以只去监听就绪好的fd，而不是去将所有fd又轮询一遍。使用IO多路复用不用多线程是因为线程上下文切换带来的性能损耗

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1760807316271-8a9c416f-a79e-4210-98fa-8fdd8f2f15ea.png)

> + 这里“多路”指的是多个网络连接客户端，“复用”指的是复用同一个线程(单进程)。
> + “Socket（套接字）” 是一种 网络通信的编程接口（API），用于在网络中不同主机之间进行通信。
>

# Redis哪些地方使用了多线程?
Redis 6.0 版本之后，Redis 在启动的时候，默认情况下会**<font style="color:rgb(48, 79, 254);">额外创建 6 个线程</font>**<font style="color:rgb(44, 62, 80);">（</font>_<font style="color:rgb(200, 73, 255);">这里的线程数不包括主线程</font>_<font style="color:rgb(44, 62, 80);">）：</font>

+ Redis-server ： Redis的主线程，主要负责执行命令；
+ bio_close_file、bio_aof_fsync、bio_lazy_free：三个后台线程，分别异步处理**关闭文件任务**、**AOF刷盘任务**、**释放内存任务**；
+ io_thd_1、io_thd_2、io_thd_3：三个 I/O 线程，io-threads 默认是 4 ，所以会启动 3（4-1）个 I/O 多线程，用来分担 Redis 网络 I/O 的压力。

Redis 6.0 版本支持的 I/O 多线程特性，默认情况下 I/O 多线程只针对发送响应数据（write client socket），并不会以多线程的方式处理读请求（read client socket）。要想开启多线程处理客户端读请求，就需要把 Redis.conf 配置文件中的 io-threads-do-reads 配置项设为 yes。

```plain
//读请求也使用io多线程
io-threads-do-reads yes
```

```plain
// io-threads N，表示启用 N-1 个 I/O 多线程（主线程也算一个 I/O 线程）
io-threads 4
```

关于线程数的设置，官方的建议是如果为 4 核的 CPU，建议线程数设置为 2 或 3，如果为 8 核 CPU 建议线程数设置为 6，线程数一定要小于机器核数，线程数并不是越大越好。

# Redis的网络模型是怎样的？
到 Redis 6.0 之后，就将网络IO的处理改成多线程的方式了，这是因为随着网络硬件的性能提升，Redis 的性能瓶颈有时会出现在网络 I/O 的处理上。

所以为了提高网络 I/O 的并行度，Redis 6.0 对于网络 I/O 采用多线程来处理。**<font style="color:rgba(48,79,254,1);">但是对于命令的执行，Redis 仍然使用单线程来处理</font>**，所以大家不要误解 Redis 有多线程同时执行命令。

# -------------------事务--------------------
# 如何实现redis 原子性？
redis 执行一条命令的时候是具备原子性的，因为 redis 执行命令的时候是单线程来处理的，不存在多线程安全的问题。

如果要保证多条命令的原子性的话，可以考虑用 lua 脚本，将多个操作写到一个 Lua 脚本中，Redis 会把整个 Lua 脚本作为一个整体执行，在执行的过程中不会被其他命令打断，从而保证了 Lua 脚本中操作的原子性。lua脚本可以进行简单的逻辑判断, 这是redis事务所做不到的

ACID 的原子性和 Redis 执行 Lua 脚本原子性在概念上的差异： 

+ ACID的原子性是指：命令要么全执行，要么全部不执行；
+ Redis中执行 Lua脚本原子性是指：Lua脚本需要作为一个整体执行且不被其他事务打断，至于 Lua脚本里面的命令是否必须全部成功，或者全部失败，并不要求。

# 除了lua有没有什么也能保证redis的原子性？
Redis中是支持事务的，他的事务主要目的是保证多个命令执行的原子性，即要在一个原子操作中执行，不会被打断。事务执行期间如果有其他命令过来就会阻塞住。

**服务器将会在累积命令的过程中检测到错误。然后，在执行 EXEC 期间会拒绝执行事务，并返回一个错误，同时丢弃该事务。如果事务执行过程中发生错误，Redis会继续执行剩余的命令而不是回滚整个事务。**

Redis错误有两种情况，一种是在命令排队的过程就就检测到的错误，比如语法错误，比如内存不够了，等等。在这种错误，会在调用 EXEC 后，不会执行任何命令。

还有一种错误，是在调用 EXEC 后，命令执行过程中出现的错误，最常见的就是操作类型不一致，比如对字符串进行列表相关的操作。这种就是在执行过程中才会出现的。此时出错并不会影响后续命令的执行

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1751969947959-f2c9f133-1f62-433a-b742-b25dac502652.png)

第一条执行失败了但是第二条还是执行成功了

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753615317412-ef3f5eb2-1f07-47a3-b55f-80f3230faa0e.png)

# -------------------日志--------------------
# Redis有哪2种持久化方式？分别的优缺点是什么？
redis的可以通过RDB快照和AOF日志进行持久化, 

RDB是将某一时刻的内存数据全部写入磁盘中, 如果数据量大的话这会是一个非常耗时的操作, 所以一般频率会设置得比较低, 同时为了不阻塞主进程, redis在自动生成RDB文件时使用的是bgsafe, fork主进程得到一个子进程来生成RDB文件, 子进程和主进程就有了相同的页表, 可以操作相同的物理内存, 为什么执行快照的时候数据还能被修改呢, 这是因为fork采用的是copy-on-write技术, 当主进程执行写操作时，则会拷贝一份数据，执行写操作。防止子进程读数据时读着读着数据就被篡改了, 这也是有一个缺点的, 极端情况下内存的占用可能会翻倍, 如果内存中有大key的话就会十分影响性能, 

AOF 日志就是每执行一条写操作命令，就把该命令以追加的方式写入到一个文件里；当满足一定条件时为了避免aof过大和通过aof恢复数据过于缓慢会进行aof重写,  也就是执行bgrewriaof, 此时会fork主进程得到一个子进程来进行重写, 在重写 AOF 期间，当 Redis 执行完一个写命令之后，它会同时将这个写命令写入到 「AOF 缓冲区」和 「AOF 重写缓冲区」, 在子进程完成aof重写后, 主进程还会将AOF重写缓冲区中的所有内容追加到新的AOF文件中。

> 在通过 fork() 函数创建子进程的时候，虽然不会复制父进程的物理内存，但是内核会把父进程的页表复制一份给子进程，如果页表很大，那么这个复制过程是会很耗时的，那么在执行 fork 函数的时候就会发生阻塞现象。我们可以执行 info 命令获取到 latest_fork_usec 指标，表示 Redis 最近一次 fork 操作耗时。
>
> 如果 fork 耗时很大，比如超过1秒，则需要做出优化调整：
>
> + 单个实例的内存占用控制在 10 GB 以下，这样 fork 函数就能很快返回。
> + 如果 Redis 只是当作纯缓存使用，不关心 Redis 数据安全性问题，可以考虑关闭 AOF 和 AOF 重写，这样就不会调用 fork 函数了。
> + 在主从架构中，要适当调大 repl-backlog-size，避免因为 repl_backlog_buffer 不够大，导致主节点频繁地使用全量同步的方式，全量同步的时候，是会创建 RDB 文件的，也就是会调用 fork 函数。
>

AOF:

+ **优点:**
    - 首先，AOF提供了更好的数据安全性，因为它默认每接收到一个写命令就会追加到文件末尾。即使Redis服务器宕机，也只会丢失最后一次写入前的数据。
    - 其次，AOF支持多种同步策略（如everysec、always等），可以根据需要调整数据安全性和性能之间的平衡。
    - 同时，AOF文件在Redis启动时可以通过重写机制优化，减少文件体积，加快恢复速度。
    - 并且，即使文件发生损坏，AOF还提供了redis - check - aof工具来修复损坏的文件。
+ **缺点:**
    - 因为记录了每一个写操作，所以AOF文件通常比RDB文件更大，消耗更多的磁盘空间。
    - 并且，频繁的磁盘IO操作（尤其是同步策略设置为always时）可能会对Redis的写入性能造成一定影响。
    - 而且，当文件体积过大时，AOF会进行重写操作，如果没有开启AOF重写或者重写频率较低，恢复过程可能较慢，因为它需要重放所有的操作命令。

RDB:

+ **优点:**
    - RDB通过快照的形式保存某一时刻的数据状态，文件体积小，备份和恢复的速度非常快。
    - 并且，RDB是在主线程之外通过fork子进程来进行的，不会阻塞服务器处理命令请求，对Redis服务的性能影响较小。
    - 最后，由于是定期快照，RDB文件通常比AOF文件小得多。
+ **缺点:**
    - RDB方式在两次快照之间，如果Redis服务器发生故障，这段时间的数据将会丢失。
    - 并且，如果在RDB创建快照到恢复期间有写操作，恢复后的数据可能与故障前的数据不完全一致。

> Redis 提供了 3 种写回硬盘的策略， 在 Redis.conf 配置文件中的 appendfsync 配置项可以有以下 3 种参数可填：
>
> + Always，这个单词的意思是「总是」，所以它的意思是每次写操作命令执行完后，同步将 AOF 日志数据写回硬盘；
> + Everysec，这个单词的意思是「每秒」，所以它的意思是每次写操作命令执行完后，先将命令写入到 AOF 文件的内核缓冲区，然后每隔一秒将缓冲区里的内容写回到硬盘；
> + No，意味着不由 Redis 控制写回硬盘的时机，转交给操作系统控制写回的时机，也就是每次写操作命令执行完后，先将命令写入到 AOF 文件的内核缓冲区，再由操作系统决定何时将缓冲区内容写回硬盘。
>

在redis4.0以后有混合持久化的功能, 当开启了混合持久化后，在 AOF 重写日志时，fork 出来的重写子进程会先将与主线程共享的内存数据以 RDB 方式写入到 AOF 文件，然后主线程处理的操作命令会被记录在重写缓冲区里，重写缓冲区里的增量命令会以 AOF 方式写入到 AOF 文件，写入完成后通知主进程将新的含有 RDB 格式和 AOF 格式的 AOF 文件替换旧的的 AOF 文件。也就是说，使用了混合持久化，AOF 文件的前半部分是 RDB 格式的全量数据，后半部分是 AOF 格式的增量数据。

这样的好处在于，重启 Redis 加载数据的时候，由于前半部分是 RDB 内容，这样加载的时候速度会很快。加载完 RDB 的内容后，才会加载后半部分的 AOF 内容，这里的内容是 Redis 后台子进程重写 AOF 期间，主线程处理的操作命令，可以使得数据更少的丢失。

# -----------缓存淘汰和过期删除-----------
# 过期删除策略和内存淘汰策略有什么区别？
区别：

+ 内存淘汰策略是在内存满了的时候，redis 会触发内存淘汰策略，来淘汰一些不必要的内存资源，以腾出空间，来保存新的内容
+ 过期键删除策略是将已过期的键值对进行删除，Redis 采用的删除策略是惰性删除+定期删除。

# 介绍一下 Redis 内存淘汰策略
Redis 的内存淘汰策略用于在内存满了(达到了maxmemory)之后，决定哪些 key 要被删除。Redis 支持多种内存淘汰策略，可以通过配置文件中的 maxmemory-policy 参数来指定。

以下是 Redis 支持的内存淘汰策略：

_<font style="color:rgb(200, 73, 255);">1、不进行数据淘汰的策略：</font>_

+ noeviction(Redis3.0之后，默认的内存淘汰策略): 不会淘汰任何键值对，内存满后写入就直接返回错误信息。

_<font style="color:rgb(200, 73, 255);">2、进行数据淘汰的策略：</font>_

+ **<font style="color:rgb(44, 62, 80);">在设置了过期时间的数据中进行淘汰：</font>**
+ volatile-random: 随机淘汰设置了过期时间的任意键值。 
+ volatile-ttl: 优先淘汰更早过期的键值。 
+ volatile-lru(Least Recently Used): 从设置了过期时间的键值中选择最近最少(最久未使用)使用的那个 key 并删除。  
+ volatile-lfu(Least Frequently Used): 从设置了过期时间的键值中选择访问频率最低的那个键值并删除。
+ **在所有数据范围内进行淘汰：**
+ allkeys-lfu: 删除所有键值中，访问频率最低的那个。
+ allkeys-lru: 从所有键值中选择最近最少使用(最久未使用)的那个键值并删除。 
+ allkeys-random: 从所有键值中随机选择一个键值并删除。

以下是腾讯针对Redis的淘汰策略设置给出的建议：

+ 当 Redis 作为缓存使用的时候，推荐使用 allkeys-lru 淘汰策略。该策略会将最近最少使用的 Key 淘汰。默认情况下，使用频率最低则后期命中的概率也最低，所以将其淘汰。
+ 当 Redis 作为半缓存半持久化使用时，可以使用 volatile-lru。但因为 Redis 本身不建议保存持久化数据，所以只作为备选方案。

阿里云Redis默认是volatile-lru （[https://www.alibabacloud.com/help/zh/redis/user-guide/how-does-apsaradb-for-redis-evict-data-by-default](https://www.alibabacloud.com/help/zh/redis/user-guide/how-does-apsaradb-for-redis-evict-data-by-default) ）

腾讯云默认是noeviction，即不删除键。在内存占满后会出现 OOM 问题，所以建议创建好实例后修改淘汰策略，减少 OOM 问题的出现。（[https://cloud.tencent.com/document/product/239/90960](https://cloud.tencent.com/document/product/239/90960) ）

# 介绍一下 Redis 过期删除策略
Redis通过设置过期时间来控制键值对的生命周期。过期时间可以通过EXPIRE(指定过期时间)、EXPIREAT(指定到期的时间戳)、PERSIST(移除过期时间)等命令设置，也可以在插入数据时直接设置过期时间。

**Redis 的过期策略采用的是定期删除和惰性删除相结合的方式。**

+ **定期删除**：Redis 默认每隔 100ms 就随机抽取一些设置了过期时间的 key，并检查其是否过期，如果过期才删除。定期删除是 Redis 的主动删除策略，它可以确保过期的 key 能够及时被删除，但是会占用 CPU 资源去扫描 key，可能会影响 Redis 的性能。
+ **惰性删除**：当一个 key 过期时，不会立即从内存中删除，而是在访问这个 key 的时候才会触发删除操作。惰性删除是 Redis 的被动删除策略，它可以节省 CPU 资源，但是会导致过期的 key 始终保存在内存中，占用内存空间。

**Redis默认同时开启定期删除和惰性删除两种过期策略。**

# ------------架构设计与高可用------------
# 如何保证 redis 和 mysql 数据缓存一致性问题？
使用旁路缓存策略, 该策略又可以细分为「读策略」和「写策略」。

读策略:

+ 如果读取的数据命中了缓存，则直接返回数据；
+ 如果读取的数据没有命中缓存，则从数据库中读取数据，然后将数据写入到缓存，并且返回给用户。

写策略:

+ 先更新数据库, 再删除缓存
+ 延迟双删: 先删除缓存, 再更新数据库, 再延迟1~2s删除一次缓存
+ 为了确保能够成功删除缓存确保高一致性可以通过消息队列来删除缓存，通过消息队列的重试机制来确保缓存删除成功，但是这样对代码侵入性比较强，因为需要改造原本业务的代码，可以通过canal监听binlog的方式来删除缓存

> 例如点赞这种读写并发都很高的情况下，频繁删除缓存可能会导致缓存命中率很低，频繁重建缓存也会给数据库带来压力，此时可以通过canal监听binlog来异步更新缓存，保证了缓存更新的顺序就不会出现缓存不一致的问题
>

# Redis集群
Redis有三种主要的集群模式，用于在分布式环境中实现高可用性和数据复制。这些集群模式分别是：主从复制（Master-Slave Replication）、哨兵模式（Sentinel）和Redis Cluster模式。

## 主从复制模式
主从复制是Redis最简单的集群模式。这个模式主要是为了解决单点故障的问题，所以将数据复制多个副本中，这样即使有一台服务器出现故障，其他服务器依然可以继续提供服务。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753349687804-1195f961-2c4a-4cbb-a579-84c3b157500b.png)

主从模式中，包括一个主节点（Master）和一个或多个从节点（Slave）。**主节点负责处理所有写操作和读操作，而从节点则复制主节点的数据，并且只能处理读操作。**当主节点发生故障时，可以将一个从节点升级为主节点，实现故障转移（需要手动实现）。

**主从复制的优势在于简单易用，适用于读多写少的场景。**它提供了数据备份功能，并且可以有很好的扩展性，只要增加更多的从节点，就能让整个集群的读的能力不断提升。

但是主从模式最大的缺点，就是不具备故障自动转移的能力，没有办法做容错和恢复。

主节点和从节点的宕机都会导致客户端部分读写请求失败，需要人工介入让节点恢复或者手动切换一台从节点服务器变成主节点服务器才可以。并且在主节点宕机时，如果数据没有及时复制到从节点，也会导致数据不一致。

### 主从复制原理
主从复制共有三种模式：**全量复制**、**基于长连接的命令传播**、**增量复制**。

1. **第一次同步**：主从第一次建立连接时，会执行**全量同步**，生成RDB文件将master节点的所有数据都发送给slave节点

那么为了保证主从服务器的数据一致性，主服务器在下面这三个时间间隙中将收到的写操作命令，写入到 replication buffer 缓冲区里：

    - 主服务器生成 RDB 文件期间；
    - 主服务器发送 RDB 文件给从服务器期间；
    - 「从服务器」加载 RDB 文件期间；

主节点会将复制缓冲区里所记录的写操作命令发送给从节点，从节点执行来自主节点 replication buffer 缓冲区里发来的命令，这时主从节点的数据就一致了。

2. **命令传播**：主从服务器在完成第一次同步后，双方之间就会维护一个 TCP **长连接**，后续主节点可以通过这个连接继续将写操作命令传播给从服务器，然后从服务器执行该命令来保持数据的一致性。主节点会先将命令写入到 replication buffer(复制缓冲区)，再发送给从节点。
3. **增量复制**：

如果主从服务器间的网络连接断开了，那么就无法进行命令传播了，这时从服务器的数据就没办法和主服务器保持一致了，客户端就可能从「从服务器」读到旧的数据。

**那么问题来了，如果此时断开的网络，又恢复正常了，要怎么继续保证主从服务器的数据一致性呢？**

**主节点会将命令写入到一个缓冲区，从节点和主节点断开连接又重新连上后，从节点会将自己的复制偏移量发送给主节点，主节点会根据自己的offset和从节点的offset进行对比，从而来决定进行哪种同步操作，如果从节点需要的数据还在缓冲区里，那就进行增量同步，反之就进行全量同步。当主服务器在repl_backlog_buffer 中找到主从节点差异（增量）的数据后，就会将增量的数据写入到 replication buffer 缓冲区**

    - repl_backlog_buffer(复制积压缓冲区)，是一个「环形」缓冲区，用于主从服务器断连后，从中找到差异的数据；
    - replication offset，标记上面那个缓冲区的同步进度，主从服务器都有各自的偏移量，主服务器使用 master_repl_offset 来记录自己「写」到的位置，从服务器使用 slave_repl_offset 来记录自己「读」到的位置。

**那 repl_backlog_buffer 缓冲区具体要调整到多大呢？**

repl_backlog_buffer 最小的大小可以根据这面这个公式估算。

**second * write_size_per_second**

second 为从节点断线重连所需的平均时间(以秒计算)。write_size_per_second 则是主节点平均每秒产生的写命令数据量大小。

举个例子，如果主服务器平均每秒产生 1 MB 的写命令，而从服务器断线之后平均要 5 秒才能重新连接主服务器。那么 repl_backlog_buffer 大小就不能低于 5 MB，否则新写地命令就会覆盖旧数据了。当然，为了应对一些突发的情况，可以将 repl_backlog_buffer 的大小设置为此基础上的 2 倍，也就是 10 MB。

关于 repl_backlog_buffer 大小修改的方法，只需要修改配置文件里下面这个参数项的值就可以。

```nginx
repl-backlog-size 1mb
```

### 主从复制架构中，过期key如何处理？
主节点处理了一个key或者通过淘汰算法淘汰了一个key，这个时间主节点模拟一条del命令发送给从节点，从节点收到该命令后，就进行删除key的操作。

### Redis 是同步复制还是异步复制？
Redis 主节点每次收到写命令之后，先写到内部的缓冲区，然后异步发送给从节点。

### 主从复制中两个 Buffer(replication buffer 、repl backlog buffer)有什么区别？
+ 出现的阶段不一样：
    - repl backlog buffer 是在增量复制阶段出现，**一个主节点只分配一个 repl backlog buffer；**
    - replication buffer 是在全量复制阶段和增量复制阶段都会出现，**主节点会给每个新连接的从节点，分配一个 replication buffer；**
+ 这两个 Buffer 都有大小限制的，当缓冲区满了之后，发生的事情不一样：
    - 当 repl backlog buffer 满了，因为是环形结构，会直接覆盖起始位置数据;
    - 当 replication buffer 满了，会导致连接断开，删除缓存，从节点重新连接，重新开始全量复制。

> 为什么有了replication buffer还要有repl backlog buffer？
>
> replication buffer只是短暂存储在命令传播中要发送给从节点的命令，在从节点断开连接后就会清空replication buffer，如果断开连接的从节点数量过多，然后未同步的数据都堆积在replication buffer中就会占用大量的内存
>

### 如何应对主从数据不一致？
**为什么会出现主从数据不一致？**

主从数据不一致，就是指客户端从从节点中读取到的值和主节点中的最新值并不一致。

之所以会出现主从数据不一致的现象，是因为主从节点间的命令复制是异步进行的，所以无法实现强一致性保证（主从数据时时刻刻保持一致）。

具体来说，在主从节点命令传播阶段，主节点收到新的写命令后，会发送给从节点。但是，主节点并不会等到从节点实际执行完命令后，再把结果返回给客户端，而是主节点自己在本地执行完命令后，就会向客户端返回结果了。如果从节点还没有执行主节点同步过来的命令，主从节点间的数据就不一致了。

**如何如何应对主从数据不一致？**

[https://www.cnblogs.com/-cyh/p/12919833.html](https://www.cnblogs.com/-cyh/p/12919833.html)

1. 业务可以接受，系统不优化
2. 强制读主， 如果某个业务需要保证强一致性那这部分数据就只从主节点读取，例如使用Redis来防止超卖，通过 Lua 脚本来完成库存判断和扣减操作，并且让主节点负责执行整个 Lua 脚本，从而保证数据的一致性。
3. 降低主从之间的网络延迟
4. 监控主从同步状态发现差异过大及时重启从节点进行全量同步，让客户端不再和这个从节点连接进行数据读取

### 主从切换如何减少数据丢失(脑裂)？
主从切换过程中，产生数据丢失的情况有两种：

+ 异步复制同步丢失
+ 集群产生脑裂数据丢失

我们不可能保证数据完全不丢失，只能做到使得尽量少的数据丢失。

**异步复制同步丢失**

简单来说就是主节点宕机后，可能会导致未同步到从节点的数据丢失

可以通过配置`min-replicas-to-write`和`min-replicas-max-lag`来尽可能减少数据的丢失

那么对于客户端，当客户端发现 master 不可写后，我们可以采取降级措施，将数据暂时写入本地缓存和磁盘中，在一段时间（等 master 恢复正常）后重新写入 master 来保证数据不丢失，也可以将数据写入 kafka 消息队列，等 master 恢复正常，再隔一段时间去消费 kafka 中的数据，让将数据重新写入 master 。

[https://raw.githubusercontent.com/redis/redis/7.4/redis.conf](https://raw.githubusercontent.com/redis/redis/7.4/redis.conf)

```plain
如果连接的从节点数量少于 N 个，且这些从节点的延迟小于或等于 M 秒，主节点可能会停止接受写入操作。
这 N 个从节点需要处于 “在线” 状态。
延迟（以秒为单位）必须小于或等于指定值，其计算依据是从从节点收到的最后一次 ping 消息，该消息通常每秒发送一次。
此选项不保证 N 个从节点都会接受写入操作，但会在可用从节点不足的情况下，将写入丢失的风险窗口限制在指定的秒数内。
例如，若要求至少有 3 个从节点的延迟 <= 10 秒，可设置：
min-replicas-to-write 3
min-replicas-max-lag 10
将其中一个或两个选项设置为 0 会禁用此功能。
默认情况下，min-replicas-to-write 设为 0（功能禁用），min-replicas-max-lag 设为 10。
```

> `min-slaves-max-lag` 是旧版本 Redis 中的参数名称，  
`min-replicas-max-lag` 是从 Redis 5.0 开始推荐使用的新名称。
>

**集群产生脑裂数据丢失**

由于网络问题，主节点和所有从节点和哨兵失去联系。主从数据不同步；重新选举主节点，就产生了两个主节点。等到网络恢复，旧主节点会降级为从节点，再与新主节点进行同步复制的时候，由于会从节点会清空自己的缓冲区，所以导致之前客户端写入的数据丢失了。

同样可以通过上面那两个参数来解决，通过设置这两个特定的参数，当脑裂发生时，原先的主节点将在接收到客户端的写入请求时拒绝这些操作。这阻止了对可能已经失去主节点地位的服务器的写入，所以能够防止在脑裂情况下发生大量数据丢失。

Redis脑裂可以采用min-slaves-to-write和min-slaves-max-lag合理配置尽量规避，但无法彻底解决

## 哨兵模式
为了解决主从模式的无法自动容错及恢复的问题，Redis引入了一种哨兵模式的集群架构，作用是实现**主从节点故障转移**。

### 哨兵机制是如何工作的？
哨兵节点主要负责三件事情：**监控**、**选主**、**通知**。

**监控：**

哨兵会每隔 1 秒给所有主从节点发送 PING 命令，当主从节点收到 PING 命令后，会发送一个响应命令给哨兵，这样就可以判断它们是否在正常运行。如果主节点或者从节点没有在规定的时间内响应哨兵的 PING 命令，哨兵就会将它们标记为「**主观下线**」。这个「规定的时间」是配置项`down-after-milliseconds`参数设定的，单位是毫秒。

当一个哨兵判断主节点主观下线后，会询问其他哨兵的意见，如果达到配置的法定人数(quorum)，主节点会被标记为“**客观下线**”。

**选主：**

在哨兵判断完主节点客观下线后，会在哨兵中选举出一个leader来负责主从的故障转移(一般是最先发现主节点下线的)，然后再进行故障转移

主从故障转移操作包含以下四个步骤：

1. **选出新主节点**

首先，哨兵会对所有从节点进行一轮基础筛选，排除那些不满足基本条件的节点。比如说已下线的节点、网络连接不稳定的节点，以及优先级设为 0 明确不参与挑选的节点。然后，哨兵会对剩下的从节点进行排序，选出最合适的主节点。

排序的标准有三个：

①、从节点优先级： slave-priority 的值越小优先级越高，优先级为 0 的从节点不会被选中。

②、复制偏移量： 偏移量越大意味着从节点的数据越新，复制的越完整。

③、运行 ID： 如果优先级和偏移量都相同，就比较运行 ID 的字典序，字典序小的优先。

2. 让从节点指向新主节点
3. 通知客户端的新主节点的ip和信息(通过 Redis 的发布者/订阅者机制来实现)

哨兵提供的消息订阅频道有很多，不同频道包含了主从节点切换过程中的不同关键事件，几个常见的事件如下：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/webp/52164061/1753473527464-888abaf9-1590-49f2-8075-e7028a5c9fd6.webp)

4. 将旧主节点变为从节点

继续监视旧主节点，当旧主节点重新上线时，哨兵集群就会向它发送 SLAVEOF 命令，让它成为新主节点的从节点

### 哨兵集群是如何组成的？
在配置哨兵的信息时，只需要填下面这几个参数，设置主节点名字、主节点的 IP 地址和端口号以及 quorum 值。

```plain
sentinel monitor <master-name> <ip> <redis-port> <quorum>
```

不需要填其他哨兵节点的信息，它们是如何感知对方的，又是如何组成哨兵集群的？

哨兵节点之间是通过 Redis 的**发布者/订阅者机制**来相互发现的。

哨兵 A 把自己的 IP 地址和端口的信息发布到__sentinel__:hello 频道上，哨兵 B 和 C 订阅了该频道。那么此时，哨兵 B 和 C 就可以从这个频道直接获取哨兵 A 的 IP 地址和端口号。然后，哨兵 B、C 可以和哨兵 A 建立网络连接。通过这个方式，哨兵 B 和 C 也可以建立网络连接，这样一来，哨兵集群就形成了。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753473899784-f0ea2b93-ba5a-4f60-9f0e-052c27b159fc.png)

哨兵集群会对「从节点」的运行状态进行监控，那哨兵集群如何知道「从节点」的信息？

主节点知道所有「从节点」的信息，所以哨兵会每 10 秒一次的频率向主节点发送 INFO 命令来获取所有「从节点」的信息。

如下图所示，哨兵 B 给主节点发送 INFO 命令，主节点接受到这个命令后，就会把从节点列表返回给哨兵。接着，哨兵就可以根据从节点列表中的连接信息，和每个从节点建立连接，并在这个连接上持续地对从节点进行监控。哨兵 A 和 C 可以通过相同的方法和从节点建立连接。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753473914288-e50741aa-658e-4afb-9256-d0d2794aea95.png)

### 为什么哨兵节点至少要有 3 个？
+ 如果只有 1 个哨兵，没法投票，哨兵自身出现问题也没法分辨。
+ 如果是 2 个哨兵，投票就容易产生平票的情况，难以决定主节点是否真的宕机。

部署至少 3 个哨兵是为了实现**投票机制、保证故障判定的准确性、避免脑裂**，从而确保 Redis 高可用系统的稳定性。  

quorum 的值建议设置为**哨兵个数的二分之一加 1**，例如 3 个哨兵就设置 2，5 个哨兵设置为 3，而且哨兵节点的数量应该是**奇数**。

## Redis Cluster分片集群
哨兵模式是在主从模式的基础上实现的，支持读写分离，并且可以在主节点出现故障时自动完成故障转移，解决了传统主从复制无法自动切换的问题，从而提高了系统的可用性。  

但是哨兵模式依然存在一个明显的限制——主节点的写能力和存储容量仍然受限于单机的硬件配置，无法满足更大规模的数据处理需求。  

为了解决这个问题，Redis 提供了 Cluster 模式。Cluster 模式实现了Redis的**<font style="color:rgb(216,57,49);">分布式存储</font>**，它把数据分片存储在多个节点上，每个节点只负责一部分读写任务。这样一来，不仅突破了单节点的写入和存储瓶颈，还支持**在线扩容**和自动故障转移，从而提升系统的可用性和扩展性。  

Redis cluster集群节点最小配置6个节点以上（3主3从），其中主节点提供读写操作，从节点作为备用节点，不提供请求，只作为故障转移使用。

> **各个节点之间是怎么通信的呢**
>
> 各个节点通过Gossip协议通信，Gossip是一种谣言传播协议，每个节点周期性地从节点列表中选择 k 个节点，将本节点存储的信息传播出去，直到所有节点信息一致。
>

### 客户端是怎样知道该访问哪个分片的?
Redis Cluster采用哈希槽（Hash Slot）来处理数据和实例之间的映射关系。

一个切片集群被分为16384个slot（槽），当我们在存取 Key 的时候，Redis 会根据 CRC16 算法得出一个结果，然后把结果对 16384 求余数，这样每个 key 都会对应一个编号在 0-16383 之间的哈希槽。

### 集群如何实现故障转移
每秒钟，节点会向一定数量的随机节点发送 PING 消息，当发现某个节点长时间未响应 PING 消息，就会将其标记为主观下线。当半数以上的主节点都认为某节点主观下线时，这个节点就会被标记为“客观下线”。如果下线的是主节点，它的从节点之一将被选举为新的主节点，接管原主节点负责的哈希槽。

### 为什么是16384个槽
Redis Cluster 采用数据分片机制，定义了 16384个 Slot槽位，集群中的每个Redis 实例负责维护一部分槽以及槽所映射的键值数据。

Redis每个节点之间会定期发送ping/pong消息（心跳包包含了其他节点的数据），用于交换数据信息。

Redis集群的节点会按照以下规则发ping消息：

+ 1、每秒会随机选取5个节点，找出最久没有通信的节点发送ping消息
+ 2、每100毫秒都会扫描本地节点列表，如果发现节点最近一次接受pong消息的时间大于cluster-node-timeout/2 则立刻发送ping消息

心跳包的消息头里面有个myslots的char数组，是一个bitmap，每一个位代表一个槽，如果该位为1，表示这个槽是属于这个节点的。

接下来，解答为什么 Redis 集群的最大槽数是 16384 个，而不是65536 个。

1、<font style="color:rgb(216,57,49);">如果采用 16384 个插槽，那么心跳包的消息头占用空间 2KB （16384/8）；如果采用 65536 个插槽，那么心跳包的消息头占用空间 8KB (65536/8)。可见采用 </font><font style="color:rgb(216,57,49);">65536</font><font style="color:rgb(216,57,49);">插槽，</font>**<font style="color:rgb(216,57,49);">发送心跳信息的消息头达8k，比较浪费带宽</font>**<font style="color:rgb(216,57,49);">。</font>

2、一般情况下一个Redis集群**不会有超过1000个master节点**，太多可能导致网络拥堵。

3、哈希槽是通过一张bitmap的形式来保存的，在传输过程中，会对bitmap进行压缩。bitmap的填充率越低，**压缩率**越高。其中bitmap 填充率 = slots / N (N表示节点数)。所以，插槽数越低， 填充率会降低，压缩率会提高

4、如果槽太少，支持的节点数就会比较低

[https://github.com/redis/redis/issues/2576](https://github.com/redis/redis/issues/2576)

> 原因是：
>
> 正常的心跳包会携带节点的完整配置信息，这些信息可以以幂等的方式替换旧配置，从而更新旧的配置。这意味着它们包含了节点的槽位（slots）配置的原始形式。对于 16k 个槽位，这种表示方式占用 2k 的空间；但如果使用 65k 个槽位，则会占用高达 8k 的空间，这是不可接受的。
>
> 与此同时，Redis Cluster 由于其他设计上的权衡，不太可能扩展到超过 1000 个主节点。因此，16k 的槽位数是一个合理的选择：它足以在最多 1000 个主节点的情况下为每个主节点分配足够的槽位，同时数量又足够小，可以方便地以原始位图的形式传播槽位配置。
>
> 需要注意的是，在小型集群中，这种位图很难进行压缩，因为当节点数 N 较小时，位图中会有大约 slots/N 个 bit 被设置，占据了相当高的比例。
>

# -----------------性能优化-----------------
# 缓存雪崩
<font style="color:rgb(51, 51, 51);">缓存雪崩是指在同一时段大量的缓存key同时失效或者Redis服务宕机，导致大量请求到达数据库，给数据库带来巨大压力。</font>

<font style="color:rgb(51, 51, 51);">针对</font>**<font style="color:rgb(51, 51, 51);">大量数据同时过期</font>**<font style="color:rgb(51, 51, 51);">而引发的缓存雪崩问题，常见的应对方法有下面这几种：</font>

+ <font style="color:rgb(51, 51, 51);">给不同的Key的TTL添加随机值</font>
+ 加互斥锁，保证同一时间内只有一个请求来构建缓存

针对 Redis 故障宕机而引发的缓存雪崩问题，常见的应对方法有下面这几种：

+ 服务熔断或请求限流机制
+ 构建 Redis 缓存高可靠集群，避免服务单点故障

# 缓存击穿
缓存击穿是指热点key突然过期导致大量请求在短时间内访问数据库

解决方案：

+ 加互斥锁，保证同一时间内只有一个请求来构建缓存

当多个线程查到缓存不存在去构建缓存, 只有第一个拿到锁的线程能去构建缓存, 其他的线程在拿到锁之后首先要判断缓存是否存在, 如果存在就直接返回不用去重复构建缓存。

+ 逻辑过期

缓存值包含过期时间，异步更新数据。<font style="color:rgb(51, 51, 51);">当用户开始查询redis时，判断是否命中，如果没有命中则直接返回空数据，不查询数据库，而一旦命中后，将value取出，判断value中的过期时间是否满足，如果没有过期，则直接返回redis中的数据，如果过期，则在开启独立线程后直接返回之前的数据，独立线程去重构数据，重构完成后释放互斥锁。</font>

# <font style="color:rgb(51, 51, 51);">缓存穿透</font>
<font style="color:rgb(51, 51, 51);">缓存穿透是指客户端请求的数据在缓存中和数据库中都不存在，这样缓存永远不会生效，这些请求都会打到数据库。</font>

<font style="color:rgb(51, 51, 51);">解决方案：</font>

+ <font style="color:rgb(51, 51, 51);">缓存空值</font>

当发现要查询的数据在数据库中也不存在时就将这个key缓存为一个空值，避免下次请求这个数据再次访问数据库

缺点是可能会造成大量的key占用内存

+ <font style="color:rgb(51, 51, 51);">布隆过滤器</font>

写入时，将Key存入布隆过滤器。查询前先检查布隆过滤器，若不存在直接返回。缺点是有一定的误判率，就是布隆过滤器返回不存在时就一定不存在，但是如果布隆过滤器说存在那就可能存在也可能不存在，解决方法是结合空值缓存，<font style="color:rgb(51, 51, 51);">先使用布隆过滤器进行初步筛选, 如果误判了去查数据库，那就缓存一个空值, 避免下次误判又去查数据库。</font>

<font style="color:rgb(51, 51, 51);">布隆过滤器还有一个缺点就是不能删除数据，针对这个缺点可以使用布谷鸟过滤器，但是布谷鸟过滤器会占用更大的内存。</font>

# 缓存预热
[https://www.yuque.com/hollis666/vn7dy3/lu2phlltfr1umdxw#CvIBR](https://www.yuque.com/hollis666/vn7dy3/lu2phlltfr1umdxw#CvIBR)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753559728723-1773f451-1776-49d9-9e70-c0135a3d18c9.png)

# Redis Pipeline是什么？
Redis Pipeline（管道）是一种批量执行命令的机制，允许客户端在一次网络往返中发送多个命令，大幅提升批量操作效率。

**Pipeline是不保证原子性的**，他的多个命令都是独立执行的，Redis并不保证这些命令可以以不可分割的原子操作进行执行。如果执行多个命令过程中，有一个命令失败了，其他命令还是会被执行，而不会回滚的。

```java
@SpringBootTest
public class redisTest {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    void test() {
        // 使用pipeline批量执行命令耗时5秒
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (int i = 0; i < 100000; i++) {
                stringRedisConn.set("key:" + i, "value" + i);
            }
            return null;
        });
        // 单独执行耗时1分22秒
        for (int i = 0; i < 100000; i++) {
            stringRedisTemplate.opsForValue().set("key:" + i, "value" + i);
        }
    }
}
```

```lua
EVAL "for i=0,999999 do redis.call('SET', 'key:'..i, 'value'..i) end" 0
```

```lua
EVAL "
local cursor = '0'
repeat
  local result = redis.call('SCAN', cursor, 'MATCH', 'key:*', 'COUNT', 1000)
  cursor = result[1]
  local keys = result[2]
  for i, key in ipairs(keys) do
    redis.call('DEL', key)
  end
until cursor == '0'
return true
" 0
```

# keys指令的问题
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1753617063599-05dd7791-a7ad-40b2-ae76-85eb936df492.png)

# 大key
**<font style="color:rgb(0, 0, 0);">Redis 的 Big Key 可能存在以下几个危害：</font>**

1. **<font style="color:rgb(0, 0, 0) !important;">影响性能</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：由于 big key 的 values 占用的内存会很大，所以读取它们的速度会很慢，会影响系统的性能。</font>
2. **<font style="color:rgb(0, 0, 0) !important;">占用内存</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：大量的 big key 也会占满 Redis 的内存，让 Redis 无法继续存储新的数据，而且也会导致 Redis 卡住</font>
3. **<font style="color:rgb(0, 0, 0) !important;">内存空间不均匀</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：比如在 Redis 集群中，可能会因为某个节点上存储了 Big Key，导致多个节点之间内存使用不均匀。</font>
4. **<font style="color:rgb(0, 0, 0) !important;">影响 Redis 备份和恢复</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：如果从 RDB 文件中恢复全量数据时，可能需要大量的时间，甚至无法正常恢复。</font>
5. **<font style="color:rgb(0, 0, 0) !important;">搜索困难</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：由于大 key 可能非常大，因此搜索 key 内容时非常困难，并且可能需要花费较长的时间完成搜索任务。</font>
6. **<font style="color:rgb(0, 0, 0) !important;">迁移困难</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：大key的迁移和复制压力较大，极易破坏缓存的一致性	</font>
7. **<font style="color:rgb(0, 0, 0) !important;">过期执行耗时</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：如果 Bigkey 设置了过期时间，当过期后，这个 key 会被删除，而大 key 的删除过程也比较耗时</font>



**<font style="color:rgb(0, 0, 0) !important;">多大算大？</font>**

<font style="color:rgba(0, 0, 0, 0.85) !important;">Redis 中多大的 key 算作大 key 并没有一个固定的标准，因为这主要取决于具体的场景和应用需求。一般来说，如果一个 key 的 value 比较大，占用的内存比较多，或者某个 key 包含的元素数量比较多，这些都可以被认为是大 key。</font><font style="color:rgb(0, 0, 0);">  
</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">通常情况下，建议不要超过以下设定，超过这些数量就可能会影响 Redis 的性能。</font><font style="color:rgb(0, 0, 0);">  
</font>

+ <font style="color:rgba(0, 0, 0, 0.85) !important;">对于 String 类型的 Value 值，值超过 5MB（腾讯云定义是 10M，阿里云定义是 5M，我认为 5M 合适一点）。</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">对于 Set 类型的 Value 值，含有的成员数量为 10000 个（成员数量多）。</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">对于 List 类型的 Value 值，含有的成员数量为 10000 个（成员数量多）。</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">对于 Hash 格式的 Value 值，含有的成员数量 1000 个，但所有成员变量的总 Value 值大小为 100MB（成员总的体积过大）。</font>

<font style="color:rgb(0, 0, 0);"></font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">但是，这些并不是绝对的限制，而是一个经验值，具体的情况还需要根据应用场景和实际情况进行调整。</font>

<font style="color:rgb(28, 31, 35);"></font>

**<font style="color:rgb(28, 31, 35);">识别big key</font>**

 Redis 提供了一个专用的分析工具叫 **redis-cli --bigkeys**，可以找出数据库中最占内存的 key  

`redis-cli --bigkeys -n 1 -a 123456`

这里的1为数据库编号，123456为密码

```bash
root@6eb6bafcf4f4:/data# redis-cli --bigkeys -n 1 -a 123456
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.

# Scanning the entire keyspace to find biggest keys as well as
# average sizes per key type.  You can use -i 0.1 to sleep 0.1 sec
# per 100 SCAN commands (not usually needed).

[00.00%] Biggest set    found so far '"short-link:stats:uip:9-1.top/2WAs8B"' with 1 members
[00.00%] Biggest set    found so far '"short-link:stats:uip:9-1.top/JIaFsF"' with 3 members
[00.00%] Biggest hash   found so far '"{userRegisterCachePenetrationBloomFilter}:config"' with 4 fields
[00.00%] Biggest string found so far '"gidRegisterCachePenetrationBloomFilter"' with 356370828 bytes
[00.00%] Biggest set    found so far '"short-link:stats:uv:9-1.top/d5DMrd"' with 96 members
[51.28%] Biggest hash   found so far '"short-link:group-link-count"' with 8 fields
[76.92%] Biggest set    found so far '"short-link:stats:uv:9-1.top/4VdCA7"' with 1359 members

-------- summary -------

Sampled 39 keys in the keyspace!
Total key length in bytes is 1377 (avg len 35.31)

Biggest   hash found '"short-link:group-link-count"' has 8 fields
Biggest string found '"gidRegisterCachePenetrationBloomFilter"' has 356370828 bytes
Biggest    set found '"short-link:stats:uv:9-1.top/4VdCA7"' has 1359 members

0 lists with 0 items (00.00% of keys, avg size 0.00)
5 hashs with 21 fields (12.82% of keys, avg size 4.20)
4 strings with 712972221 bytes (10.26% of keys, avg size 178243055.25)
0 streams with 0 entries (00.00% of keys, avg size 0.00)
30 sets with 1500 members (76.92% of keys, avg size 50.00)
0 zsets with 0 members (00.00% of keys, avg size 0.00)

```

**处理big key**

<font style="color:rgba(0, 0, 0, 0.85) !important;">想要解决 Big Key 的问题，根据具体的业务情况有很多不同的方案，下面简单列几个：</font>

1. <font style="color:rgb(0, 0, 0);">有选择地删除 Big Key</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">针对 Big Key，我们可以针对一些访问频率低的进行有选择性的删除，删除 Big Key 来优化内存占用。</font>

2. <font style="color:rgb(0, 0, 0);">合理设置缓存 TTL</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">除了手动删除以外，还可以通过</font>**<font style="color:rgb(0, 0, 0) !important;">合理的设置缓存 TTL</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">，避免过期缓存删除不及时而增大 key 大小。</font>

3. <font style="color:rgb(0, 0, 0);">拆分 Big Key</font>
+ **<font style="color:rgb(0, 0, 0) !important;">业务代码层拆分</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：在业务代码中，将一个 big key 有目的地进行拆分（比如根据日期、用户尾号等维度拆分 ）。用小键替代大键，可有效减小存储空间，避免影响系统性能。</font>
+ **<font style="color:rgb(0, 0, 0) !important;">集群模式分散</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">：使用 Cluster 集群模式，将大 key 分散到不同服务器上，加快响应速度。</font>
4. <font style="color:rgb(0, 0, 0);">部分迁移</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">将大键存放在单独的数据库中，从而实现对大键的部分迁移 。</font>

# <font style="color:rgba(0, 0, 0, 0.85) !important;">-------------------场景--------------------</font>
# 分布式锁
在使用SETNX实现的分布式锁中，因为存在锁无法续期导致并发冲突的问题，所以在真实的生产环境中用的并不是很多，在真正使用Redis时，用的比较多的是基于Redisson实现的分布式锁。

## 可重入锁
### 原理
 Redisson 使用 Redis 的 **Hash 结构** 存储锁的信息，格式如下：  

```yaml
key: 锁的名称
value (Hash): 
    field: UUID:threadId
    value: 重入次数（int）
```

UUID用于区分不同JVM实例，threadId用于区分同一JVM内的不同线程，只有同一个JVM的同一个线程才能重复获取锁，获取了多少次锁就要解多少次锁才能彻底释放锁。

锁获取释放流程：

![画板](https://cdn.nlark.com/yuque/0/2025/jpeg/52164061/1753644714544-e02e08eb-3119-4daf-a214-1b43ca9c2dc0.jpeg)

RedissonClient中提供了tryLock()和lock()两个方法来获取锁

1. **tryLock()**

tryLock()有三个参数(无参相当于传参(-1, -1, TimeUnit.SECONDS))

+ waitTime  最大等待获取锁的时间，不指定等于传入-1，表示不等待, 失败了立即返回
+ leaseTime 锁的持有时间(自动释放时间)，不指定等于传入-1，表示使用默认的看门狗过期时间超过30秒自动释放
+ unit 时间单位
2. **lock()**

与tryLock()的区别是没有waitTime这个参数，lock()在成功获取锁之前是会一直阻塞下去的。

## 读写锁
Redisson中支持分布式可重入读写锁，这种锁允许同时有多个读锁或只能有一个写锁对同一个资源进行加锁，满足<font style="color:rgb(34, 34, 38);">读读共享, 写写互斥, 读写互斥</font>。

## 看门狗
为了避免锁超时，Redisson中引入了看门狗的机制，他可以帮助我们在Redisson实例被关闭前，不断的延长锁的有效期，确保锁是因为业务执行完而释放的，而不是因为业务阻塞或者运行时间长导致的释放

自己给锁设置有效期不会触发看门狗机制

> 默认情况下，看门狗的检查锁的超时时间是30秒钟，也可以通过修改Config.lockWatchdogTimeout来另行指定
>

# <font style="color:rgba(0, 0, 0, 0.85) !important;">限流</font>
## 如何基于Redis实现滑动窗口限流？
滑动窗口限流是一种流量控制策略，用于控制在一定时间内允许执行的操作数量或请求频率。它的工作方式类似于一个滑动时间窗口，在窗口内允许的操作数量是固定的，窗口会随着时间的推移不断滑动。

主要步骤如下：

1. 定义滑动窗口的时间范围，例如，窗口大小为60秒。
2. 每次收到一个请求时，我们就往zset中新增一个元素，分数为当前时间戳。
3. 然后再通过ZREMRANGEBYSCORE命令来删除分值小于窗口起始时间戳（当前时间戳-60s）的数据，这样剩余元素就是在时间窗口内应该存在的元素。
4. 最后，再使用ZCARD命令来获取有序集合中的成员数量，如果大于等于限制数量，则不插入并返回失败，否则插入并返回成功。

[示例](https://www.yuque.com/shangshanruoshui-0bl0c/hgp3nz/ud3bu9ze4cksnaxg#cm6U8)

## 基于Redisson实现限流
在 Redisson框架中，已经给我们提供了一个限流器——RRateLimiter，不过他并不是滑动窗口，而是一个令牌桶的算法。具体的可以参考：[https://github.com/oneone1995/blog/issues/13](https://github.com/oneone1995/blog/issues/13)

使用方式如下：

```java
public Boolean tryAcquire(String key, int limit, int windowSize) {
    RRateLimiter rRateLimiter = redissonClient.getRateLimiter(LIMIT_KEY_PREFIX + key);
    if (!rRateLimiter.isExists()) {
        rRateLimiter.trySetRate(RateType.OVERALL, limit, windowSize, RateIntervalUnit.SECONDS);
    }
    return rRateLimiter.tryAcquire();
}
```

# Redisson 延迟队列
[https://developer.aliyun.com/article/1627048](https://developer.aliyun.com/article/1627048)

[https://zhuanlan.zhihu.com/p/343811173](https://zhuanlan.zhihu.com/p/343811173)

通过zset存储任务和过期时间，定时任务轮询zset取出到期的任务并执行

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1760256845356-88121c77-7381-42d6-a773-6528e3ae2fa5.png)

# <font style="color:rgba(0, 0, 0, 0.85) !important;">-------------------其他--------------------</font>
# Redis的作用
缓存（建议）

消息队列（不建议）

延迟消息（不建议）

排行榜（建议）

计数器（建议）

分布式ID（可以）

分布式锁（建议）

地理位置应用（建议）

分布式限流（可以）

分布式Session（建议）

布隆过滤器（建议）

bitmap状态统计（可以）

共同关注（建议）

推荐关注（可以）

