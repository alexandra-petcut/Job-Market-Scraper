package com.alexandrapetcut.jobmarket.scraper;

import java.sql.*;

public class DatabaseManager {
    private Connection conn;

    public DatabaseManager() throws Exception{
        conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/job_scraper",
                "root",
                "rootpassword");
    }

    public void save(Job job) throws Exception{
        String sql = "INSERT INTO jobs (title, company, location, url, salary, job_type) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, job.title);
        stmt.setString(2, job.company);
        stmt.setString(3, job.location);
        stmt.setString(4, job.url);
        stmt.setString(5, job.salary);
        stmt.setString(6, job.jobType);
        stmt.executeUpdate();
    }
}
