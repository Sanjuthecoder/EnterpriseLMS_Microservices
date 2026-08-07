# LMS Eureka Server

## Architectural Responsibilities
The **LMS Eureka Server** acts as the central service registry and discovery backbone for the Enterprise LMS microservices ecosystem. It allows all other microservices to locate each other dynamically, eliminating the need for hardcoded hostnames and ports.
- **Service Registration**: Microservices automatically register themselves with Eureka upon startup.
- **Service Discovery**: Allows API Gateway and other services to discover healthy instances of dependent services dynamically.
- **Health Monitoring**: Maintains a heartbeat check with all registered instances, automatically removing instances that become unresponsive.

As a pure infrastructure service provided by Netflix OSS/Spring Cloud, it only requires configuration and does not contain custom business logic, controllers, or database repositories.

## Environment Requirements
To run this service locally, the following environment requirements must be met:
- **Java**: JDK 17+
- **Build Tool**: Maven

## Configuration & Security
By default, the Eureka server runs on port `8761`. 
- For production environments, the dashboard and registration endpoints must be secured (e.g., using Spring Security basic authentication) to prevent unauthorized service registration.
- Do not commit any `.env` files containing environment specific credentials.

## Running Locally
1. Start the Eureka server before launching any other microservices:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Access the Eureka Dashboard via your browser at `http://localhost:8761` to view registered services.
