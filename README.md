# Fawry LMS Full-Stack Monorepo

Welcome to the **Fawry LMS Monorepo**, managed with **Nx** and containerized with **Docker Compose**. This repository houses both the backend API and the frontend client with a unified Git history.

---

## Architecture Overview

```
fawry-lms/
├── .nx/                       # Nx computation cache & workspace data
├── docker-compose.yml         # THE only compose file (Postgres + API + Frontend)
├── nx.json                    # Nx workspace configuration
├── package.json               # Root workspace package with cross-service scripts
├── fawry-lms-api/             # Spring Boot 4 / Java 25 REST API
│   ├── Dockerfile             # Multi-stage Eclipse Temurin Alpine image
│   ├── pom.xml                # Maven configuration
│   ├── src/                   # Backend source code
│   └── project.json           # Nx project targets (serve, build, test, docker)
└── fawry-lms-front-end/       # React 19 / Vite / Tailwind v4 Single-Page Application
    ├── Dockerfile             # Node build + Nginx Alpine runtime
    ├── nginx.conf             # SPA routing fallback & /api reverse proxy
    ├── src/                   # Frontend source code
    └── project.json           # Nx project targets (dev, build, lint, docker)
```

---

## Prerequisites

- **Node.js**: v20+ (recommended v24)
- **Java JDK**: 21+ (configured for 25)
- **Docker & Docker Compose**: v2.20+

---

## Run the app (Docker — recommended)

Everything starts with **one command from the repository root**:

```bash
docker compose up -d --build
```

There is exactly one compose file, at the repository root — never run `docker compose` from inside `fawry-lms-api/`.
The first run builds both images and may take a few minutes. Compose starts PostgreSQL first, waits until it is
**healthy** (accepting TCP connections), and only then starts the API.

### What runs

| Service | Container | Address |
|---|---|---|
| Frontend (React SPA) | `fawry-lms-ui` | http://localhost:5173 |
| Backend API | `fawry-lms-api` | http://localhost:8080 |
| Swagger UI | — | http://localhost:8080/swagger-ui.html |
| API health | — | http://localhost:8080/actuator/health |
| PostgreSQL | `fawry-lms-db` | `localhost:5432` — db `lms`, user `lms_user`, password `lms_pass` |

The browser never hits CORS: the SPA calls `/api/...` on its own origin and Nginx proxies those requests to the
API container over the compose network.

### Check that everything is up

```bash
docker compose ps                                # postgres: healthy, api + frontend: running
curl http://localhost:8080/actuator/health       # {"status":"UP"}
docker compose logs api | tail -20               # must end with: "Started LmsApplication"
```

### Sign in

Open http://localhost:5173, or use Swagger → `POST /api/auth/login`. These accounts are seeded automatically on
the first start against an **empty** database:

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@lms.com` | `Admin123!` |
| **Instructor** | `instructor1@lms.com` | `Instructor123!` |
| **Instructor** | `instructor2@lms.com` | `Instructor123!` |
| **Student** | `student1@lms.com` … `student6@lms.com` | `Student123!` |

The seeder runs on every application start but exits immediately when users already exist, so re-running
`docker compose up` against the same volume never duplicates data.

### Stop, restart, reset

| Command | Effect |
|---|---|
| `docker compose down` | Stop everything, **keep** the database (no re-seed on next start) |
| `docker compose up -d` | Start again with the existing database |
| `docker compose down -v` | Stop and **delete** the database volume — the next start re-seeds from scratch |
| `docker compose up -d --build` | Rebuild images after code changes, then start |
| `docker compose logs -f api` | Follow the API logs |

### Configuration (optional)

Compose ships with development defaults; no `.env` file is required. To customize, create `.env` **in the
repository root** (copy `fawry-lms-api/.env.example` as a starting point), edit it, and restart:

| Variable | Compose default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | Host port for the API |
| `POSTGRES_PORT` | `5432` | Host port for PostgreSQL |
| `FRONTEND_PORT` | `5173` | Host port for the frontend |
| `SPRING_DATASOURCE_DB` | `lms` | PostgreSQL database name |
| `SPRING_DATASOURCE_USERNAME` | `lms_user` | PostgreSQL username |
| `SPRING_DATASOURCE_PASSWORD` | `lms_pass` | PostgreSQL password |
| `JWT_SECRET` | `local-development-only-secret-change-me` | JWT signing secret — always set your own outside a local demo |
| `JWT_ACCESS_TOKEN_EXPIRATION` | `PT15M` | Access-token lifetime |
| `JWT_REFRESH_TOKEN_EXPIRATION` | `P7D` | Refresh-token lifetime |
| `ADMIN_SEED_EMAIL` | `admin@lms.com` | Admin email used when seeding an empty database |
| `ADMIN_SEED_PASSWORD` | `Admin123!` | Admin password used when seeding an empty database |

Note: `fawry-lms-api/.env` is only read when you run the API **directly on the host** (Nx/Maven); Docker Compose
reads `.env` from the repository root only.

### Troubleshooting

**The API logs `Connection refused` / `Connect timed out` (cannot connect to the database):**

1. `docker compose ps` — PostgreSQL must be `healthy` before the API starts. If it is not, check
   `docker compose logs postgres`.
2. `docker compose logs api | tail` — confirm which host/timing the failure happened on.
3. `Bind for 0.0.0.0:5432 failed: port is already allocated` — something else already owns the port (a leftover
   container or a locally installed PostgreSQL). Find it with `docker ps -a` / `ss -ltn | grep 5432`, stop it,
   then run `docker compose up -d` again. Only one Postgres may use the port at a time.
4. Everything shows healthy but the API still times out — container-to-container traffic is being dropped by the
   host firewall. Inspect the rules with `sudo iptables-legacy -L FORWARD -n`; if the policy is `DROP` and the
   rules only mention `docker0`, they are stale rules from an older Docker installation and are blocking the
   compose network. Restore them with:
   ```bash
   sudo iptables-legacy -t filter -P FORWARD ACCEPT
   ```
   Docker's own (nftables) rules remain the effective policy. Re-run `docker compose restart api` afterwards.

**Data looks wrong or you want a clean demo database:**

```bash
docker compose down -v && docker compose up -d --build   # wipe the DB volume and re-seed
```

---

## Local Development with Nx

### 1. Setup Dependencies
```bash
# Install root & workspace npm dependencies (including Nx)
npm install
```

### 2. Running Services

The API still needs PostgreSQL. Start just the database from Docker, then run the apps on the host:

```bash
docker compose up -d postgres     # publishes localhost:5432
```

| Command | Action |
|---|---|
| `npx nx run fawry-lms-front-end:dev` | Start the Vite React development server on [http://localhost:5173](http://localhost:5173) (proxies `/api` → `localhost:8080`) |
| `npx nx run fawry-lms-api:serve` | Start the Spring Boot backend on [http://localhost:8080](http://localhost:8080) (reads `fawry-lms-api/.env`) |
| `npx nx run-many -t dev serve --parallel` | Start both services concurrently |
| `npx nx run-many -t build` | Build both frontend and backend |
| `npx nx run-many -t lint` | Run linters across projects |
| `npx nx show projects` | Display all detected workspace projects |

---

## Pre-Seeded Test Accounts

The backend automatically seeds test credentials on startup (see the [Sign in](#sign-in) table above):
`admin@lms.com` / `Admin123!`, `instructor1@lms.com` and `instructor2@lms.com` / `Instructor123!`, and
`student1@lms.com` … `student6@lms.com` / `Student123!`.

---

## Git Commit History & Traceability

Both repositories have been combined into a unified Git history:
- Every past commit is preserved under `fawry-lms-api/` and `fawry-lms-front-end/`.
- File history (`git log -- <filepath>`) and line attribution (`git blame <filepath>`) work back to each project's initial commit.
