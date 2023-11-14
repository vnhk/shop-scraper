package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.SavingOffersToDBException;
import com.bervan.shopwebscraper.save.StatServerService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConsoleJsonLoaderStarter {

    public static void main(String[] args) {
        JsonService service = new JsonService();
        StatServerService statServerService = new StatServerService();
        List<Offer> load = new ArrayList<>();
//        load.addAll(service.load("products_shop_scrap-2023-11-05.json"));

        for (Offer offer : load) {
            Object date = offer.get("Date");
            if (date != null) {
                offer.put("Date", new Date(((Double) date).longValue()));
            }
        }

        try {
            statServerService.save(load);
        } catch (SavingOffersToDBException e) {
            System.err.println(e.getMessage());
        }
    }
}
