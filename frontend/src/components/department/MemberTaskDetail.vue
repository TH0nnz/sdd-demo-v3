<template>
  <el-table :data="tasks" border stripe v-loading="loading" size="small" style="width: 100%">
    <template #empty>
      <el-empty description="此成員目前無進行中或已完成 task" />
    </template>

    <el-table-column prop="taskName" label="Task 名稱" min-width="180" />
    <el-table-column prop="projectName" label="所屬專案" min-width="160" />
    <el-table-column label="狀態" min-width="120">
      <template #default="{ row }">
        <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="已消耗時數" min-width="120" align="right">
      <template #default="{ row }">{{ Number(row.consumedHours).toFixed(1) }} 小時</template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import type { MemberTask } from '@/api/dept'

defineProps<{
  tasks: MemberTask[]
  loading: boolean
}>()

const statusType = (status: MemberTask['status']) => {
  switch (status) {
    case 'IN_PROGRESS':
      return 'warning'
    case 'COMPLETED':
      return 'success'
    case 'CLOSED':
      return 'info'
    default:
      return ''
  }
}
</script>
