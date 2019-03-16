package com.company.DB;

import com.company.Main;

import java.sql.*;
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

    public int savePage(int siteId, String pageTypeCode, String url, String htmlContent, int httpStatusCode, Timestamp tm) {
        /*String code = "";
        String q = "SELECT code FROM crawldb.page_type WHERE code = HTML";
        try {
            PreparedStatement ps = Main.conn.prepareStatement(q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                code = rs.getString("code");
            }
            ps.close();

            LOGGER.info("code "+code);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }*/

        int pageId = 0;
        String query = "INSERT INTO crawldb.page(site_id, page_type_code, url, html_content, http_status_code, accessed_time) VALUES(?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);

            pst.setInt(1, siteId);
            pst.setString(2, pageTypeCode);
            pst.setString(3, url);
            pst.setString(4, htmlContent);
            pst.setInt(5, httpStatusCode);
            pst.setTimestamp(6, tm);
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();

            if (rs.next()) {
                pageId = rs.getInt(1);
            }
            LOGGER.info("PAGE ID after inserted: "+pageId);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return pageId;
    }

    public int saveSite(String domain, String robotsContent, String sitemapContent) {
        int siteId = 0;
        String query = "INSERT INTO crawldb.site(domain, robots_content, sitemap_content) VALUES(?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, domain);
            pst.setString(2, robotsContent);
            pst.setString(3, sitemapContent);
            pst.executeUpdate();

            ResultSet rs = pst.getGeneratedKeys();

            if (rs.next()) {
                siteId = rs.getInt(1);
            }
            LOGGER.info("PAGE ID after inserted: "+siteId);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return siteId;
    }

    public void saveImage(String filename, String contentType, byte[] content, Timestamp tm, int pageId) {
        String query = "INSERT INTO crawldb.image(page_id, filename, content_type, data, accessed_time) VALUES(?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            pst.setInt(1, pageId);
            pst.setString(2, filename);
            pst.setString(3, contentType);
            pst.setBytes(4, content);
            pst.setTimestamp(5, tm);
            pst.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePageData() {

    }



}
