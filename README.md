# README.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a **Rent-a-Car API** built with Spring Boot 4.1 and Java 25 (JDK 25 required to build and run), providing a comprehensive car rental management system with JWT authentication and role-based authorization.

## Common Commands

### Development
```bash
# Run the application (default local profile with H2 database)
./mvnw spring-boot:run

# Run with local profile explicitly (Windows PowerShell)
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"

# Run with local profile explicitly (Unix/macOS)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local

# Run with PostgreSQL in Docker (starts compose.yaml automatically; Docker Desktop must be running)
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker

# Run with specific profile (e.g., dev, against your own PostgreSQL)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Build the application
./mvnw clean package

# Run tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CarServiceIntegrationTest

# Run specific test method
./mvnw test -Dtest=CarServiceIntegrationTest#carLifecycleAndAvailability

# Clean and rebuild
./mvnw clean compile
```

### Database
The Flyway Maven plugin targets the dev/docker PostgreSQL (`localhost:5432/rentacar`) by default.
The database must be running, e.g. start the app once with the `docker` profile or run `docker compose up -d`.
For another database, pass `-Dflyway.url=... -Dflyway.user=... -Dflyway.password=...` and
`-Dflyway.locations=filesystem:src/main/resources/db/migration` (never include `db/dev-data` outside local/dev).

```bash
# Check database migrations
./mvnw flyway:info

# Run migrations
./mvnw flyway:migrate

# Reset database (WARNING: destructive; clean is disabled unless explicitly enabled)
./mvnw flyway:clean flyway:migrate -Dflyway.cleanDisabled=false
```

### PostgreSQL in Docker (`docker` profile)
- `compose.yaml` defines PostgreSQL 17; Spring Boot starts it when the app runs with the `docker` profile and connects to it automatically
- The `docker` profile also activates `dev` (seed users `admin`/`admin123`, `john.doe`/`admin123`)
- Data is kept in the `rent-a-car-be_postgres-data` volume; `docker compose down -v` deletes it
- From the IDE, set the active profile to `docker` (e.g. program argument `--spring.profiles.active=docker` or env `SPRING_PROFILES_ACTIVE=docker`)

### Useful Development URLs (when running locally)
- Application: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:rentacar`)
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs
- Health Check: http://localhost:8080/actuator/health

## Architecture Overview

### Domain Model
The application follows a layered architecture with distinct domain entities:

**Core Entities:**
- **Car**: Vehicle inventory with soft delete capability (uses `@SQLRestriction("deleted = false")`)
- **Reservation**: Customer bookings with overlap prevention logic
- **Customer**: Customer information with driver license validation
- **Branch**: Rental locations where cars are picked up/dropped off
- **User/Role**: Authentication system with ADMIN/EMPLOYEE/CUSTOMER roles
- **Employee**: Staff members associated with branches
- **Maintenance**: Car maintenance records and scheduling
- **Payment**: Payment processing for reservations

### Key Business Logic

**Reservation Pricing:** 
- Calculated as `dailyPrice × numberOfDays` (no taxes/fees included per business rules)
- Overlap validation prevents double-booking of cars

**Car Management:**
- Cars use **soft delete** (others use hard delete)
- Availability filtering considers reservation overlaps and car status

**Security:**
- JWT-based authentication using Nimbus library
- Role-based access control (RBAC)
- Different endpoints protected by different roles

### Package Structure
```
com.nextstep.rentacar/
├── config/                  # Configuration classes
│   └── security/           # JWT and security configuration
├── domain/
│   ├── entity/             # JPA entities with BaseEntity (Long IDs)
│   └── enums/              # Business enums (CarStatus, ReservationStatus, etc.)
├── dto/
│   ├── request/            # Request DTOs for API endpoints
│   └── response/           # Response DTOs
├── mapper/                 # MapStruct mappers for entity-DTO conversion
├── repository/             # Spring Data JPA repositories
├── service/
│   ├── impl/               # Service implementations
│   └── auth/               # Authentication services (AuthService, JwtService)
└── web/
    ├── controller/         # REST controllers
    └── exception/          # Global exception handling
```

## Environment Profiles

- **local**: H2 in-memory database (default)
- **dev**: Local PostgreSQL database
- **docker**: `dev` + PostgreSQL started from `compose.yaml` via Docker
- **test**: H2 in-memory database for integration tests
- **prod**: Production PostgreSQL; requires `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` and `JWT_SECRET`

## Key Configuration Notes

### Database
- Uses **Flyway** for migrations (located in `src/main/resources/db/migration/`)
- **Long** type used for all entity IDs
- Audit fields (`created_at`, `updated_at`) handled by `BaseEntity` with JPA auditing
- JPA auditing configured with UTC timezone via `dateTimeProvider` bean in `JpaConfig`

### Testing
- Integration tests use `@SpringBootTest` with `@ActiveProfiles("test")` (H2)
- `PostgresIntegrationTest` runs the `prod` profile against PostgreSQL via **Testcontainers**; it needs Docker and is skipped when Docker is not running
- `SecurityRulesTest` checks the production security rules for every role
- UTC timestamps for consistent auditing across all environments

### Security

> **Before you push and deploy:** wherever this backend runs, set a strong random `JWT_SECRET`
> (at least 32 characters) and deploy the latest version. Without it, the app won't start.
>
> Generate one with e.g. `openssl rand -base64 48`. Only the `local`, `dev` and `test` profiles
> have built-in development secrets.

- JWT tokens with configurable expiration
- `ADMIN_PASSWORD` (optional): if set, the seeded `admin` user's password is set from it at startup. Use it to get a working admin in prod; remove it afterwards if the admin should be able to change the password
- Password encoding with BCrypt
- CORS configured for local development
- Method-level security enabled with `@EnableMethodSecurity`

## Data Seeding

Initial data is created via Flyway migration `V2__seed_roles_and_admin.sql`:
- **Admin user**: `admin`/`admin123`
- **Employee user**: `john.doe`/`admin123`

These passwords work only in the `local`, `dev` and `docker` profiles (set by `db/dev-data/R__dev_seed_user_passwords.sql`). Elsewhere, set the admin password with `ADMIN_PASSWORD` (see Security).
- Sample branches, cars, customers, and reservations

## MapStruct Integration

The project uses MapStruct for DTO-Entity mapping with Spring component model:
- Configured in `pom.xml` with annotation processor
- Mappers are Spring beans (auto-injected)
- Custom mapping logic for complex transformations

## Important Implementation Details

1. **Car Soft Delete**: Cars use `deleted` boolean flag with `@SQLRestriction`
2. **Reservation Overlaps**: Prevented in service layer with date range queries
3. **JWT Configuration**: Separate properties for different environments
4. **Validation**: Bean validation annotations on DTOs and entities
5. **Exception Handling**: Global exception handler for consistent API responses

## Testing Strategy

- **Integration Tests**: Full Spring context with H2; PostgreSQL via Testcontainers in `PostgresIntegrationTest`
- **Service Layer Focus**: Core business logic testing
- **Entity Manager Clearing**: Tests simulate transaction boundaries
- **Date-based Logic**: Reservation availability and overlap scenarios
