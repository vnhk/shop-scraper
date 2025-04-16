package com.bervan.shopwebscraper.scheduler;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.bervan.shopwebscraper.ScrapProcessor;
import com.bervan.shopwebscraper.save.StatServerService;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SchedulerTasks {

    private final ScrapProcessor scrapProcessor;
    private final StatServerService statServerService;

    @PostConstruct
    public void initDriver() {
        WebDriverManager.chromedriver()
                .setup();
    }

    public SchedulerTasks(ScrapProcessor scrapProcessor, StatServerService statServerService) {
        this.scrapProcessor = scrapProcessor;
        this.statServerService = statServerService;
    }

//    @Scheduled(cron = "0 0 3 * * *")
//    public void refreshView1() {
//        try {
//            statServerService.refreshViews();
//        } catch (Exception e) {
//            log.error("RefreshingViews: FAILED!", e);
//        }
//    }
//
//    @Scheduled(cron = "0 0 * * * *")
//    public void refreshFavorites() throws InterruptedException {
//        Thread.sleep(15000 + RandomUtil.getPositiveInt() % 15000);
//        try {
//            statServerService.refreshFavorites();
//        } catch (Exception e) {
//            log.error("RefreshingViews: FAILED!", e);
//        }
//    }

    @Scheduled(cron = "* * * * * *")
    public void scrap() throws InterruptedException {
        Thread.sleep(RandomUtil.getPositiveInt() % 15000);
        log.info("Scraping: STARTED!");
        try {
            LocalDateTime now = LocalDateTime.now();
            scrapProcessor.run(true, "config.json", now.getHour(), "RTV Euro AGD", "Morele", "Media Expert");
            log.info("Scraping: COMPLETED!");
        } catch (Exception e) {
            log.error("Scraping: FAILED!", e);
        }
    }
}
