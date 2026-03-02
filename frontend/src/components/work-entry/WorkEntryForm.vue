<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { Task, CreateWorkEntryRequest } from '@/types'
import { myTasksApi } from '@/api/my-tasks'
import { workEntryApi } from '@/api/work-entries'

const emit = defineEmits<{ created: [] }>()

const tasks = ref<Task[]>([])
const loadingTasks = ref(false)
const submitting = ref(false)

const form = ref<CreateWorkEntryRequest>({
  taskId: 0,
  workDate: '',
  hours: 1,
})

const formRef = ref()

const rules = {
  taskId: [{ required: true, message: '請選擇任務', trigger: 'change' }],
  workDate: [{ required: true, message: '請選擇日期', trigger: 'change' }],
  hours: [{ required: true, message: '請輸入工時', trigger: 'blur' }],
}

function getRecentWorkDays(): Date[] {
  const days: Date[] = []
  const today = new Date()
  let d = new Date(today)
  while (days.length < 3) {
    const dow = d.getDay()
    if (dow !== 0 && dow !== 6) {
      days.push(new Date(d))
    }
    d.setDate(d.getDate() - 1)
  }
  return days
}

const recentWorkDays = getRecentWorkDays()
const minDate = recentWorkDays[recentWorkDays.length - 1]

function disabledDate(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  if (date > today) return true
  if (date < minDate) return true
  const dow = date.getDay()
  return dow === 0 || dow === 6
}

async function loadTasks() {
  loadingTasks.value = true
  try {
    const { data } = await myTasksApi.getMyTasks()
    tasks.value = data.content.filter(t => t.status !== 'COMPLETED' && t.status !== 'CLOSED')
  } catch {
    ElMessage.error('載入任務清單失敗')
  } finally {
    loadingTasks.value = false
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const { data } = await workEntryApi.createWorkEntry(form.value)
    ElMessage.success('工時建立成功')
    if (data.warning) {
      ElMessage.warning(data.warning)
    }
    form.value = { taskId: 0, workDate: '', hours: 1 }
    formRef.value?.resetFields()
    emit('created')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '建立工時失敗')
  } finally {
    submitting.value = false
  }
}

onMounted(loadTasks)
</script>

<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="100px"
    @submit.prevent="handleSubmit"
  >
    <el-form-item
      label="任務"
      prop="taskId"
    >
      <el-select
        v-model="form.taskId"
        filterable
        placeholder="請選擇任務"
        :loading="loadingTasks"
        style="width: 100%"
      >
        <el-option
          v-for="task in tasks"
          :key="task.id"
          :label="`${task.projectName} - ${task.name}`"
          :value="task.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item
      label="日期"
      prop="workDate"
    >
      <el-date-picker
        v-model="form.workDate"
        type="date"
        placeholder="請選擇日期"
        value-format="YYYY-MM-DD"
        :disabled-date="disabledDate"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item
      label="工時（小時）"
      prop="hours"
    >
      <el-input-number
        v-model="form.hours"
        :step="0.5"
        :min="0.5"
        :max="24"
      />
    </el-form-item>
    <el-form-item>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        送出
      </el-button>
    </el-form-item>
  </el-form>
</template>
