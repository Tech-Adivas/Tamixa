/**
 * Performance Provider - Applies P0 enhancements automatically
 * Integrates mobile performance optimizations with web frontend
 */

import { useEffect } from 'react';
import { useReducedMotion, useDeviceCapabilities } from '../hooks/use-performance';

interface PerformanceProviderProps {
  children: React.ReactNode;
}

export function PerformanceProvider({ children }: PerformanceProviderProps) {
  const prefersReducedMotion = useReducedMotion();
  const { isLowEnd } = useDeviceCapabilities();

  useEffect(() => {
    // Apply CSS classes based on user preferences and device capabilities
    const htmlElement = document.documentElement;
    
    // Reduced motion support (P0 enhancement)
    if (prefersReducedMotion) {
      htmlElement.classList.add('reduced-motion');
    } else {
      htmlElement.classList.remove('reduced-motion');
    }
    
    // Low-end device optimizations (P0 enhancement)
    if (isLowEnd) {
      htmlElement.classList.add('low-end-device');
    } else {
      htmlElement.classList.remove('low-end-device');
    }
    
    // Performance optimization: Reduce animations on low-end devices
    if (isLowEnd || prefersReducedMotion) {
      // Disable complex animations
      const style = document.createElement('style');
      style.textContent = `
        .complex-animation,
        .tamixa-splash__hero,
        .tamixa-splash__sparkle,
        .tamixa-splash__ring {
          animation: none !important;
          transition: none !important;
        }
        
        .heavy-shadow,
        .story-card:hover {
          box-shadow: var(--tamixa-shadow) !important;
          transform: none !important;
        }
        
        .tamixa-splash__particle {
          display: none !important;
        }
      `;
      document.head.appendChild(style);
      
      return () => {
        document.head.removeChild(style);
      };
    }
  }, [prefersReducedMotion, isLowEnd]);

  // Performance monitoring in development
  useEffect(() => {
    if (import.meta.env.DEV) {
      console.log('Performance Provider:', {
        prefersReducedMotion,
        isLowEnd,
        deviceCapabilities: {
          memory: navigator.deviceMemory,
          cores: navigator.hardwareConcurrency,
          connection: navigator.connection?.effectiveType,
        },
      });
    }
  }, [prefersReducedMotion, isLowEnd]);

  return <>{children}</>;
}

/**
 * Hook to get performance-aware CSS classes
 */
export function usePerformanceClasses() {
  const prefersReducedMotion = useReducedMotion();
  const { isLowEnd } = useDeviceCapabilities();

  return {
    container: [
      prefersReducedMotion && 'reduced-motion',
      isLowEnd && 'low-end-device',
    ].filter(Boolean).join(' '),
    
    animation: prefersReducedMotion || isLowEnd ? '' : 'tamixa-performance-optimized animate',
    
    shadow: isLowEnd ? 'shadow-sm' : 'shadow-card',
    
    transition: prefersReducedMotion || isLowEnd 
      ? '' 
      : 'transition-all duration-200 ease-out',
  };
}