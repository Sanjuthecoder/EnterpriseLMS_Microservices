# LMS Payment Service

## Architectural Responsibilities
The **LMS Payment Service** is responsible for managing financial transactions, premium subscriptions, and payment verifications across the Enterprise LMS platform. It acts as the secure intermediary between the platform and external payment gateways (e.g., Razorpay).
- **Order Management**: Creates secure payment orders and synchronizes them with the payment gateway.
- **Transaction Verification**: Cryptographically verifies payment signatures to prevent tampering or fraudulent activations.
- **Event Orchestration**: Publishes Kafka events upon successful payment to trigger downstream actions, such as sending confirmation emails (via Notification Service) and upgrading tenant tiers (via Identity Service).

The service strictly implements the domain-driven layered architecture: `controllers`, `dtos`, `entities`, `exceptions`, `repositories`, and `services`.

## Environment Requirements
To run this service locally, the following environment requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven
- **Database**: PostgreSQL (or compatible SQL store)
- **Message Broker**: Apache Kafka
- **Payment Gateway**: An active merchant account (e.g., Razorpay) for generating API keys.

## Configuration & Security
Application configuration must be externalized strictly via `.yml` or `.env` files. 
- **Secret Management**: Do **not** commit `.env` files or raw payment gateway keys (e.g., `razorpay.key-secret`) into source control. Always utilize injected environment variables or dedicated secret stores (like HashiCorp Vault or Kubernetes Secrets) for production configurations.

## Development Standards
This project complies strictly with the Enterprise Java Spring Boot Development Standards:
- **Architecture**: A strict separation of concerns is maintained. Controllers manage HTTP boundaries, while Services handle transaction contexts (`@Transactional`) and external API orchestration.
- **Method & Class Complexity**: Lengthy integrations are broken down into small, highly cohesive helper methods. Cyclomatic complexity is strictly constrained (guaranteeing methods are under 30 lines).
- **Exception Handling**: The `@RestControllerAdvice` (`GlobalExceptionHandler`) catches gateway failures and signature mismatches, translating them into secure, standardized JSON HTTP responses (400, 500) without exposing internal stack traces.
- **Logging**: The usage of `System.out.println()` and `System.err.println()` is completely eradicated. Structured SLF4J (`@Slf4j`) logging is implemented universally to trace payment flows without logging sensitive PII or raw transaction secrets.

## Running Locally
1. Configure required environment variables (e.g., `DB_URL`, `KAFKA_BOOTSTRAP_SERVERS`, `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`).
2. Start the application:
   ```bash
   ./mvnw spring-boot:run
   ```
