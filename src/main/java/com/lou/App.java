package com.lou;

import com.alibaba.fastjson.JSON;
import com.lou.models.ReceivedPackage;
import com.lou.models.ReceivedPackageFactory;
import com.lou.models.ReceivedPackageItem;
import com.lou.models.ServerRevDataOk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.codec.binary.Hex;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.*;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
    //Deque是双向队列
    //阻塞队列
    private static BlockingQueue<ReceivedPackage> blockingQueue = new LinkedBlockingQueue<>();
    private static final ResourceBundle appconfig = ResourceBundle.getBundle("appconfig");


    public static void main(String[] args) throws IOException {
        final int port = Integer.parseInt(appconfig.getString("server.port"));
        final String ip = appconfig.getString("server.ip");
        logger.info("监听端口{}", port);
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
        logger.info("启动消费队列{}", t.getId());
        final ServerSocket serverSocket = new ServerSocket(port);
        while (true) {//可以指定一个退出条件，或者一直保持接收
            try (final Socket clientSocket = serverSocket.accept();//自动释放,同时会阻塞当前进程
            ) {
                logger.info("{}连接成功,开始接收数据。等待下一个连接", clientSocket.getRemoteSocketAddress());
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                int maxLength = 355;//35+32x,35+320= 355 最多10个数据
                byte[] buffer = new byte[maxLength];
                final InputStream inputStream = clientSocket.getInputStream();
                int n = inputStream.read(buffer, 0, buffer.length);
                logger.info("读取到{}字节", n);
                byteArrayOutputStream.write(buffer, 0, n);
                if ((n - 35) % 32 != 0) {
                    logger.info("接收到字节数不正确，忽略当前数据包");
                    continue;
                }
                final byte[] bytes = byteArrayOutputStream.toByteArray();
                logger.info("读取到的字节数组:{}", Arrays.toString(bytes));
                final String hexString = Hex.encodeHexString(bytes, false);
                logger.info("读取到的字节HexString:{}", hexString);
                final byte[] decodeHex = Hex.decodeHex(hexString);
                logger.info("解码生成的HexString得到数组:{}", Arrays.toString(decodeHex));
                //处理得到的byte[]
                final ReceivedPackage receivedPackage = ReceivedPackageFactory.buildPackage(bytes);
                logger.info("转换成json:{}", JSON.toJSONString(receivedPackage));
                //加入队列
                if (!blockingQueue.offer(receivedPackage)) {
                    logger.info("数据添加失败{}", receivedPackage);
                }
                OutputStream outputStream = clientSocket.getOutputStream();
                byte[] rspOkBytes = new ServerRevDataOk().getBytes();
                outputStream.write(rspOkBytes);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


    }

    public static void handleReceivedPackageItem(ReceivedPackageItem item) {
        logger.info("开始处理{}元素", item.getIdNo());
        //查出startDataIndex
        String connStr = String.format("jdbc:sqlserver://%s;database=%s;user=%s;password=%s",
                appconfig.getString("db.server"),
                appconfig.getString("db.database"),
                appconfig.getString("db.user"),
                appconfig.getString("db.password"));
        System.out.println(connStr);
        try (final Connection connection = DriverManager.getConnection(connStr);
             final Statement statement = connection.createStatement()
        ) {
            String sql = String.format("select top 1 DataIndex from SensorInfo where StationId='%d' order by DataIndex desc;", item.getDtuId());
            System.out.println("执行语句:" + sql);
            ResultSet resultSet = statement.executeQuery(sql);
            int maxDataIndex = 0;
            while (resultSet.next()) {
                System.out.println("查询到的dataIndex:" + resultSet.getInt(1));//第一列是1
                maxDataIndex = resultSet.getInt(1);
            }
            System.out.println("maxDataIndex:" + maxDataIndex);
            int startIndex = 0;
            if (maxDataIndex != 0) {
                startIndex = maxDataIndex + 5;//空5格，可能后期会加传感器
                if (startIndex % 100 > 90) {//快到结尾了
                    startIndex = (startIndex / 100 + 1) * 100;
                }
            }
            final String tableName = getTableName(startIndex);

            final boolean timeExists = checkDateTimeExists(statement, tableName, item.getGatherDate());
            String processItemSql = "";
            if (timeExists) {
                processItemSql = getUpdateSql(item, startIndex, tableName);
            } else {
                processItemSql = getInsertSql(item, startIndex, tableName);
            }
            statement.execute(processItemSql);//查东西返回true，update等返回false

            System.out.println("执行结果:" + statement.getUpdateCount());

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        logger.info("处理{}元素结束", item.getIdNo());

    }

    public static boolean checkDateTimeExists(Statement statement, String tableName, Date date) throws SQLException {

        String sql = String.format("select count(*) from %s where [Time] = '%s';", tableName, dateFormat.format(date));
        System.out.println(sql);
        final ResultSet resultSet = statement.executeQuery(sql);
        resultSet.next();
        final int timeCount = resultSet.getInt(1);
        System.out.println(timeCount);
        return timeCount > 0;
    }

    public static String getTableName(int dataIndex) {
        return String.format("HistoryData_%04d", dataIndex / 100);
    }

    public static String getColumnName(int dataIndex) {
        return String.format("S%d", dataIndex % 100);
    }

    public static String getInsertSql(ReceivedPackageItem item, int startIndex, String tableName) {
        //time value1-4, voltage,瞬时流量，高程
        String sqlFormat = "insert into %s ([Time],%s,%s,%s,%s,%s,%s,%s) values('%s','%f','%f','%f','%f','%f','%f','%f');";
        String sql = String.format(sqlFormat,
                tableName,
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                getColumnName(startIndex++),
                dateFormat.format(item.getGatherDate()), item.getValue1(), item.getValue2(), item.getValue3(), item.getValue4(), item.getBoardCardVoltage(), 12.3f, 5.6f);
        System.out.println("生成sql:" + sql);
        return sql;
    }

    public static String getUpdateSql(ReceivedPackageItem item, int startIndex, String tableName) {
        //time value1-4, voltage,瞬时流量，高程
        String sqlFormat = "update %s set %s='%f', %s='%f', %s='%f', %s='%f', %s='%f', %s='%f', %s='%f' where [Time]='%s'";
        String sql = String.format(sqlFormat,
                tableName,
                getColumnName(startIndex++), item.getValue1(),
                getColumnName(startIndex++), item.getValue2(),
                getColumnName(startIndex++), item.getValue3(),
                getColumnName(startIndex++), item.getValue4(),
                getColumnName(startIndex++), item.getBoardCardVoltage(),
                getColumnName(startIndex++), 12.3f,//测试用
                getColumnName(startIndex++), 5.6f,//测试用
                dateFormat.format(item.getGatherDate()));
        System.out.println("生成sql：" + sql);
        return sql;
    }
}

