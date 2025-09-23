package com.github.hryhoriishtanko.techstarsscraper.controller;

import com.github.hryhoriishtanko.techstarsscraper.service.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/scrape")
@RequiredArgsConstructor
public class ScraperController {

    private final ScraperService scraperService;

    @PostMapping
    public ResponseEntity<String> startScraping(@RequestBody Map<String, String> payload) {
        String jobFunction = payload.get("jobFunction");
        if (jobFunction == null || jobFunction.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("'jobFunction' field is required.");
        }

        CompletableFuture.runAsync(() -> scraperService.scrapeJobsByFunction(jobFunction));
        
        return ResponseEntity.ok("Scraping process started for job function: '" + jobFunction + "'. Check logs for progress.");
    }
}
