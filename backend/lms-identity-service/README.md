# LMS Identity Service

## Architectural Responsibilities
The **LMS Identity Service** serves as the central authentication, authorization, and multi-tenant management hub for the Enterprise LMS. It orchestrates identity lifecycle management securely and efficiently.
- **Authentication & Authorization**: Manages secure logins, JWT token generation, password resets, and role-based access control (RBAC) across diverse user hierarchies (Super Admin, Company Admin, Creator, Employee).
- **Tenant Management**: Handles the isolation and structural representation of organizations and their nested companies (multi-tenancy).
- **Theme Configurations**: Supplies dynamically resolved UI thematic configurations based on the authenticated tenant.

The service respects a standard domain-driven layered architecture: `config`, `controllers`, `dtos`, `exceptions`, `kafka`, `mappers`, `models`, `repositories`, `security`, and `services`.

## Environment Requirements
To run this service locally, the following environment requirements apply:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Databases**: Relational Store (e.g., MySQL via TiDB Cloud) and Document Store (e.g., MongoDB Atlas) 
- **Message Broker**: Apache Kafka (for asynchronous notification publishing)

## Configuration & Security
Application configuration must be externalized via `.yml` or `.env` files. 
- **Secret Management**: Do **not** commit `.env` files or raw credentials to source control. In production environments, credentials, database URIs, and JWT signing keys should be retrieved from environment variables or secure vault systems (e.g., HashiCorp Vault, Kubernetes Secrets).

## Development Standards
This service enforces the Enterprise Java Spring Boot Development Standards:
- **Layering Strictness**: Controllers act purely as I/O boundary layers and must not interact directly with repositories. All database and business logic occurs exclusively within the Service layer.
- **SOLID and DRY Principles**: Constructor-based dependency injection (`@RequiredArgsConstructor`) is used system-wide. Lengthy authentication chains are broken down into smaller, strictly scoped private methods.
- **Exception Handling**: Utilizing `@ControllerAdvice` (`GlobalExceptionHandler`), domain exceptions (like `ResourceNotFoundException` and `InvalidCredentialsException`) are securely and uniformly transformed into sanitized JSON responses mapping to proper HTTP statuses (401, 403, 404, 409).
- **Logging**: Standard SLF4J loggers (`@Slf4j`) are used for capturing runtime behaviors and debug traces. The usage of `System.out.println()` and `System.err.println()` is strictly forbidden.

## Running Locally
1. Configure required environment variables (e.g., `DB_MYSQL_URL`, `DB_MONGO_URI`, `JWT_SECRET_KEY`).
2. Ensure external dependencies (Databases, Kafka) are reachable.
3. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
