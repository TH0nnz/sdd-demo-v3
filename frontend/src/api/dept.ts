import http from './http'

export interface DeptOverview {
  deptName: string
  memberCount: number
  totalHoursThisWeek: number
  totalHoursThisMonth: number
  members: MemberSummary[]
}

export interface MemberSummary {
  userId: number
  name: string
  totalHoursThisWeek: number
  totalHoursThisMonth: number
  todayHours: number
}

export interface MemberTask {
  taskId: number
  taskName: string
  projectName: string
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED'
  consumedHours: number
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

  getMemberTasks(userId: number) {
    return http.get<MemberTask[]>(`/dept/members/${userId}/tasks`)
  },

  listDepartmentsForHr() {
    return http.get<Department[]>('/hr/departments')
  },

  createDepartment(data: { name: string }) {
    return http.post<Department>('/hr/departments', data)
  },

  updateDepartment(id: number, data: { name: string }) {
    return http.put<Department>(`/hr/departments/${id}`, data)
  },

  deleteDepartment(id: number) {
    return http.delete<void>(`/hr/departments/${id}`)
  },
}
