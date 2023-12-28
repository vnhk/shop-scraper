package com.bervan.shopwebscraper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @CrossOrigin("*")
    public ResponseEntity<List<String>> scrap(@RequestParam String shopNames, @RequestParam(required = false) Integer hour) {
        String[] shops = shopNames.split(",");
        scrapProcessor.run(true, "config.json", hour, shops);

        return ResponseEntity.of(Optional.of(Arrays.asList("No messages configured yet...")));
    }

    @GetMapping("/logs")
    @CrossOrigin("*")
    private ResponseEntity<List<String>> getLogs(@RequestParam Integer linesFromEnd) {
        return new ResponseEntity<>(scrapProcessor.getLogs(linesFromEnd), HttpStatus.OK);
    }
}
