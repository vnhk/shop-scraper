package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.scrapers.Scraper;
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
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class ScrapProcessor {
    private final Map<String, Scraper> scrapers;
    private final ResourceLoader resourceLoader;
    private final ExecutorService executor;
    @Value("${logs.path}")
    private String path = "";
    private final Jackson2JsonMessageConverter messageConverter;

    public ScrapProcessor(Map<String, Scraper> scrapers, ResourceLoader resourceLoader,
                          Jackson2JsonMessageConverter messageConverter) {
        this.scrapers = scrapers;
        this.resourceLoader = resourceLoader;
        this.messageConverter = messageConverter;
        this.executor = Executors.newSingleThreadExecutor();

    }

    public void addToQueue(String configFilePath, Integer hour, String... shops) {
        Date now = new Date();
        List<ConfigRoot> roots = loadProductsFromConfig(configFilePath);

        for (ConfigRoot root : roots) {
            String shopName = root.getShopName();
            if (Arrays.asList(shops).contains(shopName)) {
                Scraper scraper = scrapers.get(shopName);
                if (scraper == null) {
                    throw new RuntimeException("Scraper not found for given shop: " + shopName);
                }
                scraper.addToQueue(root, now, hour);
            }
        }
    }

    @RabbitListener(queues = "SCRAPER_QUEUE", ackMode = "MANUAL", concurrency = "4")
    public void processMessage(Message message, Channel channel) throws IOException {
        try {
            ScrapContext scrapContext = (ScrapContext) messageConverter.fromMessage(message);
            LogUtils.info(log, scrapContext, "Scraping process message started.");
            String shopName = scrapContext.getRoot().getShopName();

            Future<?> future = executor.submit(() -> {
                scrapers.get(shopName).runOne(scrapContext);
            });

            try {
                future.get(1, TimeUnit.HOURS);
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
