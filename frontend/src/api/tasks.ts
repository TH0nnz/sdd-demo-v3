import http from './http'
import type { Task, CreateTaskRequest, UpdateTaskRequest, PageResponse } from '@/types'

export const tasksApi = {
  getTasks(projectId: number, params?: { page?: number; size?: number }) {
    return http.get<PageResponse<Task>>(`/projects/${projectId}/tasks`, { params })
  },
  createTask(projectId: number, data: CreateTaskRequest) {
    return http.post<Task>(`/projects/${projectId}/tasks`, data)
  },
  updateTask(projectId: number, taskId: number, data: UpdateTaskRequest) {
    return http.put<Task>(`/projects/${projectId}/tasks/${taskId}`, data)
  },
  closeTask(projectId: number, taskId: number) {
    return http.post<Task>(`/projects/${projectId}/tasks/${taskId}/close`)
  },
  deleteTask(projectId: number, taskId: number) {
    return http.delete(`/projects/${projectId}/tasks/${taskId}`)
  },
}
