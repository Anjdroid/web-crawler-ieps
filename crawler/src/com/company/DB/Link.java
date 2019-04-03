package com.company.DB;

public class Link {
    private int fromPage;
    private int toPage;

    public Link(int fromPage, int toPage) {
        this.fromPage = fromPage;
        this.toPage = toPage;
    }

    public void setToPage(int toPage) {
        this.toPage = toPage;
    }

    public int getToPage() {
        return toPage;
    }

    public void setFromPage(int fromPage) {
        this.fromPage = fromPage;
    }

    public int getFromPage() {
        return fromPage;
    }
}
