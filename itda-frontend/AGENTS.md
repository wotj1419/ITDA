# itda-frontend Agent Guide

## Goal
Help implement frontend changes for the itda project while preserving existing UX and architecture.

## Requirement Clarification (MANDATORY)
Before implementing changes, ask the developer enough questions to remove ambiguity. Confirm:
- The exact user flow and UI states (empty/loading/error/success).
- Which pages/components are in scope.
- Data contracts (API endpoints, DTO fields, pagination).
- Visual/design expectations (layout, colors, typography).
- Compatibility constraints (responsive breakpoints, browser support).
If you recommend an approach or alternative, explicitly state the recommendation and summarize pros/cons for each option.
Explain questions and answers in a clear, easy-to-understand way.
Only proceed once requirements are clear.

## Tech Stack
- Vue 3 + Vite
- TypeScript
- Vue Router
- Pinia
- Vue Flow

## Project Layout (typical)
- `src/` for app code (components, views, stores, router)
- `public/` for static assets

## Dev Scripts
- `npm run dev`
- `npm run build`
- `npm run preview`

## Implementation Guidelines
- Use the existing Composition API patterns and TypeScript types.
- Keep stores in Pinia; avoid ad-hoc global state.
- Reuse shared components and styles; avoid duplicated UI logic.
- When changing API contracts, update types and any affected views.
- Avoid new dependencies unless required; ask first if unsure.

## Testing & QA
- If there are no automated tests, validate changes manually in dev mode.
- Check responsive behavior for common breakpoints.
- Build check (TypeScript only): `npx vue-tsc -b`

## Local E2E Smoke (Playwright/MCP)
- Ensure frontend dev server is running: `npm run dev -- --host` (default `http://localhost:5173`).
- Backend API should be reachable at `http://localhost:8080` for real login flows.
- If Playwright browsers are missing, install once: `npx playwright install chromium`.

### Test Account (Local/Dev)
- Email: `test@gmail.com`
- Password: `qweqwe123`

### Playwright MCP Notes (UI Exploration/Screenshots)
- **Viewport matters:** Playwright MCP renders/captures based on the configured **viewport size**, not the OS browser "maximized" window. If the viewport is narrow (e.g. ~780px), responsive breakpoints may kick in and layouts can look different.
  - Recommended: set viewport to `1440x900` (or demo target `1920x1080`) before capturing.
- **Screenshot output path:** Depending on the environment, MCP may first save screenshots under `/tmp/playwright-mcp-output/<runId>/...`. You may need to copy them into your report folder.
- **Browser/channel mismatch:** If MCP errors with `Chromium distribution 'chrome' is not found at /opt/google/chrome/chrome`, it may be configured to use the `chrome` channel and require a system Chrome (or Chrome for Testing).
  - Recommended: install via Playwright (`npx playwright install chrome`) or install Chrome/Chromium via system packages and retry.
  - Alternative: adjust MCP configuration to use Playwright-downloaded `chromium` instead of the `chrome` channel (if supported).

## Documentation
- Update `docs/` if user flows or UI contracts change.
