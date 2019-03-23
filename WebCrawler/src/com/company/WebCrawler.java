package com.company;

import com.company.DB.*;
import com.gargoylesoftware.htmlunit.BrowserVersion;
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

    private HashSet<String> links;
    private final static Logger LOGGER = Logger.getLogger(DBManager.class.getName());
    private String URL;
    private String parentPage;

    public WebCrawler(String URL) {
        links = new HashSet<>();
        this.URL = URL;
    }

    public void getPageLinks(String URL) {

        Scheduler.schedule(URL);
        parentPage = null;

        Queue <String> frontier = Main.scheduler.getFrontier();
        Set<Integer> visited = Main.scheduler.getVisited();

        while (!frontier.isEmpty()) {

            // get URL from frontier
            String pageToCrawl = frontier.poll();

            // stop when visited
            if(visited.size() > 100) return;

                LOGGER.info("Current page: " + pageToCrawl);


                try {
                    // access time of URL
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                    /*// save site && page to db
                    // TODO: check site && page for duplicates
                    // duplicate page: dont set the html_content value && link to the duplicate version of the page
                    */

                    String domain = getDomain(pageToCrawl);
                    LOGGER.info("domain " + domain);

                    int pageId = 0;
                    if (Scheduler.checkForDuplicates(pageToCrawl.hashCode())) {
                        int siteId = Main.db.getSiteFromDomain(domain);
                        // get siteId
                        // get pageId with URL
                        // pageId = Main.db.savePage(new Page(siteId, "DUPLICATE", pageToCrawl, null, statusCode, timestamp));
                    } else {

                        final WebClient webClient = new WebClient(BrowserVersion.CHROME);
                        webClient.getOptions().setJavaScriptEnabled(true);
                        webClient.getOptions().setThrowExceptionOnScriptError(false);

                        final HtmlPage page = webClient.getPage("http://"+pageToCrawl);
                        // wait 5s for JS to load
                        webClient.waitForBackgroundJavaScriptStartingBefore(5000);

                        int statusCode = page.getWebResponse().getStatusCode();
                        LOGGER.info("status code " + statusCode);

                        // save SITE to db
                        // check domain for duplicate and get siteId
                        //int siteId = Main.db.saveSite(new Site(domain, "", ""));

                        Document document = Jsoup.parse(page.asXml());

                        // clean html -- what else to add
                        String cleanDoc = Jsoup.clean(document.html(), Whitelist.basic());
                        // Document cleaned = Jsoup.parse(cleanDoc);

                        // TODO: test document saving
                        String fileExtension = getFileExtension(URL);
                        if (isDocument(fileExtension)) {
                            LOGGER.info("is doc");
                            //pageId = Main.db.savePage(new Page(siteId, "BINARY", pageToCrawl, null, statusCode, timestamp));
                            byte[] fileData = readImageFromUrl(URL, "");
                            //Main.db.savePageData(new PageData(pageId, fileExtension.toUpperCase(), fileData));
                        } else {
                            //pageId = Main.db.savePage(new Page(siteId, "HTML", pageToCrawl, document.html(), statusCode, timestamp));
                        }

                        // fetch images on page
                        Elements imagesOnPage = document.select("img[src~=(?i)\\.(png|jpe?g|svg|gif)]");

                        // extract images
                        // findImagesOnPage(imagesOnPage, pageId);

                        // parse HTML to extract <a /> attrs with links
                        Elements linksOnPage = document.select("a[href]");

                        // more links yay
                        for (Element p : linksOnPage) {
                            String pageUrl = p.attr("abs:href");
                            Scheduler.schedule(pageUrl);
                        }
                    }

                    // set from to page
                    // Main.db.setLinkToFromPage(new Link(Main.db.getPageFromUrl(parentPage), pageId));


                } catch (IOException e) {
                    LOGGER.info("Error for '" + pageToCrawl + "': ");
                    e.printStackTrace();
                }

                parentPage = pageToCrawl;
            }

    }

    private String getDomain(String url) {
        return url.replace("www.","");

    }

    private String getFileExtension(String url) {
        int i = url.lastIndexOf('.');
        if (i > 0) {
            LOGGER.info("file ext: " + url.substring(i+1));
            return url.substring(i+1);
        }
        return "";
    }

    private boolean isDocument(String extension) {
        List<String> validExtensions = new ArrayList<>();
        validExtensions.add("doc");
        validExtensions.add("pdf");
        validExtensions.add("ppt");
        validExtensions.add("pptx");
        validExtensions.add("docx");

        if (validExtensions.contains(extension)) {
            return true;
        }
        return false;
    }

    private void findImagesOnPage(Elements imagesOnPage, int pageId) {
        String imageUrl;
        String filename;
        byte[] imageData;

        for (Element image : imagesOnPage) {
            imageUrl = image.absUrl("src");

            String[] splitAbsUrl = imageUrl.split("/");
            filename = splitAbsUrl[splitAbsUrl.length-1];

            String contentType = getImageMimeType(filename);
            imageData = readImageFromUrl(imageUrl, filename);
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

    private byte[] readImageFromUrl(String imageUrl, String filename) {

        byte[] response = null;
        try {
            URL url = new URL(imageUrl);
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

            /*
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(response);
            fos.close();
            */
        } catch (IOException ex) {
            LOGGER.info("shit went down while trying to get image data");
            ex.printStackTrace();
        }
        return response;
    }

    public void run() {
        try
        {
            // Displaying the thread that is running
            System.out.println ("Thread " +
                    Thread.currentThread().getId() +
                    " is running");
            getPageLinks(this.URL);

        }
        catch (Exception e)
        {
            // Throwing an exception
            System.out.println ("Exception is caught");
        }
    }

}
