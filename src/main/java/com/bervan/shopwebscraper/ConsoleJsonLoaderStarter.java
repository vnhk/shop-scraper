package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.JsonService;
import com.bervan.shopwebscraper.save.SavingOffersToDBException;
import com.bervan.shopwebscraper.save.StatServerService;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ConsoleJsonLoaderStarter {

    public static void main(String[] args) {
        loadDir("scrap_files");
    }

    private static void updateDate(List<Offer> load) {
        for (Offer offer : load) {
            Object date = offer.get("Date");
            if (date != null) {
                offer.put("Date", new Date(((Double) date).longValue()));
            }
        }
    }

    private static void loadDir(String dirName) {
        StatServerService statServerService = new StatServerService();
        statServerService.setSendToQueue(true);
        JsonService service = new JsonService();
        File folder = new File("./" + dirName);
        Set<File> listOfFiles = Arrays.stream(Objects.requireNonNull(folder.listFiles()))
                .filter(File::isFile)
                .filter(f -> f.getName().endsWith(".json")).collect(Collectors.toSet());
        for (File file : listOfFiles) {
            List<Offer> load = service.load("./" + dirName + "/" + file.getName());
            updateDate(load);
            try {
                statServerService.save(load);
            } catch (SavingOffersToDBException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
