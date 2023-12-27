package com.bervan.shopwebscraper;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/scraper")
public class ScrapController {

    private final ScrapProcessor scrapProcessor;

    public ScrapController(ScrapProcessor scrapProcessor) {
        this.scrapProcessor = scrapProcessor;
    }

    @PostMapping("/scrap")
    public ResponseEntity<List<String>> scrap(@RequestParam String shopNames) {
        String[] shops = shopNames.split(",");
        scrapProcessor.run(true, "config.json", LocalDateTime.now().getHour(), shops);

        return ResponseEntity.of(Optional.of(Arrays.asList("No messages configured yet...")));
    }
}
