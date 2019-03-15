package com.company.DB;

public class PageData {

    private int id;
    private int pageId;
    private String dataTypeCode;
    private byte[] data;

    public PageData(int pageId, String dataTypeCode, byte[] data) {
        this.pageId = pageId;
        this.dataTypeCode = dataTypeCode;
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getDataTypeCode() {
        return dataTypeCode;
    }

    public void setDataTypeCode(String dataTypeCode) {
        this.dataTypeCode = dataTypeCode;
    }

    public int getPageId() {
        return pageId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
