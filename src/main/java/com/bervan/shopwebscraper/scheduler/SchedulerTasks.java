package com.bervan.shopwebscraper.scheduler;

import ch.qos.logback.core.testUtil.RandomUtil;
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

    @Scheduled(cron = "0 0 4 * * *")
    public void scrapMediaExpert() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("Media Expert Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Media Expert");
            log.info("Media Expert Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Media Expert Scraping: FAILED!");
        }
    }

    @Scheduled(cron = "0 0 11 * * *")
    public void scrapMorele() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("Morele Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Morele");
            log.info("Morele Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Morele Scraping: FAILED!");
        }
    }

    @Scheduled(cron = "0 0 17 * * *")
    public void scrapRTVEuroAGD() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("RTV Euro AGD Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "RTV Euro AGD");
            log.info("RTV Euro AGD Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("RTV Euro AGD Scraping: FAILED!");
        }
    }
}
