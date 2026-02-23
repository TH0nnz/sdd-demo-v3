import http from './http'
import type { LoginRequest, LoginResponse, ChangePasswordRequest } from '@/types'

export const authApi = {
  login(data: LoginRequest) {
    return http.post<LoginResponse>('/auth/login', data)
  },
  changePassword(data: ChangePasswordRequest) {
    return http.post<{ message: string }>('/auth/change-password', data)
  },
}
