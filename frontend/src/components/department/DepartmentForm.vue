<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-width="80px"
    @submit.prevent
  >
    <el-form-item
      label="部門名稱"
      prop="name"
    >
      <el-input
        v-model="form.name"
        placeholder="請輸入部門名稱"
      />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'

const props = defineProps<{
  initialData?: {
    name?: string
  }
}>()

const form = reactive({
  name: props.initialData?.name || '',
})

const rules: FormRules = {
  name: [{ required: true, message: '請輸入部門名稱', trigger: 'blur' }],
}

const formRef = ref<FormInstance>()

const validate = async () => {
  if (!formRef.value) return false
  return formRef.value.validate().then(() => true).catch(() => false)
}

const getData = () => ({
  name: form.name,
})

watch(
  () => props.initialData,
  (val) => {
    if (val) {
      form.name = val.name || ''
    }
  },
)

defineExpose({ validate, getData })
</script>
