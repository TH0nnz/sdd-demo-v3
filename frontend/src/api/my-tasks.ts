import http from './http'
import type { Task, PageResponse } from '@/types'

export const myTasksApi = {
  getMyTasks(params?: { status?: string; page?: number; size?: number }) {
    return http.get<PageResponse<Task>>('/my-tasks', { params })
  },
  completeTask(taskId: number) {
    return http.post<Task>(`/my-tasks/${taskId}/complete`)
  },
}
