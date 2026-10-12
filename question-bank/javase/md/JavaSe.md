[飞书](https://k1dy9adkxea.feishu.cn/wiki/D8utwEwJYiZla9kbwq4cEoGAnEf)

[二哥的Java进阶之路](https://javabetter.cn/home.html)

[Hollis](https://www.yuque.com/hollis666/vn7dy3)

[小林coding](https://xiaolincoding.com/interview/#java%E9%9D%A2%E8%AF%95%E7%AF%87)

[面试鸭](https://www.mianshiya.com/)

# Java 是动态类型语言还是静态类型语言？
java是静态类型语言, 因为java会在编译阶段进行类型检查, 而动态类型语言是在运行时阶段进行类型检查

> 除了Java, golang, C, C++, C# 都是静态类型语言；
>
> Python, JavaScript, Ruby等则属于动态类型语言。
>

# Java 的基本数据类型有哪些？
Java 中有 8 种基本数据类型，分别为：

+ **6 种数字类型：**
    - 4 种整数型：`byte(1)`、`short(2)`、`int(4)`、`long(8)`
    - 2 种浮点型：`float(4)`、`double(8)`
+ **1 种字符类型：**`char(2)`
+ **1 种布尔型：**`boolean(?)`。

# char能存储中文吗
在Java中，char类型是用来表示一个16位（2个字节）的Unicode字符，它可以存储任何Unicode字符集中的字 符，当然也包括中文字符。

> 虽然Unicode字符集也在一直不断的迭代更新，但是对于一些非常罕见的生僻字，它们可能因为版本问题，或者时 间问题，暂时不在Unicode字符集中。在这种情况下，可能就会无法表示。
>

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1752261951072-26f4d7b1-4c61-4974-8733-3a15bacb59b1.png)

# 面向对象都有哪些特征？
面向对象的三大特征是**封装, 继承, 多态**

封装就是将对象的属性和行为(方法)结合在一起, 通过访问权限修饰符对外隐藏对象的内部状态和实现细节, 仅通过对象提供的接口与外界交互, 目的是增强安全性和简化编程, 使得对象更独立

继承是一个类可以继承另一个类的属性和方法, 可以实现代码复用和建立类与类之间的层次关系

多态就是父类引用可以指向子类对象, 并且在运行时根据实际对象类型调用相应的方法

> 多态主要体现在运行时多态，体现在方法重写，还有编译时多态，这个说法应该比较少，体现在方法重载
>

# 重载和重写的区别是什么？
**重载(**overload)是一个类中可以由多个同名的方法, 但是方法的参数列表必须不同

**重写**(override)是子类重新定义父类中已有的方法, 方法名、参数列表和返回类型必须与父类中的方法一致, 子类方法声明的**异常类型**要小于等于父类, 子类方法的**访问权限**应该大于等于父类。

# == 和 equals() 有什么区别？
对于基本数据类型来说, ==比较的是值是否相等, 对于引用数据类型来说, ==比较的是地址是否相等, equals()在没有重写的情况下和==是一样的, 一般来说重写后的equals()比较的是对象的内容是否相同



**两个对象值相同（x.equals(y) == true），但却可有不同的 hashCode()，这句话对不对？**

不对, 如果两个对象通过equals方法比较结果为true, 那么它们的hashCode()也应该返回相同的结果

> 重写 equals 方法时必须重写 hashCode 方法，以保证在使用哈希表等数据结构时，对象的相等性判断和存储查找操作能够正常工作。而重写 hashCode 方法时，需要确保相等的对象具有相同的哈希码，但相同哈希码的对象不一定相等。
>

# int 和 Integer 有什么区别？什么是包装类？
int是基本数据类型的一种, 而Integer是int对应的包装类, 属于引用数据类型

包装类是将基本数据类型封装成对象的类, 用于需要对象而非原始值的场景, 比如集合类的泛型, 包装类可以用null值来表示未知



在Boxing Conversion部分的Java语言规范(JLS)规定如下：

如果一个变量p的值是：

```plain
-128至127之间的整数(§3.10.1)
true 和 false的布尔值 (§3.10.3)
‘\u0000’至 ‘\u007f’之间的字符(§3.10.4)
```

范围内的时，将p包装成a和b两个对象时，可以直接使用a==b判断a和b的值是否相等。

例如

```java
Integer a = 100;
Integer b = 100;
```

a == b 会返回true

```java
Integer a = 300;
Integer b = 300;
```

a == b 会返回false

# 什么是字节码？
java源码编译后生成的.class文件就是字节码文件

java虚拟机负责将字节码文件翻译成特定平台下的机器码然后运行, 所以只要在不同平台上安装对应的jvm就可以运行java程序, 达到跨平台的目的

# this 关键字怎么用？super 怎么用？二者有什么区别？
`this`是一个**引用当前对象**的关键字

+ 可以调用当前类中的任意成员变量
+ 可以调用当前类中的方法(不能在static方法中使用)
+ `this()`可以调用当前类的构造方法, 但是只能在构造方法中使用且必须放在第一行
+ 可以作为参数在方法和构造方法中传递
+ 可以作为方法的返回值

`super`是一个**引用父类对象**的关键字(每当创建一个子类对象的时候，也会隐式的创建父类对象，由 super 关键字引用。)

+ 可以调用父类的方法(可以用于方法重写时访问到父类的方法)
+ `super()` 可以调用父类的构造方法(只能在构造方法中使用且必须放在第一行, super()是可以省略的，编译器会主动去调用父类的构造方法, 如果父类没有无参构造就要手动去调用父类有参构造super(xxx))。
+ 如果父类和子类拥有同名字段，super可以用来访问父类的同名字段

# i++ 和 ++i 有什么区别？
i++是先返回再递增, ++i是先递增再返回

# String 为什么是不可变的？你对字符串常量池了解吗？
在jdk8及以前, String的底层是用final修饰的char数组(从jdk9开始变为了byte数组), 一旦数组被初始化就不能再指向其他的数组, 像substring和concat这些方法并不是修改原来的字符串而是返回一个新的对象



字符串常量池存储了通过字面量和intern()方法创建的字符串的引用, 通过new String()显式创建的字符串对象不会放在字符串常量池中, 会在堆内存中单独存储, 在jdk1.7以前字符串常量池被放在永久代中, 从jdk1.7开始就被移到了堆内存中



**String s = new String("abc") 会创建几个对象？**

会创建两个对象, "abc"本身是字符串常量池中的一个对象, 在new String()时又会根据这个对象复制一个新的字符串对象放在堆上, 两者内容是相同的, 但不是同一个对象



# String、StringBuilder 和 StringBuffer 的区别是什么？StringBuffer 与 StringBuilder 的线程安全性如何？
String是不可变的，而StringBuilder和StringBuffer是可变的。StringBuffer是线程安全的，而StringBuilder是非线程安全的

StringBuilder和StringBuffer中的字符数组不是final的，append会直接拷贝字符到内部的字符数组中，如果字符数组长度不够，会进行扩展。StringBuffer的append方法使用了synchronized修饰来确保线程安全

****

**为什么不要在for循环中使用+拼接字符串？**

在jdk8及以前，+号操作符拼接字符串的本质是使用StringBuilder，在for循环中，每次拼接都是new了一个StringBuilder，然后再把String转成StringBuilder，再进行append，频繁地创建对象会不仅会耗费很多时间还会造成内存的资源的浪费。在jdk9以后+拼接字符串有一定的优化。

[https://blog.51cto.com/u_13238266/11021570](https://blog.51cto.com/u_13238266/11021570)

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1752419107997-865b4fb8-74be-42af-93ea-295aa9e55afb.png)

在这个示例代码中

+ `builder` 中的 `'a'` 和 `'b'` 的数量可能不等于 1000，因为多线程竞争导致写入丢失或混乱。
+ `buffer` 中的 `'a'` 和 `'b'` 的数量总是 1000，保证了线程安全。
+ `buffer` 中的`append`方法的synchronized是加在方法上的，只能保证append方法同时只能被一个线程执行，所以实际拼接出来的字符串是ab交错的

```java
public class Main {
    public static void main(String[] args) throws InterruptedException {
        StringBuilder builder = new StringBuilder();
        StringBuffer buffer = new StringBuffer();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                builder.append("a");
                buffer.append("a");
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                builder.append("b");
                buffer.append("b");
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("builder:");
        System.out.println("a的数量为：" + builder.chars().filter(value -> value == 'a').count());
        System.out.println("b的数量为：" + builder.chars().filter(value -> value == 'b').count());

        System.out.println("buffer:");
        System.out.println("a的数量为：" + buffer.chars().filter(value -> value == 'a').count());
        System.out.println("b的数量为：" + buffer.chars().filter(value -> value == 'b').count());
    }
}

```

[https://www.zhihu.com/question/20101840/answer/164866159](https://www.zhihu.com/question/20101840/answer/164866159)

# Error 和 Exception 的区别和联系是什么？常见的 RuntimeException 有哪些？
两者均继承自Throwable

Error是系统级的严重错误，是java运行环境内部错误或者硬件问题，不能指望程序来处理这样的问题，除了退出运行外别无选择，它是Java虚拟机抛出的，有OutOfMemoryError(内存溢出)、StackOverflowError（栈溢出）、NoClassDefFoundError()等

exception 表示程序需要捕捉、处理的异常，是由与程序设计的不完善而出现的问题。分为RuntimeException(非受检异常)和其他异常(受检异常)

受检异常：必须显式捕获或声明抛出，如IOException，SQLException

非受检异常：RuntimeException 及其子类，如

+ NullPointerException：空指针异常，访问 `null` 对象的方法或属性。
+ IllegalArgumentException：非法参数异常，传入非法参数。
+ IndexOutOfBoundsException：索引越界异常，访问数组、列表等超出合法索引范围
+ ClassCastException：类型转换异常，强制类型转换失败。
+ ArithmeticException：算术异常，算术运算错误（如除以零）。
+ IllegalStateException：非法状态异常，对象状态非法时操作。

```java
Iterator<String> it = list.iterator();
it.remove(); // 未调用 next() 前调用 remove()，抛出 IllegalStateException
```

# finally中代码一定会执行吗？
通常情况下，finally的代码一定会被执行，但是这是有一个前提的：

1. 对应 try 语句块被执行，
2. 程序正常运行。



如果没有符合这两个条件的话，finally中的代码就无法被执行，如发生以下情况，都会导致finally不会执行：

1、System.exit()方法被执行

2、Runtime.getRuntime().halt()方法被执行（**强制终止 Java 虚拟机（JVM）进程**，并返回指定的状态码。

它的作用类似于 `System.exit(int status)`，但更为激烈和彻底。）

3、try或者catch中有死循环

4、操作系统强制杀掉了JVM进程，如执行了kill -9

5、其他原因导致的虚拟机崩溃了

6、虚拟机所运行的环境挂了，如计算机电源断了

7、如果一个finally是由守护线程执行的，那么是不保证一定能执行的，如果这时候JVM要退出，JVM会检查其他非守护线程，如果都执行完了，那么就直接退出了。这时候finally可能就没办法执行完。

# 抽象类和接口的区别是什么？
特点：

抽象类用于**描述类的共同特性和行为**，可以有成员变量、构造方法和具体方法。**适用于有明显继承关系的场景，用于代码复用。**

接口用于**定义行为规范**，可以多实现，只能有常量和抽象方法（Java 8 以后可以有默认方法和静态方法）。**适用于定义类的能力或功能。**



区别：

**方法定义**：接口中只有抽象方法，是没有实现的代码的。（从Java8开始可以有默认方法）

**修饰符**：抽象类中的修饰符可以有public、protected和private这些修饰符，而接口中默认修饰符是public。不可以使用其它修饰符。（接口中，如果定义了成员变量，还必须要初始化）

**构造器**：抽象类可以有构造器。接口不能有构造器。

> 抽象类不能直接被实例化（new)出来，但是构造器也是有意义的，能起到初始化共有成员变量、强制初始化操作等作用。
>

**继承和实现**：一个类可以实现多个接口，但只能继承一个抽象类。接口支持多重继承，即一个接口可以继承多个其他接口。

**职责不同**：接口和抽象类的职责不一样。接口主要用于制定规范，因为我们提倡也经常使用的都是面向接口编程。而抽象类主要目的是为了复用，比较典型的就是模板方法模式。

**<font style="color:#ED740C;">所以当我们想要定义标准、规范的时候，就使用接口。当我们想要复用代码的时候，就使用抽象类。</font>**



一般在实际开发中，我们会先把接口暴露给外部，然后在业务代码中实现接口。如果多个实现类中有相同可复用的代码，则在接口和实现类中间加一层抽象类，将公用部分代码抽出到抽象类中。可以参考下模板方法模式，这是一个很好的理解接口、抽象类和实现类之间关系的设计模式。

# 注解的作用是什么？
注解是java5引入的一种元数据(Metadata)，用于为代码提供额外信息，作用有：

+ 编译检查：如@Override、@Deprecated。
+ 代码生成：如 Lombok 的@Data。
+ 运行时处理：如 Spring 的@Autowired、JUnit 的@Test。

# 反射机制及其使用场景有哪些？
反射指的是程序在运行时能够动态获取类的信息并操作其属性或方法的机制，通过反射，对于任意一个类，都能够知道这个类的所有属性和方法，对于任意一个对象，都能够调用它的任意属性和方法

使用场景有：框架开发，动态代理，序列化和反序列化，单元测试，注解处理等

## 除了使用new创建对象之外，还可以用什么方法创建对象？
使用Java反射可以创建对象(先调用getConstructor()获取构造方法再调用newInstance())

## Java反射创建对象效率高还是通过new创建对象的效率高？
通过new创建对象的效率比较高。通过反射时，先查找类资源，使用类加载器创建，过程比较繁琐，所以效率较低，new对象的方式在**编译期即可确定要加载的对象路径以及是否合法**，而通过反射的话在运行时才能确定，那这部分**安全合法性的检查就会影响类加载的效率。**

****

## 反射使用步骤
获取Class对象

调用对应api

```java
Object obj = // ... 任意对象;
Class<?> clazz = obj.getClass();

// 获取成员变量
Field[] fields = clazz.getDeclaredFields();
for (Field field : fields) {
    System.out.println("成员变量: " + field.getName());
}

// 获取方法
Method[] methods = clazz.getDeclaredMethods();
for (Method method : methods) {
    System.out.println("方法: " + method.getName());
}

Method method = clazz.getDeclaredMethod("methodName", // 方法参数类型...);
method.setAccessible(true); // 如果方法是私有的
method.invoke(obj, // 方法参数...);

// 默认构造函数
Object obj = clazz.newInstance();

// 或者使用特定的构造函数
Constructor<?> constructor = clazz.getConstructor(// 参数类型...);
Object obj = constructor.newInstance(// 构造函数参数...);
```

int.class=Integer.TYPE≠Integer.class

`getXXX()` 获取包括父类在内的 **公有成员**，而 `getDeclaredXXX()` 只获取 **本类声明的所有成员（含私有）**。

## 反射机制的优缺点：
优点：提升程序的灵活性和扩展性

缺点：性能低：

1、由于反射涉及动态解析的类型，因此不能执行某些Java虚拟机优化，如JIT优化。

2、在使用反射时，参数需要包装（boxing)成Object[] 类型，但是真正方法执行的时候，又需要再拆包（unboxing)成真正的类型，这些动作不仅消耗时间，而且过程中也会产生很多对象，对象一多就容易导致GC，GC也会导致应用变慢。

3、反射调用方法时会从方法数组中遍历查找，并且会检查可见性。这些动作都是耗时的。

4、不仅方法的可见性要做检查，参数也需要做很多额外的检查。

> **JIT（Just-In-Time Compilation，即时编译）** 是 Java 虚拟机（JVM）的一种性能优化机制，它在**程序运行时**将**热点代码**（频繁执行的代码）从字节码编译成本地机器码，从而提高运行效率。  
>

# 什么是序列化与反序列化
**对象序列化是将对象的状态信息（数据成员）转换为可以存储或传输的格式（如字节流、JSON、XML等）的过程。反序列化就是把这个过程反过来。**

通过对象序列化，可以把对象的状态保存为字节数组，并且可以在有需要的时候将这个字节数组通过反序列化的方式再转换成对象，这样就可以避免JVM停止运行后对对象状态丢失。

## 如何进行序列化和反序列化？
+ 在Java中，只要一个类**实现了java.io.Serializable接口**，那么它就可以被序列化(使用Java原生的序列化机制)。
+ 在Java中通过序列化对象流来完成序列化和反序列化：
    - ObjectOutputStream：通过writeObject()方法做序列化操作。
    - ObjectIntputStream：通过readObject()方法做反序列化操作。
+ 虚拟机是否允许反序列化，不仅取决于类路径和功能代码是否一致，一个非常重要的一点是两个类的序列化 ID 是否一致（就是 private static final long **serialVersionUID**（必须是这个字段名））
    - 当实现java.io.Serializable接口的类没有显式地定义一个serialVersionUID变量时候，Java序列化机制会根据编译的Class自动生成一个serialVersionUID作序列化版本比较用，这种情况下，如果Class文件没有发生变化，就算再编译多次，serialVersionUID也不会变化的。但是，如果发生了变化，那么这个文件对应的serialVersionUID也就会发生变化。
    - 在进行反序列化时，JVM会把传来的字节流中的serialVersionUID与本地相应实体类的serialVersionUID进行比较，如果相同就认为是一致的，可以进行反序列化，否则就会出现序列化版本不一致的异常，即是InvalidClassException。
    - 所以，一旦类实现了Serializable，就建议明确的定义一个serialVersionUID。不然在修改类的时候，就会发生异常。
    - serialVersionUID有两种显示的生成方式：
        * 一是默认的1L，比如：private static final long serialVersionUID = 1L;
        * 二是根据类名、接口名、成员方法及属性等来生成一个64位的哈希字段，比如：private static final long serialVersionUID = xxxxL;
+ 序列化并**不保存静态变量**。
+ 要想将父类对象也序列化，就需要让**父类也实现Serializable 接口**。
+ **transient** 关键字的作用是控制变量的序列化，在变量声明前加上该关键字，可以**阻止该变量被序列化到文件中**，在被反序列化后，transient 变量的值被设为初始值，如 int 型的是 0，对象型的是 null。

### 未实现Serializable，可以序列化吗？
如果使用**Java原生的序列化机制**（即通过 ObjectOutputStream 和 ObjectInputStream 类），则对象**必须实现 Serializable 接口**。如果对象没有实现这个接口，尝试原生序列化会抛出 NotSerializableException。

对于像**Jackson**、**Gson**这样的JSON序列化库或用于XML的库（如JAXB），对象**不需要实现 Serializable 接口**。这些库使用**反射**机制来访问对象的字段，并将它们转换成JSON或XML格式。在这种情况下，对象的序列化与 Serializable 接口无关。

> **Externalizable**继承了Serializable，该接口中定义了两个抽象方法：writeExternal()与readExternal()。当使用Externalizable接口来进行序列化与反序列化的时候需要开发人员重写writeExternal()与readExternal()方法。如果没有在这两个方法中定义序列化实现细节，那么序列化之后，对象内容为空。**实现Externalizable接口的类必须要提供一个public的无参的构造器。**
>
> **所以，实现Externalizable，并实现writeExternal()和readExternal()方法可以指定序列化哪些属性。**
>

# 什么情况下需要对对象进行序列化？
在编程中，对象序列化是将对象的状态信息（数据成员）转换为可以存储或传输的格式（如字节流、JSON、XML等）的过程。在以下常见情况下，通常需要对对象进行序列化：

1. 网络传输：
+ 核心场景： 当需要在不同的进程、机器或系统之间通过网络（如HTTP、TCP、Socket、RPC、gRPC、消息队列）发送对象时。
+ 原因： 网络协议通常传输的是原始字节流（或基于文本的格式如JSON/XML）。要将内存中的复杂对象发送给接收方，必须将其转换为这种可传输的格式。
+ 例子： 客户端向服务器发送一个包含用户数据的对象；微服务A调用微服务B的API并传递参数对象；将消息放入RabbitMQ/Kafka队列。
2. 持久化存储：
+ 核心场景： 当需要将对象的状态保存到非易失性存储介质（如硬盘、数据库、云存储）中，以便程序关闭后或未来某个时间点能重新加载恢复对象状态时。
+ 原因： 内存是易失性的，程序关闭后数据就丢失了。磁盘/数据库存储的是字节或结构化数据（如BLOB字段、文本字段）。序列化是将对象状态转化为适合存储形式的关键步骤。
+ 例子： 将应用程序配置对象保存到配置文件（如XML, JSON, YAML, .properties）；将游戏进度保存到存档文件；将复杂的业务对象状态存储到数据库的BLOB字段或NoSQL文档数据库（本质也是序列化，如MongoDB的BSON）。
3. 缓存：
+ 核心场景： 当使用分布式缓存（如Redis, Memcached）或某些本地缓存机制时。
+ 原因： 缓存系统本身通常不直接理解编程语言中的特定对象类型。它们存储的是键值对，值通常是字符串或字节数组。要将对象放入缓存，必须先序列化它；从缓存取出时再反序列化回对象。
+ 例子： 将数据库查询结果（封装成的对象）缓存到Redis中以提高后续访问速度。
4. 深度复制：
+ 核心场景： 当需要创建对象的一个完全独立的副本（深拷贝），而不仅仅是复制引用时。
+ 原因： 简单的引用赋值只会复制指向同一内存地址的指针。序列化一个对象到字节流，然后立即反序列化，会生成一个全新的、与原对象状态相同但完全独立的对象实例（前提是序列化/反序列化过程正确实现了深度复制）。
+ 例子： 需要修改一个对象的副本而不影响原始对象，且该对象内部包含对其他对象的引用（构成一个对象图）。
5. 分布式计算/状态共享：
+ 核心场景： 在分布式系统、集群或并行计算框架（如Hadoop, Spark, Flink）中，需要在不同的计算节点之间传递任务、数据或共享状态。
+ 原因： 节点间通信同样依赖于序列化机制来传输代表计算任务或数据的对象。
+ 例子： Spark将RDD（弹性分布式数据集）的分区数据序列化后通过网络传输给不同的Executor进行计算。

# 什么是 AIO、BIO 和 NIO？
BIO （Blocking I/O）：**同步阻塞I/O**，是JDK1.4之前的传统IO模型。 线程发起IO请求后，一直阻塞，直到缓冲区数据就绪后，再进入下一步操作。优点是编程简单易于理解和实现。缺点是效率低，线程大部分时间在阻塞等待 I/O 完成，CPU 利用率不高。

NIO （Non-Blocking I/O）：**同步非阻塞IO**，线程发起IO请求后，不需要阻塞，立即返回。用户线程不原地等待IO缓冲区，可以先做一些其他操作，只需要Selector 线程定时轮询检查IO缓冲区数据是否就绪即可。优点是并发性能高，缺点是编程相对复杂。

AIO （ Asynchronous I/O）：异步非阻塞I/O模型。线程发起IO请求后，不需要阻塞，立即返回，也不需要定时轮询检查结果，异步IO操作之后会回调通知调用方。优点是它的异步更彻底，理论性能更高。缺点是实现复杂且不够成熟，应用不广泛。Netty等网络框架仍然是基于NIO模型。



AIO、BIO 和 NIO 是三种不同的 I/O 模型，主要描述了程序如何与操作系统内核交互，进行输入/输出操作（如网络通信、文件读写）。它们在阻塞性、线程模型和性能上有显著区别：

1. BIO (Blocking I/O，阻塞 I/O)
+ 核心原理： 当应用程序发起一个 I/O 操作（如 read(), accept()）时，调用线程会被完全阻塞，直到操作系统内核完成该操作（数据准备好、连接建立好）并返回结果。
+ 线程模型： 通常采用 “一个连接一个线程” 的模式。每个客户端连接都需要一个独立的线程来处理其读写操作。
+ 优点：
    - 编程模型简单直观，易于理解和实现。
    - 代码逻辑是线性的（请求->等待->处理->响应）。
+ 缺点：
    - 资源消耗大： 大量线程（尤其在连接数高时）消耗大量内存（线程栈）和 CPU（线程上下文切换）。
    - 可伸缩性差： 线程数量受限于操作系统能创建的最大线程数和硬件资源，难以应对高并发（成千上万连接）。
    - 效率低： 线程大部分时间在阻塞等待 I/O 完成，CPU 利用率不高。
+ 典型代表： Java 中的 java.io 包（如 Socket, ServerSocket, FileInputStream/FileOutputStream 等）。
+ 比喻： 去银行柜台办业务，只有一个窗口（线程）。你在窗口前排队（连接），轮到你时告诉柜员要办什么（发起 I/O 请求），然后你就一直等在窗口前不能动（线程阻塞），直到柜员办完把结果给你。后面的人只能干等。
2. NIO (Non-blocking I/O / New I/O，非阻塞 I/O / 新 I/O)
+ 核心原理：
    - 通道 (Channel): 替代 BIO 中的流 (Stream)，支持双向读写，是连接数据源（如套接字、文件）的管道。
    - 缓冲区 (Buffer): 数据读写的中转容器。
    - 非阻塞模式 (Non-blocking Mode): 通道可以设置为非阻塞模式。当发起一个 I/O 操作（如 channel.read(buffer)）时，如果数据没有立即可用，调用会立即返回一个状态码（而不是阻塞线程），告诉应用程序“数据还没准备好，请稍后再试”。
    - 多路复用器 (Selector): NIO 的核心！ 一个线程可以注册并监听多个通道上的 I/O 事件（如连接就绪、读就绪、写就绪）。当某个通道上的事件发生时，Selector 会通知应用程序哪个通道准备好了什么事件，应用程序再对这个就绪的通道进行实际的 I/O 操作（此时操作通常是阻塞的，但时间很短，因为数据已经就绪）。
+ 线程模型： 通常使用少量线程（甚至一个线程） 配合 Selector 来管理大量连接。一个 Selector 线程轮询所有注册的通道，处理就绪事件，然后将实际的 I/O 操作分发给工作线程池或直接在 Selector 线程中快速处理。
+ 优点：
    - 高并发： 能用少量线程管理大量连接，极大减少了线程开销和上下文切换。
    - 高吞吐量： CPU 利用率更高，线程不再因等待 I/O 而闲置。
    - 可伸缩性： 能更好地适应连接数激增的场景。
+ 缺点：
    - 编程模型更复杂，需要理解 Channel、Buffer、Selector 及其事件机制。
    - 需要处理“轮询”逻辑（Selector 的 select() 方法）。
    - API 相对底层，容易出错（如处理半包、粘包问题）。
+ 典型代表： Java 中的 java.nio 包（Channel, Buffer, Selector）。Linux 下的 epoll, macOS/BSD 下的 kqueue, Windows 下的 IOCP（理念不同但目标类似）是操作系统提供的底层多路复用机制。
+ 比喻： 银行设置了一个分诊台（Selector） 和一个导诊员（Selector 线程）。你（连接）来了先在分诊台登记（注册到 Selector），告诉导诊员你需要什么（关注的事件，如“我要存钱” - 写就绪）。登记完你就可以去坐着玩手机（线程不阻塞）等叫号。导诊员一直盯着所有登记的人（select()），当某个窗口（Channel）空闲（事件就绪）且轮到处理你的需求时，导诊员就叫你的号（返回就绪事件集合）。你听到叫号后，走到那个窗口快速办完业务（短暂的阻塞 I/O 操作）。
3. AIO (Asynchronous I/O，异步 I/O)
+ 核心原理：
    - 应用程序发起一个 I/O 操作（如 read()）时，立即返回，不会阻塞调用线程。
    - 应用程序提供一个回调函数 (Callback) 或一个 Future 对象。
    - 操作系统内核独立地、在后台完成整个 I/O 操作（包括数据准备和数据从内核空间拷贝到用户空间）。
    - 当 I/O 操作完全完成后，操作系统内核会主动通知应用程序（通过执行回调函数或设置 Future 的状态），应用程序再处理数据。
+ 线程模型： 发起 I/O 的线程完全不阻塞，可以继续做其他事情。内核完成 I/O 后，通过回调机制通知应用程序，通常由线程池中的线程执行回调逻辑。
+ 优点：
    - 最彻底的异步： 发起 I/O 的线程完全不被阻塞，资源利用率最高。
    - 理论上性能最好： 避免了线程阻塞和用户态/内核态切换（在 NIO 的 Selector.select() 和最终读写操作时仍有切换）。
+ 缺点：
    - 编程模型最复杂，需要理解回调或 Future，容易陷入“回调地狱”。
    - 操作系统支持程度不一。Linux 对 AIO 的原生支持 (io_uring 之前）不够成熟高效，Windows 的 IOCP 是成熟的 AIO 实现。Java AIO (java.nio.channels.AsynchronousChannel) 在 Linux 上底层可能使用效率不高的线程池模拟（即不是真正的内核级 AIO）。
    - 调试和维护相对困难。
+ 典型代表： Java 中的 java.nio.channels.AsynchronousSocketChannel, AsynchronousFileChannel；操作系统层面如 Windows IOCP (Completion Ports)，Linux io_uring (较新且高效)。Node.js 的核心就是基于异步 I/O 的事件驱动模型。
+ 比喻： 你想办个复杂业务（发起一个 I/O 请求），把资料交给银行柜员后说“办好了打电话通知我”（提供回调函数），然后你就直接离开银行去逛街了（线程完全不阻塞）。银行柜员（内核）在后台处理你的业务，包括所有需要等待的步骤（数据准备和拷贝）。等整个业务完全办妥了，柜员主动打电话给你（内核主动回调） 说“办好了，结果在这里，你来取吧”（通知完成）。你再去银行拿结果（处理数据）。

# 什么是SPI，和API有啥区别
API 是面向普通开发者的，提供一组功能，使他们可以利用一个库或框架来实现具体的功能。而SPI 是面向那些希望扩展或定制基础服务的开发者的，它定义了一种机制，让其他开发者可以提供新的实现或扩展现有的功能

API是开发者调用框架提供的接口和实现，SPI是框架提供接口的规范，开发者去实现这个接口，然后框架再来调用这个接口

[https://developer.aliyun.com/article/911488](https://developer.aliyun.com/article/911488)

**<font style="color:rgb(36, 41, 46);">API的最显著特征就是</font>**<font style="color:rgb(36, 41, 46);">：</font>

实现和接口在一个包中。自定义接口，自定义实现类。

**<font style="color:rgb(36, 41, 46);">SPI最显著的特征就是</font>**<font style="color:rgb(36, 41, 46);">：</font>

「接口」在「调用方」的包，「调用方」定义规则，而自定义实现类在「实现方」的包，然后把实现类加载到「调用方」中。

以JDBC驱动为例，「调用方」（用户或者说JDK)定义了`java.sql.Driver`接口，这个接口位于「调用方」JDK的包中，各个数据库厂商实现了这个接口，比如mysql驱动`com.mysql.jdbc.Driver`。

# 什么是泛型
泛型是 Java 编程语言中的一个重要特性，它允许类、接口和方法在定义时使用一个或多个类型参数，这些类型参数在使用时可以被指定为具体的类型。

以List接口为例，我们可以将String、Integer等类型放入List中，如不用泛型，存放String类型要写一个List接口，存放Integer要写另外一个List接口，泛型可以很好的解决这个问题，如果用Object来存放对象，在取对象时又要进行类型转换，也是比较麻烦的，这就可以体现出泛型的好处

泛型的主要作用有以下几点：

1. **类型安全**：在编译时检查类型，避免类型转换错误。
2. **代码复用**：编写通用类或方法，适用于多种数据类型。
3. **可读性强**：明确变量或集合中元素的类型，代码更清晰。
4. **避免强制类型转换**：使用泛型后不需要频繁进行类型强转，提高代码简洁性和可靠性。

## 泛型中上下界限定符extends 和 super有什么区别？
<? extends T> 表示类型的上界，表示参数化类型的可能是T 或是 T的子类

<? super T> 表示类型下界（Java Core中叫超类型限定），表示参数化类型是此类型的超类型（父类型），直至Object

PECS原则：

在使用 限定通配符的时候，需要遵守PECS原则，即Producer Extends, Consumer Super；上界生产，下界消费。

如果要从集合中读取类型T的数据，并且不能写入，可以使用 ? extends 通配符；(Producer Extends)。

如果要从集合中写入类型T的数据，并且不需要读取，可以使用 ? super 通配符；(Consumer Super)。

+ `? extends T`：只能读，不能写（除了 null）
+ `? super T`：可以写，不能随便读（只能读为 Object）
+ 如果既要存又要取，那么就不要使用任何通配符。

## 泛型擦除
Java 编译器在编译期间会移除泛型的类型参数，并用原始类型或边界类型来替代。

也就是说，List<String> 和 List<Integer>经过编译后都是同一个类。



像这种情况泛型信息会保留

 在子类中写死泛型类型，可以通过反射保留具体类型参数  

```java
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class Parent<T> {
    T a;
}

class Child extends Parent<Parent<String>> {
}

public class Main {
    public static void main(String[] args) {
        Type type = Child.class.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] actualTypes = ((ParameterizedType) type).getActualTypeArguments();
            for (Type t : actualTypes) {
                System.out.println("泛型类型是: " + t.getTypeName());
            }
        }
    }
}
```

打印结果

```java
泛型类型是: com.itheima.test3.Parent<java.lang.String>
```

[https://blog.csdn.net/cpcpcp123/article/details/115141681](https://blog.csdn.net/cpcpcp123/article/details/115141681)

Java泛型信息是否擦除有如下两种情况：

1. 声明侧泛型信息保留 例如声明泛型接口，泛型类，泛型方法时的泛型信息
2. 使用侧泛型信息擦除 例如方法的局部变量等。

## 泛型中K T V E ？ Object等分别代表什么含义。
+ E – Element (在集合中使用，因为集合中存放的是元素)
+ T – Type (Java 类) 
+ K – Key（键） 
+ V – Value（值） 
+ N – Number（数值类型） 
+ ? – 表示不确定的 java 类型（无限制通配符类型） 
+ S、U、V – 这几个有时候也有，这些字母本身没有特定的含义，它们只是代表某种未指定的类型。一般认为和 T 差不多。 
+ Object – 是所有类的根类，任何类的对象都可以设置给该 Object 引用变量，使用的时候可能需要类型强制转换，但是用使用了泛型 T、E 等这些标识符后，在实际用之前类型就已经确定了，不需要再进行类型强制转换。

# 什么是深拷贝和浅拷贝？
在 Java 中，“浅拷贝”（**shallow copy**）是指拷贝一个对象时，仅仅拷贝了**对象本身的字段值**，而**不拷贝字段中引用的对象本身**。换句话说：

+ 基本数据类型（如 `int`, `float`, `boolean`）的值会被复制；
+ 引用类型（如对象、数组等）的**引用地址**会被复制，但不会复制引用对象本身，这意味着新旧对象会共享同一个引用对象。

在Java中，我们常用的各种BeanUtils基本也都是浅拷贝的。

org.springframework.beans.BeanUtils是浅拷贝的

cn.hutool.core.bean.BeanUtil是深拷贝的

## 如何实现深拷贝
1. **实现Cloneable接口，重写clone()**

在Object类中定义了一个clone方法，这个方法其实在不重写的情况下，其实也是浅拷贝的。如果想要实现深拷贝，就需要重写clone方法，而想要重写clone方法，就必须实现Cloneable，否则会报CloneNotSupportedException异常。

```java
class Person implements Cloneable {
    String name;
    int age;
    Address address;

    public Person(String name, int age, Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Person cloned = (Person) super.clone();
        // 深拷贝 Address 对象
        cloned.address = new Address(this.address.city);
        return cloned;
    }
}

class Address {
    String city;

    public Address(String city) {
        this.city = city;
    }
}

public class Main {
    public static void main(String[] args) throws CloneNotSupportedException {
        Address addr = new Address("Beijing");
        Person p1 = new Person("Tom", 25, addr);
        Person p2 = (Person) p1.clone();

        p2.name = "Jerry";
        p2.address.city = "Shanghai";

        System.out.println(p1.name);
        System.out.println(p1.address.city);
    }
}
```

相当于通过调用clone方法来复制了一个对象，在clone方法里将引用数据类型的字段重新new一份出来

2. **序列化实现深拷贝**

我们可以借助序列化来实现深拷贝。先把对象序列化成流，再从流中反序列化成对象，这样就一定是新的对象了。序列化的方式有很多，比如我们可以使用各种JSON工具，把对象序列化成JSON字符串，然后再从字符串中反序列化成对象。

3. **手动递归复制**

针对特定对象结构，手动递归复制对象及其引用类型字段。适用于对象结构复杂度不高的情况。

# Java的动态代理如何实现？


# <font style="color:rgb(51, 51, 51);">stream流的原理？常用的流？</font>
Java 8引入了Stream API，它提供了一种高效且易于使用的数据处理方式，特别适合集合对象的操作，如过滤、映射、排序等。Stream API不仅可以提高代码的可读性和简洁性，还能利用多核处理器的优势进行并行处理。

可以将要处理的元素集合看作一种流，流在管道中传输，并且可以在管道的节点上进行处理，比如筛选，排序，聚合等。

Stream有以下特性及优点：

+ 无存储。Stream不是一种数据结构，它只是某种数据源的一个视图，数据源可以是一个数组，Java容器或I/O channel等。
+ 为函数式编程而生。对Stream的任何修改都**不会影响原数据**，比如对Stream执行过滤操作并不会删除被过滤的元素，而是会产生一个不包含被过滤元素的新Stream。
+ **惰式执行**。Stream上的操作并不会立即执行，只有等到用户真正需要结果的时候才会执行。
+ **可消费性。流是一次性的**，Stream只能被“消费”一次，一旦遍历过就会失效，就像容器的迭代器那样，想要再次遍历必须重新生成。
1. **Stream的创建**
+ 单列集合：<font style="color:rgb(216,57,49);">集合对象.stream() </font>

```java
List<Author> authors = getAuthors();
Stream<Author> stream = authors.stream();
```

+ 数组： <font style="color:rgb(216,57,49);">Arrays.stream(数组)</font>或者使用<font style="color:rgb(216,57,49);">Stream.of(数组)</font>来创建

```plain
Integer[] arr = {1,2,3,4,5};
Stream<Integer> stream1 = Arrays.stream(arr);
Stream<Integer> stream2 = Stream.of(arr);
```

+ 双列集合：转换成单列集合后再创建

```java
Map<String,Integer> map = new HashMap<>();
map.put("蜡笔小新", 19);
map.put("樱木花道", 17);
map.put("漩涡鸣人", 16);

Stream<Map.Entry<String, Integer>> stream = map.entrySet().stream();
```

2. **Stream中间操作**
+ filter 过滤
+ map 计算或转换
+ peek
+ distinct 去重
+ sorted 排序
+ limit 限制
+ skip 跳过
+ flatMap 转换成多个对象
3. **Stream最终操作**
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">forEach 遍历</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">count 个数</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">max && min 最值</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">collect 集合</font>
+ <font style="color:rgba(0, 0, 0, 0.85) !important;">查找与匹配</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">anyMatch 任意符合</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">allMatch 都符合</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">noneMatch 都不符合</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">findAny 获取任一</font>
    - <font style="color:rgba(0, 0, 0, 0.85) !important;">findFirst 获取第一个</font>

## Stream的并行流一定比串行流更快吗？
Stream底层使用了ForkJoin进行并发处理，但是，并不代表着用了并发处理就一定比串行处理更快。有以下几个因素影响着并行流的性能：

**线程管理的开销**：并行流使用了多线程，而用了多线程就会带来线程管理和任务分配的开销。

**任务分割**：并行流的性能提升依赖于任务能够有效地分割和分配。如果任务分割不均衡，一些线程可能空闲或等待，从而影响性能。

**线程争用**：并行流使用公共的ForkJoinPool，如果系统中有其他并行任务，这些任务会争用线程资源，可能导致性能下降。

**数据依赖性**：并行流适用于没有数据依赖性的操作。如果操作之间存在依赖关系，并行流可能无法有效地提升性能，甚至可能导致错误。

**环境配置**：机器的硬件配置（例如CPU核心数）和当前系统负载也会影响并行流的性能。如果CPU核心数较少或负载较高，并行流的性能可能不如串行流。

**在单核CPU的情况下，Stream的串行迭代的效率是要高于Stream的并行迭代的效率的。**

**而在多核CPU的情况下，Stream的并行迭代速度要比Stream的串行迭代效率要高。但是，如果元素数量比较少的话，直接用常规迭代反而性能更好。**

# BigDecimal和Long表示金额哪个更合适，怎么选择？
对于绝大多数涉及货币计算的应用，推荐使用 BigDecimal，因为它提供了必要的精度和灵活性，尽管牺牲了一些性能。如果确定不需要处理小数，并且对执行速度有极端要求，使用 long 可能更适合。

# jdk8新特性


# Java中的static都能用来修饰什么？
可用来修饰**变量**、**方法**、**代码块**以及**类**。

1. 静态变量：
+ 定义：静态变量属于类本身，而不是类的任何特定实例（new出来的对象）。
+ 特点：
    - 所有实例共享同一静态变量。
    - 在类加载到内存时就被初始化，而不是在创建对象的时候。
    - 常用于管理类的全局状态或作为常量仓库（例如public static final修饰的常量）。
2. 静态方法：
+ 定义：静态方法同样属于类，而非类的实例。
+ 特点：可以在不创建类的实例的情况下调用。
    - 不能访问类的实例变量或实例方法，它们只能访问其他的静态成员。
    - 常用于工具类的方法，例如Math.sqrt()或Collections.sort()。
3. 静态代码块：
+ 定义：用于初始化类的静态变量。
+ 特点：
    - 当类被Java虚拟机加载并初始化时执行。
    - 通常用于执行静态变量的复杂初始化。
4. 静态内部类：
+ 定义：在一个类的内部定义的静态类。
+ 特点：
    - 可以不依赖于外部类的实例而独立存在。
    - 可以访问外部类的所有静态成员，但不能直接访问外部类的实例成员。
    - 常用于当内部类的行为不应依赖于外部类的实例时。

使用static修饰符的好处包括减少内存使用（共享静态变量而不是为每个实例创建副本）、提供一个全局访问点（例如静态方法和变量）以及无需实例化类即可使用其中的方法和变量。

# -------------------集合--------------------
# Java 中的集合类有哪些？它们的特点是什么？
Java集合主要分为Collection单列集合和Map双列集合两大类，

Collection主要有ArrayList，LinkedList，HashSet，TreeSet等，

Map主要有HashMap，LinkedHashMap，ConcurrentHashMap，TreeMap等。

<!-- 这是一张图片，ocr 内容为： -->
![](https://cdn.nlark.com/yuque/0/2025/png/52164061/1752513960444-444d434b-cb1b-4dda-ac5d-835509c93266.png)

# ArrayList、LinkedList的区别是什么？
ArrayList 是一个可改变大小的数组，LinkedList 是一个双向链表。

ArrayList 更利于查找，LinkedList 更利于增删。



# ArrayList是如何扩容的？
新增元素时检查新增元素后是否会超过数组的容量，如果超过，则设置新的容量为老容量的1.5倍，之后，申请一个容量为1.5倍的数组，并将老数组的元素复制到新数组中，扩容完成

# HashMap、Hashtable 和 ConcurrentHashMap 的区别是什么？
1. 线程安全性
+ HashMap：非线程安全，适用于单线程环境。
+ Hashtable：线程安全（所有方法被 synchronized 修饰），但性能较低。
+ ConcurrentHashMap：线程安全（JDK 8+ 使用 CAS + synchronized 分段锁），性能显著高于 Hashtable。
2. 对 null 的支持
+ HashMap：允许一个 null 键和多个 null 值。
+ Hashtable：不允许任何 null 键或值（否则抛出 NullPointerException）。
+ ConcurrentHashMap：不允许 null 键或值（设计上避免歧义，如 get(key) 返回 null 可能表示值不存在或值为 null）。
3. 底层数据结构
+ HashMap（JDK 8+）：数组 + 链表 + 红黑树（链表长度超过 8 时转为红黑树）。
+ Hashtable：数组 + 链表（不支持树化）。
+ ConcurrentHashMap（JDK 8+）：数组 + 链表 + 红黑树，与 HashMap 类似，但通过 CAS 和锁保证并发安全。
4. 性能与适用场景
+ HashMap：单线程环境下性能最优，无同步开销。
+ Hashtable：多线程环境下使用，但由于全局锁，吞吐量极低。
+ ConcurrentHashMap：多线程环境首选，锁粒度细化（仅锁定链表头或树的根节点），并发性能高。
5. 迭代器
+ HashMap/Hashtable：使用 Iterator，迭代时若结构被修改会抛出 ConcurrentModificationException（快速失败）。
+ ConcurrentHashMap：使用 ConcurrentHashMap.KeySetView 等并发迭代器，允许在迭代过程中修改结构（弱一致性）。

| 特性 | HashMap | Hashtable | ConcurrentHashMap |
| --- | --- | --- | --- |
| 线程安全 | 否 | 是（全量同步） | 是（分段锁 / CAS） |
| null 支持 | 允许 null 键 / 值 | 不允许 null | 不允许 null |
| 性能 | 单线程最优 | 低（全量同步开销） | 高（分段锁 / CAS） |
| 适用场景 | 单线程 | 多线程（已被弃用） | 高并发场景（如缓存） |


# 集合删除元素的正确方法是什么（Iterator.remove）？ArrayList 与 LinkedList 的空间占用情况如何？
集合删除元素的正确方法（以 ArrayList 为例）：

1. 普通 for 循环删除元素（错误示范）

```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "b", "c"));
for (int i = 0; i < list.size(); i++) {
    if ("b".equals(list.get(i))) {
        list.remove(i); // 会跳过元素，导致漏删
    }
}
System.out.println(list); // 输出：[a, b, c]（第二个 "b" 未被删除）
```

2. 增强 for 循环删除元素（错误示范）

```java
for (String s : list) {
    if ("b".equals(s)) {
        list.remove(s); // 抛出 ConcurrentModificationException
    }
}
```

3. 正确方式：使用 Iterator.remove ()

```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if ("b".equals(s)) {
        it.remove(); // 安全删除当前元素
    }
}
```

4. 最简洁的方式：调用用removeIf()（jdk8+）

```java
list.removeIf("b"::equals);
```

原理：

+ Iterator.remove() 会更新迭代器的内部状态（如 expectedModCount），避免并发修改异常。
+ 仅能删除当前迭代的元素（通过 next() 返回的元素）。

# HashMap 的原理？
HashMap 是 Java 里常用的键值对存储容器，查找的时间复杂度接近 O (1)。它的底层是用“数组 + 链表 / 红黑树” 实现的。

定位:

一个键值对塞到HashMap里怎么找到自己的桶呢，靠的是key的hashCode方法算出哈希值，为了减少哈希冲突，还做了扰动处理，就是把哈希值右移 16 位再和自身异或，然后利用(n-1)&hash算出数组下标，找到对应的桶。

冲突：

在jdk8以前是利用链表来处理哈希冲突，但链表一长呢查找性能就差，所以从jdk8开始，当链表长度超过8且数组大小大于等于64时就会将链表转为红黑树，因为红黑树查找的时间复杂度是O(logn)，不过java也很节俭，当红黑树节点小于6时又会退化为链表，避免维护树的额外开销。

扩容：

HashMap默认的容量是16，负载因子是0.75，意思是超过12个元素后就会触发扩容，容量会翻倍，扩容会重新计算元素的下标，为了避免频繁扩容影响性能，如果一开始就知道大概容量，在初始化的时候就应该指定容量，避免反复扩容。

并发：

+ 多线程下扩容会死循环(jdk8将链表的头插法改成了尾插法解决了这个问题)
+ 多线程下 put 时如果两个元素索引位置相同会导致元素丢失
+ put 和 get 并发时会导致 get 到 null，也就是一个线程put后刚好扩容就可能导致另一个线程get到null

为了解决这个问题，Java 提供了线程安全的 HashMap 实现类 ConcurrentHashMap。

