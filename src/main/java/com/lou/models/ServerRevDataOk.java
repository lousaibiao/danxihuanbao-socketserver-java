package com.lou.models;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ServerRevDataOk {
    private String serverRevDataOkStr = "@ServerRevDataOk@";
    private Date curDate = new Date();

    public byte[] getBytes() {
        //21字节。17+4
        ByteBuffer buffer = ByteBuffer.allocate(21);//分配一次，后面按需放入
        buffer.put(serverRevDataOkStr.getBytes(StandardCharsets.US_ASCII));
//        System.out.println(Arrays.toString(buffer.array()));
        int ticket = (int) (curDate.getTime() / 1000);
//        final Calendar instance = Calendar.getInstance(TimeZone.getDefault());
//        instance.set(1991, 1, 1, 11, 11, 11);
//        ticket = (int) (instance.getTimeInMillis() / 1000);
//        System.out.println(ticket);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
//        System.out.println(buffer.order());
//        buffer.putInt(17, ticket);
//        System.out.println(Arrays.toString(buffer.array()));
//        System.out.println(ticket);
//        buffer.order(ByteOrder.BIG_ENDIAN);
//        System.out.println(buffer.order());
        buffer.putInt(17, ticket);
        System.out.println(Arrays.toString(buffer.array()));
        return buffer.array();
    }
}
