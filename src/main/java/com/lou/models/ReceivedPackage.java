package com.lou.models;

import java.util.List;

public class ReceivedPackage {
    private String identity;
    private List<ReceivedPackageItem> items;
    private String installLocation;
    private String crc16Code;

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public List<ReceivedPackageItem> getItems() {
        return items;
    }

    public void setItems(List<ReceivedPackageItem> items) {
        this.items = items;
    }

    public String getInstallLocation() {
        return installLocation;
    }

    public void setInstallLocation(String installLocation) {
        this.installLocation = installLocation;
    }

    public String getCrc16Code() {
        return crc16Code;
    }

    public void setCrc16Code(String crc16Code) {
        this.crc16Code = crc16Code;
    }

    @Override
    public String toString() {
        return "ReceivedPackage{" +
                "identity='" + identity + '\'' +
                ", items=" + items +
                ", installLocation='" + installLocation + '\'' +
                ", crc16Code='" + crc16Code + '\'' +
                '}';
    }
}

