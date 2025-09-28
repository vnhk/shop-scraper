package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.scrapers.Scraper;
import com.bervan.shstat.ConfigRoot;
import com.bervan.shstat.ScrapContext;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class ScrapProcessor {
    private final Map<String, Scraper> scrapers;
    private final ResourceLoader resourceLoader;
    private final ExecutorService executor;
    private final Jackson2JsonMessageConverter messageConverter;
    @Value("${logs.path}")
    private String path = "";

    public ScrapProcessor(Map<String, Scraper> scrapers, ResourceLoader resourceLoader,
                          Jackson2JsonMessageConverter messageConverter) {
        this.scrapers = scrapers;
        this.resourceLoader = resourceLoader;
        this.messageConverter = messageConverter;
        this.executor = Executors.newSingleThreadExecutor();

    }

    @RabbitListener(queues = "SCRAPER_QUEUE", ackMode = "MANUAL")
    public void processMessage(Message message, Channel channel) throws IOException {
        try {
            ScrapContext scrapContext = (ScrapContext) messageConverter.fromMessage(message);

            LocalDateTime scrapDateTime = LocalDateTime.ofInstant(scrapContext.getScrapDate().toInstant(), ZoneId.of("Europe/Warsaw"));
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Warsaw"));
            Duration timeDifference = Duration.between(scrapDateTime, now).abs();

            if (timeDifference.toHours() > 24) {
                log.info("Scraping manual ack - skipped, scrap date is more than 24 hours old/future! Scrap date: {}, Current time: {}, Difference: {} hours",
                        scrapDateTime, now, timeDifference.toHours());
                return;
            }


            LogUtils.info(log, scrapContext, "Scraping process message started.");
            String shopName = scrapContext.getRoot().getShopName();

            Future<?> future = executor.submit(() -> {
                scrapers.get(shopName).runOne(scrapContext);
            });

            try {
                future.get(2, TimeUnit.HOURS);
                LogUtils.info(log, scrapContext, "Scraping process message ended without problems.");
            } catch (TimeoutException e) {
                future.cancel(true);
                LogUtils.info(log, scrapContext, "Scraping process message took too long and was cancelled", e);
            }
        } catch (Throwable e) {
            log.error("Failed to process product!", e);
            log.error(e.getMessage());
        } finally {
            log.info("Scraping manual ack");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            log.info("Scraping manual ack - finished!");
        }
    }

    private List<ConfigRoot> loadProductsFromConfig(String configFilePath) {
        Resource resource = resourceLoader.getResource("classpath:" + configFilePath);
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(resource.getFile())) {
            return List.of(gson.fromJson(reader, ConfigRoot[].class));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config!");
        }
    }

    public List<String> getLogs(Integer linesFromEnd) {
        List<String> res = new ArrayList<>();
        File file = new File(path);
        int counter = 0;
        try (ReversedLinesFileReader object = new ReversedLinesFileReader(file)) {
            while (counter < linesFromEnd) {
                res.add(object.readLine());
                counter++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return res;
    }
}
