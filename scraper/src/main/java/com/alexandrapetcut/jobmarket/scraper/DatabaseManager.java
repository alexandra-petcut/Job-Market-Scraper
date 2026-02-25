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
        // if row already exists with the same unique key, update row
        String sql = """
            INSERT INTO jobs (title, company, location, url, salary, source, external_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                company = VALUES(company),
                location = VALUES(location),
                url = VALUES(url),
                salary = VALUES(salary),
                scraped_at = CURRENT_TIMESTAMP
            """;
    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setString(1, job.title);
    stmt.setString(2, job.company);
    stmt.setString(3, job.location);
    stmt.setString(4, job.url);
    stmt.setString(5, job.salary);
    stmt.setString(6, job.source);
    stmt.setString(7, job.externalId);
    stmt.executeUpdate();
}
}
