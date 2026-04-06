import { describe, expect, it } from 'vitest'
import { getDefaultRoute } from '@/router'

describe('getDefaultRoute', () => {
  it('returns admin route first when ADMIN exists', () => {
    expect(getDefaultRoute(['ADMIN', 'PM'])).toBe('/admin/projects')
  })

  it('returns PM route for PM', () => {
    expect(getDefaultRoute(['PM'])).toBe('/pm/dashboard')
  })

  it('returns executor route for EXECUTOR', () => {
    expect(getDefaultRoute(['EXECUTOR'])).toBe('/executor/tasks')
  })

  it('returns login for empty roles', () => {
    expect(getDefaultRoute([])).toBe('/login')
  })
})


// 用途：Vitest 單元測試，驗證 router 的 getDefaultRoute 函式行為。
// 執行方式：
// 進入 frontend 目錄。
// 安裝依賴：npm install
// 執行：npx vitest run tests/unit/router/getDefaultRoute.spec.ts
// 或執行所有單元測試：npx vitest