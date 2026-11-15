# Rent-a-Car API

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
Migrations are read from the compiled classpath (V4 and V7 are Java migrations), so compile first if `target/` is empty: `./mvnw compile flyway:info`.
For another database, pass `-Dflyway.url=... -Dflyway.user=... -Dflyway.password=...` and
`-Dflyway.locations=classpath:db/migration` (never include `db/dev-data` outside local/dev).

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
- The `docker` profile also activates `dev` (seed users `admin`, `maria.manager` and `john.doe`, all with password `admin123`)
- Like every profile except `test`, it uses the real security rules: sign in with a seeded user (see Data Seeding)
- Data is kept in the `rent-a-car-be_postgres-data` volume; `docker compose down -v` deletes it
- From the IDE, set the active profile to `docker` (e.g. program argument `--spring.profiles.active=docker` or env `SPRING_PROFILES_ACTIVE=docker`)

### Useful Development URLs (when running locally)
- Application: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:rentacar`)
- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs
- Health Check: http://localhost:8080/actuator/health

### API Overview
All endpoints are under `/api/v1` (full details in Swagger UI):

| Resource | Endpoints |
|---|---|
| Auth | `POST /auth/login` (username or email), `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/change-password`, `GET /auth/me` (user, roles, permissions, branch) |
| Branches | CRUD, `GET /branches/search?name=&city=` (both optional, partial and case-insensitive), `/by-city`, `/by-country`, `/by-name-and-city` (exact matches) |
| Cars | CRUD (soft delete), `POST /cars/{id}/restore`, `GET /cars/deleted`, `GET /cars/branch/{branchId}`, `GET /cars/available` |
| Customers | CRUD, `GET /customers/search`, `GET /customers/searchany` |
| Reservations | create, `PUT` (PENDING only), list with filters/search, `POST /reservations/{id}/confirm`, `/pickup`, `/complete` (return), `/cancel` |
| Maintenance | `POST /maintenance/schedule`, `GET`/`PUT /maintenance/{id}`, `POST /maintenance/{id}/start`, `/complete`, `/cancel`, list with filters, `GET /maintenance/due-cars`, `GET /maintenance/scheduled` |
| Users | `GET /users` (`search`, `role`), `GET /users/{id}`, `POST /users`, `PUT /users/{id}`, `POST /users/{id}/reset-password` |
| Settings | `GET /settings`, `PUT /settings` |

## Architecture Overview

### Domain Model
The application follows a layered architecture with distinct domain entities:

**Core Entities:**
- **Car**: Vehicle inventory with soft delete capability (uses `@SQLRestriction("deleted = false")`)
- **Reservation**: Customer bookings with overlap prevention logic
- **Customer**: Customer information with driver license validation
- **Branch**: Rental locations where cars are picked up/dropped off; can be closed (`active` flag)
- **User/Role**: Authentication system with STAFF, MANAGER and ADMIN roles
- **Employee**: Staff members associated with branches
- **Maintenance**: Car maintenance records and scheduling
- **Payment**: Payment processing for reservations

### Key Business Logic

**Reservation Pricing:**
- Calculated as `dailyPrice × numberOfDays` (no taxes/fees included per business rules)
- A same-day rental (start = end date) counts as one day; otherwise it's the days between pick-up and return (14th to 16th = 2 days)
- Overlap validation prevents double-booking of cars

**Reservation Lifecycle:**
```
PENDING ──confirm──▶ CONFIRMED ──pick up──▶ ACTIVE ──return (complete)──▶ COMPLETED
   └────────── cancel ──────────┘
```
- Only PENDING reservations can be edited (`PUT`); the price is recalculated and availability re-checked
- **Pick up** (`POST /reservations/{id}/pickup`): CONFIRMED only, on a day from the start to the end date, and only if the car is AVAILABLE; the car becomes RENTED. Optional odometer reading (`mileageOut`)
- **Return** (`POST /reservations/{id}/complete`): ACTIVE only, also after the end date (late return); the car becomes AVAILABLE at the **drop-off** branch (one-way rentals move it). Optional odometer reading (`mileageIn`), which updates the car's mileage
- Odometer readings can't go backwards (not below the car's mileage, return not below pick-up)
- ACTIVE reservations can't be cancelled

**Car Status vs. Bookings:**
- The car status is its physical state: AVAILABLE, RENTED (picked up), MAINTENANCE (job in progress) or OUT_OF_SERVICE. A booking doesn't change it; pick-up does
- Bookings are date-based: `GET /cars/available` excludes cars with an overlapping PENDING/CONFIRMED/ACTIVE reservation, maintenance scheduled within the dates, an inactive branch, or a status other than AVAILABLE
- A rental still out past its end date also blocks bookings starting today
- `GET /cars` and `GET /cars/{id}` include `nextReservation`: the car's current or next open booking

**Branches:**
- A branch is `active` by default. An **inactive** branch is closed: its cars aren't offered in `GET /cars/available`, and it can't be the pick-up or return location of a new reservation, nor the new location when editing one (409 "Dropoff branch is inactive: …"). A reservation that keeps a branch that closed later can still be edited; existing reservations, pick-ups and returns are unaffected
- Switch it with `PUT /branches/{id}` (ADMIN): `active` is optional there, so an update without it leaves the status unchanged
- A duplicate name in the same city is refused (400)

**Car Management:**
- Cars use **soft delete** (others use hard delete)
- Catalog search (`search`) matches make, model or "year make model"; the separate filters `make` and `model` match partially (case-insensitive), `year` and `vin` exactly (the VIN is case-sensitive)

**Maintenance:**
```
SCHEDULED ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED
    └──────── cancel ────────┘
```
- A scheduled job blocks bookings on its date, and can't be scheduled on a day the car is booked (409)
- Starting a job sets the car to MAINTENANCE (not allowed while the car is RENTED); completing or cancelling sets it back to AVAILABLE unless another job is still in progress
- Only SCHEDULED jobs can be edited; a new date can't be in the past
- Completing records the final cost, odometer reading, notes and the next service date; it sets the car's last service date and mileage
- `GET /maintenance/due-cars?withinDays=14` lists cars whose next service date is near or past and that have no open job
- RECALL jobs are safety recalls: start them to take the car out of service

**Settings:**
- `GET /settings` (any signed-in user) and `PUT /settings` (ADMIN): the mileage unit (KILOMETERS or MILES; a label only, values are never converted) and the optional default branch (`defaultBranchId`, which the UI pre-selects on the availability search). `PUT` replaces both: leaving `defaultBranchId` out or `null` clears it, an unknown branch is 404 and an inactive one 409. `GET` reports `null` if the saved branch has since been closed or removed

**Security:**
- JWT-based authentication using Nimbus library
- Roles grant permissions, and the rules check permissions (`config/security/Permission`, `RolePermissions`). Each role has the rights of the one before it:

  | Role | Rights |
  |---|---|
  | STAFF | browse cars, branches and settings; customers (see, create, edit); reservations (book, edit, confirm, cancel, pick up, return); see maintenance and schedule jobs |
  | MANAGER | + add and edit cars, see deleted cars; delete customers; edit, start, complete and cancel maintenance; see users and create STAFF accounts |
  | ADMIN | everything: + delete and restore cars, branches, settings, and all user management |

- `GET /auth/me` returns the user's roles and permissions (and branch), so a client can show only what the user may do
- **Sessions:** `POST /auth/login` (username or email) returns a short **access token** (JWT, 15 minutes; send it as `Authorization: Bearer …`) and sets a **refresh cookie** (`refresh_token`: httpOnly, `SameSite=Strict`, path `/api/v1/auth`, `Secure` outside `local`/`dev`). The session lasts 12 hours from sign-in
  - `POST /auth/refresh` uses the cookie (no access token needed): new access token, new cookie (the old one is replaced), same session end. If a replaced cookie is used again it was copied, so all of that user's sessions end
  - `POST /auth/logout` ends the session and deletes the cookie; `POST /auth/change-password` needs the current password and ends the user's other sessions
  - Only a hash of each refresh token is stored (`refresh_tokens`)
- **Failed sign-ins** all get the same 401 ("Invalid username or password", also for disabled accounts). After 5 failures in a row an account is locked for 15 minutes (429 with `Retry-After`); the count is kept in memory per instance
- Without a valid token the answer is **401**; signed in without the permission it is **403** (both as problem details)
- A disabled user's tokens stop working at once. There is no public registration: accounts are created by managers (STAFF only) and admins
- **User accounts** (`/users`): each user has exactly one role (STAFF, MANAGER or ADMIN). Staff and managers need a branch (they get an employee record there); administrators don't. The username can't contain `@` (sign-in takes a username or an email, in any letter case; the password is case-sensitive); emails are stored in lower case; the username and email must be unique, ignoring case (409)
  - Managers can list users and create **STAFF** (a manager creating a manager or administrator is 403); everything else (`PUT`, reset password) is admin-only
  - `PUT /users/{id}` replaces email, name, phone, role, branch and `enabled`; the username never changes. Disabling ends the user's sessions at once. An administrator can't disable or demote their own account (409), so there is always an administrator who can manage the others
  - `POST /users/{id}/reset-password` sets a new password (min 8 characters), ends all of the user's sessions and lifts a sign-in lock
- Every profile except `test` uses these rules, so `local` and `docker` need a sign-in too (seeded users: see Data Seeding). Only the automated tests (`test` profile) use an open chain, so a test can call any endpoint without a token

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
- Uses **Flyway** for migrations (SQL in `src/main/resources/db/migration/`, Java migrations in `src/main/java/db/migration/`):
  - V1 schema, V2 seed data, V3 search indexes
  - V4 (Java) allows same-day reservations
  - V5 application settings (mileage unit)
  - V6 maintenance odometer reading
  - V7 (Java) reservation pick-up/return: ACTIVE status, odometer and pick-up/return times
  - V8 branch `active` flag (existing branches become active)
  - V9 roles STAFF (formerly EMPLOYEE), MANAGER and ADMIN; CUSTOMER retired (customer-only users are disabled)
  - V10 `refresh_tokens` (sign-in sessions)
- Most integration tests run on H2, which accepts SQL PostgreSQL rejects. For example `(:date IS NULL OR …)` with a date
  parameter fails on PostgreSQL ("could not determine data type of parameter"); write `CAST(:date AS LocalDate) IS NULL`
  and cover new queries in `PostgresIntegrationTest`
- **Long** type used for all entity IDs
- Audit fields (`created_at`, `updated_at`) handled by `BaseEntity` with JPA auditing
- JPA auditing configured with UTC timezone via `dateTimeProvider` bean in `JpaConfig`

### Testing
- Integration tests use `@SpringBootTest` with `@ActiveProfiles("test")` (H2)
- `PostgresIntegrationTest` runs the `prod` profile against PostgreSQL via **Testcontainers**; it needs Docker and is skipped when Docker is not running
- `SecurityRulesTest` checks the production security rules for every role (and without a token)
- UTC timestamps for consistent auditing across all environments

### Security

> **Before you push and deploy:** wherever this backend runs, set a strong random `JWT_SECRET`
> (at least 32 characters) and deploy the latest version. Without it, the app won't start.
>
> Generate one with e.g. `openssl rand -base64 48`. Only the `local`, `dev` and `test` profiles
> have built-in development secrets.

- Token lifetimes: `JWT_EXPIRATION` (access token, default `15m`) and `JWT_REFRESH_EXPIRATION` (session, default `12h`)
- `ADMIN_PASSWORD` (optional): if set, the seeded `admin` user's password is set from it at startup. Use it to get a working admin in prod; remove it afterwards if the admin should be able to change the password
- Password encoding with BCrypt
- CORS: browsers on other origins are allowed only from the exact origins in `cors.allowed-origins` (`CORS_ORIGINS`; `local` and `dev` allow http://localhost:3000). None by default: serve the UI and API behind one host (the UI's dev server proxies `/api`)
- Swagger UI has an **Authorize** button: paste the `access_token` from `POST /auth/login`
- Method-level security enabled with `@EnableMethodSecurity`

## Data Seeding

Initial data is created via Flyway migration `V2__seed_roles_and_admin.sql`:
- **Admin user**: `admin`/`admin123`
- **Staff user**: `john.doe`/`admin123` (Downtown Branch)
- **Manager user** (`local`, `dev` and `docker` only): `maria.manager`/`admin123` (Downtown Branch)

These passwords work only in the `local`, `dev` and `docker` profiles (set by `db/dev-data/R__dev_seed_user_passwords.sql`). Sign in with the username or the email address (e.g. `admin@rentacar.com`). Elsewhere, set the admin password with `ADMIN_PASSWORD` (see Security).
- Sample branches, cars, customers, and reservations

Extra sample data for `local`, `dev` and `docker` only (`src/main/resources/db/dev-data`):
- `R__dev_seed_maintenance.sql`: maintenance jobs in every state (one in progress, one overdue, one due today, an open safety recall, service history, a cancelled job) and cars coming due for service
- `R__dev_seed_rentals.sql`: one car out on rental and one overdue return

Dates are relative to the day the scripts first run. They are repeatable migrations, so they re-run only when the file changes, and skip rows that already exist.

## MapStruct Integration

The project uses MapStruct for DTO-Entity mapping with Spring component model:
- Configured in `pom.xml` with annotation processor
- Mappers are Spring beans (auto-injected)
- Custom mapping logic for complex transformations

## Important Implementation Details

1. **Car Soft Delete**: Cars use `deleted` boolean flag with `@SQLRestriction`
2. **Reservation Overlaps**: Prevented in service layer with date range queries; maintenance and rentals out past their end date are part of the check
3. **JWT Configuration**: Separate properties for different environments
4. **Validation**: Bean validation annotations on DTOs and entities
5. **Exception Handling**: Global exception handler for consistent API responses

## Testing Strategy

- **Integration Tests**: Full Spring context with H2; PostgreSQL via Testcontainers in `PostgresIntegrationTest`
- **Service Layer Focus**: Core business logic testing
- **Entity Manager Clearing**: Tests simulate transaction boundaries
- **Date-based Logic**: Reservation availability and overlap scenarios
- **Lifecycles**: `RentalLifecycleIntegrationTest` (pick-up/return), `MaintenanceServiceIntegrationTest`, `CarNextReservationIntegrationTest`, `BranchActiveStatusIntegrationTest`
