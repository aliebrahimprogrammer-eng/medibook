# MediBook — Productionized Clinic Appointment System

## Project Overview

MediBook is a clinic appointment management platform developed through four project stages. It supports patients, doctors, and administrators with registration, authentication, email verification, password recovery, doctor profiles, specializations, availability management, appointment booking and cancellation, notifications, auditing, and administration.

Projects 2&3 established the core appointment-management system and added a concurrent waitlist, persistent background jobs, and reliable notification delivery. Project 4 productionizes the backend as three Spring Boot microservices, adds Apache Kafka for asynchronous communication, containerizes the services, and automates build, test, and AWS deployment through Jenkins.

The Angular frontend is maintained in this same repository under `medibook-frontend/` and is hosted separately on Cloudflare.

## Features

### Core application
- User registration, login, and email verification
- JWT authentication and role-based authorization
- Patient, doctor, and administrator roles
- User profiles and profile pictures
- Doctor and specialization management
- Doctor availability management
- Appointment booking, cancellation, and status management
- Double-booking prevention and appointment validation
- Password recovery and password change
- User activation and deactivation
- Pagination and supported search/filtering
- Email and Server-Sent Events (SSE) notifications
- Audit logging
- Input validation and structured error handling
- Swagger/OpenAPI documentation

### Project 3 — Waitlist and concurrency
- Patients can join a doctor's waitlist for a preferred time range.
- Cancelling an appointment schedules a persistent waitlist-allocation job.
- A worker selects an eligible waiting entry in FIFO order.
- Successful matching creates an appointment, updates the waitlist, records an event, and creates a persistent notification.
- Notification delivery supports retries and recovery of deliveries stuck in processing.
- Database locking, atomic status updates, transactions, and a bounded `ExecutorService` protect concurrent operations.

### Project 4 — Productionization
- Three independent Spring Boot services: User, Business, and Notification
- Apache Kafka producers and consumers for event-driven communication
- Cucumber and service-level automated tests
- Dockerfiles and Docker Compose
- Nginx reverse proxy for backend routing
- Environment-based production configuration
- Jenkins CI/CD hosted on AWS EC2
- Automated Maven build and test stages for all three services
- Docker image publishing to Amazon ECR
- Deployment package upload to Amazon S3
- AWS Elastic Beanstalk deployment and post-deployment health verification
- Angular frontend in the same repository, hosted on Cloudflare

## Technology Stack

**Backend:** Java 17, Spring Boot, Spring Web/REST, Spring Data JPA, Hibernate, Spring Security, JWT, PostgreSQL, Jakarta Validation, Apache Kafka, Maven, Lombok, JUnit 5, Mockito, Cucumber, Swagger/OpenAPI, Java ExecutorService, scheduled tasks, SSE.

**Frontend:** Angular, TypeScript, Node.js, npm.

**DevOps and cloud:** Docker, Docker Compose, Nginx, Jenkins, GitHub, Amazon EC2, Amazon ECR, Amazon S3, AWS Elastic Beanstalk, AWS IAM, Cloudflare hosting.

## Architecture

### Project 4 microservices architecture

The backend is divided into three Spring Boot services. Each service has its own application and Docker image.

```text
                    Angular Frontend
                  (Cloudflare Hosting)
                           |
                       HTTP / API
                           |
                    Nginx Reverse Proxy
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
      User Service    Business Service  Notification Service
        :8081             :8082              :8083
          |                |                |
          +----------------+----------------+
                           |
                     Apache Kafka
                  (asynchronous events)
                           |
                       PostgreSQL
                (configured through DB_URL)
```

The frontend and backend are deployed separately. In the AWS backend deployment, Nginx listens on port `80` and routes requests to the internal service containers. The service ports are `8081`, `8082`, and `8083`; they are exposed within the Compose network rather than published as public host ports in the supplied Compose configuration.

### Service responsibilities

**User Service**
- User registration, authentication, JWT issuance, and account security
- User profiles and account management
- Email verification and password recovery
- User-related events and internal user operations

**Business Service**
- Doctor and specialization management
- Doctor availability and appointment operations
- Appointment booking, cancellation, and status management
- Waitlist and appointment business rules
- Appointment-related event publishing and processing

**Notification Service**
- Consuming relevant application events
- Notification processing and email delivery
- Persistent notification and delivery processing
- Delivery retry and recovery functionality

Services use APIs for synchronous operations and Kafka events for asynchronous workflows.

### Service data ownership

Each service is responsible for its own domain logic and data-access operations. Services should use another service's API or published events rather than directly accessing another service's repositories.

**Deployment note:** The current AWS Compose configuration supplies the same `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` variables to all three services. Therefore, this README does not claim that the deployed configuration uses three physically separate PostgreSQL database instances. Application-level service ownership and physical database separation are distinct concerns.

### Repository structure

```text
medibook/
├── user-service/
├── business-service/
├── notification-service/
├── medibook-frontend/
├── aws-deployment/
│   ├── .platform/
│   │   ├── confighooks/predeploy/
│   │   └── hooks/predeploy/
│   └── nginx/
├── database/
│   └── init/
├── docs/
│   ├── architecture/
│   ├── deployment/
│   └── diagrams/
├── jenkins/
├── Jenkinsfile
└── README.md
```

The original monolithic implementation and its tests may remain in the repository as part of the project's development history. The Project 4 backend deployment uses the three service directories.

## Project Planning & User Stories

MediBook development was planned and tracked using Trello.

### Projects 2 planning
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

### Project 3 planning
1. Planning and Design
2. Add new entities and repositories
3. Waitlist and background jobs implementation
4. Notifications persistence and retry implementation
5. Concurrency tests
6. API Documentation
7. Testing and Quality
8. Final Documentation

### Project 4 planning
1. Review productionization requirements and architecture
2. Define microservice boundaries and responsibilities
3. Split the backend into User, Business, and Notification services
4. Configure service communication and security
5. Integrate Kafka producers and consumers
6. Implement and test event-driven processing and reliability
7. Containerize services with Docker
8. Configure Docker Compose, Nginx, and environment variables
9. Add and execute automated and Cucumber tests
10. Configure Jenkins and connect the GitHub repository
11. Automate builds, tests, and Docker image creation
12. Publish images to Amazon ECR
13. Configure AWS deployment resources and permissions
14. Package and deploy to Elastic Beanstalk
15. Verify deployment status and health
16. Update architecture, deployment, API, and project documentation


User Stories can be found in the Board for each Project in the following links:

**Project 2 Trello Board:** [MediBook — Clinic Appointment System](https://trello.com/invite/b/6ac4cf744d887dc822afe426/ATTI93e3bc356c64ad6bb57acf1d36e02c199D87A553/medibook-clinic-appointment-system)

**Project 3 Trello Board:** [MediBook V2 — Clinic Appointment System](https://trello.com/invite/b/6ae06698cc291d572ea0ba6c/ATTIbb65c586ca42ec845ed5f61fe9038e62000CFF61/medibook-v2-clinic-appointment-system)

**Project 4 Trello Board:** [MediBook V3 — Clinic Appointment System]([https://trello.com/invite/b/6ae06698cc291d572ea0ba6c/ATTIbb65c586ca42ec845ed5f61fe9038e62000CFF61/medibook-v2-clinic-appointment-system](https://trello.com/invite/b/6afc341212c31c1e4d3703de/ATTI52a08b516337c7f2f0ed9c806ad126f6456AE3F1/medibook-v3-clinic-appointment-system))

## Roles

### PATIENT
Patients can register, verify their email, log in, manage their profile, view doctors, create/view/cancel appointments, join waitlists, and manage their password.

### DOCTOR
Doctors can log in, manage availability, view appointments, and update appointment statuses.

### ADMIN
Administrators can view users, update user roles, and deactivate or reactivate users.

## Database

MediBook uses PostgreSQL.

The original application includes these main entities:

- `User`
- `UserProfile`
- `Doctor`
- `Specialization`
- `Availability`
- `Appointment`
- `EmailVerificationToken`
- `PasswordResetToken`
- `AuditLog`

Project 3 adds:

- `AppointmentWaitlist` — stores a patient's requested doctor and preferred time range, waitlist status, and any matched appointment.
- `AppointmentEvent` — records events associated with appointment and waitlist processing.
- `Notification` — stores persistent user notifications.
- `NotificationDelivery` — tracks delivery channel, status, attempts, errors, and delivery timestamps.
- `BackgroundJob` — persists asynchronous work such as waitlist allocation, including job status and processing information.

The Project 4 services use PostgreSQL configuration supplied through environment variables. The current AWS Compose file passes the same database URL and credentials to each service. Refer to the service configuration and database initialization scripts for the actual schema and connection setup.

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

Multiple API requests or background workers can try to process the same released appointment slot or claim the same pending job/delivery. Without protection, this could lead to duplicate processing or conflicting appointment allocation.

Techniques used:
- **Pessimistic database locking:** The doctor record is locked during critical slot-allocation work so competing transactions for that doctor are serialized.
- **Atomic conditional updates:** Background jobs and notification deliveries are claimed only when the record is in an eligible status.
- **Transactions:** Related database changes during allocation are grouped into a transaction.
- **Database constraints:** Unique constraints prevent duplicate job records for the same job type and triggering appointment, and duplicate delivery records for the same notification and channel.
- **Bounded thread pool:** A fixed-size worker pool processes allocation jobs with a bounded queue.
- **Persistent jobs and deliveries:** Work is represented in the database so pending work and failure status can be inspected and retried.
- **ReentrantLock:** Serializes booking threads for the same doctor in the same JVM.

Database-level protections coordinate persisted operations. An in-memory lock coordinates threads only within the JVM where it exists.

## Kafka and Event-Driven Communication

Apache Kafka is used for asynchronous communication between microservices. The services receive broker connection details through the `KAFKA_BOOTSTRAP_SERVERS` environment variable. Event-related code is organized within the relevant services, including event/producer packages and consumer components.

A typical event-driven flow is:
1. A service performs a business operation.
2. The service publishes the relevant event to Kafka.
3. A subscribing service consumes the event.
4. The receiving service performs its event-handling work, such as notification processing.

Kafka broker provisioning is environment-specific. The supplied AWS deployment Compose file receives the broker address through an environment variable; it does not define a Kafka broker container.

## Security

MediBook uses Spring Security and JWT authentication.

Protected requests use:

```http
Authorization: Bearer <JWT>
```

Supported roles:

```text
PATIENT
DOCTOR
ADMIN
```

Inactive users and users who have not verified their email cannot authenticate for protected functionality. Passwords are hashed and are never stored as plaintext.

The User Service is responsible for user authentication and token issuance. Other services validate JWTs and use configured service-to-service mechanisms where internal user operations are required.

Production configuration is supplied through environment variables. Jenkins deployment credentials are stored in Jenkins Credentials, and AWS permissions are controlled through IAM. The Jenkins EC2 security group restricts inbound administrative access to the required source IP.

Never commit real passwords, JWT secrets, mail credentials, database credentials, internal API keys, or AWS access keys to the repository.

## API Documentation

Swagger/OpenAPI is used to document the REST APIs. Each Spring Boot service has its own API documentation endpoint based on its configured host and port.

Internal service ports in the AWS Compose configuration:

- User Service: `8081`
- Business Service: `8082`
- Notification Service: `8083`

For a locally running service, the Swagger UI path is generally:

```text
/swagger-ui/index.html
```

The OpenAPI specification path is:

```text
/v3/api-docs
```

The externally accessible URL depends on the Nginx routing configuration. Check `aws-deployment/nginx/` and the service configuration for the current route mappings before using a public URL.

## Frontend

The Angular frontend is maintained in this repository under `medibook-frontend/` and hosted separately on Cloudflare. It communicates with the backend through its configured API base URL. Frontend hosting and backend hosting are separate deployments.

Frontend technologies: Angular, TypeScript, Node.js, npm, and Cloudflare hosting.

To run the frontend locally:

```bash
cd medibook-frontend
npm install
npx ng serve --open
```

The Angular development server normally opens at:

```text
http://localhost:4200
```

Ensure the frontend environment configuration points to the backend address you are using.

## Docker and Backend Deployment Configuration

The AWS deployment configuration is under `aws-deployment/`. The supplied Compose configuration defines:

- `user-service`
- `business-service`
- `notification-service`
- `nginx`

The three application containers expose their internal ports to the Compose network. Nginx publishes port `80` and routes incoming backend requests to the services.

The service configuration uses environment variables including:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
ADMIN_EMAIL
ADMIN_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
USER_SERVICE_PUBLIC_URL
USER_SERVICE_INTERNAL_API_KEY
USER_SERVICE_BASE_URL
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

Some variables apply only to specific services. The Compose file also uses `USER_SERVICE_BASE_URL` for Business Service to reach User Service internally.

For AWS deployment, the Compose configuration references images in Amazon ECR. Jenkins updates the image tag for each build. A tag shown in an example configuration should not be assumed to represent the latest deployed version.

Provide required environment variables through the deployment environment's supported configuration. Do not commit production secrets in a `.env` file.

The deployment package includes the Compose configuration and Nginx configuration. Elastic Beanstalk platform hooks are located under `aws-deployment/.platform/`.

## CI/CD Pipeline

MediBook uses Jenkins for continuous integration and continuous deployment. Jenkins runs on an AWS EC2 instance and retrieves source code from GitHub. The pipeline builds and tests each backend microservice, builds Docker images, publishes them to Amazon ECR, and deploys a versioned application package to AWS Elastic Beanstalk.

```text
Developer pushes code to GitHub
              |
              v
       Jenkins detects change
              |
              v
      Build and test User Service
              |
              v
    Build and test Business Service
              |
              v
  Build and test Notification Service
              |
              v
        Build Docker images
              |
              v
      Push images to Amazon ECR
              |
              v
   Package deployment configuration
              |
              v
       Upload package to S3
              |
              v
 Create and deploy Elastic Beanstalk version
              |
              v
     Verify deployed version,
       status, and health
```

Pipeline stages:
1. **Source checkout:** Jenkins checks out the configured GitHub branch.
2. **Build and test:** Maven builds and verifies each Spring Boot service.
3. **Docker image build:** Jenkins builds the three service images.
4. **Image publishing:** Images are tagged and pushed to Amazon ECR.
5. **Deployment packaging:** Deployment configuration is packaged with the image version for the current build.
6. **S3 upload:** The versioned deployment package is uploaded to S3.
7. **Elastic Beanstalk deployment:** Jenkins creates an application version and updates the target environment.
8. **Health verification:** The pipeline checks that the expected version is deployed and that the environment is `Ready` with `Green` health. A failed health check causes the pipeline to fail.

## AWS Deployment Architecture

| AWS component | Purpose |
|---|---|
| Amazon EC2 | Hosts the Jenkins CI/CD server |
| Amazon ECR | Stores Docker images for the three microservices |
| Amazon S3 | Stores versioned Elastic Beanstalk deployment packages |
| AWS Elastic Beanstalk | Runs and manages the backend deployment |
| AWS IAM | Controls permissions for deployment and infrastructure access |

Jenkins builds the service images and publishes them to ECR. It packages the deployment configuration, uploads the package to S3, creates an Elastic Beanstalk application version, and updates the environment.

The Angular frontend is hosted separately on Cloudflare and connects to the backend through its configured API address.

## Getting Started

### Prerequisites

Backend development:
- Java 17
- Maven or the included Maven Wrapper
- PostgreSQL
- Apache Kafka for workflows that require event processing

Frontend development:
- Node.js
- npm

Containerized deployment:
- Docker
- Docker Compose

### Build and verify backend services

From the repository root:

```bash
./mvnw -B -f user-service/pom.xml clean verify
./mvnw -B -f business-service/pom.xml clean verify
./mvnw -B -f notification-service/pom.xml clean verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -B -f user-service/pom.xml clean verify
.\mvnw.cmd -B -f business-service/pom.xml clean verify
.\mvnw.cmd -B -f notification-service/pom.xml clean verify
```

Each service is a separate Maven/Spring Boot application. Its local run configuration depends on the active Spring profile and required environment variables. Configure database, JWT, Kafka, and service-specific settings before starting it.

### Environment variables

The production Compose configuration reads values from the deployment environment. Main configuration keys include:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
KAFKA_BOOTSTRAP_SERVERS
USER_SERVICE_INTERNAL_API_KEY
USER_SERVICE_BASE_URL
USER_SERVICE_PUBLIC_URL
ADMIN_EMAIL
ADMIN_PASSWORD
MAIL_HOST
MAIL_PORT
MAIL_USERNAME
MAIL_PASSWORD
```

Set only the variables required by the service being run. Keep real secret values outside source control.

## Testing

MediBook uses automated tests to validate authentication, authorization, validation, appointment business rules, concurrency, and microservice behaviour.

### Core and Project 3 test scenarios

The test suite covers important scenarios including:
1. Successful login returns a JWT.
2. Inactive/unverified users cannot authenticate.
3. Invalid registration requests are rejected.
4. Appointments outside doctor availability are rejected.
5. Doctor double-booking is rejected.
6. Patients can cancel their own appointments.
7. Patients cannot access admin-only endpoints.
8. Inactive patients cannot create appointments.
9. Waitlist allocation processing.
10. Concurrent attempts to claim a background job.
11. Concurrent attempts to claim a notification delivery.
12. Recovery of stale notification deliveries.

Concurrency tests use PostgreSQL test configuration to exercise database-level claiming behaviour.

### Project 4 testing

Project 4 adds tests within the microservices, including Cucumber feature tests and service-level tests. The service test source trees contain Cucumber test packages and feature resources.

Jenkins runs Maven verification for all three services:

```bash
./mvnw -B -f user-service/pom.xml clean verify
./mvnw -B -f business-service/pom.xml clean verify
./mvnw -B -f notification-service/pom.xml clean verify
```

Run the relevant service verification commands before submitting changes. Use current test reports and Jenkins results as evidence of the latest test status.

## ERD and Evidence / Demo

**ERD and evidence:** [Google Drive — MediBook ERD and evidence](https://drive.google.com/file/d/1E12_K_O2Jjx2FHFuGdnFdxOMdvWkzHef/view?usp=sharing)

Architecture and deployment documentation is maintained in:
- `docs/architecture/`
- `docs/diagrams/`
- `docs/deployment/`

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

The services use Spring profiles for environment-specific configuration. The AWS Compose configuration activates the production profile:

```yaml
SPRING_PROFILES_ACTIVE: prod
```

Runtime values such as database connection details, JWT secrets, Kafka broker addresses, mail settings, and internal service credentials are supplied through environment variables.

## Challenges

Key development challenges across the project included:
- Implementing JWT authentication and role-based authorization.
- Validating appointment availability and preventing double-booking.
- Testing method-level security and API validation.
- Implementing email and SSE notifications.
- Coordinating concurrent waitlist allocation attempts.
- Making background job and notification delivery claiming atomic.
- Persisting asynchronous work and recovering notification deliveries left in a processing state.
- Defining service boundaries while splitting the backend into microservices.
- Integrating asynchronous communication through Kafka.
- Containerizing services and configuring internal networking.
- Automating builds, tests, image publishing, and AWS deployment through Jenkins.
- Configuring AWS IAM permissions and verifying deployment health.

## Future Improvements

Potential future enhancements include:
- Separate database instances or schemas and credentials for stronger physical database isolation between services, where required by the deployment design.
- Centralized monitoring, metrics, tracing, and log aggregation.
- Further hardening of production secrets and AWS access using short-lived role-based credentials.
- More comprehensive end-to-end tests across the frontend and deployed microservices.
- API gateway or service discovery if scale and deployment requirements justify them.
- Further resilience and observability improvements for Kafka consumers and event processing.

## Credits / External Resources

The following resources and tools were used during development:
- Spring Boot and Spring Framework documentation — backend development.
- Spring Security documentation — authentication and authorization.
- Apache Kafka documentation — event-driven communication.
- Docker documentation — containerization.
- Jenkins documentation — CI/CD configuration.
- AWS documentation — ECR, S3, EC2, IAM, and Elastic Beanstalk.
- Swagger/OpenAPI documentation — REST API documentation.
- JJWT documentation — JWT implementation and token handling.
- Angular documentation — frontend development.
- Cloudflare documentation — frontend hosting and deployment.
- Stack Overflow — troubleshooting and resolving development issues.
- Course materials and examples — Java, Spring Boot, REST APIs, security, testing, databases, microservices, and productionization.
- ChatGPT — used as a development and learning assistant for explanations, troubleshooting, code review, documentation, and implementation guidance.

External resources were used for learning, reference, and troubleshooting. The MediBook application was implemented and adapted for the requirements of the project.

## License

This is an educational project and is not intended for production medical use without additional security, privacy, regulatory compliance, and operational work.
