import http from './http'
import type { HoursRequest, CreateHoursRequestRequest, PageResponse } from '@/types'

export const hoursRequestApi = {
  getRequests(params?: { page?: number; size?: number }) {
    return http.get<PageResponse<HoursRequest>>('/hours-requests', { params })
  },
  createRequest(data: CreateHoursRequestRequest) {
    return http.post<HoursRequest>('/hours-requests', data)
  },
}
