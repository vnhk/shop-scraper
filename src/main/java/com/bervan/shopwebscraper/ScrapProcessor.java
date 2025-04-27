package com.bervan.shopwebscraper;

import com.bervan.shopwebscraper.scrapers.Scraper;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ScrapProcessor {
    @Autowired
    private TaskExecutor taskExecutor;

    private final Map<String, Scraper> scrapers;
    private final ResourceLoader resourceLoader;
    @Value("${logs.path}")
    private String path = "";
    private final Jackson2JsonMessageConverter messageConverter;

    public ScrapProcessor(Map<String, Scraper> scrapers, ResourceLoader resourceLoader,
                          Jackson2JsonMessageConverter messageConverter) {
        this.scrapers = scrapers;
        this.resourceLoader = resourceLoader;
        this.messageConverter = messageConverter;
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

    @RabbitListener(queues = "SCRAPER_QUEUE", ackMode = "MANUAL")
    public void processMessage(Message message, Channel channel) throws IOException {
        try {
            ScrapContext scrapContext = (ScrapContext) messageConverter.fromMessage(message);
            String shopName = scrapContext.getRoot().getShopName();

            Future<?> future = ((ExecutorService) taskExecutor).submit(() -> {
                scrapers.get(shopName).runOne(scrapContext);
            });

            try {
                future.get(1, TimeUnit.HOURS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.error("Scraping took too long and was cancelled", e);
            }

        } catch (Exception e) {
            log.error("Failed to process product!", e);
            log.error(e.getMessage());
        } finally {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
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
