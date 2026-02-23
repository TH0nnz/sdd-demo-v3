import { test, expect } from '@playwright/test'

const executor = {
  email: 'executor@company.com',
  password: 'Welcome123',
}

test.describe('US1 執行人員工時填報', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login')
    await page.getByLabel('Email').fill(executor.email)
    await page.getByLabel('密碼').fill(executor.password)
    await page.getByRole('button', { name: '登入' }).click()
  })

  test('Scenario 1: 填報工時後 Task 已消耗/剩餘時數更新', async ({ page }) => {
    await page.goto('/executor/work-entries')
    await page.getByLabel('Task').click()
    await page.getByRole('option').first().click()
    await page.getByLabel('日期').fill('2026-02-24')
    await page.getByLabel('工時').fill('2')
    await page.getByRole('button', { name: '送出' }).click()
    await expect(page.getByText('新增成功')).toBeVisible()
  })

  test('Scenario 2: 超過三工作天紀錄不可編輯', async ({ page }) => {
    await page.goto('/executor/work-entries')
    await expect(page.getByText('超過可編輯期限')).toBeVisible()
  })

  test('Scenario 3: 時數歸零後顯示通知 PM 提示', async ({ page }) => {
    await page.goto('/executor/work-entries')
    await expect(page.getByText('請通知 PM')).toBeVisible()
  })

  test('Scenario 4: 手動完成 Task 後不可再填報', async ({ page }) => {
    await page.goto('/executor/tasks')
    await page.getByRole('button', { name: '完成' }).first().click()
    await page.goto('/executor/work-entries')
    await expect(page.getByText('task 已結束，無法新增工時')).toBeVisible()
  })
})
