# LMS AI Service

## Overview
The `lms-ai-service` is a specialized microservice within the Enterprise LMS architecture. It acts as a dedicated, asynchronous Content Engine that integrates with generative AI (Google Gemini) to automate educational assessments and analyze learner telemetry data.

This service is entirely event-driven, operating in the background to ensure the user-facing platform remains highly responsive. It never manages user sessions, payments, or course persistence directly.

## Key Responsibilities
1. **Automated Assessment Generation**: Consumes course lifecycle events via Kafka and securely generates relevant pre-quizzes and post-quizzes based on course material chunks.
2. **Telemetry Insights Analysis**: Ingests raw video engagement metrics (e.g., skips, rewinds, pauses) and produces instructional design reports and improvement suggestions for content creators.
3. **Idempotency & Quota Management**: Implements strict persistence guards to ensure that expensive AI calls are not redundantly executed during service restarts or consumer rebalances, protecting external API rate limits.

## Architecture & Tech Stack
- **Framework**: Java Spring Boot (Layered Architecture)
- **Database**: MongoDB (`spring-data-mongodb`)
- **Messaging**: Apache Kafka for inter-service event streaming
- **External Integration**: Google Gemini API via lightweight HTTP REST clients (`RestTemplate`)
- **Security**: Stateless architecture relying on gateway-forwarded headers and custom Premium Tier filters for endpoint protection.

## Environment Variables Configuration
To run this service locally or in a containerized environment, the following environment variables must be provided via your environment or secret manager (e.g., Kubernetes Secrets, HashiCorp Vault). 

**Never commit production secrets to version control.**

| Environment Variable | Description |
|----------------------|-------------|
| `GEMINI_API_KEY` | Authentication key for the Google Gemini API. |
| `DB_MONGO_URI` | MongoDB connection URI (Note: AI service writes to specific `svc_ai_*` collections). |
| `KAFKA_BOOTSTRAP_SERVERS` | Comma-separated list of Kafka broker addresses. |
| `COURSE_SERVICE_URL` | Base URL for the internal Course Service. |
| `TELEMETRY_SERVICE_URL`| Base URL for the internal Telemetry Service. |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | URL for the Eureka Service Registry. |

## Development Standards & Best Practices
All contributions to this service must adhere to the defined Enterprise Java Spring Boot Standards:
- **Layered Architecture**: Strict separation of concerns (`controller`, `service`, `repository`, `dto`, `client`).
- **Complexity Guidelines**: Methods should be focused (10-20 lines preferred, max 30). Cyclomatic complexity must remain below 10.
- **Exception Handling**: No localized try-catch blocks for API responses. All errors are routed to the `GlobalExceptionHandler` (`@RestControllerAdvice`).
- **Documentation**: Code should be inherently self-explanatory. Business logic intricacies require inline comments. JavaDoc is strictly mandated for all shared utilities and public APIs.
- **Quality Gates**: CI/CD pipelines enforce an 80%+ test coverage minimum. 

## Local Execution
To run the service locally using Maven:
```bash
mvn spring-boot:run
```
Ensure your local `.env` file is populated with valid sandbox/development credentials before launching.
