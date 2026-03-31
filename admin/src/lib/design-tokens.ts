/**
 * Tamixa Design System - Unified Design Tokens
 * Aligned with mobile P0 enhancements and enterprise standards
 * 
 * Listen • Learn • Shine
 */

// ============================================================================
// SPACING TOKENS (8dp grid system - aligned with mobile TamixaDesignTokens)
// ============================================================================

export const spacing = {
  // Base spacing scale (8dp grid)
  xs: '0.25rem',    // 4px
  sm: '0.5rem',     // 8px
  md: '0.75rem',    // 12px - smallSpacing
  lg: '1rem',       // 16px - cardSpacing
  xl: '1.25rem',    // 20px - cardContentPadding
  '2xl': '1.5rem',  // 24px - screenPadding, sectionSpacing
  '3xl': '2rem',    // 32px
  '4xl': '3rem',    // 48px - minTouchTargetSize
  '5xl': '4rem',    // 64px

  // Semantic spacing (aligned with mobile)
  screenPadding: '1.5rem',           // 24px
  sectionSpacing: '1.5rem',          // 24px
  cardSpacing: '1rem',               // 16px
  smallSpacing: '0.75rem',           // 12px
  cardContentPadding: '1.25rem',     // 20px
  headerPaddingHorizontal: '1.25rem', // 20px
  headerPaddingVertical: '1.125rem',  // 18px
  minTouchTargetSize: '3rem',        // 48px
} as const;

// ============================================================================
// BORDER RADIUS TOKENS (aligned with mobile)
// ============================================================================

export const radius = {
  none: '0',
  sm: '0.5rem',     // 8px
  md: '0.75rem',    // 12px
  lg: '1rem',       // 16px - inputRadius, cardRadius
  xl: '1.25rem',    // 20px - cardRadiusLarge
  '2xl': '1.5rem',  // 24px - dialogRadius, buttonRadius
  full: '9999px',

  // Semantic radii (aligned with mobile)
  input: '1rem',        // 16px
  card: '1rem',         // 16px
  cardLarge: '1.25rem', // 20px
  button: '1.5rem',     // 24px
  dialog: '1.5rem',     // 24px
  carousel: '1rem',     // 16px
} as const;

// ============================================================================
// ELEVATION TOKENS (aligned with mobile)
// ============================================================================

export const elevation = {
  none: '0',
  sm: '0 1px 2px 0 rgb(0 0 0 / 0.04)',
  md: '0 1px 3px 0 rgb(0 0 0 / 0.04), 0 4px 12px -4px rgb(0 0 0 / 0.06)',
  lg: '0 4px 12px -2px rgb(0 0 0 / 0.06), 0 12px 24px -8px rgb(0 0 0 / 0.1)',
  xl: '0 8px 24px -4px rgb(0 0 0 / 0.08), 0 16px 40px -12px rgb(0 0 0 / 0.08)',

  // Semantic elevations (aligned with mobile)
  card: '0 1px 3px 0 rgb(0 0 0 / 0.04), 0 4px 12px -4px rgb(0 0 0 / 0.06)',
  cardHover: '0 4px 12px -2px rgb(0 0 0 / 0.06), 0 12px 24px -8px rgb(0 0 0 / 0.1)',
  button: '0 2px 8px -2px rgb(0 0 0 / 0.15), 0 4px 12px -4px rgb(0 0 0 / 0.1)',
  fab: '0 4px 16px -4px rgb(0 0 0 / 0.15), 0 8px 24px -8px rgb(0 0 0 / 0.1)',
  glow: '0 0 24px -4px hsl(263 70% 58% / 0.25)',
} as const;

// ============================================================================
// MOTION TOKENS (aligned with mobile performance optimizations)
// ============================================================================

export const motion = {
  // Duration
  micro: '120ms',
  standard: '240ms',
  reward: '600ms',

  // Easing
  easeOutExpo: 'cubic-bezier(0.16, 1, 0.3, 1)',
  easeOutBounce: 'cubic-bezier(0.34, 1.56, 0.64, 1)',
  easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',

  // Reduce motion support (P0 enhancement)
  respectsReducedMotion: `
    @media (prefers-reduced-motion: reduce) {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
  `,
} as const;

// ============================================================================
// BREAKPOINTS (responsive design)
// ============================================================================

export const breakpoints = {
  sm: '640px',
  md: '768px',
  lg: '1024px',
  xl: '1280px',
  '2xl': '1536px',
} as const;

// ============================================================================
// ACCESSIBILITY TOKENS (P0 enhancement)
// ============================================================================

export const accessibility = {
  minTouchTarget: spacing.minTouchTargetSize,
  focusRingWidth: '2px',
  focusRingOffset: '2px',
  focusRingColor: 'hsl(217 91% 60%)',
  
  // High contrast support
  highContrastBorder: '2px solid currentColor',
  highContrastFocus: '4px solid currentColor',
} as const;

// ============================================================================
// PERFORMANCE TOKENS (P0 enhancement)
// ============================================================================

export const performance = {
  // Adaptive rendering thresholds
  lowEndDeviceThreshold: {
    ram: 3, // GB
    cores: 4,
  },
  
  // Animation performance
  maxAnimatedElements: 50,
  animationFrameTarget: 60, // fps
  
  // Reduce motion detection
  prefersReducedMotion: '(prefers-reduced-motion: reduce)',
} as const;

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Check if user prefers reduced motion (P0 enhancement)
 */
export const prefersReducedMotion = (): boolean => {
  if (typeof window === 'undefined') return false;
  return window.matchMedia(performance.prefersReducedMotion).matches;
};

/**
 * Get adaptive animation duration based on user preferences
 */
export const getAnimationDuration = (duration: keyof typeof motion): string => {
  return prefersReducedMotion() ? '0.01ms' : motion[duration];
};

/**
 * Create responsive spacing utility
 */
export const responsiveSpacing = (
  mobile: keyof typeof spacing,
  desktop?: keyof typeof spacing
) => {
  const mobileValue = spacing[mobile];
  const desktopValue = desktop ? spacing[desktop] : mobileValue;
  
  return {
    mobile: mobileValue,
    desktop: desktopValue,
    css: `${mobileValue}; @media (min-width: ${breakpoints.md}) { ${desktopValue} }`,
  };
};

/**
 * Create focus ring styles for accessibility
 */
export const focusRing = {
  default: `
    outline: ${accessibility.focusRingWidth} solid ${accessibility.focusRingColor};
    outline-offset: ${accessibility.focusRingOffset};
  `,
  
  highContrast: `
    outline: ${accessibility.highContrastFocus};
    outline-offset: ${accessibility.focusRingOffset};
  `,
};

// ============================================================================
// EXPORT ALL TOKENS
// ============================================================================

export const designTokens = {
  spacing,
  radius,
  elevation,
  motion,
  breakpoints,
  accessibility,
  performance,
  
  // Utility functions
  prefersReducedMotion,
  getAnimationDuration,
  responsiveSpacing,
  focusRing,
} as const;

export default designTokens;