import apiClient from './client';
import { resolveApiUrl } from './urls';
import {
  acquireMediaUrlLease,
  releaseMediaUrlLease,
  type MediaUrlLease,
} from '../media/mediaCache';
export type { MediaUrlLease } from '../media/mediaCache';

export async function fetchProtectedBlobUrl(url?: string | null): Promise<string | null> {
  const resolved = resolveApiUrl(url);
  if (!resolved) return null;
  if (resolved.startsWith('blob:') || resolved.startsWith('data:')) {
    return resolved;
  }
  const response = await apiClient.get(resolved, { responseType: 'blob' });
  return URL.createObjectURL(response.data);
}

export async function acquireMediaLease(url?: string | null): Promise<MediaUrlLease | null> {
  return acquireMediaUrlLease(url);
}

export function releaseMediaLease(lease?: MediaUrlLease | string | null): void {
  releaseMediaUrlLease(lease);
}
