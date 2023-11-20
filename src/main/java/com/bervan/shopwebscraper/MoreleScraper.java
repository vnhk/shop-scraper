package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("Morele")
public class MoreleScraper extends Scraper {

    public MoreleScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected int getNThreadsForConcurrentProcessing() {
        return 8;
    }

    @Override
    protected String getFirstPageUrlWithParams(String url, ScrapContext context) {
        return url;
    }

    @Override
    protected int getNumberOfPages(WebDriver driver, ScrapContext context) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        Elements paginationLastPage = parsed.getElementsByClass("pagination-btn-nolink-anchor");
        String pages = "1";
        try {
            if (paginationLastPage.size() == 0) {
                pages = parsed.select(".pagination.dynamic").attr("data-count").trim();
            } else {
                pages = paginationLastPage.get(0).text().trim();
            }
        } catch (Exception e) {
            System.err.println("Could not find pages! Only one page will be processed!");
            pages = "1";
        }
        return Integer.parseInt(pages);
    }

    @Override
    protected List<Element> loadAllOffersTiles(WebDriver driver, ScrapContext context) {
        Document doc = Jsoup.parse(driver.getPageSource());
        return doc.getElementsByClass("cat-product");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context) {
        if (url.contains(",,,,")) {
            return url + "/" + currentPage;
        }
        return url + "/,,,,,,,,0,,,,/" + currentPage;
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer, ScrapContext context) {
        Element attributes = offerElement.select(".cat-product-features").first();
        if (attributes != null) {
            List<Element> items = attributes.select(".cat-product-feature");
            for (Element item : items) {
                String attributeName = sanitize(item.html()
                        .split("<b>")[0]
                        .trim());
                if (attributeName.endsWith(":")) {
                    attributeName = attributeName.substring(0, attributeName.length() - 1);
                }
                String attributeValues = item.attr("title").trim();
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
        String price = offer.select(".price-new")
                .text()
                .split("zł")[0]
                .split(",")[0]
                .trim();
        return price.replaceAll("od", "")
                .replaceAll(" ", "");
    }

    @Override
    protected String getOfferHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, "a.productLink", "href");
    }

    @Override
    protected String getOfferName(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, "a.productLink", "title").trim();
    }
}
