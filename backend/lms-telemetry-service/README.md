# LMS Telemetry Service

## Architectural Responsibilities
The **LMS Telemetry Service** is responsible for collecting, persisting, and providing telemetry data and insights across the Enterprise LMS platform. It acts as the central hub for learning analytics and tracking user progress.
- **Video Telemetry**: Processes events related to video playback, completions, seeks, and pauses, received via REST or asynchronous Kafka messaging.
- **xAPI Statements**: Captures standard xAPI statements for tracking user interactions and learning outcomes.
- **Aggregations**: Computes aggregations on telemetry data for generating course and user level insights.

It strictly adheres to a layered architecture pattern: `controllers`, `services`, `repositories`, `models`, `exceptions`, `mappers`, and `consumers`.

## Environment Requirements
To run this service locally, the following requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Database**: External Data Store (Configured via properties or environment variables)
- **Message Broker**: Apache Kafka

## Configuration
Application configuration should be externalized via `application-{profile}.yml` files. 
Do **not** hardcode credentials (e.g., database passwords, Kafka broker credentials) in the properties files. Instead, rely on environment variables, secrets management tools like HashiCorp Vault, or Kubernetes Secrets for production environments.

## Development Standards
This project strictly follows the Enterprise Java Spring Boot Development Standards:
- **Architecture**: Enforces a strict separation of concerns among layers. Controllers and Consumers only handle I/O and delegate logic to Services. Repositories handle only database interactions.
- **Methods**: Restricted to a single responsibility. Length is kept under 30 lines, and cyclomatic complexity is minimized.
- **SOLID and DRY**: Redundant code is extracted into shared private or utility methods. Constructors (`@RequiredArgsConstructor`) are favored for dependency injection.
- **Exception Handling**: Global exception handling is enforced using `@ControllerAdvice`. Domain exceptions like `ResourceNotFoundException` are mapped to the correct HTTP status codes.
- **Logging & System Outputs**: The usage of `System.out.println()` is strictly forbidden. Instead, `SLF4J` logging (`@Slf4j`) with structured logs is utilized. No PII or credentials should be logged.

## Running Locally
1. Configure required environment variables (e.g., `DB_URL`, `DB_USER`, `DB_PASS`, `KAFKA_BOOTSTRAP_SERVERS`).
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
