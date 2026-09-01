import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

test('portfolio-demo routes retain authentication outside demo mode', async () => {
  const source = await readFile(new URL('../src/router/index.ts', import.meta.url), 'utf8')
  const paths = [
    '/favorites',
    '/shared',
    '/trash',
    '/projects/:id',
    '/projects/:projectId/scenes/:sceneId',
  ]

  for (const path of paths) {
    const start = source.indexOf("path: '" + path + "'")
    const end = source.indexOf('\n  },', start)
    assert.ok(start >= 0, 'missing route: ' + path)
    assert.match(source.slice(start, end), /requiresAuth: true/)
  }

  assert.match(source, /to\.meta\.requiresAuth && !isAuthenticated && !isPublicDemo/)
})
