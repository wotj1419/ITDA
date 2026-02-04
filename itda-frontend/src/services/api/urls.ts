import { API_BASE_URL } from './constants';

const apiUrl = new URL(API_BASE_URL);
export const API_ORIGIN = apiUrl.origin;
const API_PROTOCOL = apiUrl.protocol;

export function isBrowserNativeUrl(url?: string | null): boolean {
  if (!url) return false;
  return url.startsWith('data:') || url.startsWith('blob:');
}

export function resolveApiUrl(url?: string | null): string | null {
  if (!url) return null;
  const normalized = url.trim();
  if (!normalized) return null;
  if (isBrowserNativeUrl(normalized)) return normalized;
  if (/^https?:\/\//i.test(normalized)) return normalized;
  if (normalized.startsWith('//')) return `${API_PROTOCOL}${normalized}`;
  if (normalized.startsWith('/')) return `${API_ORIGIN}${normalized}`;
  return `${API_ORIGIN}/${normalized}`;
}

export function isApiResourceUrl(url?: string | null): boolean {
  const resolved = resolveApiUrl(url);
  if (!resolved || isBrowserNativeUrl(resolved)) return false;
  try {
    const parsed = new URL(resolved);
    return parsed.origin === API_ORIGIN && parsed.pathname.startsWith('/api/');
  } catch {
    return false;
  }
}

export function shouldUseApiClientForMedia(url?: string | null): boolean {
  return isApiResourceUrl(url);
}
