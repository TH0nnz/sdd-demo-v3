import { beforeEach, describe, expect, it } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('auth store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('sets auth state correctly', () => {
    const store = useAuthStore()
    store.setAuth(
      'token-1',
      {
        id: 1,
        name: 'Tester',
        email: 'tester@company.com',
        roles: ['EXECUTOR'],
        departmentId: 1,
        departmentName: '研發部',
      },
      true,
    )

    expect(store.isAuthenticated).toBe(true)
    expect(store.token).toBe('token-1')
    expect(store.hasRole('EXECUTOR')).toBe(true)
    expect(store.forcePasswordChange).toBe(true)
  })

  it('clears auth state on logout', () => {
    const store = useAuthStore()
    store.setAuth(
      'token-2',
      {
        id: 2,
        name: 'PM',
        email: 'pm@company.com',
        roles: ['PM'],
        departmentId: 1,
        departmentName: '研發部',
      },
      false,
    )

    store.logout()

    expect(store.isAuthenticated).toBe(false)
    expect(store.user).toBeNull()
    expect(store.token).toBeNull()
  })
})
