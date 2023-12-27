package com.bervan.shopwebscraper.scheduler;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.bervan.shopwebscraper.ScrapProcessor;
import com.bervan.shopwebscraper.save.StatServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SchedulerTasks {

    private final ScrapProcessor scrapProcessor;
    private final StatServerService statServerService;

    public SchedulerTasks(ScrapProcessor scrapProcessor, StatServerService statServerService) {
        this.scrapProcessor = scrapProcessor;
        this.statServerService = statServerService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void scrapMediaExpert() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("Media Expert Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Media Expert");
            statServerService.refreshViews();
            log.info("Media Expert Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Media Expert Scraping: FAILED!", e);
        }
    }

    @Scheduled(cron = "0 10 11 * * *")
    public void scrapMorele() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("Morele Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "Morele");
            statServerService.refreshViews();
            log.info("Morele Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Morele Scraping: FAILED!", e);
        }
    }

    @Scheduled(cron = "0 0 17 * * *")
    public void scrapRTVEuroAGD() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("RTV Euro AGD Scraping: STARTED!");
        try {
            scrapProcessor.run(true, "config.json", "RTV Euro AGD");
            statServerService.refreshViews();
            log.info("RTV Euro AGD Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("RTV Euro AGD Scraping: FAILED!", e);
        }
    }
}
