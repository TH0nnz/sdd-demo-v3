import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getNotifications, markAsRead as markAsReadApi, getUnreadCount } from '@/api/notifications'
import type { Notification } from '@/types'

export const useNotificationStore = defineStore('notifications', () => {
  const notifications = ref<Notification[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)

  let pollTimer: ReturnType<typeof setInterval> | null = null

  async function fetchNotifications(page = 0, size = 20) {
    loading.value = true
    try {
      const res = await getNotifications(page, size)
      notifications.value = res.data.content
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      unreadCount.value = res.data
    } catch {
      // silently ignore polling errors
    }
  }

  async function markAsRead(id: number) {
    await markAsReadApi(id)
    const n = notifications.value.find((n) => n.id === id)
    if (n && !n.isRead) {
      n.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  function startPolling() {
    stopPolling()
    fetchUnreadCount()
    pollTimer = setInterval(fetchUnreadCount, 30_000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  // Watch auth store – stop polling on logout, start on login
  const authStore = useAuthStore()
  watch(
    () => authStore.isAuthenticated,
    (authenticated) => {
      if (authenticated) {
        startPolling()
      } else {
        stopPolling()
        notifications.value = []
        unreadCount.value = 0
      }
    },
    { immediate: true },
  )

  return {
    notifications,
    unreadCount,
    loading,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    startPolling,
    stopPolling,
  }
})
