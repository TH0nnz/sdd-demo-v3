<template>
  <div class="change-password-container">
    <el-card class="change-password-card">
      <template #header>
        <h2 class="title">
          變更密碼
        </h2>
      </template>
      <el-alert
        v-if="authStore.forcePasswordChange"
        title="首次登入需變更密碼"
        type="warning"
        :closable="false"
        show-icon
        class="alert"
      />
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        @submit.prevent="handleSubmit"
      >
        <el-form-item
          label="目前密碼"
          prop="currentPassword"
        >
          <el-input
            v-model="form.currentPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item
          label="新密碼"
          prop="newPassword"
        >
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
          />
        </el-form-item>
        <el-form-item
          label="確認新密碼"
          prop="confirmPassword"
        >
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            確認變更
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { getDefaultRoute } from '@/router'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*]).{8,}$/

const rules: FormRules = {
  currentPassword: [
    { required: true, message: '請輸入目前密碼', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '請輸入新密碼', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (!passwordPattern.test(value)) {
          callback(new Error('密碼需至少 8 碼，包含大小寫字母、數字及特殊符號'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '請再次輸入新密碼', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.newPassword) {
          callback(new Error('兩次輸入的密碼不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authApi.changePassword({
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    authStore.forcePasswordChange = false
    ElMessage.success('密碼變更成功')
    router.push(getDefaultRoute(authStore.roles))
  } catch {
    ElMessage.error('密碼變更失敗，請確認目前密碼是否正確')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.change-password-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.change-password-card {
  width: 480px;
}

.title {
  text-align: center;
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.alert {
  margin-bottom: 20px;
}
</style>
