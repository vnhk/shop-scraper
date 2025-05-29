package com.bervan.shopwebscraper.scrapers;

import com.bervan.shopwebscraper.Offer;
import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.QueueService;
import com.bervan.shstat.ScrapContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("Centrum Rowerowe")
@Scope("prototype")
public class CentrumRoweroweScraper extends Scraper {

    public CentrumRoweroweScraper(JsonService jsonService, ExcelService excelService, QueueService queueService, @Value("#{'${USER_AGENTS}'.split(',,,,')}") List<String> userAgents) {
        super(jsonService, excelService, queueService, userAgents);
    }

    @Override
    protected String getFirstPageUrlWithParams(String url, ScrapContext context) {
        MinMaxParam result = getMinMaxParam(context);

        if (result.priceCriteria()) {
            return url + (url.contains("?") ? "&" : "?") + "price=" + result.price();
        }

        return url;
    }


    @Override
    protected int getNumberOfPages(ScrapContext context) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        Elements elementsByClass = parsed.getElementsByClass("page-number");
        String pages = elementsByClass.get(elementsByClass.size() - 1)
                .text();
        return Integer.parseInt(pages);
    }

    @Override
    protected void preSave(List<Offer> productOffers, ScrapContext context) {

    }

    @Override
    protected List<Element> loadAllOffersTiles(ScrapContext context) {
        Document doc = Jsoup.parse(driver.getPageSource());
        return doc.getElementsByClass("product");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context) {
        url = url + (url.contains("?") ? "&" : "?") + "page=" + currentPage;
        MinMaxParam result = getMinMaxParam(context);

        if (result.priceCriteria()) {
            return url + "&price=" + result.price();
        }

        return url;
    }

    @Override
    protected String getOfferName(Element offer, ScrapContext context) {
        return getFirstIfFoundTextByCssQuery(offer, ".bottom .name > a");
    }

    @Override
    protected String getOfferPrice(Element offer, ScrapContext context) {
        return offer.select(".final-price > .int-part").text().replaceAll(" ", "");
    }

    @Override
    protected String getOfferHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, ".bottom .name > a", "href");
    }

    @Override
    protected String getOfferImgHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, ".photo > img", "data-default");
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer, ScrapContext context) {

    }

    private MinMaxParam getMinMaxParam(ScrapContext context) {
        Integer minPrice = 1;
        Integer maxPrice = 10000000;
        boolean priceCriteria = false;
        if (context.getProduct().getMinPrice() != null) {
            minPrice = context.getProduct().getMinPrice();
            priceCriteria = true;
        }

        if (context.getProduct().getMaxPrice() != null) {
            maxPrice = context.getProduct().getMaxPrice();
            priceCriteria = true;
        }

        String price = minPrice + "-" + maxPrice;
        return new MinMaxParam(priceCriteria, price);
    }

    private record MinMaxParam(boolean priceCriteria, String price) {
    }
}
