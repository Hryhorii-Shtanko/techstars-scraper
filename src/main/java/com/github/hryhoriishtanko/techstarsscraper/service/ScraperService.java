package com.github.hryhoriishtanko.techstarsscraper.service;

import com.github.hryhoriishtanko.techstarsscraper.entity.Job;
import com.github.hryhoriishtanko.techstarsscraper.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private static final String BASE_URL = "https://jobs.techstars.com/jobs";
    private static final String TARGET_JOB_FUNCTION = "Software Engineering";

    @PostConstruct
    public void startScraping() {
        log.info("Starting to scrape '{}' jobs...", TARGET_JOB_FUNCTION);
        scrapeJobsByFunction(TARGET_JOB_FUNCTION);
    }

    public void scrapeJobsByFunction(String jobFunction) {
        String url = BASE_URL + "?functions=" + jobFunction.replace(" ", "+");

        try {
            Document doc = Jsoup.connect(url).get();
            Elements jobElements = doc.select("li.job");
            log.info("Found {} jobs on the list page.", jobElements.size());

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Element jobElement : jobElements) {
                String initialJobPageUrl = jobElement.select("a").first().absUrl("href");

                // Execute each job parsing in a separate thread
                CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    parseJobPage(initialJobPageUrl, jobFunction), taskExecutor
                );
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.info("Scraping for '{}' completed.", jobFunction);

        } catch (IOException e) {
            log.error("Failed to fetch main job list page: {}", url, e);
        }
    }

    private void parseJobPage(String jobUrl, String jobFunction) {
        try {
            // Check if job already exists to avoid re-parsing
            if (jobRepository.findByJobPageUrl(jobUrl).isPresent()) {
                log.info("Skipping already existing job: {}", jobUrl);
                return;
            }

            Connection.Response response = Jsoup.connect(jobUrl).execute();
            String finalUrl = response.url().toString();

            // Critical: Skip jobs that redirect to external sites
            if (!finalUrl.startsWith(BASE_URL)) {
                log.warn("Skipping job due to redirect: {} -> {}", jobUrl, finalUrl);
                return;
            }

            Document jobDoc = response.parse();
            Job job = buildJob(jobDoc, finalUrl, jobFunction);
            jobRepository.save(job);
            log.info("Saved job: {}", job.getPositionName());

        } catch (IOException e) {
            log.error("Error processing job page: {}", jobUrl, e);
        }
    }

    private Job buildJob(Document doc, String url, String function) {
        Job job = new Job();
        job.setJobPageUrl(url);
        job.setLaborFunction(function);

        job.setPositionName(doc.selectFirst("h1.heading-2").text());
        Element companyLink = doc.selectFirst("a[href*='/companies/']");
        job.setOrganizationTitle(companyLink.text());
        job.setOrganizationUrl(companyLink.absUrl("href"));
        job.setLogoUrl(companyLink.selectFirst("img").absUrl("src"));
        job.setLocation(doc.selectFirst("div.flex.items-center.text-lg > div").text());
        String postedDateText = doc.selectFirst("span.text-gray-600").text().replace("Posted ", "");
        job.setPostedDateTimestamp(parseDateToTimestamp(postedDateText));
        job.setDescriptionHtml(doc.selectFirst("div.prose").html());
        job.setTags(doc.select("div.mt-4 > span.inline-block").eachText());

        return job;
    }

    private long parseDateToTimestamp(String dateString) {
        if (dateString.equalsIgnoreCase("Today")) {
            return LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
        }
        if (dateString.equalsIgnoreCase("Yesterday")) {
            return LocalDate.now().minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
        }
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("MMMM d, yyyy"))
                    .atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond();
        } catch (Exception e) {
            log.warn("Could not parse date: '{}'. Using current timestamp.", dateString);
            return Instant.now().getEpochSecond();
        }
    }
}