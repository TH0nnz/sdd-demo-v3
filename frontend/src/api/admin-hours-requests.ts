import http from './http'
import type { HoursRequest, PageResponse } from '@/types'

export interface ReviewHoursRequestRequest {
  decision: 'APPROVED' | 'REJECTED'
  reviewNote?: string
}

export const adminHoursRequestApi = {
  getAllRequests(params?: { page?: number; size?: number }) {
    return http.get<PageResponse<HoursRequest>>('/admin/hours-requests', { params })
  },
  reviewRequest(id: number, data: ReviewHoursRequestRequest) {
    return http.post<HoursRequest>(`/admin/hours-requests/${id}/review`, data)
  },
}
