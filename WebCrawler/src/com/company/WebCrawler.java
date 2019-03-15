package com.company;

import com.company.DB.DBManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.HashSet;
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

                // fetch HTML code
                Document document = Jsoup.connect(URL).get();

                // parse HTML to extract <a /> attrs with links
                Elements linksOnPage = document.select("a[href]");
                Elements imagesOnPage = document.select("img[src~=(?i)\\.(png|jpe?g|svg|gif)]");

                findImagesOnPage(imagesOnPage, URL);

                // more links yay
                for (Element page : linksOnPage) {
                    getPageLinks(page.attr("abs:href"));
                }


            } catch (IOException e) {
                LOGGER.info("Error for '" + URL + "': ");
                e.printStackTrace();
            }
        }
    }

    private void findImagesOnPage(Elements imagesOnPage, String pageUrl) {
        String imageUrl = "";
        String realImageUrl = "";
        String imgExt = "";
        for (Element image : imagesOnPage) {
            imageUrl = image.absUrl("src");
            String[] splitAbsUrl = imageUrl.split("/");

            realImageUrl = splitAbsUrl[splitAbsUrl.length-1];

            // time to save image to DB
            // also save byte array of image
            LOGGER.info("image: " + realImageUrl);
        }
    }

}
