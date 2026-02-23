import http from './http'

export interface DeptOverview {
  deptName: string
  memberCount: number
  totalHoursThisMonth: number
  members: MemberSummary[]
}

export interface MemberSummary {
  userId: number
  name: string
  totalHoursThisMonth: number
  todayHours: number
}

export interface Department {
  id: number
  name: string
}

export const deptApi = {
  getOverview() {
    return http.get<DeptOverview>('/dept/overview')
  },

  listDepartments() {
    return http.get<Department[]>('/dept/departments')
  },
}
