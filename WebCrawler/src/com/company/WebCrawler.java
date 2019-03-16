package com.company;

import com.company.DB.DBManager;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public class WebCrawler {

    private HashSet<String> links;
    private final static Logger LOGGER = Logger.getLogger(DBManager.class.getName());

    public WebCrawler() {
        links = new HashSet<>();
    }

    public void getPageLinks(String URL) {

         if (!links.contains(URL)) {
            try {

                if (links.add(URL)) {
                    LOGGER.info("Current page: "+URL);
                }

                // access time of URL
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                // response code of url
                Connection.Response response = Jsoup.connect(URL).followRedirects(false).execute();
                int statusCode = response.statusCode();
                LOGGER.info("response code "+statusCode);

                // fetch HTML code
                Document document = Jsoup.connect(URL).get();

                // TODO: get proper domain name for site
                // URL.split("/")[2].substring(4)
                int siteId = Main.db.saveSite(URL.split("/")[2].substring(4),"","");

                // save && page to db
                // TODO: check site && page for duplicates
                int pageId = Main.db.savePage(siteId, "HTML", URL, document.html(), statusCode, timestamp);
                LOGGER.info("html "+document.html());

                // fetch images on page
                Elements imagesOnPage = document.select("img[src~=(?i)\\.(png|jpe?g|svg|gif)]");
                // extract images
                findImagesOnPage(imagesOnPage, pageId);

                // parse HTML to extract <a /> attrs with links
                Elements linksOnPage = document.select("a[href]");

                // more links yay
                for (Element page : linksOnPage) {
                    // TODO: check links for DOC endings
                    // and save documents instead
                    getPageLinks(page.attr("abs:href"));
                }


            } catch (IOException e) {
                LOGGER.info("Error for '" + URL + "': ");
                e.printStackTrace();
            }
        }
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
            Main.db.saveImage(filename, contentType, imageData, accessTime, pageId);

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

}
