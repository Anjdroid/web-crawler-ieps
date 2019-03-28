package com.company;

import com.company.DB.*;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
//import org.jsoup.Connection; // To sem zakomentiral, ker drugače dobim error, ko import java.sql.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths; // Jan
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

import java.sql.Statement; // Jan

public class WebCrawler implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(DBManager.class.getName());
    private String parentPage;

    private String protocol = "http://";
    private List<String> docs;

    public WebCrawler() {
        docs = new ArrayList<>();
        docs.add("application/pdf");
        docs.add("application/vnd.ms-powerpoint");
        docs.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        docs.add("application/msword");
        docs.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    public void getPageLinks() {

        parentPage = null;

        while (!(Main.scheduler.getFrontier()).isEmpty()) {

            // get URL from frontier
            String pageToCrawl = Main.scheduler.getFrontier().poll();

            // -------------------
            // Jan
            HashMap<String, ArrayList<String>> allow = new HashMap<>();
            HashMap<String, ArrayList<String>> disallow = new HashMap<>();
            HashMap<String, Integer> crawlDelay = new HashMap<>();
            HashMap<String, ArrayList<String>> sitemap = new HashMap<>();

            try {
                boolean domainExists;
                Connection con = DriverManager.getConnection("jdbc:postgresql://localhost:5432/webcrawlerdb", "postgres", "postgres");
                Statement stmt = con.createStatement();
                URL url1 = new URL(protocol + pageToCrawl);
                String url1Host = url1.getHost(); // www.nekipage.com
                String url1HostNoWWW = url1Host.replace("www.", ""); // nekipage.com
                String SQL = "SELECT * FROM site WHERE domain='" + url1HostNoWWW + "'";
                ResultSet rs = stmt.executeQuery(SQL);
                // Preverimo, če je domena strani, ki jo crawlamo, že v bazi
                if(rs.next())
                    domainExists = true;
                else
                    domainExists = false;

                // Če še ni v bazi ...
                if (domainExists == false) {
                    URL robotsUrl = new URL(protocol + url1Host + "/robots.txt");
                    URLConnection robotsUrlCon = robotsUrl.openConnection();
                    BufferedReader in = new BufferedReader(new InputStreamReader(robotsUrlCon.getInputStream()));
                    StringBuffer robotsTxt = new StringBuffer();
                    String line;

                    Boolean ourUserAgent = false; // Gledamo le za User-Agent: *

                    ArrayList<String> allowL = new ArrayList<>();
                    ArrayList<String> disallowL = new ArrayList<>();
                    ArrayList<String> sitemapL = new ArrayList<>();
                    Integer crawlDel = 4; //

                    while ((line = in.readLine()) != null) {
                        robotsTxt.append(line);
                        if (line == "User-Agent: *") {
                            ourUserAgent = true;
                        }
                        if ((line.contains("User-Agent:")) && (line != "User-Agent: *")) {
                            ourUserAgent = false;
                        }
                        if (ourUserAgent) {
                            if (line.contains("Disallow:")) {
                                disallowL.add(line.replace("Disallow: ", ""));
                            }
                            if (line.contains("Allow:")) {
                                allowL.add(line.replace("Allow: ", ""));
                            }
                            if (line.contains("Crawl-delay:")) {
                                crawlDel = Integer.parseInt(line.replace("Crawl-delay: ", ""));
                            }
                        }
                        if (line.contains("Sitemap:")) {
                            sitemapL.add(line.replace("Sitemap: ", ""));
                        }
                    }
                    /*
                    String stran = pageToCrawl.replace(protocol, "");
                    stran = stran.replace("www.", "");

                    allow.put(stran, allowL);
                    disallow.put(stran, disallowL);
                    sitemap.put(stran, sitemapL);
                    crawlDelay.put(stran, crawlDel);
                    */ // --- ALI ---
                    allow.put(pageToCrawl, allowL);
                    disallow.put(pageToCrawl, disallowL);
                    sitemap.put(pageToCrawl, sitemapL);
                    crawlDelay.put(pageToCrawl, crawlDel);

                    StringBuilder sitemaps = new StringBuilder();

                    for (String s : sitemapL) { // ker lahko ima ena domana več sitemap-ov
                        sitemaps.append(s);
                        sitemaps.append("\n");

                        URL sitemapUrl = new URL(s);
                        URLConnection sitemapUrlCon = sitemapUrl.openConnection();
                        BufferedReader inSitemap = new BufferedReader(new InputStreamReader(sitemapUrlCon.getInputStream()));
                        String line2;

                        while ((line2 = inSitemap.readLine()) != null) {
                            while (line2.contains("<loc>")) {
                                int start = line2.indexOf("<loc>");
                                int end = line2.indexOf("</loc>");
                                String stran2 = line2.substring(start+5, end);
                                line2 = line2.replace(line2.substring(start, end+6), "");

                                stran2 = stran2.replace(protocol, "");
                                stran2 = stran2.replace("www.", "");
                                Main.scheduler.getFrontier().add(stran2); // Vse strani iz sitemap damo na frontier
                            }
                        }
                    }

                    // Vstavimo v bazo (tukaj id DA ali NE?)
                    PreparedStatement stat = con.prepareStatement("INSERT INTO site (id, domain, robots_content, " +
                            "sitemap_content) VALUES ('" + getSiteId(pageToCrawl) + "', '" + url1HostNoWWW + "', '"
                            + robotsTxt + "', '" + sitemaps + "')");
                    stat.executeUpdate();
                }
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }

            for (String disa : disallow.get(pageToCrawl)) {
                if (pageToCrawl.contains(disa)) {
                    for (String a : allow.get(pageToCrawl)) {
                        if (!(pageToCrawl.contains(a))) {
                            // TODO: Strani ne smemo crawlati
                        }
                    }
                }
            }

            Integer spi = crawlDelay.get(pageToCrawl);
            spi = spi * 1000;
            try {
                Thread.sleep(spi);
            } catch (Exception e) {
                System.out.println("ERROR: " + e.getMessage());
            }
            // -------------------

            // add url to visited pages
            Main.scheduler.getVisited().add(pageToCrawl.hashCode());

            // stop when 100 pages visited -- for now
            if (Main.scheduler.getVisited().size() > 100) {
                LOGGER.info("100 pages visited");
                return;
            }

            LOGGER.info("Current page: " + pageToCrawl);

            try {
                // access time of URL
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                String domain = getDomain(pageToCrawl);
                LOGGER.info("domain " + domain);


                // initialize htmlunit webclient
                final WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setThrowExceptionOnScriptError(false);

                // download page
                final com.gargoylesoftware.htmlunit.Page page = webClient.getPage(protocol + pageToCrawl);
                // wait 5s for JS to load
                webClient.waitForBackgroundJavaScriptStartingBefore(5000);

                int statusCode = page.getWebResponse().getStatusCode();
                String contentType = page.getWebResponse().getContentType();

                LOGGER.info("content type " + contentType);
                LOGGER.info("status code " + statusCode);

                int pageId = 0;
                if (docs.contains(contentType)) {
                    // page is a document
                    LOGGER.info("is doc");

                    // get site id and save site to db
                    int siteId = getSiteId(domain);
                    // save document
                    pageId = Main.db.savePage(new Page(siteId, "BINARY", pageToCrawl, null, statusCode, timestamp));
                    byte[] fileData = readImageFromUrl(pageToCrawl, pageToCrawl, true);
                    Main.db.savePageData(new PageData(pageId, getFileExtension(contentType), fileData));

                } else if (contentType.equals("text/html")) {
                    // page is html content

                    // get site id and save site to DB
                    int siteId = getSiteId(domain);


                    HtmlPage htmlPage = (HtmlPage) page;
                    Document document = Jsoup.parse(htmlPage.asXml());

                    // TODO: clean html -- upgrade whitelist
                    String cleanDoc = Jsoup.clean(document.html(), Whitelist.basic());
                    Document cleaned = Jsoup.parse(cleanDoc);

                    pageId = Main.db.savePage(new Page(siteId, "HTML", pageToCrawl, document.html(), statusCode, timestamp));

                    // fetch images on page
                    Elements imagesOnPage = document.select("img[src~=(?i)\\.(png|jpe?g|svg|gif)]");

                    // extract images
                    findImagesOnPage(imagesOnPage, pageId, false);

                    // parse HTML to extract <a /> attrs with links
                    Elements linksOnPage = document.select("a[href]");

                    // more links yay
                    for (Element p : linksOnPage) {
                        String pageUrl = p.attr("abs:href");

                        if (!pageUrl.equals("")) {
                            LOGGER.info("page url: " + pageUrl + " from page " + pageToCrawl);

                            pageUrl = pageUrl.split("//")[1];
                            LOGGER.info("page url: " + pageUrl + " from page " + pageToCrawl);

                            boolean isDuplicate = Main.scheduler.isDuplicate(pageUrl);

                            if (isDuplicate) {

                                LOGGER.info("Page is duplicate '" + pageUrl + "");
                                // ne dodamo page-a v frontier ampak shranimo v DB duplikat
                                Page duplicatePage = Main.db.getPageFromUrl(pageUrl);
                                pageId = Main.db.savePage(new Page(duplicatePage.getSiteId(), "DUPLICATE", pageUrl, null, duplicatePage.getHttpStatusCode(), new Timestamp(System.currentTimeMillis())));
                                Main.db.setLinkToFromPage(new Link(duplicatePage.getId(), pageId));

                            } else {
                                // add page to frontier
                                Main.scheduler.getFrontier().add(pageUrl);

                            }
                        }

                        LOGGER.info("links on page: '" + pageUrl + " " + linksOnPage.size());

                    }
                }

                /* TODO: setting links
                // set link from to page
                LOGGER.info("parent page url: " + parentPage);
                if (parentPage != null) {
                    LOGGER.info("page url: " + pageId + " from page " + Main.db.getPageFromUrl(parentPage).getId());
                    Main.db.setLinkToFromPage(new Link(Main.db.getPageFromUrl(parentPage).getId(), pageId));
                }*/

            } catch (IOException e) {
                LOGGER.info("Error for '" + pageToCrawl + "': ");
                e.printStackTrace();
            }

            parentPage = pageToCrawl;

            logFrontier(Main.scheduler.getFrontier());


        }
        LOGGER.info("empty queue " + Main.scheduler.getFrontier().size());

    }

    private int getSiteId(String domain) {
        int siteId = 0;
        siteId = Main.db.getSiteFromDomain(domain);
        if (siteId == -1) {
            siteId = Main.db.saveSite(new Site(domain, "", ""));
        }
        return siteId;
    }

    private void logFrontier(Queue<String> q) {
        System.out.println("FRONTIER: ");
        for (String s : q) {
            System.out.println(s);
        }
    }

    private String getDomain(String url) {
        return url.replace("www.", "");

    }

    private String getCanonicalURL(String url) {
        // TODO
        return "";
    }

    private String getFileExtension(String contentType) {
        String[] extensions = {"PDF", "PPT", "PPTX", "DOC", "DOCX"};
        int i = 0;
        for (String type : docs) {
            if (contentType.equals(type)) {
                LOGGER.info("document ext found " + extensions[i]);
                return extensions[i];
            }
            i += 1;
        }
        return null;
    }

    private void findImagesOnPage(Elements imagesOnPage, int pageId, boolean isDoc) {
        String imageUrl;
        String filename;
        byte[] imageData;

        for (Element image : imagesOnPage) {
            imageUrl = image.absUrl("src");

            String[] splitAbsUrl = imageUrl.split("/");
            filename = splitAbsUrl[splitAbsUrl.length - 1];

            String contentType = getImageMimeType(filename);
            imageData = readImageFromUrl(imageUrl, filename, isDoc);
            Timestamp accessTime = new Timestamp(System.currentTimeMillis());

            // time to save image to DB
            Main.db.saveImage(new Image(pageId, filename, contentType, accessTime, imageData));

            LOGGER.info("image data type: " + filename + " " + contentType + " " + accessTime);

        }
    }

    private String getImageMimeType(String imageUrl) {
        String mimeType = "";
        try {
            mimeType = Files.probeContentType(new File(imageUrl).toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return mimeType;
    }

    private byte[] readImageFromUrl(String imageUrl, String filename, boolean isDoc) {

        byte[] response = null;
        try {
            URL url;
            if (isDoc) {
                // if url is a document add protocol to link
                url = new URL(protocol + imageUrl);
            } else {
                url = new URL(imageUrl);
            }

            InputStream in = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
            out.close();
            in.close();
            response = out.toByteArray();


            /*FileOutputStream fos = new FileOutputStream(filename);
            fos.write(response);
            fos.close();*/

        } catch (IOException ex) {
            LOGGER.info("shit went down while trying to get image data");
            ex.printStackTrace();
        }
        return response;
    }

    public void run() {
        try {
            // Displaying the thread that is running
            System.out.println("Thread " +
                    Thread.currentThread().getId() +
                    " is running");
            getPageLinks();

        } catch (Exception e) {
            // Throwing an exception
            System.out.println("Exception is caught");
            e.printStackTrace();
        }
    }

}
