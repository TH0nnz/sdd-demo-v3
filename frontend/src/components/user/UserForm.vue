<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="100px"
    @submit.prevent
  >
    <el-form-item
      label="Email"
      prop="email"
    >
      <el-input
        v-model="form.email"
        :disabled="isEdit"
        placeholder="user@company.com"
      />
    </el-form-item>
    <el-form-item
      label="姓名"
      prop="name"
    >
      <el-input
        v-model="form.name"
        placeholder="請輸入姓名"
      />
    </el-form-item>
    <el-form-item
      label="部門"
      prop="departmentId"
    >
      <el-select
        v-model="form.departmentId"
        placeholder="請選擇部門"
        style="width: 100%"
      >
        <el-option
          v-for="dept in departments"
          :key="dept.id"
          :label="dept.name"
          :value="dept.id"
        />
      </el-select>
    </el-form-item>
    <el-form-item
      label="角色"
      prop="role"
    >
      <el-radio-group v-model="form.role">
        <el-radio
          v-for="role in allRoles"
          :key="role"
          :value="role"
        >
          {{ roleLabel(role) }}
        </el-radio>
      </el-radio-group>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { Role } from '@/types'
import { deptApi, type Department } from '@/api/dept'

const props = defineProps<{
  initialData?: {
    email?: string
    name?: string
    departmentId?: number
    roles?: Role[]
  }
}>()

const emit = defineEmits<{
  submit: [data: { email: string; name: string; departmentId: number; roles: Role[] }]
}>()

const isEdit = ref(!!props.initialData?.email)

const allRoles: Role[] = ['ADMIN', 'PM', 'DEPT_MANAGER', 'EXECUTOR', 'HR']

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

const departments = ref<Department[]>([])

const form = reactive({
  email: props.initialData?.email || '',
  name: props.initialData?.name || '',
  departmentId: props.initialData?.departmentId || (null as number | null),
  role: (props.initialData?.roles?.[0] || null) as Role | null,
})

const rules: FormRules = {
  email: [{ required: true, message: '請輸入Email', trigger: 'blur' }],
  name: [{ required: true, message: '請輸入姓名', trigger: 'blur' }],
  departmentId: [{ required: true, message: '請選擇部門', trigger: 'change' }],
  role: [{ required: true, message: '請選擇角色', trigger: 'change' }],
}

const formRef = ref<FormInstance>()

const validate = async () => {
  if (!formRef.value) return false
  return formRef.value.validate().then(() => true).catch(() => false)
}

const getData = () => ({
  email: form.email,
  name: form.name,
  departmentId: form.departmentId!,
  roles: form.role ? [form.role] : [],
})

watch(
  () => props.initialData,
  (val) => {
    if (val) {
      form.email = val.email || ''
      form.name = val.name || ''
      form.departmentId = val.departmentId || null
      form.role = val.roles?.[0] || null
      isEdit.value = !!val.email
    }
  },
)

onMounted(async () => {
  try {
    const res = await deptApi.listDepartments()
    departments.value = res.data
  } catch {
    // fallback
  }
})

defineExpose({ validate, getData })
</script>
