# Approval Flow Engine

A production-ready Spring Boot 3 / Java 17+ application for managing multi-step approval workflows with role-based decisions, automatic escalation, audit trails, and comprehensive business process tracking.

## Overview

The Approval Flow Engine is an internal workflow system where requests move through sequential approval stages. Users submit requests, approvers make decisions at each step, the system automatically tracks all actions, escalates overdue approvals, and maintains a complete audit trail for compliance and transparency.

## Use Cases

- **Access Requests**: Employee access to systems, applications, or data
- **Expense Approvals**: Multi-level approval for expenses (team lead → manager → finance)
- **Purchase Orders**: Procurement requests with tiered approval based on amount
- **Policy Exceptions**: Requests for policy waivers with management oversight
- **Time-Off Requests**: Leave requests requiring manager and HR approval
- **Budget Requests**: Budget allocation requiring cross-functional approval

## Tech Stack

- **Java 17+** - LTS Java release (compatible with Java 21)
- **Spring Boot 3.2.5** - Application framework
- **Spring Data JPA** - Database access and ORM
- **Spring Security** - Authentication and authorization
- **JWT** (io.jsonwebtoken 0.12.3) - Stateless token-based authentication
- **PostgreSQL** - Production database
- **H2** - In-memory database for development/testing
- **Flyway** - Database migration management
- **Lombok** - Reduces boilerplate code
- **Maven** - Build and dependency management
- **SpringDoc OpenAPI** - API documentation (Swagger)
- **Docker & Docker Compose** - Containerization

## Entity Model

| Entity | Description |
|--------|-------------|
| **User** | System users with roles (Admin, Manager, Approver, User) |
| **Role** | User roles for authorization (ROLE_ADMIN, ROLE_MANAGER, ROLE_APPROVER, ROLE_USER) |
| **ApprovalRequest** | Main approval request with title, description, type, status, and steps |
| **ApprovalStep** | Individual step in approval process with assigned approvers |
| **ApproverAssignment** | Links users or roles to approval steps |
| **ApprovalDecision** | Decision made on a step (Approved/Rejected/Changes Requested) |
| **Comment** | Comments on approval requests |
| **NotificationLog** | Notification records (email, Slack, in-app) |
| **AuditLog** | Complete audit trail of all system actions |

## How Multi-Step Approval Works

1. **Create**: User creates a request in DRAFT status with multiple approval steps
2. **Submit**: User submits the request → status changes to IN_REVIEW
3. **Activate First Step**: First step changes to IN_PROGRESS, assigned approvers are notified
4. **Decision**: Approver makes a decision (Approve/Reject/Request Changes)
   - **Approve**: If last step → request APPROVED; otherwise advance to next step
   - **Reject**: Request immediately marked REJECTED (no further steps)
   - **Request Changes**: Request marked CHANGES_REQUESTED, requester must update
5. **Progression**: Each approved step automatically activates the next step
6. **Completion**: Request reaches final APPROVED or REJECTED status
7. **Audit**: Every action is logged for compliance and review

## Status Transition Flow

```
         ┌──────────┐
         │  DRAFT   │
         └────┬─────┘
              │ submit()
              ▼
         ┌──────────┐
         │SUBMITTED │
         └────┬─────┘
              │ auto
              ▼
    ┌────────────────┐        approve       ┌──────────┐
    │   IN_REVIEW    │────(last step)───────▶│ APPROVED │
    │ (Step 1, 2...) │                       └──────────┘
    └────┬─────┬─────┘
         │     │ reject
         │     └──────────────────────────────▶┌──────────┐
         │                                     │ REJECTED │
         │ request_changes                     └──────────┘
         └────────────────────────────────────▶┌──────────────────┐
                                               │CHANGES_REQUESTED │
                                               └──────────────────┘
    
    cancel() from any state ───────────────────▶┌───────────┐
                                                │ CANCELLED │
                                                └───────────┘
```

## API Endpoints

### Authentication
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new user | No |
| POST | `/api/auth/login` | Login and get JWT token | No |

### User Management
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/users/me` | Get current user profile | Yes |

### Approval Requests
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/requests` | Create new request | Yes |
| GET | `/api/requests` | List requests (with filters) | Yes |
| GET | `/api/requests/{id}` | Get request details | Yes |
| POST | `/api/requests/{id}/submit` | Submit request for approval | Yes (Requester) |
| POST | `/api/requests/{id}/cancel` | Cancel request | Yes (Requester/Admin) |
| GET | `/api/requests/{id}/timeline` | Get request audit timeline | Yes |

### Decisions
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/steps/{stepId}/approve` | Approve a step | Yes (Assigned Approver) |
| POST | `/api/steps/{stepId}/reject` | Reject a step | Yes (Assigned Approver) |
| POST | `/api/steps/{stepId}/request-changes` | Request changes | Yes (Assigned Approver) |

### Comments
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/requests/{requestId}/comments` | Add comment | Yes |
| GET | `/api/requests/{requestId}/comments` | List comments | Yes |

### Dashboard
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/dashboard/summary` | Get dashboard summary | Yes |

### Admin
| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/admin/overdue-steps` | List overdue steps | Yes (Admin/Manager) |
| GET | `/api/admin/requests` | All requests (admin view) | Yes (Admin/Manager) |

## Local Development Setup

### Prerequisites
- Java 17+ (OpenJDK or Oracle JDK; Java 21 recommended)
- Maven 3.8+
- Docker & Docker Compose (optional, for PostgreSQL)

### Run with H2 (In-Memory Database)

```bash
# Clone the repository
git clone <repository-url>
cd approval-flow-engine

# Build the project
mvn clean package -DskipTests

# Run with dev profile (uses H2)
mvn spring-boot:run

# Or run the JAR
java -jar target/approval-flow-engine-1.0.0.jar
```

The application will start on `http://localhost:8080`

- **H2 Console**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:approvaldb`
  - Username: `sa`
  - Password: (empty)

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **API Docs**: `http://localhost:8080/v3/api-docs`

### Run with PostgreSQL

```bash
# Start PostgreSQL with Docker Compose
docker-compose up -d postgres

# Run with prod profile
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

Or with environment variables:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/approvaldb
export DATABASE_USERNAME=approvaluser
export DATABASE_PASSWORD=approvalpass

java -jar target/approval-flow-engine-1.0.0.jar
```

### Run Full Stack with Docker Compose

```bash
# Build the application
mvn clean package -DskipTests

# Start all services (PostgreSQL + App)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

Application will be available at `http://localhost:8080`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile (dev/prod) | `dev` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/approvaldb` |
| `DATABASE_USERNAME` | Database username | `approvaluser` |
| `DATABASE_PASSWORD` | Database password | `approvalpass` |

### JWT Configuration

- **Secret Key**: Set via the `APP_JWT_SECRET` environment variable in production. A dev-only default is used when the variable is not set — **never use the default in production**.
- **Token Expiration**: Configurable via `APP_JWT_EXPIRATION` (default: 24 hours / 86400000 ms)
- **Algorithm**: HMAC-SHA256

```bash
# Set a strong secret in production
export APP_JWT_SECRET="your-very-long-random-secret-key-min-32-chars"
export APP_JWT_EXPIRATION=86400000
```

## Testing the API

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Login and Get Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "password123"
  }'
```

Save the returned `token` for subsequent requests.

### 3. Create Approval Request

```bash
curl -X POST http://localhost:8080/api/requests \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{
    "title": "Access to Production Database",
    "description": "Need read access for troubleshooting",
    "requestType": "ACCESS_REQUEST",
    "dueDate": "2024-12-31",
    "steps": [
      {
        "stepName": "Manager Approval",
        "stepOrder": 1,
        "assignedRoleIds": [2]
      },
      {
        "stepName": "Security Review",
        "stepOrder": 2,
        "assignedRoleIds": [1]
      }
    ]
  }'
```

### 4. Submit Request

```bash
curl -X POST http://localhost:8080/api/requests/1/submit \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### 5. Approve Step

```bash
curl -X POST http://localhost:8080/api/steps/1/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -d '{
    "comment": "Approved - looks good"
  }'
```

## Database Schema

The application uses Flyway for database migrations. The initial schema is created from:
- `src/main/resources/db/migration/V1__init_schema.sql`

Default roles are automatically seeded:
- ROLE_ADMIN (id: 1)
- ROLE_MANAGER (id: 2)
- ROLE_APPROVER (id: 3)
- ROLE_USER (id: 4)

## Scheduled Tasks

### Escalation Service

Runs every hour to check for overdue approval steps:
- Identifies steps in IN_PROGRESS status past their due date
- Logs escalation events to audit trail
- Creates notifications for assigned approvers

## Security

- **Password Hashing**: BCrypt with Spring Security
- **JWT Authentication**: Stateless token-based auth
- **Role-Based Access Control**: Method-level security with `@PreAuthorize`
- **CORS**: Configure as needed in SecurityConfig
- **SQL Injection**: Protected by JPA/Hibernate parameterized queries
- **XSS**: Input validation with Bean Validation (@Valid, @NotBlank, etc.)

## Future Improvements

- [ ] Email/Slack integration for notifications
- [ ] Parallel approval steps (multiple approvers at same step)
- [ ] Conditional routing (different paths based on request attributes)
- [ ] Approval delegation (assign step to another user)
- [ ] Request templates for common workflows
- [ ] SLA tracking and reporting
- [ ] File attachments on requests
- [ ] Advanced search with full-text indexing
- [ ] Webhook support for external integrations
- [ ] Mobile app/PWA frontend
- [ ] Approval by email (reply-to-approve)
- [ ] Request archival and retention policies
- [ ] Grafana dashboard for metrics
- [ ] Multi-tenancy support

## License

Internal use only - proprietary software.

## Support

For questions or issues, contact the development team.
