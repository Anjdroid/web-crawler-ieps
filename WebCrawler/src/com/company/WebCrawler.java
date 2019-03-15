package com.company;

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
        links = new HashSet<String>();
    }

    public void getPageLinks(String URL) {

         if (!links.contains(URL)) {
            try {

                if (links.add(URL)) {
                    System.out.println(URL);
                }

                // fetch HTML code
                Document document = Jsoup.connect(URL).get();

                // parse HTML to extract <a /> attrs with links
                Elements linksOnPage = document.select("a[href]");

                // more links yay
                for (Element page : linksOnPage) {
                    getPageLinks(page.attr("abs:href"));
                }
            } catch (IOException e) {
                LOGGER.info("For '" + URL + "': ");
                e.printStackTrace();
            }
        }
    }

}
