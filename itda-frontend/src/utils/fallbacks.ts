const shotSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="225" viewBox="0 0 400 225">
  <defs>
    <linearGradient id="shot-bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0%" stop-color="#0F172A"/>
      <stop offset="100%" stop-color="#334155"/>
    </linearGradient>
  </defs>
  <rect width="400" height="225" fill="url(#shot-bg)"/>
  <rect x="16" y="16" width="368" height="193" rx="14" fill="rgba(255,255,255,0.08)" stroke="rgba(255,255,255,0.18)"/>
  <circle cx="90" cy="112.5" r="32" fill="rgba(255,255,255,0.2)"/>
  <rect x="140" y="96" width="170" height="12" rx="6" fill="rgba(255,255,255,0.35)"/>
  <rect x="140" y="118" width="120" height="10" rx="5" fill="rgba(255,255,255,0.25)"/>
  <text x="330" y="190" font-family="Arial, sans-serif" font-size="12" fill="rgba(255,255,255,0.55)">SHOT</text>
</svg>`;

export const SHOT_FALLBACK_THUMBNAIL = `data:image/svg+xml;utf8,${encodeURIComponent(shotSvg)}`;

const FALLBACK_SVG_SIGNATURES = ['shot-bg', '>SHOT<'];

function safeDecodeURIComponent(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}

function decodeSvgDataUrl(url: string): string | null {
  if (!url.startsWith('data:image/svg+xml')) return null;
  const commaIndex = url.indexOf(',');
  if (commaIndex === -1) return null;
  const meta = url.slice(0, commaIndex);
  const payload = url.slice(commaIndex + 1);
  if (/;base64/i.test(meta)) {
    try {
      return atob(payload);
    } catch {
      return null;
    }
  }
  return safeDecodeURIComponent(payload);
}

export function isFallbackThumbnail(url?: string | null): boolean {
  if (!url) return false;
  if (url === SHOT_FALLBACK_THUMBNAIL) return true;
  const decoded = decodeSvgDataUrl(url);
  if (!decoded) return false;
  return FALLBACK_SVG_SIGNATURES.some((signature) => decoded.includes(signature));
}
