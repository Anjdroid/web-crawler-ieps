package com.company;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    public static void main(String[] args) {

        // change localhost:5432/<your_dbname> user:<your_username> password:<your_password>
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:5432/webcrawlerdb", "postgres", "postgres")) {

            System.out.println("Connected to PostgreSQL database!");


            /* Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM crawldb.image");
            while (resultSet.next()) {
                System.out.printf("%-30.30s  %-30.30s%n", resultSet.getString("model"), resultSet.getString("price"));
            }
            */
        } catch (SQLException e) {
            System.out.println("Connection failure.");
            e.printStackTrace();
        }
    }

}
