package com.company.DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DBManager {

    private String url;
    private String user;
    private String password;
    private final static Logger LOGGER = Logger.getLogger(DBManager.class.getName());

    public DBManager(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.password = pass;
    }

    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            LOGGER.info("Connected to the PostgreSQL server successfully.");
        } catch (SQLException e) {
            LOGGER.info("Error connecting to db.");
            e.printStackTrace();
        }

        return conn;
    }

}
