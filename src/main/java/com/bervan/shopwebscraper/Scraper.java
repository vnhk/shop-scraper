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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;

@Slf4j
@Component
public abstract class Scraper {
    protected final ChromeOptions options = new ChromeOptions();
    private ExecutorService executor;
    private final JsonService jsonService;
    private final ExcelService excelService;
    private final StatServerService statServerService;
    @Value("#{'${USER_AGENTS}'.split(',,,,')}")
    private List<String> userAgents;

    public Scraper(JsonService jsonService, ExcelService excelService, StatServerService statServerService) {
        this.jsonService = jsonService;
        this.excelService = excelService;
        this.statServerService = statServerService;
    }

    public void run(ConfigRoot config, Date scrapDate) {
        executor = Executors.newFixedThreadPool(getNThreadsForConcurrentProcessing());
        List<Offer> offers = new ArrayList<>();
        options();

        waitAndRunBrowserToPreventExceptionOnStart(config);

        List<Future<List<Offer>>> tasks = new ArrayList<>();
        for (ConfigProduct product : config.getProducts()) {
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

        LogUtils.info(log, context, "Processed {} offers.", offers.size());
        saveToFile(config, offers, context);
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
        } catch (Exception e) {
            log.error("waitAndRunBrowserToPreventExceptionOnStart:", e);
        }
    }

    protected abstract int getNThreadsForConcurrentProcessing();

    protected void options() {
//        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--headless");
        String userAgent = userAgents.get(RandomUtil.getPositiveInt() % userAgents.size());
        options.addArguments("--user-agent=" + userAgent.trim());
    }

    private void saveToFile(ConfigRoot config, List<Offer> offers, ScrapContext context) {
        try {
            String filenamePrefix = getFilenamePrefix(config);
            LogUtils.info(log, context, "Saving to files...");
            jsonService.save(offers, filenamePrefix);
            excelService.save(offers, filenamePrefix);
            LogUtils.info(log, context, "Saved to files...");
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
        LogUtils.info(log, context, "Tasks: {}", tasks.size());

        for (Future<List<Offer>> task : tasks) {
            try {
                offers.addAll(task.get(30, TimeUnit.MINUTES));
                LogUtils.info(log, context, "Task {} finished!", i);
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
            Callable<List<Offer>> callable = () -> {
                String threadName = Thread.currentThread().getName();
                context.setThread(threadName);
                WebDriver driver = new ChromeDriver(options);
                try {
                    LogUtils.info(log, context, "Started processing products.");
                    List<Offer> productOffers = new ArrayList<>();

                    String url = baseUrl + context.getProduct().getUrl();
                    goToPage(driver, url, context);
                    int pages = getNumberOfPages(driver, context);
                    processPages(driver, pages, productOffers, url, context);

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
                    driver.quit();

                    try {
                        LogUtils.info(log, context, "Saving to database...");
                        statServerService.save(productOffers);
                        LogUtils.info(log, context, "Saved to database...");
                    } catch (SavingOffersToDBException e) {
                        LogUtils.error(log, context, "Could not save to database:", e);
                    }

                    return productOffers;
                } catch (Exception e) {
                    LogUtils.error(log, context, "Could not parse products:", e);
                    driver.quit();
                    throw new ProductScrapException("Could not parse products " + context.getProduct().getName() + "!", context.getProduct());
                }
            };
            return retryer.call(callable);
        });
    }

    protected void goToPage(WebDriver driver, String url, ScrapContext context) {
        driver.get(getFirstPageUrlWithParams(url, context));
    }

    protected abstract String getFirstPageUrlWithParams(String url, ScrapContext context);

    protected abstract int getNumberOfPages(WebDriver driver, ScrapContext context);

    protected void loadPageAndProcess(WebDriver driver, List<Offer> productOffers, ScrapContext context) {
        List<Element> offerElements = loadAllOffersTiles(driver, context);
        parseOffers(offerElements, productOffers, context);
    }

    protected void processPages(WebDriver driver, int pages, List<Offer> productOffers, String url, ScrapContext context) {
        loadPageAndProcess(driver, productOffers, context);

        for (int currentPage = 2; currentPage <= pages; currentPage++) {
            String processedUrl = getUrlWithParametersForPage(url, currentPage, context);
            LogUtils.debug(log, context, "Current Url: {}", processedUrl);
            goToPage(driver, processedUrl, context);
            loadPageAndProcess(driver, productOffers, context);
        }
    }


    protected abstract String getUrlWithParametersForPage(String url, int currentPage, ScrapContext context);

    protected abstract List<Element> loadAllOffersTiles(WebDriver driver, ScrapContext context);

    protected void parseOffers(List<Element> offerElements, List<Offer> productOffers, ScrapContext context) {
        for (Element offerElement : offerElements) {
            String offerName = sanitize(getOfferName(offerElement, context));
            String href = sanitize(getOfferHref(offerElement, context));
            String imgSrc = sanitize(getOfferImgHref(offerElement, context));
            String offerPrice = sanitize(getOfferPrice(offerElement, context));

            Offer offer = new Offer();
            offer.put("Name", offerName);
            offer.put("Price", offerPrice);
            offer.put("Offer Url", href);
            offer.put("Image", imgSrc);

            processProductAdditionalAttributes(offerElement, offer, context);

            productOffers.add(offer);
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
