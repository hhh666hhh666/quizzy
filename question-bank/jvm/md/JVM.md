# 内存
## 内存结构
<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1754430997283-c69a15a0-984d-422e-b450-bfe22d06e549.png)

+ **程序计数器**：一个只读的存储器，用于记录Java虚拟机正在执行的字节码指令的地址。它是线程私有的，为每个线程维护一个独立的程序计数器，用于指示下一条将要被执行的字节码指令的位置。它保证线程执行一个字节码指令以后，才会去执行下一个字节码指令。
+ **Java虚拟机栈**：一种线程私有的存储器，用于存储Java中的局部变量。根据Java虚拟机规范，每次方法调用都会创建一个栈帧，该栈帧用于存储局部变量，操作数栈，动态链接，方法出口等信息。当方法执行完毕之后，这个栈帧就会被弹出，变量作用域就会结束，数据就会从栈中消失。
+ **本地方法栈：**本地方法栈是一种特殊的栈，它与Java虚拟机栈有着相同的功能，但是它支持本地代码（Native Code）的执行。本地方法栈中存放本地方法（Native Method）的参数和局部变量，以及其他一些附加信息。这些本地方法一般是用C等本地语言实现的，虚拟机在执行这些方法时就会通过本地方法栈来调用这些本地方法。
+ **Java堆：**是存储对象实例的运行时内存区域。它是虚拟机运行时的内存总体的最大的一块，也一直占据着虚拟机内存总量的一大部分。Java堆由Java虚拟机管理，用于存放对象实例，是几乎所有的对象实例都要在上面分配内存。此外，Java堆还用于垃圾回收，虚拟机发现没有被引用的对象时，就会对堆中对象进行垃圾回收，以释放内存空间。
+ **方法区：**用于存储已被加载的类信息、常量、静态变量(引用)、即时编译后的代码等数据的内存区域。每加载一个类，方法区就会分配一定的内存空间，用于存储该类的相关信息，这部分空间随着需要而动态变化。方法区的具体实现形式可以有多种，比如堆、永久代、元空间等。
    - **运行时常量池：**是方法区的一部分。用于存储编译阶段生成的信息，主要有字面量和符号引用常量两类。其中符号引用常量包括了类的全限定名称、字段的名称和描述符、方法的名称和描述符。
+ **直接内存**：不属于 JVM 运行时数据区的一部分，通过 NIO 类引入，是一种堆外内存，可以显著提高 I/O 性能。直接内存的使用受到本机总内存的限制，若分配不当，可能导致 OutOfMemoryError 异常。



<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1754248972731-6c792d1c-8611-47e2-a415-5a2753f9a18f.png)

## 一个对象的结构是什么样的？
[https://github.com/openjdk/jdk8u/blob/master/hotspot/src/share/vm/oops/markOop.hpp](https://github.com/openjdk/jdk8u/blob/master/hotspot/src/share/vm/oops/markOop.hpp)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/svg/52164061/1755544020520-a73db815-3a9b-45ac-9653-3c5f6c24ced8.svg)

### JVM是如何创建对象的？
1. 首先将去检查这个指令的参数是否能在常量池中定位到这个类的符号引用，并且检查这个符号引用代表的类是否已被加载过、解析和初始化过。如果没有，那必须先执行相应的类加载过程  
2. 分配内存。JVM会在堆中为对象分配内存空间（无JIT优化情况下）。在HotSpot中，对象的内存分配有两种方式，分别是指针碰撞和空闲列表法。  
    - 指针碰撞：当堆中的内存是连续的，JVM使用一个指针来标记当前可用的内存位置，然后将指针向前移动分配对象所需的内存大小。  
    - 空闲列表：当堆中的内存是离散的，JVM会维护一个空闲列表，记录可用的内存块。在分配对象时，JVM会遍历空闲列表，找到足够大小的内存块进行分配。

(分配内存解决并发有两种手段，一个是CAS+失败重试，一个是Thread Local Allocation Buffer（TLAB）)  

3. 内存分配完成后，虚拟机需要将分配到的内存空间都**初始化为零值**，这一步确保了对象的字段在创建时都有默认值。如int被初始化为0，引用类型被初始化为null  
4. 设置对象头。该实例所对应的类、如何才能找到类的元数据信息、对象的哈希码、对象的 GC 分代年龄，轻量级锁等等信息  
5. 调用该类的构造方法，初始化对象。如按照程序员意愿进行赋值  
6. 返回对象引用，当对象完成创建之后，返回一个该对象的引用，后续Java程序就可以使用这个引用来操作对象了。

总结：检查类是否加载 -> 分配内存 -> 初始化内存 -> 设置对象头 -> 调用构造方法 -> 返回对象引用

### Java中的对象一定在堆上分配内存吗？
不一定，在HotSpot虚拟机中，存在[JIT](#eOV8x)优化的机制，JIT优化中可能会进行逃逸分析，当经过逃逸分析发现某一个局部对象没有逃逸到线程和方法外的话，那么这个对象就可能不会在堆上分配内存，而是进行[栈上分配](#g8FA2)。

## 什么是强引用、软引用、弱引用和虚引用？
[https://www.yuque.com/hollis666/vn7dy3/mx9eo0s2s5iaah2s](https://www.yuque.com/hollis666/vn7dy3/mx9eo0s2s5iaah2s)

**强引用**

强引用是**默认的引用方式**， 直接使用 `new` 创建对象就是强引用，**如果引用一直在，就不会被 GC 回收掉。**HashMap使用的引用就是强引用，也就是说垃圾收集的时候，Map中引用的对象不会被GC掉。

**软引用**

通过SoftReference创建的对象就是软引用，**当内存不足时，垃圾回收器会优先回收软引用指向的对象**，以释放内存。软引用可以在系统内存充足时保留对象，以提高性能（缓存机制）。通过软引用，可以在内存不足时能自动回收缓存对象，来避免 OOM 的发生。

```java
SoftReference<Object> softRef = new SoftReference<>(new Object());
Object obj = softRef.get(); // 获取引用的对象
```

**弱引用**

通过WeakReference创建的对象就是弱引用。被弱引用指向的对象只能存活到下一次垃圾回收之前。 **只要发生 GC，无论内存是否充足，都会被回收。**基于弱引用类型实现的HashMap——WeakHashMap。当WeakHashMap中的 key已经不再被使用的时候，Entry 因为是弱引用，那么就会被自动回收掉。另外，为了避免内存泄漏，在 [ThreadLocal](https://www.yuque.com/hollis666/vn7dy3/bueq7weva8ha9f1p#va4DK) 中也用到了弱引用。

```java
WeakReference<Object> weakRef = new WeakReference<>(new Object());
Object obj = weakRef.get();
```

**虚引用**

虚引用是最弱的一种引用。虚引用的存在不会影响对象的生命周期，垃圾回收器在回收对象时不会考虑虚引用。使用PhantomReference来创建的对象就是虚引用。**虚引用主要用于跟踪对象被回收的状态，并在对象被回收后进行一些后续处理（如清理本地资源等）。**当对象被 GC 回收时，虚引用(PhantomReference实例)会被加入队列  

```java
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        byte[] bytes = new byte[1024 * 1024];
        ReferenceQueue<byte[]> refQueue = new ReferenceQueue<>();
        // 创建虚引用(需要用变量接收，否则会被回收)
        PhantomReference<byte[]> phantomRef = new PhantomReference<>(bytes, refQueue);
        // 释放强引用
        bytes = null;
        // 手动触发GC
        System.gc();
        while (true) {
            Reference<? extends byte[]> poll = refQueue.poll();
            if (poll != null) {
                System.out.println(poll);
                System.out.println("对象已回收");
                return;
            }
            System.out.println("等待对象回收...");
        }
    }
}
```

```java
等待对象回收...
java.lang.ref.PhantomReference@ed17bee
对象已回收
```

## 内存泄漏和内存溢出的区别是什么？
**内存泄漏指的是程序中分配的内存在不再需要时没有被正确释放或回收的情况。**这会导致程序持续占用内存，随着时间的推移，可用内存逐渐减少，最终可能导致程序性能下降或崩溃。

内存泄漏通常发生在程序中的对象或数据结构被创建后，但没有适时地释放对它们的引用，从而阻止垃圾回收器将它们清理出内存。常见的内存泄漏情况包括未关闭的文件或数据库连接、未释放的资源对象（如打开的文件句柄或网络连接）、长时间被引用的集合类（List、Map）等。



**内存溢出指的是程序试图分配超过其可用内存的内存空间的情况。**这通常会直接导致Java程序崩溃。

常见的内存溢出情况包括栈溢出和堆溢出。我们常说的内存溢出如果没有特别说明都是指堆溢出，即OutOfMemory。在Java中，当程序动态分配内存（例如使用new操作符在堆中创建对象）时，没有足够的可用内存时，就会发生OOM，即OutOfMemoryError。



**一般来说，内存泄漏是会导致内存溢出的，因为内存泄漏会导致部分内存一直无法被回收，久而久之就会没有内存可以分配，就会导致内存溢出。**

## **为什么JDK 1.8要废弃永久代，改用元空间**
**永久代是堆上的一部分，大小收堆大小的限制，容易发生OOM。元空间在本地内存上，不受堆大小的限制，不容易发生OOM。**

**因为方法区作为元空间和永久代的主要区域，他里面存储了大量的被加载的类，而很多时候，项目中用了很多动态生成类的框架之后，比如Spring、等等，就会占用很多空间，如果在堆上，就会导致频繁的GC，以及OOM的问题。**

**而把方法区放到元空间，即放到堆外内存中， JVM 可以根据机器内存大小动态扩展元空间的大小，减少OOM的发生**

# 类加载机制
## 类加载器
**JDK 1.8及之前**

JDK 1.8 以前会使用到以下3种系统提供的类加载器。  

1. **启动类加载器（Bootstrap ClassLoader）**： 这个类加载器负责将存放在`<JAVA_HOME>\lib`目录中的，或者被`-Xbootclasspath`参数所指定的路径中的，并且是虚拟机识别的（仅按照文件名识别，如`rt.jar(包含java.lang)`，名字不符合的类库即使放在`lib`目录中也不会被加载）类库加载到虚拟机内存中。启动类加载器无法被Java程序直接引用，用户在编写自定义类加载器时，如果需要把加载请求委派给引导类加载器，那直接使用`null`代替即可  
2. **扩展类加载器（Extension ClassLoader）**： 这个加载器由`sun.misc.Launcher$ExtClassLoader`实现，它负责加载`<JAVA_HOME>\lib\ext`目录中的，或者被`java.ext.dirs`系统变量所指定的路径中的所有类库，开发者可以直接使用扩展类加载器。  
3. **应用程序类加载器（Application ClassLoader）**： 这个类加载器由`sun.misc.Launcher$App-ClassLoader`实现。由于这个类加载器是`ClassLoader`中的`getSystemClassLoader()`方法的返回值，所以一般也称它为系统类加载器。它负责加载用户类路径（`ClassPath`）上所指定的类库，开发者可以直接使用这个类加载器，如果应用程序中没有自定义过自己的类加载器，一般情况下这个就是程序中默认的类加载器。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1754252305473-2f6f7a22-0301-4721-aa4f-e0cdba987155.png)

**JDK 1.9及以后**

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1754252319897-3c8d0e88-ca09-4cff-aaba-1322c4f963f4.png)

## 双亲委派机制
双亲委派模型的工作过程是：如果一个类加载器收到了类加载的请求，先检查是否已经加载过这个类，如果有则直接返回，如果没有那它也不会自己去尝试加载这个类，而是把这个请求委派给父加载器去完成，每一个层次的类加载器都是如此，因此所有的加载请求最终都应该传送到顶层的启动类加载器中，只有当父加载器反馈自己无法完成这个加载请求（它的搜索范围中没有找到所需的类）时，子加载器才会尝试自己去加载。

简单来说就是**类加载器加载类时，先让父加载器尝试，如果父加载器找不到，才由自己加载  **

****

**findClass用于重写类加载逻辑、loadClass方法的逻辑里如果父类加载器加载失败则会调用自己的findClass方法完成加载，保证了双亲委派规则。**

在自定义类加载器时，如果要**破坏**双亲委派模型，就要继承ClassLoader(抽象类)并重写其中的loadClass方法和findClass()方法。如果不破坏双亲委派模型则只需要重写findClass()方法

```java
protected Class<?> loadClass(String name, boolean resolve)
    throws ClassNotFoundException
{
    synchronized (getClassLoadingLock(name)) {
        // First, check if the class has already been loaded
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            long t0 = System.nanoTime();
            try {
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found
                // from the non-null parent class loader
            }

            if (c == null) {
                // If still not found, then invoke findClass in order
                // to find the class.
                long t1 = System.nanoTime();
                c = findClass(name);

                // this is the defining class loader; record the stats
                sun.misc.PerfCounter.getParentDelegationTime().addTime(t1 - t0);
                sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                sun.misc.PerfCounter.getFindClasses().increment();
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
```

****

**为什么需要双亲委派模型？**

使用双亲委派模型来组织类加载器之间的关系，有一个显而易见的好处就是Java类随着它的类加载器一起具备了一种带有优先级的层次关系。例如类java.lang.Object，它存放在rt.jar之中，无论哪一个类加载器要加载这个类，最终都是委派给处于模型最顶端的启动类加载器进行加载，因此Object类在程序的各种类加载器环境中都是同一个类。**保证了 Java 类加载的安全性、一致性和避免重复加载**。  

相反，如果没有使用双亲委派模型，由各个类加载器自行去加载的话，如果用户自己编写了一个称为java.lang.Object的类，并放在程序的ClassPath中，那系统中将会出现多个不同的Object类，Java类型体系中最基础的行为也就无法保证，应用程序也将会变得一片混乱。



[有哪些破坏双亲委派的例子](https://www.yuque.com/hollis666/vn7dy3/gt8zp4#Z0QfG)

比如说tomcat，一个tomcat可能会运行好几个项目，可能有SpringBoot2.x版本，也有SpringBoot3.x版本，他需要根据不同的情况去加载不同的类，所以需要打破双亲委派机制

## 一个类的加载流程？
**加载阶段**

首先JVM会根据类的全限定名找到class文件，然后通过类加载器加载到内存中，在方法区或者说元空间生成类对应的元数据(类名变量名方法名等)，然后生成对应的 `Class` 对象  ，作为这个类的各种数据的访问入口。

<details class="lake-collapse"><summary id="u4dc8e4f8"><span class="ne-text">Java中的类什么时候会被加载？</span></summary><ul class="ne-ul"><li id="u9c3c5922" data-lake-index-type="0"><span class="ne-text">创建类的实例</span></li><li id="u65df5469" data-lake-index-type="0"><span class="ne-text">使用类的静态变量或静态方法时</span></li><li id="ufa925720" data-lake-index-type="0"><span class="ne-text">使用反射机制访问类时</span></li><li id="u8fb7d649" data-lake-index-type="0"><span class="ne-text">当JVM启动时，会自动加载一些基础类，例如java.lang.Object类和java.lang.Class类等</span></li></ul><p id="ue6c1d03a" class="ne-p"><span class="ne-text">总之，Java中的类加载其实是延迟加载的，除了一些基础的类以外，其他的类都是在需要使用类时才会进行加载。同时，Java还支持动态加载类，即在运行时通过程序来加载类，这为Java程序带来了更大的灵活性。</span></p></details>
**链接阶段**

1. 验证：校验类的正确性
2. 准备：为类的 **静态变量** 分配内存(方法区)，并赋予**默认初始值**（不是用户写的值）。  
3. 解析：把类的符号引用转为直接引用(类或接口、字段、类方法、接口方法、方法类型、方法句柄和访问控制修饰符7类符号引用 )

> 符号引用（Symbolic Reference）是一种用来表示引用目标的符号名称，比如类名、字段名、方法名等。符号引用与实际的内存地址无关，**只是一个标识符，用于描述被引用的目标**，类似于变量名。符号引用是在编译期间产生的，在编译后的class文件中存储。
>
> 直接引用（Direct Reference）是**实际指向目标的内存地址**，比如类的实例、方法的字节码等。直接引用与具体的内存地址相关，是在程序运行期间动态生成的。
>

**初始化阶段**

初始化阶段是执行类构造器 `<clinit>()`<font style="color:#8A8F8D;">(执行静态变量的显式赋值和执行静态代码块)</font>方法<font style="color:#8A8F8D;">(由编译器自动生成  )</font>的过程。

只有在**第一次主动使用类**时，才会触发初始化(比如调用类的静态方法、访问静态变量、new 对象等)。(懒加载)

<details class="lake-collapse"><summary id="u801db17e"><span class="ne-text">示例</span></summary><p id="udef4d336" class="ne-p"><img src="https://cdn.nlark.com/yuque/0/2025/png/52164061/1755532340000-a86ad04f-5e40-435b-ba29-0849c5a72957.png" width="524.6666870117188" title="" crop="0,0,1,1" id="udac20e88" class="ne-image"></p><p id="u615f437e" class="ne-p"><span class="ne-text">要分析使用 </span><code class="ne-code"><span class="ne-text">E.a</span></code><span class="ne-text">、</span><code class="ne-code"><span class="ne-text">E.b</span></code><span class="ne-text">、</span><code class="ne-code"><span class="ne-text">E.c</span></code><span class="ne-text"> 是否会导致 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类初始化，需依据 </span><strong><span class="ne-text">Java 类加载机制</span></strong><span class="ne-text"> 中 “常量传播” 规则：被 </span><code class="ne-code"><span class="ne-text">final</span></code><span class="ne-text"> 修饰的 </span><strong><span class="ne-text">编译期常量</span></strong><span class="ne-text"> ，使用时不会触发类初始化；若为运行期确定值的 </span><code class="ne-code"><span class="ne-text">final</span></code><span class="ne-text"> 变量（如包装类型），则可能触发。</span></p><h3 id="WlGwG"><span class="ne-text">关键区分：编译期常量 vs 运行期常量</span></h3><ul class="ne-ul"><li id="u91e710a7" data-lake-index-type="0"><code class="ne-code"><span class="ne-text">E.a</span></code><strong><span class="ne-text">（</span></strong><code class="ne-code"><span class="ne-text">static final int a = 10</span></code><strong><span class="ne-text">）</span></strong><span class="ne-text">：</span><code class="ne-code"><span class="ne-text">int</span></code><span class="ne-text"> 是基本类型，</span><code class="ne-code"><span class="ne-text">final</span></code><span class="ne-text"> + 字面量赋值，属于 </span><strong><span class="ne-text">编译期常量</span></strong><span class="ne-text"> 。字节码中会直接嵌入 </span><code class="ne-code"><span class="ne-text">10</span></code><span class="ne-text"> ，使用时不触发 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类初始化。  </span></li><li id="u17160172" data-lake-index-type="0"><code class="ne-code"><span class="ne-text">E.b</span></code><strong><span class="ne-text">（</span></strong><code class="ne-code"><span class="ne-text">static final String b = &quot;hello&quot;</span></code><strong><span class="ne-text">）</span></strong><span class="ne-text">：</span><code class="ne-code"><span class="ne-text">String</span></code><span class="ne-text"> 字面量是 </span><strong><span class="ne-text">编译期常量</span></strong><span class="ne-text"> （JVM 编译时可确定值）。字节码直接嵌入 </span><code class="ne-code"><span class="ne-text">&quot;hello&quot;</span></code><span class="ne-text"> ，使用时不触发 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类初始化。  </span></li><li id="u166651dc" data-lake-index-type="0"><code class="ne-code"><span class="ne-text">E.c</span></code><strong><span class="ne-text">（</span></strong><code class="ne-code"><span class="ne-text">static final Integer c = 20</span></code><strong><span class="ne-text">）</span></strong><span class="ne-text">：</span><code class="ne-code"><span class="ne-text">Integer</span></code><span class="ne-text"> 是包装类型，</span><code class="ne-code"><span class="ne-text">final</span></code><span class="ne-text"> + 基本类型赋值会触发 </span><strong><span class="ne-text">自动装箱</span></strong><span class="ne-text">（</span><code class="ne-code"><span class="ne-text">Integer.valueOf(20)</span></code><span class="ne-text"> ），这是 </span><strong><span class="ne-text">运行期行为</span></strong><span class="ne-text"> 。使用 </span><code class="ne-code"><span class="ne-text">E.c</span></code><span class="ne-text"> 时，需加载并初始化 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类，才能执行静态变量赋值逻辑。</span></li></ul><h3 id="gv4hd"><span class="ne-text">结论</span></h3><ul class="ne-ul"><li id="u4c93102d" data-lake-index-type="0"><code class="ne-code"><span class="ne-text">E.a</span></code><strong><span class="ne-text">、</span></strong><code class="ne-code"><span class="ne-text">E.b</span></code><span class="ne-text">：不会触发 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类初始化（编译期常量直接内联）。  </span></li><li id="u2fc26f49" data-lake-index-type="0"><code class="ne-code"><span class="ne-text">E.c</span></code><span class="ne-text">：会触发 </span><code class="ne-code"><span class="ne-text">E</span></code><span class="ne-text"> 类初始化（运行期需执行装箱逻辑，依赖类初始化）。</span></li></ul></details>


## 类的生命周期是怎么样的？
**加载阶段**

见上节

**类使用过程**

类的使用，即是类在加载完毕后，会有代码段来引用该类，如初始化该类的对象，或者通过反射获取该类的元数据。

**类卸载过程**

假如说该类满足下面2个条件：

+ 该类所有的实例都已被GC回收。
+ 该类的ClassLoader已经被GC回收。那么该类会在FULLGC期间从方法区被回收掉。

这个时候，我们需要明白一个问题，我们知道，JVM自带的类加载器因为需要一直加载基础对象，所以JDK自带的基础类是一定不会被回收掉的，那么会有哪些类会被回收掉呢？

答案就是那些自定义类加载器一些场景的类会被回收掉，如tomcat，SPI，JSP等临时类，是存活不久的，所以需要来不断回收

# 垃圾回收
## JVM如何判断对象是否存活？
当JVM判断对象不再存活的时候，便会在下一次GC时候将该对象回收掉，为堆腾出空间，那么JVM如何判断对象是否存活呢？



JVM有两种算法来判断对象是否存活，分别是**引用计数法**和**可达性分析算法**

****

+ **引用计数法：**给对象中添加一个引用计数器，每当有一个地方引用它，计数器就加 1；当引用失效，计数器就减 1；任何时候计数器为 0 的对象就是不可能再被使用的。**这个方法实现简单，效率高，但是目前主流的虚拟机中并没有选择这个算法来管理内存，其最主要的原因是它很难解决对象之间相互循环引用的问题。**

> 循环引用会导致对象无法被回收，最终会导致内存泄漏及内存溢出
>

+ **可达性分析算法：** 这个算法的基本思想就是通过一系列的称为 **“GC Roots” **的对象作为起点，从这些节点开始向下搜索，节点所走过的路径称为引用链，当一个对象到 GC Roots 没有任何引用链相连的话，则证明此对象是不可用的。



但是，并不是说当进行完可达性分析算法后，即可证明某对象可以被GC。对象是否存活，需要两次标记：

1. 第一次标记通过可达性分析算法。如果没有GC Roots相连接的引用链，那么将第一次标记(没有第二步则直接回收)
2. 如果对象的finalize()方法被覆盖并且没有执行过，则放在F-Queue队列中等待执行(不一定会执行)(finalize() 的调用是异步的，由 JVM 的 Finalizer 线程 来执行  )，如果一段时间后该对象的finalize()方法被执行且和GC Roots关联，则移出“即将回收”集合。如果仍然没有关联，则进行第二次标记，才会对该对象进行回收	

> finalize：是Object类的一个方法，用于在对象被垃圾回收前执行清理操作，但通常不推荐使用(不重写就不会使用)。
>

不过现在都不提倡覆盖finalize方法，它的本意是像Cpp一样在对象销毁前执行，但是它**影响了JAVA的安全和GC的性能**，所以第二种判断会越来越少

<details class="lake-collapse"><summary id="ufeb10633"><span class="ne-text">知识扩展</span></summary><h2 id="ZEUym"><span class="ne-text">哪些内容可以作为GC roots？</span></h2><p id="u77895620" class="ne-p"><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">GC roots 是作为可达性分析算法的</span><strong><span class="ne-text" style="color: rgb(0, 0, 0) !important">起点的</span></strong><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">。要实现语义正确的可达性分析，就必须要能完整枚举出</span><strong><span class="ne-text" style="color: rgb(0, 0, 0) !important">所有的 GC Roots</span></strong><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">，否则就可能会漏扫描应该存活的对象，导致 GC 错误回收了这些被漏扫的活对象。那么，所谓 “GC Roots”，就是</span><strong><span class="ne-text" style="color: rgb(0, 0, 0) !important">一组必须活跃的引用</span></strong><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">。</span></p><p id="uc08f1413" class="ne-p"><span class="ne-text"></span></p><p id="uc5bc81f8" class="ne-p"><span class="ne-text">那么，有哪些引用是一定活跃的呢？看下下面这些是不是都符合这个条件：</span></p><p id="u4c75a031" class="ne-p"><br></p><ul class="ne-ul"><li id="udf434276" data-lake-index-type="0"><strong><span class="ne-text">Class</span></strong><span class="ne-text"> - 由系统类加载器 (system class loader) 加载的类，这些类是不能够被回收的，他们可以以静态字段的方式保存持有其它对象。</span></li><li id="u957fbb0a" data-lake-index-type="0"><span class="ne-text">Thread - 活着的线程</span></li><li id="ud60788db" data-lake-index-type="0"><span class="ne-text">Stack Local - Java 方法的 local 变量(</span><strong><span class="ne-text">局部变量</span></strong><span class="ne-text">)或参数</span></li><li id="u8ea9011e" data-lake-index-type="0"><span class="ne-text">JNI Local - JNI 方法的 local 变量或参数</span></li><li id="uad1b2405" data-lake-index-type="0"><span class="ne-text">JNI Global - 全局 JNI 引用</span></li><li id="ua0daa130" data-lake-index-type="0"><span class="ne-text">Monitor Used - 被同步锁（synchronized）持有的对象</span></li><li id="ubd053bf5" data-lake-index-type="0"><span class="ne-text">Held by JVM - 用于 JVM 特殊目的由 GC 保留的对象，但实际上这个与 JVM 的实现是有关的。可能已知的一些类型是：系统类加载器、一些 JVM 知道的重要的异常类、一些用于处理异常的预分配对象以及一些自定义的类加载器等。然而，JVM 并没有为这些对象提供其它的信息，因此需要去确定哪些是属于 &quot;JVM 持有&quot; 的了。</span></li></ul><p id="u803dbea7" class="ne-p"><span class="ne-text"></span></p><p id="uc4c7f1ae" class="ne-p"><span class="ne-text">以上，比如系统类加载器加载的类、活着的线程、方法中的本地变量、被 synchronized 锁定的对象这些，都是</span><strong><span class="ne-text">符合活跃</span></strong><span class="ne-text">的引用这个条件的！<br /></span></p><p id="u15366254" class="ne-p"><span class="ne-text">除了这些， 还有一种，基本上很少有人提到的，大家去看网上的所有关于介绍 GC Root 的八股文，基本上没人提的，那就是为了解决跨代引用的问题，会把 Remembered Set 也作为 GC Root。</span></p><p id="ua3d470ad" class="ne-p"><a href="https://www.yuque.com/hollis666/vn7dy3/efipfg3pgg4puux2" data-href="https://www.yuque.com/hollis666/vn7dy3/efipfg3pgg4puux2" target="_blank" class="ne-link"><span class="ne-text">什么是跨代引用，有什么问题？</span></a></p></details>
### 可达性分析算法的不足
虽然可达性分析算法相比于引用计数法要好很多，但是他也不是毫无缺点的。这种算法主要存在以下几个不足：

**STW 时间长**

可达性分析算法需要对程序进行全局分析，因此时间复杂度较高，可能需要很长的时间才能完成分析，并且整个过程都是 STW 的，所以对应用的整体性能有很大影响。这也使得可达性分析算法难以适用于大型程序的分析。所以一些常见的回收器都会使用一些优化技术来减少可达性分析的时间和开销，如增量标记、增量拷贝等。

[✅什么是STW？有什么影响？](https://www.yuque.com/hollis666/vn7dy3/qg9fvqfnzpbd70hl)

> STW，是Stop-The-World的缩写，Stop-The-World是指系统在执行特定操作时，必须暂停（停止）所有的应用程序线程。
>
> 比如在Java中，当需要进行垃圾回收的时候，垃圾回收器需要停止应用程序的所有线程，以便可以安全地识别和回收不再使用的对象。这个过程我们就会称之为是Stop The World了。
>

解决这个问题，主要是依赖[**三色标记法**](https://www.yuque.com/hollis666/vn7dy3/lva8a9gfhagbrw2g)

**内存消耗**

可达性分析算法需要存储程序中所有的对象和它们之间的引用关系，这些信息需要占用大量的内存空间。对于大型程序，如果要进行完整的可达性分析，需要存储的对象数量和引用关系数量都非常大，可能会导致内存空间不足或者程序性能下降的问题。

## 垃圾回收算法有哪些
### 标记-清除
标记清除是最简单和干脆的一种垃圾回收算法，他的执行流程是这样子的：当 JVM 识别出内存中的垃圾以后，直接将其清除，但是这样有一个很明显的缺点，就是会导致内存空间的不连续，也就是会产生很多的内存碎片。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755434336459-43883561-d100-4538-9b91-3f687ec460f1.png)

优点：速度快，因为不需要移动和复制对象

缺点：会产生内存碎片，造成内存的浪费

### 标记-复制
将内存分成两块，每次申请内存时都使用其中的一块，当内存不够时，将这一块内存中所有存活的复制到另一块上。然后将然后再把已使用的内存整个清理掉。复制算法解决了空间碎片的问题。但是也带来了新的问题。因为每次在申请内存时，都只能使用一半的内存空间。内存利用率严重不足。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755435435992-b7706ab8-6299-4862-861b-cdc9a4cfea3c.png)

优点：内存空间是连续的，不会产生内存碎片

缺点：浪费了一半的内存空间，复制对象会造成性能和时间上的消耗

### 标记-整理
标记：它的第一个阶段与标记-清除算法是一模一样的，均是遍历 GC Roots，然后将存活的对象标记。

整理：移动所有存活的对象，且按照内存地址次序依次排列，然后将末端内存地址以后的内存全部回收。

垃圾回收前：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755435560071-ea30e040-92de-4d29-b82d-3a9bb627ac5d.png)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755435580070-ae944ce5-6ca3-49fa-9383-bc3bf06f9f8d.png)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755435590058-38c52b12-278e-4299-8708-b1cdaabccb35.png)

优点：不会产生内存碎片：不会浪费内存空间

缺点：太耗时间（性能低）

### 分代收集算法
**分代收集算法**是目前主流的垃圾收集算法，它根据对象存活周期的不同将内存划分为**新生代**和**老年代**。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755450935957-3ef1f74d-7571-42f4-ad2f-3653c2f563af.png)

**新生代**用**标记-复制**算法，因为大部分对象生命周期短，GC频率高。

标记复制算法存在一个缺点就是会浪费空间，新生代为了解决这个问题，把区域进一步细分成一个Eden区和两个Survivor区，同时工作的只有一个Eden区+一个Survivor区，这样，另外一个空Survivor主要用来复制就可以了。只需要动态的调整Eden区和Survivor区的比例就可以降低空间浪费的问题。

**回收过程：**刚开始对象都分配在Eden区，如果Eden区快满了就触发垃圾回收，把Eden区中的存活对象转移到一块空着的survivor区，eden区清空，然后再次分配新对象到eden区，再触发垃圾回收，就把eden区存活的和survivor区存活的转移到另一块空着的survivor。那么也就是说，在平常的时候，新生代的区域中是只有一块eden和一块survivor区在被使用的，而另一块Survivor区是空着的，所以内存使用率大约 90%。

在YoungGC之后，如果存活的对象所需要的空间比Survivor区域的空间大就会把对象移动到老年代。



**老年代**用**标记-整理**算法，因为对象存活率较高。虽然效率低了一点，但是可以减少空间的浪费并且不会有空间碎片等问题。在有些回收器上面，如CMS，为了降低STW的时长，也会采用标记清除算法。



#### 对象的分代晋升
一般情况下，对象将在新生代进行分配，首先会尝试在Eden区分配对象，当Eden内存耗尽，无法满足新的对象分配请求时，将触发新生代的GC(Young GC、MinorGC)，在新生代的GC过程中，没有被回收的对象会从Eden区被搬运到Survivor区，这个过程通常被称为"晋升"

同样的，**对象也可能会晋升到老年代，触发条件主要看对象的大小和年龄**。对象进入老年代的条件有三个，满足一个就会进入到老年代：

1. 躲过15次GC。每次垃圾回收后，存活的对象的年龄就会加1，累计加到15次（jdk8默认的MaxTenuringThreshold），也就是某个对象躲过了15次垃圾回收，那么JVM就认为这个是经常被使用的对象，就没必要再待在新生代中了。具体的次数可以通过`-XX:MaxTenuringThreshold`来设置在躲过多少次垃圾收集后进去老年代。
2. 动态对象年龄判断。规则：** 从年龄小的对象开始，不断地累加对象的大小，当累加总和超过survivor空间一半时**<font style="color:#8A8F8D;">(TargetSurvivorRatio=50时为一半)</font>**，当前年龄及以上的对象将晋升到老年代。  **

```cpp
uint ageTable::compute_tenuring_threshold(size_t survivor_capacity) {
  
  size_t desired_survivor_size = (size_t)((((double) survivor_capacity)*TargetSurvivorRatio)/100);
  size_t total = 0;
  uint age = 1;
  while (age < table_size) {
    total += sizes[age];
    if (total > desired_survivor_size) break;
    age++;
  }
  uint result = age < MaxTenuringThreshold ? age : MaxTenuringThreshold;
    ...
}
```

3. 大对象直接进入老年代。-XX:PretenureSizeThreshold 来设置大对象的临界值，大于该值的就被认为是大对象，就会直接进入老年代。（PretenureSizeThreshold默认是0，也就是说，默认情况下对象不会提前进入老年代，而是直接在新生代分配。然后就GC次数和基于动态年龄判断来进入老年代。）

#### 什么是永久代？
永久代（Permanent Generation）是HotSpot虚拟机在以前版本中使用的一个永久内存区域，是JVM中垃圾收集堆之外的另一个内存区域，它主要用来实现方法区的，其中存储了Class类信息、常量池以及静态变量等数据。

Java 8以后，永久代被重构为元空间（MetaSpace）。

但是，和新生代、老年代一样，永久代也是可能会发生GC的。而且，永久代也是有可能导致内存溢出。只要永久代的内存分配超过限制指定的最大值，就会出现内存溢出。

#### 不同分代的GC方式？
Minor GC（YoungGC），主要用于对新生代垃圾回收。

Full GC，除了会对新生代垃圾回收以外，还会对老年代或者永久代进行回收。相比较于Minor GC，Full GC的收集频率更低，耗时更长。

#### 什么是跨代引用，有什么问题？
JVM的跨代引用问题是指在Java堆内存的不同代之间存在引用关系，导致对象在不同代之间的引用被称为跨代引用。比如：新生代到老年代的引用，老年代到新生代的引用等。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755522227897-e31f92e4-a391-457d-8405-2791c75287c3.png)

假如，我们现在JVM的堆是上面这种情况，那么在进行一次MinorGC（YoungGC）的时候，会从GC Root出发，然后进行可达性分析，假如当前正在进行一次Young GC，如果他发现一个对象处于老年代，那么JVM就会中断这条路径。

那么这时候，JVM就会认为只有A和B是可达的，就会在接下来的Young GC中把E回收掉，但是其实E是有引用的，只不过他的引用在老年代，发生了跨代引用。



想要解决这个问题，有两种简单的做法：

1、在做YoungGC的时候，GC Root出发后扫描到老年代对象后不中断，继续扫描和标记，把所有在新生代的对象都标记上。

2、在YoungGC的时候，把老年代的所有对象也作为GC Root，进行可达性分析扫描。



以上两种做法，其实成本都太高了，甚至第一种要比第二种成本还要高，因为他不仅要扫描，还需要不断地做标记。那么，于是就有一个好的办法出现了，那就是定义了一个全局的数据结构——**Remembered Set**。

Remembered Set 的主要作用是跟踪老年代对象与新生代）对象之间的引用关系，以帮助识别老年代中存活对象。他的核心目标是减少全堆扫描的开销。老年代中的对象通常存活更长时间，因此为了回收新生代，JVM需要知道哪些老年代对象引用了新生代对象，以确保不会错误地回收正在被老年代引用的新生代对象。

Remembered Set 记录了老年代对象指向新生代对象的引用关系，此后当发生Minor GC时，**垃圾回收器不需要扫描整个老年代来确定哪些对象存活。它只需扫描Remembered Set中的条目**，从而减少了扫描的开销。



所以，在Remember Set中的对象也会被加入到GC Roots进行扫描：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755522384412-da074a8d-50bf-441a-810f-f54889acc693.png)

而在Remembered Set的实现中，比较常见的一种叫做Card Table。

### 三色标记算法，写屏障
三色标记算法是一种JVM中垃圾标记的算法，他可以减少JVM在GC过程中的STW时长，他是CMS、G1等垃圾收集器中主要使用的标记算法。G1、Shenandoah使用了一种相当于堆内存的1/64大小的，称为BitMap的结构来记录标记信息



在出现三色标记算法之前，JVM中垃圾对象的标记主要采用可达性分析算法及引用计数法。但是这两种算法存在以下问题：

1、**循环引用问题**，如果两个对象互相引用，就形成了一个环形结构，如果采用引用计数法的话，那么这两个对象将永远无法被回收。

2、**STW时间长**，可达性分析的整个过程都需要STW，以避免对象的状态发生改变，这就导致GC停顿时长很长，大大影响应用的整体性能。为了解决上面这些问题，就引入了三色标记法。



为了解决上面这些问题，就引入了**三色标记法**。

**三色标记法将对象分为三种状态：白色、灰色和黑色。**

白色：该对象没有被标记过。

灰色：该对象已经被标记过了，但该对象的引用对象还没标记完。

黑色：该对象已经被标记过了，并且他的全部引用对象也都标记完了。



三色标记法的标记过程可以分为三个阶段：初始标记（Initial Marking）、并发标记（Concurrent Marking）和重新标记（Remark）。

+ **初始标记**：遍历所有的根对象，将根对象和直接引用的对象标记为灰色。在这个阶段中，垃圾回收器只会扫描被直接或者间接引用的对象，而不会扫描整个堆。因此，初始标记阶段的时间比较短。**（Stop The World）**
+ **并发标记**：在这个过程中，垃圾回收器会从灰色对象开始遍历整个对象图，将被引用的对象标记为灰色，并将已经遍历过的对象标记为黑色。并发标记过程中，应用程序线程可能会修改对象图，因此垃圾回收器需要使用写屏障（Write Barrier）技术来保证并发标记的正确性。**（不需要STW）**
+ **重新标记**：重新标记的主要作用是标记在并发标记阶段中被修改的对象以及未被遍历到的对象。这个过程中，垃圾回收器会从灰色对象重新开始遍历对象图，将被引用的对象标记为灰色，并将已经遍历过的对象标记为黑色。**（Stop The World）**

在重新标记阶段结束之后，垃圾回收器会执行清除操作，将未被标记为可达对象的对象进行回收，从而释放内存空间。这个过程中，垃圾回收器会将所有未被标记的对象标记为白色（White）。以上三个标记阶段中，初始标记和重新标记是需要STW的，而并发标记是不需要STW的。其中**最耗时的其实就是并发标记**的这个阶段，因为这个阶段需要遍历整个对象树，而三色标记把这个阶段做到了和应用线程并发执行，**大大降低了GC的停顿时长**。



**以上三个标记阶段中，初始标记和重新标记是需要STW的，而并发标记是不需要STW的。其中最耗时的其实就是并发标记的这个阶段，因为这个阶段需要遍历整个对象树，而三色标记把这个阶段做到了和应用线程并发执行，大大降低了GC的停顿时长。**

<details class="lake-collapse"><summary id="u28df2582"><span class="ne-text" style="color: rgb(0, 0, 0)">扩展知识</span></summary><h2 id="mmnqf"><span class="ne-text" style="color: rgb(0, 0, 0)">写屏障</span></h2><p id="ub0a4775b" class="ne-p"><span class="ne-text">写屏障可以看作在虚拟机层面对“引用类型字段赋值”这个动作的AOP切面，在引用对象赋值时会产生一个环形通知，供程序执行额外的动作。</span></p><p id="u03a6a651" class="ne-p"><span class="ne-text">写前屏障在 </span><strong><span class="ne-text">引用被覆盖之前</span></strong><span class="ne-text"> 触发，可以解决并发标记问题，有两种实现，分别是增量更新和原始快照</span></p><p id="uc8d8306a" class="ne-p"><span class="ne-text">写后屏障在 </span><strong><span class="ne-text">引用被写入之后</span></strong><span class="ne-text"> 触发，可以用于卡表维护(card table, 用于解决跨代引用问题)</span></p><h2 id="5f926276"><span class="ne-text" style="color: rgb(0, 0, 0)">多标的问题</span></h2><p id="ue58f3bc6" class="ne-p"><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">所谓多标，其实就是这个对象原本应该被回收掉的白色对象，但是被错误的标记成了黑色的存活对象。从而导致这个对象没有被 GC 回收掉。</span><span class="ne-text"><br /></span></p><p id="ubf96b284" class="ne-p"><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">这个一般发生在并发标记过程中，该对象还是有引用的，但是在过程中，应用程序执行过程中把他的引用关系删除了，导致他变成了一个垃圾对象。</span></p><p id="u48b5f2a8" class="ne-p"><span class="ne-text"></span></p><p id="u81ff1f17" class="ne-p"><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">多标的话，会产生</span><strong><span class="ne-text" style="color: rgb(0, 0, 0) !important">浮动垃圾</span></strong><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">，这个问题一般都不太需要解决，因为这种垃圾一般都不会太多，另外在下一次 GC 的时候也都能被回收掉。</span></p><h2 id="ce32db8f"><span class="ne-text" style="color: rgb(0, 0, 0)">怎么解决漏标的问题</span></h2><p id="ud6677f39" class="ne-p"><span class="ne-text">所谓漏标，和多标刚好相反，就是说一个对象本来应该是黑色存活对象，但是没有被正确的标记上，导致被错误的垃圾回收掉了。<br /></span></p><p id="u2043c111" class="ne-p"><span class="ne-text">这种情况一旦发生是很危险的，一个正常使用的对象被垃圾回收掉了，这对系统来说是灾难性的问题，那么如何解决呢？</span></p><p id="ucd49e493" class="ne-p"><span class="ne-text"></span></p><p id="u303947de" class="ne-p"><span class="ne-text" style="color: rgba(0, 0, 0, 0.85) !important">具体的解决方式，在 CMS 和 G1 中也不太一样。</span><strong><span class="ne-text" style="color: rgb(0, 0, 0) !important">CMS 采用的是增量更新方案，G1 则采用的是原始快照的方案。</span></strong><span class="ne-text"><br /></span></p><p id="u0e841eda" class="ne-p"><span class="ne-text">漏标的问题想要发生，需要</span><span class="ne-text" style="color: #DF2A3F">同时</span><span class="ne-text">满足两个充要条件：<br /></span></p><p id="ud97126aa" class="ne-p"><span class="ne-text">1、至少有一个黑色对象在自己被标记之后指向了这个白色对象(这样这个白色对象就不会再有机会被标记了)</span></p><p id="ud2410403" class="ne-p"><span class="ne-text">2、所有的灰色对象在自己引用扫描完成之前删除了对白色对象的引用(和上面同理)</span></p><p id="u6b543b39" class="ne-p"><strong><span class="ne-text">简单来说就是一个白色对象被黑色对象引用且没有灰色对象引用这个白色对象</span></strong></p><p id="u0e1c90b0" class="ne-p"><span class="ne-text"></span></p><p id="u69745da5" class="ne-p"><span class="ne-text">那么，增量更新方案就是破坏了第一个条件，而原始快照方案就是破坏了第二个条件。</span></p><h3 id="fba4610c"><span class="ne-text" style="color: rgb(0, 0, 0)">增量更新</span></h3><p id="u76be1f27" class="ne-p"><span class="ne-text">增量更新要破坏的是第一个条件，当黑色对象插入新的指向白色对象的引用关系时，就将这个新插入的引用记录下来，等并发扫描结束之后，再将这些记录过的引用关系中的黑色对象为根，重新扫描一次。这可以简化理解为，黑色对象一旦新插入了指向白色对象的引用之后，它就变回灰色对象了。</span></p><p id="u332df892" class="ne-p"><span class="ne-text">缺点：重新扫描新增的这部分黑色对象，会浪费多一些时间</span></p><h3 id="45bb5303"><span class="ne-text" style="color: rgb(0, 0, 0)">原始快照</span></h3><p id="u014cdd48" class="ne-p"><span class="ne-text">原始快照要破坏的是第二个条件，当灰色对象要删除指向白色对象的引用关系时，就将这个要删除的引用记录下来，在并发扫描结束之后，再将这些记录过的引用关系中的灰色对象为根，重新扫描一次。这也可以简化理解为，无论引用关系删除与否，都会按照刚刚开始扫描那一刻的对象图快照来进行搜索。</span></p><p id="uc1d54098" class="ne-p"><span class="ne-text">缺点：这种方式可能会把本来真的要取消引用的对象给错误的复活了，从而产生浮动垃圾。但是就像前面说的，多标的问题是可以忽略的。</span></p><p id="u5ab74e13" class="ne-p"><span class="ne-text"></span></p><p id="u8cc89d3c" class="ne-p"><span class="ne-text">以上无论是对引用关系记录的插入还是删除，虚拟机的记录操作都是通过</span><strong><span class="ne-text">写屏障</span></strong><span class="ne-text">实现的。在HotSpSpot虚拟机中，增量更新和原始快照这两种解决方案都有实际应用，譬如，CMS是基于增量更新来做并发标记的，G1、Shenandoah则是用原始快照来实现。</span></p><p id="u6dacf5d3" class="ne-p"><span class="ne-text">相比起增量更新算法，原始快照搜索能够减少并发标记和重新标记阶段的消耗，避免CMS那样在最终标记阶段停顿时间过长的缺点，但是在用户程序运行过程中确实会产生由跟踪引用变化带来的额外负担。由于G1对写屏障的复杂操作要比CMS消耗更多的运算资源，所以CMS的写屏障实现是直接的同步操作，而G1就不得不将其实现为类似于消息队列的结构，把写前屏障和写后屏障中要做的事情都放到队列里，然后再异步处理。</span></p></details>
## JVM 中一次完整的 GC 流程是怎样的？
一次完整的GC流程大致如下，基于JDK 1.8：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755620924916-86a05937-efec-4eb3-b7e1-77f78ad6c669.png)

一般来说，GC的触发是在对象分配过程中，当一个对象在创建时，他会根据他的大小决定是进入新生代或者老年代。如果他的大小超过`-XX:PretenureSizeThreshold`就会被认为是大对象，直接进入老年代，否则就会在新生代进行创建。（PretenureSizeThreshold默认是0，也就是说，默认情况下对象不会提前进入老年代，而是直接在新生代分配。然后就GC次数和基于动态年龄判断来进入老年代。）

在新生代创建对象，会发生在Eden区，但是这个时候有可能会因为Eden区内存不够，这时候就会尝试触发一次YoungGC。（会在YoungGC前做一次空间分配担保，如果失败可能直接触发FullGC）

新生代采用的是标记复制算法，主要分为，标记、复制、清除三个步骤，会从GC Root开始进行存活对象的标记，然后把Eden区和Survivor区复制到另外一个Survivor区。然后再把Eden和From Survivor区的对象清理掉。

这个过程，可能会发生两件事情，第一个就是Survivor有可能存不下这些存活的对象，这时候就会进行空间分配担保。如果担保成功了，那么就没什么事儿，正常进行Young GC就行了。但是如果担保失败了，说明老年代可能也不够了，这时候就会触发一次FullGC了。

还会发生第二件事情就是，在这个过程中，会进行对象的年龄判断，如果他经过一定次数的GC之后，还没有被回收，那么这个对象就会被放到老年代当中去。

而老年代如果不够了，或者担保失败了，那么就会触发老年代的GC，一般来说，现在用的比较多的老年代的垃圾收集器是CMS或者G1，他们采用的都是三色标记法。

也就是分为四个阶段：初始标记、并发标记、重新标记、及并发清理。

老年代在做FullGC之后，如果空间还是不够，那就要触发OOM了。

## 空间分配担保机制
如果Survivor区域的空间不够，就要分配给老年代，也就是说，老年代起到了一个兜底的作用。但是，老年代也是可能空间不足的。所以，在这个过程中就需要做一次**空间分配担保**：

在每一次执行YoungGC之前，**虚拟机会检查老年代最大可用的连续空间是否大于新生代所有对象的总空间**。

**如果大于**，那么说明本次Young GC是安全的。

**如果小于**，那么虚拟机会查看 HandlePromotionFailure 参数设置的值判断是否允许担保失败。如果值为true，那么会继续**检查老年代最大可用连续空间是否大于历次晋升到老年代的对象的平均大小**（一共有多少对象在内存回收后存活下来是不可预知的，因此只好取之前每次垃圾回收后晋升到老年代的对象大小的平均值作为参考）。如果大于，则尝试进行一次YoungGC，但这次YoungGC依然是有风险的；如果小于，或者HandlePromotionFailure=false，则会直接触发一次Full GC。

**但是，需要注意的是HandlePromotionFailure这个参数，在JDK 7中就不再支持了：**

也就是说，在后续的版本中， 只要检查老年代最大可用连续空间是否大于历次晋升到老年代的对象的平均大小，如果大于，则认为担保成功。

但是需要注意的是，担保的结果可能成功，也可能失败。所以，在YoungGC的复制阶段执行之后，会发生以下三种情况：

+ 剩余存活对象的大小，小于Survivor区，那就直接进入Survivor区。
+ 剩余存活对象的大小，大于Survivor区，小于老年代可用内存，那就直接去老年代。
+ 剩余存活对象的大小，大于Survivor并且大于老年代，触发"FullGC"。

## YoungGC和FullGC的触发条件是什么？
**YoungGC的触发条件比较简单，那就是当新生代中的eden区分配满的时候就会触发。**

****

**FullGC的触发条件**比较复杂也比较多，主要以下几种：

+ 老年代空间不足
    - 创建一个大对象，超过指定阈值会直接保存在老年代当中，如果老年代空间也不足，会触发Full GC。
    - YoungGC之后，发现要移到老年代的对象，老年代存不下的时候，会触发一次FullGC
+ 空间分配担保失败
    - 当准备要触发一次YoungGC时，会进行空间分配担保，在担保过程中，发现虚拟机会检查老年代最大可用的连续空间小于新生代所有对象的总空间，但是HandlePromotionFailure=false，那么就会触发一次FullGC（HandlePromotionFailure 这个配置，在JDK 7中并不在支持了，这一步骤在该版本已取消）
    - 当准备要触发一次YoungGC时，会进行空间分配担保，在担保过程中，发现虚拟机会检查老年代最大可用的连续空间小于新生代所有对象的总空间，但是HandlePromotionFailure=true，继续检查发现老年代最大可用连续空间小于历次晋升到老年代的对象的平均大小时，会触发一次FullGC
+ 永久代空间不足
    - 如果有永久代的话，当在永久代分配空间时没有足够空间的时候，会触发FullGC
+ 代码中执行System.gc()
    - 代码中执行System.gc()的时候，会触发FullGC，但是并不保证一定会立即触发。

## FullGC多久一次算正常？
[https://www.yuque.com/hollis666/vn7dy3/nks48167c9upybbo](https://www.yuque.com/hollis666/vn7dy3/nks48167c9upybbo)

一般来说，日常情况，FullGC不应该超过一周一次的这个频率。

## 为什么初始标记和重新标记需要STW，而并发标记不需要？
[https://www.yuque.com/hollis666/vn7dy3/acz9pk5h7waamrbe](https://www.yuque.com/hollis666/vn7dy3/acz9pk5h7waamrbe)

## 并发回收和并行回收
并发指的是一个CPU不断的通过时间片切换来执行不同的任务，并行是指多个CPU同时执行不同的任务。

并行回收其实就是Parallel GC，具体到垃圾收集器的实现上，就是Parallel Scavenge，Parallel Old，ParNew等收集器。而并发回收比较典型的就是CMS，还有G1也是并发回收器

**并行回收器主要的关注目标是吞吐量**（吞吐量=代码运行时间/(代码运行时间+垃圾收集时间)，所以他会想尽办法最高效率的利用CPU时间来进行垃圾回收。所以他会在垃圾收集期间通常会暂停应用程序的执行（STW），以快速完成垃圾收集。

**并发回收器主要关注的目标是STW的时长**，它允许垃圾收集线程在应用程序线程运行的同时执行部分垃圾收集工作，从而减少了STW的时间。并发回收期间，只有在特定的收集阶段会发生短暂的STW。所以，并发回收器减少了应用程序的停顿时间，适用于需要较低延迟的应用场景。

在适用场景上，并行垃圾回收适用于对停顿时间（STW时间）要求不是非常严格，但希望最大化吞吐量的应用。而并发垃圾回收适用于对响应时间敏感的应用，如在线交易处理系统、游戏服务器等，这些应用需要尽可能减少GC引起的停顿。

# 垃圾收集器
## 各种垃圾收集器
常见的垃圾回收器如下：  

1. 串行垃圾回收器（Serial Garbage Collector）如：Serial GC，Serial Old  
2. 并行垃圾回收器（Parallel Garbage Collector）如：Parallel Scavenge，Parallel Old，ParNew  
3. 并发标记扫描垃圾回收器（CMS Garbage Collector）  
4. G1垃圾回收器（G1 Garbage Collector，JDK 7中推出，JDK 9中设置为默认）  
5. ZGC垃圾回收器（The Z Garbage Collector，JDK 11 推出）



新生代收集器有**Serial、ParNew、Parallel Scavenge**；  

老年代收集器有**Serial Old、Parallel Old、CMS**。  

整堆收集器有**G1、ZGC**

****

### 串行垃圾收集器
1. Serial GC

**Serial是单线程的串行垃圾回收器，主要采用**`**标记-复制算法**`**进行垃圾回收。**

单线程地好处就是减少上下文切换，减少系统资源的开销。但这种方式的缺点也很明显，在GC的过程中，必须暂停其他所有的工作线程，直至Serial收集器收集结束为止（Stop The World）。若GC不是频繁发生，这或许是一个不错的选择，否则将会影响程序的执行性能。

2. Serial Old

**Serial Old是Serial的老年代版本，也是个单线程收集器，适用于老年代，使用的是**`**标记-整理算法**`**。**

优缺点基本和Serial差不多，二者主要是回收算法不一样。

### 并行垃圾收集器
“并行”主要体现在 **GC 线程之间的并行**，不是应用线程和 GC 线程并发（仍然是 **Stop-The-World** 的收集器）。  

1. ParNew

**ParNew其实就是Serial的多线程版本，在参数、回收算法上，和Serial是完全一样的，所以他也是采用**`**标记-复制算法**`**进行垃圾回收的。**

ParNew在垃圾回收的时候，同样会STW，但是因为它是多线程并行进行垃圾回收的，所以通常情况下时间会比Serial短一些。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755795870036-e0677253-7ac2-4c28-b499-0a6e1c5b2b10.png)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755797610434-c906361d-b841-4b9e-ad7c-6e017c27cec3.png)

2. Parallel Scavenge

**Parallel Scavenge 也是一个新生代的垃圾回收器，和ParNew一样，他也是多线程并行执行的，同样采用的也是**`**标记-复制算法**`**。**与ParNew最大的不同是，Parallel Scavenge 关注的是垃圾回收的吞吐量，以吞吐量优先。

所谓吞吐量就是处理器用于运行用户代码的时间与处理器总消耗时间的比值，即：

**吞吐量=代码运行时间/(代码运行时间+垃圾收集时间)**

+ **停顿越短 → 年轻代越小 → GC 更频繁(总垃圾收集时间越长) → 吞吐量可能下降**
+ **停顿允许略长 → 年轻代可以更大 → GC 不频繁 → 吞吐量提高**

Parallel Scavenge收集器的高吞吐量可以最高效率的利用CPU时间

Parallel Scavenge 收集器提供了两个参数用于精确控制吞吐量，分别是控制最大垃圾收集停顿时间的-XX：MaxGCPauseMillis参数以及直接设置吞吐量大小的-XX：GCTimeRatio 参数。

+ -XX：MaxGCPauseMillis 参数允许的值是一个大于0的毫秒数，控制单次垃圾回收停顿时间的目标值。 JVM 会尽量调整年轻代大小来接近这个停顿时间。默认值为4294967295(最大无符号int)
+ -XX：GCTimeRatio参数的值则应当是一个大于0小于100的整数，也就是垃圾收集时间占总时间的比率，相当于吞吐量的倒数。譬如把此参数设置为19，那允许的最大垃圾收集时间就占总时间的5%（即1/(1+19)），默认值为99，即允许最大1%（即1/(1+99)）的垃圾收集时间。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755796806415-47698f57-c5ef-438c-8fad-eab603338d69.png)

<details class="lake-collapse"><summary id="u7e5462ad"><span class="ne-text">官方文档</span></summary><p id="ub15ce0e4" class="ne-p"><a href="https://docs.oracle.com/javase/1.5.0/docs/guide/vm/gc-ergonomics.html" data-href="https://docs.oracle.com/javase/1.5.0/docs/guide/vm/gc-ergonomics.html" target="_blank" class="ne-link"><span class="ne-text">https://docs.oracle.com/javase/1.5.0/docs/guide/vm/gc-ergonomics.html</span></a></p><h1 id="dluH6"><span class="ne-text">垃圾收集器自适应调整</span></h1><p id="uc7103f1f" class="ne-p"><span class="ne-text">以下更改从 J2SE 5.0 开始生效。</span></p><p id="u801923e9" class="ne-p"><span class="ne-text">在运行服务器 VM 的服务器级机器上，垃圾收集器（GC）已从之前的串行收集器（</span><code class="ne-code"><span class="ne-text">-XX:+UseSerialGC</span></code><span class="ne-text">）改为并行收集器（</span><code class="ne-code"><span class="ne-text">-XX:+UseParallelGC</span></code><span class="ne-text">）。你可以通过在 </span><code class="ne-code"><span class="ne-text">java</span></code><span class="ne-text"> 命令后添加 </span><code class="ne-code"><span class="ne-text">-XX:+UseSerialGC</span></code><span class="ne-text"> 命令行选项来覆盖此默认设置。</span></p><p id="u21eb21da" class="ne-p"><span class="ne-text">在运行任一 VM（客户端或服务器）且使用并行垃圾收集器（</span><code class="ne-code"><span class="ne-text">-XX:+UseParallelGC</span></code><span class="ne-text">）的服务器级机器上，初始堆大小和最大堆大小的变化如下：</span></p><p id="u6ce54849" class="ne-p"><span class="ne-text">初始堆大小：<br /></span><span class="ne-text">机器物理内存的 1/64 或某个合理最小值中的较大者。在 J2SE 5.0 之前，默认初始堆大小是一个合理的最小值，具体值因平台而异。你可以使用 </span><code class="ne-code"><span class="ne-text">-Xms</span></code><span class="ne-text"> 命令行选项覆盖此默认值。</span></p><p id="u8798fccc" class="ne-p"><span class="ne-text">最大堆大小：<br /></span><span class="ne-text">物理内存的 1/4 或 1GB 中的较小者。在 J2SE 5.0 之前，默认最大堆大小为 64MB。你可以使用 </span><code class="ne-code"><span class="ne-text">-Xmx</span></code><span class="ne-text"> 命令行选项覆盖此默认值。</span></p><p id="uce5e0e09" class="ne-p"><br></p><p id="u713c8b52" class="ne-p"><span class="ne-text">注意：所给出的堆大小的边界和比例适用于 J2SE 5.0。随着计算机性能的提升，在后续版本中这些值可能会有所不同。</span></p><p id="u6384900d" class="ne-p"><span class="ne-text">如果并行垃圾收集器（</span><code class="ne-code"><span class="ne-text">-XX:+UseParallelGC</span></code><span class="ne-text">）花费过多时间来回收堆中少量内存，将会抛出内存不足异常。为避免此异常，你可以增大堆的大小。你还可以设置时间限制（</span><code class="ne-code"><span class="ne-text">-XX:GCTimeLimit=</span></code><span class="ne-text">）和空间限制（</span><code class="ne-code"><span class="ne-text">-XX:GCHeapFreeLimit=</span></code><span class="ne-text">）参数，其中：</span></p><p id="u2c5d1fd3" class="ne-p"><span class="ne-text">时间限制：<br /></span><span class="ne-text">垃圾收集所花费的时间占总时间的百分比上限（默认值为 98）。<br /></span><span class="ne-text">空间限制：<br /></span><span class="ne-text">一次垃圾收集中释放的空间占最大堆大小的百分比下限（默认值为 2）。</span></p><p id="ua39358a0" class="ne-p"><span class="ne-text">在使用并行垃圾收集器（</span><code class="ne-code"><span class="ne-text">-XX:+UseParallelGC</span></code><span class="ne-text">）时，默认启用的 </span><code class="ne-code"><span class="ne-text">-XX:+UseAdaptiveSizePolicy</span></code><span class="ne-text"> 实现已调整为考虑三个目标：</span></p><ul class="ne-ul"><li id="uf19c2a62" data-lake-index-type="0"><span class="ne-text">期望的最大 GC 暂停时间目标</span></li><li id="u392d5f63" data-lake-index-type="0"><span class="ne-text">期望的应用程序吞吐量目标</span></li><li id="u0ac42374" data-lake-index-type="0"><span class="ne-text">最小内存占用</span></li></ul><p id="uf0d0f0de" class="ne-p"><strong><span class="ne-text">该实现会按以下顺序进行检查：</span></strong></p><ol class="ne-ol"><li id="uec445dbc" data-lake-index-type="0"><strong><span class="ne-text">如果 GC 暂停时间大于暂停时间目标，则减小分代大小以更好地实现该目标。</span></strong></li><li id="ud4d6337d" data-lake-index-type="0"><strong><span class="ne-text">如果已达到暂停时间目标，则考虑应用程序的吞吐量目标。如果未达到应用程序的吞吐量目标，则增大分代大小以更好地实现该目标。</span></strong></li><li id="uf3afa5df" data-lake-index-type="0"><strong><span class="ne-text">如果既达到了暂停时间目标，又达到了吞吐量目标，则减小分代大小以减少内存占用。</span></strong></li></ol><h2 id="xEDoH"><span class="ne-text">标志</span></h2><p id="u8f35e16a" class="ne-p"><code class="ne-code"><span class="ne-text">-XX:MaxGCPauseMillis=nnn</span></code><span class="ne-text"><br /></span><span class="ne-text">向虚拟机提示期望暂停时间不超过 nnn 毫秒。虚拟机将调整 Java 堆大小和其他与 GC 相关的参数，以尝试使 GC 导致的暂停时间短于 nnn 毫秒。请注意，这可能会导致虚拟机降低整体吞吐量，在某些情况下，虚拟机可能无法达到期望的暂停时间目标。</span></p><p id="u7e0f8fdb" class="ne-p"><span class="ne-text">默认情况下没有暂停时间目标。实现暂停时间目标存在一定的限制。GC 的暂停时间取决于堆中的活动数据量。Minor 收集和 Major 收集对活动数据量的依赖方式不同。使用此参数时应谨慎。</span><strong><span class="ne-text">如果值设置得过小，会导致系统花费过多时间进行垃圾收集。</span></strong></p><p id="uf2c5d4b2" class="ne-p"><br></p><p id="u8fe57a82" class="ne-p"><code class="ne-code"><span class="ne-text">-XX:GCTimeRatio=nnn</span></code><span class="ne-text"><br /></span><span class="ne-text">向虚拟机提示，希望用于垃圾收集的时间不超过应用程序执行时间的 1/(1+nnn)。</span></p><p id="u8b87e275" class="ne-p"><span class="ne-text">例如，</span><code class="ne-code"><span class="ne-text">-XX:GCTimeRatio=19</span></code><span class="ne-text"> 设置的目标是 GC 时间占总时间的 5%，吞吐量目标为 95%。也就是说，应用程序的运行时间应是收集器的 19 倍。</span></p><p id="ue89f0020" class="ne-p"><span class="ne-text">默认值为 99，这意味着应用程序的运行时间应至少是收集器的 99 倍。也就是说，收集器的运行时间不应超过总时间的 1%。这被选为服务器应用程序的理想设置。</span><strong><span class="ne-text">如果值设置得过高，会导致堆大小增长到最大值。</span></strong></p><h2 id="dxzsH"><span class="ne-text">建议策略</span></h2><p id="u66130099" class="ne-p"><span class="ne-text">除非你确定堆大于默认最大堆大小，否则不要为堆选择最大值。选择一个足以满足应用程序需求的吞吐量目标。</span></p><p id="u415aeec0" class="ne-p"><span class="ne-text">在理想情况下，堆会增长到一个（小于最大值的）值，以支持所选的吞吐量目标。</span></p><p id="uc0fdcb2a" class="ne-p"><span class="ne-text">如果堆增长到最大值，在该最大值范围内无法达到吞吐量目标。将最大堆设置为你能设置的最大大小（但不超过平台的物理内存大小），然后重新运行应用程序。如果仍然无法达到吞吐量目标，则该目标对于平台上的可用内存来说过高。</span></p><p id="ub777b141" class="ne-p"><strong><span class="ne-text">如果可以达到吞吐量目标，但暂停时间过长，则选择一个暂停时间目标。这可能意味着无法达到吞吐量目标，因此请选择对应用程序来说可接受的折衷值。</span></strong></p></details>
3. Parallel Old

**Parallel 是 Parallel Scavenge的老年代版本，同样是一个关注吞吐量的并行垃圾收集器，他采用的是**`**标记-整理算法**`**进行垃圾回收的。**

> ** 在 Java 8 HotSpot JVM，默认的垃圾收集器是 Parallel Scavenge + Parallel Old。  **
>

### CMS
**JDK1.5时引入，JDK9被标记弃用，JDK14被移除**

**CMS，Concurrent Mark Sweep，并发标记扫描垃圾回收器，同样是****<font style="color:#DF2A3F;">老年代</font>****的收集器。他也是一个并发执行的垃圾收集器，他和Parallel最大的区别是他更加关注垃圾回收的停顿时间，采用的是耗时更短的**`**标记-清除算法**`**。**

CMS 的目标是回收老年代，所以老年代快满时会触发。 默认在老年代使用率达到 **92%** 时启动 (`-XX:CMSInitiatingOccupancyFraction`)

`<font style="color:#8A8F8D;">-XX:+UseCMSInitiatingOccupancyOnly</font>`<font style="color:#8A8F8D;"> 的作用是：是否只按照你设定的阈值（</font>`<font style="color:#8A8F8D;">-XX:CMSInitiatingOccupancyFraction</font>`<font style="color:#8A8F8D;">）来决定什么时候触发 CMS GC，而不用 JVM 的自适应策略。默认是false， 也就是说，即使你设置了 </font>`<font style="color:#8A8F8D;">-XX:CMSInitiatingOccupancyFraction=75</font>`<font style="color:#8A8F8D;">，JVM 也可能不严格按 75% 来触发，而是根据运行时的 GC 情况进行自适应调整（Adaptive）。  </font>

**CMS采用了三色标记法**，在标记的过程中，只有初始标记和重新标记这两个步骤是STW的，所以，相比其他的收集器整个回收过程都STW来说，他导致的应用停顿时间更短。

优点:

+ 并发
+ 低停顿

缺点：

+ 对CPU非常敏感：在并发阶段虽然不会导致用户线程停顿，但是会因为占用了一部分线程使应用程序变慢
+ 无法处理**浮动垃圾**：在最后一步并发清理过程中，用户线程执行也会产生垃圾，但是这部分垃圾是在标记之后，所以只有等到下一次gc的时候清理掉，这部分垃圾叫浮动垃圾。
+ CMS使用“标记-清理”法会产生大量的**空间碎片**，当碎片过多，将会给大对象空间的分配带来很大的麻烦，往往会出现老年代还有很大的空间但无法找到足够大的连续空间来分配当前对象，不得不提前触发一次FullGC，为了解决这个问题CMS提供了一个开关参数，用于在CMS顶不住，要进行FullGC时开启内存碎片的合并整理过程，但是内存整理的过程是无法并发的，空间碎片没有了但是停顿时间变长了

### G1
`-Xms8g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+PrintGCDetails`

JDK7引入，JDK9取代CMS成为默认垃圾收集器，至少要**4G**内存才推荐使用G1(小堆用 G1，低停顿的优势不突出，反而会消耗更多 CPU 来管理 Region)

G1是一款主要面向服务端应用的垃圾收集器。在G1收集器出现之前的所有其他收集器，包括CMS在内，垃圾收集的目标范围要么是整个新生代 （Minor GC），要么就是整个老年代（Major GC），再要么就是整个Java堆（Full GC）。 而G1跳出了这个樊笼，它可以面向堆内存任何部分来组成回收集（Collection Set，一般简称CSet）进行回收，衡量标准不再是它属于哪个分代，而是哪块内存中存放的垃圾数量最多，回收收益最大，这就是G1收集器的**Mixed GC**模式。



G1 不再坚持固定大小以及固定数量的分代区域划分，而是把连续的Java堆划分为多个大小相等的独立区域（Region），每一个Region都可以根据需要，扮演新生代的Eden空间、 Survivor 空间，或者老年代空间。收集器能够**对扮演不同角色的Region采用不同的策略去处理**

****

Region 中还有一类特殊的**Humongous**区域，专门用来存储大对象。G1认为只要大小超过了一个Region容量一半的对象即可判定为大对象。每个Region的大小可以通过参数-XX：G1HeapRegionSize设定(默认为为0，表示<font style="color:rgb(64, 64, 64);">通过heapSize/2048计算得到</font>)，取值范围为1MB～32MB，且应为2的N次幂。而对于**那些超过了整个Region容量的超级大对象，将会被存放在N个连续的Humongous Region之中**，G1的大多数行为都把Humongous Region作为老年代的一部分来进行看待。



虽然G1仍然保留新生代和老年代的概念，但新生代和老年代不再是固定的了，它们都是一系列区域（不需要连续）的动态集合。G1收集器之所以能建立**可预测的停顿时间模型**，是因为它将Region作为单次回收的最小单元，即每次收集到的内存空间都是Region大小的整数倍，这样可以有计划地**避免在整个Java堆中进行全区域的垃圾收集**。更具体的处理思路是让G1收集器去跟踪各个Region里面的垃圾堆积的“价值”大小，价值即回收所获得的空间大小以及回收所需时间的经验值，然后在后台维护一个优先级列表，**每次根据用户设定允许的收集停顿时间（使用参数**`**-XX：MaxGCPauseMillis**`**指定，默认值是200毫秒）**，优先处理回收价值收益最大的那些Region。

:::color3
收集停顿时间不能设置得太小

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755806815300-843fa5ff-5eec-4414-bb6f-5156dbb69890.png)

:::



三色标记法：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755806136584-aaf97c88-a049-4878-80a9-40fb0e9e9acf.png)



G1 收集器有以下细节：  

+ 每个Region都有自己的记忆集(卡表)，这些记忆集会记录下别的Region指向自己的指针，并标记这些指针分别在哪些卡页的范围之内，用于解决跨代引用问题  
+ 通过 **原始快照** 解决 **并发标记阶段** 线程与用户线程同时进行带来的对象引用关系改变问题  
+ 程序要继续运行就肯定会持续有新对象被创建，G1为每一个Region设计了两个名为TAMS（Top at Mark Start）的指针，把 Region中的一部分空间划分出来用于并发回收过程中的新对象分配，并发回收时新分配的对象地址都必须要在这两个指针位置 以上。G1收集器默认在这个地址以上的对象是被隐式标记过的，即默认它们是存活的，不纳入回收范围。  
+ 三种GC模式，Young GC、Mixed GC和Full GC  
    - 当Eden区的内存空间不够新对象的内存分配时，触发Young GC，采用标记-复制算法  
    - 随着时间的推移，当总堆占有率达到预设阈值（-XX:InitiatingHeapOccupancyPercent默认45%）时，就会触发**并发标记周期**（Concurrent Marking Cycle）， 标记完后当垃圾在整个堆中的占比大于`-XX:G1HeapWastePercent(默认5%)`时，G1会触发**混合回收**（Mixed GC）。这种回收会处理所有年轻代和部分老年代的对象以及大对象区。混合回收采用**复制算法**来完成，确保高效的内存回收。**如果你的项目没有大的cpu负载压力，可以适当降低这个值**，带来的好处就是提前开始Concurrent Marking Cycle Phases ，进一步来说，回收 年轻代 and 老年代 也会提前开始，这样有利于防止年轻代晋升老年代失败(老年代容量不足)而触发Full GC，降低Full GC的频率。
    - 如果内存回收的速度赶不上内存分配的速度，G1收集器也要被迫冻结用户线程执行，导致Full GC而产生长时间“Stop The World”。

优点：

+ **可预测的停顿时间**， G1 会根据目标停顿时间动态决定每次回收多少 Region，从而避免过长的 GC 停顿。  
+ **混合回收**，降低full gc频率
+ **并发标记**，降低stw时间
+ **分Region的内存布局， **这些 Region 可以动态地充当年轻代、老年代甚至大对象区。多个 GC 线程可以并行处理不同的 Region。避免内存碎片。
+ G1从整体来看是基于“标记-整理”算法实现的收集器，但从局部（两个Region之间）上看又是基于“标记-复制”算法实现，无论如何，这两种算法都意味着G1运作期间不会产生内存空间碎片，垃圾收集完成之后能提供规整的可用内存。这种特性有利于程序长时间运行，在程序为大对象分配内存时不容易因无法找到连续内存空间而提前触发下一次收集。

缺点：

+ **卡表占用内存高**

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755808231937-801e4799-0f45-46f2-90cf-751d53017b56.png)

+ 相比起增量更新算法，原始快照搜索能够减少并发标记和重新标记阶段的消耗，避免CMS那样在最终标记阶段停顿时间过长的缺点，但是在用户程序运行过程中确实会产生由跟踪引用变化带来的额外负担。由于**G1对写屏障的复杂操作要比CMS消耗更多的运算资源**，所以CMS的写屏障实现是直接的同步操作，而G1就不得不将其实现为类似于消息队列的结构，把写前屏障和写后屏障中要做的事情都放到队列里，然后再异步处理。

| 特性 | CMS | G1 |
| --- | --- | --- |
| 回收位置 | 老年代 | 整堆 |
| GC算法 | 标记-清除算法 | 标记-复制算法回收年轻代      标记-整理算法回收老年代 |
| 垃圾识别算法 | 三色标记法——增量更新解决漏标 | 三色标记法——原始快照解决漏标 |
| 碎片产生 | 存在内存碎片 | 可防止内存碎片产生 |
| 可预测性 | 无法预测 | G1的STW时长可预测 |
| 堆内存基本要求 | 一般要求不高 | 4G以上 |
| 自适应调优 | 不支持 | 支持 |


### Shenandoah
Shenandoah收集器：RedHat开发的收集器，捐赠给OpenJDK，在OracleJDK中没有，主要是改进了G1收集器在回收阶段无法与用户进程并发的问题

> Shenandoah和ZGC，几乎整个工作过程全部都是并发的，只有初始标记、最终标记这些阶段有短暂的停顿，这部分停顿的时间基本上是固定的，与堆的容量、堆中对象的数量没有正比例关系。实际上，它们都可以在任意可管理的（譬如现在 ZGC只能管理4TB以内的堆）堆容量下， 实现垃圾收集的停顿都不超过十毫秒这种以前听起来是天方夜谭、匪夷所思的目标。这两款目前仍处于实验状态的收集器，被官方命名为“低延迟垃圾收集器”
>

### ZGC
[https://tech.meituan.com/2020/08/06/new-zgc-practice-in-meituan.html](https://tech.meituan.com/2020/08/06/new-zgc-practice-in-meituan.html)

[https://www.bilibili.com/video/BV1RL41137](https://www.bilibili.com/video/BV1RL41137Lw/?spm_id_from=333.337.search-card.all.click)

[https://zhuanlan.zhihu.com/p/673971811](https://zhuanlan.zhihu.com/p/673971811)

jdk 11中新增了一种垃圾收集器ZGC

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755854715668-01e52702-dd31-40fa-96ca-bdb7599c84d7.png)

ZGC收集器有一个标志性的设计是它采用的染色指针技术（Colored Pointer，其他类似的技术中可能将它称为Tag Pointer或者Version Pointer）。<font style="color:rgba(0, 0, 0, 0.85) !important;background-color:rgba(0, 0, 0, 0);">HotSpot 虚拟机的几种收集器有不同的标记实现方案，有的把标记直接记录在对象头上（如 Serial 收集器），有的把标记记录在与对象相互独立的数据结构上（如 G1、Shenandoah 使用了一种相当于堆内存的 1/64 大小的，称为 BitMap 的结构来记录标记信息），而 ZGC 的</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;background-color:rgba(0, 0, 0, 0);">染色指针</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;background-color:rgba(0, 0, 0, 0);">是最直接的、最纯粹的，它直接把标记信息记在引用对象的指针上，这时，与其说可达性分析是遍历对象图来标记对象，还不如说是遍历 “引用图” 来标记 “引用” 了。</font>

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755855336629-de7d4525-cdf4-4c26-8e8f-ecd8553f6713.png)

**<font style="color:rgba(0, 0, 0, 0.85) !important;">染色指针是一种直接将少量额外的信息存储在指针上的技术</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">，可是为什么指针本身也可以存储额外信息呢？在 64 位系统中，理论可以访问的内存高达 16EB（2 的 64 次幂）字节。实际上，基于需求（用不到那么多内存）、性能（地址越宽在做地址转换时需要的页表级数越多）和成本（消耗更多晶体管）的考虑，在 AMD64 架构中只支持到 52 位（4PB）的地址总线和 48 位（256TB）的虚拟地址空间，所以目前 64 位的硬件实际能够支持的最大内存只有 256TB。此外，操作系统一侧也还会施加自己的约束，64 位的 Linux 则分别支持 47 位（128TB）的进程虚拟地址空间和 46 位（64TB）的物理地址空间，64 位的 Windows 系统甚至只支持 44 位（16TB）的物理地址空间。</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">尽管 Linux 下 64 位指针的高 18 位不能用来寻址，但剩余的 46 位指针所能支持的 64TB 内存在今天仍然能够充分满足大型服务器的需要。鉴于此，ZGC 的染色指针技术继续盯上了这剩下的 46 位指针宽度，</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">将其高 4 位提取出来存储四个标志信息</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">。通过这些标志位，</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">虚拟机可以直接从指针中看到其引用对象的三色标记状态</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">、是否进入了重分配集（即被移动过）、是否只能通过 finalize () 方法才能被访问到。当然，由于这些标志位进一步压缩了原本就只有 46 位的地址空间，也</font>**<font style="color:rgba(0, 0, 0, 0.85) !important;">直接导致 ZGC 能够管理的内存不可以超过 4TB（2 的 42 次幂）。</font>**



<font style="color:rgb(25, 27, 31);">ZGC（Garbage Collector）是一种在JVM中实现的低停顿时间垃圾回收器。其垃圾回收流程可以概括为以下几个关键阶段：</font>

1. **<font style="color:rgb(25, 27, 31);">初始标记阶段（Initial Mark）：</font>**<font style="color:rgb(25, 27, 31);">与G1垃圾回收器一样，初始标记阶段是一个短暂的STW阶段，目的是标记出根对象直接引用的对象，标记的过程是并发执行的，所以这个阶段的停顿时间很短。  
</font>
2. **<font style="color:rgb(25, 27, 31);">并发标记阶段（Concurrent Mark）：</font>**<font style="color:rgb(25, 27, 31);">在这个阶段，ZGC并发地标记出所有可达的对象，包括从根对象出发的引用链上的对象，这个过程是与应用程序的执行同时进行的，因此对停顿时间的影响很小。  
</font>
3. **<font style="color:rgb(25, 27, 31);">再标记阶段（Remark）：</font>**<font style="color:rgb(25, 27, 31);">如果在并发标记阶段有新的对象被创建或有对象被回收，ZGC可能需要进行一次短暂的STW再标记。这个阶段的停顿时间一般不超过1毫秒。在这个阶段，ZGC会修正并发标记阶段可能由于并发引起的标记不一致。  
</font>
4. **<font style="color:rgb(25, 27, 31);">并发转移准备（Concurrent Prepare for Relocate）：</font>**<font style="color:rgb(25, 27, 31);">在这个阶段ZGC进行整堆扫描，确定收集哪些Region，并将这些Region组成重分配集（Relocation Set）。与G1收集器不同，ZGC的重分配集扫描所有的Region，而不是计算最有价值回收的Region，由于染色指针的存在，扫描过程会很快。这个过程并不是为了计算最优的回收集，而是为了确定存活对象将被复制到其他Region。此阶段还涉及到JDK12支持的类卸载和弱引用的处理。  
</font>
5. **<font style="color:rgb(25, 27, 31);">初始转移阶段（Initial Relocation）：</font>**<font style="color:rgb(25, 27, 31);">这是ZGC垃圾回收的核心阶段之一，ZGC并发地将重分配集中的存活对象复制到新的Region。为了记录从旧对象到新对象的转移关系，ZGC需要为重分配集中的每个Region维护一个转发表（Forward Table）。  
</font><font style="color:rgb(25, 27, 31);">ZGC收集器能仅从引用上就明确得知一个对象是否处于重分配集之中，如果用户线程此时并发访问了位于重分配集中的对象，这次访问将会被预置的内存屏障(</font>**<font style="color:rgb(25, 27, 31);">读屏障</font>**<font style="color:rgb(25, 27, 31);">)所截获，然后立即根据Region上的转发表记录将访问转发到新复制的对象上，并同时修正更新该引用的值，使其直接指向新对象，ZGC将这种行为称为指针的“自愈”（Self-Healing）能力  
</font><font style="color:rgb(25, 27, 31);">ZGC的染色指针因为“自愈”（Self-Healing）能力，所以只有第一次访问旧对象会变慢，而Shenandoah的Brooks转发指针是每次都会变慢。 一旦重分配集中某个Region的存活对象都复制完毕后，这个Region就可以立即释放用于新对象的分配，但是转发表还得留着不能释放掉，因为可能还有访问在使用这个转发表。  
</font>
6. **<font style="color:rgb(25, 27, 31);">并发转移阶段（Concurrent Relocation）：</font>**<font style="color:rgb(25, 27, 31);">并发转移阶段的工作就是修正堆中指向重分配集中旧对象的所有引用，也可以直接认为就是真正进行对象引用修复的一个步骤，从这一点来看shenandoah的并发引用更新阶段是一样的。但是ZGC并不需要马上完成这个操作（因为有指针自愈的特性），ZGC把并发重映射阶段要做的工作巧妙的合并到</font>**<font style="color:rgb(25, 27, 31);">下一次垃圾收集循环</font>**<font style="color:rgb(25, 27, 31);">中的并发标记阶段中去完成，这样做的好处是节省遍历对象图的开销。一旦所有指针修复，新旧对象的引用关系</font>**<font style="color:rgb(25, 27, 31);">转发表</font>**<font style="color:rgb(25, 27, 31);">就可以释放了。</font>



| **<font style="color:rgb(25, 27, 31);">优点</font>** | **<font style="color:rgb(25, 27, 31);">描述</font>** |
| --- | --- |
| 1. 低停顿 | ZGC以低停顿为首要目标，几乎所有垃圾回收过程都是并发的，只有初始标记、最终标记这些阶段有短暂的停顿，这部分停顿的时间基本上是固定的(10ms以内)，与堆的容量、堆中对象的数量没有正比例关系。 |
| 2. 高吞吐量 | ZGC在吞吐量方面取得了显著进展，超越了G1，接近Parallel Scavenge。 |
| 3. 内存小 | ZGC不通过原始快照或增量更新来实现并发标记，而是通过染色指针，不需要通过写屏障来维护记忆集(卡表)，而记忆集要占用大量内存空间，写屏障也对正常程序运行造成额外负担 |
| 4. 局部内存分配 | 在多核处理器的某些架构下，优先在当前线程所处的处理器的本地内存上分配对象。 |
| 5. 并发停顿 | 并发停顿非常短暂，大部分过程都是与应用线程并发执行。 |
| 6. 无分代 | 没有引入分代的概念，简化了内存管理的复杂性。 |
| 7. 无内存碎片 | 采用并发的标记-整理算法，没有内存碎片问题。 |


| **<font style="color:rgb(25, 27, 31);">缺点</font>** | **<font style="color:rgb(25, 27, 31);">描述</font>** |
| :--- | :--- |
| <font style="color:rgb(25, 27, 31);">1. 浮动垃圾</font> | <font style="color:rgb(25, 27, 31);">承受的对象分配速率不会太高，产生浮动垃圾，难以及时回收。</font> |
| <font style="color:rgb(25, 27, 31);">2. 执行时间较长</font> | <font style="color:rgb(25, 27, 31);">停顿时间短，但整个垃圾回收过程的执行时间可能较长。</font> |
| <font style="color:rgb(25, 27, 31);">3. 无分代概念</font> | <font style="color:rgb(25, 27, 31);">没有分代概念，可能导致朝生夕死的对象无法及时回收。</font> |
| <font style="color:rgb(25, 27, 31);">4. 平台限制</font> | <font style="color:rgb(25, 27, 31);">目前仅在Linux/x64上可用，可能限制了在其他平台的应用。</font> |


## Java 8 和 Java 11 的GC有什么区别？
首先，在垃圾收集器上面，**Java 8 中默认的是Parallel Scavenge GC+Parallel Old GC**，分别用来做新生代和老年代的垃圾回收。而在**Java 11中默认的是采用G1进行整堆回收**（Java 9中就是默认的了）。

另外，**Java 11中还新增了一种垃圾收集器，那就是ZGC**，他可以在保证高吞吐量的同时保证最短的暂停时间。

在垃圾识别及回收上面，**Java 8基于的是单纯地可达性分析，而Java 11中的G1采用的是三色标记法，可以大大降低STW的时长。**

另外，G1的内存划分是自适应的，它会根据堆的大小和使用情况来动态调整各个区域的大小和比例。而Parallel Scavenge GC+Parallel Old GC虽然也可以自适应划分内存，但是不够灵活。

## 项目中如何选择垃圾回收器？
1. 串行垃圾回收器（Serial Garbage Collector） 如：Serial GC，Serial Old  
    - 单线程垃圾收集器，适用于**单核**机器。  
    - 暂停时间较长，不适用于多核环境和大内存应用。
2. 并行垃圾回收器（Parallel Garbage Collector） 如：Parallel Scavenge，Parallel Old，ParNew  
    - 多线程垃圾收集器，适用于多核机器。
    - 暂停时间较长，不适合需要低延迟的应用。
3. 并发标记扫描垃圾回收器（CMS Garbage Collector）  
    - 低延迟垃圾收集器，适合需要响应时间较短、高响应性要求的应用。  
    - **容易产生碎片**，可能需要Full GC进行碎片整理，Full GC时会有较长暂停。
4. G1垃圾回收器（G1 Garbage Collector，JDK 7中推出，JDK 9中设置为默认）  
    - 设计用于替代CMS，适合大内存、多核环境。较低的暂停时间，能够预测性地控制暂停时间，适合大数据量应用。
5. ZGC垃圾回收器（The Z Garbage Collector，JDK 11 推出）、Shenandoah GC（JDK 12 推出）  
    - 超低延迟垃圾收集器，适用于超大堆内存。暂停时间通常在10ms以下，适合对响应时间有极高要求的应用。  
    - 目前只支持较新的JDK版本，可能存在一些不成熟的特性。

综上，在进行选择的时候，按照以下步骤：

1、根据机器情况判断，如果是单核机器，或者内存较小的机器，则选择Serial GC。

2、根据业务类型判断，看你的应用更在意的是吞吐量还是 STW 的时长。比如批处理任务的应用，更在意的就是吞吐量，而实时交易系统，更在意的就是 STW 的时长。

3、根据机器分配的堆内存大小进行判断，一把来说，我们认为至少达到4G 以上才可以用 G1、ZGC 等，通常要比如超过8G、16G 这样效果才更好。

4、根据 JDK 版本进行判断，不同的版本支持的垃圾收集器不一样。可以参考以下的选择方式（但是，并不绝对，尤其是 ZGC 和Shenandoah GC 的选择，其实还是要慎重，毕竟他们的稳定性各方面还有待验证）：

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755884761473-1b029c86-98f4-4167-a730-4cb42b0507fd.png)

# 调优
## [JDK11-G1收集器调优](https://zhuanlan.zhihu.com/p/338682253)
## 常用的JVM启动参数(待补充)
JVM的启动参数有很多，但是我们平常能用上的并不是特别多，这里介绍几个我们常用的：

1. 堆设置:

+ `-Xms`：设置堆的初始大小。  
+ `-Xmx`：设置堆的最大大小。

2. 栈设置:

+ `-Xss`：设置每个线程的栈大小。

3. 垃圾回收器设置:

+ `-XX:+UseG1GC`：使用 G1 垃圾回收器。  
+ `-XX:+UseParallelGC`：使用并行垃圾回收器。

4. 性能调优:

+ `-XX:PermSize` 和 `-XX:MaxPermSize`：在 Java 8 之前设置永久代的初始大小和最大大小。  
+ `-XX:MetaspaceSize` 和 `-XX:MaxMetaspaceSize`：在 Java 8 及以上版本设置 Metaspace 的初始大小和最大大小。  
+ `-XX:+PrintGCDetails`：打印垃圾回收的详细信息。

5. 调试和分析:

+ `-verbose:gc`：输出垃圾回收的详细信息。  
+ `-XX:+HeapDumpOnOutOfMemoryError`：在内存溢出时生成堆转储。

# 其他
## Java是编译型还是解释型?
编译性：Java源代码首先被编译成字节码，JIT 会把编译过的机器码保存起来,以备下次使用。

解释性：JVM中一个方法调用计数器，当累计计数大于一定值的时候，就使用JIT进行编译生成机器码文件。否则就是用解释器进行解释执行，然后字节码也是经过解释器进行解释运行的。

所以Java既是编译型也是解释性语言，默认采用的是解释器和编译器混合的模式。

## JIT优化技术
我们知道，想要把高级语言转变成计算机认识的机器语言有两种方式，分别是编译和解释，虽然Java转成机器语言的过程中有一个步骤是要编译成字节码，但是，这里的字节码并不能在机器上直接执行。

所以，JVM中内置了解释器(interpreter)，在运行时对字节码进行解释翻译成机器码，然后再执行。

解释器的执行方式是一边翻译，一边执行，因此执行效率很低。为了解决这样的低效问题，HotSpot引入了JIT技术（Just-In-Time，即时编译）。

有了JIT技术之后，JVM还是通过解释器进行解释执行。但是，**当JVM发现某个方法或代码块运行时执行的特别频繁的时候，就会认为这是“热点代码”（Hot Spot Code)。然后JIT会把部分“热点代码”翻译成本地机器相关的机器码，并进行优化，然后再把翻译后的机器码缓存起来，以备下次使用。**

****

**基于计数器的热点探测**（Counter Based Hot Spot Detection)：虚拟机会为每个方法，甚至是代码块建立计数器，统计方法的执行次数，某个方法超过阀值(CompileThreshold，默认为10000)就认为是热点方法，触发JIT编译。

它为每个方法准备了两个计数器：**方法调用计数器**和**回边计数器**。

+ 方法计数器。顾名思义，就是记录一个方法被调用次数的计数器。
+ 回边计数器。是记录方法中的for或者while的运行次数的计数器。

### 编译优化
JIT除了具有缓存的功能外，还会对代码做各种优化。

主要的优化有：[逃逸分析](#x0nyh)、 [锁消除](#Limom)、 锁膨胀、 [方法内联](#LTpPx)、 空值检查消除、 类型检测消除、 公共子表达式消除。只有热点方法会进行这些优化

#### 逃逸分析
对象基于逃逸分析可以有三种状态：全局逃逸（GlobalEscape）、参数逃逸（ArgEscape）和无逃逸（NoEscape）。

1. 全局逃逸（GlobalEscape）：对象超出了方法或线程的范围，比如被存储在**静态字段**或作为**方法的返回值**。
2. 参数逃逸（ArgEscape）：对象被作为参数传递或被参数引用，但在方法调用期间不会全局逃逸。
3. 无逃逸（NoEscape）：对象可以被标量替换，意味着它的内存分配可以从生成的代码中移除。

```java
public static String createStringBuffer(String s1, String s2) {
    StringBuffer sb = new StringBuffer();
    sb.append(s1);
    sb.append(s2);
    return sb.toString();
}
```

如上面的sb，就没有发生逃逸，因为这个对象本身没有作为参数传递，也没有被当做方法返回值，并没有赋值给静态变量。在Java中，不同的逃逸状态影响JIT（即时编译器）的优化策略：  

1. **全局逃逸（GlobalEscape）**：由于对象可能被多个线程访问，全局逃逸的对象一般不适合进行栈上分配或其他内存优化。但JIT可能会进行其他类型的优化，如方法内联或循环优化。
2. **参数逃逸（ArgEscape）**：这种情况下，对象虽然作为参数传递，但不会被方法外部的代码使用。JIT可以对这些对象进行一些优化，例如锁消除  
3. **无逃逸（NoEscape）**：这是最适合优化的情况。JIT可以采取多种优化措施，如在栈上分配内存，消除锁，甚至完全消除对象分配（标量替换）。这些优化可以显著提高性能，减少垃圾收集的压力。



| 优化手段 | 全局逃逸 | 参数逃逸 | 无逃逸 |
| --- | --- | --- | --- |
| 方法内联 | Y | Y | Y |
| 循环优化 | Y | Y | Y |
| 锁消除 | N | Y | Y |
| 栈上分配 | N | N | Y |


总的来说，JIT编译器根据对象的逃逸状态采用不同的优化策略，以提高Java程序的性能和效率。



**<font style="color:rgb(0, 0, 0);">开启逃逸分析</font>**

<font style="color:rgba(0, 0, 0, 0.85) !important;">在 Java 代码运行时，通过 JVM 参数可指定是否开启逃逸分析，</font>

+ `<font style="color:rgb(0, 0, 0);">-XX:+DoEscapeAnalysis</font>`<font style="color:rgba(0, 0, 0, 0.85) !important;"> </font><font style="color:rgba(0, 0, 0, 0.85) !important;">：表示开启逃逸分析</font>
+ `<font style="color:rgb(0, 0, 0);">-XX:-DoEscapeAnalysis</font>`<font style="color:rgba(0, 0, 0, 0.85) !important;"> </font><font style="color:rgba(0, 0, 0, 0.85) !important;">：表示关闭逃逸分析 从 jdk 1.7 开始已经默认开始逃逸分析，如需关闭，需要指定</font>`<font style="color:rgb(0, 0, 0);">-XX:-DoEscapeAnalysis</font>`



**<font style="color:rgb(0, 0, 0);">逃逸分析并不成熟</font>**

<font style="color:rgba(0, 0, 0, 0.85) !important;">关于逃逸分析的论文在 1999 年就已经发表了，但直到 JDK 1.6 才有实现，而且这项技术到如今也并不是十分成熟的。</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">其根本原因就是无法保证逃逸分析的性能消耗一定能高于他的消耗。虽然经过逃逸分析可以做标量替换、栈上分配、和锁消除。但是逃逸分析自身也是需要进行一系列复杂的分析的，这其实也是一个相对耗时的过程。</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">一个极端的例子，就是经过逃逸分析之后，发现没有一个对象是不逃逸的。那这个逃逸分析的过程就白白浪费掉了。</font>

<font style="color:rgba(0, 0, 0, 0.85) !important;">虽然这项技术并不十分成熟，但是他也是即时编译器优化技术中一个十分重要的手段。</font>

#### <font style="color:rgba(0, 0, 0, 0.85) !important;">锁消除</font>
在动态编译同步块的时候，JIT编译器可以借助逃逸分析来判断同步块所使用的锁对象是否只能够被一个线程访问而没有被发布到其他线程。

如果同步块所使用的锁对象通过这种分析被证实只能够被一个线程访问，那么JIT编译器在编译这个同步块的时候就会取消对这部分代码的同步。这个取消同步的过程就叫同步省略，也叫锁消除。

如以下代码：

```java
public void f() {
    Object hollis = new Object();
    synchronized(hollis) {
        System.out.println(hollis);
    }
}
```

代码中对`hollis`这个对象进行加锁，但是`hollis`对象的生命周期只在`f()`方法中，并不会被其他线程所访问到，所以在JIT编译阶段就会被优化掉。优化成：

```java
public void f() {
    Object hollis = new Object();
    System.out.println(hollis);
}
```

所以，在使用`synchronized`的时候，如果JIT经过逃逸分析之后发现并无线程安全问题的话，就会做锁消除。

#### 标量替换
标量（Scalar）是指一个无法再分解成更小的数据的数据。Java中的原始数据类型就是标量。相对的，那些还可以分解的数据叫做聚合量（Aggregate），Java中的对象就是聚合量，因为他可以分解成其他聚合量和标量。

在JIT阶段，如果经过逃逸分析，发现一个对象不会被外界访问的话，那么经过JIT优化，就会把这个对象拆解成若干个其中包含的若干个成员变量来代替。这个过程就是标量替换。

```java
public static void main(String[] args) {
    alloc();
}

private static void alloc() {
    Point point = new Point (1,2);
    System.out.println("point.x="+point.x+"; point.y="+point.y);
}

class Point{
    private int x;
    private int y;
}
```

以上代码中，`point`对象并没有逃逸出`alloc`方法，并且`point`对象是可以拆解成标量的。那么，JIT就不会直接创建`Point`对象，而是直接使用两个标量`int x`，`int y`来替代`Point`对象。

以上代码，经过标量替换后，就会变成:

```java
private static void alloc() {
    int x = 1;
    int y = 2;
    System.out.println("point.x="+x+"; point.y="+y);
}
```

可以看到，Point这个聚合量经过逃逸分析后，发现他并没有逃逸，就被替换成两个标量了。那么标量替换有什么好处呢？就是可以大大减少堆内存的占用。因为一旦不需要创建对象了，那么就不再需要分配堆内存了。

同时，标量替换为栈上分配提供了很好的基础。

#### 栈上分配
在一般情况下，对象和数组元素的内存分配是在堆内存上进行的。**JIT编译器可以在编译期间根据逃逸分析的结果，来决定是否可以将对象的内存分配从堆转化为栈。hotspot的栈上分配本质是标量替换实现的。**

> 栈上分配可以支持方法逃逸，但不能支持线程逃逸。标量替换可以视作栈上分配的一种特例，实现更简单（不用考虑整个对象完整结构的分配），但对逃逸程度的要求更高，它不允许对象逃逸出方法范围内。  ——《深入理解Java虚拟机：JVM高级特性与最佳实践（第3版）周志明》哪个是对的？
>

注意不要用调试而是直接运行

```java
public class 栈上分配 {
    public static void main(String[] args) {
        long a1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            alloc();
        }
        // 查看执行时间
        long a2 = System.currentTimeMillis();
        System.out.println("cost " + (a2 - a1) + " ms");
        while (true) ;
    }

    private static void alloc() {
        User user = new User();
    }

    static class User {

    }
}
```

可以看到堆中只有十万多个User对象，剩下的都在栈上

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1755896041979-1954c85a-1a89-4253-a035-0d1d7e21570a.png)

#### 方法内联
方法内联是Java中的一个优化技术，即时编译器（JIT）用它来提高程序的运行效率。在Java中，方法内联意味着将一个方法的代码直接插入到调用它的地方，从而避免了方法调用的开销。这种优化对于小型且频繁调用的方法特别有用。

### JIT优化可能带来的问题
理解了 JIT 编译原理后就能明白，JIT 优化发生在运行期，而且不是应用一启动就能生效的，它需要先通过执行收集到热点代码信息。在此之前，所有请求只能通过解释器执行，速度较慢。如果应用启动时请求量大，解释器会高负荷运行，占用大量 CPU，导致负载升高，性能下降，这也是为什么很多应用在发布过程中，会出现刚刚重启好的应用会发生大量的超时问题了。 随着请求增多，JIT触发并缓存热点代码的机器码，后续执行速度才会明显提升。  



如何解决：

主要有两种思路：

1、提升JIT优化的效率

2、降低瞬时请求量

在提升JIT优化效率的设计上，大家可以了解一下阿里研发的JDK——Dragonwell。

这个相比OpenJDK提供了一些专有特性，其中一项叫做JwarmUp的技术就是解决JIT优化效率的问题的。

这个技术主要是通过记录Java应用上一次运行时候的编译信息到文件中，在下次应用启动时，读取该文件，从而在流量进来之前，提前完成类的加载、初始化和方法编译，从而跳过解释阶段，直接执行编译好的机器码。

除了针对JDK做优化之外，还可以采用另外一种方式来解决这个问题，那就是做预热。

很多人都听说过缓存预热，其实思想是类似的。

就是说在应用刚刚启动的时候，通过调节负载均衡，不要很快的把大流量分发给他，而是先分给他一小部分流量，通过这部分流量来触发JIT优化，等优化好了之后，再把流量调大。

## AOT编译
JIT编译有一些比较明显的缺点也是不能忽视的：

+ 增加启动时间：由于JIT编译器在程序运行时编译代码，它可能导致应用程序的启动时间较长。
+ 可能会影响应用性能：JIT编译是需要进行热点代码检测、代码编译等动作的，这些都是要占用运行期的资源，所以，JIT编译过程中也可能会影响应用性能。

AOT(Ahead-of-Time Compilation)编译，翻译一下就是提前编译，它不像JIT一样在运行期才生成机器码，而是**在编译期间就将字节码转换为机器码**，这就直接省去了运行时对JVM的依赖。这是一种典型的静态编译技术。



一个Java程序之所以能做静态编译，需要满足一个至关重要的前提——**封闭性假设**。什么是封闭性假设，也就是说他要求所有运行时的内容必须在编译时可见，并且可以被编译到native image中。但是Java中有很多代码是没有办法在编译期就确定的，比如反射，他就是不满足封闭性假设的。其他的类似的违反封闭性假设的特性还有动态代理、序列化、JNI、动态类加载等等。所以，遇到这些代码的时候，就需要额外的适配来解决。带来了很多的复杂性，也给静态编译带来了一定的局限性。



静态编译以后的代码程序是**平台相关**的。



> [GraalVM](https://www.yuque.com/shangshanruoshui-0bl0c/hgp3nz/bk4c0wd1gp4ospcw#sLQEs)
>

## 常见的JVM工具有哪些
[https://www.yuque.com/hollis666/vn7dy3/ouwbti](https://www.yuque.com/hollis666/vn7dy3/ouwbti)

**jps**：展示显示当前所有java进程pid

# 安全点
[https://www.yuque.com/hollis666/vn7dy3/rpclpg5ag63bkyyq](https://www.yuque.com/hollis666/vn7dy3/rpclpg5ag63bkyyq)

