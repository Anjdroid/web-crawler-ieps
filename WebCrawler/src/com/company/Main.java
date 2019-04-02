package com.company;

import com.company.DB.DBManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    protected static String url = "jdbc:postgresql://localhost:5432/";
    protected static String dbName = "webcrawlerdb";
    protected static String user = "postgres";
    protected static String password = "postgres";
    public static DBManager db;
    public static Scheduler scheduler;
    private static int numberOfThreads = 40;

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // initialize scheduler
        scheduler = new Scheduler();

        db = new DBManager(url + dbName, user, password);

        for (int i = 0; i < numberOfThreads; i++) {
            Runnable theAmazingSpiderman = new WebCrawler();
            executor.execute(theAmazingSpiderman);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {

        }
    }

}
