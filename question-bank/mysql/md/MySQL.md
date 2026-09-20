# -------------------概念-------------------
# MySQL的数据类型有哪些?
 MySQL 的数据类型主要可以分为三大类：数值类型、字符串类型和日期/时间类型。  

数值类型有tinyint, int, bigint, float, double, decimal等

字符串类型有定长字符串char, 变长字符串varchar以及存储大文本的text和blob等

日期和时间类型有date, time, datetime, timestamp, year等

除了这些类型还有枚举和空间数据类型等, 这个应该不怎么常用

#  MySQL的DDL和DML分别是什么含义？  
DDL就是数据定义语言, 用来定义或修改数据库结构

DML就是数据操作语言, 用来对数据进行插入、更新 、删除等 

#  MySQL的三范式？反范式？
**<font style="color:rgb(0, 0, 0);">第一范式（1NF）:</font>**

+ 定义：所有字段都是原子性的，不可再分。
+ 目的：确保每个字段只存储一个值。

**<font style="color:rgb(0, 0, 0);">第二范式（2NF）:</font>**

+ 定义：在满足第一范式的基础上，所有非主键字段完全依赖于主键。(<font style="color:rgba(0, 0, 0, 0.85) !important;">主键是</font>**组合主键**<font style="color:rgba(0, 0, 0, 0.85) !important;">（多个字段组合而成）时才可能出现部分依赖。  </font>)
+ 目的：消除部分依赖，确保数据的完整性。

<details class="lake-collapse"><summary id="u5d5c7502"><span class="ne-text">示例</span></summary><h3 id="f92d8eba"><span class="ne-text">📌</span><span class="ne-text">示例：学生课程成绩表（未满足2NF）</span></h3><p id="uc169778b" class="ne-p"><img src="https://cdn.nlark.com/yuque/0/2025/png/52164061/1753900013121-c7a6c564-011b-430a-a933-1e825b6fad3a.png" width="720.6666666666666" title="" crop="0,0,1,1" id="u1496b58c" class="ne-image"></p><hr id="cpVgj" class="ne-hr"><h3 id="21baca25"><span class="ne-text">🔍</span><span class="ne-text">主键说明：</span></h3><ul class="ne-ul"><li id="u01bf0eb7" data-lake-index-type="0"><span class="ne-text">主键是 </span><strong><span class="ne-text">(学号, 课程号)</span></strong><span class="ne-text"> 的组合，因为一个学生选多门课，一个课程也被多个学生选。</span></li></ul><hr id="GuAEu" class="ne-hr"><h3 id="af3a2bd9"><span class="ne-text">❌</span><span class="ne-text">问题分析（为什么不满足2NF）：</span></h3><ul class="ne-ul"><li id="uac702e09" data-lake-index-type="0"><strong><span class="ne-text">学生姓名 Name</span></strong><span class="ne-text"> 只依赖于 </span><strong><span class="ne-text">学号 StudentID</span></strong><span class="ne-text">，不依赖于完整的主键（学号, 课程号）。</span></li><li id="u896da467" data-lake-index-type="0"><span class="ne-text">所以，“学生姓名”对主键存在</span><strong><span class="ne-text">部分依赖</span></strong><span class="ne-text">，违反了第二范式。</span></li></ul></details>
**<font style="color:rgb(0, 0, 0);">第三范式（3NF）:</font>**

+ 定义：在满足<font style="color:rgba(0, 0, 0, 0.85) !important;">第二范式</font>的基础上，非主键字段不依赖于其他非主键字段。
+ 目的：消除传递依赖，确保数据的独立性。

<details class="lake-collapse"><summary id="ud35cb577"><span class="ne-text">示例</span></summary><h3 id="15e5c60f"><span class="ne-text">📌</span><span class="ne-text">示例：员工表（未满足3NF）</span></h3><p id="uc4e5ca76" class="ne-p"><img src="https://cdn.nlark.com/yuque/0/2025/png/52164061/1753900070032-2660b739-2442-4c1e-88e8-d1ac0a9a2b95.png" width="724.6666666666666" title="" crop="0,0,1,1" id="ua9f1ee7c" class="ne-image"></p><hr id="SEOiN" class="ne-hr"><h3 id="ljq1R"><span class="ne-text">🔍</span><span class="ne-text">主键说明：</span></h3><ul class="ne-ul"><li id="uacdcdfe3" data-lake-index-type="0"><span class="ne-text">主键是 </span><strong><span class="ne-text">EmpID</span></strong><span class="ne-text">（员工ID），因为它唯一标识每一行记录。</span></li></ul><hr id="mOQyp" class="ne-hr"><h3 id="76189a27"><span class="ne-text">❌</span><span class="ne-text">问题分析（为什么不满足3NF）：</span></h3><ul class="ne-ul"><li id="u949875d4" data-lake-index-type="0"><strong><span class="ne-text">部门名称 DeptName</span></strong><span class="ne-text"> 是一个非主属性，它</span><strong><span class="ne-text">不是直接依赖于主键 EmpID</span></strong><span class="ne-text">。</span></li><li id="uf95c5dad" data-lake-index-type="0"><span class="ne-text">它依赖的是 </span><strong><span class="ne-text">DeptID</span></strong><span class="ne-text">，而 DeptID 又依赖于主键 EmpID。</span></li><li id="uba7cbfc7" data-lake-index-type="0"><span class="ne-text">所以，DeptName 对 EmpID 是一种</span><strong><span class="ne-text">传递依赖</span></strong><span class="ne-text">，这违反了第三范式。</span></li></ul></details>
**反范式****<font style="color:rgb(0, 0, 0);">:</font>**

+ MySQL中的“反范式”是一种数据库设计策略，与“范式化”设计相反。它是指为了提升性能（尤其是读取性能）而有意违反数据库规范化范式的一种做法。反范式通常会引入一些数据冗余或重复字段，以换取更高的查询效率、更少的表连接，尤其在高并发、大数据量的系统中比较常见。  
+ 现在业务上的表设计基本都是反范式的。当然不是说完全不遵守范式，而是适当的进行调整。 比如业务上经常需要冗余字段，减少联表查询，提升性能，特别是业务量比较大的公司，这种冗余是很有必要的！
+ 比如说上面员工表的部门名称原本是属于部门表的，增加这个冗余字段可以减少联表查询，提升性能

<font style="color:rgba(0, 0, 0, 0.85) !important;">总结: </font>

**<font style="color:rgba(0, 0, 0, 0.85) !important;">第一范式</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">就是确保每个列的值都是原子值, 比如说地址这个字段如果可以拆成省、市、区的话那它就不符合第一范式</font>

**第二范式**就是在第一范式的基础上消除了部分依赖, 所有的非主键字段必须依赖于整个主键, 而不是只依赖于主键的一部分。  

**第三范式**就是在第二范式的基础上消除了传递依赖, 非主键字段不依赖于其他非主键字段

**反范式**就是适当的违反范式, 比如说增加一些冗余字段, 减少联表查询, 提升性能

# 为什么要小表驱动大表
**小表驱动大表**，意思是：

让记录较少的表作为驱动表，记录较多的表作为被驱动表。



在 MySQL 中，**驱动表（Driving Table）** 是在执行 **多表连接（JOIN）查询** 时，查询优化器首先选择用于驱动整个连接过程的那张表。  



**STRAIGHT_JOIN**只适用于内连接，因为**left join、right join已经知道了哪个表作为驱动表**，哪个表作为被驱动表，比如left join就是以左表为驱动表，right join反之，而STRAIGHT_JOIN就是在内连接中使用，而强制使用左表来当驱动表，所以这个特性可以用于一些调优，强制改变mysql的优化器选择的执行计划

[https://www.cnblogs.com/mzq123/p/11830429.html](https://www.cnblogs.com/mzq123/p/11830429.html)



假设我们有两个表：employees（1000 条记录）和 departments（10 条记录），并且要进行以下查询：

```sql
SELECT e.name, d.department_name
FROM employees e
JOIN departments d ON e.department_id = d.id
```

在不考虑hash join等其他链接方式，只考虑nested loop join的情况下，其实执行的次数是笛卡尔积，即：

```java
for(1000) {
    for(10)
}

和

for(10) {
    for(1000)
}
```

但是，假设employees.department_id和departments.id 都有索引的情况下，就不一样了，因为索引的查询是比较快的，他的复杂度是log(n)。那么：

+ 大表驱动小表，复杂度为：O(1000) * O(log 10)
+ 小表驱动大表，复杂度为：O(10) * O(log 1000)

这样一算的话，就非常清楚了，肯定是小表驱动大表的整体的复杂度更低！



假如有两张表

```sql
CREATE TABLE `user`
(
    `id`   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL,
    INDEX (`name`) -- 可用于按姓名查询
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
```

```sql
CREATE TABLE `order`
(
    `id`           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    `user_id`      INT UNSIGNED   NOT NULL,
    `order_amount` DECIMAL(10, 2) NOT NULL,
    `order_status` ENUM ('pending', 'paid', 'shipped', 'cancelled') DEFAULT 'pending',
    `order_date`   DATETIME                                         DEFAULT CURRENT_TIMESTAMP,
    INDEX (`user_id`) -- 加快通过 user_id 查询订单的速度
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
```

```sql
SELECT o.*, u.name
FROM user u
         JOIN `order` o ON o.user_id = u.id
WHERE u.name = '张三';
```

使用`user`表作为驱动表, MySQL会通过`u.name = '张三'`将数据先过滤一遍再和`order`表进行连接

```sql
SELECT o.*, u.name
FROM `order` o
         JOIN user u ON o.user_id = u.id
WHERE u.name = '张三';
```

使用`order`表作为驱动表, MySQL会直接使用`order`表和`user表`进行连接后再用`u.name = '张三';`筛选, 效率极低。

但是 MySQL 优化器会自动将`user`表作为驱动表, 除非使用`STRAIGHT_JOIN`。

**优化器可能根据统计信息自动决定驱动表**，如果没有索引或统计信息失真，可能会出现“让大表驱动小表”的低效计划。可以通过 **添加合适的索引** 或 **使用 **`**STRAIGHT_JOIN**`（MySQL中）来引导执行计划。 

---

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/jpeg/52164061/1749399924739-45e8095f-0c8d-4815-bce6-5295f99e36b3.jpeg)



关于`STRAIGHT_JOIN`

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1749400318291-b0dc8859-91b5-4302-956e-dfb5a3d47102.png)

---

总结:  **驱动表就是多表连接中，最先读取并驱动整个连接过程的表,  通常应优先选择小表作为驱动表,  尤其是在连接条件明确、且筛选条件出现在小表中的情况下。**比如说有一个user表和一个非常大的order表, 将这两张表通过主键进行联表查询并且where里面是用user表中的name作为条件 , 这时候就要将user表作为驱动表, 这样的话就可以先去user表里面筛选出一部分数据来, 然后再和order表进行连接, 否则的话就**有可能**先将order表和user表连接后再根据name筛选, 这样的效率是很低的

还有一种情况就是连接条件的字段没有索引, 这个时候可能会用到`join_buffer`, mysql会先将驱动表的数据加载进`join_buffer`, 然后再和被驱动表进行匹配, 如果驱动表太大的话, `join_buffer`一次性可能装不完, 就要进行多次加载, 效率比较低

直接使用join时MySQL 优化器会**自动选择驱动表**, 除非用`STRAIGHT_JOIN``left jon``right join`等

# MySQL表的连接方式 (七种)
1. **内连接（INNER JOIN）**
    - **说明**：返回两个表中匹配的记录（交集）。
    - **语法**：

```sql
SELECT * FROM A INNER JOIN B ON A.id = B.a_id;
```

2. **左连接（LEFT JOIN / LEFT OUTER JOIN）**
    - **说明**：返回左表的所有记录，即使右表中没有匹配的记录。
    - **语法**：

```sql
SELECT * FROM A LEFT JOIN B ON A.id = B.a_id;
```

3. **右连接（RIGHT JOIN / RIGHT OUTER JOIN）**
    - **说明**：返回右表的所有记录，即使左表中没有匹配的记录。
    - **语法**：

```sql
SELECT * FROM A RIGHT JOIN B ON A.id = B.a_id;
```

4. **全连接（FULL JOIN / FULL OUTER JOIN）**
    - **说明**：返回左表和右表中所有的记录，没有匹配的部分补 `NULL`。
    - **MySQL 不直接支持**，可以用 `UNION` 模拟：

```sql
SELECT * FROM A LEFT JOIN B ON A.id = B.a_id
UNION
SELECT * FROM A RIGHT JOIN B ON A.id = B.a_id;
```

5. **自连接（SELF JOIN）**
    - **说明**：表与自身进行连接，用于查找表中相关的记录（如上下级关系）。
    - **语法**：

```sql
SELECT A.name, B.name FROM employees A
JOIN employees B ON A.manager_id = B.id;
```

6. **交叉连接（CROSS JOIN）**
    - **说明**：笛卡尔积，返回两个表所有组合（不推荐在大表中使用）。
    - **语法**：

```sql
SELECT * FROM A CROSS JOIN B;
```

7. **自然连接（NATURAL JOIN）**
    - **说明**：自动基于两个表中同名且相同类型的列进行连接（不常用，容易出错）。
    - **语法**：

```sql
SELECT * FROM A NATURAL JOIN B;
```

# count(列名), count(1), count(*)有什么区别
1. `count(*)` 会统计表中所有行的数量，包括 `null` 值（不会忽略任何一行数据）。由于只是计算行数，不需要对具体的列进行处理，因此性能通常较高。
2. `count(1)` 和 `count(*)` 几乎没差别，也会统计表中所有行的数量，包括 `null` 值。 
3. `count(字段名)` 会统计指定字段不为 `null` 的行数。这种写法会对指定的字段进行计数，只会统计字段值不为 `null` 的行。
    1. 如果是主键或者有`not null`约束会直接按行进行累加, 所以就更快

`<font style="color:rgb(28, 31, 35);">COUNT(*) ≈ COUNT(1) > COUNT(主键字段) > COUNT(字段名)</font>`



优化思路:

+ MyISAM 引擎把一个表的总行数存在了磁盘上，因此执行 count(*) 的时候会直接返回这个数，效率很高；但是如果是带条件的count，MyISAM也慢。
+ InnoDB 引擎就麻烦了，它执行 count(*) 的时候，需要把数据一行一行地从引擎里面读出来，然后累积计数。

如果说要大幅度提升InnoDB表的count效率，主要的优化思路：自己计数(可以借助于redis这样的数据库进行,但是如果是带条件的count又比较麻烦了)。  



总结: count(*)和count(1)的效率是最高的, 会统计表中所有行的数量, 包括 `null` 值, 而`count(字段名)` 会统计指定字段不为 `null` 的行数

# 自增还是 UUID, 数据库主键的类型该如何选择  
推荐使用自增 ID，不建议使用 UUID。

在使用自增 ID 时，新数据总是插入到 B+ 树的末尾，能够有效**减少页分裂**和**数据移动的频率**，效率更高。

而 UUID 则因为它生成的值是随机的，插入位置不可预测，容易导致频繁的页分裂和数据重排，影响插入性能。

所以自增 ID 更适合作为数据库的主键。

# ----------------结构和事务----------------
# <font style="color:rgb(51, 51, 51);background-color:rgba(247, 255, 254, 0.5);">存储引擎</font>
MySQL 的存储引擎主要包括 InnoDB、MyISAM、Memory、CSV、Archive 等。InnoDB 支持事务、外键和行级锁，适合高并发写操作；MyISAM 不支持事务、外键和行级锁, 只支持表锁，但查询速度快，适合读多写少的场景；Memory 使用内存存储数据，速度快但数据易丢失；CSV 和 Archive 适合存储大批量数据。**MySQL 默认使用 InnoDB。**

MyISAM支持表锁不支持行锁, 而InnoDB都支持

MyISAM不支持事务和外键约束, 而InnoDB都支持

MyISAM读性能好, 而InnoDB写性能好

MyISAM主键索引的数据和索引是分开的, 而InnoDB主键索引的数据和索引是一起的

MyISAM会保存表的行数，而InnoDB不会

对于自增长的字段，InnoDB中必须包含只有该字段的索引，但是在MyISAM表中可以和其他字段一起建立联合索引

InnoDB不支持FULLTEXT类型的索引（5.6之前不支持全文索引）

# mysql中事务的特性
•  原子性（Atomicity）：一个事务中的所有操作，要么全部成功，要么全部失败。如果失败，就会回滚到事务开启前的状态。

•  一致性（Consistency）：<font style="color:#8A8F8D;">(任何事务都使数据库从一个有效的状态转换到另一个有效状态。)</font>事务执行前后，数据必须保持一致性的有效状态。（如A给B转账，不论转账的事务操作是否成功，其两者的存款总额不变）。

•  隔离性（Isolation）：事务之间相互隔离，不会互相干扰。	

•  持久性（Durability）：事务处理结束后，对数据的修改就是永久的，即使系统故障也不会丢失。

# ACID是怎么保证的
原子性-undo log

—致性-其他三个性质

隔离性-锁+MVCC(多版本并发控制)

持久性-redolog



原子性: undolog 用来回滚事务保证事务的原子性, 是一种用于撤销回退的日志, 属于逻辑日志, 在更新一条记录时，会将旧值给记录下来，这样就可以根据这个旧值来回滚<font style="color:#8A8F8D;">(undolog和数据页的刷盘策略是一样的，都需要通过redolog 保证持久化。)</font>

<font style="color:#8A8F8D;">一条记录的每一次更新操作产生的undolog格式都有一个 DB_ROLL_PTR  指针和一个 DB_TRX_ID 事务id:</font>

+ <font style="color:#8A8F8D;">通过 DB_TRX_ID(事务id) 可以知道该记录是被哪个事务修改的;</font>
+ <font style="color:#8A8F8D;">通过 DB_ROLL_PTR(回滚指针) 指针指向上一个undolog, 将这些undolog 串成一个链表，这个链表就被称为版本链；</font>

—致性: 通过其他三个性质共同保证

隔离性: 详见下一节

持久性: mysql修改数据是在buffer pool中进行的, 数据在修改提交后不会立即刷新到磁盘中, 一旦数据库或系统宕机内存中的数据就会丢失, 这时候就需要 redolog 来保证数据库的持久性。redolog 就是重做日志, 用来记录某个数据页发生了什么修改, 它由两个部分组成, 一个是 redolog buffer在内存中, 一个是 redolog 文件在磁盘中。mysql在修改数据后会生成一条 redolog 在 redolog buffer 中, 默认情况下, 在事务提交的时候会将 redolog  buffer中的 redolog 刷新到磁盘, 这个刷盘时机可以通过mysql的一个参数来设置<font style="color:#8A8F8D;">（</font>`<font style="color:#8A8F8D;">innodb_flush_log_at_trx_commit</font>`<font style="color:#8A8F8D;">)(除了默认情况还可以设置为事务提交时不主动刷盘和事务提交时将 redolog 缓存在PageCache, 后者只要操作系统不宕机即使数据库崩溃了也不会丢失数据, 前者性能最好, 此外InnoDB的后台线程每隔1秒也会将 redolog buffer持久化到磁盘。)</font>。redolog 刷盘后更新就算完成了, 但是磁盘上的数据并不一定被修改了, 因为InnoDB使用了WAL技术, 也就是先写日志, 先将更新操作记录到日志里面, 然后在合适的时间再写到磁盘上。<font style="color:#8A8F8D;">(为什么要这么干, 不直接将数据写到磁盘呢, 因为写入 redo log 的方式是顺序写，而直接写入磁盘是随机写, 顺序写的效率要大于随机写。)</font>这样即便系统宕机内存数据丢失了也还能够通过 redolog 来恢复数据来实现持久性。<font style="color:#8A8F8D;">( redolog 是以循环写的方式工作的，也就是说 redolog 是可以写满的, 此时mysql会阻塞住来将buffer pool中的脏页刷新到磁盘中来为 redolog 腾出空间)</font>

[https://xiaolincoding.com/mysql/log/how_update.html#%E4%B8%BA%E4%BB%80%E4%B9%88%E9%9C%80%E8%A6%81-redo-log](https://xiaolincoding.com/mysql/log/how_update.html#%E4%B8%BA%E4%BB%80%E4%B9%88%E9%9C%80%E8%A6%81-redo-log)

<details class="lake-collapse"><summary id="u049ae53e"><span class="ne-text">格式化后的</span></summary><p id="u1134c403" class="ne-p"><span class="ne-text">在 MySQL 中，数据的修改首先发生在 </span><strong><span class="ne-text">Buffer Pool（内存）</span></strong><span class="ne-text"> 中，</span><strong><span class="ne-text">并不会立即刷新到磁盘</span></strong><span class="ne-text">。因此，如果数据库发生宕机，内存中的修改就会丢失。为了保证数据的</span><strong><span class="ne-text">持久性（Durability）</span></strong><span class="ne-text">，MySQL 引入了 </span><strong><span class="ne-text">Redo Log（重做日志）</span></strong><span class="ne-text">。</span></p><h3 id="091971f1"><span class="ne-text">Redo Log 的组成</span></h3><p id="u223c9c20" class="ne-p"><span class="ne-text">Redo Log 由两个部分组成：</span></p><ul class="ne-ul"><li id="u95ed789c" data-lake-index-type="0"><strong><span class="ne-text">Redo Log Buffer</span></strong><span class="ne-text">：内存中的缓冲区；</span></li><li id="ua5c98bdb" data-lake-index-type="0"><strong><span class="ne-text">Redo Log 文件</span></strong><span class="ne-text">：磁盘上的日志文件。</span></li></ul><p id="ufae49b28" class="ne-p"><span class="ne-text">当数据被修改时，InnoDB 会生成一条 Redo Log，并首先写入 Redo Log Buffer。默认情况下，在事务提交时，这条日志会被刷新（flush）到磁盘上的 Redo Log 文件中，从而实现持久化。</span></p><p id="u39209a81" class="ne-p"><span class="ne-text">这个行为可以通过参数进行配置：</span></p><ul class="ne-ul"><li id="ub98fa8a6" data-lake-index-type="0"><strong><span class="ne-text">立即刷盘（默认）</span></strong><span class="ne-text">：事务提交时同步将日志写入磁盘；</span></li><li id="u72f528f6" data-lake-index-type="0"><strong><span class="ne-text">延迟刷盘</span></strong><span class="ne-text">：事务提交时只写入内存，不立即持久化，性能更好但风险更高；</span></li><li id="u24f13b21" data-lake-index-type="0"><strong><span class="ne-text">写入 Page Cache</span></strong><span class="ne-text">：事务提交时写入操作系统的页缓存，只要操作系统不宕机，就不会丢失数据，兼顾性能与安全。</span></li></ul><p id="u6a148453" class="ne-p"><span class="ne-text">此外，InnoDB 的后台线程每隔 1 秒也会将 Redo Log Buffer 中的内容刷新到磁盘，作为补充保障。</span></p><h3 id="e6dc1498"><span class="ne-text">WAL 技术（Write-Ahead Logging）</span></h3><p id="u5c3dd954" class="ne-p"><span class="ne-text">InnoDB 使用了 WAL 技术：</span><strong><span class="ne-text">先写日志，再写磁盘</span></strong><span class="ne-text">。也就是说，数据更新会优先写入 Redo Log，而不是直接写入数据页。这样做的原因是：</span></p><ul class="ne-ul"><li id="u6512f7a6" data-lake-index-type="0"><strong><span class="ne-text">写 Redo Log 是顺序写</span></strong><span class="ne-text">，而直接修改数据页是随机写；</span></li><li id="ue9349dfb" data-lake-index-type="0"><strong><span class="ne-text">顺序写的性能远优于随机写</span></strong><span class="ne-text">，尤其在磁盘 IO 上差距明显。</span></li></ul><p id="u95f31a5b" class="ne-p"><span class="ne-text">这样，即使系统突然宕机，只要 Redo Log 已经写入磁盘，MySQL 就可以通过 Redo Log 来</span><strong><span class="ne-text">恢复丢失的内存数据</span></strong><span class="ne-text">。</span></p><h3 id="52fe04df"><span class="ne-text">Redo Log 的循环写机制</span></h3><p id="u0e54414b" class="ne-p"><span class="ne-text">Redo Log 使用 </span><strong><span class="ne-text">循环写入</span></strong><span class="ne-text"> 的方式。如果 Redo Log 写满了，MySQL 会</span><strong><span class="ne-text">阻塞写操作</span></strong><span class="ne-text">，并强制将 Buffer Pool 中的</span><strong><span class="ne-text">脏页（已修改但未落盘的页）刷新到磁盘</span></strong><span class="ne-text">，以释放 Redo Log 空间，确保新的日志可以写入。</span></p></details>
# 什么是脏读、幻读、不可重复读？
+ 脏读：读到了其他事务还没有提交的数据。
+ 脏写：当前事务修改了另一个事务未提交的数据，如果另一个事务回滚了就会导致当前事务的修改无效。在读已提交隔离级别下会解决这个问题
+ 不可重复读：在一个事务中，第一次读取某行数据后，有其他事务对数据进行了修改（UPDATE、DELETE)，导致第二次读取的结果不同。（**多次读取数据，结果不一样）**
+ 幻读：事务在两次范围查询期间，有另外一个事务在这个范围内新增了记录(INSERT)，导致范围查询的结果记录数不一致。**（多次读取数据，数据量不一样）**

[https://dev.mysql.com/doc/refman/8.0/en/innodb-next-key-locking.html?utm_source=chatgpt.com](https://dev.mysql.com/doc/refman/8.0/en/innodb-next-key-locking.html?utm_source=chatgpt.com)

> MySQL 文档是怎么定义幻读（Phantom Read）的:
>
> The so-called phantom problem occurs within a transaction when the same query produces different sets of rows at different times. For example, if a SELECT is executed twice, but returns a row the second time that was not returned the first time, the row is a “phantom” row.
>
> **翻译**: 当同一个查询在不同的时间产生不同的结果集时，事务中就会出现所谓的幻象问题。例如，如果 SELECT 执行了两次，但第二次返回了第一次没有返回的行，则该行是“幻像”行。
>

<details class="lake-collapse"><summary id="u76c5e9fd"><span class="ne-text">全文翻译</span></summary><p id="u67bc0ffe" class="ne-p"><span class="ne-text">当同一个查询在不同的时间产生不同的结果集时，事务中就会出现所谓的幻象问题。例如，如果 SELECT 执行了两次，但第二次返回了第一次没有返回的行，则该行是“幻像”行。</span></p><p id="u2de54395" class="ne-p"><span class="ne-text">假设子表的id列上有一个索引，并且你想读取并锁定表中所有标识符值大于100的行，以便后续更新所选行中的某些列：</span></p><pre data-language="sql" id="vxDTg" class="ne-codeblock language-sql"><code>SELECT * FROM child WHERE id &gt; 100 FOR UPDATE;</code></pre><p id="u985c012d" class="ne-p"><span class="ne-text">该查询从id大于100的第一条记录开始扫描索引。假设表中包含id值为90和102的行。如果在扫描范围内对索引记录设置的锁未锁定间隙中插入的内容（在这种情况下，是90和102之间的间隙），另一个会话可以向表中插入id为101的新行。如果在同一个事务中执行相同的SELECT查询，你会在查询返回的结果集中看到id为101的新行（“幻像”）。如果我们将一组行视为一个数据项，新的幻像子行会违反事务的隔离原则，即事务应能够运行，使其读取的数据在事务期间不发生变化。</span></p><p id="ud3317a2d" class="ne-p"><span class="ne-text">为防止幻读，InnoDB使用一种称为next-key锁的算法，该算法将索引行锁与间隙锁相结合。InnoDB执行行级锁的方式是，当它搜索或扫描表索引时，会对遇到的索引记录设置共享锁或排他锁。因此，行级锁实际上是索引记录锁。此外，索引记录上的next-key锁还会影响该索引记录之前的“间隙”。也就是说，next-key锁是索引记录锁加上该索引记录之前间隙的间隙锁。如果一个会话在索引中的记录R上具有共享锁或排他锁，另一个会话不能在索引顺序中紧接R之前的间隙中插入新的索引记录。</span></p><p id="uf0c9850d" class="ne-p"><span class="ne-text">当InnoDB扫描索引时，它还可以锁定索引中最后一条记录之后的间隙。在前述示例中就会发生这种情况：为防止在id大于100的表中进行任何插入，InnoDB设置的锁包括对id值102之后间隙的锁。</span></p><p id="u6dc5f3e8" class="ne-p"><span class="ne-text">你可以使用next-key锁在应用程序中实现唯一性检查：如果以共享模式读取数据，并且没有看到要插入行的重复项，那么可以安全地插入该行，并且知道在读取期间对该行后继设置的next-key锁可防止其他会话同时插入该行的重复项。因此，next-key锁使你能够“锁定”表中某事物的不存在性。</span></p><p id="uaedefde8" class="ne-p"><span class="ne-text">如第17.7.1节“InnoDB锁定”中所讨论的，可以禁用间隙锁。这可能会导致幻读问题，因为当禁用间隙锁时，其他会话可以在间隙中插入新行。</span></p></details>
# mysql中事务的隔离级别
**读未提交**（read uncommitted），指一个事务可以读到其他事务未提交的数据；

**读已提交**（read committed），指一个事务提交之后，它做的变更才能被其他事务看到；

**可重复读**（repeatable read），指一个事务在整个执行过程中看到的数据，跟这个事务启动时看到的数据是一致的，MySQL InnoDB 引擎的默认隔离级别；

**串行化**（serializable ）；会对记录加上**读写锁**，在多个事务对这条记录进行读写操作时，如果发生了读写冲突的时候，后访问的事务必须等前一个事务执行完成，才能继续执行；(简单说就是利用读写锁让事务串行执行)

+ 在「读未提交」隔离级别下，可能发生脏读、不可重复读和幻读现象；
+ 在「读已提交」隔离级别下，可能发生不可重复读和幻读现象，但是不可能发生脏读现象；
+ 在「可重复读」隔离级别下，可能发生幻读现象，但是不可能脏读和不可重复读现象；
+ 在「串行化」隔离级别下，脏读、不可重复读和幻读现象都不可能会发生。

# MVCC是什么
MVCC 全称 Multi-Version Concurrency Control，多版本并发控制。通过快照读的方式来解决读写并发的问题，解决了脏读和不可重复读以及部分的幻读，在可重复读中可以通过MVCC+锁来进一步解决幻读的问题。

**MVCC的原理是通过 InnoDB 表的隐藏字段、UndoLog 版本链、ReadView来实现的。**



**快照读**：读取数据的历史可见版本(版本访问权限由RedView控制,通过Readview来读取undolog版本链中满足一定条件的数据)，不加锁是非阻塞读，像我们常用的普通的SELECT语句在不加锁情况下就是快照读。在MySQL 中，只有READ COMMITTED 和 REPEATABLE READ这两种事务隔离级别才会使用快照读。快照读是实现MVCC的基础。

# Readview是什么
ReadView（读视图）是MVCC机制中快照读 SQL 执行时读取数据的依据，通过记录一系列未提交事务的id，控制快照读时可访问的数据版本

# 读已提交和可重复读的原理是什么?	
**读已提交:**

在 RC 隔离级别下, 读取数据在不加锁时执行的是快照读, 每次执行快照读的时候都会生成一个ReadView, Read View 在创建时会生成四个重要的字段, m_ids: 当前活跃的事务ID集合, min_trx_id: 最小活跃事务ID, max_trx_id: 预分配事务ID<font style="color:#8A8F8D;">(当前最大事务ID+1（因为事务ID是自增的）),</font> creator_trx_id: ReadView创建者的事务ID,  需要注意的是每条记录都有两条隐藏字段, 分别是DB_TRX_ID(trx_id)和DB_ROLL_PTR(roll_ptr), roll_ptr, 通过 trx_id 可以知道该记录是被哪个事务修改的, 通过 roll_ptr 指针可以将 undo log 串成一个链表，这个链表就被称为版本链, 

ReadView 生成完后, 就会去遍历 undolog 版本链中的每一条记录, 将 trx_id 与 ReadView 中的字段进行比较, 如果 trx_id 等于 creator_trx_id, 说明数据是当前这个事务更改的, 可以访问, <font style="color:#8A8F8D;">如果 trx_id < min_trx_id, 说明数据已经提交了, 可以访问, 如果 trx_id >= max_trx_id, 说明该事务是在 ReadView 生成后才开启, 不可以访问, 如果 trx_id 在 min_trx_id 和 max_trx_id之间, 那就要看 trx_id 在不在 m_ids 也就是当前活跃的事务ID集合中, 如果不在, 说明数据已经提交, 可以访问, 否则就不能访问, 这样就实现了读已提交</font>（简单来说就是遍历undolog版本链上的记录，将记录的事务id与readview中的事务id进行比较，比最大的还要大就不要，比最小的还要小就要，中间的就去判断还在不在活跃的事务id列表里面，如果还在，那就说明没提交，那就不要，反之就要）

**可重复读:**

可重复读的原理和读已提交类似, 唯一区别就是 ReadView 只在第一次执行快照读的时候生成(当前读不会), 后续都是复用该

ReadView，那么既然ReadView都一样，那么最终快照读返回的结果也是一样的。需要注意的是如果是通过`start transaction with consistent snapshot`来开启事务会立即生成 ReadView

**总结: **

这两个隔离级别实现是通过「事务的 Read View 里的字段」和「记录中的隐藏字段」的比对，来控制并发事务访问同一个记录时的行为，这就叫 MVCC（多版本并发控制）。

在读已提交和可重复读隔离级别中，普通的 select 语句就是基于 MVCC 实现的快照读，也就是不会加锁的。而 select .. for update 语句就不是快照读了，而是当前读了，也就是每次读都是拿到最新版本的数据，但是它会对读到的记录加上 next-key lock 锁。

# mysql的可重复读是怎么实现的？
可重复读的原理和读已提交类似, 唯一区别就是 ReadView 只在第一次执行快照读的时候生成, 后续都是复用该

ReadView，那么既然ReadView都一样，那么最终快照读返回的结果也是一样的。需要注意的是如果是通过`start transaction with consistent snapshot`来开启事务会立即生成 ReadView

读已提交原理详见上节

# 可重复读解决了幻读问题吗？
MySQL InnoDB 引擎的默认隔离级别虽然是「可重复读」，但是它很大程度上避免幻读现象（并不是完全解决了），解决的方案有两种：

+ 针对快照读（普通 select 语句），是**通过 MVCC 方式解决了部分幻读**，因为可重复读隔离级别下，事务执行过程中看到的数据，一直跟这个事务启动时看到的数据是一致的，即使中途有其他事务插入了一条数据，是查询不出来这条数据的，所以就很好了避免幻读问题。快照读也有出现幻读的情况, 比如事务A查询一条记录返回不存在, 此时事务B去插入这条不存在的记录, 然后事务A再去更新这条记录, 最后事务A就能使用快照读的方式查出这条记录了, 这就出现了幻读, 原因是事务A进行更新后这条记录对应 undolog 版本链上的记录的 trx_id 变成了自己的, 所以就可以访问这条记录, 最终造成了幻读。
+ 针对当前读（select ... for update / lock in share mode 等语句），是**通过 next-key lock（记录锁+间隙锁）方式解决了幻读**，因为当执行 select ... for update 语句的时候，会加上 next-key lock，如果有其他事务在 next-key lock 锁范围内插入了一条记录，那么这个插入语句就会被阻塞，无法成功插入，所以就很好了避免幻读问题。

**当前读是如何造成幻读的？**

MySQL 里除了普通查询是快照读，其他都是当前读，比如 update、insert、delete，这些语句执行前都会查询最新版本的数据，然后再做进一步的操作, <font style="color:#8A8F8D;">(这很好理解，假设你要 update 一个记录，另一个事务已经 delete 这条记录并且提交事务了，这样不是会产生冲突吗，所以 update 的时候肯定要知道最新的数据。)</font>并不会使用 ReadView, 所有容易造成幻读

Innodb 引擎为了解决「可重复读」隔离级别使用「当前读」而造成的幻读问题，就引出了间隙锁。需要注意的是「读已提交」隔离级别是没有间隙锁的。

# innodb如何管理page页
Innodb 存储引擎设计了一个**<font style="color:rgba(48,79,254,1);">缓冲池 (</font>**_**<font style="color:rgba(200,73,255,1);">Buffer Pool</font>**_**<font style="color:rgba(48,79,254,1);">)</font>** ，来提高数据库的读写性能。

Buffer Pool 以页为单位缓冲数据，可以通过 `<font style="color:blue;">innodb_buffer_pool_size</font>` 参数调整缓冲池的大小，默认是128 M。

为了更好的管理这些在 Buffer Pool 中的缓存页，InnoDB 为每一个缓存页都创建了一个控制块，控制块信息包括「缓存页的表空间、页号、缓存页地址、链表节点」等等。

Innodb 通过三种链表来管理缓冲页(链表的节点都是控制块)：

+ Free List（空闲页链表），管理空闲页，有了 Free 链表后，每当需要从磁盘中加载一个页到 Buffer Pool 中时，就从 Free链表中取一个空闲的缓存页，并且把该缓存页对应的控制块的信息填上，然后把该缓存页对应的控制块从 Free 链表中移除。
+ Flush List（脏页链表），管理脏页，有了 Flush 链表后，后台线程就可以遍历 Flush 链表，将脏页写入到磁盘。 
+ LRU List，管理脏页+干净页，将最近且经常查询的数据缓存在其中，而不常查询的数据就淘汰出去。

InnoDB 对 LRU 做了一些优化，我们熟悉的 LRU 算法通常是将最近查询的数据放到 LRU 链表的头部，而InnoDB 做 2 点优化：

+ 将 LRU 链表分为 **<font style="color:rgba(48,79,254,1);">young</font>**<font style="color:rgba(48,79,254,1);"> 和 </font>**<font style="color:rgba(48,79,254,1);">old</font>**<font style="color:rgba(48,79,254,1);"> 两个区域</font>，加入缓冲池的页，优先插入 old 区域；页被访问时，才进入young 区域，目的是为了解决**预读失效**的问题。
+ 当「页被访问」且「old 区域停留时间超过 `innodb_old_blocks_time` 阈值（默认为1秒）」时，才会将页插入到 young 区域，否则还是插入到 old 区域，目的是为了解决批量数据访问，大量热数据淘汰的问题(**Buffer Pool 污染**)。

可以通过调整 `innodb_old_blocks_pct` 参数，设置 young 区域和 old 区域比例。

> 什么是预读失效？
>
> 程序是有空间局部性的，靠近当前被访问数据的数据，在未来很大概率会被访问到。所以，MySQL 在加载数据页时，会提前把它相邻的数据页一并加载进来，目的是为了减少磁盘 IO。但是可能这些被提前加载进来的数据页，并没有被访问，相当于这个预读是白做了，这个就是预读失效。
>

**脏页什么时候会被刷入磁盘？**

引入了 Buffer Pool 后，当修改数据时，首先是修改 Buffer Pool 中数据所在的页，然后将其页设置为脏页，但是磁盘中还是原数据。

因此，脏页需要被刷入磁盘，保证缓存和磁盘数据一致，但是若每次修改数据都刷入磁盘，则性能会很差，因此一般都会在一定时机进行批量刷盘。

可能大家担心，如果在脏页还没有来得及刷入到磁盘时，MySQL 宕机了，不就丢失数据了吗？

这个不用担心，InnoDB 的更新操作采用的是 Write Ahead Log 策略，即先写日志，再写入磁盘，通过 redo log 日志让 MySQL 拥有了崩溃恢复能力。

下面几种情况会触发脏页的刷新：

**当 redo log 日志满了**的情况下，会主动触发脏页刷新到磁盘；

**Buffer Pool 空间不足**时，需要将一部分数据页淘汰掉，如果淘汰的是脏页，需要先将脏页同步到磁盘；

MySQL 认为**空闲时**，**后台线程会定期**将适量的脏页刷入到磁盘；

**MySQL 正常关闭之前**，会把所有的脏页刷入到磁盘；

在我们开启了慢 SQL 监控后，如果你发现「偶尔」会出现一些用时稍长的 SQL，这可能是因为脏页在刷新到磁盘时可能会给数据库带来性能开销，导致数据库操作抖动。

如果间断出现这种现象，就需要调大 Buffer Pool 空间或 redo log 日志的大小。

# 什么是buffer pool
buffer pool是 Innodb 存储引擎为了提高数据库的读写性能设计的一个**<font style="color:rgba(48,79,254,1);">缓冲池 (</font>**_**<font style="color:rgba(200,73,255,1);">Buffer Pool</font>**_**<font style="color:rgba(48,79,254,1);">)</font>** 。Buffer Pool 以页为单位缓冲数据，

+ 当读取数据时，如果数据存在于 Buffer Pool 中，客户端就会直接读取 Buffer Pool 中的数据，否则再去磁盘中读取。
+ 当修改数据时，如果数据存在于 Buffer Pool 中，那直接修改 Buffer Pool 中数据所在的页，然后将其页设置为脏页（该页的内存数据和磁盘上的数据已经不一致），为了减少磁盘I/O，不会立即将脏页写入磁盘，后续由后台线程选择一个合适的时机将脏页写入到磁盘

# change buffer
在 MySQL（准确来说是 InnoDB 存储引擎）中，**Change Buffer（变更缓冲区）** 是一个用于**优化对非聚簇索引的DML操作性能**的机制。它的主要作用是将某些**对二级索引页的更改操作延迟执行**，从而减少磁盘 I/O。

**Change Buffer** 是 InnoDB Buffer Pool 中的一个部分，用来缓存对**二级索引**页（也就是非聚簇索引页）的以下三种操作：

+ 插入（Insert Buffering）
+ 删除（Delete Buffering）
+ 更新（Purge Buffering）

这些操作如果目标页当前**不在内存中**，就会被先记录在 Change Buffer 中，而不是立即去读取磁盘中的页并更新。

# 如何判断一个页是否在buffer pool中缓存
MySQL 使用了一个哈希表来跟踪哪些数据页已经在 Buffer Pool 中被缓存。这个哈希表称为 "页哈希索引" 或 "页目录"。

每当一个数据页被加载到 Buffer Pool 中，MySQL 就会在哈希表中添加一项，其中键（key）是由表空间 ID 和数据页号组成，值（value）是该数据页在 Buffer Pool 中的地址。

因此，当需要访问一个数据页时，MySQL 可以通过查找此哈希表来快速确定这个页面是否已经在 Buffer Pool 中。如果哈希表中有对应的项，那么就表示这个数据页已经被缓存，可以直接从内存中读取；否则，需要从硬盘上的文件中加载这个数据页。

通过使用此哈希表，MySQL 可以避免不必要的硬盘 I/O 操作，从而大大提高数据访问的效率。

同时，当一个数据页从 Buffer Pool 中被移除时，对应的哈希表项也会被删除，以保持哈希表和 Buffer Pool 的一致性。

# -------------------索引--------------------
# 索引有哪几种类型？使用索引一定可以提升效率吗？
**索引（Index）** 是一种用于加快查询速度的数据结构。  

**索引分类:**

按 **索引的存储形式** 分类：

+ 聚簇索引（Clustered Index）(只能有一个)(主键索引)
+ 二级索引（Non-clustered Index）(非聚簇索引或辅助索引)

按 **字段特性** 分类：

+ 主键索引
+ 唯一索引
+ 普通索引
+ 前缀索引
+ 全文索引
+ 空间索引

按 **字段个数** 分类：

+ 单列索引
+ 联合索引

按 **数据结构** 分类：

+ B+树索引
+ 哈希索引(MEMORY)

[https://blog.csdn.net/w1014074794/article/details/105617884](https://blog.csdn.net/w1014074794/article/details/105617884)

+ 倒排索引（即全文索引 Full-Text）
+ R-树索引（多维空间树）(MyISAM)

**聚簇索引选取规则:**

+ 如果存在主键，主键索引就是聚集索引。
+ 如果不存在主键，将使用第一个唯一（UNIQUE）索引作为聚集索引。 
+ 如果表没有主键，或没有合适的唯一索引，则InnoDB会自动生成一个rowid作为隐藏的聚集索引。

**使用索引不一定可以提升效率**

**不需要创建索引的情况:**

WHERE 条件，GROUP BY，ORDER BY 里**用不到的字段**，索引的价值是快速定位，如果起不到定位的字段通常是不需要创建索引的，因为索引是会占用物理空间的。

字段中存在**大量重复数据**，不需要创建索引，比如性别字段，只有男女，如果数据库表中，男女的记录分布均匀，那么无论搜索哪个值都可能得到一半的数据。在这些情况下，还不如不要索引，因为 MySQL 还有一个查询优化器，查询优化器发现某个值出现在表的数据行中的百分比很高的时候(也就是**选择性低**)，它一般会忽略索引，进行全表扫描。(**直接走主键索引效率更高**)

表数据太少的时候，不需要创建索引；

**经常更新的字段**不用创建索引，比如不要对电商项目的用户余额建立索引，因为索引字段频繁修改，由于要维护 B+Tree的有序性，那么就需要频繁的重建索引，这个过程是会影响数据库性能的。

# 聚簇索引与非聚簇索引区别
**聚簇索引**的 B+Tree 的叶子节点存放的是**整行的完整记录**；

**非聚簇索引**的 B+Tree 的叶子节点存放的是**主键值**和**索引列的值**。在二级索引的 B+Tree 就能查询到结果的过程就叫作「**覆盖索引**」，也就是只需要查一个 B+Tree 就能找到数据。如果获取不到所有需要的值就会去获取主键值，然后再通过主键索引中的 B+Tree 树查询到对应的叶子节点，然后获取整行数据。这个过程叫「**回表**」，也就是说要查两个 B+Tree 才能查到数据。

**聚簇索引选取规则:**

+ 如果存在主键，主键索引就是聚集索引。
+ 如果不存在主键，将使用第一个唯一（UNIQUE）索引作为聚集索引。 
+ 如果表没有主键，或没有合适的唯一索引，则InnoDB会自动生成一个rowid作为隐藏的聚集索引。

# B树和B+树的区别
B树和B+树都是平衡多路搜索树(平衡指叶子节点高度相同多路指一个节点可以有多个子节点，节点中可以存储多个键，相邻两个键中间会挂一个子节点，子节点中所有键的值都在上面的两个键之间，同一个节点的所有键是有序排列的)

B+树的**非叶子节点只存索引键**, 将所有的数据都存储在叶子节点, 而B树的每个节点既包含索引键又包含数据, 所以 B+树 的单个节点的数据量更小，在相同的磁盘 I/O 次数下，就能查询更多的节点。

B+树的所有叶子节点之间都通过指针连接构成一个双向链表, **范围查询和顺序遍历的效率比较高**, 而b树的范围查询和顺序遍历的效率很低

B+树的**更新操作主要影响叶子节点**, 而B树的更新操作可能会影响整个树

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1749998809760-576a7a6f-45ba-4c36-b14b-cbda8f0fdde7.png)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1749998754234-1fa1138a-654a-4c73-9818-edeac5e76a3e.png)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1745082142891-28734f8e-4e8f-4613-bfb0-e2d60d4dd621.png?x-oss-process=image%2Fformat%2Cwebp)

# B树和其他树的区别
**B+Tree vs 二叉树**

对于有 N 个叶子节点的 B+Tree，其搜索复杂度为 `O(logdN)`，其中 d 表示节点允许的最大子节点个数为 d 个。

在实际的应用当中，d 值是大于 100 的，这样就保证了，即使数据达到千万级别时，B+Tree 的高度依然维持在 3~4 层左右，也就是说一次数据查询操作只需要做 34 次的磁盘 I/O 操作就能查询到目标数据。

而二叉树的每个父节点的子节点个数只能是 2 个，意味着其搜索复杂度为 `O(logN)`，这已经比 B+Tree 高出不少，因此二叉树检索到目标数据所经历的磁盘 I/O 次数要更多。

**B+Tree vs Hash**

Hash 在做等值查询的时候效率贼快，搜索复杂度为 O(1)。

但是 Hash 表不适合做范围查询，它更适合做等值的查询，这也是 B+Tree 索引要比 Hash 表索引有着更广泛的适用场景的原因。

# 为什么MySQL选择使用B+树作为索引结构？
**首先**b+树查找的时间复杂度是O(log n)比较低, **然后**b+树的非叶子节点只存索引键, 将所有的数据都存储在叶子节点, 那么一个非叶子节点就可以存很多个索引键, **因此**b+树的层数比较低, 可以降低磁盘io(每读取一页就会进行一次磁盘io)。**并且**所有叶子节点之间都通过指针连接, 范围查询和顺序遍历的效率比较高

**每层key的数量太多不会导致检索效率低吗?**

虽然每层 B+ 树的 key 数量很多（比如一个节点可能有上百个 key），但查找并不会慢，因为：

在每个节点内部查找 key 是通过二分查找（或更高效的算法）完成的，而不是线性扫描。	

# MySQL三层B+树能存多少数据？
b+树中的一个节点就是一个页, 大小默认为16KB, 存主键索引的话如果是`bigint`可以存1170个索引键,也就是1170个主键, 这样的话一个节点可以有1170个子节点, 假如一条数据按1KB算, 一个叶子节点可以放下16条数据, 那么三层b+树就可以存下大约2000万行数据。

# 最左前缀法则
最左前缀法则指的是通过联合索引查询时查询要从索引的最左边的列(最左边的列必须存在)开始， 并且不能跳过索引中间的列。如果跳跃某一列，<font style="color:#DF2A3F;">这一列后面的字段索引将会失效。</font>

联合索引的非叶子节点用多个字段的值作为 B+Tree 的 key 值。比如说有联合索引(a, b), 当在联合索引查询数据时，先按 a 字段比较，在 a 相同的情况下再按 b 字段比较。

也就是说，联合索引查询的 B+Tree 叶子节点上的链表是先按 a 进行排序，然后在 a 相同的情况再按 b 字段排序。

因此，使用联合索引时，存在最左匹配原则，也就是按照最左优先的方式进行索引的匹配。在使用联合索引进行查询的时候，如果不遵循「最左匹配原则」，联合索引会失效，这样就无法利用到索引快速查询的特性了。

> 比如，如果创建了一个 (a, b, c) 联合索引，如果查询条件是以下这几种，就可以匹配上联合索引：
>
> + where a=1；
> + where a=1 and b=2 and c=3；
> + where a=1 and b=2；
>
> 需要注意的是，因为有查询优化器，所以 a 字段在 where 子句的顺序并不重要。
>
> 但是，如果查询条件是以下这几种，因为不符合最左匹配原则，所以就无法匹配上联合索引，联合索引就会失效:
>
> + where b=2；
> + where c=3；
> + where b=2 and c=3；
>

符合联合索引的最左匹配原则的前提下，在遇到范围查询（如 >、<）的时候，就会停止匹配，也就是范围查询的字段可以用到联合索引，但是在范围查询字段的后面的字段无法用到联合索引。注意，对于 >=、<=、BETWEEN、like 前缀匹配的范围查询，并不会停止匹配。

比如说where a >= 1 and b = 2, MySQL会去扫描第一个符合a = 1, b = 2的记录然后向右遍历, 而对于where a > 1 and b = 2, MySQL只会去扫描第一个符合a > 1的记录然后向右遍历, 这时候 b 就用不上索引

# 为什么LIKE以%开头会失效（覆盖索引不会失效）
 当 `LIKE` 以 `%` 开头时，MySQL 无法利用 B+ 树索引的有序性快速定位，只能全表扫描，因此索引失效；但如果查询的字段刚好全部包含在索引中（也就是覆盖索引），即使不能加速查找，也可以避免回表，提高查询效率，所以还是使用了索引。  

# explain
**explain 主要用来 SQL 分析，它主要的属性详解如下：**

+ id：查询的执行顺序的标识符，值越大优先级越高。简单查询的 id 通常为 1，复杂查询（如包含子查询或 UNION）的 id 会有多个。
+ select_type（**重要**）：查询的类型，如 SIMPLE（简单查询）、PRIMARY（主查询）、SUBQUERY（子查询）等。 
+ table：查询的数据表。 
+ type（**重要**）：访问类型，如 ALL（全表扫描）、index（索引扫描）、range（范围扫描）等。一般来说，性能从好到差的顺序是：const > eq_ref > ref > range > index > ALL。 
+ possible_keys：可能用到的索引。 
+ key（**重要**）：实际用到的索引。 
+ key_len：用到索引的长度。 
+ ref：显示索引的哪一列被使用。 
+ rows（**重要**）：估计要扫描的行数，值越小越好。 
+ filtered：显示查询条件过滤掉的行的百分比。一个高百分比表示查询条件的选择性好。 
+ Extra（**重要**）：额外信息，如 Using index（表示使用覆盖索引）、Using where（表示使用 WHERE 条件进行过滤）、Using temporary（表示使用临时表）、Using filesort（表示使用了文件排序）、Using index condition(使用了索引下推优化)、Backward index scan(反向索引扫描)。

**type 详解：**

+ system：表示查询的表只有一行（系统表）。这是一个特殊的情况，不常见。 
+ const：表示查询的表最多只有一行匹配结果。这通常发生在查询条件是主键或唯一索引，并且是常量比较。 
+ eq_ref：表示在连接操作中使用了主键或唯一索引，并且连接条件是基于这些索引的等值条件。 
+ ref：表示采用了非唯一索引或者前缀索引，返回数据返回可能是多条。
+ range：表示 MySQL 会扫描表的一部分，而不是全部行。范围扫描通常出现在使用索引的范围查询中（如 BETWEEN、>、<、>=、<= ）。 
+ index：表示 MySQL 扫描索引中的所有行，而不是表中的所有行。即使索引的值覆盖查询，也需要扫描整个索引。 
+ all（性能最差）：表示 MySQL 需要扫描表中的所有行，即全表扫描。通常出现在没有索引的查询条件中。

```sql
CREATE TABLE `t2` (
  `id` INT(11),
  `a` varchar(64) NOT NULL,
  `b` varchar(64) NOT NULL,
  `c` varchar(64) NOT NULL,
  `d` varchar(64) NOT NULL,
  `f` varchar(64) DEFAULT NULL,
  PRIMARY KEY(`id`),
  UNIQUE KEY `f` (`f`),
  KEY `idx_abc` (`a`,`b`,`c`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1
```

[更详细的](https://k1dy9adkxea.feishu.cn/wiki/KUxQwbTJ8i8UbmkvirscmCg6nxc)

**Extra 详解**

+ Using where：当我们使用全表扫描来执行对某个表的查询，并且该语句的 WHERE 子句中有针对该表的搜索条件时，在Extra 列中会提示上述额外信息。当使用索引访问来执行对某个表的查询，并且该语句的 WHERE 子句中有除了该索引包含的列之外的其他搜索条件时，在 Extra 列中也会提示上述额外信息。
+ Using filesort: 表示当查询语句中包含 order by 或 group by 操作时，MySQL 将使用文件排序而不是索引排序，这通常发生在无法使用索引来进行排序时。
+ Using index：所需数据只需在索引即可全部获得，不需要再到表中取数据，也就是使用了覆盖索引，避免了回表。
+ Using index condition：表示查询在索引上执行了部分条件过滤。这通常和索引下推有关。
+ Using where; Using index：查询的列被索引覆盖，并且where筛选条件是索引列之一，但不是索引的前导列，或者where筛选条件是索引列前导列的一个范围
    - `explain select a from t2 where b = "ni";`  索引覆盖，但是不符合最左前缀  
    - `explain select b from t2 where a in ('a','d','sd');`  索引覆盖，但是前导列是个范围
+ Using join buffer (Block Nested Loop)：在连接查询执行过程中，当被驱动表不能有效的利用索引加快访问速度，MySQL一般会为其分配一块名叫 join buffer 的内存块来加快查询速度，也就是我们所讲的基于块的嵌套循环算法。
+ Using temporary：表示 MySQL 创建了临时表来存储查询结果。这通常是在排序或分组时发生的。

# 自适应哈希索引
自适应哈希索引不是用户手动创建的索引，而是 InnoDB 在运行过程中**自动创建和维护**的一种基于哈希的加速访问机制。

简单来说，当 InnoDB 发现对某些 **B+ 树索引的访问模式具有“热点”特征（重复访问特定的页或范围）**，它就会基于这些 B+ 树页内容创建哈希索引，从而实现更快的查找（O(1) 时间复杂度，相比于 B+ 树的 O(log n)）。

# 索引下推
现在我们知道，对于联合索引（a, b），在执行 `select * from table where a > 1 and b = 2` 语句的时候，只有 a 字段能用到索引，那在联合索引的 B+Tree 找到第一个满足条件的主键值（ID 为 2）后，还需要判断其他条件是否满足（看 b 是否等于 2），那是在联合索引里判断？还是回主键索引去判断呢？

在 MySQL 5.6 之前，只能根据 id 一个个回表，到「主键索引」上找出数据行，再对比 b 字段值。

而 MySQL 5.6 引入的**<font style="color:rgba(48,79,254,1);">索引下推优化</font>**（index condition pushdown)， **<font style="color:rgba(48,79,254,1);">可以在联合索引遍历过程中，对联合索引中包含的字段先做判断，直接过滤掉不满足条件的记录，减少回表次数。</font>**

传统的查询流程是，存储引擎通过联合索引定位到符合最左前缀条件的主键 ID；回表读取完整数据行并返回给服务层；服务层对所有返回的行根据 WHERE 条件进行过滤。

有了索引下推优化后，存储引擎在索引层直接过滤可下推的条件，仅对符合索引条件的记录回表读取数据，再返回给服务层进行剩余条件过滤。

当你的查询语句的执行计划里，出现了 Extra 为 Using index condition，那么说明使用了索引下推的优化。

一句话总结: 索引下推是mysql在遍历联合索引时为了减少回表次数所做的优化

# 覆盖索引
MySQL 的覆盖索引（CoveringIndex）是指二级索引中包含了查询所需的所有字段，从而使查询可以仅通过访问二级索引而不需要访问实际的表数据（主键索引）。

# 索引跳跃
**索引跳跃（Index Skip Scan）**：<font style="color:rgb(77, 77, 77);">索引跳跃是一种优化技术，用于在多列索引中查找数据，即使查询不是以索引的第一列开始。当索引的第一列选择性很差时，索引跳跃可以跳过该列，并在后续列上进行查找。这可以减少所需的索引扫描次数，从而提高查询性能。</font>

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1750017576463-a724e61d-9f10-4690-bf23-cbc623782e7e.png)

**索引区分度(选择性)**

另外，建立联合索引时的字段顺序，对索引效率也有很大影响。越靠前的字段被用于索引过滤的概率越高，实际开发工作中建立联合索引时，要把区分度大的字段排在前面，这样区分度大的字段越有可能被更多的 SQL 使用到。

区分度就是某个字段 column 不同值的个数「除以」表的总行数，计算公式如下：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1750017488314-317a20e2-b686-47ff-9b83-7299a72596e2.png)

比如，性别的区分度就很小，不适合建立索引或不适合排在联合索引列的靠前的位置，而 UUID 这类字段就比较适合做索引或排在联合索引列的靠前的位置。

因为如果索引的区分度很小，假设字段的值分布均匀，那么无论搜索哪个值都可能得到一半的数据。在这些情况下，还不如不要索引，因为 MySQL 还有一个查询优化器，查询优化器发现某个值出现在表的数据行中的百分比（惯用的百分比界线是"30%"）很高的时候，它一般会忽略索引，进行全表扫描(避免频繁回表导致的频繁磁盘IO)。

# 索引失效有哪些
+ 使用联合索引时不满足最左前缀法则
    - 多个字段order by时，索引顺序与联合索引的顺序不一致
+ LIKE以通配符开头：LIKE‘%张'
+ 使用了is null或is not null或!=或not in
+ OR条件中存在无索引的字段，出现>, <, >=, <=等索引也会失效
+ where中使用了函数或表达式: where score / 10 > 8
+ mysql优化器计算认为使用全表扫描更快时
    - 数据重复率高（选择性低）
    - 查询条件范围过大
    - ~~表中数据量过小~~
+ 对索引隐式类型转换, 例如用数字去匹配字符串类型的索引列：user_id = '123', mysql会尝试把字符串转为数字再匹配, 这就相当于对索引使用了函数

# 设计索引的时候有哪些原则？
1. 考虑查询的频率和效率：在决定创建索引之前，需要分析查询频率和效率。对于频繁查询的列，可以创建索引来加速查询，但对于不经常查询或者数据量较少的列，可以不创建索引。
2. 选择适合的索引类型：MySQL提供了多种索引类型，如B+Tree索引、哈希索引和全文索引等。不同类型的索引适用于不同的查询操作，需要根据实际情况选择适合的索引类型。
3. 考虑区分度：尽量不要选择区分度不高的字段作为索引，比如性别。但是也并不绝对，对于一些数据倾斜比较严重的字段，虽然区分度不高，但是如果有索引，查询占比少的数据时效率也会提升。
4. 考虑联合索引：联合索引是将多个列组合在一起创建的索引。当多个列一起被频繁查询时，可以考虑创建联合索引。
5. 考虑索引覆盖：联合索引可以通过索引覆盖而避免回表查询，可以大大提升效率，对于频繁的查询，可以考虑将select后面的字段和where后面的条件放在一起创建联合索引。
6. 避免创建过多的索引：创建过多的索引会占用大量的磁盘空间，影响写入性能。并且在数据新增和删除时也需要对索引进行维护。所以在创建索引时，需要仔细考虑需要索引的列，避免创建过多的索引。
7. 避免使用过长的索引：索引列的长度越长，索引效率越低。在创建索引时，需要选择长度合适的列作为索引列。对于文本列，可以使用前缀索引来减少索引大小。虽然索引不建议太长，但也要合理设置。如果设置太短（比如身份证号仅用前 6 位作为索引），可能会因区分度不足导致查询效率下降。
8. 执行计划分析：多用执行计划分析，因为随着数据库的数据量变化、索引数量变化，最终使用的索引可能也不太一样，所以需要经常查看索引是否有使用正确。

总结：

+ **避免冗余**：索引数量要控制，避免过多索引影响写性能和占用空间。
+ **查询频繁**：列经常出现在 `WHERE`、`JOIN`、`ORDER BY`、`GROUP BY` 中，且查询性能存在瓶颈。
+ **区分度高**：如手机号、用户ID等能有效过滤大量数据，不要选择区分度不高的字段作为索引，比如性别。
+ **覆盖索引**：优先考虑能覆盖查询的联合索引，避免回表，提升效率。
+ **联合索引**：当多个列一起被频繁查询时考虑创建联合索引。并且列顺序需满足“最左前缀”原则。
+ **索引字段不宜过长**：长文本字段可使用**前缀索引**，兼顾区分度与性能。
+ 合适的索引类型：根据实际场景选择 B+Tree、哈希、全文索引等。
+ 动态优化：定期通过执行计划（`EXPLAIN`）分析索引使用情况，及时调整。

# -------------------日志--------------------
# undolog, redolog, binlog的作用
**undolog** 用来**回滚事务保证事务的原子性**, 是一种用于撤销回退的日志, 属于逻辑日志, 在更新一条记录时，会将旧值给记录下来，这样就可以根据这个旧值来回滚<font style="color:#8A8F8D;">(如果一条update语句更新了上千万行记录会产生非常大的undolog)</font>。

> buffer pool 中有 undo 页，对 undo 页的修改也都会记录到 redo log。redo log 会每秒刷盘，提交事务时也会刷盘，数据页和 undo 页都是靠这个机制保证持久化的。
>

**redolog** 来**保证事务的持久性**, redolog 就是重做日志, 用来记录某个数据页发生了什么修改, 它由两个部分组成, 一个是 redolog buffer在内存中, 一个是 redolog 文件在磁盘中。mysql在修改数据后会生成一条 redolog 在 redolog buffer 中, 默认情况下, 在事务提交的时候会将 redolog  buffer中的 redolog 刷新到磁盘, 这个可以通过mysql的一个参数来设置<font style="color:#8A8F8D;">(除了默认情况还可以设置为事务提交时不主动刷盘和事务提交时将 redolog 缓存在PageCache, 后者只要操作系统不宕机即使数据库崩溃了也不会丢失数据, 前者性能最好, 此外InnoDB的后台线程每隔1秒也会将 redolog buffer持久化到磁盘。)</font>。redolog 刷盘后更新就算完成了, 但是磁盘上的数据并不一定被修改了, 因为InnoDB使用了WAL技术, 也就是先写日志(日志先行), 先将更新操作记录到日志里面, 然后在合适的时间再写到磁盘上。<font style="color:#8A8F8D;">(为什么要这么干, 不直接将数据写到磁盘呢, 因为写入 redo log 的方式是顺序写，而直接写入磁盘是随机写, 顺序写的效率要大于随机写。)</font>这样即便系统宕机内存数据丢失了也还能够通过 redolog 来恢复数据来实现持久性。<font style="color:#8A8F8D;">( redolog 是以循环写的方式工作的，也就是说 redolog 是可以写满的, 此时mysql会阻塞住来将buffer pool中的脏页刷新到磁盘中来为 redolog 腾出空间)</font>

**binlog **用于**备份恢复、主从复制**。MySQL 在完成一条更新操作后，Server 层还会生成一条 binlog，等之后事务提交的时候，会将该事务执行过程中产生的所有 binlog 统一写入 binlog 文件。binlog 文件是记录了所有数据库表结构变更和表数据修改的日志, 保存的是全量的日志，也就是保存了所有数据变更的情况，可以用 binlog 文件+全量备份**恢复数据**到特定的时间点。因为 binlog 默认的格式是 row, 所以会导致 binlog 占用的空间比较大, 默认情况下 binlog 的过期时间是 30 天, , 主从复制也是通过 binlog 来实现的, 将主库的 binlog 复制到从库上, 从库读取 binlog 来更新数据。



> 更新数据时先写undolog再写bufferpool再写redolog
>
> 1. 写 Undo Log（记录旧值）
> 2. 更新 Buffer Pool 中的数据页（写新值）
> 3. 写 Redo Log（记录对页的更改）
> 4. 提交事务前，Redo Log 刷盘（Write Ahead Logging）
> 5. 根据情况（例如 checkpoint）将 Buffer Pool 脏页刷盘
>

# binlog 与 redolog 的区别
适用对象不同:

+ binlog 是 MySQL 的 Server 层实现的日志，所有存储引擎都可以使用；
+ redo log 是 InnoDB 存储引擎实现的日志；

文件格式不同:

+ binlog 是逻辑日志，有 3 种格式类型，分别是 STATEMENT、ROW（默认格式(5.7及以后)）、MIXED，区别如下：
    - STATEMENT：每一条修改数据的 SQL 都会被记录到 binlog 中（相当于记录了逻辑操作，所以针对这种格式，binlog 可以称为逻辑日志），主从复制中 slave 端再根据 SQL 语句重现。但 STATEMENT 有动态函数的问题，比如你用了 uuid 或者 now 这些函数，你在主库上执行的结果并不是你在从库执行的结果，这种随时在变的函数会导致复制的数据不一致；
    - ROW：记录行数据最终被修改成什么样了~~（这种格式的日志，就不能称为逻辑日志了）~~，不会出现 STATEMENT 下动态函数的问题。但 ROW 的缺点是每行数据的变化结果都会被记录，比如执行批量 update 语句，更新多少行数据就会产生多少条记录，使 binlog 文件过大，而在 STATEMENT 格式下只会记录一个 update 语句而已； 
    - MIXED：包含了 STATEMENT 和 ROW 模式，它会根据不同的情况自动使用 ROW 模式和 STATEMENT 模式, 比如说使用了 now 这样的函数就会切换到 ROW 模式；
+ redo log 是物理日志，记录的是在某个数据页做了什么修改，比如对 XXX 表空间中的 YYY 数据页 ZZZ 偏移量的地方做了 AAA 更新；

写入方式不同:

+ binlog 是追加写，写满一个文件，就创建一个新的文件继续写，不会覆盖以前的日志，保存的是全量的日志。
+ redo log 是循环写，日志空间大小是固定的，全部写满就从头开始，保存未被刷入磁盘的脏页日志。

用途不同:

+ binlog 用于备份恢复、主从复制；
+ redo log 用于掉电等故障恢复, 不能用于备份恢复应该 redolog 是循环写的不是全量日志。<font style="color:#8A8F8D;">(为什么故障恢复不可以用 binlog? 因为redo log记录的是页的变化, 恢复效率更高)</font>

# mysql 的 binlog 有几种日志格式
binlog是逻辑日志

binlog 有 3 种格式类型，分别是 STATEMENT、ROW（默认格式(5.7及以后)）、MIXED，通过`binlog_format设置`区别如下：

+ STATEMENT：每一条修改数据的 SQL 都会被记录到 binlog 中，主从复制中 slave 端再根据 SQL 语句重现。但 STATEMENT 有动态函数的问题，比如你用了 uuid 或者 now 这些函数，你在主库上执行的结果并不是你在从库执行的结果，这种随时在变的函数会导致复制的数据不一致；

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1754338281647-47e17a9f-04be-4fb4-9e97-3e5360878856.png)

+ ROW：记录行数据最终被修改成什么样了（~~这种格式的日志，就不能称为逻辑日志了~~），不会出现 STATEMENT 下动态函数的问题。但 ROW 的缺点是每行数据的变化结果都会被记录，比如执行批量 update 语句，更新多少行数据就会产生多少条记录，使 binlog 文件过大，而在 STATEMENT 格式下只会记录一个 update 语句而已； 

如：

```java
# at 1486
#250805  2:33:25 server id 1  end_log_pos 1556 CRC32 0x269a7d17         Update_rows: table id 87 flags: STMT_END_F
### UPDATE `test`.`user`
### WHERE
###   @1=4
###   @2=4
###   @3=11
###   @4=13
### SET
###   @1=4
###   @2=4
###   @3=11
###   @4=14
```

 `@1` 到 `@4` 是字段序号，对应于表定义中字段的顺序，这条语句严格匹配 `WHERE` 条件，确保在行级复制或恢复中不会误操作其它行。  

+ MIXED：包含了 STATEMENT 和 ROW 模式，它会根据不同的情况自动使用 ROW 模式和 STATEMENT 模式, 比如说使用了 now 这样的函数就会切换到 ROW 模式；

# binlog 和 redolog 的刷盘时机
**总结: binlog和redolog默认都是是在事务提交的时候刷盘, binlog的刷盘时机可以由参数**`**sync_binlog**`** 控制, 可以由操作系统决定也可以累积n个事务后刷盘, redolog的刷盘时机可以由**`**innodb_flush_log_at_trx_commit**`**控制, 可以不主动刷盘也可以由操作系统决定, 此外InnoDB的后台线程每隔1秒也会将 redolog buffer持久化到磁盘。**

**binlog: **事务执行过程中, MySQL 先把 binlog 日志写到 binlog cache（Server 层的 cache），事务提交的时候，再把 binlog cache 写到 binlog 文件中。这里并不一定是立即将 binlog 刷新到磁盘中, MySQL提供一个 `sync_binlog` 参数来控制数据库的 binlog 刷到磁盘上的频率, 为 0 的时候每次提交事务只写到 page cache, 由操作系统决定何时将数据持久化到磁盘, <font style="color:#8A8F8D;">(默认)</font>为 1 的时候每次提交事务都会立即持久化到磁盘, 大于 1 为 N 的时候表示每次提交事务都写到 page cache, 累积 N 个事务时会 fsync 刷新到磁盘

**redolog: **事务执行过程中, MySQL 先把 redolog 日志写到 redolog buffer，默认情况下, 在事务提交的时候会将 redolog  buffer中的 redolog 刷新到磁盘, 这个可以通过mysql的一个参数来设置<font style="color:#8A8F8D;">(innodb_flush_log_at_trx_commit)</font>, 除了默认情况<font style="color:#8A8F8D;">(参数为1)</font>还可以设置为事务提交时不主动刷盘<font style="color:#8A8F8D;">(参数为0)</font>和事务提交时将 redolog 缓存在PageCache,<font style="color:#8A8F8D;">(参数为2)</font> 后者只要操作系统不宕机即使数据库崩溃了也不会丢失数据, 前者性能最好, **此外**InnoDB的后台线程每隔1秒也会将 redolog buffer持久化到磁盘。

# mysql的错误日志/慢查询日志/ 查询日志/中继日志
**错误日志:**



**慢查询日志:**

慢查询日志用于记录执行时间超过指定阈值的 SQL 语句。

+ <font style="color:rgb(28, 31, 35);">开启MySQL慢日志查询开关 </font>
    - `<font style="color:rgb(28, 31, 35);">slow_query_log=1 </font>`
+ <font style="color:rgb(28, 31, 35);">设置慢日志的时间为2秒，SQL语句执行时间超过2秒，就会视为慢查询，记录慢查询日志 </font>
    - `<font style="color:rgb(28, 31, 35);">long_query_time=2 </font>`
+ <font style="color:rgba(0, 0, 0, 0.85);">通过设置 </font>`<font style="color:rgba(0, 0, 0, 0.85);">slow_query_log_file</font>`<font style="color:rgba(0, 0, 0, 0.85);"> 参数来指定慢查询日志文件的存放位置和名称。</font>

**查询日志:**



**中继日志:**



# --------------------锁----------------------
# 数据库锁的种类
在 MySQL 里根据锁的粒度或者范围, 可以分为**全局锁、表级锁和行级锁**三类。

**全局锁**主要用于备份数据库, 加上全局锁的时候整个数据库就处于只读状态了, 避免数据库备份时出现数据不一致的问题, 除了表级锁也可以利用可重复读隔离级别的特性(备份时开启事务)来保证数据的一致性。

**表级锁**分为四类: **表锁**, **元数据锁**(MDL), **意向锁**, **AUTO-INC锁**

对于**表锁**，分为两类：

+ 表共享读锁（read lock）
    - 共享读锁是一种允许**多个事务同时读取**某一张表（或某一行）但**不能写入(包括自己)**的锁。
    - 如果本线程对一张表加了表共享读锁, 那本线程也无法访问其他表

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1751012691468-ca9fed4b-c095-498e-8ca7-67de59721859.png)

+ 表独占写锁（write lock）
    - 独占写锁是一种**只允许一个事务对数据进行修改**的锁，这期间其他事务**不能读取也不能写入**这张表或被锁的行。 

**元数据锁**(MDL) 是为了保证当用户对表执行 CRUD 操作时，防止其他线程对这个表结构进行变更。

我们不需要显式的使用 MDL，因为当我们对数据库表进行操作时，会自动给这个表加上 MDL：

+ 对一张表进行 CRUD 操作时，加的是 MDL 读锁；
+ 对一张表做结构变更操作的时候，加的是 MDL 写锁；

MDL 是在事务提交后才会释放，这意味着事务执行期间，MDL 是一直持有的。

如果有一个**长事务**一直未提交, 然后又有一个线程进行了表结构的修改, 那么这个线程就会一直阻塞直到长事务的提交, 在这个线程阻塞住之后如果后续还有 CRUD 操作也会一并被阻塞住, 这是因为申请 MDL 锁的操作会形成一个队列，位于写锁后面的读锁会一直阻塞等待写锁，一旦出现 MDL 写锁等待，会阻塞后续该表的所有 CRUD 操作。

所以为了能安全的对表结构进行变更，**在对表结构变更前，先要看看数据库中的长事务**，是否有事务已经对表加上了 MDL 读锁，如果可以考虑 kill 掉这个长事务，然后再做表结构的变更。

**意向锁**的作用是在**加表级锁的时候快速判断行级锁是否存在**, 在加行锁的时候mysql会自动加上相应的意向锁, 比如说行锁如果是共享锁那表上就会有意向共享锁, 行锁如果是排他锁那表上就会有意向排他锁, 当你去给一张表加表锁的时候就会先去判断有没有意向锁, 如果有冲突的锁就会加锁失败, 这里的表锁和行锁是满足读读共享, 读写互斥, 写写互斥的, 意向锁和意向锁之间是不会互斥的因为意向锁只是相当于一个标记

**自增锁(AUTO-INC 锁)的**作用是在插入数据时确保自增后的主键是唯一的, 防止并发插入导致的冲突, mysql提供了一个参数(innodb_autoinc_lock_mode)来控制自增锁的类型, 默认是轻量级锁, 性能是最高的, 在申请完自增主键后立即释放锁, 而不需要等到语句执行完后才释放, 但是这样在「主从复制的场景」中可能会发生数据不一致的问题, 如果binlog的日志格式是statement, 那么binlog里面insert的顺序和实际申请主键的顺序可能会不一样从而导致数据的不一致, 但是binlog默认的日志格式是row, 所以不会导致这个问题。

**行级锁**分为三类: **记录锁**, **间隙锁**和**临键锁**

**记录锁(Record Lock)**锁住的是一条记录, 分为共享锁(S锁)和排它锁(X锁)

**间隙锁(Gap Lock) **锁的是记录之间间隙, 存在于可重复读隔离级别和串行化隔离级别, 作用是解决可重复读隔离级别下幻读的现象, 防止其他事务插入间隙, 间隙锁可以共存，一个事务采用的间隙锁不会阻止另一个事务在同一间隙上采用间隙锁。

**临键锁(Next-Key Lock)**相当于记录锁+间隙锁的组合, 锁的范围是一个左开右闭的区间, 比如说data_locks表中的lock_data字段显示5并且是临键锁的话, 说明它锁的范围就是5对应的记录以及这条记录前的间隙, 因为临键锁包含了记录锁, 所有两个范围相同的临键锁是互斥的

**插入意向锁**是一种特殊的间隙锁, 它锁住的是一个点, 当一个事务持有间隙锁时, 另一个事务想要插入这个间隙就会先生成一个插入意向锁, 但此时锁的状态还是等待状态, 这个事务会一直阻塞直到另一个事务提交, 此时锁的状态就会变为正常状态

总结: 间隙锁锁的是记录之间间隙, 临键锁相当于记录锁+间隙锁的组合, 间隙锁和临键锁只存在于可重复读及以上的隔离级别中, 在可重复读隔离级别中, 使用当前读的时候(比如说update)会进行加锁, 它会对每个符合where条件的记录和记录之间的间隙加锁, 需要注意的是, 在可重复读隔离级别下进行update, 如果where里面的字段没有索引的话会导致**整张表被锁**, 因为没有索引会进行全表扫描, 每一个匹配的记录都会被加上临键锁, 相当于锁了整张表, 可以通过sql_safe_updates这个参数, 来让update满足一定的条件才能够执行成功

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1751201747452-7203b199-cf77-4869-b6af-f3bbd9356aad.png)

# 共享锁和排他锁
**共享锁(Shared Lock)**: 又叫做读锁。允许多个事务并发读取同一资源，但不允许修改。只有在释放共享锁后，其他事务才能获得排它锁。共享锁与共享锁不互斥, 与排它锁互斥。像select ... lock in share mode就会申请共享锁

**排它锁(Exclusive Lock): **又叫做写锁。排它锁只允许持有锁的事务对相应数据进行修改, 排它锁与排它锁和共享锁都是互斥的, 像update, insert delete还有select ... for update都会申请排它锁。

# 死锁
事务a和事务b同时拥有同一个范围的间隙锁, 然后事务a向这个间隙插入一条记录, 因为间隙锁的存在, 这个插入操作会生成一个插入意向锁并阻塞等待, 因为插入意向锁和间隙锁是互斥的, 再接着事务b向这个间隙插入一条记录, 同样会生成一个插入意向锁并阻塞等待, 由于事务不提交间隙锁就不会释放, 所以两个事务就陷入了不停的等待, 也就是死锁, 一般这个死锁是不会一直持续下去的, 因为mysql中有个参数(`innodb_lock_wait_timeout`)控制了事务的超时等待时间, 时间一到事务就会被回滚, 并且mysql还默认开启了死锁检测(由参数`innodb_deadlock_detect`控制), 一旦检测到了死锁就会回滚后其中的一个事务

# -------------------架构--------------------
# 两阶段提交
**两阶段提交用来保证 redo log 和 binlog 的一致性**

事务提交后，redo log 和 binlog 都要持久化到磁盘，但是这两个是独立的逻辑，可能出现半成功的状态，这样就造成两份日志之间的逻辑不一致，为了避免这个情况, 就需要进行两阶段提交。两阶段提交把单个事务的提交拆分成了 2 个阶段，分别是「准备（Prepare）阶段」和「提交（Commit）阶段」。当客户端执行 commit 语句或者在自动提交的情况下，MySQL 内部开启一个 XA 事务，分两阶段来完成 XA 事务的提交, 在prepare 阶段, MySQL 将 XID（内部 XA 事务的 ID） 写入到 redo log，同时将 redo log 对应的事务状态设置为 prepare，然后将 redo log 持久化到磁盘（innodb_flush_log_at_trx_commit = 1 的作用）；在commit 阶段, MySQL 把 XID 写入到 binlog，然后将 binlog 持久化到磁盘（sync_binlog = 1 的作用），接着调用引擎的提交事务接口，将 redo log 状态设置为 commit，<font style="color:#8A8F8D;">(此时该状态并不需要持久化到磁盘，只需要 write 到文件系统的 page cache 中就够了，因为只要 binlog 写磁盘成功，就算 redo log 的状态还是 prepare 也没有关系，一样会被认为事务已经执行成功)；</font>这样事务就算提交成功了。在 MySQL 重启后会按顺序扫描 redo log 文件，碰到处于 prepare 状态的 redo log，就拿着 redo log 中的 XID 去 binlog 查看是否存在此 XID：

+ 如果 binlog 中没有当前内部 XA 事务的 XID，说明 redolog 完成刷盘，但是 binlog 还没有刷盘，则回滚事务。
+ 如果 binlog 中有当前内部 XA 事务的 XID，说明 redolog 和 binlog 都已经完成了刷盘，则提交事务。

# mysql执行一条查询语句的内部执行过程
+ 连接器：建立连接，管理连接、校验用户身份；
+ 查询缓存：查询语句如果命中查询缓存则直接返回，否则继续往下执行。MySQL 8.0 已删除该模块；
+ 解析 SQL，通过解析器对 SQL 查询语句进行词法分析、语法分析，然后构建语法树，方便后续模块读取表名、字段、语句类型；
+ 执行 SQL：执行 SQL 共有三个阶段：
    - 预处理阶段：检查表或字段是否存在；将 `select *` 中的 `*` 符号扩展为表上的所有列。
    - 优化阶段：基于查询成本的考虑，选择查询成本最小的执行计划；
    - 执行阶段：根据执行计划执行 SQL 查询语句，从存储引擎读取记录，返回给客户端；

首先是解析sql, 判断有没有语法错误, 然后就是执行sql, 首先会检查表或字段是否存在, 然后优化器会确定一个查询成本最小的执行计划, 然后执行器就会按照执行计划执行sql语句, 从存储引擎读取记录, 然后返回给客户端，执行sql的时候如果有索引就会去走对应的索引，如果说是select for update这种还会去加对应的锁

# update语句的具体执行过程
具体更新一条记录 `UPDATE t_user SET name = 'xiaolin' WHERE id = 1;` 的流程如下:

1. 执行器负责具体执行，会调用存储引擎的接口，通过主键索引树搜索获取 `id = 1` 这一行记录:
    - 如果 `id=1` 这一行所在的数据页本来就在 buffer pool 中，就直接返回给执行器更新；
    - 如果记录不在 buffer pool，将数据页从磁盘读入到 buffer pool，返回记录给执行器。
2. 执行器得到聚簇索引记录后，会看一下更新前的记录和更新后的记录是否一样:
    - 如果一样的话就不进行后续更新流程；
    - 如果不一样的话就把更新前的记录和更新后的记录都当作参数传给 InnoDB 层，让 InnoDB 真正的执行更新记录的操作；
3. 开启事务，InnoDB 层更新记录前，首先要记录相应的 undo log，因为这是更新操作，需要把被更新的列的旧值记下来，也就是要生成一条 undo log，undo log 会写入 Buffer Pool 中的 Undo 页，不过在内存修改该 Undo 页后，需要记录对应的 redo log。
4. InnoDB 层开始更新记录，会先更新内存 (同时标记为脏页)，然后将记录写到 redo log 里面，这个时候更新就算完成了。为了减少磁盘I/O，不会立即将脏页写入磁盘，后续由后台线程选择一个合适的时机将脏页写入到磁盘。这就是 WAL 技术，MySQL的写操作并不是立刻写到磁盘上，而是先写 redo 日志，然后在合适的时间再将修改的行数据写到磁盘上。
5. 至此，一条记录更新完了。
6. 在一条更新语句执行完成后，然后开始记录该语句对应的 binlog，此时记录的 binlog 会被保存到 binlog cache，并没有刷新到硬盘上的 binlog 文件，在事务提交时才会统一将该事务运行过程中的所有 binlog 刷新到硬盘。
7. 事务提交 (为了方便说明，这里不说组提交的过程，只说两阶段提交):
    - prepare 阶段: 将 redo log 对应的事务状态设置为 prepare，然后将 redo log 刷新到硬盘；
    - commit 阶段: 将 binlog 刷新到磁盘，接着调用引擎的提交事务接口，将 redo log 状态设置为 commit (将事务设置为 commit 状态后，将状态刷入到磁盘 redo log 文件)；
8. 至此，一条更新语句执行完成。



1. 操作 buffer pool 数据（加载页）。
2. 记录 undo log（用于回滚、MVCC）。
3. 修改 buffer pool 内存数据。
4. 写入 redo log buffer（记录物理修改）。
5. redo log 刷盘（prepare 阶段）。
6. 写入并刷盘 binlog（保证主从 / 恢复一致性）。
7. redo log 刷盘（commit 阶段，事务提交）。
8. 异步刷脏页（buffer pool 数据落盘，后台操作）。

# SQL查询语句的执行顺序是怎么样的？
from -> join on -> where -> group by -> having -> select -> distinct -> order by -> limit

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1751706617551-48127963-1195-4262-bda6-a37e08408c86.png)

# -------------------调优--------------------
# sql调优
<font style="color:rgba(0, 0, 0, 0.85) !important;">一般一个 SQL 慢，可能有以下几种原因：</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">1、索引失效  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">2、多表 join  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">3、查询字段太多  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">4、表中数据量太大  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">5、索引区分度不高  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">6、数据库连接数不够  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">7、数据库的表结构不合理  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">8、数据库 IO 或者 CPU 比较高  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">9、数据库参数不合理  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">10、事务比较长  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">11、锁竞争导致的等待  
</font><font style="color:rgba(0, 0, 0, 0.85) !important;">12、深分页问题</font>

# 给你张表，发现查询速度很慢，你有哪些解决方案
**<font style="color:rgba(48,79,254,1);">查看慢查询语句</font>**: 首先去慢查询日志(xxx-slow.log)里查看有哪些慢查询语句(不知道有哪些慢查询语句的情况下)

**<font style="color:rgba(48,79,254,1);">分析查询语句</font>**：使用EXPLAIN命令分析SQL执行计划，找出慢查询的原因，比如是否使用了全表扫描，是否存在索引未被利用的情况等，并根据相应情况对索引进行适当修改。

**<font style="color:rgba(48,79,254,1);">创建或优化索引</font>**：根据查询条件创建合适的索引，特别是经常用于WHERE子句的字段、Orderby 排序的字段、Join 连表查询的连接条件的字段、 group by的字段，并且如果查询中经常涉及多个字段，考虑创建联合索引，使用联合索引要符合最左匹配原则，不然会索引失效

**<font style="color:rgba(48,79,254,1);">避免索引失效</font>**：比如不要用左模糊匹配、函数计算、表达式计算等等。

**<font style="color:rgba(48,79,254,1);">查询优化</font>**：避免使用SELECT *，只查询真正需要的列；使用覆盖索引，即索引包含所有查询的字段；联表查询最好要以小表驱动大表，~~并且被驱动表的字段要有索引~~，当然最好通过冗余字段的设计，避免联表查询。

**<font style="color:rgba(48,79,254,1);">分页优化</font>**：

+ **利用子查询**,  先将对应记录的 id 通过分页查出来, 再通过这些 id 将对应的完整记录查出来, 例如

```sql
#  优化前
select *
from `order`
order by id
limit 5999990, 10;
#优化后
select *
from `order`
where id >=
      (select id from `order` order by id limit 5999990, 1)
order by id
limit 10;
# 或
select *
from `order` a,
     (select id
      from `order`
      order by id
      limit 5999990, 10) b
where a.id = b.id;
# 注意, 这里的 order by 一定要加上, 否则可能不会按照 id 排序
```

+ **每次分页都返回当前的最大id**，然后下次查询的时候，带上这个id，就可以利用id > maxid过滤了。 这种查询仅适合连续查询的情况，如果跳页的话就不生效了。

**<font style="color:rgba(48,79,254,1);">优化数据库表</font>**：如果单表的数据超过了千万级别，考虑是否需要将大表拆分为小表，减轻单个表的查询压力。也可以将字段多的表分解成多个表，有些字段使用频率高，有些低，数据量大时，会由于使用频率低的存在而变慢，可以考虑分开。

**<font style="color:rgba(48,79,254,1);">使用缓存技术</font>**：引入缓存层，如Redis，存储热点数据和频繁查询的结果，但是要考虑缓存一致性的问题，对于读请求会选择旁路缓存策略，对于写请求会选择先更新 db，再删除缓存的策略。

**<font style="color:rgba(48,79,254,1);">其他:</font>** 可以通过show processlist查看mysql上活跃的线程, 查看是否有执行时间长的query查询

还可以通过information_schema.innodb表查看有哪些长事务, 有可能是这些长事务占用了资源导致其他事务卡住

# count(*)的优化
[https://cloud.tencent.com/developer/article/1479194](https://cloud.tencent.com/developer/article/1479194)

# -----------------主从复制-----------------


# -----------------分库分表-----------------


# -------------------其他-------------------
# 隐式锁
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1745759654218-4e0fdbc3-62fb-46a7-9e69-3efed263a6e2.png)

事务a执行`update user set a = 1 where c = 2`时因为c没有索引, 所以会去主键索引里扫描整张表, 而update是当前读, 每扫描到一个记录都会用临键锁锁起来, 就相当于是把整张表给锁起来了, 这是**显式锁**; 在主键索引中将a的值修改后还要再到a自己的索引中修改a的值, 但是<font style="color:rgb(24, 25, 28);">在a索引找到记录直接修改是不行的，会破坏索引的有序性, innodb采取的措施是：先删后插; 在事务b执行</font>`<font style="color:rgb(24, 25, 28);">select id from user where a = 1 lock in share mode;</font>`<font style="color:rgb(24, 25, 28);">前a索引中a=1的记录是没有被显式地锁起来的, 执行之后事务b将a=1的记录锁起来, 但是锁的持有者还是事务a, 这就是</font>**<font style="color:rgb(24, 25, 28);">隐式锁</font>**

sql变成`update user set id = id +1 where a = 1;`的话, 首先会将a索引和主键索引用显式锁锁起来, b索引中对应的记录会被隐式锁锁起来, id的数据在主键索引中采用先删后插, 在a索引和b索引可以直接修改, 因为不会影响a索引和b索引的有序性

# insert是如何加锁的
**对于唯一二级索引:**

事务A和事务B插入一条相同的记录

事务B会给已存在的二级索引列值重复的二级索引记录添加 S 型**临键锁**(等待状态), 同时会将事务A的隐式锁变为显式锁(X 型记录锁)

**对于唯一主键索引:**

事务A和事务B插入一条相同的记录

事务B会给已存在的二级索引列值重复的二级索引记录添加 S 型**记录锁**(等待状态), 同时会将事务A的隐式锁变为显式锁(X 型记录锁)

**记录之间有间隙锁:**

加插入意向锁(等待状态)

