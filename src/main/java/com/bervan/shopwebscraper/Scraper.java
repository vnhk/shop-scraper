package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

public abstract class Scraper {
    protected final ChromeOptions options = new ChromeOptions();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final JsonService jsonService;
    private final ExcelService excelService;
    private final StatServerService statServerService;

    public Scraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        this.jsonService = jsonService;
        this.excelService = excelService;
        this.statServerService = statServerService;
    }

    public void run(ConfigRoot config, Date scrapDate) {
        List<Offer> offers = new ArrayList<>();
        options();

        String baseUrl = config.getBaseUrl();

        List<Future<List<Offer>>> tasks = new ArrayList<>();
        for (ConfigProduct product : config.getProducts()) {
            Future<List<Offer>> offerTasks = processProduct(scrapDate, config, baseUrl, product);
            tasks.add(offerTasks);
        }

        waitForOffers(offers, tasks);

        System.out.printf("Processed %d offers.\n", offers.size());
        saveToFile(config, offers);
    }

    protected void options() {
        options.addArguments("--blink-settings=imagesEnabled=false");
    }

    private void saveToFile(ConfigRoot config, List<Offer> offers) {
        try {
            String filenamePrefix = getFilenamePrefix(config);
            jsonService.save(offers, filenamePrefix);
            excelService.save(offers, filenamePrefix);
        } catch (Exception e) {
            System.err.println("Could not save to file!");
            e.printStackTrace();
        }
    }

    protected String getFilenamePrefix(ConfigRoot config) {
        String shopName = config.getShopName().replaceAll(" ", "_")
                .toUpperCase(Locale.ROOT);
        return "products_shop_scrap_" + shopName + "-";
    }

    private void waitForOffers(List<Offer> mediaExpertOffers, List<Future<List<Offer>>> tasks) {
        for (Future<List<Offer>> task : tasks) {
            try {
                mediaExpertOffers.addAll(task.get(30, TimeUnit.MINUTES));
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Future<List<Offer>> processProduct(Date now, ConfigRoot config, String baseUrl, ConfigProduct product) {
        return executor.submit(() -> {
            WebDriver driver = new ChromeDriver(options);
            try {
                System.out.println(" - " + product.getName());
                List<Offer> productOffers = new ArrayList<>();

                String url = baseUrl + product.getUrl();
                driver.get(getFirstPageUrlWithParams(url));
                int pages = getNumberOfPages(driver);
                processPages(driver, pages, productOffers, url);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String formattedDate = simpleDateFormat.format(now);
                for (Offer offer : productOffers) {
                    offer.put("Date", now.getTime());
                    offer.put("Formatted Date", formattedDate);
                    offer.put("Product List Name", product.getName());
                    offer.put("Categories", product.getCategories());
                    offer.put("Product List Url", product.getUrl());
                    offer.put("Shop", config.getShopName());
                }

                statServerService.save(productOffers);

                return productOffers;
            } catch (Exception e) {
                System.err.println("Could not parse product " + product.getName() + "!");
                throw new RuntimeException("Could not parse product " + product.getName() + "!");
            } finally {
                driver.quit();
            }
        });
    }

    protected abstract String getFirstPageUrlWithParams(String url);

    protected abstract int getNumberOfPages(WebDriver driver);

    protected void loadPageAndProcess(WebDriver driver, List<Offer> productOffers) {
        Document doc = Jsoup.parse(driver.getPageSource());
        List<Element> offerElements = loadAllOffersTiles(doc);
        parseOffers(offerElements, productOffers);
    }

    protected void processPages(WebDriver driver, int pages, List<Offer> productOffers, String url) {
        loadPageAndProcess(driver, productOffers);

        for (int currentPage = 2; currentPage <= pages; currentPage++) {
            String processedUrl = getUrlWithParametersForPage(url, currentPage);
            System.out.println("Current url: " + processedUrl);
            driver.get(processedUrl);
            loadPageAndProcess(driver, productOffers);
        }
    }


    protected abstract String getUrlWithParametersForPage(String url, int currentPage);

    protected abstract List<Element> loadAllOffersTiles(Document doc);

    protected void parseOffers(List<Element> offerElements, List<Offer> productOffers) {
        for (Element offerElement : offerElements) {
            String offerName = sanitize(getOfferName(offerElement));
            String href = sanitize(getOfferHref(offerElement));
            String offerPrice = sanitize(getOfferPrice(offerElement));

            Offer offer = new Offer();
            offer.put("Name", offerName);
            offer.put("Price", offerPrice);
            offer.put("Offer Url", href);

            processProductAdditionalAttributes(offerElement, offer);

            productOffers.add(offer);
        }
    }

    protected abstract void processProductAdditionalAttributes(Element offerElement, Offer offer);

    protected abstract String getOfferPrice(Element offer);

    protected abstract String getOfferHref(Element offer);

    protected abstract String getOfferName(Element offer);

    protected String sanitize(String text) {
        return text.replace(" ", "")
                .replace("\\u0027", "'")
                .replace("\\u0026", "&");
    }

    protected String getFirstIfFoundTextByCssQuery(Element offer, String cssQuery) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.text();
        }
        return elements.first().text();
    }

    protected String getFirstIfFoundAttrByCssQuery(Element offer, String cssQuery, String attr) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.attr(attr);
        }
        return elements.first().attr(attr);
    }
}
