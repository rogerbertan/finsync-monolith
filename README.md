<a id="readme-top"></a>

<!-- PROJECT LOGO -->
<br />
<div align="center">

  <h3 align="center">FinSync</h3>

  <p align="center">
    A financial reconciliation monolith that matches orders with payment gateway transactions, detects discrepancies, and flags suspicious activity.
    <br />
    <a href="https://github.com/rogerbertan/finsync/issues/new?labels=bug&template=bug-report---.md">Report Bug</a>
    &middot;
    <a href="https://github.com/rogerbertan/finsync/issues/new?labels=enhancement&template=feature-request---.md">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
      <ul>
        <li><a href="#built-with">Built With</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->

## About The Project

FinSync is a financial reconciliation application designed to automate the matching of orders against incoming payment gateway transactions. It continuously processes pending orders, validates payment amounts and currencies, and records the outcome of each reconciliation attempt.

Key capabilities:

- **Automatic reconciliation** — runs on a configurable schedule (default: every 15 minutes) and can also be triggered manually via REST API
- **Discrepancy detection** — identifies amount or currency mismatches between orders and payments, recording a detailed divergence reason for each case
- **Unmatched payment tracking** — flags payments received without a corresponding order, enabling fraud detection and auditing
- **Full audit trail** — every reconciliation result is persisted with status, divergence details, and timestamp

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Built With

- [![Java][Java]][Java-url]
- [![Spring Boot][SpringBoot]][SpringBoot-url]
- [![Spring Data JPA][SpringDataJPA]][SpringDataJPA-url]
- [![PostgreSQL][PostgreSQL]][PostgreSQL-url]
- [![Flyway][Flyway]][Flyway-url]
- [![Maven][Maven]][Maven-url]
- [![Swagger][Swagger]][Swagger-url]

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- GETTING STARTED -->

## Getting Started

### Prerequisites

- **Java 21** — [Download](https://adoptium.net/)
- **PostgreSQL** — running instance with a database named `finsync_monolith` (or configure via environment variables)
- **Maven** — the project ships with Maven Wrapper, so a local install is optional

### Installation

1. Clone the repository
   ```sh
   git clone https://github.com/rogerbertan/finsync.git
   cd finsync
   ```

2. Set up the database — create a PostgreSQL database (defaults to `finsync_monolith`) and ensure the user has permission to create tables. Flyway will handle schema migrations automatically on startup.

3. Configure environment variables (optional — defaults are shown)
   ```sh
   export DB_HOST=localhost
   export DB_NAME=finsync_monolith
   export DB_USER=postgres
   export DB_PASSWORD=postgres
   ```

4. Build and run
   ```sh
   ./mvnw spring-boot:run
   ```

   Or build a JAR and run it:
   ```sh
   ./mvnw clean package
   java -jar target/finsync-0.0.1-SNAPSHOT.jar
   ```

5. Verify the application is running
   ```sh
   curl http://localhost:8080/v1/reconciliations/report
   ```

   OpenAPI documentation is available at `http://localhost:8080/swagger-ui.html`.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->

## Usage

### Trigger a reconciliation run manually

```sh
curl -X POST http://localhost:8080/v1/reconciliations/trigger
```

Response:
```json
{
  "triggeredAt": "2026-03-31T14:00:00"
}
```

### Query reconciliation results

Retrieve all results paginated:
```sh
curl "http://localhost:8080/v1/reconciliations/report?page=0&size=20"
```

Filter by status and date range:
```sh
curl "http://localhost:8080/v1/reconciliations/report?status=DIVERGED&from=2026-03-01T00:00:00&to=2026-03-31T23:59:59"
```

Available `status` values: `MATCHED`, `DIVERGED`, `UNMATCHED`

### Example response

```json
{
  "content": [
    {
      "id": "a1b2c3d4-...",
      "status": "DIVERGED",
      "divergenceReason": "Amount mismatch: expected 100.00 BRL, received 90.00 BRL",
      "reconciledAt": "2026-03-31T14:00:05",
      "order": {
        "id": "...",
        "externalReference": "ORD-001",
        "amount": 100.00,
        "currency": "BRL",
        "status": "PENDING"
      },
      "payment": {
        "id": "...",
        "gatewayPaymentId": "GW-XYZ",
        "orderExternalReference": "ORD-001",
        "amount": 90.00,
        "currency": "BRL",
        "method": "PIX",
        "status": "RECEIVED"
      }
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

### Reconciliation schedule

The scheduler runs automatically every 15 minutes by default. To change the interval, set the `finsync.reconciliation.cron` property in `application.yaml` or pass it as a system property:

```sh
java -jar target/finsync-0.0.1-SNAPSHOT.jar --finsync.reconciliation.cron="0 */5 * * * *"
```

### Running tests

```sh
./mvnw test
```

Generate a code coverage report with JaCoCo:
```sh
./mvnw clean verify
# Report available at: target/site/jacoco/index.html
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->

## Roadmap

- [x] Core reconciliation engine (MATCHED / DIVERGED / UNMATCHED)
- [x] REST API with filtering and pagination
- [x] Scheduled reconciliation job
- [x] OpenAPI / Swagger documentation
- [x] Flyway database migrations
- [x] Comprehensive unit and integration test coverage
- [ ] Docker and docker-compose support
- [ ] Webhook notifications on divergence detection
- [ ] Multi-currency conversion support
- [ ] Metrics and observability (Micrometer / Prometheus)
- [ ] Bulk payment ingestion endpoint

See the [open issues](https://github.com/rogerbertan/finsync/issues) for a full list of proposed features and known issues.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTRIBUTING -->

## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<!-- LICENSE -->

## License

Distributed under the MIT License. See `LICENSE.txt` for more information.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[Java]: https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[Java-url]: https://adoptium.net/
[SpringBoot]: https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[SpringBoot-url]: https://spring.io/projects/spring-boot
[SpringDataJPA]: https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white
[SpringDataJPA-url]: https://spring.io/projects/spring-data-jpa
[PostgreSQL]: https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white
[PostgreSQL-url]: https://www.postgresql.org/
[Flyway]: https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white
[Flyway-url]: https://flywaydb.org/
[Maven]: https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white
[Maven-url]: https://maven.apache.org/
[Swagger]: https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black
[Swagger-url]: https://swagger.io/
