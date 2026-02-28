<template>
  <div class="task-management-page">
    <div class="page-header">
      <h2>任務管理</h2>
      <el-button
        type="primary"
        @click="openCreateDialog"
      >
        新增任務
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="tasks"
      stripe
      border
      style="width: 100%"
    >
      <el-table-column
        prop="name"
        label="任務名稱"
        min-width="180"
      />
      <el-table-column
        prop="status"
        label="狀態"
        width="120"
      >
        <template #default="{ row }">
          <el-tag
            :type="statusTagType(row.status)"
            size="small"
          >
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="budgetHours"
        label="配額時數"
        width="100"
        align="right"
      />
      <el-table-column
        prop="consumedHours"
        label="已用時數"
        width="100"
        align="right"
      />
      <el-table-column
        prop="remainingHours"
        label="剩餘時數"
        width="100"
        align="right"
      />
      <el-table-column
        prop="assigneeName"
        label="指派人員"
        width="120"
      >
        <template #default="{ row }">
          {{ row.assigneeName ?? '未指派' }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作"
        width="220"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            :disabled="isTerminal(row.status)"
            @click="openEditDialog(row)"
          >
            編輯
          </el-button>
          <el-button
            size="small"
            type="warning"
            :disabled="isTerminal(row.status)"
            @click="handleClose(row)"
          >
            關閉
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(row)"
          >
            刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div
      v-if="totalPages > 1"
      class="pagination-wrapper"
    >
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
        layout="prev, pager, next"
        @current-change="loadTasks"
      />
    </div>

    <el-empty
      v-if="!loading && tasks.length === 0"
      description="尚無任務"
    />

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingTask ? '編輯任務' : '新增任務'"
      width="500px"
      destroy-on-close
    >
      <TaskForm
        :initial-data="editingTask ? { name: editingTask.name, budgetHours: editingTask.budgetHours, assigneeId: editingTask.assigneeId } : undefined"
        :project-id="projectId"
        @submit="handleFormSubmit"
        @cancel="dialogVisible = false"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { tasksApi } from '@/api/tasks'
import TaskForm from '@/components/task/TaskForm.vue'
import type { Task, TaskStatus } from '@/types'

const route = useRoute()
const projectId = Number(route.params.projectId)

const loading = ref(false)
const tasks = ref<Task[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const totalPages = ref(0)

const dialogVisible = ref(false)
const editingTask = ref<Task | null>(null)

function statusTagType(status: TaskStatus) {
  const map: Record<TaskStatus, string> = {
    PENDING: 'info',
    IN_PROGRESS: 'primary',
    COMPLETED: 'success',
    CLOSED: 'danger',
  }
  return map[status] ?? 'info'
}

function statusLabel(status: TaskStatus) {
  const map: Record<TaskStatus, string> = {
    PENDING: '待處理',
    IN_PROGRESS: '進行中',
    COMPLETED: '已完成',
    CLOSED: '已關閉',
  }
  return map[status] ?? status
}

function isTerminal(status: TaskStatus) {
  return status === 'COMPLETED' || status === 'CLOSED'
}

function openCreateDialog() {
  editingTask.value = null
  dialogVisible.value = true
}

function openEditDialog(task: Task) {
  editingTask.value = task
  dialogVisible.value = true
}

async function handleFormSubmit(formData: { name: string; budgetHours: number; assigneeId?: number }) {
  try {
    if (editingTask.value) {
      await tasksApi.updateTask(projectId, editingTask.value.id, formData)
      ElMessage.success('任務已更新')
    } else {
      await tasksApi.createTask(projectId, formData)
      ElMessage.success('任務已建立')
    }
    dialogVisible.value = false
    loadTasks()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message ?? '操作失敗')
  }
}

async function handleClose(task: Task) {
  try {
    await ElMessageBox.confirm('確定要強制關閉此任務？此操作無法還原。', '確認關閉', {
      type: 'warning',
    })
    await tasksApi.closeTask(projectId, task.id)
    ElMessage.success('任務已關閉')
    loadTasks()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message ?? '關閉失敗')
    }
  }
}

async function handleDelete(task: Task) {
  try {
    await ElMessageBox.confirm('確定要刪除此任務？', '確認刪除', {
      type: 'warning',
    })
    await tasksApi.deleteTask(projectId, task.id)
    ElMessage.success('任務已刪除')
    loadTasks()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message ?? '刪除失敗')
    }
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const { data } = await tasksApi.getTasks(projectId, {
      page: currentPage.value - 1,
      size: pageSize,
    })
    tasks.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch {
    ElMessage.error('載入任務列表失敗')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTasks()
})
</script>

<style scoped>
.task-management-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
