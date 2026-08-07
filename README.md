# Enterprise LMS

Enterprise LMS is a highly scalable, microservices-based Learning Management System designed to deliver adaptive learning experiences, real-time analytics, and automated quiz generation. Built with a robust technology stack including Spring Boot, React, Apache Kafka, and Google Gemini AI, it is structured to handle enterprise-level traffic and requirements.

## 🏗 Architecture Overview

The system is designed around a microservices architecture pattern, ensuring that each domain of the application is isolated, scalable, and independently deployable. 
All services communicate through the API Gateway, and asynchronous events are handled via Apache Kafka. Service discovery is managed by Eureka Server.

## 🚀 Key Technologies
- **Backend:** Java 17, Spring Boot, Spring Cloud, Spring Security
- **Frontend:** React, Vite, Context API
- **Databases:** PostgreSQL (Relational Data), MongoDB (NoSQL/Telemetry Data)
- **Message Broker:** Apache Kafka
- **AI Integration:** Google Gemini API
- **Infrastructure:** Docker, Docker Compose, Nginx

## 🧩 Microservices

1. **API Gateway (`lms-api-gateway`):**
   - Central entry point for all frontend client requests.
   - Handles route mapping and basic CORS configurations.

2. **Service Registry (`lms-eureka-server`):**
   - Acts as the discovery server for all microservices, allowing them to locate each other dynamically without hardcoded IPs.

3. **Identity Service (`lms-identity-service`):**
   - Manages user authentication and authorization using JWTs.
   - Handles roles (e.g., Student, Instructor, Admin).

4. **Course Service (`lms-course-service`):**
   - Manages the core entities: courses, lessons, and quizzes.
   - Handles multimedia uploads and course structuring.

5. **Telemetry Service (`lms-telemetry-service`):**
   - Event-driven service capturing user interactions (e.g., video watch times, quiz completions) asynchronously.
   - Utilizes MongoDB for high-throughput write operations.

6. **AI Service (`lms-ai-service`):**
   - Integrates with Google Gemini AI to auto-generate quizzes based on course content.
   - Orchestrated via Kafka to ensure non-blocking, reliable content creation pipelines.

7. **Notification Service (`lms-notification-service`):**
   - Listens to Kafka topics and dispatches emails (via Mailpit in development) or other alerts to users.

8. **Payment Service (`lms-payment-service`):**
   - Handles subscriptions and payment processing lifecycles for premium courses.

9. **Communication Service (`lms-communication-service`):**
   - Facilitates real-time WebSockets-based communication for in-course chat and Q&A.

## ⚙️ Getting Started

### Prerequisites
- Docker and Docker Compose installed.
- (Optional) Java 17 and Node.js if you want to run services individually.

### Running the Project

The entire application can be started locally using Docker Compose:

```bash
# Start all microservices in the background
docker-compose up -d
```

### Accessing the Services
- **Frontend App:** http://localhost:5173
- **Eureka Server UI:** http://localhost:8761
- **API Gateway:** http://localhost:8080
- **Mailpit (Email Testing UI):** http://localhost:8025

## 🔒 Security & Best Practices
- **No Secrets in Repo:** All `.env` files are excluded from source control. Provide your own keys (e.g., `GEMINI_API_KEY`) via environment variables in production.
- **Event-Driven Architecture:** High-throughput components are decoupled using Apache Kafka to improve fault tolerance and response times.
- **Polyglot Persistence:** We use the right database for the right job (PostgreSQL for transactional ACID compliance, MongoDB for massive telemetry logs).
