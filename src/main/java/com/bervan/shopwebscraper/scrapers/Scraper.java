package com.bervan.shopwebscraper.scrapers;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.bervan.shopwebscraper.*;
import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.QueueService;
import com.bervan.shopwebscraper.save.SavingOffersToDBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@Slf4j
public abstract class Scraper {
    protected final ChromeOptions options = new ChromeOptions();
    //    private ExecutorService executor;
    //no threads better to add new device and run it again with different config/or read from the same queue
    protected WebDriver driver;
    protected WebDriver newDriver;
    private final JsonService jsonService;
    private final ExcelService excelService;
    private final QueueService queueService;
    private final List<String> userAgents;

    public Scraper(JsonService jsonService, ExcelService excelService, QueueService queueService, List<String> userAgents) {
        this.jsonService = jsonService;
        this.excelService = excelService;
        this.queueService = queueService;
        this.userAgents = userAgents;
    }

    public synchronized void create(ConfigRoot config) {
        try {
            options();
            waitAndRunBrowserToPreventExceptionOnStart(config);
            if (driver != null) {
                driver.quit();
            }

            if (newDriver != null) {
                newDriver.quit();
            }

            driver = new ChromeDriver(options);
            newDriver = new ChromeDriver(options);
        } catch (Exception e) {
            log.error("Could not execute 'create'! {}", e.getMessage());
            if (driver != null) {
                driver.quit();
            }

            if (newDriver != null) {
                newDriver.quit();
            }
            driver = new ChromeDriver(options);
            newDriver = new ChromeDriver(options);
        }
    }

    private void waitAndRunBrowserToPreventExceptionOnStart(ConfigRoot config) {
        try {
            ChromeDriver driver = new ChromeDriver(options);
            driver.get(config.getBaseUrl());
            driver.quit();
            driver = new ChromeDriver(options);
            driver.get(config.getBaseUrl());
            driver.quit();
            driver = new ChromeDriver(options);
            driver.get(config.getBaseUrl());
            driver.quit();
            driver = new ChromeDriver(options);
            driver.get(config.getBaseUrl());
            driver.quit();
        } catch (Exception e) {
            log.error("waitAndRunBrowserToPreventExceptionOnStart: EXCEPTION: {}", e.getMessage());
        }
    }

    public void addToQueue(ConfigRoot config, Date scrapDate, Integer hour) {
        for (ConfigProduct product : config.getProducts()) {
            if (!product.getScrapTime().getHours().equals(hour)) {
                continue;
            }
            ScrapContext context = new ScrapContext();
            context.setRoot(config);
            context.setProduct(product);
            context.setScrapDate(scrapDate);

            LogUtils.info(log, context, "Adding scrapping request to queue!");
            queueService.addScrapingToQueue(context);
        }
    }

    public void runOne(ScrapContext context) {
        try {
            LogUtils.info(log, context, "Started runOne!");
            context.setThread(Thread.currentThread().getName());

            create(context.getRoot());
            List<Offer> offers = processProduct(context);

            LogUtils.info(log, context, "Processed %d offers.", offers.size());
            saveToFile(context.getRoot(), offers, context);
        } finally {
            if (driver != null) {
                driver.quit();
            }

            if (newDriver != null) {
                newDriver.quit();
            }
        }
    }

    protected void options() {
//        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        String userAgent = userAgents.get(RandomUtil.getPositiveInt() % userAgents.size());
        options.addArguments("--user-agent=" + userAgent.trim());
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--ignore-ssl-errors=yes");
        options.addArguments("--ignore-certificate-errors");
    }

    private void saveToFile(ConfigRoot config, List<Offer> offers, ScrapContext context) {
        try {
            if (!offers.isEmpty()) {
                String filenamePrefix = getFilenamePrefix(config);
                LogUtils.info(log, context, "Saving to files...");
                jsonService.save(offers, filenamePrefix);
                excelService.save(offers, filenamePrefix);
                LogUtils.info(log, context, "Saved to files...");
            } else {
                LogUtils.info(log, context, "No offers to process!");
            }

        } catch (Exception e) {
            LogUtils.error(log, context, "Could not save to file!", e);
        }
    }

    protected String getFilenamePrefix(ConfigRoot config) {
        String shopName = config.getShopName().replaceAll(" ", "_")
                .toUpperCase(Locale.ROOT);
        return "products_shop_scrap_" + shopName + "-";
    }

    private List<Offer> processProduct(ScrapContext context) {
        String baseUrl = context.getRoot().getBaseUrl();
            ScrapContext newContext = new ScrapContext();
            newContext.setProduct(context.getProduct());
            newContext.setRoot(context.getRoot());
            newContext.setThread(context.getThread());
            newContext.setScrapDate(context.getScrapDate());
            create(newContext.getRoot());
                try {
                    LogUtils.info(log, newContext, "Started processing products.");
                    List<Offer> productOffers = new ArrayList<>();

                    String url = baseUrl + newContext.getProduct().getUrl();
                    goToFirstPage(url, newContext);
                    int pages = getNumberOfPages(newContext);
                    processPages(pages, productOffers, url, newContext);

                    return productOffers;
                } catch (Exception e) {
                    LogUtils.error(log, newContext, "Could not parse products:", e);
                }
        return new ArrayList<>();
    }

    public static void applyWait(WebDriver driver) {
        try {
            if (driver == null) {
                return;
            }
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        } catch (Exception e) {
            log.error("Could not 'applyWait', Exception: {}", e.getMessage());
        }
    }

    protected void goToFirstPage(String url, ScrapContext context) {
        applyWait(driver);
        if (driver == null) {
            create(context.getRoot());
        }
        driver.get(getFirstPageUrlWithParams(url, context));
    }

    protected void goToPage(String url, ScrapContext context) {
        applyWait(driver);
        if (driver == null) {
            create(context.getRoot());
        }
        driver.get(url);
    }

    protected abstract String getFirstPageUrlWithParams(String url, ScrapContext context);

    protected abstract int getNumberOfPages(ScrapContext context);

    protected void loadPageAndProcess(List<Offer> allProductOffers, ScrapContext context) {
        List<Offer> productOffers = new ArrayList<>();
        List<Element> offerElements = loadAllOffersTiles(context);
        parseOffers(offerElements, productOffers, context);
        allProductOffers.addAll(productOffers);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDate = simpleDateFormat.format(context.getScrapDate());
        for (Offer offer : productOffers) {
            offer.put("Date", context.getScrapDate().getTime());
            offer.put("Formatted Date", formattedDate);
            offer.put("Product List Name", context.getProduct().getName());
            offer.put("Categories", context.getProduct().getCategories());
            offer.put("Product List Url", context.getProduct().getUrl());
            offer.put("Shop", context.getRoot().getShopName());
        }

        try {
            if (!productOffers.isEmpty()) {
                preSave(productOffers, context);
                LogUtils.info(log, context, "Saving" + productOffers.size() + "  to database...");
                queueService.save(productOffers);
                LogUtils.info(log, context, "Saved " + productOffers.size() + " context to database...");
            } else {
                LogUtils.info(log, context, "No offers to save to the database");
            }

        } catch (SavingOffersToDBException e) {
            LogUtils.error(log, context, "Could not save to database:", e);
        }

    }

    protected abstract void preSave(List<Offer> productOffers, ScrapContext context);

    protected void processPages(int pages, List<Offer> productOffers, String url, ScrapContext context) {
        loadPageAndProcess(productOffers, context);

        for (int currentPage = 2; currentPage <= pages; currentPage++) {
            String processedUrl = getUrlWithParametersForPage(url, currentPage, context);
            LogUtils.debug(log, context, "Current Url: %s", processedUrl);
            goToPage(processedUrl, context);
            loadPageAndProcess(productOffers, context);
        }
    }


    protected abstract String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context);

    protected abstract List<Element> loadAllOffersTiles(ScrapContext context);

    protected void parseOffers(List<Element> offerElements, List<Offer> productOffers, ScrapContext context) {
        LogUtils.info(log, context, "Found " + offerElements.size() + " to process.");
        int base64ImagesFailed = 0;
        for (Element offerElement : offerElements) {
            try {
                String offerName = sanitize(getOfferName(offerElement, context)).trim();
                String href = sanitize(getOfferHref(offerElement, context)).trim();
                if (!href.contains(context.getRoot().getBaseUrl()) && Strings.isNotBlank(href) && !href.contains("http")) {
                    String newHref = context.getRoot().getBaseUrl();
                    if (!(context.getRoot().getBaseUrl().endsWith("/") || href.startsWith("/"))) {
                        newHref += "/";
                    }
                    newHref += href;
                    href = newHref;
                }
                String imgSrc = sanitize(getOfferImgHref(offerElement, context)).trim();
                if (Strings.isNotBlank(imgSrc) && base64ImagesFailed <= 3) {
                    imgSrc = convertToBase64IfPossible(imgSrc);
                    if (imgSrc.startsWith("http")) {
                        base64ImagesFailed++;
                    } else {
                        base64ImagesFailed = 0;
                    }
                }

                String offerPrice = sanitize(getOfferPrice(offerElement, context));

                Offer offer = new Offer();
                offer.put("Name", offerName);
                offer.put("Price", offerPrice);
                offer.put("Offer Url", href);
                offer.put("Image", imgSrc);

                processProductAdditionalAttributes(offerElement, offer, context);

                productOffers.add(offer);
            } catch (SkipProcessingException e) {
                LogUtils.info(log, context, "Offer is skipped: " + e.getMessage());
            } catch (Exception e) {
                LogUtils.error(log, context, "Offer could not been parsed!", e);
            }
        }
    }

    private String convertToBase64IfPossible(String imgSrc) {
        if (imgSrc.startsWith("http")) {
            try {
                URL url = new URL(imgSrc);
                URLConnection conn = url.openConnection();
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(3000);
                try (InputStream inputStream = conn.getInputStream();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }

                    byte[] imageBytes = outputStream.toByteArray();
                    return Base64.getEncoder().encodeToString(imageBytes);
                } catch (Exception e2) {
                    log.error("Could not convert to base 64! {}: {}", imgSrc, e2.getMessage());
                    return imgSrc;
                }
            } catch (Exception e) {
                log.error("Could not convert to base 64! {}", imgSrc);
                return imgSrc;
            }

        } else {
            return imgSrc;
        }
    }

    protected abstract void processProductAdditionalAttributes(Element offerElement, Offer offer, ScrapContext context);

    protected abstract String getOfferPrice(Element offer, ScrapContext context);

    protected abstract String getOfferHref(Element offer, ScrapContext context);

    protected abstract String getOfferImgHref(Element offer, ScrapContext context);

    protected abstract String getOfferName(Element offer, ScrapContext context);

    protected String sanitize(String text) {
        return text.replace(" ", "")
                .replace("\\u0027", "'")
                .replace("\\u0026", "&")
                .trim();
    }

    protected String getFirstIfFoundTextByCssQuery(Element offer, String cssQuery) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.text();
        }
        return Objects.requireNonNull(elements.first()).text();
    }

    protected String getFirstIfFoundAttrByCssQuery(Element offer, String cssQuery, String attr) {
        Elements elements = offer.select(cssQuery);
        if (elements.first() == null) {
            return elements.attr(attr);
        }
        return Objects.requireNonNull(elements.first()).attr(attr);
    }
}
