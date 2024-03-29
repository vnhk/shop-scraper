package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("Media Expert")
public class MediaExpertScraper extends Scraper {

    @Value("${MEDIA_EXPERT_N_THREADS:1}")
    private final Integer N_THREADS = 1;

    public MediaExpertScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected int getNThreadsForConcurrentProcessing() {
        return N_THREADS;
    }

    @Override
    protected String getFirstPageUrlWithParams(String url, ScrapContext context) {
        return url + "?limit=50";
    }

    @Override
    protected int getNumberOfPages(WebDriver driver, ScrapContext context) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        String pages = parsed.getElementsByClass("pagination").get(0)
                .getElementsByClass("from").get(0).text();
        return Integer.parseInt(pages.split("z ")[1]);
    }

    @Override
    protected List<Element> loadAllOffersTiles(WebDriver driver, ScrapContext context) {
        Document doc = Jsoup.parse(driver.getPageSource());
        return doc.getElementsByClass("offer-box");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context) {
        return url + (url.contains("?") ? "&" : "?") + "limit=50&page=" + currentPage;
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer, ScrapContext context) {
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
    protected String getOfferPrice(Element offer, ScrapContext context) {
        return offer.select(".main-price .whole").text();
    }

    @Override
    protected String getOfferHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, ".name > a", "href");
    }

    @Override
    protected String getOfferImgHref(Element offer, ScrapContext context) {
        String src = getFirstIfFoundAttrByCssQuery(offer, "div.product-list-gallery-slider.is-possible-hover > a > div:nth-child(1) > img", "src");
        if (src == null || src.isBlank()) {
            return getFirstIfFoundAttrByCssQuery(offer, "img.is-loaded", "src");
        }
        return src;
    }

    @Override
    protected String getOfferName(Element offer, ScrapContext context) {
        return getFirstIfFoundTextByCssQuery(offer, ".name > a");
    }
}
