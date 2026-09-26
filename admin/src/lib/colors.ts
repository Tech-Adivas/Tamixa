/**
 * Tamixa Color System - Storybook Dusk Theme
 * Aligned with mobile TamixaColors
 * 
 * Warm, inviting palette suitable for bedtime storytelling and learning.
 * Differentiates from competitors (Duolingo's bright green, Spotify's blue).
 */

// ============================================================================
// PRIMARY COLORS (Storybook Dusk Palette)
// ============================================================================

export const colors = {
  // Primary palette
  terracotta: 'hsl(6 52% 57%)',        // #C4625A - Warm accent
  deepTeal: 'hsl(180 33% 27%)',        // #2D5A5A - Primary
  eduStoryMint: 'hsl(168 44% 55%)',    // #5CBCA8 - Learn accent
  warmSand: 'hsl(40 42% 85%)',         // #E8DCC8 - Cream
  warmCharcoal: 'hsl(30 14% 8%)',      // #141210 - Dark base
  warmSurface: 'hsl(30 11% 16%)',      // #2C2822 - Card surface
  
  // Extended palette
  coral: 'hsl(12 71% 66%)',            // Gradient complement to terracotta
  tealLight: 'hsl(180 33% 40%)',       // Lighter teal for hover states
  mintLight: 'hsl(168 44% 70%)',       // Lighter mint for backgrounds
  sandDark: 'hsl(40 42% 70%)',         // Darker sand for borders
  
  // Gradients (CSS gradient strings)
  gradientPrimary: 'linear-gradient(135deg, hsl(6 52% 57%) 0%, hsl(12 71% 66%) 100%)',
  gradientTeal: 'linear-gradient(135deg, hsl(180 33% 27%) 0%, hsl(180 33% 40%) 100%)',
  gradientMint: 'linear-gradient(135deg, hsl(168 44% 55%) 0%, hsl(168 44% 70%) 100%)',
  
  // Semantic colors
  primary: 'hsl(180 33% 27%)',         // deepTeal
  primaryHover: 'hsl(180 33% 40%)',    // tealLight
  accent: 'hsl(6 52% 57%)',            // terracotta
  accentHover: 'hsl(12 71% 66%)',      // coral
  success: 'hsl(168 44% 55%)',         // eduStoryMint
  warning: 'hsl(40 96% 56%)',          // Amber
  error: 'hsl(0 84% 60%)',             // Red
  info: 'hsl(217 91% 60%)',            // Blue
  
  // Neutral colors (for text, borders, backgrounds)
  neutral: {
    50: 'hsl(40 42% 98%)',
    100: 'hsl(40 42% 95%)',
    200: 'hsl(40 42% 90%)',
    300: 'hsl(40 42% 85%)',  // warmSand
    400: 'hsl(40 42% 70%)',
    500: 'hsl(40 42% 50%)',
    600: 'hsl(40 42% 40%)',
    700: 'hsl(30 14% 20%)',
    800: 'hsl(30 11% 16%)',  // warmSurface
    900: 'hsl(30 14% 8%)',   // warmCharcoal
    950: 'hsl(30 14% 4%)',
  },
} as const;

// ============================================================================
// THEME VARIANTS
// ============================================================================

export const lightTheme = {
  background: colors.neutral[50],
  foreground: colors.warmCharcoal,
  
  card: {
    background: 'hsl(0 0% 100%)',
    foreground: colors.warmCharcoal,
    border: colors.neutral[200],
  },
  
  primary: {
    background: colors.deepTeal,
    foreground: 'hsl(0 0% 100%)',
    hover: colors.tealLight,
  },
  
  accent: {
    background: colors.terracotta,
    foreground: 'hsl(0 0% 100%)',
    hover: colors.coral,
  },
  
  muted: {
    background: colors.neutral[100],
    foreground: colors.neutral[700],
  },
  
  border: colors.neutral[200],
  input: colors.neutral[200],
  ring: colors.deepTeal,
} as const;

export const darkTheme = {
  background: colors.warmCharcoal,
  foreground: colors.warmSand,
  
  card: {
    background: colors.warmSurface,
    foreground: colors.warmSand,
    border: colors.neutral[800],
  },
  
  primary: {
    background: colors.deepTeal,
    foreground: 'hsl(0 0% 100%)',
    hover: colors.tealLight,
  },
  
  accent: {
    background: colors.terracotta,
    foreground: 'hsl(0 0% 100%)',
    hover: colors.coral,
  },
  
  muted: {
    background: colors.neutral[800],
    foreground: colors.neutral[400],
  },
  
  border: colors.neutral[800],
  input: colors.neutral[800],
  ring: colors.eduStoryMint,
} as const;

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Convert HSL color to RGB for opacity manipulation
 */
export const hslToRgb = (hsl: string): { r: number; g: number; b: number } => {
  const match = hsl.match(/hsl\((\d+)\s+(\d+)%\s+(\d+)%\)/);
  if (!match) return { r: 0, g: 0, b: 0 };
  
  const h = parseInt(match[1]) / 360;
  const s = parseInt(match[2]) / 100;
  const l = parseInt(match[3]) / 100;
  
  let r, g, b;
  
  if (s === 0) {
    r = g = b = l;
  } else {
    const hue2rgb = (p: number, q: number, t: number) => {
      if (t < 0) t += 1;
      if (t > 1) t -= 1;
      if (t < 1/6) return p + (q - p) * 6 * t;
      if (t < 1/2) return q;
      if (t < 2/3) return p + (q - p) * (2/3 - t) * 6;
      return p;
    };
    
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    r = hue2rgb(p, q, h + 1/3);
    g = hue2rgb(p, q, h);
    b = hue2rgb(p, q, h - 1/3);
  }
  
  return {
    r: Math.round(r * 255),
    g: Math.round(g * 255),
    b: Math.round(b * 255),
  };
};

/**
 * Add opacity to HSL color
 */
export const withOpacity = (hsl: string, opacity: number): string => {
  const rgb = hslToRgb(hsl);
  return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${opacity})`;
};

/**
 * Get color with hover state
 */
export const getColorWithHover = (baseColor: string, hoverColor: string) => ({
  base: baseColor,
  hover: hoverColor,
  css: `
    color: ${baseColor};
    transition: color 150ms ease-in-out;
    
    &:hover {
      color: ${hoverColor};
    }
  `,
});

// ============================================================================
// EXPORT
// ============================================================================

export const colorSystem = {
  colors,
  lightTheme,
  darkTheme,
  withOpacity,
  getColorWithHover,
};

export default colorSystem;
