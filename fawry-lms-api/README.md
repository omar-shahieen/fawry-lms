# Fawry LMS API

A university learning-management REST API built with Spring Boot, Spring Security/JWT, Spring Data JPA, and PostgreSQL. The API and database run together with Docker Compose.

## Run the application

Prerequisite: Docker Desktop (or Docker Engine with the Compose plugin) is installed and running.

From the repository root, run this single command:

```powershell
docker compose up -d --build
```

This builds the Alpine-based Java 25 application image, waits for PostgreSQL to become healthy, and starts both services. The first build downloads dependencies and may take a few minutes. To use the legacy standalone Compose command, run `docker-compose up -d --build` instead.

The API is available at `http://localhost:8080`. Check startup with `http://localhost:8080/actuator/health`; a healthy app returns HTTP 200 with health status `UP`. Swagger UI is at [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html).

To stop the services while keeping database data, run `docker compose down`. Compose stores PostgreSQL data in the persistent `pgdata` volume, so a later `docker compose up -d` reuses it. The sample data is inserted only when the database has no users; existing databases are left unchanged.

## Configuration

No `.env` file is required for a local demo. Compose supplies development defaults. To customize them, copy `.env.example` to `.env` in the repository root and set the values before starting Compose. Keep `.env` private; it is ignored by Git. In particular, set a private `JWT_SECRET` of at least 32 characters and a private admin password outside local demos.

| Variable | Compose default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | Host port for the API |
| `SPRING_DATASOURCE_DB` | `lms` | PostgreSQL database name |
| `SPRING_DATASOURCE_USERNAME` | `lms_user` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | `lms_pass` | PostgreSQL password |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/lms` | Host-run app URL; Compose configures the internal `postgres` service URL automatically |
| `JWT_SECRET` | `local-development-only-secret-change-me` | JWT signing secret; provide your own private value outside a local demo |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `PT15M` | Access-token lifetime |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `P7D` | Refresh-token lifetime |
| `ADMIN_SEED_EMAIL` | `admin@lms.com` | Admin email used on the first seed of an empty database |
| `ADMIN_SEED_PASSWORD` | `Admin123!` | Admin password used on the first seed of an empty database |

Changing the admin seed variables does not alter an account already present in the persistent database.

## Seeded accounts

The seeder creates the following BCrypt-hashed demo accounts on the first application startup against an empty database. In Swagger UI, open the `POST /api/auth/login` operation, enter the email and password, and execute it. The response contains an access token and refresh token.

| Role | Email | Password |
| --- | --- | --- |
| Admin | `admin@lms.com` | `Admin123!` |
| Instructor | `instructor1@lms.com` | `Instructor123!` |
| Instructor | `instructor2@lms.com` | `Instructor123!` |
| Student | `student1@lms.com` through `student6@lms.com` | `Student123!` |

The admin email and password in this table are the Compose defaults; if `ADMIN_SEED_EMAIL` or `ADMIN_SEED_PASSWORD` was customized before the first seed, use those configured values instead. Instructor and student demo passwords are fixed by the seeder and should only be used for local demonstration.

## Tests and build

Run the full test suite and package the application with Maven:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
```
