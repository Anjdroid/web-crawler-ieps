package com.company;

import com.company.DB.DBManager;

import javax.management.relation.RoleUnresolved;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Main {

    protected static String url = "jdbc:postgresql://localhost:5432/";
    protected static String dbName = "webcrawlerdb";
    protected static String user = "postgres";
    protected static String password = "postgres";
    public static DBManager db;
    //public static Connection conn;
    public static Scheduler scheduler;
    private static int numberOfThreads = 20;
    private static List<Runnable> threads;

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        // initialize scheduler
        scheduler = new Scheduler();

        db = new DBManager(url + dbName, user, password);

        threads = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            Runnable theAmazingSpiderman = new WebCrawler();
            executor.execute(theAmazingSpiderman);
        }
        executor.shutdown();
        // Wait until all threads are finish
        while (!executor.isTerminated()) {

        }

        // start crawler with multiple threads
        //startThreads(numberOfThreads);

        //for (int i = 0; i < numberOfThreads; i++) {
        //    threads.get(i).stop();
        //}

    }

    public static void startThreads(int numberOfThreads) {

        // start threads
        for (int i = 0; i < numberOfThreads; i++) {
            Runnable object = new WebCrawler();
            threads.add(object);
            object.run();
        }
    }

}
