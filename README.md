# RideLink Backend

[![CI](https://github.com/kavinduumayanga/ridelink-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/kavinduumayanga/ridelink-backend/actions/workflows/ci.yml)

RideLink is a microservices-based ride-hailing backend developed for the IT3130 Application Development university assignment. It demonstrates account management, driver operations, ride orchestration, fare calculation, simulated payment, JWT security, API documentation, integration testing, and continuous integration. The assignment is backend-only; no frontend is required.

## Technology stack

- Java 21 and Spring Boot 3.4.5
- Spring Web and synchronous REST/JSON
- Spring Data MongoDB with MongoDB Atlas
- Spring Security with JWT authentication and role-based authorization
- Maven, JUnit 5, and Mockito
- SpringDoc Swagger/OpenAPI
- Postman integration and demonstration suite
- GitHub Actions CI

## Services and team ownership

The four services were developed independently and integrated through frozen REST contracts.

| Service | Team member | Port | Responsibility | Owned database |
| --- | --- | ---: | --- | --- |
| Account Service (`account-service`) | Nethmi | 8081 | Registration, login, profiles, account status, JWT issuance | `ridelink_account_db` |
| Driver & Vehicle Service (`driver-service`) | Ridma | 8082 | Driver profiles, vehicles, availability, simulated location | `ridelink_driver_db` |
| Ride Management Service (`ride-service`) | Kavindu | 8083 | Ride lifecycle, driver assignment, Driver/Fare orchestration | `ridelink_ride_db` |
| Fare & Payment Service (`fare-payment-service`) | Pehansa | 8084 | Fare estimates, final fares, simulated payments, receipts | `ridelink_payment_db` |

## Architecture

Each Spring Boot service owns its MongoDB database and persistence layer. Services exchange stable string identifiers through REST DTOs; no service accesses another service's database or repository.

- Account runs on `:8081` and is the identity and JWT issuer.
- Driver runs on `:8082` and owns operational driver and vehicle data.
- Ride runs on `:8083` and owns the ride lifecycle.
- Fare/Payment runs on `:8084` and owns fare and payment records.
- Ride calls Driver synchronously to find available drivers.
- Ride calls Fare/Payment synchronously for estimated and final fares.

See the [architecture and sequence diagrams](docs/ARCHITECTURE.md) for service boundaries, database isolation, security flow, lifecycle, and the complete demonstrated sequence.

## Core workflow

The demonstrated workflow is:

1. Register and log in the passenger and driver accounts.
2. Create the operational driver profile, configure the vehicle and simulated location, and set the driver to `AVAILABLE`.
3. Create a ride; Ride obtains and stores an estimated fare from Fare/Payment.
4. Request assignment; Ride queries Driver and selects the first eligible driver.
5. Progress through `REQUESTED -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED`.
6. During completion, Ride obtains the authoritative final `fareId` and total from Fare/Payment and stores them as `finalFareId` and `finalFare`.
7. Create a simulated payment using the completed ride and retrieve its receipt.

Cancellation is supported from `REQUESTED`, `ASSIGNED`, and `ACCEPTED`. Invalid lifecycle transitions return HTTP 409.

## Security

Account issues HS256 JWTs after successful login. A token contains `sub = userId`, a `role` claim with `PASSENGER`, `DRIVER`, or `ADMIN`, issuance time, and expiration.

Driver, Ride, and Fare/Payment validate the signature, claims, and expiration locally; they do not call Account for authentication. Ride forwards the caller's bearer JWT when it calls protected Driver and Fare endpoints. Spring Security applies role and ownership checks to protected operations.

All services must receive the same runtime `JWT_SECRET`. It must contain at least 32 UTF-8 bytes, must be supplied through the environment, and must never be committed. Missing, malformed, invalid, or expired authentication returns HTTP 401. A valid authenticated user without the required role or ownership returns HTTP 403.

## Prerequisites

- JDK 21
- Maven 3.9 or later
- A MongoDB Atlas project/cluster reachable from the development machine
- Four logically isolated service databases, with service-specific users and permissions as appropriate
- Postman for the API demonstration suite

No frontend, Docker, Node.js, service registry, message broker, or API Gateway is required.

## Local configuration

Clone the repository and create an ignored local environment file:

```bash
git clone https://github.com/kavinduumayanga/ridelink-backend.git
cd ridelink-backend
cp .env.example .env
```

Configure `.env` with local values for:

| Variable | Purpose |
| --- | --- |
| `ACCOUNT_MONGODB_URI` | Account-owned MongoDB connection |
| `DRIVER_MONGODB_URI` | Driver-owned MongoDB connection |
| `RIDE_MONGODB_URI` | Ride-owned MongoDB connection |
| `PAYMENT_MONGODB_URI` | Fare/Payment-owned MongoDB connection |
| `JWT_SECRET` | Shared JWT signing and validation secret for all services |
| `DRIVER_SERVICE_URL` | Driver base URL used by Ride |
| `FARE_PAYMENT_SERVICE_URL` | Fare/Payment base URL used by Ride |

Replace all sample or blank values, and quote URI values that contain shell-sensitive characters such as `&`. Never commit `.env`, Atlas credentials, passwords, connection strings, or runtime JWTs. The same `JWT_SECRET` value must be loaded into all four service processes.

## Quick Demo Start

After `.env` has been configured, the macOS/Linux launcher can start all four services concurrently with one command. On first use, make the scripts executable:

```bash
chmod +x start-all.sh stop-all.sh
```

Start RideLink:

```bash
./start-all.sh
```

The launcher checks required environment variables and ports, writes service output under `logs/`, waits for all four ports, and remains in the foreground. Press `Ctrl+C` for normal shutdown. If the launcher terminal was closed unexpectedly, stop only its recorded service processes with:

```bash
./stop-all.sh
```

The individual-service commands below remain available as the manual alternative.

## Running the services

Environment variables must be exported in every terminal because each service runs as a separate process. Open four terminals at the repository root and use the corresponding command block.

Account Service:

```bash
set -a
source .env
set +a
cd account-service
mvn spring-boot:run
```

Driver & Vehicle Service:

```bash
set -a
source .env
set +a
cd driver-service
mvn spring-boot:run
```

Ride Management Service:

```bash
set -a
source .env
set +a
cd ride-service
mvn spring-boot:run
```

Fare & Payment Service:

```bash
set -a
source .env
set +a
cd fare-payment-service
mvn spring-boot:run
```

The services listen on ports 8081, 8082, 8083, and 8084 respectively. If startup reports that a port is already in use, identify and stop the existing process on that port before restarting the service; do not run duplicate instances on alternate ports unless all dependent URLs are updated consistently.

## Swagger and OpenAPI

After the services are running, their configured documentation endpoints are:

| Service | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Account | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs) |
| Driver | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs) |
| Ride | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs) |
| Fare/Payment | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [http://localhost:8084/v3/api-docs](http://localhost:8084/v3/api-docs) |

Swagger/OpenAPI documentation is intentionally public. Protected business requests invoked from Swagger still require the appropriate bearer JWT.

## Postman integration suite

The final Postman suite contains 56 requests covering the happy path and reproducible negative scenarios.

1. Import [RideLink.postman_collection.json](postman/RideLink.postman_collection.json).
2. Import [RideLink.local.postman_environment.json](postman/RideLink.local.postman_environment.json).
3. Select the **RideLink Local** environment.
4. Set a local `testPassword` value of at least eight characters; do not export or commit it.
5. Run folders `00` through `09` in numeric order.

Unique emails and other test data are generated dynamically. IDs, fares, and Account-issued JWTs are captured and reused automatically. Folders `01`–`04` execute the complete happy path; folders `05`–`09` cover security, lifecycle, validation, ownership, fare, and payment failures. See the [Postman guide](postman/README.md) for prerequisites, variables, reruns, and scenarios requiring dedicated setup data.

## Testing

Run verification independently from the repository root:

```bash
(cd account-service && mvn clean verify)
(cd driver-service && mvn clean verify)
(cd ride-service && mvn clean verify)
(cd fare-payment-service && mvn clean verify)
```

Current totals derived from successful Surefire reports are:

| Service | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Account | 86 | 0 | 0 | 0 |
| Driver | 102 | 0 | 0 | 0 |
| Ride | 90 | 0 | 0 | 0 |
| Fare/Payment | 72 | 0 | 0 | 0 |
| **Total** | **350** | **0** | **0** | **0** |

Separate real HTTP/API verification completed the 17-step happy path and 17 negative integration scenarios successfully. These are end-to-end verification results, not additional Maven/JUnit test counts.

## Continuous integration and Git workflow

[GitHub Actions CI](.github/workflows/ci.yml) runs a four-service matrix on Ubuntu with Temurin Java 21 and Maven dependency caching. Each matrix entry runs:

```text
mvn --batch-mode clean verify
```

The workflow runs for pushes to `dev` and `main`, and pull requests targeting `dev` or `main`. All four jobs must pass; a failed job uploads its Surefire reports. This is continuous integration only—there is no deployment/CD workflow.

The repository contains service-specific feature branches and records pull-request merge commits in its history. The `dev` branch is used for integration, while CI also protects changes proposed to `main`.

## Repository structure

```text
account-service/       Account identity and profile service
driver-service/        Driver and vehicle operations service
ride-service/          Ride lifecycle and orchestration service
fare-payment-service/  Fare calculation and simulated payment service
docs/                  Frozen API contracts and architecture documentation
postman/               Collection, safe local environment template, and guide
.github/workflows/     GitHub Actions continuous integration
```

## Documentation

- [API contracts](docs/API_CONTRACTS.md) — frozen endpoints, DTOs, statuses, security, and interservice contracts
- [Architecture](docs/ARCHITECTURE.md) — Mermaid system, ownership, sequence, lifecycle, security, database, and CI diagrams
- [Postman guide](postman/README.md) — importing, executing, resetting, and understanding the integration suite

## Known assignment simplifications

- Driver location is simulated rather than supplied by a live tracking system.
- Driver assignment uses exact/simple `serviceArea` matching and selects the first eligible driver.
- Payments are deterministic simulations; there is no real payment gateway.
- Interservice communication is synchronous REST without a distributed transaction coordinator.
- Services share one symmetric JWT secret; automated key rotation and an external identity provider are outside scope.
- No frontend is required or included.
- Cleanup APIs are not part of the assignment, so Postman-created test data persists in the owning Atlas databases.

## Assignment scope and compliance

- The repository contains exactly four Java 21/Spring Boot microservices.
- Persistence is database-per-service with no cross-service repository or MongoDB access.
- The backend does not use MERN, Node.js, or Express and does not require a frontend.
- Swagger/OpenAPI and the Postman collection provide interactive demonstration coverage.
- Service-specific feature branches and pull-request merge commits are present in repository history.
- GitHub Actions verifies all four Maven projects; it does not perform deployment.
