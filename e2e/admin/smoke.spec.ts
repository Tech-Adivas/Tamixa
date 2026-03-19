import { test, expect } from "@playwright/test";

/**
 * Admin dashboard smoke tests.
 * Run with: ADMIN_BASE_URL=http://localhost:3001 npm run test
 * Requires admin app and backend to be running.
 */
test.describe("Admin app smoke @smoke", () => {
  test("admin login page loads", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Admin|Tamixa/i);
    await expect(page.getByRole("textbox", { name: /email/i })).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();
  });

  test("unauthenticated redirect to login from dashboard", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/(login)?$/);
    await expect(page.getByRole("textbox", { name: /email/i })).toBeVisible();
  });
});
