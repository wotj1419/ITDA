import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('dashboard route is available without authentication', async () => {
  const routerSource = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
  const dashboardRoute = routerSource.match(/path: '\/dashboard',[\s\S]*?\n  },/)

  assert.ok(dashboardRoute, 'dashboard route should exist')
  assert.doesNotMatch(dashboardRoute[0], /requiresAuth: true/)
})

test('Vercel serves the single-page app for dashboard refreshes', async () => {
  let config

  try {
    config = JSON.parse(await readFile(new URL('../vercel.json', import.meta.url), 'utf8'))
  } catch {
    assert.fail('vercel.json should define a single-page app fallback')
  }

  assert.deepEqual(config.rewrites, [{ source: '/(.*)', destination: '/index.html' }])
})
