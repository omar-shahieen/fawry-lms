# University LMS — Front-End

React front-end for the university Learning Management System API.

- Build plan: [`docs/frontend-build.md`](docs/frontend-build.md)
- Endpoints & access rules: [`docs/lms-endpoints.md`](docs/lms-endpoints.md)
- Product overview: [`docs/overview.md`](docs/overview.md)
- Agent instructions: [`AGENTS.md`](AGENTS.md)

## Commands

```bash
npm install
npm run dev       # http://localhost:5173 (API at http://localhost:8080)
npm run build
npm run lint
npm run gen:api   # regenerate src/api/schema.d.ts (backend must be running)
```

## Stack

Vite 8 · React 19 · TypeScript 7 · react-router 8 (declarative) · TanStack Query 5 · Tailwind CSS 4 · openapi-typescript 7 · react-markdown + remark-gfm
