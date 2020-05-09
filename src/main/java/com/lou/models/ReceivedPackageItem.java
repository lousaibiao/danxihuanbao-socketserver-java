package com.lou.models;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.Date;

public class ReceivedPackageItem {
    //每一项占的字节数
    public static final int BYTES_COUNT = 32;
    private int idNo;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private Date gatherDate;
    private short dtuId;
    private byte meterType;
    private byte meterAddr;
    private float value1;
    private float value2;
    private float value3;
    private float value4;
    private float boardCardVoltage;

    public int getIdNo() {
        return idNo;
    }

    public void setIdNo(int idNo) {
        this.idNo = idNo;
    }

    public Date getGatherDate() {
        return gatherDate;
    }

    public void setGatherDate(Date gatherDate) {
        this.gatherDate = gatherDate;
    }

    public short getDtuId() {
        return dtuId;
    }

    public void setDtuId(short dtuId) {
        this.dtuId = dtuId;
    }

    public byte getMeterType() {
        return meterType;
    }

    public void setMeterType(byte meterType) {
        this.meterType = meterType;
    }

    public byte getMeterAddr() {
        return meterAddr;
    }

    public void setMeterAddr(byte meterAddr) {
        this.meterAddr = meterAddr;
    }

    public float getValue1() {
        return value1;
    }

    public void setValue1(float value1) {
        this.value1 = value1;
    }

    public float getValue2() {
        return value2;
    }

    public void setValue2(float value2) {
        this.value2 = value2;
    }

    public float getValue3() {
        return value3;
    }

    public void setValue3(float value3) {
        this.value3 = value3;
    }

    public float getValue4() {
        return value4;
    }

    public void setValue4(float value4) {
        this.value4 = value4;
    }

    public float getBoardCardVoltage() {
        return boardCardVoltage;
    }

    public void setBoardCardVoltage(float boardCardVoltage) {

        this.boardCardVoltage = boardCardVoltage;
    }

    @Override
    public String toString() {
        return "ReceivedPackageItem{" +
                "idNo=" + idNo +
                ", gatherDate=" + gatherDate +
                ", dtuId=" + dtuId +
                ", meterType=" + meterType +
                ", meterAddr=" + meterAddr +
                ", value1=" + value1 +
                ", value2=" + value2 +
                ", value3=" + value3 +
                ", value4=" + value4 +
                ", boardCardVoltage=" + boardCardVoltage +
                '}';
    }
}
