package com.bervan.shopwebscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShopWebscraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopWebscraperApplication.class, args);
    }

}
