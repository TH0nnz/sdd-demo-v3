<template>
  <el-container class="main-layout">
    <el-aside
      width="220px"
      class="sidebar"
    >
      <div class="logo">
        <h2>報工系統</h2>
      </div>
      <el-menu
        :default-active="route.path"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <!-- EXECUTOR -->
        <template v-if="authStore.hasRole('EXECUTOR')">
          <el-menu-item index="/executor/tasks">
            <el-icon><List /></el-icon>
            <span>我的 Task</span>
          </el-menu-item>
          <el-menu-item index="/executor/work-entries">
            <el-icon><Clock /></el-icon>
            <span>工時填報</span>
          </el-menu-item>
        </template>

        <!-- PM -->
        <template v-if="authStore.hasRole('PM')">
          <el-menu-item index="/pm/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>專案儀表板</span>
          </el-menu-item>
          <el-menu-item index="/pm/hours-requests">
            <el-icon><DocumentAdd /></el-icon>
            <span>時數申請</span>
          </el-menu-item>
        </template>

        <!-- ADMIN -->
        <template v-if="authStore.hasRole('ADMIN')">
          <el-menu-item index="/admin/projects">
            <el-icon><FolderOpened /></el-icon>
            <span>專案管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/hours-review">
            <el-icon><Checked /></el-icon>
            <span>時數審核</span>
          </el-menu-item>
        </template>

        <!-- HR -->
        <template v-if="authStore.hasRole('HR')">
          <el-menu-item index="/hr/users">
            <el-icon><User /></el-icon>
            <span>使用者管理</span>
          </el-menu-item>
          <el-menu-item index="/hr/departments">
            <el-icon><OfficeBuilding /></el-icon>
            <span>部門管理</span>
          </el-menu-item>
        </template>

        <!-- DEPT_MANAGER -->
        <template v-if="authStore.hasRole('DEPT_MANAGER')">
          <el-menu-item index="/dept/overview">
            <el-icon><OfficeBuilding /></el-icon>
            <span>部門工時</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left" />
        <div class="header-right">
          <NotificationDropdown />
          <span class="username">{{ authStore.user?.name }}</span>
          <el-button
            type="danger"
            text
            @click="handleLogout"
          >
            登出
          </el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import NotificationDropdown from '@/components/notification/NotificationDropdown.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.main-layout {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  overflow-y: auto;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-badge {
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #606266;
}
</style>
