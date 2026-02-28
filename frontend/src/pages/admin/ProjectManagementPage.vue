<template>
  <div class="project-management-page">
    <div class="page-header">
      <h2>專案管理</h2>
      <el-button
        type="primary"
        @click="openCreateDialog"
      >
        新增專案
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="statusFilter"
        placeholder="篩選狀態"
        clearable
        style="width: 200px"
        @change="fetchProjects"
      >
        <el-option
          label="ACTIVE"
          value="ACTIVE"
        />
        <el-option
          label="CLOSED"
          value="CLOSED"
        />
      </el-select>
    </div>

    <el-table
      v-loading="loading"
      :data="projects"
      stripe
      style="width: 100%"
    >
      <el-table-column
        prop="name"
        label="專案名稱"
        min-width="180"
      />
      <el-table-column
        label="狀態"
        width="100"
      >
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="totalBudgetHours"
        label="預算時數"
        width="120"
      />
      <el-table-column
        prop="consumedHours"
        label="已用時數"
        width="120"
      />
      <el-table-column
        prop="pmName"
        label="專案經理"
        width="140"
      />
      <el-table-column
        label="操作"
        width="260"
        fixed="right"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            :disabled="row.status === 'CLOSED'"
            @click="openEditDialog(row)"
          >
            編輯
          </el-button>
          <el-button
            size="small"
            type="warning"
            :disabled="row.status === 'CLOSED'"
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
      v-if="totalElements > 0"
      class="pagination-bar"
    >
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
        layout="total, prev, pager, next"
        @current-change="fetchProjects"
      />
    </div>

    <el-empty
      v-if="!loading && projects.length === 0"
      description="目前沒有專案"
    />

    <!-- Create/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingProject ? '編輯專案' : '新增專案'"
      width="500px"
      destroy-on-close
    >
      <ProjectForm
        :initial-data="editingProject ? {
          name: editingProject.name,
          totalBudgetHours: editingProject.totalBudgetHours,
          pmId: editingProject.pmId,
          departmentId: editingProject.departmentId ?? undefined,
        } : undefined"
        @submit="handleFormSubmit"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Project } from '@/types'
import { projectsApi } from '@/api/projects'
import ProjectForm from '@/components/project/ProjectForm.vue'

const loading = ref(false)
const projects = ref<Project[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const statusFilter = ref<string>('')

const dialogVisible = ref(false)
const editingProject = ref<Project | null>(null)

const fetchProjects = async () => {
  loading.value = true
  try {
    const { data } = await projectsApi.getProjects({
      page: currentPage.value - 1,
      size: pageSize,
    })
    projects.value = data.content
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error('載入專案列表失敗')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  editingProject.value = null
  dialogVisible.value = true
}

const openEditDialog = (project: Project) => {
  editingProject.value = project
  dialogVisible.value = true
}

const handleFormSubmit = async (formData: { name: string; totalBudgetHours: number; pmId: number; departmentId: number }) => {
  try {
    if (editingProject.value) {
      await projectsApi.updateProject(editingProject.value.id, formData)
      ElMessage.success('專案已更新')
    } else {
      await projectsApi.createProject(formData)
      ElMessage.success('專案已建立')
    }
    dialogVisible.value = false
    fetchProjects()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '操作失敗')
  }
}

const handleClose = async (project: Project) => {
  try {
    await ElMessageBox.confirm(`確定要關閉專案「${project.name}」嗎？`, '確認關閉', {
      confirmButtonText: '確定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await projectsApi.closeProject(project.id)
    ElMessage.success('專案已關閉')
    fetchProjects()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '關閉失敗')
    }
  }
}

const handleDelete = async (project: Project) => {
  try {
    await ElMessageBox.confirm(`確定要刪除專案「${project.name}」嗎？此操作無法復原。`, '確認刪除', {
      confirmButtonText: '確定',
      cancelButtonText: '取消',
      type: 'error',
    })
    await projectsApi.deleteProject(project.id)
    ElMessage.success('專案已刪除')
    fetchProjects()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '刪除失敗')
    }
  }
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.project-management-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.filter-bar {
  margin-bottom: 16px;
}
.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
