// === Enums ===
export type Role = 'ADMIN' | 'PM' | 'DEPT_MANAGER' | 'EXECUTOR' | 'HR'
export type ProjectStatus = 'ACTIVE' | 'CLOSED' | 'DELETED'
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED'
export type HoursRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type HoursRequestTargetType = 'TASK' | 'PROJECT'
export type NotificationType =
  | 'TASK_HOURS_EXHAUSTED'
  | 'HOURS_REQUEST_SUBMITTED'
  | 'HOURS_REQUEST_APPROVED'
  | 'HOURS_REQUEST_REJECTED'
  | 'TASK_COMPLETED'
  | 'TASK_UNASSIGNED'

// === Auth ===
export interface LoginRequest {
  email: string
  password: string
}

export interface UserInfo {
  id: number
  name: string
  email: string
  roles: Role[]
  departmentId: number
  departmentName: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
  forcePasswordChange: boolean
}

export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

// === Project ===
export interface Project {
  id: number
  name: string
  status: ProjectStatus
  totalBudgetHours: number
  consumedHours: number
  remainingHours: number
  pmId: number
  pmName: string
  departmentId: number | null
  departmentName: string | null
  createdAt: string
  closedAt: string | null
}

export interface CreateProjectRequest {
  name: string
  totalBudgetHours: number
  pmId: number
  departmentId: number
}

export interface UpdateProjectRequest {
  name: string
  totalBudgetHours: number
  pmId: number
}

// === Task ===
export interface Task {
  id: number
  name: string
  projectId: number
  projectName: string
  status: TaskStatus
  budgetHours: number
  consumedHours: number
  remainingHours: number
  assigneeId: number | null
  assigneeName: string | null
  createdAt: string
}

export interface CreateTaskRequest {
  name: string
  budgetHours: number
  assigneeId?: number
}

export interface UpdateTaskRequest {
  name: string
  budgetHours: number
  assigneeId?: number
}

// === Work Entry ===
export interface WorkEntry {
  id: number
  taskId: number
  taskName: string
  projectName: string
  workDate: string
  hours: number
  editable: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateWorkEntryRequest {
  taskId: number
  workDate: string
  hours: number
}

export interface UpdateWorkEntryRequest {
  hours: number
}

export interface WorkEntryResponse {
  id: number
  taskId: number
  taskName: string
  workDate: string
  hours: number
  taskRemainingHours: number
  warning: string | null
}

// === Hours Request ===
export interface HoursRequest {
  id: number
  projectId: number
  projectName: string
  requesterId: number
  requesterName: string
  requestedHours: number
  description: string
  targetType: HoursRequestTargetType
  targetTaskId: number | null
  targetTaskName: string | null
  status: HoursRequestStatus
  reviewerId: number | null
  reviewerName: string | null
  reviewComment: string | null
  reviewedAt: string | null
  createdAt: string
}

export interface CreateHoursRequestRequest {
  projectId: number
  requestedHours: number
  description: string
  targetType: HoursRequestTargetType
  targetTaskId?: number
}

// === User / HR ===
export interface User {
  id: number
  name: string
  email: string
  departmentId: number
  departmentName: string
  roles: Role[]
  active: boolean
  createdAt: string
}

export interface CreateUserRequest {
  name: string
  email: string
  departmentId: number
  roles: Role[]
}

export interface UpdateUserRequest {
  name: string
  departmentId: number
  roles: Role[]
}

// === Department ===
export interface DepartmentMember {
  userId: number
  userName: string
  weeklyHours: number
  monthlyHours: number
  activeTaskCount: number
}

export interface MemberTask {
  taskId: number
  taskName: string
  projectName: string
  status: TaskStatus
  consumedHours: number
}

// === Notification ===
export interface Notification {
  id: number
  type: NotificationType
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

// === PM Dashboard ===
export interface TaskSummary {
  total: number
  pending: number
  inProgress: number
  completed: number
  closed: number
}

export interface ProjectDashboard {
  id: number
  name: string
  status: ProjectStatus
  totalBudgetHours: number
  consumedHours: number
  remainingHours: number
  usageRate: number
  taskSummary: TaskSummary
}

// === Common ===
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}
