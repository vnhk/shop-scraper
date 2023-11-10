package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.StatServerService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsoleJsonLoaderStarter {

    public static void main(String[] args) {
        JsonService service = new JsonService();
        StatServerService statServerService = new StatServerService();
        List<Offer> load = new ArrayList<>();
        load.addAll(service.load("products_shop_scrap-2023-11-05.json"));
        load.addAll(service.load("products_shop_scrap-2023-11-06.json"));
        load.addAll(service.load("products_shop_scrap-2023-11-07.json"));
        load.addAll(service.load("products_shop_scrap-2023-11-08.json"));
        load.addAll(service.load("products_shop_scrap-2023-11-09.json"));
        load.addAll(service.load("products_shop_scrap-2023-11-10.json"));

        for (Offer offer : load) {
            Object date = offer.get("Date");
            if (date != null) {
                offer.put("Date", new Date(((Double) date).longValue()));
            }
        }

        statServerService.save(load);
    }
}
