package com.company;

import com.company.DB.*;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

public class WebCrawler implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(DBManager.class.getName());
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

        // while frontier queue is not empty run web crawler
        while (!(Main.scheduler.getFrontier()).isEmpty()) {

            // get URL from frontier
            String pageToCrawl = Main.scheduler.getFrontier().poll();

            // add url to visited pages
            Main.scheduler.getVisited().add(pageToCrawl);

            if (Main.db.checkSize() > 100000) {
                LOGGER.info("100.000 pages visited");
                return;
            }

            LOGGER.info("Current page: " + pageToCrawl+ " running on thread: "+ Thread.currentThread().getId() );

            int pageId = -1;

            // get cannonical URL of page
            pageToCrawl = getCanonicalURL(protocol+pageToCrawl);

            // first check if page is a duplicate in visited pages set
            if (Scheduler.isDuplicate(pageToCrawl)) {
                // save page as DUPLICATE to database
                Page duplicate = Main.db.getPageFromUrl(pageToCrawl);
                saveDuplicate(pageToCrawl, duplicate);
            } else {

                // get page domain
                String domain = getDomainName(protocol+pageToCrawl);

                boolean robotsExist = false;

                // get side id from DB if site exists
                int siteId = Main.db.getSiteFromDomain(domain);

                StringBuffer sitemaps = new StringBuffer();
                StringBuffer robotsTxt = new StringBuffer();

                if (siteId == -1) {
                    // domain is not in DB
                    // we collect robots.txt and sitemap content

                    try {
                        URL robotsUrl = new URL(protocol + domain + "/robots.txt");
                        HttpURLConnection robotsUrlCon = (HttpURLConnection) robotsUrl.openConnection();
                        robotsUrlCon.setReadTimeout(5000);

                        boolean redirect = false;

                        int status = robotsUrlCon.getResponseCode();
                        if (status != HttpURLConnection.HTTP_OK) {
                            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                                    || status == HttpURLConnection.HTTP_MOVED_PERM
                                    || status == HttpURLConnection.HTTP_SEE_OTHER)
                                redirect = true;
                        }

                        if (redirect) {
                            // get redirect url from "location" header field
                            String newUrl = robotsUrlCon.getHeaderField("Location");

                            // open the new connnection again
                            robotsUrlCon = (HttpURLConnection) new URL(newUrl).openConnection();
                        }

                        BufferedReader in = new BufferedReader(new InputStreamReader(robotsUrlCon.getInputStream()));
                        String line;

                        // checking only for User-Agent: *
                        boolean ourUserAgent = false;

                        ArrayList<String> allowL = new ArrayList<>();
                        ArrayList<String> disallowL = new ArrayList<>();
                        ArrayList<String> sitemapL = new ArrayList<>();
                        int crawlDel = 4;

                        // reading robots.txt file
                        while ((line = in.readLine()) != null) {
                            robotsTxt.append(line);
                            if ((line.contains("User-Agent: *")) || (line.contains("User-agent: *"))) {
                                ourUserAgent = true;
                            }
                            if (((line.contains("User-Agent:")) && (!line.contains("User-Agent: *"))) || ((line.contains("User-agent:")) && (!line.contains("User-agent: *")))) {
                                ourUserAgent = false;
                            }
                            if (ourUserAgent) {
                                if ((line.contains("Disallow:")) && ((line.replace("Disallow: ", "")).length() > 0)) {
                                    disallowL.add(line.replace("Disallow: ", ""));
                                }
                                if ((line.contains("Allow:")) && ((line.replace("Allow: ", "")).length() > 1)) {
                                    allowL.add(line.replace("Allow: ", ""));
                                }
                                if ((line.contains("Crawl-delay:")) && ((line.replace("Crawl-delay: ", "")).length() > 0)) {
                                    crawlDel = Integer.parseInt(line.replace("Crawl-delay: ", ""));
                                }
                            }
                            if ((line.contains("Sitemap:")) && ((line.replace("Sitemap: ", "")).length() > 0)) {
                                sitemapL.add(line.replace("Sitemap: ", ""));
                            }
                            robotsTxt.append("\n");
                        }

                        // setting allowed and disallowed pages for domain
                        Main.scheduler.getAllowed().put(domain, allowL);
                        Main.scheduler.getDissallowed().put(domain, disallowL);
                        // setting crawl delay for domain
                        Main.scheduler.getCrawlDelay().put(domain, crawlDel);

                        // checking for multiple sitemaps
                        for (String s : sitemapL) {

                            URL sitemapUrl = new URL(s);
                            URLConnection sitemapUrlCon = sitemapUrl.openConnection();
                            BufferedReader inSitemap = new BufferedReader(new InputStreamReader(sitemapUrlCon.getInputStream()));
                            String line2;

                            while ((line2 = inSitemap.readLine()) != null) {
                                sitemaps.append(line2);

                                while (line2.contains("<loc>")) {
                                    int start = line2.indexOf("<loc>");
                                    int end = line2.indexOf("</loc>");
                                    String stran2 = line2.substring(start + 5, end);
                                    line2 = line2.replace(line2.substring(start, end + 6), "");

                                    // fix sitemap url
                                    stran2 = stran2.contains(protocol) ? stran2.replace(protocol, "") : stran2;
                                    stran2 = stran2.contains("https://") ? stran2.replace("https://", "") : stran2;
                                    stran2 = stran2.contains("www.") ? stran2.replace("www.", "") : stran2;

                                    // adding all pages from sitemap to frontier
                                    Main.scheduler.getFrontier().add(stran2);
                                }
                                sitemaps.append("\n");
                            }
                        }

                        // we were able to get robots.txt content
                        robotsExist = true;
                    } catch (Exception e) {
                        LOGGER.info("ERROR getting robots content for domain: "+domain);
                        e.printStackTrace();
                        // robots file doesn't exist
                        robotsExist = false;
                    }
                }

                if (!Main.scheduler.getCrawlDelay().containsKey(domain)) {
                    int crawlDel = 4;
                    Main.scheduler.getCrawlDelay().put(domain, crawlDel);
                }

                boolean allowCrawl = true;

                // check if we can crawl the page
                if (robotsExist) {

                    HashMap<String, ArrayList<String>> checkDisallowed = Main.scheduler.getDissallowed();
                    if (checkDisallowed.containsKey(domain)) {
                        // disallow pages list
                        ArrayList<String> disallowedPages = checkDisallowed.get(domain);
                        outerloop:
                        for (String disa : disallowedPages) {
                            String disaH = disa;
                            if ((disaH.substring(disaH.length() - 1)).contains("*")) {
                                disaH = disaH.substring(0, disaH.length()-1);
                            }
                            if ((disaH.substring(0,1)).contains("*")) {
                                disaH = disaH.substring(1);
                            }
                            else if ((disaH.length() > 1) && ((disaH.substring(0,2)).contains("/*"))) {
                                disaH = disaH.substring(2);
                            }
                            if (pageToCrawl.contains(disaH)) {
                                allowCrawl = false;
                                HashMap<String, ArrayList<String>> checkAllowed = Main.scheduler.getAllowed();
                                if (checkAllowed.containsKey(domain)) {
                                    // allow page list
                                    ArrayList<String> allowedPages = checkAllowed.get(domain);
                                    for (String a : allowedPages) {
                                        String aH = a;
                                        if ((aH.substring(aH.length() - 1)).contains("*")) {
                                            aH = aH.substring(0, aH.length()-1);
                                        }
                                        if ((aH.substring(0,1)).contains("*")) {
                                            aH = aH.substring(1);
                                        }
                                        else if ((aH.substring(0,2)).contains("/*")) {
                                            aH = aH.substring(2);
                                        }
                                        if (pageToCrawl.contains(aH)) {
                                            allowCrawl = true;
                                            break outerloop;
                                        }
                                    }
                                } else {
                                    allowCrawl = false;
                                }
                            }
                        }
                    }
                }

                if (allowCrawl) {
                    if (Main.scheduler.getCrawlDelay().containsKey(domain)) {
                        // getting crawl delay for domain
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

                        // initialize htmlUnit webclient
                        final WebClient webClient = new WebClient(BrowserVersion.BEST_SUPPORTED);
                        webClient.getOptions().setJavaScriptEnabled(true);
                        webClient.getOptions().setThrowExceptionOnScriptError(false);

                        // download page
                        final com.gargoylesoftware.htmlunit.Page page = webClient.getPage(protocol + pageToCrawl);
                        int statusCode = page.getWebResponse().getStatusCode();

                        if (statusCode != 404 && statusCode != 500) {

                            // wait 5s for JS to load
                            webClient.waitForBackgroundJavaScriptStartingBefore(5000);

                            String contentType = page.getWebResponse().getContentType();

                            if (docs.contains(contentType)) {
                                // page is a document
                                LOGGER.info("is document");

                                // get site id and save site to db
                                siteId = getSiteId(domain, sitemaps.toString(), robotsTxt.toString());
                                // save page as BINARY to DB
                                pageId = Main.db.savePage(new Page(siteId, "BINARY", pageToCrawl, null, statusCode, timestamp, 0));
                                // read file data
                                byte[] fileData = readImageFromUrl(pageToCrawl, pageToCrawl, true);
                                // save document to DB
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
                                // duplicate detection: check if hashcode is in database
                                Page pageDuplicate = isContentDuplicate(pageContentHash);
                                if (pageDuplicate.getId() > 0) {
                                    saveDuplicate(pageToCrawl, pageDuplicate);
                                } else {
                                    // page is not a duplicate

                                    pageId = Main.db.savePage(new Page(siteId, "HTML", pageToCrawl, cleaned.html(), statusCode, timestamp, pageContentHash));

                                    // fetch images on page
                                    Elements imagesOnPage = document.select("img[src~=(?i)\\.(png|jpe?g|svg|gif)]");

                                    // extract images
                                    findImagesOnPage(imagesOnPage, pageId, false);

                                    // parse HTML to extract <a /> attrs with links
                                    Elements linksOnPage = document.select("a[href]");

                                    // getting all the links from the page
                                    for (Element p : linksOnPage) {
                                        String pageUrl = p.attr("abs:href");

                                        if (!pageUrl.equals("")) {

                                            LOGGER.info("page url: " + pageUrl + " from page " + pageToCrawl);
                                            if (pageUrl.contains("//")) {
                                                pageUrl = pageUrl.split("//")[1];
                                            }

                                            // add page to frontier
                                            Main.scheduler.getFrontier().add(pageUrl);
                                            Main.scheduler.getParentChild().put(pageUrl, pageToCrawl);

                                        }
                                    }
                                }
                            }
                            // set link from to page
                            String parentPage = Main.scheduler.getParentChild().get(pageToCrawl);
                            if (parentPage != null) {
                                LOGGER.info("setting link page url: " + parentPage + " " + Main.db.getPageFromUrl(parentPage).getId() +
                                        " to page: " + pageId + " " + pageToCrawl);
                                Main.db.setLinkToFromPage(new Link(Main.db.getPageFromUrl(parentPage).getId(),
                                        Main.db.getPageFromUrl(pageToCrawl).getId()));
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.info("Error for: " + pageToCrawl);
                        e.printStackTrace();
                    }
                }
            }
        }
        LOGGER.info("Queue is empty, finish crawling." + Main.scheduler.getFrontier().size());
    }

    private int getSiteId(String domain, String sitemaps, String robots) {
        int siteId = 0;
        // get side id from DB
        siteId = Main.db.getSiteFromDomain(domain);
        if (siteId == -1) {
            // add site if its not already in DB
            siteId = Main.db.saveSite(new Site(domain, robots, sitemaps));
        }
        return siteId;
    }

    public static String getDomainName(String url) {
        // get domain from URL
        String domain = url;
        try {
            URI uri = new URI(url);
            domain = uri.getHost();
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    private Page isContentDuplicate(int hashcode) {
        // check page content for duplicate
        Page page = Main.db.getPageFromHash(hashcode);
        return page;
    }

    private String getCanonicalURL(String myUrl) {
        // get canonical URL of page
        String canonical = myUrl;
        try {
            URL url = new URL(myUrl);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                    url.getPath(), url.getQuery(), url.getRef());
            canonical = uri.toString();
        } catch (Exception e) {
            LOGGER.info("can't get canonical url");
            e.printStackTrace();
        }
        return canonical.split("//")[1];
    }

    private String getFileExtension(String contentType) {
        // get file extension of extracted document
        String[] extensions = {"PDF", "PPT", "PPTX", "DOC", "DOCX"};
        int i = 0;
        for (String type : docs) {
            if (contentType.equals(type)) {
                return extensions[i];
            }
            i += 1;
        }
        return null;
    }

    private void findImagesOnPage(Elements imagesOnPage, int pageId, boolean isDoc) {
        // extract images from page
        String imageUrl;
        String filename;
        byte[] imageData;

        for (Element image : imagesOnPage) {
            imageUrl = image.absUrl("src");

            String[] splitAbsUrl = imageUrl.split("/");
            filename = splitAbsUrl[splitAbsUrl.length - 1];

            // get image mime type
            String contentType = getImageMimeType(filename);
            // get image data
            imageData = readImageFromUrl(imageUrl, filename, isDoc);
            Timestamp accessTime = new Timestamp(System.currentTimeMillis());

            // save image to DB
            Main.db.saveImage(new Image(pageId, filename, contentType, accessTime, imageData));
        }
    }

    private String getImageMimeType(String imageUrl) {
        // get image mime type
        String mimeType = "";
        try {
            mimeType = Files.probeContentType(new File(imageUrl).toPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return mimeType;
    }

    private void saveDuplicate(String pageToCrawl, Page duplicatePage) {
        // save page to DB as DUPLICATE
        int pageId = Main.db.savePage(new Page(duplicatePage.getSiteId(), "DUPLICATE", pageToCrawl+"-duplicate", null,
                duplicatePage.getHttpStatusCode(), new Timestamp(System.currentTimeMillis()), duplicatePage.getHashcode()));
        Main.db.setLinkToFromPage(new Link(duplicatePage.getId(), pageId));
    }

    private byte[] readImageFromUrl(String imageUrl, String filename, boolean isDoc) {
        // read image data
        byte[] response = null;
        try {
            URL url;
            if (isDoc) {
                // if url is a document add protocol to link for download
                url = new URL(protocol + imageUrl);
            } else {
                try {
                    url = new URL(imageUrl);
                } catch (MalformedURLException e) {
                    url = new URL(protocol + imageUrl);
                }
            }

            InputStream input = new BufferedInputStream(url.openStream());
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
            output.close();
            input.close();
            response = output.toByteArray();
        } catch (IOException ex) {
            LOGGER.info("ERROR while trying to get data");
            ex.printStackTrace();
        }
        return response;
    }

    public void run() {
        try {
            // Displaying the thread that is running
            LOGGER.info("Thread " +
                    Thread.currentThread().getId() +
                    " is running");
            getPageLinks();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
