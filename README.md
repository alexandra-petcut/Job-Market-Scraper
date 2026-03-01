# Job Market Web Scraper

A Java application that automatically collects job listings from three Romanian job portals and stores them in a database. Instead of manually browsing multiple websites to find job postings, this program does it for you — it visits each site, extracts the relevant information (title, company, salary, location, etc.), and saves everything into a single database where you can query and analyze it.

## Table of Contents

- [Key Concepts](#key-concepts)
- [Supported Job Portals](#supported-job-portals)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Build and Run](#build-and-run)
- [MySQL Workbench Queries](#mysql-workbench-queries)
- [How the Scraper Works](#how-the-scraper-works)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [How Each Scraper Works](#how-each-scraper-works)
- [Adding a New Scraper](#adding-a-new-scraper)
- [Dependencies](#dependencies)

---

## Supported Job Portals

| Portal | Website | How Data is Fetched |
|--------|---------|---------------------|
| **BestJobs** | bestjobs.eu | JSON API — requests structured data directly |
| **eJobs** | ejobs.ro | JSON API — requests structured data directly |
| **Hipo** | hipo.ro | HTML parsing — downloads web pages and extracts data from the page structure |

---

## Prerequisites

Before running this project, make sure you have the following installed:

| Tool | Purpose | Download |
|------|---------|----------|
| **Java 17+** | The programming language the scraper is written in | [adoptium.net](https://adoptium.net/) |
| **Maven** | Builds the project and downloads its dependencies | [maven.apache.org](https://maven.apache.org/download.cgi) |
| **Docker Desktop** | Runs the MySQL database in a container | [docker.com](https://www.docker.com/products/docker-desktop/) |
| **MySQL Workbench** | GUI for viewing and managing the database | [dev.mysql.com](https://dev.mysql.com/downloads/workbench/) |

---

## Database Setup

### Step 1: Start a MySQL container with Docker

Open a terminal and run:

```bash
docker run --name job_mysql -e MYSQL_ROOT_PASSWORD=rootpassword -e MYSQL_DATABASE=job_scraper -p 3306:3306 -d mysql:8.0
```

Here's what each part of this command does:

| Flag | Meaning |
|------|---------|
| `--name job_mysql` | Names the container `job_mysql` so you can refer to it easily |
| `-e MYSQL_ROOT_PASSWORD=rootpassword` | Sets the root (admin) password for MySQL to `rootpassword` |
| `-e MYSQL_DATABASE=job_scraper` | Automatically creates a database called `job_scraper` on startup |
| `-p 3306:3306` | Maps port 3306 on your machine to port 3306 inside the container, so your Java application can connect to MySQL at `localhost:3306` |
| `-d` | Runs the container in the background (detached mode) |
| `mysql:8.0` | The Docker image to use — MySQL version 8.0 |

After running this, you should see the `job_mysql` container running in Docker Desktop:

> Docker Desktop will show the container as a green (running) entry with name `job_mysql`, image `mysql:8.0`, and port `3306:3306`.

### Step 2: Connect MySQL Workbench to the container

1. Open **MySQL Workbench**
2. Click the **+** icon next to "MySQL Connections" to create a new connection
3. Fill in:
   - **Connection Name**: `JobScraperDB` (or any name you like)
   - **Hostname**: `localhost`
   - **Port**: `3306`
   - **Username**: `root`
   - **Password**: click "Store in Vault" and enter `rootpassword`
4. Click **Test Connection** — you should see "Successfully made the MySQL connection"
5. Click **OK** to save, then double-click the connection to open it

### Step 3: Create the jobs table

In the MySQL Workbench query editor, run the following SQL:

```sql
USE job_scraper;

CREATE TABLE jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    company VARCHAR(255),
    location VARCHAR(255),
    url VARCHAR(1000),
    salary VARCHAR(255),
    scraped_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(50),
    external_id VARCHAR(255),
    UNIQUE KEY uniq_job (source, external_id, location)  -- avoid duplicates
);
```

What each column stores:

| Column | Type | Purpose |
|--------|------|---------|
| `id` | `INT AUTO_INCREMENT` | Unique row number, automatically assigned |
| `title` | `VARCHAR(255)` | Job title (e.g. "Software Developer") |
| `company` | `VARCHAR(255)` | Company name |
| `location` | `VARCHAR(255)` | City name |
| `url` | `VARCHAR(1000)` | Direct link to the original job posting |
| `salary` | `VARCHAR(255)` | Salary information, or "Confidential" if not disclosed |
| `scraped_at` | `TIMESTAMP` | When the job was scraped (auto-filled with the current time) |
| `source` | `VARCHAR(50)` | Which portal the job came from (`bestjobs`, `ejobs`, or `hipo`) |
| `external_id` | `VARCHAR(255)` | The job's unique identifier on the original site |

The `UNIQUE KEY uniq_job (source, external_id, location)` line creates a **uniqueness constraint** — it prevents the database from storing duplicate entries. If the scraper finds a job that already exists (same source, same external ID, same location), it updates the existing row instead of creating a new one.

---

## Build and Run

All commands must be run from the `scraper/` directory:

```bash
# Navigate to the scraper module
cd scraper

# Download dependencies and compile
mvn clean compile

# Run the scraper
mvn exec:java -Dexec.mainClass="com.alexandrapetcut.jobmarket.scraper.Main"
```

When the scraper runs, you'll see output like:

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

---

## MySQL Workbench Queries

After the scraper finishes, you can use these queries in MySQL Workbench to inspect the data:

```sql
-- Switch to the job_scraper database
USE job_scraper;

-- Verify the table exists
SHOW TABLES;

-- View all scraped jobs
SELECT * FROM jobs;

-- Delete the table and all its data (use with caution)
DROP TABLE jobs;
```

---

## How the Scraper Works

Here's the step-by-step process that happens when you run the application:

```
1. Main.java starts
       |
2. Connect to the MySQL database (localhost:3306/job_scraper)
       |
3. Load eJobs static data
   (fetches a city ID -> city name lookup table from the eJobs API,
    because eJobs returns city IDs like "142" instead of names like "Cluj-Napoca")
       |
4. Create the three scrapers: BestJobsScraper, EJobsScraper, HipoScraper
       |
5. ScrapeRunner.runAll() loops through each scraper:
       |
       +---> scraper.scrape(100) — fetch up to 100 jobs from the portal
       |         |
       |         +---> Send HTTP requests to the portal (API call or HTML page fetch)
       |         +---> Parse the response (JSON or HTML) into Job objects
       |         +---> Handle pagination (fetch next page until limit is reached)
       |         +---> Return a list of Job objects
       |
       +---> For each Job, call DatabaseManager.save(job)
                 |
                 +---> INSERT the job into MySQL
                 +---> If it already exists (duplicate), UPDATE it instead
```

### Pagination

Job portals don't return all jobs at once — they split results into pages. Just like a Google search shows 10 results per page, the BestJobs API returns 24 jobs per request, and the eJobs API returns 40. The scraper must keep requesting the next page of results until it reaches the desired number of jobs (100 by default). Each portal uses a different method to move between pages:

- **BestJobs**: The API response includes a `nextCursor` value — a token you include in the next request to get the next batch
- **eJobs**: The API response includes a `morePagesFollow` flag — if `true`, you increment the page number and request again
- **Hipo**: The HTML page contains a "next page" link — if it exists, the scraper follows it

### Deduplication

When the scraper runs multiple times, it will encounter jobs it has already saved. Instead of creating duplicate rows, the database uses the `UNIQUE KEY` constraint to detect duplicates and update the existing row with fresh data (and a new `scraped_at` timestamp). This is done through an SQL pattern called **upsert** (`INSERT ... ON DUPLICATE KEY UPDATE`).

### Multi-Location Jobs

Some job listings are available in multiple cities. For example, a job might be posted for both Bucharest and Cluj-Napoca. In this case, the scraper creates a **separate row for each location**, so the same job appears once per city in the database. This makes it easy to filter jobs by location.

---

## Project Structure

```
WebScraper/
├── pom.xml                              # Root Maven config (unused scaffold)
├── README.md
└── scraper/
    ├── pom.xml                          # Module Maven config (dependencies defined here)
    ├── ejobs-api-endpoints.md           # Detailed eJobs API documentation
    └── src/main/java/com/alexandrapetcut/jobmarket/scraper/
        ├── Main.java                    # Entry point — wires everything together
        ├── Job.java                     # Data model — represents one job listing
        ├── JobScraper.java              # Interface that all scrapers implement
        ├── ScrapeRunner.java            # Runs all scrapers and saves results to DB
        ├── DatabaseManager.java         # Connects to MySQL and handles inserts/updates
        ├── BestJobsScraper.java         # Scraper for bestjobs.eu (JSON API)
        ├── EJobsScraper.java            # Scraper for ejobs.ro (JSON API)
        ├── EJobsStaticData.java         # City ID -> name lookup for eJobs
        └── HipoScraper.java            # Scraper for hipo.ro (HTML parsing)
```

---

## Architecture

The project follows the **Strategy design pattern** — there is a common interface (`JobScraper`) that defines what every scraper must do, and each portal has its own class that implements that interface in its own way. A central runner (`ScrapeRunner`) doesn't need to know the details of each scraper; it just calls `scrape()` on each one and saves the results.

### Components

| Class | Role |
|-------|------|
| **`JobScraper`** | Interface with two methods: `source()` returns the portal name, `scrape(int limit)` returns a list of jobs |
| **`Job`** | Simple data object with fields: `title`, `company`, `location`, `url`, `salary`, `source`, `externalId` |
| **`ScrapeRunner`** | Loops through all scrapers, calls `scrape()` on each, then saves every job to the database. If one scraper fails, it prints the error and continues with the next one |
| **`DatabaseManager`** | Opens a connection to MySQL and provides a `save(job)` method that inserts or updates a row |
| **`EJobsStaticData`** | Fetches a list of all cities from the eJobs API and builds a lookup map (city ID -> city name). Required because eJobs returns numeric city IDs instead of readable names |
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

---

## How Each Scraper Works

### BestJobsScraper

- **Source**: BestJobs public API (`bestjobs.eu/api/proxy/v2/jobs`)
- **Method**: Sends HTTP GET requests that return JSON
- **Pagination**: Cursor-based — each response includes a `nextCursor` token for the next batch (24 jobs per request)
- **Salary handling**: Uses the `salary` field if available; falls back to `estimatedSalary` (marked as estimated); defaults to "Confidential"
- **External ID**: Uses the job's URL slug (e.g. `software-developer-at-techcorp-123`)

### EJobsScraper

- **Source**: eJobs public API (`api.ejobs.ro/jobs`)
- **Method**: Sends HTTP GET requests that return JSON
- **Pagination**: Page-based — increments a `page` parameter (40 jobs per page) as long as `morePagesFollow` is `true`
- **Location resolution**: The API returns city IDs (integers) instead of city names. `EJobsStaticData` provides the translation — it fetches all city mappings once at startup from `api.ejobs.ro/all-statics`
- **External ID**: Uses the job's URL slug

### HipoScraper

- **Source**: Hipo website HTML pages (`hipo.ro/locuri-de-munca/cautajob/`)
- **Method**: Downloads the full HTML page and parses the DOM (document structure) using JSoup
- **Pagination**: Checks for a "next page" link (`a.page-next`); if it exists, fetches the next page
- **Data extraction**: Uses CSS selectors to find elements on the page:
  - `div.job-item` — each job card
  - `a.job-title` — the job title and URL
  - `p.company-name span` — the company name
  - `i.fa-money-bill-alt` — the salary icon (reads the parent element's text)
  - `span.badge-type` — the location(s)
- **External ID**: Extracts the numeric ID from the URL (e.g. `264847` from `/locuri_de_munca/264847/...`)

---

## Adding a New Scraper

To scrape a new job portal, you only need to do two things:

**1. Create a new class** that implements `JobScraper`:

```java
public class NewSiteScraper implements JobScraper {
    @Override
    public String source() {
        return "newsite";  // unique name for this portal
    }

    @Override
    public List<Job> scrape(int limit) throws Exception {
        List<Job> results = new ArrayList<>();
        // Fetch jobs from the portal (API or HTML)
        // Parse the responses into Job objects
        // Handle pagination until you reach the limit
        return results;
    }
}
```

**2. Register it** in the scrapers list in `Main.java`:

```java
List<JobScraper> scrapers = List.of(
    new BestJobsScraper(),
    new EJobsScraper(ejobsStatics),
    new HipoScraper(),
    new NewSiteScraper()   // add your new scraper here
);
```

The `ScrapeRunner` will automatically include it in its loop, and `DatabaseManager` will save its results using the same upsert logic.

---

## Dependencies

These are the external libraries used by the project (managed by Maven via `scraper/pom.xml`):

| Library | Version | What It Does |
|---------|---------|--------------|
| **[JSoup](https://jsoup.org/)** | 1.17.2 | Makes HTTP requests to websites and APIs. Also parses HTML pages into a navigable structure (used by HipoScraper to extract data from page elements) |
| **[MySQL Connector/J](https://dev.mysql.com/downloads/connector/j/)** | 8.3.0 | JDBC driver — lets the Java application talk to the MySQL database |
| **[org.json](https://github.com/stleary/JSON-java)** | 20250517 | Parses JSON text into Java objects so the scraper can read API responses |
