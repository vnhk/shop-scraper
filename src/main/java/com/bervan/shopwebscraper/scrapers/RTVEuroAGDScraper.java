package com.bervan.shopwebscraper.scrapers;

import com.bervan.shopwebscraper.Offer;
import com.bervan.shopwebscraper.ScrapContext;
import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.QueueService;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service("RTV Euro AGD")
@Scope("prototype")
public class RTVEuroAGDScraper extends Scraper {

    private static final String PAGE_SIZE = "20";

    public RTVEuroAGDScraper(JsonService jsonService, ExcelService excelService, QueueService queueService,
                             @Value("#{'${USER_AGENTS}'.split(',,,,')}") List<String> userAgents) {
        super(jsonService, excelService, queueService, userAgents);
    }

    @Override
    protected String getFirstPageUrlWithParams(String url, ScrapContext context) {
        return url;
    }

//    @Override
//    protected void options() {
//        super.options();
////        options.addArguments("--blink-settings=imagesEnabled=false");
//    }

    @Override
    protected int getNumberOfPages(ScrapContext context) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        Elements elementsByClass = parsed.getElementsByClass("progress-info");
        if (elementsByClass.isEmpty()) {
            return 1;
        }
        String info = elementsByClass.get(0).text().trim();

        Matcher matcher = Pattern.compile("Zobaczyłeś (\\d+) z (\\d+)").matcher(info);
        if (matcher.find()) {
            int pageSizeBeforeZ = Integer.parseInt(matcher.group(1));
            int pageSizeAfterZ = Integer.parseInt(matcher.group(2));
            if (pageSizeBeforeZ == pageSizeAfterZ) {
                // only one page
                return 1;
            } else {
                // more than one page
                int allProducts = pageSizeAfterZ;
                int productsPerPage = pageSizeBeforeZ;
                Double ratio = allProducts * 1.0 / productsPerPage;
                if (allProducts % productsPerPage == 0) {
                    return ratio.intValue();
                } else {
                    return ratio.intValue() + 1;
                }
            }
        } else {
            //only one page
            return 1;
        }
    }

    @Override
    protected void preSave(List<Offer> productOffers, ScrapContext context) {
        // Extract names
        List<String> names = productOffers.stream()
                .map(offer -> offer.get("Name").toString())
                .collect(Collectors.toList());

        // Find common substrings
        String commonSubstring = findCommonSubstring(names);

        if (!commonSubstring.isEmpty()) {
            System.out.println("Common substring found: " + commonSubstring);

            // Update offer names, removing the common substring
            for (Offer offer : productOffers) {
                String name = offer.get("Name").toString();
                name = name.replace(commonSubstring, "").trim();
                offer.put("Name", name);
            }
        }
    }

    private String findCommonSubstring(List<String> strings) {
        if (strings.isEmpty()) {
            return "";
        }

        // Start with smallest string as a candidate for common substring
        String reference = strings.get(0);
        String longestCommonSubstring = "";

        for (int i = 0; i < reference.length(); i++) {
            for (int j = i + 4; j <= reference.length(); j++) {
                String subStr = reference.substring(i, j);
                if (strings.stream().allMatch(s -> s.contains(subStr)) && subStr.length() > longestCommonSubstring.length()) {
                    longestCommonSubstring = subStr;
                }
            }
        }

        return longestCommonSubstring;
    }


    @Override
    protected List<Element> loadAllOffersTiles(ScrapContext context) {
        Elements offers = loadOffers(driver);

        int tries = 0;
        //page can be not loaded yet, try wait 15s to load content
        //or it is the last page
        //to refactor
        while (offers.size() < Integer.parseInt(PAGE_SIZE) && tries < 15) {
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
        return doc.getElementsByClass("product-list-results__product-box");
    }

    @Override
    protected String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context) {
        return url.split("\\.bhtml")[0] + ",strona-" + currentPage + ".bhtml";
    }

    @Override
    protected void processProductAdditionalAttributes(Element offerElement, Offer offer, ScrapContext context) {
        Element attributes = offerElement.select(".product-medium-box-content__desc").first();
        if (attributes != null) {
            List<Element> items = attributes.select(".technical-data__list-item");
            for (Element item : items) {
                String attributeName = sanitize(item.text().trim());
                String parsedAttributeName = attributeName;
                String attributeValues="";
                if (attributeName.contains(":")) {
                    parsedAttributeName = attributeName.split(":")[0].trim();
                    attributeValues = attributeName.split(":")[1].trim();
                }

                if (!parsedAttributeName.isBlank()) {
                    offer.put(parsedAttributeName, Arrays.stream(attributeValues.split(", "))
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
        return offer.select(".parted-price-total")
                .text()
                .replace(" ", "")
                .trim();
    }

    @Override
    protected String getOfferHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, ".product-medium-box-intro__link", "href");
    }

    @Override
    protected String getOfferImgHref(Element offer, ScrapContext context) {
        return getFirstIfFoundAttrByCssQuery(offer, "img", "src");
    }

    @Override
    protected String getOfferName(Element offer, ScrapContext context) {
        return getFirstIfFoundTextByCssQuery(offer, ".product-medium-box-intro__link");
    }
}
