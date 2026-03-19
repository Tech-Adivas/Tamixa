import { defineConfig, devices } from "@playwright/test";

const webBaseURL = process.env.WEB_BASE_URL ?? "http://localhost:3000";
const adminBaseURL = process.env.ADMIN_BASE_URL ?? "http://localhost:3001";

export default defineConfig({
  testDir: ".",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: "list",
  use: {
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "web",
      use: {
        ...devices["Desktop Chrome"],
        baseURL: webBaseURL,
      },
      testMatch: /web\/.*\.spec\.ts/,
    },
    {
      name: "admin",
      use: {
        ...devices["Desktop Chrome"],
        baseURL: adminBaseURL,
      },
      testMatch: /admin\/.*\.spec\.ts/,
    },
  ],
  webServer: undefined,
});
