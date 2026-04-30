# BazarFlow Core

BazarFlow Core is an internal operations platform for chilled and frozen food distributors. The practical business problem is simple: many distributors still coordinate orders, stock, expiry dates, pricing, and delivery through spreadsheets, chat messages, and manual checks. Once SKUs, batches, outlets, and delivery windows grow, that workflow becomes hard to audit and easy to break.

The system is built as a Spring Boot modular monolith with clear module boundaries and a path to service extraction later. It covers OAuth2 security, idempotent order submission, expiry-aware inventory reservation, event-driven workflows, PostgreSQL persistence, and a React operations dashboard.

The backend focuses on the business-critical paths: order intake, inventory reservation, batch expiry, pricing, delivery planning, auditability, and operations reporting. The frontend is designed for staff who need to scan risk and act quickly.

## Current Status

The repository currently includes:

- Spring Boot 3.5.14 backend scaffolded with a Java 17+ baseline and Java 21 LTS verification
- Spring Security resource server, local Keycloak realm import, Flyway, PostgreSQL, Kafka, Springdoc, and internal module-boundary verification
- Problem-details error responses and `X-Correlation-Id` propagation for API requests
- Business module packages created for `common`, `identity`, `partner`, `catalog`, `inventory`, `pricing`, `ordering`, `fulfillment`, `audit`, and `reporting`
- `GET /api/platform/status` exposes the current module map
- Partner APIs for retailer creation, retailer lookup, outlet creation, and credit-status updates
- Catalog APIs for products, SKUs, storage classes, and SKU lifecycle status
- Inventory APIs for lot receiving, stock movement recording, SKU availability lookup, and reservation lookup
- Pricing APIs for price books, deterministic SKU price rules, and quote calculation
- Order APIs for priced draft creation, idempotent submission with stock reservation, acceptance, delivery outcomes, order lookup, and status timeline
- Fulfillment APIs for pick wave generation, planned dispatch jobs, completion/failure recording, reservation consumption, and SLA-risk lookup
- Audit APIs for order and inventory event lookup with request actor and correlation metadata
- Reporting API for dashboard KPIs, work queue, risk watch, throughput, dispatch backlog, and audit timeline
- React 19.2 and Vite 8.0 operations console connected to live platform and reporting endpoints, with local fallback data
- Swagger bearer-token authentication wired to the local Keycloak realm
- Docker Compose, Taskfile, ADRs, HTTP examples, and GitHub Actions CI added

## Tech Stack

- Java 17+ baseline, tested on Java 21 LTS
- Spring Boot 3.5.14 modular monolith
- PostgreSQL 18.3
- Keycloak 26.6.1
- Kafka
- React 19.2
- Vite 8.0
- TypeScript

## Quickstart

Prerequisites:

- JDK 17 or newer. JDK 21 LTS is recommended.
- Docker Desktop
- Node.js 24 or another Vite 8 compatible Node release
- Task, optional

Run the backend tests:

```bash
cd server
./mvnw test
```

Run the frontend checks:

```bash
cd ops-console
npm install
npm run lint
npm run build
```

Start local infrastructure:

```bash
docker compose --env-file .env.example up -d
```

Start the API:

```bash
cd server
./mvnw spring-boot:run
```

Start the ops console:

```bash
cd ops-console
npm run dev
```

Useful URLs:

- API health: `http://localhost:8080/actuator/health`
- Platform status: `http://localhost:8080/api/platform/status`
- Reporting dashboard: `http://localhost:8080/api/reporting/dashboard`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Keycloak admin: `http://localhost:8081`
- Ops console: `http://localhost:5173`

Local Keycloak users use the password `bazarflow`:

| Username | Role |
|---|---|
| `ops.manager` | `OPS_MANAGER` |
| `warehouse.operator` | `WAREHOUSE` |
| `sales.operator` | `SALES` |
| `dispatch.operator` | `DISPATCH` |
| `audit.viewer` | `AUDITOR` |

## Git Workflow

Implementation changes should be committed in small, verified steps and pushed to GitHub for backup. Check `git status --short --branch` before work, verify with tests or builds, commit with a clear message, then push.

## Database Portability

PostgreSQL is the primary database for local development and production-like testing. Core schema design stays relational and portable where practical so the data model can be adapted to MySQL or Oracle if an environment requires it. PostgreSQL-specific features such as JSONB should stay isolated and documented.
