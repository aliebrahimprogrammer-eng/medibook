# MediBook

## Project Overview
MediBook is a secure Spring Boot REST API for managing clinic appointments.

The system provides separate access for patients, doctors, and administrators. It supports user registration and authentication, email verification, password recovery, doctor profiles, specializations, doctor availability, appointment booking and cancellation, appointment status management, notifications, auditing, and administrative user management.

## Features
- User registration and login
- Email verification
- JWT authentication and role-based authorization
- Patient, doctor, and admin roles
- User profile and profile picture support
- Doctor and specialization management
- Doctor availability management
- Appointment booking, cancellation, and status management
- Prevention of invalid bookings and double-booking
- Password recovery and password change
- User activation/deactivation
- Pagination and supported search/filtering
- Email notifications
- Server-Sent Events (SSE) notifications
- Audit logging
- Input validation and structured error handling
- Swagger/OpenAPI documentation
- Automated tests for important business logic and authentication

## Technology Stack
- Java 17
- Spring Boot 4.1.1
- Spring Web / REST
- Spring Data JPA / Hibernate
- PostgreSQL
- Spring Security
- JWT
- Jakarta Validation
- Maven
- Lombok
- JUnit 5
- Mockito
- Swagger / OpenAPI
- Embedded Tomcat

## Architecture
MediBook follows a layered architecture:

```text
com.ga.medibook
├── controller
├── service
├── repository
├── model
│   ├── entity
│   └── enums
├── dto
│   ├── request
│   └── response
├── security
├── exception
├── config
└── notification

```

- **Controller:** Handles HTTP requests and responses.
- **Service:** Contains business logic and business rules.
- **Repository:** Handles database access.
- **Entity/Model:** Represents the domain and database model.
- **DTO:** Controls API request and response data.
- **Security:** Handles JWT authentication and authorization.
- **Notification:** Handles email and SSE notifications.
- **Exception:** Handles application errors.

## Roles

### PATIENT
Patients can register, verify their email, log in, manage their profile, view doctors, create/view/cancel appointments, and manage their password.

### DOCTOR
Doctors can log in, manage availability, view appointments, and update appointment statuses.

### ADMIN
Administrators can view users, update user roles, and deactivate or reactivate users.

## Database
MediBook uses PostgreSQL.

Main entities:
- User
- UserProfile
- Doctor
- Specialization
- Availability
- Appointment
- EmailVerificationToken
- PasswordResetToken
- AuditLog

The database contains meaningful relationships between users, profiles, doctors, specializations, availability, appointments, tokens, and audit logs.

## Security
MediBook uses Spring Security with JWT authentication.

Protected requests use:

```text
Authorization: Bearer <JWT>
```

Supported roles:

```text
PATIENT
DOCTOR
ADMIN
```

Inactive users and users who have not verified their email cannot authenticate for protected functionality. Passwords are hashed and are never stored as plaintext.

## API Documentation
Swagger/OpenAPI is used to document the REST API.

After starting the application:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

The exact port may differ if the application configuration is changed.

## Getting Started

### Prerequisites
- Java 17
- Maven
- PostgreSQL

### Database Setup
Create these PostgreSQL databases:

```text
medibook
medibook_test
```

The configured development database uses PostgreSQL on port `5432`:

```text
jdbc:postgresql://localhost:5432/medibook
```

The test database uses:

```text
jdbc:postgresql://localhost:5432/medibook_test
```

### Environment Variables
The application reads sensitive configuration from environment variables:

```text
DB_USERNAME
DB_PASSWORD
JWT_SECRET
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

### Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

The development Spring profile is used by default.

## Testing
Run the automated tests with:

```bash
mvn test
```

The tests cover important authentication, security, validation, and appointment business logic, including:
1. Successful login returns a JWT.
2. Inactive/unverified users cannot authenticate.
3. Invalid registration requests are rejected.
4. Appointments outside doctor availability are rejected.
5. Doctor double-booking is rejected.
6. Patients can cancel their own appointments.
7. Patients cannot access admin-only endpoints.
8. Inactive patients cannot create appointments.

## Appointment Business Rules
- Only patient users can create appointments.
- The patient must be active and email verified.
- Appointment end time must be after start time.
- Appointments cannot be created in the past.
- The doctor must exist and be active.
- The appointment must fit the doctor's availability.
- Doctors cannot have overlapping active appointments.
- Cancelled appointments do not block new appointments.
- Patients can only cancel their own appointments.
- Completed appointments cannot be cancelled.
- Appointment status changes must follow valid transitions.

## Configuration
The project uses Spring profiles:

```text
application-dev.properties
application-test.properties
```

The application activates the development profile with:

```properties
spring.profiles.active=dev
```

Sensitive values are supplied through environment variables.

## Challenges
Key development challenges included JWT authentication, role-based authorization, appointment availability validation, double-booking prevention, method-level security testing, rate limit and implementing email/SSE notifications.

## Future Improvements
- Add a frontend application.
- Expand appointment search and filtering.
- Add more advanced scheduling.
- Add additional automated tests.
- Add production deployment configuration.

## Credits
This project was developed as an educational Java/Spring Boot backend project.

The project uses Spring Boot, Spring Security, Spring Data JPA, Hibernate, PostgreSQL, JWT libraries, Lombok, JUnit, Mockito, and Swagger/OpenAPI.

## License
This is an educational project and is not for production medical use without additional security, privacy, compliance, and operational work.
