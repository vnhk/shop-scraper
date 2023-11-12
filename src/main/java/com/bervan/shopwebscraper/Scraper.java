package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;
import com.google.gson.Gson;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class Scraper {
    private final ChromeOptions options = new ChromeOptions();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private final JsonService jsonService;
    private final ExcelService excelService;
    private final StatServerService statServerService;

    public Scraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        this.jsonService = jsonService;
        this.excelService = excelService;
        this.statServerService = statServerService;
    }

    public void run() {
        options.addArguments("--blink-settings=imagesEnabled=false");

        Date now = new Date();
        List<Offer> mediaExpertOffers = new ArrayList<>();
        List<ConfigRoot> roots = loadProductsFromConfig("/Users/zbyszek/IdeaProjects/ShopWebscraper/src/main/resources/config.json");
        ConfigRoot mediaExpert = roots.get(0);
        String baseUrl = mediaExpert.getBaseUrl();

        List<Future<List<Offer>>> tasks = new ArrayList<>();
        for (ConfigProduct product : mediaExpert.getProducts()) {
            Future<List<Offer>> offers = processProduct(now, mediaExpert, baseUrl, product);
            tasks.add(offers);
        }
        waitForOffers(mediaExpertOffers, tasks);

        System.out.printf("Processed %d offers.\n", mediaExpertOffers.size());
        try {
            jsonService.save(mediaExpertOffers, "products_shop_scrap-");
            excelService.save(mediaExpertOffers, "products_shop_scrap-");
        } catch (Exception e) {
            System.err.println("Could not save to file!");
            e.printStackTrace();
        }

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
                driver.get(url + "?limit=50");

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

    private List<ConfigRoot> loadProductsFromConfig(String configFilePath) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(configFilePath)) {
            return List.of(gson.fromJson(reader, ConfigRoot[].class));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config!");
        }
    }

    private static int getNumberOfPages(WebDriver driver) {
        String pageSource = driver.getPageSource();
        Document parsed = Jsoup.parse(pageSource);
        String pages = parsed.getElementsByClass("pagination").get(0)
                .getElementsByClass("from").get(0).text();
        return Integer.parseInt(pages.split("z ")[1]);
    }

    private void loadPageAndProcess(WebDriver driver, List<Offer> productOffers) {
        Document doc = Jsoup.parse(driver.getPageSource());
        List<Element> offerElements = findOffers(doc);

        if (offerElements.size() < 25) {
            // Scroll down to load more offers if needed
            // Implement scrolling logic
        }

        mediaExpertParseOffers(offerElements, productOffers);
    }

    private void processPages(WebDriver driver, int pages, List<Offer> productOffers, String url) {
        loadPageAndProcess(driver, productOffers);

        for (int currentPage = 2; currentPage <= pages; currentPage++) {
            String processedUrl = url + (url.contains("?") ? "&" : "?") + "limit=50&page=" + currentPage;
            System.out.println("Current url: " + processedUrl);
            driver.get(processedUrl);
            loadPageAndProcess(driver, productOffers);
        }
    }

    private static List<Element> findOffers(Document doc) {
        return doc.getElementsByClass("offer-box");
    }

    private void mediaExpertParseOffers(List<Element> offerElements, List<Offer> productOffers) {
        for (Element offer : offerElements) {
            String offerName = sanitize(getFirstIfFoundTextByCssQuery(offer, ".name > a"));
            String href = sanitize(getFirstIfFoundAttrByCssQuery(offer, ".name > a", "href"));
            String offerPrice = sanitize(offer.select(".main-price .whole").text());

            Offer o = new Offer();
            o.put("Name", offerName);
            o.put("Price", offerPrice);
            o.put("Offer Url", href);

            Element attributes = offer.select(".list.attributes").first();
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
                        o.put(attributeName, Arrays.stream(attributeValues.split(","))
                                .map(String::trim)
                                .map(this::sanitize)
                                .filter(Strings::isNotEmpty)
                                .collect(Collectors.toList()));
                    }
                }
            }

            productOffers.add(o);
        }
    }

    private String sanitize(String text) {
        return text.replace(" ", "")
                .replace("\\u0027", "'")
                .replace("\\u0026", "&");
    }

    private String getFirstIfFoundTextByCssQuery(Element offer, String cssQuery) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.text();
        }
        return elements.first().text();
    }

    private String getFirstIfFoundAttrByCssQuery(Element offer, String cssQuery, String attr) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.attr(attr);
        }
        return elements.first().attr(attr);
    }
}
