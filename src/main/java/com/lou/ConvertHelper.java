package com.lou;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ConvertHelper {
    private static ByteBuffer byteBuffer;
    private static ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    public static int getInt(byte[] bs, int offset, int length) {
        byteBuffer = ByteBuffer.allocate(Integer.BYTES);
        byteBuffer.put(bs, offset, length);
        byteBuffer.order(byteOrder);
        byteBuffer.position(0);
        return byteBuffer.getInt();
    }

    public static short getShort(byte[] bs, int offset, int length) {
        byteBuffer = ByteBuffer.allocate(Short.BYTES);
        byteBuffer.put(bs, offset, length);
        byteBuffer.order(byteOrder);
        byteBuffer.position(0);
        return byteBuffer.getShort();
    }

    public static float getFloat(byte[] bs, int offset, int length) {
        byteBuffer = ByteBuffer.allocate(Float.BYTES);
        byteBuffer.put(bs, offset, length);
        byteBuffer.order(byteOrder);
        byteBuffer.position(0);
        return byteBuffer.getFloat();
    }

    public static Date getDate(long tickets) {
        final Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(tickets);
        calendar.add(Calendar.HOUR_OF_DAY, -8);
        return calendar.getTime();
    }
}
