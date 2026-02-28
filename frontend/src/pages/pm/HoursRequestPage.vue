<template>
  <div class="hours-request-page">
    <h2>時數增補申請</h2>

    <!-- Create Request Form -->
    <el-card
      class="form-card"
      shadow="never"
    >
      <template #header>
        <span>新增申請</span>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item
          label="專案"
          prop="projectId"
        >
          <el-select
            v-model="form.projectId"
            placeholder="選擇專案"
            style="width: 100%"
            @change="onProjectChange"
          >
            <el-option
              v-for="p in projects"
              :key="p.id"
              :label="p.name"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="增補類型"
          prop="targetType"
        >
          <el-select
            v-model="form.targetType"
            placeholder="選擇類型"
            style="width: 100%"
          >
            <el-option
              label="專案"
              value="PROJECT"
            />
            <el-option
              label="任務"
              value="TASK"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          v-if="form.targetType === 'TASK'"
          label="目標任務"
          prop="targetTaskId"
        >
          <el-select
            v-model="form.targetTaskId"
            placeholder="選擇任務"
            style="width: 100%"
          >
            <el-option
              v-for="t in projectTasks"
              :key="t.id"
              :label="t.name"
              :value="t.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="申請時數"
          prop="requestedHours"
        >
          <el-input-number
            v-model="form.requestedHours"
            :min="0.5"
            :step="0.5"
            :precision="1"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item
          label="說明"
          prop="description"
        >
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="請說明增補原因"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            提交申請
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Request History -->
    <el-card
      class="history-card"
      shadow="never"
      style="margin-top: 20px"
    >
      <template #header>
        <span>申請紀錄</span>
      </template>
      <el-table
        v-loading="loading"
        :data="requests"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column
          prop="projectName"
          label="專案"
          min-width="150"
        />
        <el-table-column
          prop="targetType"
          label="類型"
          width="80"
        >
          <template #default="{ row }">
            {{ row.targetType === 'TASK' ? '任務' : '專案' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="targetTaskName"
          label="目標任務"
          width="150"
        >
          <template #default="{ row }">
            {{ row.targetTaskName ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="requestedHours"
          label="申請時數"
          width="100"
          align="right"
        />
        <el-table-column
          prop="description"
          label="說明"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="狀態"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="requestStatusType(row.status)"
              size="small"
            >
              {{ requestStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="reviewComment"
          label="審核意見"
          min-width="150"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.reviewComment ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="createdAt"
          label="申請時間"
          width="180"
        />
      </el-table>

      <el-empty
        v-if="!loading && requests.length === 0"
        description="尚無申請紀錄"
      />

      <div
        v-if="totalPages > 1"
        class="pagination-wrapper"
      >
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalElements"
          layout="prev, pager, next"
          @current-change="loadRequests"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { hoursRequestApi } from '@/api/hours-requests'
import { pmProjectsApi } from '@/api/pm-projects'
import { tasksApi } from '@/api/tasks'
import type { HoursRequest, HoursRequestStatus, ProjectDashboard, Task } from '@/types'

const formRef = ref<FormInstance>()
const submitting = ref(false)
const loading = ref(false)

const projects = ref<ProjectDashboard[]>([])
const projectTasks = ref<Task[]>([])
const requests = ref<HoursRequest[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const totalPages = ref(0)

const form = reactive({
  projectId: undefined as number | undefined,
  targetType: 'PROJECT' as 'PROJECT' | 'TASK',
  targetTaskId: undefined as number | undefined,
  requestedHours: 8,
  description: '',
})

const rules: FormRules = {
  projectId: [{ required: true, message: '請選擇專案', trigger: 'change' }],
  targetType: [{ required: true, message: '請選擇類型', trigger: 'change' }],
  requestedHours: [{ required: true, message: '請輸入申請時數', trigger: 'blur' }],
  description: [{ required: true, message: '請輸入說明', trigger: 'blur' }],
}

function requestStatusType(status: HoursRequestStatus) {
  const map: Record<HoursRequestStatus, string> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return map[status] ?? 'info'
}

function requestStatusLabel(status: HoursRequestStatus) {
  const map: Record<HoursRequestStatus, string> = {
    PENDING: '審核中',
    APPROVED: '已核准',
    REJECTED: '已拒絕',
  }
  return map[status] ?? status
}

async function onProjectChange(projectId: number) {
  form.targetTaskId = undefined
  if (projectId) {
    try {
      const { data } = await tasksApi.getTasks(projectId, { size: 100 })
      projectTasks.value = data.content
    } catch {
      projectTasks.value = []
    }
  } else {
    projectTasks.value = []
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.targetType === 'TASK' && !form.targetTaskId) {
    ElMessage.warning('請選擇目標任務')
    return
  }

  submitting.value = true
  try {
    await hoursRequestApi.createRequest({
      projectId: form.projectId!,
      requestedHours: form.requestedHours,
      description: form.description,
      targetType: form.targetType,
      targetTaskId: form.targetType === 'TASK' ? form.targetTaskId : undefined,
    })
    ElMessage.success('申請已提交')
    formRef.value?.resetFields()
    form.targetTaskId = undefined
    projectTasks.value = []
    loadRequests()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message ?? '提交失敗')
  } finally {
    submitting.value = false
  }
}

async function loadProjects() {
  try {
    const { data } = await pmProjectsApi.getDashboard({ size: 100 })
    projects.value = data.content
  } catch {
    projects.value = []
  }
}

async function loadRequests() {
  loading.value = true
  try {
    const { data } = await hoursRequestApi.getRequests({
      page: currentPage.value - 1,
      size: pageSize,
    })
    requests.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch {
    ElMessage.error('載入申請紀錄失敗')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProjects()
  loadRequests()
})
</script>

<style scoped>
.hours-request-page {
  padding: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}
</style>
