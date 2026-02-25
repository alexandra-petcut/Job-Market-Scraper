package com.alexandrapetcut.jobmarket.scraper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Map;

public class EJobsStaticData {

    private static final String STATICS_URL = "https://api.ejobs.ro/all-statics";

    private final Map<Integer, String> cities = new HashMap<>();

    public EJobsStaticData() throws Exception {
        load();
    }

    private void load() throws Exception {
        String json = Jsoup.connect(STATICS_URL)
                .ignoreContentType(true)
                .userAgent("Mozilla/5.0")
                .timeout(15000)
                .execute()
                .body();

        JSONObject root = new JSONObject(json);
        JSONObject locale = root.getJSONObject("ro");
        JSONArray citiesArr = locale.getJSONArray("cities");

        for (int i = 0; i < citiesArr.length(); i++) {
            JSONObject obj = citiesArr.getJSONObject(i);
            cities.put(obj.getInt("id"), obj.getString("name"));
        }

        System.out.println("Loaded " + cities.size() + " eJobs cities");
    }

    public String cityName(int id) {
        return cities.getOrDefault(id, "Unknown");
    }
}
