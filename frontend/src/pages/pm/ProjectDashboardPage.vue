<template>
  <div class="dashboard-page">
    <h2>專案儀表板</h2>

    <div
      v-if="loading"
      v-loading="true"
      style="min-height: 200px"
    />

    <el-empty
      v-else-if="projects.length === 0"
      description="目前沒有負責的專案"
    />

    <div
      v-else
      class="project-grid"
    >
      <el-card
        v-for="project in projects"
        :key="project.id"
        class="project-card"
        shadow="hover"
        style="cursor: pointer"
        @click="goToTasks(project.id)"
      >
        <template #header>
          <div class="card-header">
            <span class="project-name">{{ project.name }}</span>
            <el-tag
              :type="statusTagType(project.status)"
              size="small"
            >
              {{ project.status }}
            </el-tag>
          </div>
        </template>

        <div class="budget-section">
          <div class="budget-label">
            時數使用率：{{ project.usageRate }}%
          </div>
          <el-progress
            :percentage="Math.min(Number(project.usageRate), 100)"
            :color="progressColor(Number(project.usageRate))"
          />
          <div class="budget-detail">
            已用 {{ project.consumedHours }} / {{ project.totalBudgetHours }} 小時
            （剩餘 {{ project.remainingHours }}）
          </div>
        </div>

        <el-divider />

        <div class="task-summary">
          <div class="summary-title">
            任務摘要（共 {{ project.taskSummary.total }} 個）
          </div>
          <div class="summary-stats">
            <el-tag
              type="info"
              size="small"
            >
              待處理 {{ project.taskSummary.pending }}
            </el-tag>
            <el-tag
              type="primary"
              size="small"
            >
              進行中 {{ project.taskSummary.inProgress }}
            </el-tag>
            <el-tag
              type="success"
              size="small"
            >
              已完成 {{ project.taskSummary.completed }}
            </el-tag>
            <el-tag
              type="danger"
              size="small"
            >
              已關閉 {{ project.taskSummary.closed }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <div
      v-if="totalPages > 1"
      class="pagination-wrapper"
    >
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
        layout="prev, pager, next"
        @current-change="loadProjects"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pmProjectsApi } from '@/api/pm-projects'
import type { ProjectDashboard, ProjectStatus } from '@/types'

const router = useRouter()
const loading = ref(false)
const projects = ref<ProjectDashboard[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const totalPages = ref(0)

function statusTagType(status: ProjectStatus) {
  return status === 'ACTIVE' ? 'success' : 'danger'
}

function progressColor(rate: number) {
  if (rate >= 90) return '#F56C6C'
  if (rate >= 70) return '#E6A23C'
  return '#67C23A'
}

function goToTasks(projectId: number) {
  router.push({ name: 'pm-tasks', params: { projectId } })
}

async function loadProjects() {
  loading.value = true
  try {
    const { data } = await pmProjectsApi.getDashboard({
      page: currentPage.value - 1,
      size: pageSize,
    })
    projects.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch {
    ElMessage.error('載入專案資料失敗')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProjects()
})
</script>

<style scoped>
.dashboard-page {
  padding: 20px;
}

.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.project-name {
  font-size: 16px;
  font-weight: bold;
}

.budget-section {
  margin-bottom: 8px;
}

.budget-label {
  font-size: 14px;
  margin-bottom: 8px;
  color: #606266;
}

.budget-detail {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.summary-title {
  font-size: 14px;
  margin-bottom: 8px;
  color: #606266;
}

.summary-stats {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
