# LMS Course Service

## Overview
The `lms-course-service` is the central orchestration hub for the Enterprise LMS. It manages course content, structures, enrollments, quiz evaluations, and coordinates asynchronous interactions with the AI and Telemetry services. 

This service enforces the business logic around personalized learning paths (gating), premium content delivery, and tier-based routing for AI assessments.

## Key Features
- **Multi-Tenant Course Delivery**: Segregates course access, structures, and analytics based on `companyId` and `employeeId`.
- **Smart Quiz Routing**: Dynamically serves AI-generated assessments or creator-authored fallbacks based on a user's subscription tier.
- **Personalized Gating Engine**: Evaluates quiz performance, hesitation (answer flips), and cognitive load (time spent) to determine if subsequent lessons should be enforced or optional.
- **Event-Driven Architecture**: Uses Apache Kafka for asynchronous course lifecycle broadcasting and to trigger the AI Quiz generator.

## Architecture & Tech Stack
- **Framework**: Java 17, Spring Boot 3.x
- **Database**: PostgreSQL (Relational persistence for courses, enrollments, and structured content)
- **Messaging**: Apache Kafka (Producers and Consumers)
- **Security**: Stateless API design. Assumes incoming requests are pre-validated by the API Gateway (Trust-the-Gateway pattern), relying on forwarded headers (`X-Employee-Id`, `X-Company-Id`).

## Environment Variables
To run this service, you need to configure the following environment variables (do not commit actual secrets to version control):

| Environment Variable | Description |
|----------------------|-------------|
| `DB_JDBC_URL` | PostgreSQL connection URL. |
| `DB_USERNAME` | PostgreSQL database user. |
| `DB_PASSWORD` | PostgreSQL database password. |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker endpoints. |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL for the Eureka Service Registry. |

## Development Standards
All development must adhere to the defined Enterprise Java Spring Boot Standards:
- **Architecture**: Strict layering (`controller`, `service`, `repository`, etc.). No business logic in controllers. No database calls outside of repositories.
- **Exception Handling**: Global exception handling is enforced via `GlobalExceptionHandler` (`@RestControllerAdvice`). Localized try-catch blocks in controllers are prohibited.
- **Coding Style**:
  - Cyclomatic complexity < 10.
  - Method length < 30 lines.
  - Constructor injection (`@RequiredArgsConstructor`) instead of field-level `@Autowired`.
  - Immutable configurations (`final` wherever possible).
- **Logging**: Uses SLF4J. `System.out.println` is explicitly banned.

## Local Execution
To run the service locally using Maven:
```bash
mvn spring-boot:run
```
Ensure your local `.env` or IDE configuration is populated with the correct sandbox/development credentials before launching.
