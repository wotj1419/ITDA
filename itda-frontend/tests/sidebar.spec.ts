import { test, expect } from '@playwright/test';

test('Sidebar Profile Dropdown Test', async ({ page }) => {
  // 1. Visit the site
  await page.goto('/auth');

  // 2. Login Flow
  await page.getByTestId('auth-tab-login').click();
  await page.getByPlaceholder('your@email.com').fill('test@example.com');
  await page.getByPlaceholder('비밀번호').fill('password123');
  await page.getByTestId('auth-submit-login').click();

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
