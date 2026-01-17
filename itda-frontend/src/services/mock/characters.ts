import type { ObjectSheet, CreateObjectRequest } from '../../types'

// Mock characters (ObjectSheet with type = 'CHARACTER') per project
const mockCharactersData: Record<number, ObjectSheet[]> = {
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

// ID counter for new characters
let nextCharacterId = 100

// Simulated API delay
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

// Art style options
export const artStyles = ['실사', '만화', '애니메이션', '사이버펑크', '판타지', '미니멀']

// Mock API functions
export async function fetchCharactersByProjectId(projectId: number): Promise<ObjectSheet[]> {
  await delay(300)
  return [...(mockCharactersData[projectId] || [])]
}

export async function createCharacter(
  projectId: number,
  data: CreateObjectRequest
): Promise<ObjectSheet> {
  await delay(500)

  const newCharacter: ObjectSheet = {
    objectId: nextCharacterId++,
    name: data.name,
    type: 'CHARACTER',
    description: data.description,
    style: data.style,
    sheetImageUrl: `https://i.pravatar.cc/150?u=${data.name.toLowerCase().replace(/\s/g, '')}`,
  }

  if (!mockCharactersData[projectId]) {
    mockCharactersData[projectId] = []
  }
  mockCharactersData[projectId].push(newCharacter)

  return newCharacter
}

export async function updateCharacter(
  projectId: number,
  characterId: number,
  data: Partial<ObjectSheet>
): Promise<ObjectSheet | null> {
  await delay(300)
  const characters = mockCharactersData[projectId]
  if (!characters) return null

  const index = characters.findIndex((c) => c.objectId === characterId)
  if (index === -1) return null

  const existingCharacter = characters[index]
  if (!existingCharacter) return null

  const updatedCharacter: ObjectSheet = { ...existingCharacter, ...data }
  characters[index] = updatedCharacter
  return updatedCharacter
}

export async function deleteCharacter(projectId: number, characterId: number): Promise<boolean> {
  await delay(300)
  const characters = mockCharactersData[projectId]
  if (!characters) return false

  const index = characters.findIndex((c) => c.objectId === characterId)
  if (index === -1) return false

  characters.splice(index, 1)
  return true
}

// Generate character with AI (mock)
export interface GenerateCharacterRequest {
  name: string
  description: string
  style: string
}

export async function generateCharacterWithAI(
  projectId: number,
  request: GenerateCharacterRequest
): Promise<ObjectSheet> {
  // Simulate AI image generation time
  await delay(2000)

  const newCharacter: ObjectSheet = {
    objectId: nextCharacterId++,
    name: request.name,
    type: 'CHARACTER',
    description: request.description,
    style: request.style,
    sheetImageUrl: `https://i.pravatar.cc/150?u=${request.name.toLowerCase().replace(/\s/g, '')}_${Date.now()}`,
  }

  if (!mockCharactersData[projectId]) {
    mockCharactersData[projectId] = []
  }
  mockCharactersData[projectId].push(newCharacter)

  return newCharacter
}
