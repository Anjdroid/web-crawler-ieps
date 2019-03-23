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

    public int savePage(Page page) {
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

            pst.setInt(1, page.getSiteId());
            pst.setString(2, page.getPageTypeCode());
            pst.setString(3, page.getUrl());
            pst.setString(4, page.getHtmlContent());
            pst.setInt(5, page.getHttpStatusCode());
            pst.setTimestamp(6, page.getAccessedTime());
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

    public int getSiteFromDomain(String domain) {
        int siteId = 0;
        String query = "SELECT id FROM crawldb.site WHERE domain =\'"+ domain + "\' ";
        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                siteId = rs.getInt("id");
            }
            System.out.println("site id"+ siteId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return siteId;
    }

    public int getPageFromUrl(String url) {
        int pageId = 0;
        String query = "SELECT id FROM crawldb.page WHERE url =\'"+ url + "\' ";
        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                pageId = rs.getInt("id");
            }
            System.out.println("page id"+ pageId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pageId;
    }

    public int saveSite(Site site) {

        int siteId = 0;
        String query = "INSERT INTO crawldb.site(domain, robots_content, sitemap_content) VALUES(?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query,Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, site.getDomain());
            pst.setString(2, site.getRobotsRontent());
            pst.setString(3, site.getSitemapContent());
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

    public void saveImage(Image image) {
        String query = "INSERT INTO crawldb.image(page_id, filename, content_type, data, accessed_time) VALUES(?, ?, ?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            pst.setInt(1, image.getPageId());
            pst.setString(2, image.getFilename());
            pst.setString(3, image.getContentType());
            pst.setBytes(4, image.getData());
            pst.setTimestamp(5, image.getAccessedTime());
            pst.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void savePageData(PageData pageData) {

        String query = "INSERT INTO crawldb.page_data(page_id, data_type_code, data) VALUES(?, ?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            pst.setInt(1, pageData.getPageId());
            pst.setString(2, pageData.getDataTypeCode());
            pst.setBytes(3, pageData.getData());
            pst.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void setLinkToFromPage(Link link) {
        String query = "INSERT INTO crawldb.link(from_page, to_page) VALUES(?, ?)";

        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            pst.setInt(1, link.getFromPage());
            pst.setInt(2, link.getToPage());
            pst.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



}
