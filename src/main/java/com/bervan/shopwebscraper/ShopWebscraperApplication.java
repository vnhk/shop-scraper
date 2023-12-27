package com.bervan.shopwebscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
@EnableScheduling
public class ShopWebscraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopWebscraperApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.of(1, ChronoUnit.MINUTES))
                .setReadTimeout(Duration.of(1, ChronoUnit.HOURS))
                .build();
    }
}
