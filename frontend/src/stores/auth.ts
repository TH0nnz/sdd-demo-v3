import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, Role } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const forcePasswordChange = ref(false)

  const isAuthenticated = computed(() => !!token.value)
  const roles = computed(() => user.value?.roles ?? [])

  function hasRole(role: Role): boolean {
    return roles.value.includes(role)
  }

  function hasAnyRole(...checkRoles: Role[]): boolean {
    return checkRoles.some((r) => roles.value.includes(r))
  }

  function setAuth(newToken: string, userInfo: UserInfo, forceChange: boolean) {
    token.value = newToken
    user.value = userInfo
    forcePasswordChange.value = forceChange
  }

  function logout() {
    token.value = null
    user.value = null
    forcePasswordChange.value = false
  }

  return {
    token,
    user,
    forcePasswordChange,
    isAuthenticated,
    roles,
    hasRole,
    hasAnyRole,
    setAuth,
    logout,
  }
})
