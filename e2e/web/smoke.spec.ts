import { test, expect } from "@playwright/test";

/**
 * Web app smoke tests.
 * Run with: WEB_BASE_URL=http://localhost:3000 npm run test
 * Requires web app and backend to be running.
 */
test.describe("Web app smoke @smoke", () => {
  test("home page loads", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Tamixa|tamixa|Stories/i);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  });

  test("login page loads", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: /log in|sign in/i })).toBeVisible();
    await expect(page.getByRole("textbox", { name: /email/i })).toBeVisible();
  });

  test("privacy and terms links exist on home", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("link", { name: /privacy/i })).toBeVisible();
    await expect(page.getByRole("link", { name: /terms/i })).toBeVisible();
  });
});
