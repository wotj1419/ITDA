# itda-backend Agent Guide

## Goal
Help implement backend changes for the itda project while preserving existing architecture and behavior.

## Requirement Clarification (MANDATORY)
Before implementing changes, ask the developer enough questions to remove ambiguity. Confirm:
- The exact user story / expected behavior (inputs, outputs, error cases).
- Which endpoints, services, or modules are in scope.
- Data contracts (DTOs, validation rules, DB schema expectations).
- Security implications (auth requirements, roles/permissions).
- Non-functional needs (performance, logging, backward compatibility).
If you recommend an approach or alternative, explicitly state the recommendation and summarize pros/cons for each option.
Explain questions and answers in a clear, easy-to-understand way.
Only proceed once requirements are clear.

## Tech Stack
- Java 17
- Spring Boot 3.2.x
- Spring Security
- MyBatis + MySQL
- Redis (refresh tokens)
- JWT (auth0)
- WebSocket
- springdoc-openapi (Swagger)
- Lombok

## Project Layout (typical)
- `src/main/java` for application code
- `src/main/resources` for configuration and mapper XML
- `src/test/java` for tests

## Build & Run
- `./gradlew bootRun`
- `./gradlew test`

## Git Safety (MANDATORY)
- Do NOT run any git command that discards working-tree changes without explicit developer instruction.
- Forbidden commands (non-exhaustive): `git reset --hard`, `git checkout -- <path>`, `git checkout -- .`, `git restore <path>`, `git restore .`, `git clean -fd`, `git clean -fdx`.
- Prefer reverting via `apply_patch` with a minimal, explicit patch.

## Implementation Guidelines
- Follow existing package structure (controller/service/mapper/domain/dto).
- Keep validation consistent with existing patterns (`@Valid`, `@NotNull`, etc.).
- Do not bypass Spring Security; update config and tests if security rules change.
- When changing API responses, update swagger annotations and related DTOs.
- Avoid introducing new dependencies unless required; ask first if unsure.
- Do not commit secrets; use env config (`.env` / `application-*.yml`).

## Testing
- Add or update unit/integration tests when behavior changes.
- Prefer `@SpringBootTest` or slice tests consistent with existing tests.

## Local Services (DB/Redis/S3)
- Default compose file: `docker-compose.s3.yml` (repo root).
- Default ports:
  - MySQL `3307` → container `3306`
  - Redis `6379`
  - LocalStack S3 `4566`
- Backend `local` profile expects MySQL on `localhost:3306`. If using compose defaults, override:
  - `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/itda_local?...`
  - `DB_USERNAME` / `DB_PASSWORD` as needed (see `itda-backend/env`).

## Test Account (Local Seed)
- Local schema seed inserts a test user (see `itda-backend/src/main/resources/sql/schema-local.sql`).
- Email: `test@gmail.com`
- Password: `qweqwe123`

## Playwright/MCP UI Smoke Notes
- The frontend targets `http://localhost:8080/api` by default. For UI exploration/screenshots, the backend must be running so that login/project creation and other core flows work.
- AI integrations (e.g. prompt generation / scenario generation) may fail locally without GCP configuration/credentials, so when doing UI demos/reviews you should also validate **error/loading/empty states**.

## Documentation
- Update `docs/` if endpoints or contracts change.
