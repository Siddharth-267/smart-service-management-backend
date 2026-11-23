
# Smart Service Request Prioritization and Resolution Tracking System

A Java-based backend application designed to manage service requests, prioritize issues, assign staff, track resolutions, and automatically escalate overdue requests.

## Project Overview

Traditional complaint management systems often lack effective prioritization, recurring issue detection, accountability, and proper resolution verification.

This application addresses these challenges through a centralized REST API with role-based access control, rule-based priority calculation, and automated request management workflows.

A key feature is resolution verification: when staff marks a request as resolved, the original requester must verify the resolution before the request can be closed.

## Key Features

- User registration and login with BCrypt password hashing.
- JWT-based authentication and authorization.
- Role-based access for USER, STAFF, and ADMIN.
- Service request creation, retrieval, and tracking.
- Automatic rule-based priority calculation (LOW, MEDIUM, HIGH).
- Recurring issue detection based on category and location.
- Staff assignment and assignment-based access control.
- Request status tracking with status history.
- Resolution verification by the original requester.
- Request reopening when an issue remains unresolved.
- Automatic escalation of overdue requests.
- Ownership validation to prevent unauthorized access.
- Input validation and inactive-account access prevention.

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Backend Framework | Spring Boot |
| Database | MySQL 8 |
| Database Access | Spring JDBC (JdbcTemplate) |
| Security | Spring Security |
| Authentication | JSON Web Token (JWT) |
| Password Security | BCrypt |
| API Testing | Postman |
| Build Tool | Maven |
| IDE | IntelliJ IDEA |

## Application Roles

### USER
- Register and log in.
- Create service requests.
- View personal requests.
- Verify completed requests.
- Close or reopen resolved requests.

### STAFF
- Log in securely.
- View assigned requests.
- Update assigned request statuses.
- Mark requests as resolved.

### ADMIN
- View all users and service requests.
- Access administrative user-management operations.
- Trigger overdue request escalation.
- Access administrative database testing endpoint.

## Priority Management

Requests are prioritized using rule-based business logic.

| Priority | Example conditions | Escalation threshold |
|---|---|---|
| HIGH | Electrical hazards, fire, gas leaks | 2 hours |
| MEDIUM | Water leakage, internet issues, broken facilities | 24 hours |
| LOW | General service issues | 48 hours |

Recurring issues can increase the priority of a request based on the number of matching requests within the defined time window.

## Resolution Workflow

1. User creates a service request.
2. System calculates its priority.
3. Staff handles an assigned request.
4. Staff marks the request as RESOLVED.
5. Original requester verifies the resolution.
6. Requester either closes the request or reopens it if the issue persists.
7. Overdue requests are automatically escalated based on priority.

## Automatic Escalation

The system identifies overdue requests using priority-based time thresholds:

- HIGH: 2 hours.
- MEDIUM: 24 hours.
- LOW: 48 hours.

A scheduled task checks for overdue requests every 5 minutes and automatically escalates eligible requests.

Escalation events are recorded in request status history.

## Security

- JWT-based stateless authentication.
- BCrypt password hashing.
- Role-based endpoint protection.
- Request ownership validation.
- Staff assignment validation.
- Inactive account access prevention.
- Database-backed user-role verification.
- JWT signing secret supplied through an environment variable.
- Database password supplied through an environment variable.

## Database

MySQL database: `smart_service_db`

Main tables:

- `users`
- `request_categories`
- `service_requests`
- `request_assignments`
- `request_status_history`

## Running the Project

### Prerequisites

- Java 17
- MySQL 8
- Maven
- IntelliJ IDEA (or another Java IDE)

### Database Configuration

Create the MySQL database:

```sql
CREATE DATABASE smart_service_db;
```

Configure the database connection in `application.properties`.

Set the following environment variables before running the application:

- `DB_PASSWORD` – MySQL database password.
- `JWT_SECRET` – Securely generated Base64-encoded JWT signing secret.

Do not commit database passwords, JWT secrets, or other credentials to GitHub.

### Start the Application

1. Clone or download the repository.
2. Open the project in IntelliJ IDEA.
3. Configure the required environment variables.
4. Ensure MySQL is running and the database tables are created.
5. Build the project using Maven.
6. Run the main class:

`SmartServiceManagementApplication.java`

The application starts at:

`http://localhost:8080`

## API Documentation

Base URL:

`http://localhost:8080`

Detailed endpoint information, request formats, authentication requirements, and response examples are available in `API_DOCUMENTATION.md`.

## Project Purpose

This project demonstrates Java backend development, REST API design, Spring Boot application development, JDBC database integration, authentication, authorization, business-rule implementation, input validation, and automated workflow management.