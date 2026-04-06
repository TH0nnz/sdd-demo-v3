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


// 這個檔案是 Playwright 的 E2E（端對端）自動化測試腳本，用來驗證「執行人員工時填報」功能。你可以依下列步驟執行：
//
// 1. **安裝 Playwright 及瀏覽器驅動**
// 若尚未安裝，請在 frontend 目錄下執行：
//    ```
//    npm install
//    npx playwright install
//    ```
//
// 2. **啟動前端與後端服務**
// 確保你的前後端服務都已啟動（通常用 docker compose up -d --build）。
//
// 3. **執行 E2E 測試**
// 在 frontend 目錄下執行：
//    ```
//    npx playwright test tests/e2e/us1-work-entry.spec.ts
//    ```
// 或執行全部 E2E 測試：
//    ```
//    npx playwright test
//    ```
//
// 4. **檢查測試結果**
// 終端機會顯示每個情境（Scenario）是否通過。失敗時可用
//   ```
//    npx playwright show-report
//    ```
// 查看詳細報告與截圖。
//
// 如需自動登入、資料準備等，請確保測試帳號（executor@company.com / Welcome123）在資料庫中存在。