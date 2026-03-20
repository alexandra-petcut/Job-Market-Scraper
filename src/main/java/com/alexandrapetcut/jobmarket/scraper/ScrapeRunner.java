package com.alexandrapetcut.jobmarket.scraper;

import java.util.List;

public class ScrapeRunner {
    private final DatabaseManager db;

    public ScrapeRunner(DatabaseManager db) {
        this.db = db;
    }

    public void runAll(List<JobScraper> scrapers, int limitPerSite) {
        for(JobScraper scraper : scrapers) {
            System.out.println("=== Running: " + scraper.source() + " ===");
            try {
                List<Job> jobs = scraper.scrape(limitPerSite);

                for(Job j : jobs) {
                    j.source = scraper.source();
                    db.save(j);
                }
                System.out.println("Saved/updated " + jobs.size() + " jobs from " + scraper.source());
            } catch (Exception e) {
                System.err.println("Scraper failed: " + scraper.source());
                e.printStackTrace();
            }
        }
    }
}
