# RideLink Architecture

This document describes the architecture implemented in the current RideLink backend. RideLink consists of four independently deployable Spring Boot services that communicate through synchronous HTTP/JSON APIs and own separate MongoDB data stores.

## 1. System architecture

```mermaid
flowchart LR
    subgraph clients["API clients"]
        postman["Postman"]
        swagger["Swagger UI"]
    end

    subgraph services["RideLink Spring Boot services"]
        account["Account Service :8081<br/>Registration and login<br/>JWT issuance and authentication"]
        driver["Driver and Vehicle Service :8082<br/>Driver profile, vehicle,<br/>availability and simulated location"]
        ride["Ride Management Service :8083<br/>Ride lifecycle, assignment<br/>and downstream orchestration"]
        fare["Fare and Payment Service :8084<br/>Fare estimate and final fare<br/>simulated payments and receipts"]
    end

    subgraph atlas["MongoDB Atlas - logically isolated data stores"]
        accountDb[("Account database")]
        driverDb[("Driver database")]
        rideDb[("Ride database")]
        paymentDb[("Fare/Payment database")]
    end

    postman -->|"HTTP/JSON"| account
    postman -->|"HTTP/JSON + Bearer JWT"| driver
    postman -->|"HTTP/JSON + Bearer JWT"| ride
    postman -->|"HTTP/JSON + Bearer JWT"| fare
    swagger -->|"Documents and invokes APIs"| account
    swagger -->|"Documents and invokes APIs"| driver
    swagger -->|"Documents and invokes APIs"| ride
    swagger -->|"Documents and invokes APIs"| fare

    account -.->|"Issues JWT after login"| postman
    ride -->|"GET /api/drivers/available<br/>forwarded caller JWT"| driver
    ride -->|"POST /api/fares/estimate<br/>POST /api/fares/final<br/>forwarded caller JWT"| fare

    account -->|"Own persistence only"| accountDb
    driver -->|"Own persistence only"| driverDb
    ride -->|"Own persistence only"| rideDb
    fare -->|"Own persistence only"| paymentDb
```

The Account Service is the only JWT issuer. Driver, Ride, and Fare/Payment do not call Account to authenticate a request; each validates the signed JWT locally using the shared runtime signing secret. Ride forwards the caller's bearer token when it calls Driver or Fare/Payment.

## 2. Service ownership and stable identifiers

| Service | Owned identifiers and data | External stable references held |
| --- | --- | --- |
| Account | `userId`, account/profile, role, account status, credentials and password hash | None |
| Driver & Vehicle | `driverId`, `vehicleId`, driver profile, vehicle, availability, simulated location, `serviceArea` | `accountId` references Account `userId` |
| Ride | `rideId`, lifecycle state, pickup/drop-off data, distance, `estimatedFare`, `finalFare` | `passengerId` references Account `userId`; `driverId` references Driver `driverId`; `finalFareId` references Fare/Payment `fareId` |
| Fare & Payment | `fareId`, fare calculations, `paymentId`, payment status and receipt data | `rideId` references Ride `rideId` |

```mermaid
flowchart LR
    accountId["Account userId"] -->|"accountId"| driverId["Driver driverId"]
    accountId -->|"passengerId"| rideId["Ride rideId"]
    driverId -->|"driverId"| rideId
    rideId -->|"rideId"| fareId["Fare fareId"]
    fareId -->|"Ride.finalFareId"| rideId
    fareId -->|"fareId"| paymentId["Payment paymentId"]
    rideId -->|"rideId"| paymentId
```

These arrows represent stable string identifiers exchanged through REST DTOs, not database relationships. No service imports another service's persistence entity, repository, collection, or database connection.

## 3. Happy-path sequence

```mermaid
sequenceDiagram
    autonumber
    actor Passenger
    actor Driver
    participant Account as Account Service :8081
    participant DriverSvc as Driver Service :8082
    participant Ride as Ride Service :8083
    participant Fare as Fare/Payment Service :8084

    Passenger->>Account: POST /api/accounts/register (PASSENGER)
    Account-->>Passenger: 201 account with userId
    Passenger->>Account: POST /api/accounts/login
    Account-->>Passenger: 200 passenger JWT

    Driver->>Account: POST /api/accounts/register (DRIVER)
    Account-->>Driver: 201 account with userId
    Driver->>Account: POST /api/accounts/login
    Account-->>Driver: 200 driver JWT

    Driver->>DriverSvc: POST /api/drivers<br/>Bearer driver JWT + accountId
    DriverSvc-->>Driver: 201 driverId, UNAVAILABLE
    Driver->>DriverSvc: POST /api/drivers/{driverId}/vehicle<br/>Bearer driver JWT
    DriverSvc-->>Driver: 201 vehicle
    Driver->>DriverSvc: PATCH /api/drivers/{driverId}/location<br/>Bearer driver JWT
    DriverSvc-->>Driver: 200 simulated location
    Driver->>DriverSvc: PATCH /api/drivers/{driverId}/availability<br/>Bearer driver JWT, AVAILABLE
    DriverSvc-->>Driver: 200 AVAILABLE

    Passenger->>Ride: POST /api/rides<br/>Bearer passenger JWT
    Ride->>Fare: POST /api/fares/estimate<br/>rideId + distanceKm + forwarded JWT
    Fare-->>Ride: 200 fare response with totalFare
    Ride-->>Passenger: 201 REQUESTED + estimatedFare

    Passenger->>Ride: PATCH /api/rides/{rideId}/assign<br/>Bearer passenger JWT
    Ride->>DriverSvc: GET /api/drivers/available?serviceArea=...<br/>forwarded JWT
    DriverSvc-->>Ride: 200 eligible AVAILABLE drivers
    Note over Ride: Select the first returned driver
    Ride-->>Passenger: 200 ASSIGNED + driverId

    Driver->>Ride: PATCH /api/rides/{rideId}/accept<br/>Bearer driver JWT
    Note over Ride,DriverSvc: Ride authorizes the assigned driver by resolving<br/>driverId to accountId through GET /api/drivers/{driverId}
    Ride-->>Driver: 200 ACCEPTED
    Driver->>Ride: PATCH /api/rides/{rideId}/start<br/>Bearer driver JWT
    Ride-->>Driver: 200 IN_PROGRESS
    Driver->>Ride: PATCH /api/rides/{rideId}/complete<br/>Bearer driver JWT
    Ride->>Fare: POST /api/fares/final<br/>rideId + distanceKm + forwarded JWT
    Fare-->>Ride: 200 authoritative fareId + totalFare
    Note over Ride: Store fareId as finalFareId<br/>and totalFare as finalFare
    Ride-->>Driver: 200 COMPLETED

    Passenger->>Fare: POST /api/payments<br/>rideId + finalFareId + finalFare<br/>Bearer passenger JWT
    Fare-->>Passenger: 201 paymentId + PAID
    Passenger->>Fare: GET /api/payments/{paymentId}/receipt<br/>Bearer passenger JWT
    Fare-->>Passenger: 200 receipt for the PAID payment
```

The Driver ownership check compares the JWT `sub` (Account `userId`) with the Driver profile's `accountId`; it does not incorrectly compare the Account `userId` with the distinct `driverId`.

## 4. Ride state machine

```mermaid
stateDiagram-v2
    [*] --> REQUESTED
    REQUESTED --> ASSIGNED: assign driver
    ASSIGNED --> ACCEPTED: assigned driver accepts
    ACCEPTED --> IN_PROGRESS: assigned driver starts
    IN_PROGRESS --> COMPLETED: assigned driver completes

    REQUESTED --> CANCELLED: passenger or admin cancels
    ASSIGNED --> CANCELLED: passenger or admin cancels
    ACCEPTED --> CANCELLED: passenger or admin cancels

    COMPLETED --> [*]
    CANCELLED --> [*]
```

`COMPLETED` and `CANCELLED` are terminal states. A lifecycle request that does not match an allowed transition returns `409 CONFLICT` and does not advance the ride.

## 5. Security flow

```mermaid
flowchart LR
    login["Account login"] --> issuer["Account signs JWT with HS256"]
    issuer --> claims["Claims<br/>sub = userId<br/>role = PASSENGER, DRIVER, or ADMIN<br/>iat and exp"]
    claims --> bearer["Authorization: Bearer token"]
    bearer --> driver["Driver validates locally"]
    bearer --> ride["Ride validates locally"]
    bearer --> fare["Fare/Payment validates locally"]
    ride -->|"Forwards caller token"| driver
    ride -->|"Forwards caller token"| fare
```

- All four services use the same `JWT_SECRET` value at runtime.
- The secret is read as UTF-8 and must contain at least 32 bytes for HS256.
- The secret is supplied through the environment and is never committed to the repository.
- Account issues tokens with `sub = userId`, a `role` claim containing `PASSENGER`, `DRIVER`, or `ADMIN`, and an expiration.
- Driver, Ride, and Fare/Payment verify the signature and expiration locally. They do not query Account during authentication.
- Authorities are represented as `ROLE_PASSENGER`, `ROLE_DRIVER`, and `ROLE_ADMIN` inside Spring Security.
- Missing, malformed, invalid, or expired credentials produce `401 UNAUTHENTICATED`.
- A valid token without the required role or resource ownership produces `403 FORBIDDEN`.
- Registration, login, and the intentionally public Swagger/OpenAPI paths do not require a bearer token. Business endpoints retain their contracted authentication and authorization rules.

## 6. Fare and payment flow

The Fare/Payment Service owns fare calculation and persists both estimated and final fare records. Its configured calculation is:

```text
fare = baseFare + (distanceKm * ratePerKm)
```

```mermaid
flowchart TD
    create["Ride creation"] --> estimate["POST /api/fares/estimate<br/>rideId + distanceKm"]
    estimate --> estimated["Ride stores totalFare<br/>as estimatedFare"]

    complete["Ride completion"] --> final["POST /api/fares/final<br/>rideId + distanceKm"]
    final --> authoritative["Fare Service returns<br/>fareId + totalFare"]
    authoritative --> stored["Ride stores<br/>finalFareId + finalFare"]

    stored --> payment["POST /api/payments<br/>rideId + fareId=finalFareId<br/>amount=finalFare"]
    payment --> decision{"simulateFailure?"}
    decision -->|"absent or false"| paid["Persist PAID<br/>paidAt populated"]
    decision -->|"true"| failed["Persist FAILED<br/>paidAt null"]
    paid --> receipt["Receipt available"]
    failed --> conflict["Receipt request returns<br/>409 CONFLICT"]
```

`finalFareId` is an external stable identifier owned and generated by Fare/Payment. It is `null` in Ride before successful final-fare calculation. Ride never generates a replacement fare ID. Payment creation validates that the referenced fare exists even when failure simulation is requested.

## 7. Database isolation

```mermaid
flowchart TB
    subgraph servicePersistence["Exclusive persistence access"]
        account["Account Service"] --> accountDb[("Account DB only")]
        driver["Driver Service"] --> driverDb[("Driver DB only")]
        ride["Ride Service"] --> rideDb[("Ride DB only")]
        fare["Fare/Payment Service"] --> paymentDb[("Fare/Payment DB only")]
    end

    subgraph restOnly["Cross-service communication - HTTP/REST only"]
        rideRest["Ride Service"] -->|"GET available drivers"| driverRest["Driver Service"]
        rideRest -->|"POST fare estimate/final"| fareRest["Fare/Payment Service"]
    end
```

The service-to-database arrows above are deliberately one-to-one. Cross-service references remain string IDs in service-owned documents; they are not MongoDB joins, foreign-key constraints, shared collections, or repository access.

## 8. Deployment and local development

| Service | Local address | Persistence setting |
| --- | --- | --- |
| Account | `http://localhost:8081` | `ACCOUNT_MONGODB_URI` |
| Driver & Vehicle | `http://localhost:8082` | `DRIVER_MONGODB_URI` |
| Ride | `http://localhost:8083` | `RIDE_MONGODB_URI` |
| Fare & Payment | `http://localhost:8084` | `PAYMENT_MONGODB_URI` |

MongoDB Atlas is the external persistence platform, with a logically isolated database configured for each service. Runtime configuration uses these environment-variable names without embedding values in source control:

| Environment variable | Purpose |
| --- | --- |
| `ACCOUNT_MONGODB_URI` | Account-owned MongoDB connection |
| `DRIVER_MONGODB_URI` | Driver-owned MongoDB connection |
| `RIDE_MONGODB_URI` | Ride-owned MongoDB connection |
| `PAYMENT_MONGODB_URI` | Fare/Payment-owned MongoDB connection |
| `JWT_SECRET` | Shared JWT signing and validation secret |
| `DRIVER_SERVICE_URL` | Driver base URL used by Ride; defaults locally to port 8082 |
| `FARE_PAYMENT_SERVICE_URL` | Fare/Payment base URL used by Ride; defaults locally to port 8084 |

No API Gateway, service registry, message broker, container platform, frontend, Redis, or gRPC component is required by this implementation.

## 9. Continuous integration

```mermaid
flowchart TD
    change["Push or pull request<br/>dev or main"] --> actions["GitHub Actions<br/>Ubuntu + Java 21<br/>Maven dependency cache"]
    actions --> account["Account<br/>mvn --batch-mode clean verify"]
    actions --> driver["Driver<br/>mvn --batch-mode clean verify"]
    actions --> ride["Ride<br/>mvn --batch-mode clean verify"]
    actions --> fare["Fare/Payment<br/>mvn --batch-mode clean verify"]
    account --> result{"All four matrix jobs pass?"}
    driver --> result
    ride --> result
    fare --> result
    result -->|"Yes"| pass["CI passes"]
    result -->|"No"| fail["CI fails<br/>failed-job Surefire report uploaded"]
```

The workflow uses a four-entry matrix, Temurin Java 21, Maven caching, and `mvn --batch-mode clean verify` from each service directory. Every matrix job must pass. Surefire reports are uploaded only for failed jobs.

Current automated test totals, derived from the repository's generated Surefire reports after successful `clean verify` runs, are:

| Matrix service | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Account | 86 | 0 | 0 | 0 |
| Driver | 102 | 0 | 0 | 0 |
| Ride | 90 | 0 | 0 | 0 |
| Fare/Payment | 72 | 0 | 0 | 0 |
| **Total** | **350** | **0** | **0** | **0** |

CI uses a clearly fake test-only JWT secret that meets the minimum key length. It does not use Atlas credentials, start the services, or require external RideLink services.

## 10. Current implementation boundaries

- Driver assignment intentionally selects the first available driver returned for an exact `serviceArea` match; there is no geospatial ranking or dispatch optimization.
- The Ride implementation currently uses the ride's `pickupLocation` value as the Driver `serviceArea` query value, so demonstrated data must use matching area names.
- Payments are deterministic simulations (`PAID` or `FAILED`); no external payment provider is integrated.
- JWT trust uses one shared symmetric runtime secret. There is no identity-provider discovery or automated key rotation in the assignment scope.
- The APIs provide no cross-service transaction coordinator. Each service protects its own consistency boundary and communicates synchronously over REST.
