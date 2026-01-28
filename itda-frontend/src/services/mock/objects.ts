import type { ObjectSheet, CreateObjectRequest, UpdateObjectRequest } from '../../types/api/objects'

const mockObjectsData: Record<number, ObjectSheet[]> = {
  1: [
    {
      objectId: 1,
      name: 'Mark Watney',
      type: 'CHARACTER',
      description: 'Astronaut, 30s. A resilient and resourceful scientist stranded on Mars.',
      style: '실사',
      sheetImageUrl: 'https://i.pravatar.cc/150?u=mark',
    },
    {
      objectId: 2,
      name: 'ARES Commander',
      type: 'CHARACTER',
      description: 'Mission commander. Calm and decisive under pressure.',
      style: '실사',
      sheetImageUrl: 'https://i.pravatar.cc/150?u=commander',
    },
  ],
  2: [
    {
      objectId: 3,
      name: 'Kira Chen',
      type: 'CHARACTER',
      description: 'A skilled hacker with cybernetic implants. She navigates the digital underworld of neo-Tokyo.',
      style: '사이버펑크',
      sheetImageUrl: 'https://i.pravatar.cc/150?u=kira',
    },
  ],
  3: [
    {
      objectId: 4,
      name: 'Dr. Marina Silva',
      type: 'CHARACTER',
      description: 'Marine biologist leading the deep sea expedition. Passionate about ocean conservation.',
      style: '실사',
      sheetImageUrl: 'https://i.pravatar.cc/150?u=marina',
    },
  ],
}

let nextObjectId = 100

// Simulated API delay
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Art style options
export const artStyles = ['실사', '만화', '애니메이션', '사이버펑크', '판타지', '미니멀']

// Mock API functions
export async function fetchObjectsByProjectId(projectId: number): Promise<ObjectSheet[]> {
  await delay(300)
  return [...(mockObjectsData[projectId] || [])]
}

export async function fetchObjectById(objectId: number): Promise<ObjectSheet | null> {
  await delay(200)
  const allObjects = Object.values(mockObjectsData).flat()
  return allObjects.find((item) => item.objectId === objectId) || null
}

async function readFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(new Error('Failed to read file'))
    reader.readAsDataURL(file)
  })
}

export async function createObject(
  projectId: number,
  data: CreateObjectRequest,
  file: File
): Promise<ObjectSheet> {
  await delay(500)

  const sheetImageUrl = file ? await readFileAsDataUrl(file) : ''
  const newObject: ObjectSheet = {
    objectId: nextObjectId++,
    name: data.name,
    type: data.type,
    description: data.description,
    style: data.style,
    sheetImageUrl,
  }

  if (!mockObjectsData[projectId]) {
    mockObjectsData[projectId] = []
  }
  mockObjectsData[projectId].push(newObject)

  return newObject
}

export async function updateObject(
  objectId: number,
  data: UpdateObjectRequest
): Promise<ObjectSheet | null> {
  await delay(300)
  const entries = Object.entries(mockObjectsData)
  for (const [, objects] of entries) {
    const index = objects.findIndex((item) => item.objectId === objectId)
    if (index >= 0) {
      const existingObject = objects[index]
      const updatedObject: ObjectSheet = { ...existingObject, ...data }
      objects[index] = updatedObject
      return updatedObject
    }
  }
  return null
}

export async function replaceObjectImage(
  objectId: number,
  file: File
): Promise<ObjectSheet> {
  await delay(300)
  const entries = Object.entries(mockObjectsData)
  for (const [, objects] of entries) {
    const index = objects.findIndex((item) => item.objectId === objectId)
    if (index >= 0) {
      const existingObject = objects[index]
      const sheetImageUrl = await readFileAsDataUrl(file)
      const updatedObject: ObjectSheet = { ...existingObject, sheetImageUrl }
      objects[index] = updatedObject
      return updatedObject
    }
  }
  throw new Error('Object not found')
}

export async function deleteObject(objectId: number): Promise<boolean> {
  await delay(300)
  const entries = Object.entries(mockObjectsData)
  for (const [, objects] of entries) {
    const index = objects.findIndex((item) => item.objectId === objectId)
    if (index >= 0) {
      objects.splice(index, 1)
      return true
    }
  }
  return false
}

export async function downloadObjectImage(
  objectId: number
): Promise<Blob> {
  await delay(200)
  const allObjects = Object.values(mockObjectsData).flat()
  const item = allObjects.find((o) => o.objectId === objectId)
  if (!item?.sheetImageUrl) {
    return new Blob()
  }
  const response = await fetch(item.sheetImageUrl)
  return response.blob()
}
