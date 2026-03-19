# Tamixa Shared Configuration

Common configuration for web, admin, and mobile apps. Uses **localhost** for local development.

## API Configuration

| Setting | Default | Env Override | Used By |
|---------|---------|--------------|---------|
| API Base URL | `http://localhost:8080` | `NEXT_PUBLIC_API_URL` (admin) | Admin, Mobile |
| API Path | `/api/v1` | — | All |
| Web App URL | `http://localhost:3000` | `MAGIC_LINK_BASE_URL` (backend) | Backend (magic links) |

## Per-App Usage

### Web
- Uses Vite proxy: `/api` → `http://localhost:8080`
- `config/api.config.ts` → `DEFAULT_API_BASE_URL` for proxy target

### Admin (Next.js)
- Imports `DEFAULT_API_BASE_URL` from config
- Override: `NEXT_PUBLIC_API_URL=http://localhost:8080` in `admin/.env.local`

### Mobile (Kotlin)
- Default: `http://10.0.2.2:8080` (Android emulator – 10.0.2.2 = host's localhost)
- **iOS Simulator**: Change to `http://localhost:8080` in ApiConfig.kt and build.gradle.kts
- **Physical device**: Use your machine's IP, e.g. `http://192.168.1.x:8080`
- **Subscription Web URL**: Debug uses `http://10.0.2.2:3000/subscription`; release uses `TAMIXA_WEB_APP_URL` (e.g. `https://app.tamixa.com/subscription`)

### Backend
- Magic link base URL: `app.magic-link.base-url` (default: `http://localhost:3000`)
