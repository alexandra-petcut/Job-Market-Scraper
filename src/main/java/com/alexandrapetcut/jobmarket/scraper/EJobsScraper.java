package com.alexandrapetcut.jobmarket.scraper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;

public class EJobsScraper implements JobScraper {

    private final EJobsStaticData statics;

    public EJobsScraper(EJobsStaticData statics) {
        this.statics = statics;
    }

    @Override
    public String source() {
        return "ejobs";
    }

    @Override
    public List<Job> scrape(int targetCount) throws Exception {
        int collectedItems = 0;
        int page = 1;

        List<Job> results = new ArrayList<>();

        while (collectedItems < targetCount) {

            String url = "https://api.ejobs.ro/jobs?page=" + page + "&pageSize=40&sort=suitability";

            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .execute()
                    .body();

            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("jobs");

            if (items == null || items.length() == 0) {
                break;
            }

            for (int i = 0; i < items.length() && collectedItems < targetCount; i++) {

                JSONObject jobObj = items.getJSONObject(i);
                List<Job> jobsFromItem = extractJobDetails(jobObj);
                results.addAll(jobsFromItem);
                collectedItems++;
            }

            if (obj.getBoolean("morePagesFollow")) {
                page++;
            } else {
                System.out.println("No more available jobs!");
                break;
            }
        }
        return results;
    }

    private List<Job> extractJobDetails(JSONObject jobObj) {
        List<Job> jobs = new ArrayList<>();

        String id = jobObj.optString("id");
        String title = jobObj.optString("title", "Unknown");

        JSONObject companyObj = jobObj.optJSONObject("company");
        String company = companyObj != null
                ? companyObj.optString("name", "Unknown")
                : "Unknown";

        String slug = jobObj.optString("slug", "");
        String url = "https://www.ejobs.ro/user/locuri-de-munca/" + slug + "/" + id;

        // Use slug as external id
        String externalId = !slug.isEmpty() ? slug : url;

        String salary = jobObj.optString("salary", "Confidential");

        // Locations contain cityId (int), resolve to city name via /all-statics
        JSONArray locations = jobObj.optJSONArray("locations");

        if (locations == null || locations.length() == 0) {
            Job j = new Job();
            j.title = title;
            j.company = company;
            j.url = url;
            j.salary = salary;
            j.location = "Unknown";
            j.externalId = externalId;
            jobs.add(j);
            return jobs;
        }

        for (int idx = 0; idx < locations.length(); idx++) {
            JSONObject loc = locations.optJSONObject(idx);
            String locName;
            if (loc != null && loc.has("cityId")) {
                locName = statics.cityName(loc.getInt("cityId"));
            } else {
                locName = "Unknown";
            }

            Job j = new Job();
            j.title = title;
            j.company = company;
            j.url = url;
            j.salary = salary;
            j.location = locName;
            j.externalId = externalId;
            jobs.add(j);
        }

        return jobs;
    }
}
