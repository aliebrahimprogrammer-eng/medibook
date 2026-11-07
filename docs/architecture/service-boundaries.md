
# MediBook Microservices Architecture

## 1. Architecture Overview

MediBook will be refactored from a monolithic Spring Boot application
into three independently deployable Spring Boot microservices and
one Angular frontend application.

The services are:

- User Service
- Business Service
- Notification Service

Apache Kafka will support asynchronous communication.

PostgreSQL will provide persistent storage with separate database
ownership for each service.

## 2. User Service

Responsibilities:

- User registration and authentication
- JWT issuance and validation configuration
- User profiles
- Roles and permissions
- Password reset
- Email verification
- Administrative user management

Database: medibook_users

The User Service owns user identity and account information.

## 3. Business Service

Responsibilities:

- Doctor management
- Specialization management
- Doctor availability
- Appointment booking and cancellation
- Patient appointments
- Waitlist management
- Automatic appointment allocation
- Booking concurrency and consistency
- Business-operation audit logs

Database: medibook_business

The Business Service owns all clinic scheduling and appointment
business rules.

It must preserve the existing concurrency protections, database
transactions, locking mechanisms and consistency constraints.

## 4. Notification Service

Responsibilities:

- Email notifications
- Persistent notifications
- Server-Sent Events (SSE)
- Notification delivery tracking
- Failed delivery recovery

Database: medibook_notifications

The Notification Service consumes relevant events from Kafka.

## 5. Database Ownership

Each service owns its database.

Services must not directly access another service's database tables.

References to entities owned by another service will use identifiers
rather than cross-service JPA relationships or foreign keys.

## 6. Communication

REST:

Used for synchronous communication when an immediate response
or information from another service is required.

Kafka:

Used for asynchronous event communication.

Initial Kafka topic:

appointment-events

Initial event:

AppointmentCreated

Producer:

Business Service

Consumer:

Notification Service

Additional events may include:

- AppointmentCancelled
- WaitlistAppointmentAllocated

## 7. Security

User Service is responsible for issuing authentication tokens.

Other services independently validate incoming tokens.

Service-to-service communication must be authenticated.

Secrets and credentials must be supplied through environment
configuration rather than committed to Git.

## 8. Reliability and Concurrency

The Business Service must preserve appointment consistency
across multiple service instances.

Kafka consumers must handle duplicate events and processing failures.

Notification delivery status must be persisted to support recovery.

Publishing appointment events must not compromise database
transaction consistency.

## 9. Infrastructure

Local development:

- Docker
- Docker Compose
- PostgreSQL
- Apache Kafka
- Three Spring Boot services
- Angular frontend

Cloud deployment:

- AWS EC2
- AWS ECR
- AWS RDS
- AWS IAM
- AWS Security Groups
- CloudWatch logging where appropriate

## 10. Architecture Principles

- Independent service deployment
- Clear service ownership
- Database-per-service ownership
- Loose coupling
- REST for synchronous communication
- Kafka for asynchronous communication
- Preserve existing business functionality
- Keep implementation simple and maintainable
