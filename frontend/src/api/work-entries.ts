import http from './http'
import type { WorkEntry, CreateWorkEntryRequest, UpdateWorkEntryRequest, WorkEntryResponse, PageResponse } from '@/types'

export const workEntryApi = {
  getWorkEntries(params?: { startDate?: string; endDate?: string; taskId?: number; page?: number; size?: number }) {
    return http.get<PageResponse<WorkEntry>>('/work-entries', { params })
  },
  createWorkEntry(data: CreateWorkEntryRequest) {
    return http.post<WorkEntryResponse>('/work-entries', data)
  },
  updateWorkEntry(id: number, data: UpdateWorkEntryRequest) {
    return http.put<WorkEntryResponse>(`/work-entries/${id}`, data)
  },
}
