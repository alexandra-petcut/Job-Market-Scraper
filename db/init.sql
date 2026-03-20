USE job_scraper;

CREATE TABLE IF NOT EXISTS jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    url VARCHAR(1000),
    salary VARCHAR(255),
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),
    external_id VARCHAR(255),
    UNIQUE KEY uniq_job (source, external_id, location)
);

SELECT * FROM jobs; -- show scraped jobs