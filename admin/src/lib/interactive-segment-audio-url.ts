/**
 * Validates segment audioUrl values in interactive graph JSON.
 * Production expects HTTPS + .mp3; local dev often uses http:// with LAN or localhost (matches backend AUDIO_PUBLIC_BASE_URL).
 */

function isDevelopmentHttpAudioUrl(raw: string): boolean {
  const t = raw.trim();
  if (!/^http:\/\//i.test(t)) return false;
  try {
    const url = new URL(t);
    const h = url.hostname.toLowerCase();
    if (h === "localhost" || h === "127.0.0.1" || h === "0.0.0.0") return true;
    if (h === "10.0.2.2") return true; // Android emulator → host
    if (/^192\.168\.\d{1,3}\.\d{1,3}$/.test(h)) return true;
    if (/^10\.\d{1,3}\.\d{1,3}\.\d{1,3}$/.test(h)) return true;
    if (/^172\.(1[6-9]|2\d|3[01])\.\d{1,3}\.\d{1,3}$/.test(h)) return true;
    return false;
  } catch {
    return false;
  }
}

export function validateInteractiveSegmentUrls(raw: string): string[] {
  const errors: string[] = [];
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return errors;
    const segments = (parsed as Record<string, unknown>).segments;
    if (!segments || typeof segments !== "object" || Array.isArray(segments)) return errors;
    for (const [sid, seg] of Object.entries(segments as Record<string, unknown>)) {
      if (!seg || typeof seg !== "object" || Array.isArray(seg)) continue;
      const audioUrl = (seg as Record<string, unknown>).audioUrl;
      if (typeof audioUrl !== "string" || !audioUrl.trim()) continue;
      const u = audioUrl.trim();
      const httpsOk = /^https:\/\/.+/i.test(u);
      const devHttpOk = isDevelopmentHttpAudioUrl(u);
      if (!httpsOk && !devHttpOk) {
        errors.push(
          `Segment "${sid}": audioUrl must be HTTPS (http is allowed only on localhost or private LAN for dev).`,
        );
        continue;
      }
      if (!/\.mp3(\?|#|$)/i.test(u)) {
        errors.push(`Segment "${sid}": audioUrl should point to an .mp3 file.`);
      }
    }
  } catch {
    return errors;
  }
  return errors;
}
