package com.bervan.shopwebscraper;

import com.google.gson.Gson;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
public class ScrapProcessor {
    private final Map<String, Scraper> scrapers;
    private final ResourceLoader resourceLoader;

    public ScrapProcessor(Map<String, Scraper> scrapers, ResourceLoader resourceLoader) {
        this.scrapers = scrapers;
        this.resourceLoader = resourceLoader;
    }

    public void run(boolean scrapInMultiMode, String configFilePath, int hour, String... shops) {
        List<Future> tasks = new ArrayList<>();
        Date now = new Date();
        List<ConfigRoot> roots = loadProductsFromConfig(configFilePath);
        ExecutorService executor = Executors.newFixedThreadPool(roots.size());
        if (!scrapInMultiMode) {
            executor = Executors.newFixedThreadPool(1);
        }

        for (ConfigRoot root : roots) {
            String shopName = root.getShopName();
            if (Arrays.asList(shops).contains(shopName)) {
                Scraper scraper = scrapers.get(shopName);
                if (scraper == null) {
                    throw new RuntimeException("Scraper not found for given shop: " + shopName);
                }
                //threads
                tasks.add(executor.submit(() -> scraper.run(root, now, hour)));
            }
        }

        for (Future task : tasks) {
            try {
                task.get(120, TimeUnit.MINUTES);
            } catch (Exception e) {
                e.printStackTrace();
            }
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
}
