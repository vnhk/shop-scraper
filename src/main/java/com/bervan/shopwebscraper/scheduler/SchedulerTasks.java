package com.bervan.shopwebscraper.scheduler;

import com.bervan.shopwebscraper.ScrapProcessor;
import com.bervan.shopwebscraper.save.QueueService;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class SchedulerTasks {

    private final ScrapProcessor scrapProcessor;
    private final QueueService queueService;
    private final String pathToDriver;

    @Value("${config.read.and.apply}")
    private Boolean doConfig;

    @PostConstruct
    public void initDriver() {
        if (pathToDriver.isBlank()) {
            WebDriverManager.chromedriver()
                    .setup();
        } else {
            System.setProperty("webdriver.chrome.driver", pathToDriver);
        }
    }

    public SchedulerTasks(ScrapProcessor scrapProcessor, QueueService queueService,
                          @Value("${path-to-driver}") String pathToDriver) {
        this.scrapProcessor = scrapProcessor;
        this.pathToDriver = pathToDriver;
        this.queueService = queueService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void refreshViews() {
        try {
            if (doConfig) {
                queueService.refreshViews();
            }
        } catch (Exception e) {
            log.error("RefreshingViews: FAILED!", e);
        }
    }
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

//    @Scheduled(cron = "0 0 * * * *")
//    public void scrapAddToQueue() throws InterruptedException {
//        try {
//            if (doConfig) {
//                LocalDateTime now = LocalDateTime.now();
//                scrapProcessor.addToQueue("config.json", now.getHour(), "RTV Euro AGD", "Morele", "Media Expert");
//            }
//        } catch (Exception e) {
//            log.error("scrapAddToQueue: FAILED!", e);
//        }
//    }
}
