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

## Documentation
- Update `docs/` if user flows or UI contracts change.
