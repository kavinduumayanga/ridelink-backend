# RideLink Agent Guidelines

- Use Java 21, Spring Boot 3.x, and Maven.
- Maintain exactly four core microservices: account, driver, ride, and fare/payment.
- Use MongoDB with independent, service-owned persistence. Never access another service's database.
- Use REST and JSON for current interservice communication.
- Authentication and authorization will use JWT and role-based access control (RBAC).
- Document APIs with Swagger/OpenAPI.
- Test with JUnit 5 and Mockito.
- Do not add a frontend or use MERN, Node.js, or Express.
- Supply secrets through environment variables; never commit credentials, tokens, or secrets.
- Keep code simple, maintainable, and explainable in a university viva.
- Follow SOLID principles and layered architecture where appropriate.
- Use DTOs instead of exposing persistence entities directly.
- Use validation and consistent exception handling.
- Use stable IDs between services.
- Do not introduce unnecessary infrastructure.
- Do not modify another service unless explicitly requested.
- Implement only the requested task.
- Never automatically run `git commit` or `git push`.
- Never generate fake evidence or fake test results.

