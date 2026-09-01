# Public Demo Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Make the preview branch a backend-free portfolio demo with navigable project pages and a creation modal that produces three sample scenarios.

**Architecture:** Add an explicit VITE_PUBLIC_DEMO switch and a browser-local repository for projects, favorites, trash, and scenes. Project and scene stores use that repository only in demo mode. Existing Vue pages retain their layouts; the dashboard opens the existing modal and the existing editor loads mock nodes so its right-side properties panel remains usable.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router, Vite, Node.js built-in test runner, Playwright.

**Spec:** docs/superpowers/specs/2026-08-31-public-demo-workspace-design.md

## Global Constraints

- Use Vue 3, TypeScript, Vue Router, and Pinia already present in the repository.
- Add no dependencies.
- Keep all persistent data local to the visitor's browser; do not deploy or expose a backend.
- Keep existing non-demo API behavior unchanged.
- Work only on the existing public-dashboard-preview branch.
- Enable VITE_PUBLIC_DEMO=true only for this Vercel preview branch; normal builds must retain login protection.

---

### Task 1: Public-demo runtime and local repository

**Files:**
- Create: itda-frontend/src/services/demo/publicDemoRepository.ts
- Modify: itda-frontend/src/services/config.ts
- Create: itda-frontend/tests/public-demo-repository.test.ts

**Interfaces:**
- Produces isPublicDemo: boolean from src/services/config.ts.
- Produces createPublicDemoRepository(storage) with listProjects(), getProject(id), createProject(data), listScenes(projectId), getDeletedProjects(), moveToTrash(id), restoreProject(id), deleteProjectPermanently(id), isFavorite(id), and toggleFavorite(id).
- createProject(data) returns a Project and always creates exactly three Scene records.

- [ ] **Step 1: Write the failing repository test**

~~~ts
import assert from 'node:assert/strict'
import test from 'node:test'
import { createPublicDemoRepository } from '../src/services/demo/publicDemoRepository.ts'

test('creates three draft scenarios for a new demo project', () => {
  const values = new Map<string, string>()
  const repository = createPublicDemoRepository({
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  })

  const project = repository.createProject({ title: '포트폴리오 영상', description: '', genre: 'sf' })
  const scenes = repository.listScenes(project.projectId)

  assert.equal(scenes.length, 3)
  assert.deepEqual(scenes.map((scene) => scene.status), ['DRAFT', 'DRAFT', 'DRAFT'])
})

test('persists favorite and trash changes in supplied storage', () => {
  const values = new Map<string, string>()
  const storage = {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  }
  const first = createPublicDemoRepository(storage)
  const project = first.listProjects()[0]!
  first.toggleFavorite(project.projectId)
  first.moveToTrash(project.projectId)

  const second = createPublicDemoRepository(storage)
  assert.equal(second.listProjects().some((item) => item.projectId === project.projectId), false)
  assert.equal(second.getDeletedProjects().some((item) => item.projectId === project.projectId), true)
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: node --experimental-strip-types --test tests/public-demo-repository.test.ts

Expected: FAIL because publicDemoRepository.ts does not exist.

- [ ] **Step 3: Write the minimal local repository**

Create a versioned local-storage state containing three active projects, one deleted project, favorite project IDs, and a scenesByProject map. Seed all active projects with three draft Korean scenarios. Reset malformed local-storage content to the seed state.

~~~ts
export function createPublicDemoRepository(storage: StorageLike) {
  const state = readOrSeed(storage)

  return {
    listProjects: () => state.projects.filter((project) => !project.isDeleted),
    createProject: (data: CreateProjectRequest) => {
      const project = makeProject(data)
      state.projects.push(project)
      state.scenesByProject[project.projectId] = makeGeneratedScenes(project.projectId)
      persist(storage, state)
      return project
    },
    listScenes: (projectId: number) => [...(state.scenesByProject[projectId] ?? [])],
  }
}
~~~

Implement every interface method declared above and persist every mutation. Append this exact configuration export:

~~~ts
export const isPublicDemo = import.meta.env.VITE_PUBLIC_DEMO === 'true'
~~~

- [ ] **Step 4: Run test to verify it passes**

Run: node --experimental-strip-types --test tests/public-demo-repository.test.ts

Expected: PASS.

- [ ] **Step 5: Commit**

~~~bash
git add itda-frontend/src/services/config.ts itda-frontend/src/services/demo/publicDemoRepository.ts itda-frontend/tests/public-demo-repository.test.ts
git commit -m "feat: 공개 데모 데이터 저장소 추가"
~~~

### Task 2: Public routes and store delegation

**Files:**
- Modify: itda-frontend/src/router/index.ts
- Modify: itda-frontend/src/stores/project.ts
- Modify: itda-frontend/src/stores/scene.ts
- Create: itda-frontend/tests/public-demo-routes.test.mjs

**Interfaces:**
- Consumes isPublicDemo and the repository from Task 1.
- Project store keeps its existing action names and return types.
- Scene store keeps orderedScenes, loadScenes(projectId), addScene(data), and addScenes(dataList).

- [ ] **Step 1: Write the failing route test**

~~~js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('all portfolio-demo routes are public', async () => {
  const source = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
  for (const path of ['/favorites', '/shared', '/trash', '/projects/:id', '/projects/:projectId/scenes/:sceneId']) {
    const start = source.indexOf("path: '" + path + "'")
    const end = source.indexOf('\n  },', start)
    assert.ok(start >= 0, 'missing route: ' + path)
    assert.doesNotMatch(source.slice(start, end), /requiresAuth: true/)
  }
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: node --test tests/public-demo-routes.test.mjs

Expected: FAIL because the routes still require authentication.

- [ ] **Step 3: Delegate demo branches to the repository**

In project.ts, branch all page-facing project actions when isPublicDemo is true: active list, detail, create, favorite toggle, trash list, restore, and permanent delete. Return seeded members or an empty array without an API request.

In scene.ts, branch list/create/update/delete/reorder actions to the repository when isPublicDemo is true and keep existing API paths unchanged otherwise. Update completion counts after each local scene mutation.

Keep requiresAuth on /favorites, /shared, /trash, /projects/:id, and /projects/:projectId/scenes/:sceneId. Update the navigation guard so only isPublicDemo bypasses those route protections; profile and auth routes remain protected.

- [ ] **Step 4: Run tests and type check**

Run: node --test tests/public-demo-routes.test.mjs

Expected: PASS.

Run: npx vue-tsc -b

Expected: PASS with no type errors.

- [ ] **Step 5: Commit**

~~~bash
git add itda-frontend/src/router/index.ts itda-frontend/src/stores/project.ts itda-frontend/src/stores/scene.ts itda-frontend/tests/public-demo-routes.test.mjs
git commit -m "feat: 공개 데모 페이지 데이터 연결"
~~~

### Task 3: Existing modal and editor presentation flow

**Files:**
- Modify: itda-frontend/src/pages/DashboardPage.vue
- Modify: itda-frontend/src/components/project/NewProjectModal.vue
- Modify: itda-frontend/src/pages/project/composables/useProjectDetail.ts
- Modify: itda-frontend/src/pages/ProjectDetailPage.vue
- Modify: itda-frontend/src/pages/SceneEditPage.vue
- Create: itda-frontend/tests/public-demo-flow.test.mjs

**Interfaces:**
- Dashboard calls uiStore.openModal('new-project').
- The existing NewProjectModal creates a project through the Task 2 store and then navigates to detail.
- Scene editor uses existing NodePanelContainer and nodeStore.loadMockSceneNodes in demo mode.

- [ ] **Step 1: Write the failing UI-flow test**

~~~js
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('dashboard opens the existing creation modal', async () => {
  const source = await readFile(new URL('../src/pages/DashboardPage.vue', import.meta.url), 'utf8')
  assert.match(source, /openModal\('new-project'\)/)
})

test('demo editor keeps the existing right-side node panel', async () => {
  const source = await readFile(new URL('../src/pages/SceneEditPage.vue', import.meta.url), 'utf8')
  assert.match(source, /isPublicDemo/)
  assert.match(source, /nodeStore\.loadMockSceneNodes/)
  assert.match(source, /<NodePanelContainer/)
})
~~~

- [ ] **Step 2: Run test to verify it fails**

Run: node --test tests/public-demo-flow.test.mjs

Expected: FAIL because the dashboard creates a blank project immediately and the editor does not use isPublicDemo.

- [ ] **Step 3: Implement visual flow without backend calls**

In DashboardPage.vue, replace direct blank-project creation triggers with a single handler that opens the existing new-project modal.

In NewProjectModal.vue, make the existing AI generation control complete locally in demo mode. On project submit, use the store creation action and show the message that three example scenarios were created. Do not add a new modal or a new form.

In useProjectDetail.ts and ProjectDetailPage.vue, skip collaboration, object, media, and timeline requests in demo mode while loading the project and its scenes. Set isScenesTabHidden to false in demo mode so existing scene cards provide the selection screen.

In SceneEditPage.vue, load selected project and scenes through local stores, use existing mock nodes, and skip collaboration, timeline, and API calls in demo mode. Do not change NodePanelContainer; clicking a seeded node must continue to open it at the right edge.

- [ ] **Step 4: Run tests, build, and local smoke test**

Run: node --test tests/public-demo-flow.test.mjs tests/public-demo-routes.test.mjs

Expected: PASS.

Run: npm run build

Expected: PASS.

Run: npm run dev -- --host 127.0.0.1

Verify:
1. Dashboard 새 프로젝트 opens 새 프로젝트 만들기.
2. Open AI 시나리오 생성, press generation, submit a title, and see three scene cards.
3. Select a scene, click a generated node, and see the existing right-side properties panel.
4. Visit favorites, shared, and trash through navigation; refresh one nested scene URL.

- [ ] **Step 5: Commit**

~~~bash
git add itda-frontend/src/pages/DashboardPage.vue itda-frontend/src/components/project/NewProjectModal.vue itda-frontend/src/pages/project/composables/useProjectDetail.ts itda-frontend/src/pages/ProjectDetailPage.vue itda-frontend/src/pages/SceneEditPage.vue itda-frontend/tests/public-demo-flow.test.mjs
git commit -m "feat: 공개 데모 프로젝트 생성 흐름 추가"
~~~

### Task 4: Preview deployment and verification

**Files:**
- Modify: no files unless a minimal build fix is required
- Test: itda-frontend/tests/public-demo-repository.test.ts
- Test: itda-frontend/tests/public-demo-routes.test.mjs
- Test: itda-frontend/tests/public-demo-flow.test.mjs

**Interfaces:**
- Consumes all previous tasks.
- Produces a Vercel preview built with VITE_PUBLIC_DEMO=true for public-dashboard-preview.

- [ ] **Step 1: Run complete checks**

Run: node --experimental-strip-types --test tests/public-demo-repository.test.ts && node --test tests/public-dashboard-route.test.mjs tests/public-demo-routes.test.mjs tests/public-demo-flow.test.mjs

Expected: PASS.

Run: npm run build

Expected: PASS.

- [ ] **Step 2: Deploy preview**

Set the Vercel Preview variable VITE_PUBLIC_DEMO to true for branch public-dashboard-preview, then push completed commits.

- [ ] **Step 3: Verify deployed preview**

Open the preview and repeat the four local smoke checks. Confirm direct refresh succeeds at a nested scene URL.

- [ ] **Step 4: Report**

Report the preview URL, Git commit, verified flow, and intentional limitation that all data is browser-local.

## Plan Self-Review

- **Spec coverage:** Tasks 1–2 provide local data, persistence, public routes, favorites, shared projects, and trash. Task 3 provides the existing creation modal, three scenarios, cards for selection, and the existing right-side editor panel. Task 4 covers preview configuration and verification.
- **Placeholder scan:** No deferred placeholders or unspecified error handling remain. Malformed storage reseeds; demo-mode pages skip backend-only calls.
- **Type consistency:** Repository methods return existing Project, ProjectDetail, and Scene types. Existing store method names and page contracts remain stable.
