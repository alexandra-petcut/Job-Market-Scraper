package com.alexandrapetcut.jobmarket.scraper;

import java.util.List;

public interface JobScraper {
    String source();
    List<Job> scrape(int limit) throws Exception;
}
