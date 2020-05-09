package com.lou.models;

import com.lou.BizException;
import com.lou.ConvertHelper;
import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class ReceivedPackageFactory {
    public static ReceivedPackage buildPackage(byte[] bs) {
        validReceivedPackage(bs);
        ByteBuffer buffer = ByteBuffer.wrap(bs);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        final int itemsCount = (bs.length - 35) / ReceivedPackageItem.BYTES_COUNT;
        final ReceivedPackage receivedPackage = new ReceivedPackage();
        int index = 0;
        byte[] identityBytes = new byte[1];
        buffer.get(identityBytes, 0, identityBytes.length);
        receivedPackage.setIdentity(new String(identityBytes, StandardCharsets.US_ASCII));
        index++;
        final ArrayList<ReceivedPackageItem> receivedPackageItems = new ArrayList<>(itemsCount);
        for (int i = 0; i < itemsCount; i++) {
            receivedPackageItems.add(buildReceivedPackageItem(buffer, i));
        }
        receivedPackage.setItems(receivedPackageItems);
        index += itemsCount * ReceivedPackageItem.BYTES_COUNT;
        //拿byte数组，然后转hexString
        byte[] locationBytes = new byte[32];
        buffer.position(index);
        buffer.get(locationBytes, 0, locationBytes.length);
        receivedPackage.setInstallLocation(Hex.encodeHexString(locationBytes, false));
        byte[] crc16Bytes = new byte[2];
        index += 32;
        buffer.position(index);
        buffer.get(crc16Bytes, 0, crc16Bytes.length);
        receivedPackage.setCrc16Code(Hex.encodeHexString(crc16Bytes, false));
        return receivedPackage;
    }

    public static ReceivedPackageItem buildReceivedPackageItem(ByteBuffer buffer, int pageIndex) {
        ReceivedPackageItem item = new ReceivedPackageItem();
        int startIndex = 1 + pageIndex * ReceivedPackageItem.BYTES_COUNT;
        item.setIdNo(buffer.getInt(startIndex));
        startIndex += 4;
        long tickets = buffer.getInt(startIndex);//定义成int要溢出，再说一遍
        item.setGatherDate(ConvertHelper.getDate(tickets * 1000));
        startIndex += 4;
        item.setDtuId(buffer.getShort(startIndex));
        startIndex += 2;
        item.setMeterType(buffer.get(startIndex));
        startIndex++;
        item.setMeterAddr(buffer.get(startIndex));
        startIndex++;
        item.setValue1(buffer.getFloat(startIndex));
        startIndex += 4;
        item.setValue2(buffer.getFloat(startIndex));
        startIndex += 4;
        item.setValue3(buffer.getFloat(startIndex));
        startIndex += 4;
        item.setValue4(buffer.getFloat(startIndex));
        startIndex += 4;
        item.setBoardCardVoltage(buffer.getFloat(startIndex));
        return item;
    }

    public static ReceivedPackageItem buildReceivedPackageItem(byte[] bs, int pageIndex) {
        final ReceivedPackageItem item = new ReceivedPackageItem();
        int offset = 1 + pageIndex * ReceivedPackageItem.BYTES_COUNT;
        int length = 4;//id的位数
        item.setIdNo(ConvertHelper.getInt(bs, offset, length));
        offset += length;
        length = 4;//采集时间的位数
        final long tickets = ConvertHelper.getInt(bs, offset, length);//int后面乘以1000会溢出
        item.setGatherDate(ConvertHelper.getDate(tickets * 1000));
        offset += length;
        length = 2;
        item.setDtuId(ConvertHelper.getShort(bs, offset, length));
        offset += length;
        length = 1;
        item.setMeterType(bs[offset]);
        offset += length;
        length = 1;
        item.setMeterAddr(bs[offset]);
        offset += length;
        length = 4;
        item.setValue1(ConvertHelper.getFloat(bs, offset, length));
        offset += length;
        length = 4;
        item.setValue2(ConvertHelper.getFloat(bs, offset, length));
        offset += length;
        length = 4;
        item.setValue3(ConvertHelper.getFloat(bs, offset, length));
        offset += length;
        length = 4;
        item.setValue4(ConvertHelper.getFloat(bs, offset, length));
        offset += length;
        length = 4;
        item.setBoardCardVoltage(ConvertHelper.getFloat(bs, offset, length));

        return item;
    }

    public static void validReceivedPackage(byte[] bs) {
        if ((bs.length - 35) % 32 != 0)
            throw new BizException("数据长度不正确");
    }

}
