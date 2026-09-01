import assert from 'node:assert/strict'
import test from 'node:test'
import { createPublicDemoRepository } from '../src/services/demo/publicDemoRepository.ts'

type MemoryStorage = {
  getItem: (key: string) => string | null
  setItem: (key: string, value: string) => void
  removeItem: (key: string) => void
}

const createMemoryStorage = (): MemoryStorage => {
  const values = new Map<string, string>()

  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  }
}

test('creates three draft scenarios for a new demo project', () => {
  const repository = createPublicDemoRepository(createMemoryStorage())
  const project = repository.createProject({
    title: '포트폴리오 영상',
    description: '',
    genre: 'sf',
  })
  const scenes = repository.listScenes(project.projectId)

  assert.equal(scenes.length, 3)
  assert.deepEqual(scenes.map((scene) => scene.status), ['DRAFT', 'DRAFT', 'DRAFT'])
})

test('persists favorite and trash changes in supplied storage', () => {
  const storage = createMemoryStorage()
  const first = createPublicDemoRepository(storage)
  const project = first.listProjects().find((item) => !first.isFavorite(item.projectId))

  assert.ok(project)
  first.toggleFavorite(project.projectId)
  first.moveToTrash(project.projectId)

  const second = createPublicDemoRepository(storage)
  assert.equal(second.listProjects().some((item) => item.projectId === project.projectId), false)
  assert.equal(second.getDeletedProjects().some((item) => item.projectId === project.projectId), true)
  assert.equal(second.isFavorite(project.projectId), true)
})
