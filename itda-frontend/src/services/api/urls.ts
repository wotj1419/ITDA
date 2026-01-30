import { API_BASE_URL } from './constants';

const apiUrl = new URL(API_BASE_URL);
export const API_ORIGIN = apiUrl.origin;
const API_PROTOCOL = apiUrl.protocol;

export function resolveApiUrl(url?: string | null): string | null {
  if (!url) return null;
  if (url.startsWith('data:')) return url;
  if (url.startsWith('blob:')) return url;
  if (/^https?:\/\//i.test(url)) return url;
  if (url.startsWith('//')) return `${API_PROTOCOL}${url}`;
  if (url.startsWith('/')) return `${API_ORIGIN}${url}`;
  return `${API_ORIGIN}/${url}`;
}

export function isApiResourceUrl(url?: string | null): boolean {
  const resolved = resolveApiUrl(url);
  if (!resolved) return false;
  try {
    const parsed = new URL(resolved);
    return parsed.origin === API_ORIGIN && parsed.pathname.startsWith('/api/');
  } catch {
    return false;
  }
}
