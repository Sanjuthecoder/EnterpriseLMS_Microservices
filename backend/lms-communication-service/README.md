# LMS Communication Service

## Architectural Responsibilities
The **LMS Communication Service** handles real-time interactions across the Enterprise LMS platform, specifically focusing on chat functionality for courses. It provides a secure, reliable communication layer that bridges real-time and persistent storage seamlessly.
- **WebSocket Communication**: Enables bidirectional, real-time message exchange between users using STOMP over WebSockets.
- **Persistent Chat History**: Securely persists chat logs (with Base64 encoding to support special characters and emojis) partitioned by company and course to an underlying document store (MongoDB).
- **Scheduled Maintenance**: Automatically purges outdated chat messages to maintain database performance and enforce data retention policies.

This service adheres strictly to the standard layered architecture pattern: `controllers`, `services`, `repositories`, `models`, `exceptions`, `config`, and `mappers`.

## Environment Requirements
To run this service locally, the following requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Database**: MongoDB (Configured via properties or environment variables)
- **Message Broker**: Apache Kafka (if utilized for notifications)

## Configuration
Application configuration must be externalized via `application-{profile}.yml` or `.env` files. 
Do **not** hardcode critical details (e.g., database URIs, broker configurations) within the source code. In production, utilize environment variables or secure secrets management tools (e.g., Vault, Kubernetes Secrets).

## Development Standards
This project strictly follows the Enterprise Java Spring Boot Development Standards:
- **Architecture**: A strict separation of concerns is maintained. Controllers manage WebSocket/HTTP I/O, Services orchestrate business logic and encoding, and Repositories handle BSON persistence.
- **Method & Class Complexity**: Methods are strictly limited to a single responsibility with minimal cyclomatic complexity and are guaranteed to be under 30 lines.
- **SOLID and DRY Principles**: Constructor injection (`@RequiredArgsConstructor`) is used universally. Duplicate logic is minimized.
- **Exception Handling**: A `@RestControllerAdvice` (`GlobalExceptionHandler`) is implemented to gracefully catch and standardize error outputs (e.g., `ResourceNotFoundException`), preventing generic exception leakages.
- **Logging**: Usage of `System.out.println()` and `System.err.println()` is completely eradicated. Structured application logging leverages `SLF4J` via `@Slf4j`. No sensitive information is logged.

## Running Locally
1. Ensure all dependent infrastructure (MongoDB, etc.) is running.
2. Provide the necessary `.env` variables or system properties (e.g., `MONGO_URI`).
3. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
