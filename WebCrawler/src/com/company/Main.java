package com.company;

import com.company.DB.DBManager;

import java.sql.Connection;

public class Main {

    private static String url = "jdbc:postgresql://localhost:5432/";
    private static String dbName = "webcrawlerdb";
    private static String user = "postgres";
    private static String password = "postgres";
    public static DBManager db;
    public static Connection conn;

    public static void main(String[] args) {

        db = new DBManager(url+dbName, user, password);
        conn = db.connect();

        WebCrawler theAmazingSpiderman = new WebCrawler();
        theAmazingSpiderman.getPageLinks("http://www.mkyong.com/");
    }

}
