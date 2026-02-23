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
