/// <reference types="vitest" />
import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import { DEFAULT_API_BASE_URL } from "./src/config/api.config"

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: DEFAULT_API_BASE_URL,
        changeOrigin: true,
      },
    },
  },
})
