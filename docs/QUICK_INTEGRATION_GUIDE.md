# Quick Integration Guide - P0 Enhancements

**For developers integrating the P0 enhancements into existing components**

## 1. Admin (Next.js) Integration

### Add Performance Provider to App Root

```typescript
// admin/src/app/layout.tsx
import { PerformanceProvider } from '@/components/performance-provider';

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <PerformanceProvider>
          {children}
        </PerformanceProvider>
      </body>
    </html>
  );
}
```

### Use Design Tokens in Components

```typescript
// admin/src/components/MyComponent.tsx
import { designTokens } from '@/lib/design-tokens';
import { usePerformanceClasses } from '@/components/performance-provider';

export function MyComponent() {
  const { container, animation, shadow } = usePerformanceClasses();
  
  return (
    <div 
      className={`${container} ${animation}`}
      style={{
        padding: designTokens.spacing.cardContentPadding,
        borderRadius: designTokens.radius.card,
        boxShadow: designTokens.elevation.card,
      }}
    >
      Content with P0 enhancements
    </div>
  );
}
```

## 2. Web (React/Vite) Integration

### Add Performance Provider to App Root

```typescript
// web/src/main.tsx
import { PerformanceProvider } from './components/PerformanceProvider';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <PerformanceProvider>
      <App />
    </PerformanceProvider>
  </React.StrictMode>
);
```

### Use Performance Hooks

```typescript
// web/src/components/StoryCard.tsx
import { useAdaptiveAnimation, useReducedMotion } from '../hooks/use-performance';
import { designTokens } from '../lib/design-tokens';

export function StoryCard() {
  const { shouldDisableAnimations } = useAdaptiveAnimation();
  const prefersReducedMotion = useReducedMotion();
  
  return (
    <div 
      className={`story-card ${!shouldDisableAnimations ? 'animate-hover' : ''}`}
      style={{
        padding: designTokens.spacing.cardSpacing,
        borderRadius: designTokens.radius.card,
        minHeight: designTokens.accessibility.minTouchTarget,
      }}
    >
      Story content
    </div>
  );
}
```

## 3. CSS Class Usage

### Automatic Classes Applied by PerformanceProvider

```css
/* These classes are automatically applied based on user preferences */
.reduced-motion .complex-animation {
  animation: none !important;
}

.low-end-device .heavy-shadow {
  box-shadow: var(--shadow-sm) !important;
}
```

### Manual Class Application

```typescript
// Apply classes manually when needed
const className = [
  'base-component',
  prefersReducedMotion && 'reduced-motion',
  isLowEnd && 'low-end-device',
].filter(Boolean).join(' ');
```

## 4. Common Patterns

### Adaptive Table Rendering

```typescript
import { useAdaptiveRendering } from '@/hooks/use-performance';

export function DataTable({ data }: { data: any[] }) {
  const { getPageSize } = useAdaptiveRendering();
  const pageSize = getPageSize(20); // Default 20, reduced on low-end devices
  
  return (
    <Table>
      {data.slice(0, pageSize).map(item => (
        <TableRow key={item.id}>{item.name}</TableRow>
      ))}
    </Table>
  );
}
```

### Accessibility-First Buttons

```typescript
import { designTokens } from '@/lib/design-tokens';

export function AccessibleButton({ children, ...props }: ButtonProps) {
  return (
    <button
      {...props}
      className="min-touch-target focus-visible"
      style={{
        minWidth: designTokens.accessibility.minTouchTarget,
        minHeight: designTokens.accessibility.minTouchTarget,
        borderRadius: designTokens.radius.button,
      }}
    >
      {children}
    </button>
  );
}
```

### Performance-Aware Animations

```typescript
import { useAdaptiveAnimation } from '@/hooks/use-performance';

export function AnimatedCard() {
  const { getDuration, shouldDisableAnimations } = useAdaptiveAnimation();
  
  return (
    <div
      className={shouldDisableAnimations ? '' : 'animate-bounce-in'}
      style={{
        animationDuration: getDuration('standard'),
        transition: `transform ${getDuration('micro')} ease-out`,
      }}
    >
      Animated content
    </div>
  );
}
```

## 5. Testing Integration

### Test Performance Classes

```typescript
// Test that performance classes are applied correctly
import { render } from '@testing-library/react';
import { PerformanceProvider } from '@/components/performance-provider';

test('applies low-end device optimizations', () => {
  // Mock low-end device
  Object.defineProperty(navigator, 'deviceMemory', { value: 2 });
  
  render(
    <PerformanceProvider>
      <TestComponent />
    </PerformanceProvider>
  );
  
  expect(document.documentElement).toHaveClass('low-end-device');
});
```

### Test Reduced Motion

```typescript
test('respects reduced motion preference', () => {
  // Mock reduced motion preference
  Object.defineProperty(window, 'matchMedia', {
    value: jest.fn(() => ({ matches: true })),
  });
  
  render(
    <PerformanceProvider>
      <AnimatedComponent />
    </PerformanceProvider>
  );
  
  expect(document.documentElement).toHaveClass('reduced-motion');
});
```

## 6. Migration Checklist

### For Existing Components

- [ ] Replace hardcoded spacing with design tokens
- [ ] Add minimum touch target sizes to interactive elements
- [ ] Implement focus-visible styles for accessibility
- [ ] Add performance-aware animation classes
- [ ] Test with reduced motion preferences
- [ ] Validate on low-end device simulation

### For New Components

- [ ] Use design tokens from the start
- [ ] Implement adaptive rendering hooks
- [ ] Add accessibility attributes
- [ ] Test performance across device types
- [ ] Ensure WCAG 2.1 compliance

## 7. Quick Reference

### Design Token Import

```typescript
// Admin
import { designTokens } from '@/lib/design-tokens';

// Web
import { designTokens } from '../lib/design-tokens';
```

### Performance Hook Import

```typescript
// Admin
import { useAdaptiveAnimation, useReducedMotion } from '@/hooks/use-performance';

// Web
import { useAdaptiveAnimation, useReducedMotion } from '../hooks/use-performance';
```

### Common CSS Classes

```css
.focus-visible          /* Accessibility focus ring */
.min-touch-target      /* Minimum 48px touch target */
.reduced-motion        /* Applied when user prefers reduced motion */
.low-end-device        /* Applied on low-end devices */
```

This integration ensures consistent, accessible, and performant experiences across all Tamixa platforms while maintaining the mobile-first approach.