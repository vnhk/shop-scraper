package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.scrapers.Scraper;
import com.google.gson.Gson;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ScrapProcessor {
    private final Map<String, Scraper> scrapers;
    private final ResourceLoader resourceLoader;
    @Value("${logs.path}")
    private String path = "";
    private final Jackson2JsonMessageConverter messageConverter;

    public ScrapProcessor(Map<String, Scraper> scrapers, ResourceLoader resourceLoader, Jackson2JsonMessageConverter messageConverter) {
        this.scrapers = scrapers;
        this.resourceLoader = resourceLoader;
        this.messageConverter = messageConverter;
    }

    public void addToQueue(String configFilePath, Integer hour, String... shops) {
        Date now = new Date();
        List<ConfigRoot> roots = loadProductsFromConfig(configFilePath);

        for (ConfigRoot root : roots) {
            String shopName = root.getShopName();
            if (Arrays.asList(shops).contains(shopName)) {
                Scraper scraper = scrapers.get(shopName);
                if (scraper == null) {
                    throw new RuntimeException("Scraper not found for given shop: " + shopName);
                }
                scraper.addToQueue(root, now, hour);
            }
        }
    }

    @RabbitListener(queues = "SCRAPER_QUEUE", concurrency = "1")
    public void processMessage(Message message) throws Exception {
        ScrapContext scrapContext = (ScrapContext) messageConverter.fromMessage(message);
        String shopName = scrapContext.getRoot().getShopName();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            scrapers.get(shopName).runOne(scrapContext);
        });

        try {
            future.get(1, TimeUnit.HOURS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Scraping took too long and was cancelled", e);
        } finally {
            executor.shutdownNow();
        }
    }
    private List<ConfigRoot> loadProductsFromConfig(String configFilePath) {
        Resource resource = resourceLoader.getResource("classpath:" + configFilePath);
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(resource.getFile())) {
            return List.of(gson.fromJson(reader, ConfigRoot[].class));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config!");
        }
    }

    public List<String> getLogs(Integer linesFromEnd) {
        List<String> res = new ArrayList<>();
        File file = new File(path);
        int counter = 0;
        try (ReversedLinesFileReader object = new ReversedLinesFileReader(file)) {
            while (counter < linesFromEnd) {
                res.add(object.readLine());
                counter++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return res;
    }
}
