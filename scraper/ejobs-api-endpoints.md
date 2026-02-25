# eJobs.ro API Documentation

> Discovered by analyzing the Nuxt.js frontend bundles and testing endpoints.
> Last updated: 2026-02-23

## Base URLs

| Service | URL |
|---------|-----|
| **Main API** | `https://api.ejobs.ro/` |
| **Identity Provider (OAuth2)** | `https://idp.ejobs.ro/` |
| **Content/CDN** | `https://content.ejobs.ro/` |
| **Blog (WordPress)** | `https://cariera.ejobs.ro/` |
| **Website** | `https://www.ejobs.ro/` |

---

## Public Endpoints (No Authentication Required)

These endpoints return `200 OK` without any auth token.

### 1. Job Search / Listing

```
GET https://api.ejobs.ro/jobs
```

**Query Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `page` | int | Page number (starts at 1) |
| `pageSize` | int | Results per page (default ~20) |
| `q` | string | Keyword search (e.g. `java`, `developer`) |
| `orderBy` | string | Sort order: `date` or `distance` |
| `filters.cities` | int | City ID (repeatable) |
| `filters.departments` | int | Department ID (repeatable) |
| `filters.industries` | int | Industry ID (repeatable) |
| `filters.careerLevels` | int | Career level ID (repeatable) |
| `filters.contractTypes` | int | Contract type ID (repeatable) |
| `filters.educationLevels` | int | Education level ID (repeatable) |
| `filters.hasSalaryOffer` | bool | Only jobs with visible salary |
| `lat` | float | Latitude (for distance sorting) |
| `long` | float | Longitude (for distance sorting) |
| `jobTypes` | string | Filter by job type |

**Example:**

```bash
curl "https://api.ejobs.ro/jobs?page=1&pageSize=2&q=java&filters.cities=1"
```

**Response:**

```json
{
  "jobs": [
    {
      "id": 1938902,
      "title": "DIRECTOR DE VANZARI",
      "slug": "director-de-vanzari",
      "salary": "5000 - 7000 RON",
      "positions": 1,
      "creationDate": "2026-02-23T19:04:50Z",
      "expirationDate": "2026-03-25T00:00:00Z",
      "company": {
        "id": 245437,
        "name": "MIROGLIO ROMANIA SRL",
        "logoUrl": "/img/logos/2/245437.jpg",
        "verified": true,
        "slug": "miroglio-romania-srl"
      },
      "locations": [
        { "cityId": 21 },
        { "cityId": 1 }
      ],
      "industriesIds": [8, 17, 25, 28],
      "careerLevelsIds": [4, 6],
      "departmentsIds": [5, 65, 101, 53],
      "contractTypesIds": [5]
    }
  ],
  "totalCount": 18366,
  "morePagesFollow": true
}
```

---

### 2. Job Details

```
GET https://api.ejobs.ro/jobs/{jobId}?viewedFromMobile={true|false}
```

**Example:**

```bash
curl "https://api.ejobs.ro/jobs/1938902?viewedFromMobile=false"
```

**Response:** Full job object including `details.jobDescription`, `details.idealCandidate`, full location data with `latitude`/`longitude`, `educationLevelsIds`, and company info with `positions` count.

---

### 3. All Static Data (Lookup Tables)

```
GET https://api.ejobs.ro/all-statics
```

Returns all reference data for both `ro` and `en` locales:
- **cities** - ID, name, slug, countyId
- **counties** - ID, name, slug
- **departments** - ID, name, slug
- **industries** - ID, name, slug
- **careerLevels** - ID, name, slug
- **contractTypes** - ID, name, slug
- **educationLevels** - ID, name, slug
- **languages** - ID, name, slug

Use this to decode the IDs returned by the `/jobs` endpoint.

**Example:**

```bash
curl "https://api.ejobs.ro/all-statics"
```

**Response (truncated):**

```json
{
  "ro": {
    "cities": [
      { "id": 381, "name": "Remote (de acasă)", "slug": "remote" },
      { "id": 1, "name": "București", "slug": "bucuresti", "countyId": 10 },
      { "id": 14, "name": "Cluj-Napoca", "slug": "cluj-napoca", "countyId": 21 }
    ],
    "departments": [...],
    "industries": [...],
    "careerLevels": [...],
    "contractTypes": [...],
    "educationLevels": [...],
    "languages": [...]
  },
  "en": { ... }
}
```

---

### 4. Homepage Details (Featured Jobs)

```
GET https://api.ejobs.ro/homepage-details
```

Returns featured/promoted job listings displayed on the homepage. Response includes full job objects with company info, locations with coordinates, languages, etc.

---

### 5. Verified Jobs Count

```
GET https://api.ejobs.ro/verified-jobs-number
```

**Response:**

```json
{ "verifiedJobsNumber": 18366 }
```

---

### 6. Search Autocomplete / Suggestions

```
GET https://api.ejobs.ro/suggested-searches?q={query}
```

**Example:**

```bash
curl "https://api.ejobs.ro/suggested-searches?q=java"
```

**Response:**

```json
{
  "suggestions": [
    "java", "java developer", "javascript", "programator java",
    "junior java", "java junior", "java programmer", "java j2ee",
    "java oracle", "training java"
  ]
}
```

---

### 7. SEO Whitelist (Valid Search Terms)

```
GET https://api.ejobs.ro/seo/whitelist
```

Returns a large list of allowed/valid search keywords. Useful for understanding which queries are expected.

---

### 8. Countries List

```
GET https://api.ejobs.ro/countries?lang={ro|en}&pageSize={n}
```

**Example:**

```bash
curl "https://api.ejobs.ro/countries?lang=ro&pageSize=500"
```

**Response:**

```json
{
  "countries": [
    { "id": 1, "name": "Afganistan", "slug": "AF", "flagCode": "AFG", "prefix": "+93" },
    { "id": 141, "name": "România", "slug": "RO", "flagCode": "ROU", "prefix": "+40" }
  ],
  "totalCount": 219
}
```

---

### 9. Vouchers

```
GET https://api.ejobs.ro/vouchers?lang={ro|en}
```

Returns active promotional vouchers (may be empty).

---

### 10. Skills Search

```
GET https://api.ejobs.ro/search/skills?q={query}
```

Autocomplete for skill names.

---

### 11. Skill Suggestions from Text

```
GET https://api.ejobs.ro/suggester/text-skills?count={n}&text={text}
```

AI-powered skill extraction from free text.

---

### 12. Salary Calculator - Positions Search

```
GET https://api.ejobs.ro/salario/positions?q={query}
```

Search job positions for the salary calculator.

---

### 13. Average Salary

```
GET https://api.ejobs.ro/salario/average-salary/{positionId}
```

Get average salary data for a specific position.

---

### 14. Salary Calculator

```
POST https://api.ejobs.ro/salario/calculate
```

Calculate estimated salary (body params TBD).

---

### 15. Unsubscribe Reasons

```
GET https://api.ejobs.ro/unsubscribe-reasons
```

---

### 16. Legal Terms

```
GET https://api.ejobs.ro/tos?lang={ro|en}
```

---

### 17. Email Alerts

```
GET https://api.ejobs.ro/email-alerts
```

---

### 18. Cookies Configuration

```
GET https://api.ejobs.ro/cookies
```

---

## Authenticated Endpoints (Require OAuth2 Bearer Token)

These require an `Authorization: Bearer {token}` header.

### Candidate Profile

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}` | Get candidate profile |
| GET | `/candidate/details` | Get current logged-in candidate details |
| GET | `/candidates/{id}/dashboard-data` | Dashboard stats |
| GET | `/candidates/{id}/export-data` | GDPR data export |
| POST | `/candidates/accept-tos` | Accept terms of service |
| GET | `/candidates/latest-tos-accepted` | Check if latest TOS accepted |

### Applications

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}/applications?page={n}&pageSize={n}&q={q}&status={s}` | List applications |
| GET | `/candidates/{id}/applications/{appId}` | Application details |
| POST | `/candidates/{id}/jobs-applied/{jobId}/{locale}` | Apply to a job |
| GET | `/candidates/{id}/applications/{appId}/employer-requests` | Employer requests on application |
| GET | `/candidates/{id}/applications/{appId}/presentation-message` | Presentation message |
| POST | `/candidates/{id}/applications/{appId}/mini-interview` | Submit mini interview answers |

### Saved / Hidden / Preferred Jobs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}/jobs-preferred?page={n}&pageSize={n}&q={q}` | List saved jobs |
| POST | `/candidates/{id}/jobs-preferred/{jobId}` | Save a job |
| DELETE | `/candidates/{id}/jobs-preferred/{jobId}` | Unsave a job |
| GET | `/candidates/{id}/jobs-hidden?page={n}&pageSize={n}&q={q}` | List hidden jobs |
| POST | `/candidates/{id}/jobs-hidden/{jobId}` | Hide a job |
| DELETE | `/candidates/{id}/jobs-hidden/{jobId}` | Unhide a job |

### Saved Searches

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}/saved-searches?page={n}&pageSize={n}&q={q}` | List saved searches |
| GET | `/candidates/{id}/saved-searches/{searchId}` | Saved search details |
| GET | `/candidates/{id}/recentSearches` | Recent searches |

### Preferred Companies

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}/companies-preferred?page={n}&pageSize={n}&q={q}` | List followed companies |
| POST | `/candidates/{id}/companies-preferred/{companyId}` | Follow a company |
| DELETE | `/candidates/{id}/companies-preferred/{companyId}` | Unfollow a company |

### CV Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/candidates/{id}/cvs/{cvId}` | Get CV details |
| DELETE | `/candidates/{id}/cvs/{cvId}` | Delete a CV |
| GET | `/candidates/{id}/cvs/{cvId}/pdf` | Download CV as PDF |
| POST | `/candidates/{id}/cvs/{cvId}/send-email` | Email CV to someone |
| GET/POST | `/candidates/{id}/cvs/{cvId}/skills` | List/add skills |
| PUT/DELETE | `/candidates/{id}/cvs/{cvId}/skills/{skillId}` | Update/delete a skill |
| GET/POST | `/candidates/{id}/cvs/{cvId}/hobbies` | List/add hobbies |
| PUT/DELETE | `/candidates/{id}/cvs/{cvId}/hobbies/{hobbyId}` | Update/delete a hobby |
| POST | `/candidates/{id}/cvs/{cvId}/bulkExperiences` | Bulk add work experiences |
| POST | `/candidates/{id}/cvs/{cvId}/bulkStudies` | Bulk add education entries |
| POST | `/assets/candidates/{id}/cv` | Upload CV file |
| POST | `/candidates/cvs/log-unparsed-mandatory-fields` | Log CV parsing errors |
| GET | `/cv-progress-percentages` | CV completion percentages |

### Avatar

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/assets/candidates/{id}/avatar` | Upload profile photo |
| DELETE | `/candidates/{id}/avatar` | Delete profile photo |

### Interviews

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/interviews?pageSize={n}&filters.candidate={id}` | List interviews |
| GET | `/interviews/{interviewId}` | Interview details |
| GET | `/interview/cancel-reasons?lang={locale}` | Cancel reason options |
| GET | `/jobs/{jobId}/mini-interview` | Mini interview questions for a job |
| POST | `/mocha/test-details/{testId}/{candidateId}` | Mocha assessment details |

### Alerts & Notifications

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/alerts?lang={ro\|en}` | Job alerts |
| GET | `/candidates/{id}/email-alerts` | Email alert preferences |
| POST | `/candidates/{id}/activate-alerts` | Activate job alerts |
| POST | `/candidates/{id}/deactivate-alerts` | Deactivate job alerts |

### Account Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/accounts` | Account info |
| DELETE | `/accounts/delete` | Delete account |
| POST | `/accounts/confirm-received-hash-email` | Confirm email via hash |
| POST | `/accounts/confirm-received-hash-change-email` | Confirm email change |
| POST | `/accounts/send-confirmation-link-email` | Resend confirmation email |
| POST | `/accounts/send-change-email-confirmation-link-email` | Send email change link |
| POST | `/accounts/send-confirmation-code-phonenumber` | Send phone confirmation SMS |
| POST | `/accounts/confirm-received-code-phonenumber` | Confirm phone with code |
| POST | `/accounts/2fa-email/enable` | Enable two-factor auth |
| POST | `/accounts/2fa-email/request-disable` | Request 2FA disable code |
| GET | `/accounts/2fa-email/is-enabled` | Check if 2FA is enabled |
| GET | `/accounts/error/{code}` | Test error handling |

### Company Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/companies/{id}?jobsPageNo={n}&jobsPageSize={n}` | Company details with job listings |
| GET | `/company/details` | Logged-in company profile |

### Unregistered Candidate Searches

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/unregistered-candidates/saved-search/create` | Create saved search without account |
| GET | `/unregistered-candidates/saved-searches/{token}` | Get searches by token |
| DELETE | `/unregistered-candidates/saved-search/delete` | Delete saved search |
| POST | `/unregistered-candidates/saved-search/{action}` | Confirm search action |
| POST | `/unregistered-candidates/saved-search/resend-confirmation` | Resend confirmation email |

### Contact / Support

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/zendesk/contact` | Submit contact form |
| POST | `/zendesk/company-get-offer` | Company request for offer |

### Salary (Salario) - Authenticated

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/salario/positions` | Add new salary position |

---

## Authentication (OAuth2)

**IDP Base URL:** `https://idp.ejobs.ro/`
**Client ID:** `ejobs-live-web`

### Login Flow

```
GET https://idp.ejobs.ro/oauth2/auth
  ?client_id=ejobs-live-web
  &redirect_uri=https://www.ejobs.ro/oauth/auth_handler
  &scope=openid+offline
  &response_type=code
  &state={state}
```

### Token Exchange

```
POST https://idp.ejobs.ro/oauth2/token
```

### Token Introspection

```
GET https://www.ejobs.ro/oauth/introspect/{token}
```

### Logout

```
GET https://idp.ejobs.ro/oauth2/sessions/logout
  ?id_token_hint={token}
  &client_id=ejobs-live-web
  &post_logout_redirect_uri=https://www.ejobs.ro/oauth/logout
  &state=randomState
```

### Session Check

```
GET https://www.ejobs.ro/oauth/auth_handler
```

### Token Refresh

```
POST https://www.ejobs.ro/oauth/refresh_token
```

---

## RSS & Sitemaps

| URL | Description |
|-----|-------------|
| `https://www.ejobs.ro/rss-listings.xml` | RSS feed of latest job listings |
| `https://www.ejobs.ro/sitemap-listings-index.xml` | Sitemap index (by category) |
| `https://www.ejobs.ro/sitemap-expired-listings.xml` | Expired listings sitemap |
| `https://www.ejobs.ro/sitemap-listings-{category}.xml` | Per-category sitemaps (e.g. `it-software`, `marketing`) |

**Available sitemap categories:**
achizitii, administrativ-logistica, agricultura, alimentatie-horeca, altele, arhitectura-design-interior, asigurari, au-pair-babysitter-curatenie, audit-consultanta, auto-echipamente, automatizari, banci, cercetare-dezvoltare, chimie-biochimie, confectii-design-vestimentar, constructii-instalatii, controlul-calitatii, crewing-casino-entertainment, educatie-training-arte, electric, farmacie, financiar-contabilitate, functii-publice, grafica-webdesign-dtp, imobiliare, import-export, inginerie, internet-ecommerce, it-hardware, it-software, juridic, jurnalism-editorial, management, marketing, medicina-alternativa, medicina-umana, medicina-veterinara, merchandising-promoteri, mlm-vanzari-directe, naval-aeronautic, office-secretariat, ong-voluntariat, paza-si-protectie-militar, personal-calificat, petrol-gaze, prelucrarea-lemnului-pvc, productie, proiectare-civila-industriala, project-management, protectia-mediului, protectia-muncii, publicitate, relatii-clienti-call-center, relatii-publice, resurse-umane-psihologie, saloane-clinici-frumusete, sanitar, service-reparatii, specialisti-tehnicieni, sport-wellness, statistica-matematica, telecomunicatii, termice, tipografii-edituri, traduceri, transport-distributie, turism-hotel-staff, vanzari

---

## Blog API (WordPress)

```
GET https://cariera.ejobs.ro/wp-json/wp/v2/posts?per_page={n}
```

Standard WordPress REST API. Returns blog articles with title, content, date, slug, etc.

---

## Content Storage

```
https://content.ejobs.ro/{path}
```

Used for serving static assets like company logos and images. Logo URLs from the API are relative paths (e.g. `/img/logos/2/245437.jpg`) and should be prefixed with `https://content.ejobs.ro`.

**Full logo URL example:**

```
https://content.ejobs.ro/img/logos/2/245437.jpg
```

---

## Miscellaneous

| URL | Description |
|-----|-------------|
| `https://www.ejobs.ro/emails/send-report-job` | Report illegal job content |
| `https://score2skill.ejobs.ro/` | Score2Skill assessment platform |
| `https://www.jobradar24.ro/` | JobRadar24 (related service) |
| `https://www.wearehr.ro/` | WeAreHR (related service) |

---

## Notes

- The site uses **Cloudflare Turnstile** captcha (siteKey: `0x4AAAAAACaAXHKxh2RPvMnx`) on some forms
- The frontend is built with **Nuxt.js** (Vue) with Pinia for state management and Axios for HTTP
- Company logo URLs from API responses are relative - prepend `https://content.ejobs.ro`
- The API uses standard HTTP status codes (200, 301, 403, 404)
- City/department/industry IDs in job listings can be decoded using the `/all-statics` endpoint
- Pagination follows `page` + `pageSize` pattern with `totalCount` and `morePagesFollow` in responses
