package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("Media Markt")
public class MediaMarktScraper extends Scraper {

    private final String PAGE_SIZE = "50";

    public MediaMarktScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected String getFirstPageUrlWithParams(String url) {
        return url + "?limit=50&page=1";
    }

    @Override
    protected void options() {
//        options.addArguments("--blink-settings=imagesEnabled=false");
    }

    @Override
    protected int getNumberOfPages(WebDriver driver) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        String info = parsed.getElementsByClass("more-offers").get(0)
                .getElementsByClass("info").get(0).text().trim();
        if (info.startsWith("Wyświetlono " + PAGE_SIZE + " z " + PAGE_SIZE)) {
            //only one page
            return 1;
        } else if (info.startsWith("Wyświetlono " + PAGE_SIZE + " z ")) {
            //more than one page
            int allProducts = Integer.parseInt(info.split("Wyświetlono " + PAGE_SIZE + " z ")[1].split(" ")[0]);
            int productsPerPage = Integer.parseInt(PAGE_SIZE);
            Double ratio = allProducts * 1.0 / productsPerPage;
            if (allProducts % productsPerPage == 0) {
                return ratio.intValue();
            } else {
                return ratio.intValue() + 1;
            }
        } else {
            //only one page
            return 1;
        }
    }

    @Override
    protected List<Element> loadAllOffersTiles(Document doc) {
        return doc.getElementsByClass("offer");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage) {
        return url + (url.contains("?") ? "&" : "?") + "limit=" + PAGE_SIZE + "&page=" + currentPage;
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer) {
        Element attributes = offerElement.select(".list.attributes").first();
        if (attributes != null) {
            List<Element> items = attributes.select(".item");
            for (Element item : items) {
                String attributeName = sanitize(item.select(".attribute-name").text()
                        .trim());
                if (attributeName.endsWith(":")) {
                    attributeName = attributeName.substring(0, attributeName.length() - 1);
                }
                String attributeValues = item.select(".attribute-values").text().trim();
                if (!attributeName.isBlank()) {
                    offer.put(attributeName, Arrays.stream(attributeValues.split(","))
                            .map(String::trim)
                            .map(this::sanitize)
                            .filter(Strings::isNotEmpty)
                            .collect(Collectors.toList()));
                }
            }
        }
    }

    @Override
    protected String getOfferPrice(Element offer) {
        return offer.select(".main-price .whole").text();
    }

    @Override
    protected String getOfferHref(Element offer) {
        return getFirstIfFoundAttrByCssQuery(offer, ".name > a", "href");
    }

    @Override
    protected String getOfferName(Element offer) {
        return getFirstIfFoundTextByCssQuery(offer, ".name > a");
    }
}
