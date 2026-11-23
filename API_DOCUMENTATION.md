# API Documentation
## Smart Service Request Prioritization and Resolution Tracking System

**Base URL:** `http://localhost:8080`

**API Format:** REST / JSON

**Authentication:** JWT Bearer Token

---

## 1. Authentication APIs

### Login

- **Method:** POST
- **Endpoint:** `/api/auth/login`
- **Access:** Public

Request body:

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

Returns user information and a JWT authentication token.

### User Registration

- **Method:** POST
- **Endpoint:** `/api/users/register`
- **Access:** Public

Request body:

```json
{
  "name": "Test User",
  "email": "user@example.com",
  "password": "password123"
}
```

Public registration creates a USER account. Passwords are securely hashed using BCrypt.

---

## 2. User Management APIs

All endpoints in this section require ADMIN authorization.

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/users` | Retrieve all users |
| GET | `/api/users/{id}` | Retrieve user by ID |
| PUT | `/api/users/{id}` | Update user details |
| PATCH | `/api/users/{id}/deactivate` | Deactivate a user |

User responses do not expose password hashes.

---

## 3. Service Request APIs

These endpoints require JWT authentication and enforce role-based access and ownership restrictions.

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/requests` | Create a service request |
| GET | `/api/requests` | Retrieve accessible requests |
| GET | `/api/requests/{id}` | Retrieve a specific request |

### Create Service Request

- **Method:** POST
- **Endpoint:** `/api/requests`

Request body:

```json
{
  "userId": 11,
  "categoryId": 1,
  "title": "Water leakage in hostel",
  "description": "Water is leaking from the bathroom pipe.",
  "location": "Hostel Block A"
}
```

The authenticated user's ID is used as the request owner. A supplied user ID cannot override JWT identity.

The system:
- Validates request information.
- Calculates priority using rule-based business logic.
- Checks for recurring issues based on category and location.
- Stores the new service request.

### Request Access Rules

- USER: Can view personal requests only.
- STAFF: Can view requests assigned to them.
- ADMIN: Can view all requests.

---

## 4. Staff Assignment APIs

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/assignments` | Create a staff assignment |
| GET | `/api/assignments/request/{requestId}` | Retrieve assignments for a request |
| GET | `/api/assignments/staff/{staffId}` | Retrieve assignments for a staff member |

All endpoints require authentication and enforce role-based authorization.

### Assignment Rules

- Assignment creation is performed by an authenticated STAFF account.
- The authenticated staff identity is used; a supplied staff ID cannot override it.
- Only OPEN requests can be assigned.
- Duplicate assignments are prevented.
- Inactive or non-STAFF accounts cannot create assignments.

---

## 5. Request Status Management APIs

### Update Request Status

- **Method:** PATCH
- **Endpoint:** `/api/requests/{requestId}/status`

Request body:

```json
{
  "newStatus": "IN_PROGRESS",
  "remarks": "Work has started."
}
```

### Supported Status Transitions

| Previous Status | New Status | Authorized Role |
|---|---|---|
| OPEN | IN_PROGRESS | Assigned STAFF |
| IN_PROGRESS | RESOLVED | Assigned STAFF |
| RESOLVED | CLOSED | Original USER |
| RESOLVED | OPEN | Original USER |

Every successful status change is recorded in request status history.

### Retrieve Request Status History

- **Method:** GET
- **Endpoint:** `/api/requests/{requestId}/history`

Returns the status history of a specific request.

Access rules:

- ADMIN: Can view any request history.
- USER: Can view personal request history.
- STAFF: Can view history for assigned requests.

---

## 6. Automatic Escalation API

### Run Escalation

- **Method:** POST
- **Endpoint:** `/api/escalation/run`
- **Access:** ADMIN only

Identifies overdue requests and changes eligible request statuses to ESCALATED.

### Escalation Rules

| Priority | Time Limit |
|---|---|
| HIGH | 2 hours |
| MEDIUM | 24 hours |
| LOW | 48 hours |

The application also runs scheduled escalation checks every five minutes.

Escalation events are recorded in request status history.

---

## 7. Database Testing API

### Test Database Connection

- **Method:** GET
- **Endpoint:** `/api/database/test`
- **Access:** ADMIN only

Checks database connectivity and returns the database name, connection status, and available application tables.

Example successful response:

```json
{
  "database": "smart_service_db",
  "connection": "SUCCESS",
  "tables": [
    "request_assignments",
    "request_categories",
    "request_status_history",
    "service_requests",
    "users"
  ]
}
```

---

## 8. Security and Authorization

The application uses:

- JWT-based stateless authentication.
- BCrypt password hashing.
- Spring Security.
- Role-based access control.
- Request ownership validation.
- Staff assignment validation.
- Inactive account restrictions.
- Database-backed user-role verification.

### User Roles

| Role | Permissions |
|---|---|
| USER | Create and track personal requests; verify, close, or reopen resolved requests |
| STAFF | View assigned requests and update their status |
| ADMIN | Manage users, view all requests, and run escalation |

### Authorization Header

For protected endpoints, include:

```text
Authorization: Bearer <JWT_TOKEN>
```

### Common HTTP Status Codes

| Status | Meaning |
|---|---|
| 200 | Request successful |
| 201 | Resource created, where returned by the endpoint |
| 400 | Invalid request data |
| 401 | Missing or invalid authentication |
| 403 | Insufficient permissions |
| 404 | Resource not found |
| 409 | Resource conflict, such as duplicate email or assignment |

---

## 9. Database

**Database:** `smart_service_db`

Main tables:

1. `users`
2. `request_categories`
3. `service_requests`
4. `request_assignments`
5. `request_status_history`

---

## 10. Technology Stack

- Java 17
- Spring Boot
- Spring JDBC (JdbcTemplate)
- MySQL 8
- Spring Security
- JWT
- BCrypt
- Maven
- Postman