<template>
  <div class="user-management">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2 style="margin: 0">
        使用者管理
      </h2>
      <el-button
        type="primary"
        @click="openCreateDialog"
      >
        新增使用者
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="users"
      border
      stripe
    >
      <el-table-column
        prop="email"
        label="Email"
        min-width="180"
      />
      <el-table-column
        prop="name"
        label="姓名"
        min-width="120"
      />
      <el-table-column
        prop="departmentName"
        label="部門"
        min-width="120"
      />
      <el-table-column
        label="角色"
        min-width="200"
      >
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role"
            size="small"
            style="margin-right: 4px"
          >
            {{ roleLabel(role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="狀態"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <el-tag
            :type="row.active ? 'success' : 'danger'"
            size="small"
          >
            {{ row.active ? '啟用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="目前沒有使用者" />
      </template>
      <el-table-column
        label="操作"
        width="260"
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
            :type="row.active ? 'warning' : 'success'"
            @click="toggleActive(row)"
          >
            {{ row.active ? '停用' : '啟用' }}
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleResetPassword(row)"
          >
            重設密碼
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      style="margin-top: 16px; justify-content: flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="handlePageChange"
    />

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '編輯使用者' : '新增使用者'"
      width="520px"
      destroy-on-close
    >
      <UserForm
        ref="userFormRef"
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

    <!-- Temp Password Dialog -->
    <el-dialog
      v-model="tempPasswordVisible"
      title="臨時密碼"
      width="420px"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      >
        請將此臨時密碼提供給使用者，使用者首次登入後須變更密碼。
      </el-alert>
      <el-input
        :model-value="tempPassword"
        readonly
      >
        <template #append>
          <el-button @click="copyPassword">
            複製
          </el-button>
        </template>
      </el-input>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { User, Role } from '@/types'
import { usersApi } from '@/api/users'
import UserForm from '@/components/user/UserForm.vue'

const loading = ref(false)
const users = ref<User[]>([])
const total = ref(0)
const pageSize = 20
const currentPage = ref(1)

const dialogVisible = ref(false)
const tempPasswordVisible = ref(false)
const tempPassword = ref('')
const isEdit = ref(false)
const editingUserId = ref<number | null>(null)
const editData = ref<{ email?: string; name?: string; departmentId?: number; roles?: Role[] }>()
const submitting = ref(false)
const userFormRef = ref<InstanceType<typeof UserForm>>()

const roleLabel = (role: Role) => {
  const map: Record<Role, string> = {
    ADMIN: '管理員',
    PM: '專案經理',
    DEPT_MANAGER: '部門主管',
    EXECUTOR: '執行人員',
    HR: '人資',
  }
  return map[role] || role
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await usersApi.fetchUsers({ page: currentPage.value - 1, size: pageSize })
    users.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('載入使用者失敗')
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchUsers()
}

const openCreateDialog = () => {
  isEdit.value = false
  editingUserId.value = null
  editData.value = undefined
  dialogVisible.value = true
}

const openEditDialog = (user: User) => {
  isEdit.value = true
  editingUserId.value = user.id
  editData.value = {
    email: user.email,
    name: user.name,
    departmentId: user.departmentId,
    roles: user.roles,
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!userFormRef.value) return
  const valid = await userFormRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const data = userFormRef.value.getData()
    if (isEdit.value && editingUserId.value) {
      await usersApi.updateUser(editingUserId.value, {
        name: data.name,
        departmentId: data.departmentId,
        roles: data.roles,
      })
      ElMessage.success('使用者已更新')
    } else {
      const res = await usersApi.createUser({
        name: data.name,
        email: data.email,
        departmentId: data.departmentId,
        roles: data.roles,
      })
      tempPassword.value = res.data.temporaryPassword
      tempPasswordVisible.value = true
      ElMessage.success('使用者已建立')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '操作失敗')
  } finally {
    submitting.value = false
  }
}

const toggleActive = async (user: User) => {
  const action = user.active ? '停用' : '啟用'
  try {
    await ElMessageBox.confirm(`確定要${action}使用者「${user.name}」嗎？`, '確認', {
      type: 'warning',
    })
    if (user.active) {
      await usersApi.disableUser(user.id)
    } else {
      await usersApi.enableUser(user.id)
    }
    ElMessage.success(`使用者已${action}`)
    fetchUsers()
  } catch {
    // cancelled
  }
}

const handleResetPassword = async (user: User) => {
  try {
    await ElMessageBox.confirm(`確定要重設「${user.name}」的密碼嗎？`, '確認重設密碼', {
      type: 'warning',
    })
    const res = await usersApi.resetPassword(user.id)
    tempPassword.value = res.data.temporaryPassword
    tempPasswordVisible.value = true
  } catch {
    // cancelled
  }
}

const copyPassword = async () => {
  const text = tempPassword.value
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      throw new Error('Clipboard API not available')
    }
    ElMessage.success('已複製到剪貼簿')
  } catch {
    // Fallback for non-secure context (e.g. HTTP): use execCommand
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.setAttribute('readonly', '')
    document.body.appendChild(textarea)
    textarea.select()
    try {
      const ok = document.execCommand('copy')
      if (ok) {
        ElMessage.success('已複製到剪貼簿')
      } else {
        ElMessage.error('複製失敗')
      }
    } catch {
      ElMessage.error('複製失敗')
    } finally {
      document.body.removeChild(textarea)
    }
  }
}

onMounted(fetchUsers)
</script>
