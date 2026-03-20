package com.alexandrapetcut.jobmarket.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HipoScraper implements JobScraper {

    private static final String BASE_URL = "https://www.hipo.ro/locuri-de-munca/cautajob/";
    private static final Pattern ID_PATTERN = Pattern.compile("/locuri_de_munca/(\\d+)/");

    @Override
    public String source() {
        return "hipo";
    }

    @Override
    public List<Job> scrape(int targetCount) throws Exception {
        List<Job> results = new ArrayList<>();
        int page = 1;

        while (results.size() < targetCount) {
            Document doc = Jsoup.connect(BASE_URL + page)
                    .userAgent("Mozilla/5.0")
                    .timeout(15000)
                    .get();

            Elements cards = doc.select("div.job-item");

            if (cards.isEmpty()) break;

            for (Element card : cards) {
                if (results.size() >= targetCount) break;

                List<Job> jobs = parseCard(card);
                results.addAll(jobs);
            }

            // Check if there's a next page link
            Element nextPage = doc.selectFirst("a.page-next");
            if (nextPage == null) {
                System.out.println("No more available jobs!");
                break;
            }
            page++;
        }

        return results;
    }

    private List<Job> parseCard(Element card) {
        List<Job> jobs = new ArrayList<>();

        // Title and URL from the job-title link
        Element titleLink = card.selectFirst("a.job-title");
        if (titleLink == null) return jobs;

        String title = titleLink.text().trim();
        String href = titleLink.attr("href");
        String url = "https://www.hipo.ro" + href;

        // Extract numeric ID from URL: /locuri_de_munca/264847/...
        String externalId = href;
        Matcher m = ID_PATTERN.matcher(href);
        if (m.find()) {
            externalId = m.group(1);
        }

        // Company name
        Element companyEl = card.selectFirst("p.company-name span");
        String company = (companyEl != null) ? companyEl.text().trim() : "Unknown";

        // Salary - parent span of the fa-money-bill-alt icon
        String salary = "Confidential";
        Element salaryIcon = card.selectFirst("i.fa-money-bill-alt");
        if (salaryIcon != null) {
            Element salarySpan = salaryIcon.parent();
            if (salarySpan != null) {
                salary = salarySpan.text().trim();
            }
        }

        // Location - comma-separated cities inside span.badge-type
        Element locationEl = card.selectFirst("span.badge-type");
        String locationText = (locationEl != null) ? locationEl.text().trim() : "Unknown";

        String[] locations = locationText.split(", ");

        for (String loc : locations) {
            Job j = new Job();
            j.title = title;
            j.company = company;
            j.url = url;
            j.salary = salary;
            j.location = loc;
            j.externalId = externalId;
            jobs.add(j);
        }

        if (jobs.isEmpty()) {
            Job j = new Job();
            j.title = title;
            j.company = company;
            j.url = url;
            j.salary = salary;
            j.location = "Unknown";
            j.externalId = externalId;
            jobs.add(j);
        }

        return jobs;
    }
}
