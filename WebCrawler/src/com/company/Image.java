package com.company;

import java.sql.Timestamp;

public class Image {

    private int id;
    private int pageId;
    private String filename;
    private String contentType;
    private Timestamp accessedTime;
    private byte[] data;

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setAccessedTime(Timestamp accessedTime) {
        this.accessedTime = accessedTime;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    public Timestamp getAccessedTime() {
        return accessedTime;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFilename() {
        return filename;
    }

    public int getPageId() {
        return pageId;
    }

    public int getId() {
        return id;
    }
}
