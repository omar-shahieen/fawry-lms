# Fawry LMS API

A university learning-management REST API built with Spring Boot, Spring Security/JWT, Spring Data JPA, and PostgreSQL. The whole stack (PostgreSQL + API + frontend) is orchestrated by the single compose file at the **monorepo root**: [`../docker-compose.yml`](../docker-compose.yml).

## Run the application

Prerequisite: Docker Desktop (or Docker Engine with the Compose plugin) is installed and running.

From the **monorepo root** (`..`, one level above this directory), run this single command:

```bash
docker compose up -d --build
```

There is no `docker-compose.yml` in this directory anymore — always run Compose from the monorepo root so the API and database end up on the same network. This builds the Alpine-based Java 25 application image, waits for PostgreSQL to become healthy (accepting TCP connections), and then starts the API. The first build downloads dependencies and may take a few minutes.

The API is available at `http://localhost:8080`. Check startup with `http://localhost:8080/actuator/health`; a healthy app returns HTTP 200 with health status `UP`. Swagger UI is at [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html).

To stop the services while keeping database data, run `docker compose down`. Compose stores PostgreSQL data in the persistent `pgdata` volume, so a later `docker compose up -d` reuses it. To wipe the database and re-seed from scratch, run `docker compose down -v`. The sample data is inserted only when the database has no users; existing databases are left unchanged.

See the [root README](../README.md) for URLs, configuration, troubleshooting, and the local (non-Docker) development workflow.

## Configuration

No `.env` file is required for a local demo. Compose supplies development defaults.

* **Docker Compose** reads `.env` from the **monorepo root** (create it there to customize container settings; copy [`../fawry-lms-api/.env.example`](.env.example) as a starting point).
* **Running the API directly on the host** (`./mvnw spring-boot:run` or `npx nx run fawry-lms-api:serve`) reads `fawry-lms-api/.env` via `spring.config.import` — this is where `SPRING_DATASOURCE_URL` pointing at `localhost:5432` applies.

Keep `.env` private; it is ignored by Git. In particular, set a private `JWT_SECRET` of at least 32 characters and a private admin password outside local demos.

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

```bash
./mvnw test      # Windows: .\mvnw.cmd test
./mvnw verify    # Windows: .\mvnw.cmd verify
```
