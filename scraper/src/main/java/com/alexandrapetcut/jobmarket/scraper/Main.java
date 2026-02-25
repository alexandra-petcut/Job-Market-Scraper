package com.alexandrapetcut.jobmarket.scraper;

import java.util.List;

public class Main {
    public static void main(String[] args){
        try {
            DatabaseManager db = new DatabaseManager();
            System.out.println("Connected to MySQL successfully!");

            // Load eJobs city lookup data once (maps cityId -> city name)
            EJobsStaticData ejobsStatics = new EJobsStaticData();

            List<JobScraper> scrapers = List.of(
                    new BestJobsScraper(),
                    new EJobsScraper(ejobsStatics),
                    new HipoScraper()
            );
            ScrapeRunner sr = new ScrapeRunner(db);
            sr.runAll(scrapers, 100);

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
