<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="100px"
  >
    <el-form-item
      label="任務名稱"
      prop="name"
    >
      <el-input
        v-model="form.name"
        placeholder="請輸入任務名稱"
      />
    </el-form-item>
    <el-form-item
      label="配額時數"
      prop="budgetHours"
    >
      <el-input-number
        v-model="form.budgetHours"
        :min="0.5"
        :step="0.5"
        :precision="1"
        controls-position="right"
      />
    </el-form-item>
    <el-form-item
      label="指派人員"
      prop="assigneeId"
    >
      <div style="margin-bottom: 8px; color: #666; font-size: 12px;">
        除錯：找到 {{ executors.length }} 位執行人員
      </div>
      <el-select
        v-model="form.assigneeId"
        placeholder="選擇執行人員"
        clearable
        style="width: 100%"
      >
        <el-option
          v-for="user in executors"
          :key="user.id"
          :label="user.name"
          :value="user.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button
        type="primary"
        :loading="loading"
        @click="handleSubmit"
      >
        {{ initialData ? '更新' : '建立' }}
      </el-button>
      <el-button @click="$emit('cancel')">
        取消
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import http from '@/api/http'
import type { User } from '@/types'

interface TaskFormData {
  name: string
  budgetHours: number
  assigneeId: number | undefined
}

const props = defineProps<{
  initialData?: { name: string; budgetHours: number; assigneeId?: number | null }
  projectId: number
}>()

const emit = defineEmits<{
  submit: [data: { name: string; budgetHours: number; assigneeId?: number }]
  cancel: []
}>()

const formRef = ref<FormInstance>()
const loading = ref(false)
const executors = ref<User[]>([])

const form = reactive<TaskFormData>({
  name: props.initialData?.name ?? '',
  budgetHours: props.initialData?.budgetHours ?? 8,
  assigneeId: props.initialData?.assigneeId ?? undefined,
})

const rules: FormRules = {
  name: [{ required: true, message: '請輸入任務名稱', trigger: 'blur' }],
  budgetHours: [{ required: true, message: '請輸入配額時數', trigger: 'blur' }],
}

async function loadExecutors() {
  try {
    console.log('Loading executors for project:', props.projectId)
    const { data } = await http.get<User[]>(`/projects/${props.projectId}/tasks/assignable-executors`)
    console.log('Executors loaded:', data)
    executors.value = data
  } catch (error) {
    console.error('Failed to load executors:', error)
    executors.value = []
  }
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (!valid) return
    loading.value = true
    const submitData: { name: string; budgetHours: number; assigneeId?: number } = {
      name: form.name,
      budgetHours: form.budgetHours,
    }
    if (form.assigneeId) {
      submitData.assigneeId = form.assigneeId
    }
    emit('submit', submitData)
    loading.value = false
  })
}

onMounted(() => {
  loadExecutors()
})
</script>
