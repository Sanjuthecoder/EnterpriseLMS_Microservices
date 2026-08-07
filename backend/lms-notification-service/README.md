# LMS Notification Service

## Architectural Responsibilities
The **LMS Notification Service** is a decoupled, event-driven component responsible for handling all outbound communications from the Enterprise LMS platform. It listens asynchronously to Kafka topics and dispatches formatted transactional emails to users.
- **Event Consumption**: Securely consumes standard JSON payloads from the `notification-topic` triggered by other microservices.
- **Transactional Emails**: Orchestrates the formatting and dispatching of templated emails for crucial workflows like user onboarding, password resets, and premium subscription upgrades.
- **Resilience**: Designed to gracefully catch serialization issues and unknown event payloads without crashing the main Kafka consumer thread.

It adheres to a streamlined layered architecture specific to event consumers: `consumers`, `dtos`, `services`, and `config`. Note that it intentionally omits the `controllers` and `repositories` layers as it does not expose REST APIs nor persist data directly.

## Environment Requirements
To run this service locally, the following environment requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Message Broker**: Apache Kafka
- **Mail Server**: An SMTP server (like Mailpit/Mailhog for local development or AWS SES/SendGrid for production).

## Configuration & Security
Application configuration should be externalized strictly via `.yml` or `.env` files. 
- **Secret Management**: Do **not** commit `.env` files containing raw SMTP credentials or Kafka configurations into source control. Always utilize injected environment variables or dedicated secret stores for production configurations.

## Development Standards
This project strictly follows the Enterprise Java Spring Boot Development Standards:
- **Architecture**: A clear separation between the message listener (`NotificationConsumer`) and business logic (`EmailService`) is maintained.
- **SOLID and DRY Principles**: Classes favor constructor-based dependency injection (`@RequiredArgsConstructor`) over field injection (`@Autowired`). Method complexities are strictly constrained, utilizing `switch` expressions to handle diverse payloads efficiently.
- **Exception Handling**: Within consumers, serialization exceptions and unknown payloads are explicitly caught and logged gracefully rather than terminating the process thread.
- **Logging**: The usage of `System.out.println()` and `System.err.println()` is strictly forbidden. Structured SLF4J loggers are implemented across the service to trace email dispatches reliably.

## Running Locally
1. Configure required environment variables (e.g., `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`).
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
