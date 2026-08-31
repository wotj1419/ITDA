import { expect, test } from '@playwright/test'

test('shows the dashboard without requiring a login', async ({ page }) => {
  await page.goto('/dashboard')

  await expect(page).toHaveURL(/\/dashboard$/)
  await expect(page.getByRole('heading', { name: '내 프로젝트' })).toBeVisible()
})
