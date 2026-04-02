/**
 * Web app API configuration (Vite / browser).
 * Keep behavior aligned with repo root `config/api.config.ts` (shared spec for admin/mobile docs).
 */
export const DEFAULT_API_BASE_URL = (() => {
  try {
    const v = (import.meta as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL
    if (typeof v === "string" && v.trim().length > 0) return v.trim().replace(/\/$/, "")
  } catch {
    /* non-Vite consumers */
  }
  return "http://localhost:8080"
})()

export const API_PATH = "/api/v1"

export const DEFAULT_FULL_API_URL = `${DEFAULT_API_BASE_URL}${API_PATH}`

export const DEFAULT_WEB_APP_URL = "http://localhost:3000"

export const SUBSCRIPTION_PATH = "/subscription"

export const DEFAULT_SUBSCRIPTION_URL = `${DEFAULT_WEB_APP_URL}${SUBSCRIPTION_PATH}`
