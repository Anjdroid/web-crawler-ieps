package com.company;

import com.company.DB.DBManager;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.Queue;

public class Main {

    private static String url = "jdbc:postgresql://localhost:5432/";
    private static String dbName = "webcrawlerdb";
    private static String user = "postgres";
    private static String password = "postgres";
    public static DBManager db;
    public static Connection conn;
    public static Scheduler scheduler;

    public static void main(String[] args) {

        db = new DBManager(url+dbName, user, password);
        conn = db.connect();

        scheduler = new Scheduler();

        //WebCrawler theAmazingSpiderman = new WebCrawler();

        startThreads(1);

        //theAmazingSpiderman.getPageLinks("http://www.mkyong.com/");

    }

    public static void startThreads(int numberOfThreads) {
        // pass url from frontier

        Queue <String> frontier = scheduler.getFrontier();

        for (int i=0; i < numberOfThreads; i++)
        {
            if (!frontier.isEmpty()) {
                String URL = frontier.poll();
                Thread object = new Thread(new WebCrawler("http://"+URL));
                object.start();
            }
        }
    }

}
