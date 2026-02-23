import http from './http'
import type { ProjectDashboard, PageResponse } from '@/types'

export const pmProjectsApi = {
  getDashboard(params?: { page?: number; size?: number }) {
    return http.get<PageResponse<ProjectDashboard>>('/pm/projects', { params })
  },
}
