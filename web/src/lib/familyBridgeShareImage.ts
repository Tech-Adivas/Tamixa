/**
 * Canvas-based share cards for family WhatsApp groups (Brag & Protect).
 * No extra npm deps; React Native can mirror with react-native-view-shot or similar.
 */

function setupHiDpiCanvas(cssW: number, cssH: number): { canvas: HTMLCanvasElement; ctx: CanvasRenderingContext2D } {
  const canvas = document.createElement("canvas");
  const dpr = typeof window !== "undefined" ? Math.min(2, window.devicePixelRatio || 1) : 1;
  canvas.width = Math.round(cssW * dpr);
  canvas.height = Math.round(cssH * dpr);
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Canvas 2D unsupported");
  ctx.scale(dpr, dpr);
  return { canvas, ctx };
}

function wrapText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, maxW: number, lineHeight: number): number {
  const words = text.split(/\s+/);
  let line = "";
  let yy = y;
  for (let n = 0; n < words.length; n++) {
    const test = line ? `${line} ${words[n]}` : words[n];
    if (ctx.measureText(test).width > maxW && line) {
      ctx.fillText(line, x, yy);
      line = words[n];
      yy += lineHeight;
    } else {
      line = test;
    }
  }
  if (line) {
    ctx.fillText(line, x, yy);
    yy += lineHeight;
  }
  return yy;
}

export async function renderFamilyMissionCardPng(params: {
  missionLine: string;
  storyTitle?: string | null;
}): Promise<Blob | null> {
  if (typeof document === "undefined") return null;
  const W = 900;
  const H = 520;
  const { canvas, ctx } = setupHiDpiCanvas(W, H);
  const grd = ctx.createLinearGradient(0, 0, W, H);
  grd.addColorStop(0, "#075e54");
  grd.addColorStop(1, "#128c7e");
  ctx.fillStyle = grd;
  ctx.beginPath();
  const r = 28;
  ctx.moveTo(r, 0);
  ctx.lineTo(W - r, 0);
  ctx.quadraticCurveTo(W, 0, W, r);
  ctx.lineTo(W, H - r);
  ctx.quadraticCurveTo(W, H, W - r, H);
  ctx.lineTo(r, H);
  ctx.quadraticCurveTo(0, H, 0, H - r);
  ctx.lineTo(0, r);
  ctx.quadraticCurveTo(0, 0, r, 0);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "rgba(255,255,255,0.95)";
  ctx.font = "600 28px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillText("Tamixa · Family Mission Card", 48, 72);

  ctx.font = "400 18px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillStyle = "rgba(255,255,255,0.88)";
  const sub = params.storyTitle?.trim()
    ? `From: ${params.storyTitle.trim()}`
    : "Brag & Protect — share with family on WhatsApp";
  ctx.fillText(sub, 48, 108);

  ctx.fillStyle = "#ffffff";
  ctx.fillRect(40, 140, W - 80, H - 200);
  ctx.fillStyle = "#1f2c34";
  ctx.font = "500 26px system-ui, -apple-system, Segoe UI, sans-serif";
  const bodyY = wrapText(ctx, params.missionLine.trim(), 64, 180, W - 128, 34);

  ctx.fillStyle = "#667781";
  ctx.font = "400 16px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillText("Real-world challenge — try together tonight.", 64, Math.max(bodyY + 24, H - 120));

  return new Promise((resolve) => {
    canvas.toBlob((b) => resolve(b), "image/png", 0.92);
  });
}

export async function renderScamProofBadgePng(params: {
  badgeTitle: string;
  badgeSubtitle: string;
}): Promise<Blob | null> {
  if (typeof document === "undefined") return null;
  const W = 900;
  const H = 520;
  const { canvas, ctx } = setupHiDpiCanvas(W, H);
  const grd = ctx.createLinearGradient(0, 0, W, H);
  grd.addColorStop(0, "#1a237e");
  grd.addColorStop(0.5, "#3949ab");
  grd.addColorStop(1, "#00897b");
  ctx.fillStyle = grd;
  ctx.beginPath();
  const r = 28;
  ctx.moveTo(r, 0);
  ctx.lineTo(W - r, 0);
  ctx.quadraticCurveTo(W, 0, W, r);
  ctx.lineTo(W, H - r);
  ctx.quadraticCurveTo(W, H, W - r, H);
  ctx.lineTo(r, H);
  ctx.quadraticCurveTo(0, H, 0, H - r);
  ctx.lineTo(0, r);
  ctx.quadraticCurveTo(0, 0, r, 0);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#fff";
  ctx.font = "600 22px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillText("Tamixa · Brag & Protect", 48, 64);

  ctx.font = "700 40px system-ui, -apple-system, Segoe UI, sans-serif";
  const y = wrapText(ctx, params.badgeTitle, 48, 150, W - 96, 48);

  ctx.font = "400 22px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillStyle = "rgba(255,255,255,0.92)";
  wrapText(ctx, params.badgeSubtitle, 48, y + 28, W - 96, 32);

  ctx.font = "400 16px system-ui, -apple-system, Segoe UI, sans-serif";
  ctx.fillStyle = "rgba(255,255,255,0.75)";
  ctx.fillText("Share in your family WhatsApp — celebrate safe choices.", 48, H - 72);

  return new Promise((resolve) => {
    canvas.toBlob((b) => resolve(b), "image/png", 0.92);
  });
}

/**
 * Prefer Web Share API with image; otherwise download PNG and open WhatsApp with a short caption.
 */
export async function shareFamilyBridgeImage(blob: Blob, fallbackCaption: string): Promise<void> {
  const file = new File([blob], "tamixa-family-bridge.png", { type: "image/png" });
  const shareData: ShareData = {
    text: fallbackCaption,
    files: [file],
  };
  try {
    if (typeof navigator !== "undefined" && navigator.share && navigator.canShare?.(shareData)) {
      await navigator.share(shareData);
      return;
    }
  } catch {
    /* fall through */
  }
  try {
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "tamixa-family-bridge.png";
    a.rel = "noopener";
    a.click();
    URL.revokeObjectURL(url);
  } catch {
    /* ignore */
  }
  const enc = encodeURIComponent(`${fallbackCaption}\n(Image saved — attach it in WhatsApp.)`);
  if (typeof window !== "undefined") {
    window.open(`https://wa.me/?text=${enc}`, "_blank", "noopener,noreferrer");
  }
}
