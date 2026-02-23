import http from './http'

export function getNotifications(page = 0, size = 20) {
  return http.get('/notifications', { params: { page, size } })
}

export function markAsRead(id: number) {
  return http.patch(`/notifications/${id}/read`)
}

export function getUnreadCount() {
  return http.get<number>('/notifications/unread-count')
}
