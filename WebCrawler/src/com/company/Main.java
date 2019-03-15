package com.company;

public class Main {

    private static String url = "jdbc:postgresql://localhost:5432/";
    private static String dbName = "webcrawlerdb";
    private static String user = "postgres";
    private static String password = "postgres";

    public static void main(String[] args) {

        DBManager db = new DBManager(url+dbName, user, password);
        db.connect();

    }

}
