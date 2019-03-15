package com.company;

public class Site {

    private int id;
    private String domain;
    private String robotsRontent;
    private String sitemapContent;

    public void setSitemapContent(String sitemapContent) {
        this.sitemapContent = sitemapContent;
    }

    public String getSitemapContent() {
        return sitemapContent;
    }

    public void setRobotsRontent(String robotsRontent) {
        this.robotsRontent = robotsRontent;
    }

    public String getRobotsRontent() {
        return robotsRontent;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
