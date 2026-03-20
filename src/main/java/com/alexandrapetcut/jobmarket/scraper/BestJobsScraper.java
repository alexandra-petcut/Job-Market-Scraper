package com.alexandrapetcut.jobmarket.scraper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import java.util.ArrayList;
import java.util.List;

public class BestJobsScraper implements JobScraper {
    @Override
    public String source() {
        return "bestjobs";
    }

    @Override
    public List<Job> scrape(int targetCount) throws Exception {
        int batchSize = 24;
        int collectedItems = 0;
        String cursor = null;

        List<Job> results = new ArrayList<>();

        while (collectedItems < targetCount) {

            String url = "https://www.bestjobs.eu/api/proxy/v2/jobs?limit=" + batchSize + "&_lat=46.771210&_lon=23.623635";
            if (cursor != null) {
                url = "https://www.bestjobs.eu/api/proxy/v2/jobs?limit=" + batchSize + "&cursor=" + cursor + "&_lat=46.771210&_lon=23.623635";
            }

            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .execute()
                    .body();

            JSONObject obj = new JSONObject(json);
            JSONArray items = obj.getJSONArray("items");

            if (items == null || items.length() == 0) {
                break;
            }

            for (int i = 0; i < items.length() && collectedItems < targetCount; i++) {

                JSONObject jobObj = items.getJSONObject(i);
                List<Job> jobsFromItem = extractJobDetails(jobObj);
                results.addAll(jobsFromItem);
                collectedItems++;
            }

            if(obj.has("nextCursor")){
                cursor = obj.getString("nextCursor");
            }
            else{
                System.out.println("No more available jobs!");
                break;
            }
        }

        return results;

    }

    private List<Job> extractJobDetails(JSONObject jobObj) {
        List<Job> jobs = new ArrayList<>();

        String title = jobObj.optString("title", "Unknown");
        String company = jobObj.optString("companyName", "Unknown");
        String slug = jobObj.optString("slug", "");
        String url = "https://www.bestjobs.eu/loc-de-munca/" + slug;

        // Use slug as external id
        String externalId = !slug.isEmpty() ? slug : url;

        String salary = jobObj.optString("salary", "");
        String estimated = jobObj.optString("estimatedSalary", "");

        if (salary.isEmpty()) {
            salary = estimated.isEmpty() ? "Confidential" : estimated + " EUR (estimated)";
        } else {
                salary = salary + " EUR";
        }

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
            String locName = (loc != null) ? loc.optString("name", "Unknown") : "Unknown";

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