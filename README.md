# MediBook V2

## Project Overview
MediBook is a secure Spring Boot REST API for managing clinic appointments.

The system provides separate access for patients, doctors, and administrators. It supports user registration and authentication, email verification, password recovery, doctor profiles, specializations, doctor availability, appointment booking and cancellation, appointment status management, notifications, auditing, and administrative user management.

Project 3 extends the existing MediBook application with a concurrent appointment waitlist and automatic slot-allocation workflow. When an appointment is cancelled, a background worker can match the released slot to an eligible patient on the waitlist.

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

### Project 3: Waitlist and concurrent slot allocation
- Patients can join a doctor's appointment waitlist for a preferred time range.
- Patients can view their own waitlist entries and cancel entries that are still waiting.
- Cancelling an appointment schedules a persistent background job to try to allocate the released slot.
- The worker selects an eligible waiting entry in FIFO order.
- The matching process creates an appointment, marks the waitlist entry as matched, records an event, and creates a persistent notification.
- Background notification delivery supports retries and recovery of deliveries that remain stuck in processing.
- Database locking and atomic status updates protect critical operations when workers or API requests run concurrently.
- A bounded `ExecutorService` thread pool runs waitlist allocation jobs asynchronously.

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
- Java `ExecutorService`, scheduled tasks, and Server-Sent Events (SSE)


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

## Project Planning & User Stories

The development of MediBook was planned and tracked using Trello.

The Trello board contains the project's planning structure, development tasks, progress, and final completion status. It also contains the user stories that define the main requirements from the perspective of patients, doctors, and administrators.

### Project Planning

The project was divided into smaller development stages to keep the implementation organized:

1. Planning and Design
2. Database and Core Setup
3. Authentication and Security
4. User Management
5. Doctors and Availability
6. Appointments
7. Notifications and Auditing
8. API Documentation
9. Testing and Quality
10. Final Documentation

The Trello board was used to track the project's scope, development tasks, testing, documentation, and progress toward completion.

**Trello Board:** [MediBook — Clinic Appointment System](https://trello.com/invite/b/6ac4cf744d887dc822afe426/ATTI93e3bc356c64ad6bb57acf1d36e02c199D87A553/medibook-clinic-appointment-system)

### Project 3 Planning

The project was divided into smaller development stages to keep the implementation organized:

1. Planning and Design
2. Add new entities and repository
3. Waitlist, background jobs implementation
4. Notifications persistence and retry implementation
5. Create tests for concurrency 
6. API Documentation
7. Testing and Quality
8. Final Documentation

The Trello board was used to track the project's scope, development tasks, testing, documentation, and progress toward completion.

**Trello Board V2:** [MediBook — Clinic Appointment System]([https://trello.com/invite/b/6ac4cf744d887dc822afe426/ATTI93e3bc356c64ad6bb57acf1d36e02c199D87A553/medibook-clinic-appointment-system](https://trello.com/invite/b/6ae06698cc291d572ea0ba6c/ATTIbb65c586ca42ec845ed5f61fe9038e62000CFF61/medibook-v2-clinic-appointment-system))

### User Stories

The Trello board contains user stories for the main MediBook users:

The user stories follow the format:

> As a [type of user], I want to [perform an action], so that [reason].

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

Project 3 adds five entities:
- `AppointmentWaitlist` — stores a patient's requested doctor and preferred time range, waitlist status, and any matched appointment.
- `AppointmentEvent` — records events associated with appointment and waitlist processing.
- `Notification` — stores persistent user notifications.
- `NotificationDelivery` — tracks delivery channel, status, attempts, errors, and delivery timestamps.
- `BackgroundJob` — persists asynchronous work such as waitlist allocation, including job status and processing information.


The database contains meaningful relationships between users, profiles, doctors, specializations, availability, appointments, tokens, and audit logs.


## Project 3 Workflow

1. A patient books an appointment for a doctor and time.
2. Another patient can join the waitlist for a compatible time range.
3. When the booked appointment is cancelled, the application creates a persistent waitlist-allocation background job.
4. A scheduled dispatcher submits pending jobs to a bounded `ExecutorService` thread pool.
5. The worker atomically claims the job, locks the doctor record, checks for overlapping appointments, and selects an eligible waiting entry.
6. If a match is found, the worker creates an appointment for the waiting patient, marks the waitlist entry as matched, records an event, creates a persistent notification, and completes the job.
7. A separate scheduled worker processes pending notification deliveries and retries eligible failures.
8. A recovery task marks notification deliveries stuck in `PROCESSING` for more than five minutes as `FAILED`, allowing them to be retried within the configured attempt limit.

If no eligible waitlist entry is found, the allocation job finishes without assigning the slot.

## Concurrency and Consistency

### Race condition addressed

Multiple API requests or background workers can try to process the same released appointment slot or claim the same pending job/delivery at the same time. Without protection, this could lead to duplicate processing or conflicting appointment allocation.

### Techniques used

- **Pessimistic database locking:** The doctor record is locked during critical slot-allocation work so competing transactions for that doctor are serialized.
- **Atomic conditional updates:** Background jobs and notification deliveries are claimed with database updates that only succeed when the record is in an eligible status. The affected-row count indicates whether the claim succeeded.
- **Transactions:** Related database changes during allocation are grouped into a transaction.
- **Database constraints:** Unique constraints prevent duplicate job records for the same job type and triggering appointment, and duplicate delivery records for the same notification and channel.
- **Bounded thread pool:** A fixed-size worker pool processes allocation jobs with a bounded queue rather than creating an unbounded number of threads.
- **Persistent jobs and deliveries:** Work is represented in the database so pending work and failure status can be inspected and retried.
- **ReentrantLock:** Serializes booking threads for the same doctor in this JVM.

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
You can refer to .env.example to create your own .env in the project root
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
9. Waitlist allocation processing
10. Concurrent attempts to claim a background job
11. Concurrent attempts to claim a notification delivery
12. Recovery of stale notification deliveries

Concurrency tests use the PostgreSQL test database to exercise database-level claiming behavior. Run `mvn test` before submission and update this section if the final test results differ from the scenarios listed above.


## ERD and evidence/demo
**Google Drive link:** https://drive.google.com/file/d/1E12_K_O2Jjx2FHFuGdnFdxOMdvWkzHef/view?usp=sharing

## Frontend Setup

The MediBook frontend is maintained in a separate repository:

**Frontend Repository:** https://github.com/aliebrahimprogrammer-eng/medibook-frontend

### Prerequisites
- Node.js and npm
- The MediBook backend running locally if you want to use the frontend with the API

### Clone and Install

Clone the frontend repository and install its dependencies:

```bash
git clone https://github.com/aliebrahimprogrammer-eng/medibook-frontend.git
cd medibook-frontend
npm install
```

### Run the Frontend

Start the Angular development server from the frontend project directory:

```bash
npx ng serve --open
```

Then open:

```text
http://localhost:4200
```

The frontend and backend run as separate applications. Start the Spring Boot backend separately using the backend instructions above. Make sure the frontend's API base URL/configuration points to the backend address used by your local setup.

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
For Project 3, additional challenges included coordinating concurrent allocation attempts, making job/delivery claiming atomic, persisting background work, and recovering notification deliveries left in a processing state.

## Future Improvements
- Add a frontend application.
- Expand appointment search and filtering.
- Add more advanced scheduling.
- Add additional automated tests.
- Add production deployment configuration.

## Credits / External Resources

The following resources were used during the development of MediBook:

- Swagger/OpenAPI Documentation — REST API documentation.
- JJWT Documentation — JWT implementation and token handling.
- Stack Overflow — troubleshooting and resolving development issues.
- Course materials and examples — guidance on Java, Spring Boot, REST APIs, security, testing, and database development.
- ChatGPT — used as a development and learning assistant for explanations, troubleshooting, code review, and implementation guidance.

All external resources were used for learning, reference, and troubleshooting. The MediBook application was implemented and adapted for the requirements of this project.

This project was developed as an educational Java/Spring Boot backend project.

The project uses Spring Boot, Spring Security, Spring Data JPA, Hibernate, PostgreSQL, JWT libraries, Lombok, JUnit, Mockito, and Swagger/OpenAPI.

## License
This is an educational project and is not for production medical use without additional security, privacy, compliance, and operational work.
