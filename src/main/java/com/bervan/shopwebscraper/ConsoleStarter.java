package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.file.ExcelService;
import com.bervan.shopwebscraper.file.JsonService;

public class ConsoleStarter {

    public static void main(String[] args) {
        JsonService service = new JsonService();
        ExcelService excelService = new ExcelService();
        Scraper scraper = new Scraper(service, excelService);
        scraper.run();
    }
}
