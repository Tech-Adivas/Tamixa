import { test, expect } from "@playwright/test";

/**
 * Admin dashboard smoke tests.
 * Run with: ADMIN_BASE_URL=http://localhost:3001 npm run test
 * Requires admin app and backend to be running.
 * Optional full login check: set ADMIN_EMAIL and ADMIN_PASSWORD (dev: admin@techadivas.com / Admin123! after POST /api/v1/dev/seed-admin).
 */
test.describe("Admin app smoke @smoke", () => {
  test("landing page links to admin login", async ({ page }) => {
    await page.goto("/");
    await expect(page).toHaveTitle(/Admin|Tamixa/i);
    await expect(page.getByRole("heading", { name: /Tamixa Admin/i })).toBeVisible();
    await page.getByRole("link", { name: /admin login/i }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test("login page shows email and password fields", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByLabel(/email/i)).toBeVisible();
    await expect(page.getByLabel(/password/i)).toBeVisible();
  });

  test("unauthenticated dashboard redirects to login", async ({ page }) => {
    await page.goto("/dashboard");
    await expect(page).toHaveURL(/\/login(\?.*)?$/);
    await expect(page.getByLabel(/email/i)).toBeVisible();
  });

  test("admin can log in and see the dashboard", async ({ page }) => {
    const email = process.env.ADMIN_EMAIL;
    const password = process.env.ADMIN_PASSWORD;
    test.skip(!email || !password, "Set ADMIN_EMAIL and ADMIN_PASSWORD to run the login check");
    await page.goto("/login");
    await page.getByLabel(/email/i).fill(email!);
    await page.getByLabel(/password/i).fill(password!);
    await page.getByRole("button", { name: /log ?in|sign ?in/i }).click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 30_000 });
    await expect(page.getByRole("heading", { name: /dashboard/i }).first()).toBeVisible();
  });
});
