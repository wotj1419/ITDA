import apiClient from '../api/client'
import { isApiResourceUrl, resolveApiUrl } from '../api/urls'

type CacheEntry = {
  blobUrl: string
  refCount: number
}

export type MediaUrlLease = {
  key: string
  url: string
  releasable: boolean
}

const blobCache = new Map<string, CacheEntry>()
const inflightRequests = new Map<string, Promise<string>>()

function createStaticLease(resolvedUrl: string): MediaUrlLease {
  return {
    key: resolvedUrl,
    url: resolvedUrl,
    releasable: false,
  }
}

function createBlobLease(key: string, blobUrl: string): MediaUrlLease {
  return {
    key,
    url: blobUrl,
    releasable: true,
  }
}

async function fetchBlobUrl(cacheKey: string): Promise<string> {
  const inflight = inflightRequests.get(cacheKey)
  if (inflight) {
    return inflight
  }

  const request = apiClient
    .get(cacheKey, { responseType: 'blob' })
    .then((response) => URL.createObjectURL(response.data))
    .finally(() => {
      inflightRequests.delete(cacheKey)
    })

  inflightRequests.set(cacheKey, request)
  return request
}

export async function acquireMediaUrlLease(url?: string | null): Promise<MediaUrlLease | null> {
  const resolvedUrl = resolveApiUrl(url)
  if (!resolvedUrl) return null

  if (resolvedUrl.startsWith('blob:') || resolvedUrl.startsWith('data:')) {
    return createStaticLease(resolvedUrl)
  }

  if (!isApiResourceUrl(resolvedUrl)) {
    return createStaticLease(resolvedUrl)
  }

  const cacheKey = resolvedUrl
  const cached = blobCache.get(cacheKey)
  if (cached) {
    cached.refCount += 1
    return createBlobLease(cacheKey, cached.blobUrl)
  }

  const blobUrl = await fetchBlobUrl(cacheKey)
  const existing = blobCache.get(cacheKey)
  if (existing) {
    existing.refCount += 1
    if (existing.blobUrl !== blobUrl && blobUrl.startsWith('blob:')) {
      URL.revokeObjectURL(blobUrl)
    }
    return createBlobLease(cacheKey, existing.blobUrl)
  }

  blobCache.set(cacheKey, {
    blobUrl,
    refCount: 1,
  })
  return createBlobLease(cacheKey, blobUrl)
}

export function releaseMediaUrlLease(lease?: MediaUrlLease | string | null): void {
  if (!lease) return
  const key = typeof lease === 'string' ? lease : lease.key
  const releasable = typeof lease === 'string' ? true : lease.releasable
  if (!releasable) return

  const cached = blobCache.get(key)
  if (!cached) return

  cached.refCount -= 1
  if (cached.refCount > 0) return

  blobCache.delete(key)
  if (cached.blobUrl.startsWith('blob:')) {
    URL.revokeObjectURL(cached.blobUrl)
  }
}

export function clearMediaUrlLeaseCache(): void {
  blobCache.forEach((entry) => {
    if (entry.blobUrl.startsWith('blob:')) {
      URL.revokeObjectURL(entry.blobUrl)
    }
  })
  blobCache.clear()
  inflightRequests.clear()
}

