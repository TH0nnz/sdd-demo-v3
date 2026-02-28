<template>
  <div class="department-management">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2 style="margin: 0">
        部門管理
      </h2>
      <el-button
        type="primary"
        @click="openCreateDialog"
      >
        新增部門
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="departments"
      border
      stripe
    >
      <el-table-column
        prop="name"
        label="部門名稱"
        min-width="200"
      />
      <el-table-column
        label="操作"
        width="180"
        align="center"
      >
        <template #default="{ row }">
          <el-button
            size="small"
            @click="openEditDialog(row)"
          >
            編輯
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
      <template #empty>
        <el-empty description="目前沒有部門" />
      </template>
    </el-table>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '編輯部門' : '新增部門'"
      width="420px"
      destroy-on-close
    >
      <DepartmentForm
        ref="deptFormRef"
        :initial-data="editData"
      />
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitting"
          @click="handleSubmit"
        >
          確認
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deptApi, type Department } from '@/api/dept'
import DepartmentForm from '@/components/department/DepartmentForm.vue'

const loading = ref(false)
const departments = ref<Department[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const editData = ref<{ name?: string }>()
const submitting = ref(false)
const deptFormRef = ref<InstanceType<typeof DepartmentForm>>()

const fetchDepartments = async () => {
  loading.value = true
  try {
    const res = await deptApi.listDepartmentsForHr()
    departments.value = res.data
  } catch {
    ElMessage.error('載入部門失敗')
  } finally {
    loading.value = false
  }
}

const openCreateDialog = () => {
  isEdit.value = false
  editingId.value = null
  editData.value = undefined
  dialogVisible.value = true
}

const openEditDialog = (dept: Department) => {
  isEdit.value = true
  editingId.value = dept.id
  editData.value = { name: dept.name }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!deptFormRef.value) return
  const valid = await deptFormRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const data = deptFormRef.value.getData()
    if (isEdit.value && editingId.value) {
      await deptApi.updateDepartment(editingId.value, data)
      ElMessage.success('部門已更新')
    } else {
      await deptApi.createDepartment(data)
      ElMessage.success('部門已建立')
    }
    dialogVisible.value = false
    fetchDepartments()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '操作失敗')
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (dept: Department) => {
  try {
    await ElMessageBox.confirm(`確定要刪除部門「${dept.name}」嗎？`, '確認刪除', {
      type: 'warning',
    })
    await deptApi.deleteDepartment(dept.id)
    ElMessage.success('部門已刪除')
    fetchDepartments()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '刪除失敗')
    }
  }
}

onMounted(fetchDepartments)
</script>
