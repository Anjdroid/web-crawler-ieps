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
    public static Scheduler scheduler;

    public static void main(String[] args) {

        db = new DBManager(url + dbName, user, password);
        conn = db.connect();

        scheduler = new Scheduler();

        startThreads(5);

    }

    public static void startThreads(int numberOfThreads) {

        for (int i = 0; i < numberOfThreads; i++) {
            Thread object = new Thread(new WebCrawler());
            object.start();
        }
    }

}
