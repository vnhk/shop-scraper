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

@Service("Media Expert")
public class MediaExpertScraper extends Scraper {

    public MediaExpertScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected String getFirstPageUrlWithParams(String url) {
        return url + "?limit=50";
    }

    @Override
    protected int getNumberOfPages(WebDriver driver) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        String pages = parsed.getElementsByClass("pagination").get(0)
                .getElementsByClass("from").get(0).text();
        return Integer.parseInt(pages.split("z ")[1]);
    }

    @Override
    protected List<Element> loadAllOffersTiles(Document doc) {
        return doc.getElementsByClass("offer-box");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage) {
        return url + (url.contains("?") ? "&" : "?") + "limit=50&page=" + currentPage;
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
                    offer.put(attributeName, Arrays.stream(attributeValues.split(", "))
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
