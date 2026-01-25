# Frontend Refactor Plan (itda-frontend)

Date: 2026-01-25
Owner: Codex (per user request)

## Goals
- Reduce duplication across scene editor panels.
- Split oversized stores into focused modules.
- Separate API vs UI types and unify mappings.
- Standardize async store patterns.
- Modularize ProjectDetail page logic.

## Scope (Approved)
- Apply all previously suggested refactors:
  - Scene node store decomposition.
  - Scene editor panel common logic extraction.
  - ProjectDetail page split into smaller components/composables.
  - API types vs UI types separation (with mapping layer).
  - Shared async action pattern across stores.
  - API client cleanups without adding env vars.

## Constraints
- Environment variables are NOT introduced now (keep base URL as-is).
- UI/behavior changes are allowed during refactor.
- Folder moves and type relocations are allowed.
- Single combined change set (no staged PRs).

## Proposed Structure Changes

### 1) Scene Node Store Decomposition
Current: `src/stores/sceneNode.ts` (large, mixed concerns)
Proposed modules (examples):
- `src/stores/sceneNode/index.ts` (store wiring + exports)
- `src/stores/sceneNode/mappers.ts` (API <-> UI node mapping)
- `src/stores/sceneNode/validators.ts` (connection rules, guards)
- `src/stores/sceneNode/history.ts` (position/history tracking)
- `src/stores/sceneNode/nodeFactory.ts` (node creation helpers)
- `src/stores/sceneNode/sideEffects.ts` (WS subscriptions, sync)

### 2) Scene Editor Panel Common Logic
Common flow: generate prompt, approve prompt, generate job, update status, toasts
Proposed:
- `src/composables/useNodeGeneration.ts`
  - Accepts node id + input builder
  - Handles job start/polling + status + toasts
  - Returns helpers: `generatePrompt`, `approvePrompt`, `runJob`
- Panels keep only view/form specifics.

### 3) Project Detail Page Split
Current: `src/pages/ProjectDetailPage.vue` (tabs, data loading, preview caching)
Proposed:
- `src/pages/project/ProjectDetailPage.vue` (thin shell)
- `src/pages/project/composables/useProjectDetail.ts`
- `src/pages/project/sections/SceneTab.vue`
- `src/pages/project/sections/StoryTab.vue`
- `src/pages/project/sections/ObjectTab.vue`

### 4) Type Separation: API vs UI
Current: `src/types/index.ts` mixed API/UI; `src/types/node.ts` UI node types
Proposed:
- `src/types/api/*` (API DTOs)
- `src/types/ui/*` (UI/view model types)
- `src/types/mappers/*` (explicit transforms)

### 5) Standardized Async Store Helpers
Current: repeated `isLoading/error` patterns in `project`, `scene`, `scenario`
Proposed:
- `src/stores/helpers/useAsyncAction.ts`
  - Wraps async actions, standardizes loading and errors
  - Optional error mapper for consistent messages

### 6) API Client Cleanups (No Env Vars)
Current: `src/services/api/client.ts` hardcoded base URL
Proposed:
- Keep base URL as constant but centralize in one file
- Extract token get/set + redirect handling to helper(s)

## Implementation Plan (Single Pass)

1) Type reorg and mapping scaffolding
   - Move DTO types to `src/types/api/*`
   - Move UI/view types to `src/types/ui/*`
   - Add explicit mappers and update imports

2) Store helpers
   - Create `useAsyncAction` (or similar)
   - Refactor `project`, `scene`, `scenario` stores to use it

3) Scene node store split
   - Carve helpers into modules
   - Keep store API stable (public functions & state)
   - Update imports across app

4) Panel composable extraction
   - Create `useNodeGeneration`
   - Update `ShotPanel`, `VideoPanel`, `MasterImagePanel`, `StoryboardGridPanel`

5) Project detail page modularization
   - Extract tabs to components
   - Move preview logic to composable
   - Ensure router links & data loading remain functional

6) API client refactor (no env vars)
   - Isolate auth/redirect logic
   - Keep URL constant for now

## Risk & Mitigation
- Import churn: use consistent path aliases or index exports for migrated types.
- Behavioral drift: allowed, but avoid breaking type contracts with backend.
- Runtime errors after moves: run TypeScript build and fix fast.

## Verification
- `npm run build` in `itda-frontend`
- Manual smoke:
  - Auth flow
  - Project detail tabs
  - Scene editor panel generate/approve flows
  - Scenario drawer steps

