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
    private static int numberOfThreads = 5;

    public static void main(String[] args) {

        // connect to database
        db = new DBManager(url + dbName, user, password);
        conn = db.connect();

        // initialize scheduler
        scheduler = new Scheduler();

        // start crawler with multiple threads
        startThreads(numberOfThreads);

    }

    public static void startThreads(int numberOfThreads) {

        // start threads
        for (int i = 0; i < numberOfThreads; i++) {
            Thread object = new Thread(new WebCrawler());
            object.start();
        }
    }

}
