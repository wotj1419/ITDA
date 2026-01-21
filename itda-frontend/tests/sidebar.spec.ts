import { test, expect } from '@playwright/test';

test('Sidebar Profile Dropdown Test', async ({ page }) => {
    // 1. Visit the site
    await page.goto('/');

    // 2. Login Flow
    // Click the 'Login' tab. Using class selector to avoid whitespace issues with getByRole name.
    await page.locator('.tab', { hasText: '로그인' }).first().click();

    // Fill Email
    await page.getByPlaceholder('your@email.com').fill('test@example.com');

    // Fill Password
    await page.getByPlaceholder('••••••••').fill('password123');

    // Click Submit Button (Login)
    // Target the submit button specifically
    await page.locator('button[type="submit"]').click();

    // Wait for navigation
    await page.waitForURL('**/dashboard', { timeout: 15000 });

    // 3. Sidebar Interaction
    const profileArea = page.locator('.sidebar-user .user-trigger');
    await expect(profileArea).toBeVisible();

    // 4. Toggle Menu
    await profileArea.click();

    // 5. Verify Dropdown
    const dropdown = page.locator('.profile-menu');
    await expect(dropdown).toBeVisible();

    // 6. Verify Contents
    await expect(dropdown.locator('.menu-header')).toBeVisible();
    await expect(dropdown.getByRole('button', { name: '로그아웃' })).toBeVisible();

    // 7. Test Logout
    await dropdown.getByRole('button', { name: '로그아웃' }).click();

    // 8. Verify return to auth
    await page.waitForURL('**/auth');
});
