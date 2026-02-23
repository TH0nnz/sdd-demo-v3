<template>
  <div class="hours-review-page">
    <div class="page-header">
      <h2>時數審核</h2>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="statusFilter"
        placeholder="篩選狀態"
        clearable
        style="width: 200px"
        @change="fetchRequests"
      >
        <el-option label="全部" value="" />
        <el-option label="PENDING" value="PENDING" />
        <el-option label="APPROVED" value="APPROVED" />
        <el-option label="REJECTED" value="REJECTED" />
      </el-select>
    </div>

    <el-table v-loading="loading" :data="filteredRequests" stripe style="width: 100%">
      <el-table-column prop="projectName" label="專案" min-width="160" />
      <el-table-column label="目標類型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.targetType === 'PROJECT' ? 'primary' : 'warning'" size="small">
            {{ row.targetType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="targetTaskName" label="目標 Task" width="140">
        <template #default="{ row }">
          {{ row.targetTaskName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="requestedHours" label="申請時數" width="100" />
      <el-table-column prop="description" label="說明" min-width="180" show-overflow-tooltip />
      <el-table-column label="狀態" width="100">
        <template #default="{ row }">
          <el-tag
            :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'"
          >
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="requesterName" label="申請人" width="120" />
      <el-table-column label="申請時間" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button size="small" type="success" @click="handleApprove(row)">
              核准
            </el-button>
            <el-button size="small" type="danger" @click="handleReject(row)">
              駁回
            </el-button>
          </template>
          <span v-else class="review-info">
            {{ row.reviewerName || '-' }}
          </span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar" v-if="totalElements > 0">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
        layout="total, prev, pager, next"
        @current-change="fetchRequests"
      />
    </div>

    <el-empty v-if="!loading && requests.length === 0" description="目前沒有時數增補申請" />

    <!-- Reject Note Dialog -->
    <el-dialog v-model="rejectDialogVisible" title="駁回原因" width="400px">
      <el-input
        v-model="rejectNote"
        type="textarea"
        :rows="3"
        placeholder="請輸入駁回原因"
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject" :loading="submitting">
          確定駁回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { HoursRequest } from '@/types'
import { adminHoursRequestApi } from '@/api/admin-hours-requests'

const loading = ref(false)
const submitting = ref(false)
const requests = ref<HoursRequest[]>([])
const currentPage = ref(1)
const pageSize = 20
const totalElements = ref(0)
const statusFilter = ref('')

const rejectDialogVisible = ref(false)
const rejectNote = ref('')
const rejectingRequestId = ref<number | null>(null)

const filteredRequests = computed(() => {
  if (!statusFilter.value) return requests.value
  return requests.value.filter((r) => r.status === statusFilter.value)
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-TW')
}

const fetchRequests = async () => {
  loading.value = true
  try {
    const { data } = await adminHoursRequestApi.getAllRequests({
      page: currentPage.value - 1,
      size: pageSize,
    })
    requests.value = data.content
    totalElements.value = data.totalElements
  } catch {
    ElMessage.error('載入時數申請列表失敗')
  } finally {
    loading.value = false
  }
}

const handleApprove = async (request: HoursRequest) => {
  try {
    await ElMessageBox.confirm(
      `確定核准此時數增補申請（${request.requestedHours} 小時）？`,
      '確認核准',
      {
        confirmButtonText: '核准',
        cancelButtonText: '取消',
        type: 'success',
      },
    )
    submitting.value = true
    await adminHoursRequestApi.reviewRequest(request.id, {
      decision: 'APPROVED',
      reviewNote: '核准',
    })
    ElMessage.success('已核准')
    fetchRequests()
  } catch (err: any) {
    if (err !== 'cancel') {
      ElMessage.error(err.response?.data?.message || '核准失敗')
    }
  } finally {
    submitting.value = false
  }
}

const handleReject = (request: HoursRequest) => {
  rejectingRequestId.value = request.id
  rejectNote.value = ''
  rejectDialogVisible.value = true
}

const confirmReject = async () => {
  if (rejectingRequestId.value === null) return
  submitting.value = true
  try {
    await adminHoursRequestApi.reviewRequest(rejectingRequestId.value, {
      decision: 'REJECTED',
      reviewNote: rejectNote.value || undefined,
    })
    ElMessage.success('已駁回')
    rejectDialogVisible.value = false
    fetchRequests()
  } catch (err: any) {
    ElMessage.error(err.response?.data?.message || '駁回失敗')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchRequests()
})
</script>

<style scoped>
.hours-review-page {
  padding: 20px;
}
.page-header {
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
.review-info {
  color: #909399;
  font-size: 13px;
}
</style>
