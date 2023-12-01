package com.bervan.shopwebscraper.scheduler;

import com.bervan.shopwebscraper.ScrapProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SchedulerTasks {

    private final ScrapProcessor scrapProcessor;

    public SchedulerTasks(ScrapProcessor scrapProcessor) {
        this.scrapProcessor = scrapProcessor;
    }

    @Scheduled(cron = "0 0 6 * * *")
    public void scrapMediaExpert() {
        log.info("Media Expert Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Media Expert");
            log.info("Media Expert Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Media Expert Scraping: FAILED!");
        }
    }

    @Scheduled(cron = "0 0 11 * * *")
    public void scrapMorele() {
        log.info("Morele Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Morele");
            log.info("Morele Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Morele Scraping: FAILED!");
        }
    }

    @Scheduled(cron = "0 0 15 * * *")
    public void scrapRTVEuroAGD() {
        log.info("RTV Euro AGD Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "RTV Euro AGD");
            log.info("RTV Euro AGD Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("RTV Euro AGD Scraping: FAILED!");
        }
    }
}
