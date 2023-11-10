package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.save.StatServerService;
import com.bervan.shopwebscraper.save.ExcelService;
import com.bervan.shopwebscraper.save.JsonService;

public class ConsoleScraperStarter {

    public static void main(String[] args) {
        JsonService service = new JsonService();
        ExcelService excelService = new ExcelService();
        StatServerService statServerService = new StatServerService();
        Scraper scraper = new Scraper(service, excelService, statServerService);
        scraper.run();
    }
}
