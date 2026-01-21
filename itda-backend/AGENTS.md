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

## Documentation
- Update `docs/` if endpoints or contracts change.
