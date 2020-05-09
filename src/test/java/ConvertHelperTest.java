import com.alibaba.fastjson.JSON;
import com.lou.App;
import com.lou.ConvertHelper;
import com.lou.models.ReceivedPackage;
import com.lou.models.ReceivedPackageFactory;
import com.lou.models.ReceivedPackageItem;
import com.lou.models.ServerRevDataOk;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

public class ConvertHelperTest {
    private static ByteBuffer byteBuffer;

    /**
     * 1.通过hexString转成字节数组。
     * 2.通过offset和length，自己控制每次需要的字节数
     * 3.每次构造一个ByteBuffer对象put进第二步的数组，get方法来获取对应的值
     * 缺点：每次要构造ByteBuffer对象，做一些重复的设置。
     */
    @Test
    public void test1() {
        String hexStr = "40 94 08 00 00 53 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 05 82 49 41 93 08 00 00 17 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 34 66 49 41 8E 7C";
        final String[] hexStrs = hexStr.split(" ");
        byte[] sourceBytes = new byte[hexStrs.length];
        for (int i = 0; i < hexStrs.length; i++) {
            //16进制的str转成byte，原始数据
            sourceBytes[i] = (byte) (Integer.valueOf(hexStrs[i], 16) & 0xff);
        }
        System.out.println(Arrays.toString(hexStrs));
        System.out.println(Arrays.toString(sourceBytes));
        String[] newStrs = new String[sourceBytes.length];
        for (int i = 0; i < sourceBytes.length; i++) {
            //字节转成16进制表示
            newStrs[i] = String.format("%02X", sourceBytes[i]);
        }
        System.out.println(Arrays.toString(newStrs));
        final String hexStr2 = String.join(" ", newStrs);
        Assert.assertEquals(hexStr, hexStr2);

        final String identitySymble = new String(sourceBytes, 0, 1, StandardCharsets.US_ASCII);
        System.out.println(identitySymble);
        Assert.assertEquals("@", identitySymble);

//        byteBuffer = ByteBuffer.allocateDirect(Integer.BYTES);
        //数据ID号
        int offset = 1, length = Integer.BYTES;//过一段计数一下
        final int idNo = ConvertHelper.getInt(sourceBytes, offset, length);
        Assert.assertEquals(2196, idNo);
        //采集时间
        offset += length;
        length = Integer.BYTES;
        final int tickets = ConvertHelper.getInt(sourceBytes, offset, length);
        Assert.assertEquals(1587404371, tickets);
        //DTU编号
        offset += length;
        length = Short.BYTES;
        final short dtuId = ConvertHelper.getShort(sourceBytes, offset, length);
        Assert.assertEquals(1, dtuId);

        //仪表类型
        offset += length;//用完要往后移
        length = 1;
        final byte meterType = sourceBytes[offset];
        Assert.assertEquals(4, meterType);
        //仪表地址
        length = 1;
        offset += length;
        final byte meterAddr = sourceBytes[offset];
        Assert.assertEquals(1, meterAddr);
        //仪表数据1
        offset += length;//移动上一次
        length = Float.BYTES;//新数据的字节数

        final float value1 = ConvertHelper.getFloat(sourceBytes, offset, length);
        Assert.assertEquals(0.0, value1, 0.001);
        //仪表数据2
        offset += length;//移动上一次
        length = Float.BYTES;//新数据的字节数
        final float value2 = ConvertHelper.getFloat(sourceBytes, offset, length);
        Assert.assertEquals(0.0, value2, 0.001);

        //仪表数据3
        offset += length;//移动上一次
        length = Float.BYTES;//新数据的字节数
        final float value3 = ConvertHelper.getFloat(sourceBytes, offset, length);
        Assert.assertEquals(18.466047, value3, 0.001);
        //仪表数据4
        offset += length;//移动上一次
        length = Float.BYTES;//新数据的字节数
        final float value4 = ConvertHelper.getFloat(sourceBytes, offset, length);
        Assert.assertEquals(0.0, value4, 0.001);

        //板卡电压值
        offset += length;
        length = Float.BYTES;
        final float boardCardVoltage = ConvertHelper.getFloat(sourceBytes, offset, length);
        Assert.assertEquals(12.594243, boardCardVoltage, 0.001);

    }

    @Test
    public void test2() {
        final ServerRevDataOk serverRevDataOk = new ServerRevDataOk();
        System.out.println("out:" + Arrays.toString(serverRevDataOk.getBytes()));
    }

    /**
     * 1. 获取字节数组
     * 2. wrap一个ByteBuffer对象
     * 3. 设置他的byteOrder等值
     * 4. 通过buffer的get方法制定的index参数来表示从哪一位开始获取对应的值
     */
    @Test
    public void test3() {
        String hexStr = "40 94 08 00 00 53 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 05 82 49 41 93 08 00 00 17 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 34 66 49 41 8E 7C";
        final String[] hexStrs = hexStr.split(" ");
        byte[] sourceBytes = new byte[hexStrs.length];
        for (int i = 0; i < hexStrs.length; i++) {
            //16进制的str转成byte，原始数据index
            sourceBytes[i] = (byte) (Integer.valueOf(hexStrs[i], 16) & 0xff);
        }
        int index = 0;
        ByteBuffer buffer = ByteBuffer.wrap(sourceBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        byte b = buffer.get(index);
        String identitySymbol = new String(new byte[]{b}, StandardCharsets.US_ASCII);

        Assert.assertEquals("@", identitySymbol);
        index++;
        int anInt = buffer.getInt(1);
        System.out.println(anInt);
        index += 4;
        System.out.println(buffer.getInt(index));
        index += 4;
        System.out.println(buffer.getShort(index));
        index += 2;
        System.out.println(buffer.get(index));
        index++;
        System.out.println(buffer.get(index));
        index++;
        System.out.println(buffer.getFloat(index));
        index += 4;
        System.out.println(buffer.getFloat(index));
        index += 4;
        System.out.println(buffer.getFloat(index));
        index += 4;
        System.out.println(buffer.getFloat(index));
        index += 4;
        System.out.println(buffer.getFloat(index));


    }

    @Test
    public void test4() {
        String hexStr = "40 94 08 00 00 53 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 05 82 49 41 93 08 00 00 17 DE 9D 5E 01 00 04 01 00 00 00 00 00 00 00 00 77 BA 93 41 00 00 00 00 34 66 49 41 8E 7C";
        final String[] hexStrs = hexStr.split(" ");
        byte[] sourceBytes = new byte[hexStrs.length];
        for (int i = 0; i < hexStrs.length; i++) {
            //16进制的str转成byte，原始数据index
            sourceBytes[i] = (byte) (Integer.valueOf(hexStrs[i], 16) & 0xff);
        }
        ReceivedPackage receivedPackage = ReceivedPackageFactory.buildPackage(sourceBytes);
        System.out.println(JSON.toJSONString(receivedPackage));
    }

    @Test
    public void test5() {
        int value = 123;
        System.out.println(String.format("%07d", value));
        System.out.println(String.format("%d", value % 100));
        final ReceivedPackageItem item = new ReceivedPackageItem();
        item.setDtuId((short) 111);
        App.handleReceivedPackageItem(item);
    }

    @Test
    public void getTableNameTest(){
        System.out.println(App.getTableName(100));
    }

}