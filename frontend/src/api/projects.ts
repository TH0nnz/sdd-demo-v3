import http from './http'
import type { Project, CreateProjectRequest, UpdateProjectRequest, PageResponse } from '@/types'

export const projectsApi = {
  getProjects(params?: { page?: number; size?: number }) {
    return http.get<PageResponse<Project>>('/projects', { params })
  },
  getProject(id: number) {
    return http.get<Project>(`/projects/${id}`)
  },
  createProject(data: CreateProjectRequest) {
    return http.post<Project>('/projects', data)
  },
  updateProject(id: number, data: UpdateProjectRequest) {
    return http.put<Project>(`/projects/${id}`, data)
  },
  closeProject(id: number) {
    return http.post<Project>(`/projects/${id}/close`)
  },
  activateProject(id: number) {
    return http.post<Project>(`/projects/${id}/activate`)
  },
  deleteProject(id: number) {
    return http.delete(`/projects/${id}`)
  },
}
