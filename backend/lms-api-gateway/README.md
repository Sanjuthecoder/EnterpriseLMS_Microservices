# LMS API Gateway

## Architectural Responsibilities
The **LMS API Gateway** is the single entry point for all external client traffic into the Enterprise LMS platform. It leverages Spring Cloud Gateway (built on Spring WebFlux) to provide a non-blocking, highly scalable routing mechanism.
- **Dynamic Routing**: Discovers backend microservices dynamically via the Eureka Service Registry and routes incoming HTTP requests to the appropriate service instance.
- **Global Authentication & Authorization**: Utilizes a centralized `JwtAuthenticationFilter` to validate JWT tokens on all incoming requests (excluding public endpoints). It extracts user identity and tenant context, propagating them as HTTP headers (`X-User-Id`, `X-Company-Id`, etc.) to downstream services.
- **Cross-Cutting Concerns**: Handles centralized CORS configurations, rate limiting, and standardizing HTTP responses.

## Environment Requirements
To run this service locally, the following environment requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Service Registry**: The LMS Eureka Server must be running for dynamic routing to succeed.

## Configuration & Security
Application configuration must be externalized strictly via `.yml` or `.env` files. 
- **Secret Management**: Do **not** commit `.env` files or the raw `jwt.secret` into source control. Always utilize injected environment variables for production JWT validation.

## Development Standards
This service complies with the Enterprise Java Spring Boot Development Standards:
- **Reactive Principles**: As a WebFlux application, it avoids blocking operations. Filters (like `JwtAuthenticationFilter`) return `Mono<Void>`.
- **Method & Class Complexity**: Logic within global filters is heavily modularized into smaller private helper methods to ensure cyclomatic complexity remains low and no method exceeds the 30-line threshold.
- **Exception Handling**: Authentication failures manually mutate the `ServerWebExchange` to return precise HTTP 401 Unauthorized statuses without leaking internal token parsing traces.
- **Logging**: Uses `@Slf4j` (or explicit SLF4J loggers) exclusively. `System.out.println()` usage is completely prohibited.

## Running Locally
1. Ensure the Eureka Server is already running.
2. Configure required environment variables (e.g., `JWT_SECRET`).
3. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
