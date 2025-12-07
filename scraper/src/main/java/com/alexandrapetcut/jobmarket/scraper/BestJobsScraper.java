package com.alexandrapetcut.jobmarket.scraper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BestJobsScraper {
    private static final String BASE_URL= "https://www.bestjobs.eu";

    public void scrapeAndSave(DatabaseManager db){
        try{
            int targetCount = 100;
            int batchSize = 24;
            int collected = 0;
            String cursor = null;

            // https://www.bestjobs.eu/api/proxy/v2/jobs?limit=24&cursor=eyJ0eXBlIjoib2Zmc2V0IiwicGFnZSI6Mywic2l6ZSI6MjQsIm9mZnNldCI6NDh9&_lat=46.771210&_lon=23.623635

            while (collected < targetCount) {

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

                for (int i = 0; i < items.length(); i++) {

                    JSONObject jobObj = items.getJSONObject(i);
                    String slug = jobObj.getString("slug");
                    String jobUrl = "https://www.bestjobs.eu/loc-de-munca/" + slug;

                    Job job = extractJobDetails(jobUrl);

                    //db.save(job);
                    System.out.println("Titile: " + job.title);
                    System.out.println("Company: " + job.company);
                    System.out.println("Location: " + job.location);
                    System.out.println("url: " + job.url);
                    System.out.println("job type: " + job.jobType);
                    System.out.println("Salary: " + job.salary);

                    collected++;
                    if (collected >= targetCount)
                        break;
                }

                if(obj.has("nextCursor")){
                    cursor = obj.getString("nextCursor");
                }
                else{
                    System.out.println("No more available jobs!");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        try{

            String url = BASE_URL + "/locuri-de-munca";
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            Elements jobs = doc.select("div.flex.flex-col.transition-all");
            System.out.println("Found jobs: " + jobs.size());

            for (Element job : jobs){
                Job j = new Job();

                j.title = job.select("h2").text();

                j.company = job.select(".line-clamp-1.text-ink-medium").text();

                Element locEl = job.selectFirst("a[href*='locuri-de-munca-in-']");
                j.location = (locEl != null) ? locEl.text() : "Not specified";

                String relativeUrl = job.select("a[href]").attr("href");
                j.url = BASE_URL + relativeUrl;

                Element salaryEl = job.selectFirst("div.flex.items-center.tracking-wider");
                if (salaryEl != null && salaryEl.select("a").isEmpty()) {
                    String salaryText = salaryEl.ownText();
                    if (salaryText.contains("(Estimare)")) {
                        int idx = salaryText.indexOf("(Estimare)");
                        String range = salaryText.substring(0, idx).trim();           // "1410 - 1560"
                        String estimation = salaryText.substring(idx).trim();         // "(Estimare)"
                        j.salary = range + " € " + estimation;
                    }
                    else {
                        j.salary = salaryEl.ownText().replace("\"", "").trim() + " €";
                    }
                } else {
                    j.salary = "Salariu confidential";
                }

                try {
                    Document jobDoc = Jsoup.connect(j.url)
                            .userAgent("Mozilla/5.0")
                            .timeout(10_000)
                            .get();

                    Element infoDiv = jobDoc.selectFirst("div.mt-8.space-y-2.text-sm.text-ink-medium");

                    if (infoDiv != null) {
                        Element typeDiv = infoDiv.selectFirst("div.ml-6");
                        if (typeDiv != null) {
                            String fullText = typeDiv.text();
                            String jobType = fullText.split(";")[0].trim();
                            j.jobType = jobType;
                        } else {
                            j.jobType = "Not specified";
                        }
                    } else {
                        j.jobType = "Not specified";
                    }

                } catch (Exception e) {
                    j.jobType = "Not specified";
                }

                //db.save(j);
                //System.out.println("Saved: " + j.title);
                System.out.println("Titile: " + j.title);
                System.out.println("Company: " + j.company);
                System.out.println("Location: " + j.location);
                System.out.println("url: " + j.url);
                System.out.println("job type: " + j.jobType);
                System.out.println("Salary: " + j.salary);

            }
            System.out.println("Scraping BestJobs completed!");

        } catch (Exception e) {
            e.printStackTrace();
        }
         */
    }

    private Job extractJobDetails(String url) {
        Job j = new Job();
        j.url = url;

        /*
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            j.title = doc.selectFirst("h1").text();

            j.company = doc.selectFirst(".line-clamp-1.text-ink-medium").text();

            Element locEl = doc.selectFirst("a[href*='locuri-de-munca-in-']");
            j.location = (locEl != null) ? locEl.text() : "Not specified";

            // Salary logic (yours)
            Element salaryEl = doc.selectFirst("div.flex.items-center.tracking-wider");
            if (salaryEl != null && salaryEl.select("a").isEmpty()) {
                String salaryText = salaryEl.ownText();
                if (salaryText.contains("(Estimare)")) {
                    int idx = salaryText.indexOf("(Estimare)");
                    String range = salaryText.substring(0, idx).trim();
                    String estimation = salaryText.substring(idx).trim();
                    j.salary = range + " € " + estimation;
                } else {
                    j.salary = salaryEl.ownText().trim() + " €";
                }
            } else {
                j.salary = "Confidential";
            }

            // Job type
            Element infoDiv = doc.selectFirst("div.mt-8.space-y-2.text-sm.text-ink-medium");
            if (infoDiv != null) {
                Element typeDiv = infoDiv.selectFirst("div.ml-6");
                j.jobType = (typeDiv != null)
                        ? typeDiv.text().split(";")[0].trim()
                        : "Not specified";
            } else {
                j.jobType = "Not specified";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        return j;
    }

}
