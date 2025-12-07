package com.alexandrapetcut.jobmarket.scraper;

public class Main {
    public static void main(String[] args){
        try {
            DatabaseManager db = new DatabaseManager();
            System.out.println("Connected to MySQL successfully!");

            BestJobsScraper scraper = new BestJobsScraper();
            scraper.scrapeAndSave(db);


        } catch (Exception e){
            e.printStackTrace();
        }






    }
}
