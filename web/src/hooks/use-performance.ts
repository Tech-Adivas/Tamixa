/**
 * Performance optimization hooks aligned with mobile P0 enhancements
 * Provides adaptive rendering and reduce motion support
 */

import { useEffect, useState, useMemo } from 'react';
import { designTokens } from '../lib/design-tokens';

/**
 * Hook to detect user's reduced motion preference (P0 enhancement)
 */
export function useReducedMotion(): boolean {
  const [prefersReducedMotion, setPrefersReducedMotion] = useState(false);

  useEffect(() => {
    const mediaQuery = window.matchMedia(designTokens.performance.prefersReducedMotion);
    
    // Set initial value
    setPrefersReducedMotion(mediaQuery.matches);
    
    // Listen for changes
    const handleChange = (e: MediaQueryListEvent) => {
      setPrefersReducedMotion(e.matches);
    };
    
    mediaQuery.addEventListener('change', handleChange);
    
    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, []);

  return prefersReducedMotion;
}

/**
 * Hook to detect low-end device for adaptive rendering (P0 enhancement)
 */
export function useDeviceCapabilities() {
  const [capabilities, setCapabilities] = useState({
    isLowEnd: false,
    deviceMemory: undefined as number | undefined,
    hardwareConcurrency: undefined as number | undefined,
    connectionType: undefined as string | undefined,
  });

  useEffect(() => {
    const nav = navigator as any;
    
    const deviceMemory = nav.deviceMemory;
    const hardwareConcurrency = nav.hardwareConcurrency;
    const connection = nav.connection;
    
    const isLowEnd = 
      (deviceMemory && deviceMemory < designTokens.performance.lowEndDeviceThreshold.ram) ||
      (hardwareConcurrency && hardwareConcurrency < designTokens.performance.lowEndDeviceThreshold.cores) ||
      (connection && (connection.effectiveType === 'slow-2g' || connection.effectiveType === '2g'));

    setCapabilities({
      isLowEnd: Boolean(isLowEnd),
      deviceMemory,
      hardwareConcurrency,
      connectionType: connection?.effectiveType,
    });
  }, []);

  return capabilities;
}

/**
 * Hook for adaptive animation duration based on user preferences and device capabilities
 */
export function useAdaptiveAnimation() {
  const prefersReducedMotion = useReducedMotion();
  const { isLowEnd } = useDeviceCapabilities();

  return useMemo(() => ({
    // Return minimal duration if reduced motion is preferred or device is low-end
    getDuration: (duration: keyof typeof designTokens.motion): string => {
      if (prefersReducedMotion || isLowEnd) {
        return '0.01ms';
      }
      return designTokens.motion[duration];
    },
    
    // Check if animations should be disabled
    shouldDisableAnimations: prefersReducedMotion || isLowEnd,
    
    // Get reduced animation count for low-end devices
    getMaxAnimatedElements: (): number => {
      if (isLowEnd) {
        return Math.floor(designTokens.performance.maxAnimatedElements * 0.6); // 60% reduction
      }
      return designTokens.performance.maxAnimatedElements;
    },
  }), [prefersReducedMotion, isLowEnd]);
}

/**
 * Hook for adaptive rendering (P0 enhancement)
 */
export function useAdaptiveRendering() {
  const { isLowEnd } = useDeviceCapabilities();
  
  return useMemo(() => ({
    // Reduce items per page on low-end devices
    getPageSize: (defaultSize: number): number => {
      if (isLowEnd) {
        return Math.max(5, Math.floor(defaultSize * 0.7)); // 30% reduction, minimum 5
      }
      return defaultSize;
    },
    
    // Disable heavy visual effects on low-end devices
    shouldUseSimpleEffects: isLowEnd,
    
    // Reduce story card animations on low-end devices
    getStoryCardCount: (defaultCount: number): number => {
      if (isLowEnd) {
        return Math.max(6, Math.floor(defaultCount * 0.6)); // 40% reduction, minimum 6
      }
      return defaultCount;
    },
    
    // Simplify background animations
    shouldUseSimpleBackground: isLowEnd,
  }), [isLowEnd]);
}

/**
 * Hook for performance monitoring (enterprise-grade)
 */
export function usePerformanceMonitoring() {
  const [metrics, setMetrics] = useState({
    renderTime: 0,
    interactionTime: 0,
    memoryUsage: 0,
  });

  useEffect(() => {
    // Monitor performance metrics
    const observer = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      
      entries.forEach((entry) => {
        if (entry.entryType === 'measure') {
          setMetrics(prev => ({
            ...prev,
            renderTime: entry.duration,
          }));
        }
        
        if (entry.entryType === 'navigation') {
          const navEntry = entry as PerformanceNavigationTiming;
          setMetrics(prev => ({
            ...prev,
            interactionTime: navEntry.loadEventEnd - navEntry.loadEventStart,
          }));
        }
      });
    });

    observer.observe({ entryTypes: ['measure', 'navigation'] });

    // Monitor memory usage if available
    const updateMemoryUsage = () => {
      const memory = (performance as any).memory;
      if (memory) {
        setMetrics(prev => ({
          ...prev,
          memoryUsage: memory.usedJSHeapSize / memory.jsHeapSizeLimit,
        }));
      }
    };

    const memoryInterval = setInterval(updateMemoryUsage, 5000);

    return () => {
      observer.disconnect();
      clearInterval(memoryInterval);
    };
  }, []);

  return metrics;
}

/**
 * Hook for focus management and accessibility (P0 enhancement)
 */
export function useFocusManagement() {
  const [focusVisible, setFocusVisible] = useState(false);

  useEffect(() => {
    let hadKeyboardEvent = false;

    const onKeyDown = () => {
      hadKeyboardEvent = true;
    };

    const onMouseDown = () => {
      hadKeyboardEvent = false;
    };

    const onFocus = () => {
      setFocusVisible(hadKeyboardEvent);
    };

    const onBlur = () => {
      setFocusVisible(false);
    };

    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('mousedown', onMouseDown);
    document.addEventListener('focusin', onFocus);
    document.addEventListener('focusout', onBlur);

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('mousedown', onMouseDown);
      document.removeEventListener('focusin', onFocus);
      document.removeEventListener('focusout', onBlur);
    };
  }, []);

  return {
    focusVisible,
    getFocusStyles: () => focusVisible ? designTokens.focusRing.default : '',
  };
}