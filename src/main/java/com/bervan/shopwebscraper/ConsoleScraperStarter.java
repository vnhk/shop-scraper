package com.bervan.shopwebscraper;

public class ConsoleScraperStarter {

    public static void main(String[] args) {
//        JsonService service = new JsonService();
//        ExcelService excelService = new ExcelService();
//        StatServerService statServerService = new StatServerService();
//        Map<String, Scraper> scrapers = new HashMap<>();
//        scrapers.put("Media Markt", new MediaMarktScraper(service, excelService, statServerService));
//        scrapers.put("Media Expert", new MediaExpertScraper(service, excelService, statServerService));
//        scrapers.put("RTV Euro AGD", new RTVEuroAGDScraper(service, excelService, statServerService));
//        ScrapProcessor scraper = new ScrapProcessor(scrapers, resourceLoader);
//        scrapAll(scraper);
//        scrapOnlyRTV(scraper);
//        scrapOnlyMediaExpert(scraper);
    }

    private static void scrapAll(ScrapProcessor scraper) {
        scraper.run(true, "config.json", "Media Expert", "RTV Euro AGD");
    }

    private static void scrapOnlyRTV(ScrapProcessor scraper) {
        scraper.run(false, "config.json", "RTV Euro AGD");
    }

    private static void scrapOnlyMediaExpert(ScrapProcessor scraper) {
        scraper.run(false, "config.json", "Media Expert");
    }
}
