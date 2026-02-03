import { expect, test } from '@playwright/test'
import { normalizeDurationSeconds, toDurationSeconds } from '../src/utils/duration'

test.describe('duration utils regression', () => {
  test('normalizes milliseconds and seconds consistently', () => {
    expect(toDurationSeconds(4200)).toBe(4)
    expect(toDurationSeconds(7)).toBe(7)
    expect(toDurationSeconds(undefined)).toBe(0)
    expect(normalizeDurationSeconds(undefined)).toBe(5)
    expect(normalizeDurationSeconds(0)).toBe(5)
    expect(normalizeDurationSeconds(3200)).toBe(3)
  })
})
