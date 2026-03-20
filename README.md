# Job Market Web Scraper

A Java application that automatically collects job listings from three Romanian job portals and stores them in a database. Instead of manually browsing multiple websites to find job postings, this program does it for you — it visits each site, extracts the relevant information (title, company, salary, location, etc.), and saves everything into a single database where you can query and analyze it.

## Supported Job Portals

| Portal | Website | How Data is Fetched |
|--------|---------|---------------------|
| **BestJobs** | bestjobs.eu | JSON API — requests structured data directly |
| **eJobs** | ejobs.ro | JSON API — requests structured data directly |
| **Hipo** | hipo.ro | HTML parsing — downloads web pages and extracts data from the page structure |

## Prerequisites

| Tool | Purpose | Download |
|------|---------|----------|
| **Java 17+** | The programming language the scraper is written in | [adoptium.net](https://adoptium.net/) |
| **Maven** | Builds the project and downloads its dependencies | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **Docker Desktop** | Runs the MySQL database in a container | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **MySQL Workbench** *(optional)* | GUI for viewing and managing the database | [dev.mysql.com](https://dev.mysql.com/downloads/workbench/) |

## Quick Start

### 1. Start the database

```bash
docker compose up -d
```

This starts a MySQL 8.0 container and automatically creates the `job_scraper` database and `jobs` table.

### 2. Build and run

```bash
mvn clean compile exec:java
```

You'll see output like:

```
Connected to MySQL successfully!
Loaded 312 eJobs cities
=== Running: bestjobs ===
Saved/updated 100 jobs from bestjobs
=== Running: ejobs ===
Saved/updated 100 jobs from ejobs
=== Running: hipo ===
Saved/updated 100 jobs from hipo
```

### 3. Query the results

Connect MySQL Workbench to `localhost:3306` (user: `root`, password: `rootpassword`) and run:

```sql
USE job_scraper;
SELECT * FROM jobs;
```

## Project Structure

```
├── pom.xml                          Maven config (Java 17, dependencies)
├── docker-compose.yml               One-command MySQL setup
├── db/
│   └── init.sql                     Table creation script (runs on first start)
├── docs/
│   └── ejobs-api-endpoints.md       Detailed eJobs API documentation
└── src/main/java/com/alexandrapetcut/jobmarket/scraper/
    ├── Main.java                    Entry point — wires everything together
    ├── Job.java                     Data model — represents one job listing
    ├── JobScraper.java              Interface that all scrapers implement
    ├── ScrapeRunner.java            Runs all scrapers and saves results to DB
    ├── DatabaseManager.java         Connects to MySQL and handles inserts/updates
    ├── BestJobsScraper.java         Scraper for bestjobs.eu (JSON API)
    ├── EJobsScraper.java            Scraper for ejobs.ro (JSON API)
    ├── EJobsStaticData.java         City ID → name lookup for eJobs
    └── HipoScraper.java            Scraper for hipo.ro (HTML parsing)
```

## Architecture

The project follows the **Strategy design pattern** — there is a common interface (`JobScraper`) that defines what every scraper must do, and each portal has its own class that implements that interface in its own way. A central runner (`ScrapeRunner`) doesn't need to know the details of each scraper; it just calls `scrape()` on each one and saves the results.

### Components

| Class | Role |
|-------|------|
| **`JobScraper`** | Interface with two methods: `source()` returns the portal name, `scrape(int limit)` returns a list of jobs |
| **`Job`** | Simple data object with fields: `title`, `company`, `location`, `url`, `salary`, `source`, `externalId` |
| **`ScrapeRunner`** | Loops through all scrapers, calls `scrape()` on each, then saves every job to the database. If one scraper fails, it prints the error and continues with the next one |
| **`DatabaseManager`** | Opens a connection to MySQL and provides a `save(job)` method that inserts or updates a row |
| **`EJobsStaticData`** | Fetches a list of all cities from the eJobs API and builds a lookup map (city ID → city name). Required because eJobs returns numeric city IDs instead of readable names |
| **`Main`** | Entry point — initializes the database connection, loads static data, creates the three scrapers, and starts the runner |

### Data Flow

```
Main
 ├── new DatabaseManager()         →  opens MySQL connection
 ├── new EJobsStaticData()         →  fetches city lookup from eJobs API
 ├── creates [BestJobsScraper, EJobsScraper, HipoScraper]
 └── ScrapeRunner.runAll(scrapers, 100)
      ├── BestJobsScraper.scrape(100)  →  calls BestJobs API  →  List<Job>
      ├── EJobsScraper.scrape(100)     →  calls eJobs API     →  List<Job>
      ├── HipoScraper.scrape(100)      →  parses Hipo HTML    →  List<Job>
      └── for each Job: DatabaseManager.save(job)  →  MySQL upsert
```

### Pagination

Job portals don't return all jobs at once — they split results into pages. Each portal uses a different method:

- **BestJobs**: The API response includes a `nextCursor` value — a token you include in the next request to get the next batch (24 jobs per request)
- **eJobs**: The API response includes a `morePagesFollow` flag — if `true`, you increment the page number and request again (40 jobs per page)
- **Hipo**: The HTML page contains a "next page" link — if it exists, the scraper follows it

### Deduplication

When the scraper runs multiple times, it will encounter jobs it has already saved. Instead of creating duplicate rows, the database uses a `UNIQUE KEY` constraint on `(source, external_id, location)` to detect duplicates and update the existing row with fresh data. This is done through an SQL pattern called **upsert** (`INSERT ... ON DUPLICATE KEY UPDATE`).

### Multi-Location Jobs

Some job listings are available in multiple cities. The scraper creates a **separate row for each location**, so the same job appears once per city in the database. This makes it easy to filter jobs by location.

## How Each Scraper Works

### BestJobsScraper

- **Source**: BestJobs public API (`bestjobs.eu/api/proxy/v2/jobs`)
- **Method**: Sends HTTP GET requests that return JSON
- **Pagination**: Cursor-based — each response includes a `nextCursor` token for the next batch (24 jobs per request)
- **Salary handling**: Uses the `salary` field if available; falls back to `estimatedSalary` (marked as estimated); defaults to "Confidential"
- **External ID**: Uses the job's URL slug

### EJobsScraper

- **Source**: eJobs public API (`api.ejobs.ro/jobs`)
- **Method**: Sends HTTP GET requests that return JSON
- **Pagination**: Page-based — increments a `page` parameter (40 jobs per page) as long as `morePagesFollow` is `true`
- **Location resolution**: The API returns city IDs (integers) instead of city names. `EJobsStaticData` provides the translation — it fetches all city mappings once at startup from `api.ejobs.ro/all-statics`
- **External ID**: Uses the job's URL slug

### HipoScraper

- **Source**: Hipo website HTML pages (`hipo.ro/locuri-de-munca/cautajob/`)
- **Method**: Downloads the full HTML page and parses the DOM using JSoup
- **Pagination**: Checks for a "next page" link (`a.page-next`); if it exists, fetches the next page
- **Data extraction**: Uses CSS selectors to find elements on the page:
  - `div.job-item` — each job card
  - `a.job-title` — the job title and URL
  - `p.company-name span` — the company name
  - `i.fa-money-bill-alt` — the salary icon (reads the parent element's text)
  - `span.badge-type` — the location(s)
- **External ID**: Extracts the numeric ID from the URL

## Dependencies

| Library | Version | What It Does |
|---------|---------|--------------|
| **[JSoup](https://jsoup.org/)** | 1.17.2 | Makes HTTP requests to websites and APIs. Also parses HTML pages into a navigable structure (used by HipoScraper to extract data from page elements) |
| **[MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)** | 8.3.0 | JDBC driver — lets the Java application talk to the MySQL database |
| **[org.json](https://github.com/stleary/JSON-java)** | 20250517 | Parses JSON text into Java objects so the scraper can read API responses |
