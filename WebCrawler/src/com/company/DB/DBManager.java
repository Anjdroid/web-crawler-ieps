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
        int siteId = -1;
        String query = "SELECT id FROM crawldb.site WHERE domain =\'"+ domain + "\' ";
        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                siteId = rs.getInt("id");
            }
            LOGGER.info("site id from domain "+ siteId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return siteId;
    }

    public int getPageFromHash(int hash) {
        String query = "SELECT id \n" +
                "       \n" +
                "  FROM crawldb.page WHERE hashcode =\'"+ hash + "\'";

        int id = -1;
        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                id = rs.getInt("id");
            }
            LOGGER.info("getting page id from hash "+ id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }

    public Page getPageFromUrl(String url) {
        String query = "SELECT id, site_id, http_status_code \n" +
                "       \n" +
                "  FROM crawldb.page WHERE url =\'"+ url + "\'";

        Page p = new Page();
        try {
            PreparedStatement pst = Main.conn.prepareStatement(query);

            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                p.setId(rs.getInt("id"));
                p.setSiteId(rs.getInt("site_id"));
                p.setHttpStatusCode(rs.getInt("http_status_code"));
            }
            LOGGER.info("getting page id from url "+ p.getHttpStatusCode());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return p;
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
            LOGGER.info("SITE ID after inserted: "+siteId);

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

            LOGGER.info("image inserted ");

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

            LOGGER.info("page data inserted ");

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

            LOGGER.info("link from to inserted "+link.getFromPage() + " "+ link.getToPage());

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }



}
