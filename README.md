# Fawry LMS Full-Stack Monorepo

Welcome to the **Fawry LMS Monorepo**, managed with **Nx** and containerized with **Docker Compose**. This repository houses both the backend API and the frontend client with a unified Git history.

---

## Architecture Overview

```
fawry-lms/
├── .nx/                       # Nx computation cache & workspace data
├── docker-compose.yml         # Full-stack orchestrator (Postgres + API + Frontend)
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

## Quick Start with Docker (Recommended)

To run the entire platform (PostgreSQL database, Spring Boot API, and React frontend) with a single command:

```powershell
docker compose up -d --build
```

- **Frontend (Web Application)**: [http://localhost:5173](http://localhost:5173)
- **Backend API**: [http://localhost:8080](http://localhost:8080)
- **Swagger / OpenAPI Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **PostgreSQL Database**: `localhost:5432` (`db: lms`, `user: lms_user`, `password: lms_pass`)

To stop all services:
```powershell
docker compose down
```

---

## Local Development with Nx

### 1. Setup Dependencies
```powershell
# Install root & workspace npm dependencies (including Nx)
npm install
```


### 2. Running Services

| Command | Action |
|---|---|
| `npx nx run fawry-lms-front-end:dev` | Start the Vite React development server on [http://localhost:5173](http://localhost:5173) |
| `npx nx run fawry-lms-api:serve` | Start the Spring Boot backend on [http://localhost:8080](http://localhost:8080) |
| `npx nx run-many -t dev serve --parallel` | Start both services concurrently |
| `npx nx run-many -t build` | Build both frontend and backend |
| `npx nx run-many -t lint` | Run linters across projects |
| `npx nx show projects` | Display all detected workspace projects |

---

## Pre-Seeded Test Accounts

The backend automatically seeds test credentials on startup:

| Role | Email | Password |
|---|---|---|
| **Admin** | `admin@lms.com` | `Admin123!` |
| **Instructor** | `instructor@lms.com` | `Instructor123!` |
| **Student** | `student@lms.com` | `Student123!` |

---

## Git Commit History & Traceability

Both repositories have been combined into a unified Git history:
- Every past commit is preserved under `fawry-lms-api/` and `fawry-lms-front-end/`.
- File history (`git log -- <filepath>`) and line attribution (`git blame <filepath>`) work back to each project's initial commit.
