# Job Application Tracker

A full-stack web app for organizing a job search: record every application, move it through the
hiring pipeline (Wishlist → Applied → Phone Screen → Interview → Offer), and see your progress on a dashboard.

Built with **Spring Boot 3 (Java 17)** and **Angular 17**. Includes JWT authentication, a PostgreSQL schema
managed by Flyway, automated tests on both ends, Docker, and CI.

![Dashboard](docs/screenshots/dashboard.jpg)

## Features

- **Accounts**: register and log in with JWT-based, stateless authentication; passwords hashed with BCrypt
- **Applications CRUD**: company, position, location, salary, job link, date and notes, with validation on both client and server
- **Pipeline status**: change status inline from the list (HTTP `PATCH`)
- **Search, filter, sort, paginate**: all done server-side with JPA Specifications
- **Dashboard**: totals, response rate, a doughnut chart by status and a bar chart of applications per week (Chart.js)
- **Data isolation**: users can only ever see and change their own records
- **API docs**: interactive Swagger UI
- **Demo account**: seeded automatically (`demo@example.com` / `password123`)

| Applications list | Edit form |
|---|---|
| ![List](docs/screenshots/applications.jpg) | ![Form](docs/screenshots/edit-form.jpg) |

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17 (standalone components, signals, new control flow), Reactive Forms, RxJS, Chart.js, SCSS |
| Backend | Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Spring Security, Bean Validation, jjwt, springdoc-openapi |
| Database | PostgreSQL 16 (production), H2 in-memory (development and tests), Flyway migrations |
| Testing | JUnit 5, Mockito, MockMvc, AssertJ; Jasmine + Karma |
| DevOps | Docker multi-stage builds, docker-compose, nginx, GitHub Actions |

## Quick start

### Option A: run locally (no database to install)

Requirements: Java 17+, Node 18+.

```bash
# Terminal 1: API on http://localhost:8080 (H2 in-memory DB, demo data seeded)
cd backend
./mvnw spring-boot:run

# Terminal 2: UI on http://localhost:4200 (proxies /api to the backend)
cd frontend
npm install
npm start
```

Open http://localhost:4200 and click **"Use the demo account"**.

- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:jobtracker`, user `sa`, no password)

### Option B: Docker (PostgreSQL + API + nginx)

```bash
docker compose up --build
```

Open http://localhost:8091.

### Running the tests

```bash
cd backend && ./mvnw test
cd frontend && npx ng test --watch=false --browsers=ChromeHeadless
```

## Documentation

**[docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md)** is a detailed walkthrough of the architecture, the
request lifecycle, security, the data model, every API endpoint, the frontend, testing and deployment.

The source code is also heavily commented, so you can read it top to bottom as a tutorial.

## Project structure

```
job-tracker/
├── backend/            Spring Boot REST API
│   └── src/main/java/com/portfolio/jobtracker/
│       ├── auth/           register / login / me endpoints
│       ├── user/           User entity + repository
│       ├── security/       JWT creation/validation + request filter
│       ├── application/    job application entity, repository, service, controller, DTOs
│       ├── common/         error handling, pagination wrapper
│       └── config/         security, OpenAPI, clock, demo data
├── frontend/           Angular single-page app
│   └── src/app/
│       ├── core/           models, auth (service/interceptor/guards), API services
│       ├── features/       auth pages, dashboard, applications list + form
│       └── shared/         pipes and helpers
├── docs/               documentation and screenshots
├── docker-compose.yml
└── .github/workflows/ci.yml
```
