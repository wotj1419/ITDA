# Public Demo Workspace Design

## Goal

Make the public Vercel preview usable as a portfolio demo without a deployed backend. Visitors can navigate the dashboard, favorites, shared projects, and trash; create a project; and select generated scenarios from a sidebar.

## Scope

The demo includes these public routes:

- `/dashboard`
- `/favorites`
- `/shared`
- `/trash`
- `/projects/:id`
- `/projects/:projectId/scenes/:sceneId`

The existing Vercel SPA rewrite remains the only hosting requirement, so direct navigation and browser refresh continue to work on every route.

## Architecture

Add an explicit public-demo mode to the frontend. In this mode the Pinia project and scene stores use a small local data repository instead of importing API functions directly. The repository owns seeded portfolio data and persists user actions to `localStorage` under a versioned, demo-only key.

The production API implementation remains unchanged and remains selected when public-demo mode is off. Demo mode is enabled only for the preview deployment by a Vite environment variable, never inferred from authentication state.

## Demo Data

On a first visit, the repository creates these records:

- Three active projects: one owned project, one shared project, and one favorite project.
- One deleted project, visible in the trash page.
- At least one favorite active project.
- At least one shared project, marked with a non-owner role.
- Three draft scenarios for every seeded active project.

Project and scene fields conform to the existing `Project`, `ProjectDetail`, and `Scene` types. IDs are stable for seed data and newly created IDs are generated locally.

## User Flows

### Sidebar navigation

The existing default layout exposes the dashboard, favorites, shared, and trash entries. Public-demo mode removes the authentication gate from the routes required by this spec. Each target route loads from the local repository and renders without a network request.

### Create a project with scenarios

The dashboard's `새 프로젝트` action opens the existing `새 프로젝트 만들기` modal. The visitor can open its `AI 시나리오 생성` section, select a scene count, and press the existing generation control; public-demo mode presents the existing completion state without calling an AI service. When the visitor submits a project title, demo mode creates the project and automatically creates exactly three draft scenarios with fixed Korean portfolio copy. The app then navigates to the new project detail route.

### Select a scenario

Project detail displays the three scenarios in its existing scene view. Selecting a scenario navigates to its existing scene-edit route and displays its stored title and description. The editor continues to use its existing right-side properties panel: selecting one of the seeded example nodes opens that panel. The creation flow therefore has an unbroken path from project creation to scenario selection and the existing right-side editor panel.

### Favorites, shared, and trash

Favorites displays projects selected by the existing favorite state. Shared displays the seeded project where the visitor is a non-owner. Trash displays the seeded deleted project.

Moving an active project to the trash, restoring a trashed project, and toggling a favorite update only the demo repository. Their effects survive refresh in the same browser. Permanent deletion, member invitations, collaboration, media upload, authentication, and AI generation are out of scope and must not call the backend in demo mode.

## Error Handling

If a project or scene ID does not exist in the local repository, the existing empty/not-found presentation is used rather than issuing a backend request. If browser storage is unavailable or malformed, the repository resets to the seed data and the pages remain usable.

## Testing

Automated tests verify:

- Public-demo mode returns seeded active, shared, favorite, and deleted data.
- The existing project creation modal is opened from the dashboard and, in public-demo mode, its AI-generation control completes without a network request.
- Creating a project creates exactly three draft scenarios and makes them retrievable by project ID.
- Local favorite and trash mutations affect the corresponding lists.
- All in-scope routes are public in the router configuration.

The build must pass. A deployed-preview smoke test checks direct navigation to the dashboard, favorites, shared, trash, a project detail page, and a scenario route, including a refresh on one nested route.

## Constraints

- Use Vue 3, TypeScript, Vue Router, and Pinia already present in the repository.
- Add no dependencies.
- Keep all persistent data local to the visitor's browser; do not deploy or expose a backend.
- Keep existing non-demo API behavior unchanged.
- Work only on the existing `public-dashboard-preview` branch.
