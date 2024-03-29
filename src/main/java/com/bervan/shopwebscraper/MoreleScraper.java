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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("Morele")
public class MoreleScraper extends Scraper {
    @Value("${MORELE_N_THREADS:1}")
    private final Integer N_THREADS = 1;

    public MoreleScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected int getNThreadsForConcurrentProcessing() {
        return N_THREADS;
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
        price = price.replaceAll("od", "")
                .replaceAll(" ", "");

        Elements outletButton = offer.select(".cat-product-outlet-button");
        if (outletButton.size() != 0) {
            String text = outletButton.get(0).text();
            String outletPrice = text.split(" od ")[1].split(" zł")[0].split(",")[0]
                    .replaceAll(" ", "").trim();

            if (outletPrice.equals(price)) {
                throw new SkipProcessingException("Product is outlet product!");
            }
        }

        return price;
    }

    @Override
    protected String getOfferHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, "a.productLink", "href");
    }

    @Override
    protected String getOfferImgHref(Element offer, ScrapContext context) {
        String src = getFirstIfFoundAttrByCssQuery(offer, "div.cat-product-left > a > picture > img", "src");
        if (src == null || src.isBlank()) {
            return getFirstIfFoundAttrByCssQuery(offer, "div.cat-product-left > a > picture > img", "data-src");
        }
        return src;
    }

    @Override
    protected String getOfferName(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, "a.productLink", "title").trim();
    }
}
