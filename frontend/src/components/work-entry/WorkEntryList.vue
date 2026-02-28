<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit } from '@element-plus/icons-vue'
import type { WorkEntry } from '@/types'
import { workEntryApi } from '@/api/work-entries'

defineProps<{
  entries: WorkEntry[]
  loading: boolean
}>()

const emit = defineEmits<{ updated: [] }>()

const editDialogVisible = ref(false)
const editingEntry = ref<WorkEntry | null>(null)
const editHours = ref(0)
const saving = ref(false)

function openEditDialog(entry: WorkEntry) {
  editingEntry.value = entry
  editHours.value = entry.hours
  editDialogVisible.value = true
}

async function handleSaveEdit() {
  if (!editingEntry.value) return
  saving.value = true
  try {
    const { data } = await workEntryApi.updateWorkEntry(editingEntry.value.id, { hours: editHours.value })
    ElMessage.success('工時更新成功')
    if (data.warning) {
      ElMessage.warning(data.warning)
    }
    editDialogVisible.value = false
    emit('updated')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '更新工時失敗')
  } finally {
    saving.value = false
  }
}

function statusType(editable: boolean) {
  return editable ? 'success' : 'info'
}
</script>

<template>
  <el-table
    v-loading="loading"
    :data="entries"
    stripe
  >
    <el-table-column
      prop="taskName"
      label="任務"
      min-width="140"
    />
    <el-table-column
      prop="projectName"
      label="專案"
      min-width="140"
    />
    <el-table-column
      prop="workDate"
      label="工作日期"
      width="120"
    />
    <el-table-column
      prop="hours"
      label="工時"
      width="80"
      align="center"
    />
    <el-table-column
      label="狀態"
      width="100"
      align="center"
    >
      <template #default="{ row }">
        <el-tag
          :type="statusType(row.editable)"
          size="small"
        >
          {{ row.editable ? '可編輯' : '已鎖定' }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column
      label="操作"
      width="80"
      align="center"
    >
      <template #default="{ row }">
        <el-button
          v-if="row.editable"
          :icon="Edit"
          size="small"
          circle
          @click="openEditDialog(row)"
        />
      </template>
    </el-table-column>
    <template #empty>
      <el-empty description="尚無工時紀錄" />
    </template>
  </el-table>

  <el-dialog
    v-model="editDialogVisible"
    title="編輯工時"
    width="400px"
  >
    <p style="margin-bottom: 12px">
      任務：{{ editingEntry?.taskName }} ｜ 日期：{{ editingEntry?.workDate }}
    </p>
    <el-form-item label="工時（小時）">
      <el-input-number
        v-model="editHours"
        :step="0.5"
        :min="0.5"
        :max="24"
      />
    </el-form-item>
    <template #footer>
      <el-button @click="editDialogVisible = false">
        取消
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        @click="handleSaveEdit"
      >
        儲存
      </el-button>
    </template>
  </el-dialog>
</template>
