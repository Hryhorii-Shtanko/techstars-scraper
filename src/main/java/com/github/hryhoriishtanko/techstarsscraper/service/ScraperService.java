package com.github.hryhoriishtanko.techstarsscraper.service;

import com.github.hryhoriishtanko.techstarsscraper.entity.Job;
import com.github.hryhoriishtanko.techstarsscraper.repository.JobRepository;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperService {

    private final JobRepository jobRepository;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Value("${app.scraper.base-url}")
    private String baseUrl;

    @Value("${app.scraper.target-function}")
    private String targetJobFunction;

    @PostConstruct
    public void startScraping() {
        log.info("Starting to scrape '{}' jobs...", targetJobFunction);
        scrapeJobsByFunction(targetJobFunction);
    }

    public void scrapeJobsByFunction(String jobFunction) {
    int page = 1;
    int totalJobsFound = 0;

    while (true) {
        String url = baseUrl + "?functions=" + jobFunction.replace(" ", "+") + "&page=" + page;
        log.info("Scraping page {} for job function '{}'", page, jobFunction);
        
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36")
                    .get();

            Element scriptTag = doc.selectFirst("script[id=__NEXT_DATA__]");
            if (scriptTag == null) {
                log.error("Could not find script tag on page {}", page);
                break;
            }

            String jsonData = scriptTag.html();
            JsonObject rootObject = JsonParser.parseString(jsonData).getAsJsonObject();
            JsonArray jobsArray = rootObject.getAsJsonObject("props")
                                            .getAsJsonObject("pageProps")
                                            .getAsJsonObject("initialState")
                                            .getAsJsonObject("jobs")
                                            .getAsJsonArray("found");
            
            if (jobsArray.isEmpty()) {
                log.info("No more jobs found on page {}. Finishing pagination.", page);
                break;
            }

            log.info("Found {} jobs on page {}. Submitting for processing...", jobsArray.size(), page);
            totalJobsFound += jobsArray.size();

            for (var jobElement : jobsArray) {
                JsonObject jobJson = jobElement.getAsJsonObject();
                CompletableFuture.runAsync(() -> saveJobFromJson(jobJson, jobFunction), taskExecutor);
            }
            
            page++; 

        } catch (IOException e) {
            log.error("Failed to fetch page {}: {}", page, url, e);
            break;
        }
    }
    log.info("Finished scraping for '{}'. Total jobs found: {}", jobFunction, totalJobsFound);
}

    private void saveJobFromJson(JsonObject jobJson, String laborFunction) {
        String jobUrl = jobJson.get("url").getAsString();

        if (jobRepository.findByJobPageUrl(jobUrl).isPresent()) {
            log.info("Skipping already existing job: {}", jobUrl);
            return;
        }

        try {
            Job job = new Job();
            job.setJobPageUrl(jobUrl);
            job.setLaborFunction(laborFunction);
            job.setPositionName(jobJson.get("title").getAsString());

            JsonObject orgJson = jobJson.getAsJsonObject("organization");
            job.setOrganizationTitle(orgJson.get("name").getAsString());
            if (orgJson.has("logoUrl") && !orgJson.get("logoUrl").isJsonNull()) {
                job.setLogoUrl(orgJson.get("logoUrl").getAsString());
            }

            // Combine locations into a single string
            List<String> locations = new ArrayList<>();
            if (jobJson.has("locations") && jobJson.get("locations").isJsonArray()) {
                jobJson.getAsJsonArray("locations").forEach(loc -> locations.add(loc.getAsString()));
            }
            job.setLocation(String.join(", ", locations));

            job.setPostedDateTimestamp(jobJson.get("createdAt").getAsLong());

            // Get skills as tags
            List<String> tags = new ArrayList<>();
            if (jobJson.has("skills") && jobJson.get("skills").isJsonArray()) {
                jobJson.getAsJsonArray("skills").forEach(skill -> tags.add(skill.getAsString()));
            }
            job.setTags(tags);

            // Description is not available in the main JSON, so we leave it empty
            job.setDescriptionHtml("");

            jobRepository.save(job);
            log.info("SUCCESSFULLY SAVED JOB: {}", job.getPositionName());

        } catch (Exception e) {
            log.error("Failed to save job from JSON for URL {}. Error: {}", jobUrl, e.getMessage());
        }
    }

}
