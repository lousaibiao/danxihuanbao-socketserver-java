# 项目介绍

对接丹溪环保-java版本。框架为java 控制台项目，`jdk8`

# 基本逻辑

总体和.net core 版本一样。

1. 开线程循环收到的消息
2. 构造`ServerSocket`对象对socket连接进行while监听。
3. 获取连接，构造数据
   1. 构造一个大（35+32x）对象来接收数据。
   2. 通过read方法返回值截取实际的数据。
   3. write进`ByteArrayOutputStream`，然后转成最终`byte[]`
   4. wrap进`ByteBuffer`。最后get出特定的值。
4. 存数据进队列。

# 核心代码

## 读取配置文件

用 `ResourceBundle`

```java
final ResourceBundle appconfig = ResourceBundle.getBundle("appconfig");
final int port = Integer.parseInt(appconfig.getString("server.port"));
final String ip = appconfig.getString("server.ip");
```

## Try-With-Resources

详见[文档](https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html)。简单来说就是不需要自己去维护socket等资源的关闭dispose等操作。

```java
final ServerSocket serverSocket = new ServerSocket(port);
while (true) {//可以指定一个退出条件，或者一直保持接收
	try (final Socket clientSocket = serverSocket.accept();//自动释放
) {
        //...其他
}
```

## 读取以及回复socket

```java
final InputStream inputStream = clientSocket.getInputStream();//read这个inputStream可以获取到传过来的值。
final OutputStream outputStream = clientSocket.getOutputStream();//write这个outputStream可以对客户端进行回复。
```

## 对象转换

### 把字节转string等

核心是个`ByteBuffer` 对象。基本使用方法见注释。

```java
public static ReceivedPackage buildPackage(byte[] bs) {
    ByteBuffer buffer = ByteBuffer.wrap(bs);//把整个数组wrap起来。
    buffer.order(ByteOrder.LITTLE_ENDIAN);//设置字节的方式。传过来的数据结构决定。
    //...省略其他
    //这里需warp的对象里面的部分byte数组，然后转hexString
    //1.首先开一个接收数组，dst。
    byte[] locationBytes = new byte[32];
    //2.让buffer移动到需要的起点。
    buffer.position(index);
    //3.get出这个buffer从index，长度为locationBytes.length的字节。
    buffer.get(locationBytes, 0, locationBytes.length);
    receivedPackage.setInstallLocation(Hex.encodeHexString(locationBytes, false));
    //crc16校验同理
    return receivedPackage;
}
```

通过getFloat获取一个`float`值。startIndex为这个buffer的一个起点，因为float为4字节，所以会从startIndex开始，取4个字节来生成一个float值。

```java
float f = buffer.getFloat(startIndex);
```

### 把string等值转成byte[]

这里需要构造一个21字节的数组，内容分为两部分，一部分一串String的Ascii编码后的数组，另一部分为一个int值转换成小端表示。详见注释。

```java
public class ServerRevDataOk {
    private String serverRevDataOkStr = "@ServerRevDataOk@";
    private Date curDate = new Date();

    public byte[] getBytes() {
        //21字节。17+4
        //1. 开辟一块空间
        ByteBuffer buffer = ByteBuffer.allocate(21);//分配一次，后面按需放入
        //2. put方法将内容放入。
        buffer.put(serverRevDataOkStr.getBytes(StandardCharsets.US_ASCII));
        int ticket = (int) (curDate.getTime() / 1000);
        //3. 后面需要放int，需要设置一下小端的模式
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        //4. 从index=17开始，放入一个int转成的byte[]
        buffer.putInt(17, ticket);
        System.out.println(Arrays.toString(buffer.array()));
        //5. buffer.array()返回数组结果
        return buffer.array();
    }
}
```

日志里面的16进制表示的字符串通过Hex对象的`encodeHexString`以及`decodeHex`方法实现互转。

```java
final String hexString = Hex.encodeHexString(bytes, false);
//409408000053DE9D5E01000401000000000000000077BA934100000000058249419308000017DE9D5E01000401000000000000000077BA934100000000346649418E7C
final byte[] decodeHex = Hex.decodeHex(hexString);
[64, -108, 8, 0, 0, 83, -34, -99, 94, 1, 0, 4, 1, 0, 0, 0, 0, 0, 0, 0, 0, 119, -70]//省略部分
```

## 日志框架

选用SLF4J，[文档](http://www.slf4j.org/manual.html)。 slf4j属于api接口，实现类用Logback。

需要3个依赖。`slf4j-api`,`logback-classic`,`logback-core`。

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.30</version>
</dependency>
<!-- https://mvnrepository.com/artifact/ch.qos.logback/logback-classic -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.3</version>
</dependency>
<!-- https://mvnrepository.com/artifact/ch.qos.logback/logback-core -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-core</artifactId>
    <version>1.2.3</version>
</dependency>
```

```java
logger.info("{}连接成功,开始接收数据。等待下一个连接", clientSocket.getRemoteSocketAddress());
```

## 消费队列

用一个**阻塞队列**来实现等待的操作。`handleReceivedPackageItem`部分代码为业务代码，可以忽略。

```java
private static BlockingQueue<ReceivedPackage> blockingQueue = new LinkedBlockingQueue<>();

Thread t = new Thread(() -> {
    while (true) {
        try {
            logger.info("以30秒的超时获取队列数据，并将它移除队列");
            final ReceivedPackage receivedPackage = blockingQueue.poll(30, TimeUnit.SECONDS);
            if (receivedPackage == null) {
                logger.info("队列为空，重试");
                continue;
            }
            logger.info("获取到idNo为{}的一个元素", receivedPackage.getItems().get(0).getIdNo());
            receivedPackage.getItems().forEach(item -> {
                handleReceivedPackageItem(item);
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
});
t.start();
```

## 数据库

用最简答的`JDBC`来实施数据库的CRUD操作。[文档](https://docs.microsoft.com/en-us/sql/connect/jdbc/step-3-proof-of-concept-connecting-to-sql-using-java?view=sql-server-ver15)

```xml
<!-- https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <version>8.2.2.jre8</version>
</dependency>
```

基本流程

1. getConnection()
2. getStatement()
3. executeQuery 或execute。

