# RideLink Backend

[![CI](https://github.com/kavinduumayanga/ridelink-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/kavinduumayanga/ridelink-backend/actions/workflows/ci.yml)

RideLink is a university application-development project for a ride-management platform. This repository contains the backend only, organized as four independently executable Spring Boot microservices that communicate through REST/JSON.

## Services and ownership

| Service | Owner | Port | MongoDB database |
| --- | --- | ---: | --- |
| Ride Management Service (`ride-service`) | Kavindu | 8083 | `ridelink_ride_db` |
| Account Service (`account-service`) | Nethmi | 8081 | `ridelink_account_db` |
| Driver & Vehicle Service (`driver-service`) | Ridma | 8082 | `ridelink_driver_db` |
| Fare & Payment Service (`fare-payment-service`) | Pehansa | 8084 | `ridelink_payment_db` |

Each service owns its MongoDB database. A service must never read from or write to another service's database.

## Technologies

- Java 21
- Spring Boot 3 and Maven
- Spring Web and REST/JSON
- Spring Data MongoDB
- Spring Security
- Jakarta Validation
- SpringDoc OpenAPI / Swagger UI
- JUnit 5 and Mockito

## Prerequisites

- JDK 21
- Maven 3.9+
- Access to four independently configured MongoDB databases

Copy `.env.example` to a local `.env` and supply environment-specific values without committing that file. Detailed setup, API, and operational instructions will be added incrementally as the project develops.
