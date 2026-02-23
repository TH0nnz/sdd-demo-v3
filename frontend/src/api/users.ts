import http from './http'
import type { User, CreateUserRequest, UpdateUserRequest, PageResponse } from '@/types'

export interface CreateUserResponse {
  user: User
  temporaryPassword: string
}

export interface ResetPasswordResponse {
  temporaryPassword: string
}

export const usersApi = {
  fetchUsers(params?: { page?: number; size?: number }) {
    return http.get<PageResponse<User>>('/users', { params })
  },

  fetchPmUsers() {
    return http.get<User[]>('/users', { params: { role: 'PM' } })
  },

  fetchUsersByRole(role: string) {
    return http.get<User[]>('/users', { params: { role } })
  },

  createUser(data: CreateUserRequest) {
    return http.post<CreateUserResponse>('/users', data)
  },

  updateUser(id: number, data: UpdateUserRequest) {
    return http.put<User>(`/users/${id}`, data)
  },

  disableUser(id: number) {
    return http.post<User>(`/users/${id}/disable`)
  },

  enableUser(id: number) {
    return http.post<User>(`/users/${id}/enable`)
  },

  resetPassword(id: number) {
    return http.post<ResetPasswordResponse>(`/users/${id}/reset-password`)
  },
}
