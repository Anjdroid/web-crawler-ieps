package com.company.DB;

import java.sql.Timestamp;

public class Page {

    private int id;
    private int siteId;
    private String pageTypeCode;
    private String url;
    private String htmlContent;
    private int httpStatusCode;
    private Timestamp accessedTime;
    private int hashcode;

    public Page() { }

    public Page(int siteId, String pageTypeCode, String url, String htmlContent, int httpStatusCode, Timestamp accessedTime, int hashcode) {
        this.siteId = siteId;
        this.pageTypeCode = pageTypeCode;
        this.url = url;
        this.htmlContent = htmlContent;
        this.httpStatusCode = httpStatusCode;
        this.accessedTime = accessedTime;
        this.hashcode = hashcode;
    }

    public void setAccessedTime(Timestamp accessedTime) {
        this.accessedTime = accessedTime;
    }

    public Timestamp getAccessedTime() {
        return accessedTime;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setPageTypeCode(String pageTypeCode) {
        this.pageTypeCode = pageTypeCode;
    }

    public String getPageTypeCode() {
        return pageTypeCode;
    }

    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    public int getSiteId() {
        return siteId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setHashcode(int hashcode) {
        this.hashcode = hashcode;
    }

    public int getHashcode() {
        return hashcode;
    }
}
