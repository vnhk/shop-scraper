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

@Service("RTV Euro AGD")
public class RTVEuroAGDScraper extends Scraper {

    private static final String PAGE_SIZE = "15";

    public RTVEuroAGDScraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        super(jsonService, excelService, statServerService);
    }

    @Override
    protected String getFirstPageUrlWithParams(String url) {
        return url;
    }

    @Override
    protected int getNumberOfPages(WebDriver driver) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        String info = parsed.getElementsByClass("progress-info").get(0).text().trim();
        if (info.startsWith("Zobaczyłeś " + PAGE_SIZE + " z " + PAGE_SIZE)) {
            //only one page
            return 1;
        } else if (info.startsWith("Zobaczyłeś " + PAGE_SIZE + " z ")) {
            //more than one page
            int allProducts = Integer.parseInt(info.split("Zobaczyłeś " + PAGE_SIZE + " z ")[1].split(" ")[0]);
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
    protected List<Element> loadAllOffersTiles(WebDriver driver) {
        Elements offers = loadOffers(driver);

        int tries = 0;
        //page can be not loaded yet, try wait 15s to load content
        //or it is the last page
        //to refactor
        while (offers.size() < Integer.parseInt(PAGE_SIZE) || tries < 15) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            offers = loadOffers(driver);
            tries++;
        }

        return offers;
    }

    private Elements loadOffers(WebDriver driver) {
        Document doc = Jsoup.parse(driver.getPageSource());
        Elements offers = doc.getElementsByClass("product-list__product-box");
        offers.addAll(doc.getElementsByClass("product-paginator__box-container--first"));
        return offers;
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage) {
        return url.split("\\.bhtml")[0] + ",strona-" + currentPage + ".bhtml";
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer) {
        Element attributes = offerElement.select(".box-medium__desc").first();
        if (attributes != null) {
            List<Element> items = attributes.select(".box-medium__specs-item");
            for (Element item : items) {
                String attributeName = sanitize(item.select("span").get(0).text()
                        .trim());
                if (attributeName.endsWith(":")) {
                    attributeName = attributeName.substring(0, attributeName.length() - 1);
                }
                String attributeValues = item.select("span").get(1).text().trim();
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
        return offer.select(".box-medium__price .price-template__large--total")
                .text()
                .replace(" ", "")
                .trim();
    }

    @Override
    protected String getOfferHref(Element offer) {
        return getFirstIfFoundAttrByCssQuery(offer, ".box-medium__link", "href");
    }

    @Override
    protected String getOfferName(Element offer) {
        return getFirstIfFoundTextByCssQuery(offer, ".box-medium__link");
    }
}
