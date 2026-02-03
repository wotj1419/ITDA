import apiClient from './client';
import { resolveApiUrl } from './urls';

export async function fetchProtectedBlobUrl(url?: string | null): Promise<string | null> {
  const resolved = resolveApiUrl(url);
  if (!resolved) return null;
  if (resolved.startsWith('blob:') || resolved.startsWith('data:')) {
    return resolved;
  }
  const response = await apiClient.get(resolved, { responseType: 'blob' });
  return URL.createObjectURL(response.data);
}
