<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="120px"
    @submit.prevent
  >
    <el-form-item label="專案名稱" prop="name">
      <el-input v-model="form.name" placeholder="請輸入專案名稱" />
    </el-form-item>
    <el-form-item label="預算時數" prop="totalBudgetHours">
      <el-input-number
        v-model="form.totalBudgetHours"
        :min="0.1"
        :step="10"
        :precision="1"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="所屬部門" prop="departmentId">
      <el-select
        v-model="form.departmentId"
        placeholder="請選擇部門"
        style="width: 100%"
        :loading="loadingDepartments"
      >
        <el-option
          v-for="dept in departments"
          :key="dept.id"
          :label="dept.name"
          :value="dept.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="專案經理" prop="pmId">
      <el-select
        v-model="form.pmId"
        placeholder="請選擇 PM"
        style="width: 100%"
        :loading="loadingPms"
      >
        <el-option
          v-for="pm in pmUsers"
          :key="pm.id"
          :label="pm.name"
          :value="pm.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleSubmit">確認</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { User } from '@/types'
import type { Department } from '@/api/dept'
import { usersApi } from '@/api/users'
import { deptApi } from '@/api/dept'

interface ProjectFormData {
  name: string
  totalBudgetHours: number
  pmId: number | undefined
  departmentId: number | undefined
}

const props = defineProps<{
  initialData?: {
    name: string
    totalBudgetHours: number
    pmId: number
    departmentId?: number
  }
}>()

const emit = defineEmits<{
  submit: [data: { name: string; totalBudgetHours: number; pmId: number; departmentId: number }]
}>()

const formRef = ref<FormInstance>()
const pmUsers = ref<User[]>([])
const loadingPms = ref(false)
const departments = ref<Department[]>([])
const loadingDepartments = ref(false)

const form = reactive<ProjectFormData>({
  name: props.initialData?.name ?? '',
  totalBudgetHours: props.initialData?.totalBudgetHours ?? 100,
  pmId: props.initialData?.pmId ?? undefined,
  departmentId: props.initialData?.departmentId ?? undefined,
})

const rules: FormRules = {
  name: [{ required: true, message: '請輸入專案名稱', trigger: 'blur' }],
  totalBudgetHours: [{ required: true, message: '請輸入預算時數', trigger: 'blur' }],
  departmentId: [{ required: true, message: '請選擇所屬部門', trigger: 'change' }],
  pmId: [{ required: true, message: '請選擇專案經理', trigger: 'change' }],
}

const fetchPmUsers = async () => {
  loadingPms.value = true
  try {
    const { data } = await usersApi.fetchPmUsers()
    pmUsers.value = Array.isArray(data) ? data : []
  } catch {
    pmUsers.value = []
  } finally {
    loadingPms.value = false
  }
}

const fetchDepartments = async () => {
  loadingDepartments.value = true
  try {
    const { data } = await deptApi.listDepartments()
    departments.value = Array.isArray(data) ? data : []
  } catch {
    departments.value = []
  } finally {
    loadingDepartments.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid && form.pmId && form.departmentId) {
      emit('submit', {
        name: form.name,
        totalBudgetHours: form.totalBudgetHours,
        pmId: form.pmId,
        departmentId: form.departmentId,
      })
    }
  })
}

onMounted(() => {
  fetchPmUsers()
  fetchDepartments()
})
</script>
