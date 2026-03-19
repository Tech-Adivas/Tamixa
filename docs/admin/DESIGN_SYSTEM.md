# Tamixa Design System

**Tagline:** Listen • Learn • Shine  
**Mascot:** Friendly yellow star reading a purple book

## Design tokens

Defined in `src/app/globals.css` and `tailwind.config.ts`.

### Brand colors (Tailwind: `tamixa.*`)

| Token        | Hex       | Usage              |
|-------------|-----------|--------------------|
| Blue        | `#3B82F6` | Links, trust       |
| Purple      | `#7C3AED` | Primary, accent    |
| Yellow      | `#FFD84D` | Rewards, stars     |
| Orange      | `#FF8A00` | CTAs, warmth       |
| Pink        | `#FF4D8D` | Highlights         |

### Semantic colors

- `primary` → Tamixa purple (buttons, links, focus)
- `secondary` → Soft blue
- `accent` → Warm highlight (yellow tint)
- `success` → Green (success states)
- `destructive` → Pink/red

### Gradients

- **Blue → Purple:** `from-[#4F46E5] to-[#7C3AED]` (primary CTAs)
- **Yellow → Orange:** `from-tamixa-yellow to-tamixa-orange` (rewards)

### Spacing (8px grid)

Use Tailwind spacing: `1` (4px) through `16` (64px). Minimum touch target: `min-h-touch min-w-touch` (48px).

### Motion

- **Micro:** 120ms — taps, toggles
- **Standard:** 240ms — cards, buttons
- **Reward:** 600ms — stars, celebrations

Animations: `animate-tamixa-sparkle`, `animate-tamixa-bounce-in`, `animate-tamixa-float`, `animate-tamixa-pulse-soft`.

### Typography

- **Font:** Nunito (primary), Inter (fallback) — set in `layout.tsx`
- **Weights:** 400, 600, 700, 800
- Use `font-semibold` for emphasis; keep body large and readable.

### Border radius

- Cards/buttons: `rounded-xl` (1rem) or `rounded-2xl`
- Pills: `rounded-full`

## App icon / logo

Use the **`.tamixa-app-icon`** wrapper so the logo fits inside any container shape (circle, rounded square, pill, etc.):

- **Container:** Add `tamixa-app-icon` to the element that wraps the logo image. Set the shape with Tailwind (e.g. `rounded-xl`, `rounded-full` for circle, `rounded-2xl`).
- **Image:** The logo scales with `object-fit: contain`, centered, and never crops. Works with Next.js `Image` or plain `img`.

Example: circle icon `rounded-full w-12 h-12 tamixa-app-icon`, rounded square `rounded-xl w-12 h-12 tamixa-app-icon`.

## Component library

Located in `src/components/design-system/`. Import from `@/components/design-system`.

| Component            | Purpose                          |
|----------------------|----------------------------------|
| `LessonCard`         | Story/lesson card with illustration |
| `RewardBadge`        | Star reward label                |
| `AppHeader`          | App header with logo/slots       |
| `MascotHelper`       | Mascot + message                 |
| `StarProgressBar`    | Star count (e.g. 3/5)            |
| `PlayButton`         | Play/pause (story player)        |
| `RewardStar`         | Single star (filled/empty)       |
| `StoryProgressBar`    | Linear progress                  |
| `AudioControlButton` | Play, pause, replay, next, volume |

## UI primitives (updated for Tamixa)

- **Button** (`@/components/ui/button`): variants `default`, `primary`, `secondary`, `accent`, `success`, `destructive`, `outline`, `ghost`, `link`. Sizes: `default` (48px min), `sm`, `lg`, `icon`, `icon-sm`.
- **Card** (`@/components/ui/card`): `rounded-2xl`, `shadow-card`, hover `shadow-card-hover`.
- **Badge** (`@/components/ui/badge`): variants include `reward`, `success`, `star`.

## Accessibility

- Minimum touch target: 48px for primary actions.
- Use `aria-label` on icon-only buttons.
- Focus visible: `ring-2 ring-ring ring-offset-2`.
- Prefer semantic colors; ensure contrast (AA).

## Dark / Bedtime mode

Toggle with `dark` class or `data-theme="bedtime"`. Tokens in `globals.css` switch to calmer, lower-contrast values.
