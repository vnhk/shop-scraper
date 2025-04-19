package com.bervan.shopwebscraper;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.SavingOffersToDBException;
import com.bervan.shopwebscraper.save.StatServerService;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public abstract class Scraper {
    protected final ChromeOptions options = new ChromeOptions();
    private ExecutorService executor;
    protected WebDriver driver;
    protected WebDriver newDriver;
    private final JsonService jsonService;
    private final ExcelService excelService;
    private final StatServerService statServerService;
    private final List<String> userAgents;

    public Scraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService, List<String> userAgents) {
        this.jsonService = jsonService;
        this.excelService = excelService;
        this.statServerService = statServerService;
        this.userAgents = userAgents;
    }

    public synchronized void create() {
        if (driver == null || newDriver == null) {
            options();
            driver = new ChromeDriver(options);
            newDriver = new ChromeDriver(options);
        }
    }

    public void run(ConfigRoot config, Date scrapDate, Integer hour) {
        executor = Executors.newFixedThreadPool(getNThreadsForConcurrentProcessing());
        create();
        List<Offer> offers = new ArrayList<>();
        List<Future<List<Offer>>> tasks = new ArrayList<>();
        for (ConfigProduct product : config.getProducts()) {
            if (!product.getScrapTime().getHours().equals(hour)) {
                continue;
            }
            ScrapContext context = new ScrapContext();
            context.setRoot(config);
            context.setProduct(product);
            context.setScrapDate(scrapDate);
            Future<List<Offer>> offerTasks = processProduct(context);
            tasks.add(offerTasks);
        }

        ScrapContext context = new ScrapContext();
        context.setRoot(config);
        context.setScrapDate(scrapDate);
        context.setThread(Thread.currentThread().getName());
        waitForOffers(offers, tasks, context);

        LogUtils.info(log, context, "Processed %d offers.", offers.size());
        saveToFile(config, offers, context);
    }

    protected abstract int getNThreadsForConcurrentProcessing();

    protected void options() {
//        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        String userAgent = userAgents.get(RandomUtil.getPositiveInt() % userAgents.size());
        options.addArguments("--user-agent=" + userAgent.trim());
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--ignore-ssl-errors=yes");
        options.addArguments("--ignore-certificate-errors");
    }

    private void saveToFile(ConfigRoot config, List<Offer> offers, ScrapContext context) {
        try {
            if (offers.size() > 0) {
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

    private void waitForOffers(List<Offer> offers, List<Future<List<Offer>>> tasks, ScrapContext context) {
        int i = 1;
        LogUtils.info(log, context, "Tasks: %d", tasks.size());

        for (Future<List<Offer>> task : tasks) {
            try {
                offers.addAll(task.get(30, TimeUnit.MINUTES));
                LogUtils.info(log, context, "Task %d finished!", i);
                i++;
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                task.cancel(true);
                throw new RuntimeException(e);
            }
        }
    }

    private Future<List<Offer>> processProduct(ScrapContext context) {
        String baseUrl = context.getRoot().getBaseUrl();
        Retryer<List<Offer>> retryer = RetryerBuilder.<List<Offer>>newBuilder()
                .retryIfExceptionOfType(ProductScrapException.class)
                .retryIfRuntimeException()
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();

        return executor.submit(() -> {
            create();
            Callable<List<Offer>> callable = () -> {
                String threadName = Thread.currentThread().getName();
                context.setThread(threadName);
                try {
                    LogUtils.info(log, context, "Started processing products.");
                    List<Offer> productOffers = new ArrayList<>();

                    String url = baseUrl + context.getProduct().getUrl();
                    goToFirstPage(url, context);
                    int pages = getNumberOfPages(context);
                    processPages(pages, productOffers, url, context);

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
                        if(!productOffers.isEmpty()) {
                            preSave(productOffers, context);
                            LogUtils.info(log, context, "Saving to database...");
                            statServerService.save(productOffers);
                            LogUtils.info(log, context, "Saved to database...");
                        } else {
                            LogUtils.info(log, context, "No offers to save to the database");
                        }

                    } catch (SavingOffersToDBException e) {
                        LogUtils.error(log, context, "Could not save to database:", e);
                    }

                    return productOffers;
                } catch (Exception e) {
                    LogUtils.error(log, context, "Could not parse products:", e);
                    throw new ProductScrapException("Could not parse products " + context.getProduct().getName() + "!", context.getProduct());
                } finally {
                    driver.quit();
                    newDriver.quit();
                    driver = null;
                    newDriver = null;
                    create();
                }
            };
            return retryer.call(callable);
        });
    }

    public static void applyWait(WebDriver driver) {
        if (driver == null) {
            return;
        }
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
        wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
    }

    protected void goToFirstPage(String url, ScrapContext context) {
        applyWait(driver);
        driver.get(getFirstPageUrlWithParams(url, context));
    }

    protected void goToPage(String url, ScrapContext context) {
        applyWait(driver);
        driver.get(url);
    }

    protected abstract String getFirstPageUrlWithParams(String url, ScrapContext context);

    protected abstract int getNumberOfPages(ScrapContext context);

    protected void loadPageAndProcess(List<Offer> productOffers, ScrapContext context) {
        List<Element> offerElements = loadAllOffersTiles(context);
        parseOffers(offerElements, productOffers, context);
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
                if (Strings.isNotBlank(imgSrc)) {
                    imgSrc = convertToBase64IfPossible(imgSrc);
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
            }
        }
    }

    private String convertToBase64IfPossible(String imgSrc) {
        if (imgSrc.startsWith("http")) {
            try (InputStream inputStream = new URL(imgSrc).openStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] imageBytes = outputStream.toByteArray();
                return Base64.getEncoder().encodeToString(imageBytes);
            } catch (Exception e) {
                log.error("Could not convert to base 64! " + imgSrc);
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
