package com.company;

import com.company.DB.*;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Logger;


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


            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(response);
            fos.close();

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
