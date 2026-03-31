/**
 * Performance Provider - Applies P0 enhancements automatically
 * Integrates mobile performance optimizations with admin frontend
 */

'use client';

import { useEffect } from 'react';
import { useReducedMotion, useDeviceCapabilities } from '@/hooks/use-performance';

interface PerformanceProviderProps {
  children: React.ReactNode;
}

export function PerformanceProvider({ children }: PerformanceProviderProps) {
  const prefersReducedMotion = useReducedMotion();
  const {
    isLowEnd,
    deviceMemory,
    hardwareConcurrency,
    connectionType,
  } = useDeviceCapabilities();

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
        .animate-tamixa-sparkle,
        .animate-tamixa-bounce-in {
          animation: none !important;
          transition: none !important;
        }
        
        .heavy-shadow {
          box-shadow: var(--shadow-sm) !important;
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
    if (process.env.NODE_ENV === 'development') {
      console.log('Performance Provider:', {
        prefersReducedMotion,
        isLowEnd,
        deviceCapabilities: {
          memory: deviceMemory,
          cores: hardwareConcurrency,
          connection: connectionType,
        },
      });
    }
  }, [prefersReducedMotion, isLowEnd, deviceMemory, hardwareConcurrency, connectionType]);

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
    
    animation: prefersReducedMotion || isLowEnd ? '' : 'animate-tamixa-bounce-in',
    
    shadow: isLowEnd ? 'shadow-sm' : 'shadow-card',
    
    transition: prefersReducedMotion || isLowEnd 
      ? '' 
      : 'transition-all duration-200 ease-out',
  };
}