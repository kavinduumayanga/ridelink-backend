# RideLink Postman Collection

## Prerequisites

- Java 21 and the four RideLink Spring Boot services.
- MongoDB/Atlas connectivity configured through the repository environment variables.
- Account Service on `http://localhost:8081`.
- Driver Service on `http://localhost:8082`.
- Ride Service on `http://localhost:8083`.
- Fare/Payment Service on `http://localhost:8084`.
- Postman desktop or web client with local-agent access.
- A non-production demo password entered locally in the environment variable `testPassword`. The repository template intentionally leaves it blank.

## Import and select

1. Start all four services.
2. Import `RideLink.postman_collection.json`.
3. Import `RideLink.local.postman_environment.json`.
4. Select **RideLink Local** in Postman.
5. Set a local value for `testPassword` (minimum eight characters). Do not commit an exported environment containing that value.

## Recommended execution order

Run folders in numeric order:

1. `00 - Health / Documentation`
2. `01 - Account Setup`
3. `02 - Driver Setup`
4. `03 - Ride Happy Path`
5. `04 - Payment Happy Path`
6. `05 - Negative Security`
7. `06 - Negative Ride`
8. `07 - Negative Driver`
9. `08 - Negative Account`
10. `09 - Negative Payment`

The **Register PASSENGER** pre-request script creates a unique run identifier, unique emails, service areas, license values, and a syntactically valid nonexistent ID. It also clears runtime IDs and tokens from any prior run.

## Variables populated automatically

The collection captures and reuses:

- `passengerId`, `passengerToken`
- `driverAccountId`, `driverToken`, `driverId`
- `rideId`, `estimatedFare`, `finalFare`, `finalFareId`
- `paymentId`
- second-passenger ownership-test values
- transition and no-driver ride IDs
- `failureRideId`, `failureFinalFare`, `failureFinalFareId`, `failedPaymentId`

JWTs and IDs exist only in the selected local Postman environment after execution; they are blank in the repository template.

## Happy-path workflow

Folders 01–04 register and authenticate the two actors, configure the driver and vehicle, run the full ride lifecycle, and create/retrieve a PAID payment and receipt. Ride completion captures the Fare/Payment-owned `finalFareId`, which payment creation sends as `fareId`.

## Negative-test folders

- **05** verifies missing/malformed JWTs, role enforcement, unchanged protected state, and same-role ownership.
- **06** verifies invalid ride input, an invalid lifecycle transition, and no available driver behavior using dedicated rides.
- **07** verifies coordinate validation and nonexistent drivers.
- **08** verifies invalid login and duplicate-email conflict. The nonexistent-user request is conditional: Account ownership rules require an ADMIN JWT. Set `adminToken` manually only if one is legitimately available; public registration cannot create ADMIN accounts.
- **09** creates a separate completed unpaid ride. It verifies invalid amount, nonexistent fare, non-persistence of rejected payments, deterministic FAILED payment creation/retrieval, null `paidAt`, and receipt rejection with 409.

## Reset and rerun

Atlas retains test records. To rerun safely:

1. Start again at **01 - Account Setup / Register PASSENGER**.
2. That pre-request script generates new emails, service areas, license values, and clears captured runtime variables.
3. Continue in numeric order.
4. Do not rerun payment creation alone for a ride that already has a payment; create a fresh run instead.
5. If running an individual request, confirm all variables it references have already been populated.

Cleanup/delete endpoints are intentionally not part of the assignment, so collection-created accounts, drivers, rides, fares, and payments persist in their owning databases.

