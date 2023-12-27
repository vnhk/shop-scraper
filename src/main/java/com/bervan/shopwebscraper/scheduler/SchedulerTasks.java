package com.bervan.shopwebscraper.scheduler;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.bervan.shopwebscraper.ScrapProcessor;
import com.bervan.shopwebscraper.save.StatServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SchedulerTasks {

    private final ScrapProcessor scrapProcessor;
    private final StatServerService statServerService;

    public SchedulerTasks(ScrapProcessor scrapProcessor, StatServerService statServerService) {
        this.scrapProcessor = scrapProcessor;
        this.statServerService = statServerService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void refreshView1() {
        try {
            statServerService.refreshViews();
        } catch (Exception e) {
            log.error("RefreshingViews: FAILED!", e);
        }
    }

    @Scheduled(cron = "0 10 15 * * *")
    public void refreshView2() {
        try {
            statServerService.refreshViews();
        } catch (Exception e) {
            log.error("RefreshingViews: FAILED!", e);
        }
    }

    @Scheduled(cron = "0 0 19 * * *")
    public void refreshView3() {
        try {
            statServerService.refreshViews();
        } catch (Exception e) {
            log.error("RefreshingViews: FAILED!", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scrapRTVEuroAGD() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("RTV Euro AGD Scraping: STARTED!");
        try {
            LocalDateTime now = LocalDateTime.now();
            scrapProcessor.run(true, "config.json", now.getHour(), "RTV Euro AGD", "Morele", "Media Expert");
            statServerService.refreshViews();
            log.info("RTV Euro AGD Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("RTV Euro AGD Scraping: FAILED!", e);
        }
    }
}
