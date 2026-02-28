<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Task, TaskStatus } from '@/types'
import { myTasksApi } from '@/api/my-tasks'

const tasks = ref<Task[]>([])
const loading = ref(false)
const statusFilter = ref<string>('')

const statusOptions: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '待指派', value: 'PENDING' },
  { label: '進行中', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已關閉', value: 'CLOSED' },
]

function statusTagType(status: TaskStatus) {
  const map: Record<TaskStatus, string> = {
    PENDING: 'info',
    IN_PROGRESS: '',
    COMPLETED: 'success',
    CLOSED: 'danger',
  }
  return map[status] ?? 'info'
}

function statusLabel(status: TaskStatus) {
  const map: Record<TaskStatus, string> = {
    PENDING: '待指派',
    IN_PROGRESS: '進行中',
    COMPLETED: '已完成',
    CLOSED: '已關閉',
  }
  return map[status] ?? status
}

async function loadTasks() {
  loading.value = true
  try {
    const params = statusFilter.value ? { status: statusFilter.value } : undefined
    const { data } = await myTasksApi.getMyTasks(params)
    tasks.value = data.content
  } catch {
    ElMessage.error('載入任務失敗')
  } finally {
    loading.value = false
  }
}

async function handleComplete(task: Task) {
  try {
    await ElMessageBox.confirm(`確定要將任務「${task.name}」標記為完成嗎？`, '確認完成', {
      confirmButtonText: '確定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await myTasksApi.completeTask(task.id)
    ElMessage.success('任務已完成')
    await loadTasks()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失敗')
    }
  }
}

function handleFilterChange() {
  loadTasks()
}

onMounted(loadTasks)
</script>

<template>
  <div>
    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px">
      <h2 style="margin: 0">
        我的任務
      </h2>
      <el-select
        v-model="statusFilter"
        placeholder="篩選狀態"
        style="width: 160px"
        @change="handleFilterChange"
      >
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </div>

    <el-table
      v-loading="loading"
      :data="tasks"
      stripe
    >
      <el-table-column
        prop="name"
        label="任務名稱"
        min-width="160"
      />
      <el-table-column
        prop="projectName"
        label="專案"
        min-width="140"
      />
      <el-table-column
        label="狀態"
        width="100"
        align="center"
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
        label="預算工時"
        width="100"
        align="center"
      />
      <el-table-column
        prop="consumedHours"
        label="已用工時"
        width="100"
        align="center"
      />
      <el-table-column
        prop="remainingHours"
        label="剩餘工時"
        width="100"
        align="center"
      />
      <el-table-column
        label="操作"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'IN_PROGRESS'"
            type="success"
            size="small"
            @click="handleComplete(row)"
          >
            完成
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="目前沒有任務" />
      </template>
    </el-table>
  </div>
</template>
