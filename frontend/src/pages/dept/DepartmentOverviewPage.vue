<template>
  <div class="dept-overview">
    <div v-loading="loading">
      <template v-if="overview">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
          <h2 style="margin: 0">{{ overview.deptName }} — 部門工時總覽</h2>
        </div>

        <el-row :gutter="16" style="margin-bottom: 24px">
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="部門人數" :value="overview.memberCount" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="本週總工時" :value="overview.totalHoursThisWeek" :precision="1" suffix="小時" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card shadow="hover">
              <el-statistic title="本月總工時" :value="overview.totalHoursThisMonth" :precision="1" suffix="小時" />
            </el-card>
          </el-col>
          <el-col :span="8" style="margin-top: 16px">
            <el-card shadow="hover">
              <el-statistic
                title="人均本月工時"
                :value="overview.memberCount > 0 ? (overview.totalHoursThisMonth / overview.memberCount) : 0"
                :precision="1"
                suffix="小時"
              />
            </el-card>
          </el-col>
        </el-row>

        <el-table :data="overview.members" border stripe row-key="userId">
          <el-table-column type="expand" width="48">
            <template #default="{ row }">
              <MemberTaskDetail :tasks="memberTasks[row.userId] || []" :loading="detailLoading[row.userId] || false" />
            </template>
          </el-table-column>
          <el-table-column prop="name" label="成員姓名" min-width="150" />
          <el-table-column label="本週工時" min-width="120" align="right">
            <template #default="{ row }">
              {{ row.totalHoursThisWeek.toFixed(1) }} 小時
            </template>
          </el-table-column>
          <el-table-column label="本月工時" min-width="120" align="right">
            <template #default="{ row }">
              {{ row.totalHoursThisMonth.toFixed(1) }} 小時
            </template>
          </el-table-column>
          <el-table-column label="今日工時" min-width="120" align="right">
            <template #default="{ row }">
              <el-tag :type="row.todayHours > 0 ? 'success' : 'info'" size="small">
                {{ row.todayHours.toFixed(1) }} 小時
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else-if="!loading" description="無法載入部門資料" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { deptApi, type DeptOverview, type MemberTask } from '@/api/dept'
import MemberTaskDetail from '@/components/department/MemberTaskDetail.vue'

const loading = ref(false)
const overview = ref<DeptOverview | null>(null)
const memberTasks = ref<Record<number, MemberTask[]>>({})
const detailLoading = ref<Record<number, boolean>>({})

const fetchOverview = async () => {
  loading.value = true
  try {
    const res = await deptApi.getOverview()
    overview.value = res.data
  } catch {
    ElMessage.error('載入部門總覽失敗')
  } finally {
    loading.value = false
  }
}

const fetchMemberTasks = async (userId: number) => {
  if (memberTasks.value[userId]) {
    return
  }
  detailLoading.value[userId] = true
  try {
    const res = await deptApi.getMemberTasks(userId)
    memberTasks.value[userId] = res.data
  } catch {
    ElMessage.error('載入成員 task 詳情失敗')
  } finally {
    detailLoading.value[userId] = false
  }
}

onMounted(async () => {
  await fetchOverview()
  if (overview.value) {
    await Promise.all(overview.value.members.map((m) => fetchMemberTasks(m.userId)))
  }
})
</script>
