# URL Shortener

A work-in-progress URL shortener REST API built with Java 21, Spring Boot, Spring Data JPA, and PostgreSQL. Create short codes for HTTP(S) URLs, redirect to the original URL, and view click statistics.

> This repository currently contains the backend API only; there is no web UI yet.

## Features implemented

- Create a six-character alphanumeric short code for an HTTP or HTTPS URL.
- Persist URLs and click counts in PostgreSQL.
- Redirect to the original URL and increment its click count.
- Retrieve statistics for a short code.
- List URLs with pagination and sorting by creation time or click count.
- Validate create requests and return a not-found response for unknown short codes.

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven

## Prerequisites

- JDK 21
- PostgreSQL

The Maven Wrapper is included, so a separate Maven installation is not required.

## Run locally

1. Create a PostgreSQL database named `url_shortener` (or change the datasource URL below).
2. Configure the database connection in `src/main/resources/application.properties`:

   ```properties
   spring.application.name=url-shortener
   spring.datasource.url=jdbc:postgresql://localhost:5432/url_shortener
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_LOCAL_DATABASE_PASSWORD

   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```

   Keep real credentials out of version control. For a deployed application, use environment-based configuration and a managed secrets store.

3. Start the application from the repository root:

   **Windows PowerShell**

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   **macOS / Linux**

   ```bash
   ./mvnw spring-boot:run
   ```

The API listens on `http://localhost:8080` by default.

## API

All API endpoints use the `/api/v1/urls` base path.

### Create a short URL

```http
POST /api/v1/urls
Content-Type: application/json
```

Request:

```json
{
  "originalUrl": "https://example.com"
}
```

The original URL must be non-empty and start with `http://` or `https://`.

Example response:

```json
{
  "shortCode": "Ab12xY",
  "shortUrl": "http://localhost:8080/Ab12xY",
  "originalUrl": "https://example.com",
  "createdAt": "2026-10-20T12:00:00",
  "clickCount": 0
}
```

### Redirect to the original URL

```http
GET /api/v1/urls/{shortCode}
```

Returns a `302 Found` response with a `Location` header pointing to the original URL. A successful redirect increments the click count.

### Get URL statistics

```http
GET /api/v1/urls/{shortCode}/stats
```

Example response:

```json
{
  "shortCode": "Ab12xY",
  "originalUrl": "https://example.com",
  "createdAt": "2026-10-20T12:00:00",
  "clickCount": 3
}
```

### List URLs

```http
GET /api/v1/urls?page=0&size=10&sortBy=createdAt&direction=desc
```

Query parameters:

| Parameter | Default | Description |
|---|---:|---|
| `page` | `0` | Zero-based page number. Must be 0 or greater. |
| `size` | `10` | Page size. Must be at least 1. |
| `sortBy` | `createdAt` | Sort field: `createdAt` or `clickCount`. |
| `direction` | `desc` | Sort direction: `asc` or `desc` (case-insensitive). |

Example response:

```json
{
  "contents": [
    {
      "shortCode": "Ab12xY",
      "originalUrl": "https://example.com",
      "createdAt": "2026-10-20T12:00:00",
      "clickCount": 3
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

Unknown short codes return `404 Not Found`. Invalid request values return `400 Bad Request`.

## Run tests

**Windows PowerShell**

```powershell
.\mvnw.cmd test
```

**macOS / Linux**

```bash
./mvnw test
```

## Current limitations

- The API currently constructs the `shortUrl` response using `http://localhost:8080/{shortCode}`, but the redirect controller is mapped to `/api/v1/urls/{shortCode}`. Therefore, the returned `shortUrl` does not currently match the implemented redirect route.
- The short-link host is hard-coded to localhost; it is not yet configurable for deployment.
- Short codes are generated randomly, and there is no custom alias or expiration support.
- The project is under development; no frontend, authentication, rate limiting, or deployment configuration is included yet.

## GitHub repository suggestions

**Suggested repository name:** `spring-boot-url-shortener`

**Suggested description:** `A Java 21 and Spring Boot REST API for shortening URLs, redirecting to original links, and tracking click statistics with PostgreSQL.`

**Suggested GitHub topics:** `java`, `spring-boot`, `url-shortener`, `rest-api`, `postgresql`, `spring-data-jpa`, `maven`, `backend`

These repository topics are discoverability tags (often informally called skills); they describe the technologies and project domain rather than user skills.
