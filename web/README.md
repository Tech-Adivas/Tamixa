# Tamixa — Web

React + Vite + TypeScript frontend for Tamixa. Talks to the **backend** API (`/api/v1`).

## Setup

```bash
npm ci
```

## Run

- **Dev** (with API proxy to `http://localhost:8080`):
  ```bash
  npm run dev
  ```
  Opens at `http://localhost:3000`.

- **From root Gradle**:
  ```bash
  ./gradlew :web:npm_run_dev
  ```

## Build

```bash
npm run build
```

Output in `dist/`. From root: `./gradlew :web:npm_run_build`.

## Proxy

In dev, Vite proxies `/api` to the backend (see `vite.config.ts`). Set `server.port` and `server.proxy.target` as needed.
