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
import { usersApi } from '@/api/users'

interface ProjectFormData {
  name: string
  totalBudgetHours: number
  pmId: number | undefined
}

const props = defineProps<{
  initialData?: {
    name: string
    totalBudgetHours: number
    pmId: number
  }
}>()

const emit = defineEmits<{
  submit: [data: { name: string; totalBudgetHours: number; pmId: number }]
}>()

const formRef = ref<FormInstance>()
const pmUsers = ref<User[]>([])
const loadingPms = ref(false)

const form = reactive<ProjectFormData>({
  name: props.initialData?.name ?? '',
  totalBudgetHours: props.initialData?.totalBudgetHours ?? 100,
  pmId: props.initialData?.pmId ?? undefined,
})

const rules: FormRules = {
  name: [{ required: true, message: '請輸入專案名稱', trigger: 'blur' }],
  totalBudgetHours: [{ required: true, message: '請輸入預算時數', trigger: 'blur' }],
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

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid) => {
    if (valid && form.pmId) {
      emit('submit', {
        name: form.name,
        totalBudgetHours: form.totalBudgetHours,
        pmId: form.pmId,
      })
    }
  })
}

onMounted(() => {
  fetchPmUsers()
})
</script>
