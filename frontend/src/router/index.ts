import { createRouter, createWebHistory } from 'vue-router'
import type { Role } from '@/types'
import { useAuthStore } from '@/stores/auth'

export function getDefaultRoute(roles: Role[] | readonly Role[]): string {
  if (roles.includes('ADMIN')) return '/admin/projects'
  if (roles.includes('PM')) return '/pm/dashboard'
  if (roles.includes('DEPT_MANAGER')) return '/dept/overview'
  if (roles.includes('EXECUTOR')) return '/executor/tasks'
  if (roles.includes('HR')) return '/hr/users'
  return '/login'
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/auth/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('@/pages/auth/ChangePasswordPage.vue'),
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        // EXECUTOR
        {
          path: 'executor/tasks',
          name: 'executor-tasks',
          component: () => import('@/pages/executor/MyTasksPage.vue'),
          meta: { roles: ['EXECUTOR'] as Role[] },
        },
        {
          path: 'executor/work-entries',
          name: 'executor-work-entries',
          component: () => import('@/pages/executor/WorkEntryPage.vue'),
          meta: { roles: ['EXECUTOR'] as Role[] },
        },
        // PM
        {
          path: 'pm/dashboard',
          name: 'pm-dashboard',
          component: () => import('@/pages/pm/ProjectDashboardPage.vue'),
          meta: { roles: ['PM'] as Role[] },
        },
        {
          path: 'pm/tasks/:projectId',
          name: 'pm-tasks',
          component: () => import('@/pages/pm/TaskManagementPage.vue'),
          meta: { roles: ['PM'] as Role[] },
        },
        {
          path: 'pm/hours-requests',
          name: 'pm-hours-requests',
          component: () => import('@/pages/pm/HoursRequestPage.vue'),
          meta: { roles: ['PM'] as Role[] },
        },
        // ADMIN
        {
          path: 'admin/projects',
          name: 'admin-projects',
          component: () => import('@/pages/admin/ProjectManagementPage.vue'),
          meta: { roles: ['ADMIN'] as Role[] },
        },
        {
          path: 'admin/hours-review',
          name: 'admin-hours-review',
          component: () => import('@/pages/admin/HoursReviewPage.vue'),
          meta: { roles: ['ADMIN'] as Role[] },
        },
        // HR
        {
          path: 'hr/users',
          name: 'hr-users',
          component: () => import('@/pages/hr/UserManagementPage.vue'),
          meta: { roles: ['HR'] as Role[] },
        },
        // DEPT_MANAGER
        {
          path: 'dept/overview',
          name: 'dept-overview',
          component: () => import('@/pages/dept/DepartmentOverviewPage.vue'),
          meta: { roles: ['DEPT_MANAGER'] as Role[] },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // Public routes
  if (to.meta.public) {
    if (authStore.isAuthenticated) {
      next(getDefaultRoute(authStore.roles))
    } else {
      next()
    }
    return
  }

  // Not authenticated
  if (!authStore.isAuthenticated) {
    next({ name: 'login' })
    return
  }

  // Force password change
  if (authStore.forcePasswordChange && to.name !== 'change-password') {
    next({ name: 'change-password' })
    return
  }

  // Role check
  const requiredRoles = to.meta.roles as Role[] | undefined
  if (requiredRoles && requiredRoles.length > 0) {
    const hasAccess = requiredRoles.some((r) => authStore.hasRole(r))
    if (!hasAccess) {
      next(getDefaultRoute(authStore.roles))
      return
    }
  }

  next()
})

export default router
