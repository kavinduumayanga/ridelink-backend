# RideLink API Contracts

> **Version:** 1.0  
> **Status:** AGREED — ready for implementation  
> **Last updated:** 2026-09-27

This document defines every REST endpoint, request/response shape, status code, and interservice contract for RideLink.
Each developer must implement their service to match these contracts exactly.
Field names, JSON keys, enum values, and status codes are **frozen** — do not invent alternatives.

---

## Table of Contents

1. [Conventions](#1-conventions)
2. [Account Service — port 8081](#2-account-service--port-8081)
3. [Driver & Vehicle Service — port 8082](#3-driver--vehicle-service--port-8082)
4. [Fare & Payment Service — port 8084](#4-fare--payment-service--port-8084)
5. [Ride Management Service — port 8083](#5-ride-management-service--port-8083)
6. [Interservice Contracts](#6-interservice-contracts)
7. [End-to-End Ride Workflow](#7-end-to-end-ride-workflow)

---

## 1. Conventions

### 1.1 Base URLs (local development)

| Service                | Base URL                  |
| ---------------------- | ------------------------- |
| Account Service        | `http://localhost:8081`    |
| Driver & Vehicle       | `http://localhost:8082`    |
| Ride Management        | `http://localhost:8083`    |
| Fare & Payment         | `http://localhost:8084`    |

### 1.2 Common Headers

| Header          | Value                          | When                         |
| --------------- | ------------------------------ | ---------------------------- |
| `Content-Type`  | `application/json`             | All requests with a body     |
| `Authorization` | `Bearer <jwt>`                 | All authenticated endpoints  |

### 1.3 Standard Error Response

Every service must return errors in this shape:

```json
{
  "timestamp": "2026-09-27T17:00:00.000+00:00",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Email is required",
  "path": "/api/accounts/register"
}
```

| `error` code              | HTTP status | Meaning                              |
| ------------------------- | ----------: | ------------------------------------ |
| `VALIDATION_ERROR`        |         400 | Invalid or missing fields            |
| `UNAUTHENTICATED`         |         401 | Missing or invalid JWT               |
| `FORBIDDEN`               |         403 | Valid JWT but insufficient role       |
| `NOT_FOUND`               |         404 | Resource does not exist              |
| `CONFLICT`                |         409 | Duplicate resource or invalid state transition |

### 1.4 Shared Enums

These values are shared across services and must be used exactly as written.

**Roles** (Account Service owns, other services read from JWT):

```
PASSENGER, DRIVER, ADMIN
```

**Account Status:**

```
ACTIVE, INACTIVE
```

**Driver Availability:**

```
AVAILABLE, UNAVAILABLE
```

**Ride Status:**

```
REQUESTED, ASSIGNED, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED
```

**Payment Status:**

```
PENDING, PAID, FAILED
```

### 1.5 ID Conventions

| ID field        | Owned by         | Format                    | Notes                                     |
| --------------- | ---------------- | ------------------------- | ----------------------------------------- |
| `userId`        | Account Service  | MongoDB ObjectId (String) | Identifies a registered account           |
| `driverId`      | Driver Service   | MongoDB ObjectId (String) | Identifies a driver operational profile   |
| `vehicleId`     | Driver Service   | MongoDB ObjectId (String) | Identifies a vehicle                      |
| `rideId`        | Ride Service     | MongoDB ObjectId (String) | Identifies a ride                         |
| `fareId`        | Fare Service     | MongoDB ObjectId (String) | Identifies a fare calculation             |
| `paymentId`     | Fare Service     | MongoDB ObjectId (String) | Identifies a payment                      |
| `accountId`     | (reference)      | Same value as `userId`    | Used in Driver Service to link to Account |

Services exchange these IDs over REST. **No service reads another service's MongoDB database.**

---

## 2. Account Service — port 8081

**Owner:** Nethmi  
**Base path:** `/api/accounts`  
**Database:** `ridelink_account_db`

### 2.1 POST `/api/accounts/register` — Register

Creates a new PASSENGER or DRIVER account.

**Auth:** None

**Request:**

```json
{
  "firstName": "Kavindu",
  "lastName": "Umayanga",
  "email": "kavindu@example.com",
  "phone": "+94771234567",
  "password": "secureP@ss1",
  "role": "PASSENGER"
}
```

| Field       | Type   | Required | Validation                            |
| ----------- | ------ | -------- | ------------------------------------- |
| `firstName` | String | Yes      | Non-blank                             |
| `lastName`  | String | Yes      | Non-blank                             |
| `email`     | String | Yes      | Valid email, unique                    |
| `phone`     | String | Yes      | Non-blank                             |
| `password`  | String | Yes      | Min 8 characters                      |
| `role`      | String | Yes      | `PASSENGER` or `DRIVER`               |

**201 Created:**

```json
{
  "userId": "665f1a2b3c4d5e6f7a8b9c0d",
  "firstName": "Kavindu",
  "lastName": "Umayanga",
  "email": "kavindu@example.com",
  "phone": "+94771234567",
  "role": "PASSENGER",
  "status": "ACTIVE"
}
```

> `passwordHash` is **never** returned in any response.

**Error codes:** `400` duplicate email or validation failure.

---

### 2.2 POST `/api/accounts/login` — Login

Authenticates a user and returns a JWT.

**Auth:** None

**Request:**

```json
{
  "email": "kavindu@example.com",
  "password": "secureP@ss1"
}
```

**200 OK:**

```json
{
  "userId": "665f1a2b3c4d5e6f7a8b9c0d",
  "email": "kavindu@example.com",
  "role": "PASSENGER",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

The JWT payload must contain at minimum:

```json
{
  "sub": "665f1a2b3c4d5e6f7a8b9c0d",
  "role": "PASSENGER",
  "iat": 1719500000,
  "exp": 1719586400
}
```

- `sub` = `userId`
- Other services extract `userId` and `role` from the JWT to authorize requests.

**Error codes:** `401` invalid credentials, `403` account is `INACTIVE`.

---

### 2.3 GET `/api/accounts/{userId}` — Get Profile

**Auth:** Bearer JWT (own account, or ADMIN)

**200 OK:**

```json
{
  "userId": "665f1a2b3c4d5e6f7a8b9c0d",
  "firstName": "Kavindu",
  "lastName": "Umayanga",
  "email": "kavindu@example.com",
  "phone": "+94771234567",
  "role": "PASSENGER",
  "status": "ACTIVE"
}
```

**Error codes:** `401`, `403`, `404`.

---

### 2.4 PUT `/api/accounts/{userId}` — Update Profile

**Auth:** Bearer JWT (own account, or ADMIN)

**Request:**

```json
{
  "firstName": "Kavindu",
  "lastName": "Umayanga",
  "phone": "+94779999999"
}
```

| Field       | Type   | Required | Notes                     |
| ----------- | ------ | -------- | ------------------------- |
| `firstName` | String | Yes      | Non-blank                 |
| `lastName`  | String | Yes      | Non-blank                 |
| `phone`     | String | Yes      | Non-blank                 |

> `email`, `role`, and `status` cannot be changed through this endpoint.

**200 OK:** Returns the full profile DTO (same shape as GET).

**Error codes:** `400`, `401`, `403`, `404`.

---

### 2.5 PATCH `/api/accounts/{userId}/status` — Update Account Status

**Auth:** Bearer JWT (ADMIN only)

**Request:**

```json
{
  "status": "INACTIVE"
}
```

**200 OK:** Returns the full profile DTO.

**Error codes:** `400`, `401`, `403`, `404`.

---

## 3. Driver & Vehicle Service — port 8082

**Owner:** Ridma  
**Base path:** `/api/drivers`  
**Database:** `ridelink_driver_db`

### 3.1 POST `/api/drivers` — Create Driver Profile

Creates the operational profile for a user who registered with role `DRIVER` in Account Service.

**Auth:** Bearer JWT (DRIVER role, own `accountId`)

**Request:**

```json
{
  "accountId": "665f1a2b3c4d5e6f7a8b9c0d",
  "licenseNumber": "DL-123456",
  "serviceArea": "Colombo"
}
```

| Field           | Type   | Required | Notes                                  |
| --------------- | ------ | -------- | -------------------------------------- |
| `accountId`     | String | Yes      | Must match the JWT `sub` (userId)      |
| `licenseNumber` | String | Yes      | Non-blank                              |
| `serviceArea`   | String | Yes      | City or zone name                      |

**201 Created:**

```json
{
  "driverId": "775a1b2c3d4e5f6a7b8c9d0e",
  "accountId": "665f1a2b3c4d5e6f7a8b9c0d",
  "licenseNumber": "DL-123456",
  "serviceArea": "Colombo",
  "availability": "UNAVAILABLE",
  "latitude": 0.0,
  "longitude": 0.0
}
```

**Error codes:** `400` validation, `401`, `403`, `409` profile already exists for this `accountId`.

---

### 3.2 GET `/api/drivers/{driverId}` — Get Driver

**Auth:** Bearer JWT

**200 OK:**

```json
{
  "driverId": "775a1b2c3d4e5f6a7b8c9d0e",
  "accountId": "665f1a2b3c4d5e6f7a8b9c0d",
  "licenseNumber": "DL-123456",
  "serviceArea": "Colombo",
  "availability": "AVAILABLE",
  "latitude": 6.9271,
  "longitude": 79.8612
}
```

**Error codes:** `401`, `404`.

---

### 3.3 POST `/api/drivers/{driverId}/vehicle` — Create Vehicle

**Auth:** Bearer JWT (DRIVER role, own profile)

**Request:**

```json
{
  "licensePlate": "ABC-1234",
  "make": "Toyota",
  "model": "Prius",
  "year": 2022,
  "color": "White",
  "vehicleType": "CAR"
}
```

| Field          | Type    | Required | Notes                        |
| -------------- | ------- | -------- | ---------------------------- |
| `licensePlate` | String  | Yes      | Non-blank, unique            |
| `make`         | String  | Yes      | Non-blank                    |
| `model`        | String  | Yes      | Non-blank                    |
| `year`         | Integer | Yes      | Positive                     |
| `color`        | String  | Yes      | Non-blank                    |
| `vehicleType`  | String  | Yes      | `CAR`, `THREE_WHEELER`, `VAN`, `BIKE` |

**201 Created:**

```json
{
  "vehicleId": "885b2c3d4e5f6a7b8c9d0e1f",
  "driverId": "775a1b2c3d4e5f6a7b8c9d0e",
  "licensePlate": "ABC-1234",
  "make": "Toyota",
  "model": "Prius",
  "year": 2022,
  "color": "White",
  "vehicleType": "CAR"
}
```

**Error codes:** `400`, `401`, `403`, `404` driver not found, `409` vehicle already exists for this driver.

---

### 3.4 PUT `/api/drivers/{driverId}/vehicle` — Update Vehicle

**Auth:** Bearer JWT (DRIVER role, own profile)

**Request:** Same shape as Create Vehicle.

**200 OK:** Returns the updated vehicle DTO.

**Error codes:** `400`, `401`, `403`, `404`.

---

### 3.5 PATCH `/api/drivers/{driverId}/availability` — Update Availability

**Auth:** Bearer JWT (DRIVER role, own profile)

**Request:**

```json
{
  "availability": "AVAILABLE"
}
```

**200 OK:** Returns the full driver DTO.

**Error codes:** `400`, `401`, `403`, `404`.

---

### 3.6 PATCH `/api/drivers/{driverId}/location` — Update Simulated Location

**Auth:** Bearer JWT (DRIVER role, own profile)

**Request:**

```json
{
  "latitude": 6.9271,
  "longitude": 79.8612
}
```

| Field       | Type   | Required | Validation          |
| ----------- | ------ | -------- | ------------------- |
| `latitude`  | Double | Yes      | -90.0 to 90.0       |
| `longitude` | Double | Yes      | -180.0 to 180.0     |

**200 OK:** Returns the full driver DTO.

**Error codes:** `400`, `401`, `403`, `404`.

---

### 3.7 GET `/api/drivers/available` — Get Available Drivers

Returns all drivers with `availability` = `AVAILABLE` in a given `serviceArea`.

**Auth:** Bearer JWT

**Query parameters:**

| Param         | Type   | Required | Notes             |
| ------------- | ------ | -------- | ----------------- |
| `serviceArea` | String | Yes      | Exact match       |

**Example:** `GET /api/drivers/available?serviceArea=Colombo`

**200 OK:**

```json
[
  {
    "driverId": "775a1b2c3d4e5f6a7b8c9d0e",
    "accountId": "665f1a2b3c4d5e6f7a8b9c0d",
    "licenseNumber": "DL-123456",
    "serviceArea": "Colombo",
    "availability": "AVAILABLE",
    "latitude": 6.9271,
    "longitude": 79.8612
  }
]
```

Returns an empty array `[]` if no drivers are available. Never returns `404` for an empty list.

**Error codes:** `400` missing `serviceArea`, `401`.

---

## 4. Fare & Payment Service — port 8084

**Owner:** Pehansa  
**Base path:** `/api/fares` and `/api/payments`  
**Database:** `ridelink_payment_db`

### Fare Formula

```
fare = baseFare + (distanceKm × ratePerKm)
```

The service owns `baseFare` and `ratePerKm` configuration internally (e.g., hardcoded defaults or config properties). Callers do not supply rates.

### 4.1 POST `/api/fares/estimate` — Fare Estimate

Returns an estimated fare before a ride begins.

**Auth:** Bearer JWT

**Request:**

```json
{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "distanceKm": 12.5
}
```

| Field        | Type   | Required | Validation     |
| ------------ | ------ | -------- | -------------- |
| `rideId`     | String | Yes      | Non-blank      |
| `distanceKm` | Double | Yes     | > 0            |

**200 OK:**

```json
{
  "fareId": "aa5d4e5f6a7b8c9d0e1f2a3b",
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "fareType": "ESTIMATE",
  "distanceKm": 12.5,
  "baseFare": 200.00,
  "ratePerKm": 50.00,
  "totalFare": 825.00
}
```

**Error codes:** `400`.

---

### 4.2 POST `/api/fares/final` — Final Fare Calculation

Called when a ride is completed. Produces the authoritative fare.

**Auth:** Bearer JWT

**Request:**

```json
{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "distanceKm": 13.2
}
```

**200 OK:**

```json
{
  "fareId": "bb6e5f6a7b8c9d0e1f2a3b4c",
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "fareType": "FINAL",
  "distanceKm": 13.2,
  "baseFare": 200.00,
  "ratePerKm": 50.00,
  "totalFare": 860.00
}
```

**Error codes:** `400`.

---

### 4.3 POST `/api/payments` — Create Payment

Creates a simulated payment for a completed ride.

**Auth:** Bearer JWT

**Request:**

```json
{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "fareId": "bb6e5f6a7b8c9d0e1f2a3b4c",
  "amount": 860.00,
  "paymentMethod": "CASH"
}
```

| Field           | Type   | Required | Validation                 |
| --------------- | ------ | -------- | -------------------------- |
| `rideId`        | String | Yes      | Non-blank                  |
| `fareId`        | String | Yes      | Non-blank                  |
| `amount`        | Double | Yes      | > 0                        |
| `paymentMethod` | String | Yes      | `CASH` or `CARD_SIMULATED` |

**201 Created:**

```json
{
  "paymentId": "cc7f6a7b8c9d0e1f2a3b4c5d",
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "fareId": "bb6e5f6a7b8c9d0e1f2a3b4c",
  "amount": 860.00,
  "paymentMethod": "CASH",
  "paymentStatus": "PAID",
  "paidAt": "2026-09-27T17:30:00.000+00:00"
}
```

> Simulated: CASH immediately becomes `PAID`; `CARD_SIMULATED` immediately becomes `PAID`. No real gateway.

**Error codes:** `400`.

---

### 4.4 GET `/api/payments/{paymentId}` — Get Payment

**Auth:** Bearer JWT

**200 OK:**

```json
{
  "paymentId": "cc7f6a7b8c9d0e1f2a3b4c5d",
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "fareId": "bb6e5f6a7b8c9d0e1f2a3b4c",
  "amount": 860.00,
  "paymentMethod": "CASH",
  "paymentStatus": "PAID",
  "paidAt": "2026-09-27T17:30:00.000+00:00"
}
```

**Error codes:** `401`, `404`.

---

### 4.5 GET `/api/payments/ride/{rideId}` — Get Payment by Ride

**Auth:** Bearer JWT

**200 OK:** Same shape as Get Payment.

**Error codes:** `401`, `404`.

---

### 4.6 GET `/api/payments/{paymentId}/receipt` — Get Receipt

Returns a receipt summary combining fare and payment data.

**Auth:** Bearer JWT

**200 OK:**

```json
{
  "receiptId": "cc7f6a7b8c9d0e1f2a3b4c5d",
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "distanceKm": 13.2,
  "baseFare": 200.00,
  "ratePerKm": 50.00,
  "totalFare": 860.00,
  "paymentMethod": "CASH",
  "paymentStatus": "PAID",
  "paidAt": "2026-09-27T17:30:00.000+00:00"
}
```

**Error codes:** `401`, `404`.

---

## 5. Ride Management Service — port 8083

**Owner:** Kavindu  
**Base path:** `/api/rides`  
**Database:** `ridelink_ride_db`

### 5.1 Ride State Machine

```
REQUESTED ──→ ASSIGNED ──→ ACCEPTED ──→ IN_PROGRESS ──→ COMPLETED
    │             │            │              │
    └─────────────┴────────────┴──────────────┘
                        │
                    CANCELLED
```

**Valid transitions:**

| From          | To            | Trigger endpoint       |
| ------------- | ------------- | ---------------------- |
| `REQUESTED`   | `ASSIGNED`    | Assign Driver          |
| `ASSIGNED`    | `ACCEPTED`    | Accept Ride            |
| `ACCEPTED`    | `IN_PROGRESS` | Start Ride             |
| `IN_PROGRESS` | `COMPLETED`   | Complete Ride          |
| `REQUESTED`   | `CANCELLED`   | Cancel Ride            |
| `ASSIGNED`    | `CANCELLED`   | Cancel Ride            |
| `ACCEPTED`    | `CANCELLED`   | Cancel Ride            |

**Terminal states:** `COMPLETED`, `CANCELLED` — no further transitions allowed.

Invalid transitions return `409 CONFLICT`.

---

### 5.2 POST `/api/rides` — Create Ride

Creates a new ride request. The Ride Service calls Fare Service for an estimate.

**Auth:** Bearer JWT (PASSENGER role)

**Request:**

```json
{
  "passengerId": "665f1a2b3c4d5e6f7a8b9c0d",
  "pickupLocation": "Colombo Fort",
  "dropoffLocation": "Bambalapitiya",
  "pickupLatitude": 6.9340,
  "pickupLongitude": 79.8428,
  "dropoffLatitude": 6.8942,
  "dropoffLongitude": 79.8558,
  "distanceKm": 5.5
}
```

| Field              | Type   | Required | Validation                        |
| ------------------ | ------ | -------- | --------------------------------- |
| `passengerId`      | String | Yes      | Must match JWT `sub`              |
| `pickupLocation`   | String | Yes      | Non-blank                         |
| `dropoffLocation`  | String | Yes      | Non-blank                         |
| `pickupLatitude`   | Double | Yes      | -90.0 to 90.0                     |
| `pickupLongitude`  | Double | Yes      | -180.0 to 180.0                   |
| `dropoffLatitude`  | Double | Yes      | -90.0 to 90.0                     |
| `dropoffLongitude` | Double | Yes      | -180.0 to 180.0                   |
| `distanceKm`       | Double | Yes      | > 0                               |

**201 Created:**

```json
{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "passengerId": "665f1a2b3c4d5e6f7a8b9c0d",
  "driverId": null,
  "pickupLocation": "Colombo Fort",
  "dropoffLocation": "Bambalapitiya",
  "pickupLatitude": 6.9340,
  "pickupLongitude": 79.8428,
  "dropoffLatitude": 6.8942,
  "dropoffLongitude": 79.8558,
  "distanceKm": 5.5,
  "status": "REQUESTED",
  "estimatedFare": 475.00,
  "finalFare": null,
  "createdAt": "2026-09-27T17:00:00.000+00:00",
  "updatedAt": "2026-09-27T17:00:00.000+00:00"
}
```

**Error codes:** `400`, `401`, `403`.

---

### 5.3 GET `/api/rides/{rideId}` — Get Ride

**Auth:** Bearer JWT

**200 OK:** Same shape as the ride DTO above.

**Error codes:** `401`, `404`.

---

### 5.4 GET `/api/rides/passenger/{passengerId}` — Get Passenger Rides

Returns all rides for a passenger, newest first.

**Auth:** Bearer JWT (own rides, or ADMIN)

**200 OK:**

```json
[
  { "rideId": "...", "...": "..." }
]
```

Returns an empty array `[]` if no rides exist.

**Error codes:** `401`, `403`.

---

### 5.5 PATCH `/api/rides/{rideId}/assign` — Assign Driver

Ride Service orchestrates assignment: it calls Driver Service `GET /api/drivers/available`, selects the **first eligible driver** returned (deterministic, simple rule), and assigns that `driverId` to the ride. No `driverId` is supplied in the request body — the Ride Service resolves it.

**Auth:** Bearer JWT (PASSENGER who created the ride, or ADMIN)

**Request body:** None (empty body or `{}`)

**200 OK:** Returns the updated ride DTO with `status: "ASSIGNED"` and `driverId` populated.

**Error codes:** `401`, `403`, `404` ride not found, `409` invalid state transition, `404` no available drivers found.

---

### 5.6 PATCH `/api/rides/{rideId}/accept` — Accept Ride

Driver accepts the assigned ride.

**Auth:** Bearer JWT (DRIVER role, must be the assigned driver)

**200 OK:** Returns the updated ride DTO with `status: "ACCEPTED"`.

**Error codes:** `401`, `403`, `404`, `409`.

---

### 5.7 PATCH `/api/rides/{rideId}/start` — Start Ride

**Auth:** Bearer JWT (DRIVER role, must be the assigned driver)

**200 OK:** Returns the updated ride DTO with `status: "IN_PROGRESS"`.

**Error codes:** `401`, `403`, `404`, `409`.

---

### 5.8 PATCH `/api/rides/{rideId}/complete` — Complete Ride

Marks the ride as completed. The Ride Service calls Fare Service to calculate the final fare.

**Auth:** Bearer JWT (DRIVER role, must be the assigned driver)

**200 OK:** Returns the updated ride DTO with `status: "COMPLETED"` and `finalFare` populated.

**Error codes:** `401`, `403`, `404`, `409`.

---

### 5.9 PATCH `/api/rides/{rideId}/cancel` — Cancel Ride

Cancels a ride. Allowed only from `REQUESTED`, `ASSIGNED`, or `ACCEPTED`.

**Auth:** Bearer JWT (PASSENGER who created the ride, or ADMIN)

**200 OK:** Returns the updated ride DTO with `status: "CANCELLED"`.

**Error codes:** `401`, `403`, `404`, `409` ride is in a terminal or non-cancellable state.

---

## 6. Interservice Contracts

Services communicate **only** through the REST endpoints defined in this document.
They exchange **stable IDs** — never raw database documents.
**No service accesses another service's MongoDB database.**

### 6.1 Ride Service → Driver Service

**When:** During driver assignment (PATCH `/api/rides/{rideId}/assign`).

```
GET http://localhost:8082/api/drivers/available?serviceArea=Colombo
```

| Aspect    | Detail                                                             |
| --------- | ------------------------------------------------------------------ |
| Purpose   | Find eligible AVAILABLE drivers in the ride's service area         |
| Caller    | Ride Service                                                       |
| Provider  | Driver Service                                                     |
| Auth      | Ride Service forwards the caller's JWT                             |
| Response  | Array of driver DTOs (see §3.7), each containing `driverId`, `latitude`, `longitude` — enough to select a driver without accessing Driver DB |
| Selection | Ride Service picks the **first driver** in the returned array (deterministic, simple rule) and assigns that `driverId` to the ride |

---

### 6.2 Ride Service → Fare Service (Estimate)

**When:** During ride creation (POST `/api/rides`).

```
POST http://localhost:8084/api/fares/estimate
Content-Type: application/json

{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "distanceKm": 5.5
}
```

| Aspect    | Detail                                                |
| --------- | ----------------------------------------------------- |
| Purpose   | Get estimated fare to display to the passenger        |
| Caller    | Ride Service                                          |
| Provider  | Fare & Payment Service                                |
| Auth      | Ride Service forwards the caller's JWT                |
| Response  | Fare DTO (see §4.1) — Ride Service stores `totalFare` as `estimatedFare` |

---

### 6.3 Ride Service → Fare Service (Final)

**When:** During ride completion (PATCH `/api/rides/{rideId}/complete`).

```
POST http://localhost:8084/api/fares/final
Content-Type: application/json

{
  "rideId": "995c3d4e5f6a7b8c9d0e1f2a",
  "distanceKm": 5.5
}
```

| Aspect    | Detail                                                |
| --------- | ----------------------------------------------------- |
| Purpose   | Calculate the final authoritative fare                |
| Caller    | Ride Service                                          |
| Provider  | Fare & Payment Service                                |
| Auth      | Ride Service forwards the caller's JWT                |
| Response  | Fare DTO (see §4.2) — Ride Service stores `totalFare` as `finalFare` |

---

### 6.4 Interservice Communication Summary

```
┌──────────────┐          GET /api/drivers/available          ┌──────────────────┐
│              │ ──────────────────────────────────────────→  │                  │
│  Ride        │                                              │  Driver &        │
│  Service     │          POST /api/fares/estimate            │  Vehicle Service │
│  :8083       │ ───────────────────────────┐                 │  :8082           │
│              │                            │                 └──────────────────┘
│              │          POST /api/fares/final                
│              │ ───────────────────────────┤                 ┌──────────────────┐
│              │                            └───────────────→ │  Fare & Payment  │
└──────────────┘                                              │  Service :8084   │
                                                              └──────────────────┘

┌──────────────┐
│  Account     │  ← No interservice calls TO Account Service from other services.
│  Service     │     Other services read userId and role from the JWT only.
│  :8081       │
└──────────────┘
```

---

## 7. End-to-End Ride Workflow

This is the complete happy-path sequence using contracted endpoints:

| Step | Actor     | Action                                       | Endpoint                                      |
| ---: | --------- | -------------------------------------------- | --------------------------------------------- |
|    1 | Passenger | Register account                             | `POST /api/accounts/register`                 |
|    2 | Driver    | Register account (role=DRIVER)               | `POST /api/accounts/register`                 |
|    3 | Driver    | Login                                        | `POST /api/accounts/login`                    |
|    4 | Driver    | Create driver profile                        | `POST /api/drivers`                           |
|    5 | Driver    | Add vehicle                                  | `POST /api/drivers/{driverId}/vehicle`        |
|    6 | Driver    | Set availability to AVAILABLE                | `PATCH /api/drivers/{driverId}/availability`  |
|    7 | Driver    | Update simulated location                    | `PATCH /api/drivers/{driverId}/location`      |
|    8 | Passenger | Login                                        | `POST /api/accounts/login`                    |
|    9 | Passenger | Request ride → Ride Svc calls fare estimate  | `POST /api/rides`                             |
|   10 | Passenger | Assign driver → Ride Svc queries available drivers, picks first eligible | `PATCH /api/rides/{rideId}/assign` |
|   11 | Driver    | Accept ride                                  | `PATCH /api/rides/{rideId}/accept`            |
|   12 | Driver    | Start ride                                   | `PATCH /api/rides/{rideId}/start`             |
|   13 | Driver    | Complete ride → Ride Svc calls final fare    | `PATCH /api/rides/{rideId}/complete`          |
|   14 | Passenger | Create payment                               | `POST /api/payments`                          |
|   15 | Passenger | View receipt                                 | `GET /api/payments/{paymentId}/receipt`        |

---

## Appendix: DTO Summary

For quick reference, here are the canonical DTO names each service should use:

| Service          | DTO Name              | Used in                              |
| ---------------- | --------------------- | ------------------------------------ |
| Account          | `AccountResponse`     | Register, Login, Get/Update Profile  |
| Account          | `LoginResponse`       | Login (includes `token`)             |
| Driver & Vehicle | `DriverResponse`      | All driver endpoints                 |
| Driver & Vehicle | `VehicleResponse`     | Vehicle create/update                |
| Fare & Payment   | `FareResponse`        | Estimate and Final fare              |
| Fare & Payment   | `PaymentResponse`     | Create/Get payment                   |
| Fare & Payment   | `ReceiptResponse`     | Get receipt                          |
| Ride             | `RideResponse`        | All ride endpoints                   |

---

*End of API Contracts.*
