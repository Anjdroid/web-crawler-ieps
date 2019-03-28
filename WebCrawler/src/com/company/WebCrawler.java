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
import java.net.URI;
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

            // add url to visited pages
            Main.scheduler.getVisited().add(pageToCrawl.hashCode());

            // stop when 100 pages visited -- for now
            if (Main.scheduler.getVisited().size() > 100) {
                LOGGER.info("100 pages visited");
                return;
            }

            LOGGER.info("Current page: " + pageToCrawl);

            int pageId = -1;

            // get cannonical URL of page
            pageToCrawl = getCanonicalURL(protocol+pageToCrawl);
            LOGGER.info("canonical url: "+pageToCrawl);

            // first check if page is a duplicate in frontier / visited
            if (Scheduler.isDuplicate(pageToCrawl)) {

                LOGGER.info("Page is duplicate '" + pageToCrawl + "");
                // ne dodamo page-a v frontier ampak shranimo v DB duplikat
                saveDuplicate(pageToCrawl);

            } else {

                // get domain
                String domain = getDomain(pageToCrawl);
                LOGGER.info("domain " + domain);

                boolean robotsExsist = false;

                // get side id from db if it exsists
                int siteId = Main.db.getSiteFromDomain(domain);
                StringBuilder sitemaps = new StringBuilder();
                StringBuffer robotsTxt = new StringBuffer();

                // domena ne obstaja v bazi
                if (siteId == -1) {
                    // dobimo robots content in sitemap

                    HashMap<String, ArrayList<String>> sitemap = new HashMap<>();

                    try {
                        URL robotsUrl = new URL(protocol + domain + "/robots.txt");
                        URLConnection robotsUrlCon = robotsUrl.openConnection();
                        BufferedReader in = new BufferedReader(new InputStreamReader(robotsUrlCon.getInputStream()));
                        String line;

                        boolean ourUserAgent = false; // Gledamo le za User-Agent: *

                        ArrayList<String> allowL = new ArrayList<>();
                        ArrayList<String> disallowL = new ArrayList<>();
                        ArrayList<String> sitemapL = new ArrayList<>();
                        int crawlDel = 4; //

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
                                if ((line.contains("Allow:")) && (line.replace("Allow: ", "") != "/")) {
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

                        //String stran = pageToCrawl.replace(protocol, "");
                        //stran = stran.replace("www.", "");

                        /*Main.scheduler.getAllowed().put(stran, allowL);
                        disallow.put(stran, disallowL);
                        sitemap.put(stran, sitemapL);
                        crawlDelay.put(stran, crawlDel);*/
                        // --- ALI ---
                        sitemap.put(domain, sitemapL);
                        // to se doda v globalne hashmape, da lahko do njih dostopajo tudi drugi threadi
                        // za vsako domeno shranimo allowed, disallowed in crawldelay
                        Main.scheduler.getAllowed().put(domain, allowL);
                        Main.scheduler.getDissallowed().put(domain, disallowL);
                        Main.scheduler.getCrawlDelay().put(domain, crawlDel);

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
                                    String stran2 = line2.substring(start + 5, end);
                                    line2 = line2.replace(line2.substring(start, end + 6), "");

                                    stran2 = stran2.replace(protocol, "");
                                    stran2 = stran2.replace("www.", "");
                                    Main.scheduler.getFrontier().add(stran2); // Vse strani iz sitemap damo na frontier
                                }
                            }
                        }
                        robotsExsist = true;
                    } catch (Exception e) {
                        LOGGER.info("ERROR getting sitemap/robots for domain "+domain);
                        e.printStackTrace();
                        // robots file doesnt exsist
                        robotsExsist = false;

                        /*
                        // Vstavimo v bazo (tukaj siteId DA ali NE?)
                        PreparedStatement stat = con.prepareStatement("INSERT INTO site (id, domain, robots_content, " +
                                "sitemap_content) VALUES ('" + getSiteId(pageToCrawl) + "', '" + url1HostNoWWW + "', '"
                                + robotsTxt + "', '" + sitemaps + "')");
                        stat.executeUpdate();
                    } catch (Exception e) {
                        System.out.println("ERROR: " + e.getMessage());
                    }*/
                    }
                }

                // pogledamo ce lahko page crawlamo

                boolean allowCrawl = true;

                if (robotsExsist) {

                    HashMap<String, ArrayList<String>> checkDisallowed = Main.scheduler.getDissallowed();
                    if (checkDisallowed.containsKey(domain)) {
                        // dobimo seznam disallowed pages
                        ArrayList<String> disallowedPages = checkDisallowed.get(domain);
                        if (disallowedPages.contains(pageToCrawl)) {
                            // ce ta seznam vsebuje nas page
                            allowCrawl = false;
                        }
                    }
                }

                if (allowCrawl) {
                    // dobimo crawl delay za domeno
                    if (robotsExsist && !Main.scheduler.getCrawlDelay().isEmpty()) {
                        int spi = Main.scheduler.getCrawlDelay().get(domain);
                        spi = spi * 1000;

                        try {
                            Thread.sleep(spi);
                        } catch (Exception e) {
                            System.out.println("ERROR: " + e.getMessage());
                        }
                    }

                    try {
                        // access time of URL
                        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

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

                        if (docs.contains(contentType)) {
                            // page is a document
                            LOGGER.info("is doc");

                            // get site id and save site to db
                            siteId = getSiteId(domain, sitemaps.toString(), robotsTxt.toString());
                            // save document
                            pageId = Main.db.savePage(new Page(siteId, "BINARY", pageToCrawl, null, statusCode, timestamp, 0));
                            byte[] fileData = readImageFromUrl(pageToCrawl, pageToCrawl, true);
                            Main.db.savePageData(new PageData(pageId, getFileExtension(contentType), fileData));

                        } else if (contentType.equals("text/html")) {
                            // page is html content

                            // get site id and save site to DB
                            siteId = getSiteId(domain, sitemaps.toString(), robotsTxt.toString());

                            HtmlPage htmlPage = (HtmlPage) page;
                            Document document = Jsoup.parse(htmlPage.asXml());

                            // get cleaned HTML
                            String cleanDoc = Jsoup.clean(document.html(), Whitelist.relaxed());
                            Document cleaned = Jsoup.parse(cleanDoc);

                            int pageContentHash = cleaned.html().hashCode();
                            // check if hashcode is in database
                            if (isContentDuplicate(pageContentHash)) {
                                saveDuplicate(pageToCrawl);
                            } else {

                                pageId = Main.db.savePage(new Page(siteId, "HTML", pageToCrawl, cleaned.html(), statusCode, timestamp, pageContentHash));

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
                                        if (pageUrl.contains("//")) {
                                            pageUrl = pageUrl.split("//")[1];
                                        }
                                        //LOGGER.info("page url: " + pageUrl + " from page " + pageToCrawl);

                                        //boolean isDuplicate = Main.scheduler.isDuplicate(pageUrl);

                                        // else {
                                        // add page to frontier
                                        Main.scheduler.getFrontier().add(pageUrl);
                                        Main.scheduler.getParentChild().put(pageUrl, pageToCrawl);

                                        //}
                                        LOGGER.info("links on page: '" + pageUrl + " " + linksOnPage.size());
                                    }


                                }
                            }
                        }

                            // set link from to page
                            String parentPage = Main.scheduler.getParentChild().get(pageToCrawl);
                        if (parentPage != null) {
                            LOGGER.info("setting link page url: " + parentPage + " " + Main.db.getPageFromUrl(parentPage).getId() +
                                    " from page " + pageId);
                            Main.db.setLinkToFromPage(new Link(Main.db.getPageFromUrl(parentPage).getId(), pageId));
                        }


                    } catch (IOException e) {
                        LOGGER.info("Error for '" + pageToCrawl + "': ");
                        e.printStackTrace();
                    }

                    //logFrontier(Main.scheduler.getFrontier());
                }
            }
        }
            LOGGER.info("empty queue " + Main.scheduler.getFrontier().size());

    }

    private int getSiteId(String domain, String sitemaps, String robots) {
        int siteId = 0;
        siteId = Main.db.getSiteFromDomain(domain);
        if (siteId == -1) {
            siteId = Main.db.saveSite(new Site(domain, robots, sitemaps));
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

    private boolean isContentDuplicate(int hashcode) {
        int id = Main.db.getPageFromHash(hashcode);
        return id > 0;
    }

    private String getCanonicalURL(String myUrl) {
        String canonical = myUrl;
        try {
            URL url = new URL(myUrl);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());
            canonical = uri.toString();
        } catch (Exception e) {
            LOGGER.info("cant get canonical url");
            e.printStackTrace();
        }
        return canonical.split("//")[1];
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

    private void saveDuplicate(String pageToCrawl) {
        Page duplicatePage = Main.db.getPageFromUrl(pageToCrawl);
        int pageId = Main.db.savePage(new Page(duplicatePage.getSiteId(), "DUPLICATE", pageToCrawl, null,
                duplicatePage.getHttpStatusCode(), new Timestamp(System.currentTimeMillis()), duplicatePage.getHashcode()));
        Main.db.setLinkToFromPage(new Link(duplicatePage.getId(), pageId));
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
