package com.bervan.shopwebscraper;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ScrapProcessor {
    private final Map<String, Scraper> scrapers;

    public ScrapProcessor(Map<String, Scraper> scrapers) {
        this.scrapers = scrapers;
    }

    public void run() {
        Date now = new Date();
        List<ConfigRoot> roots = loadProductsFromConfig("/Users/zbyszek/IdeaProjects/ShopWebscraper/src/main/resources/config.json");
        ExecutorService executor = Executors.newFixedThreadPool(roots.size());
        for (ConfigRoot root : roots) {
            String shopName = root.getShopName();
            Scraper scraper = scrapers.get(shopName);
            if (scraper == null) {
                throw new RuntimeException("Scraper not found for given shop: " + shopName);
            }

            //threads
            executor.submit(() -> scraper.run(root, now));
        }
    }

    private List<ConfigRoot> loadProductsFromConfig(String configFilePath) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(configFilePath)) {
            return List.of(gson.fromJson(reader, ConfigRoot[].class));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config!");
        }
    }
}
