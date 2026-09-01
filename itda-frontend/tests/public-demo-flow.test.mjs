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
