<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useWorkEntryStore } from '@/stores/work-entries'
import WorkEntryForm from '@/components/work-entry/WorkEntryForm.vue'
import WorkEntryList from '@/components/work-entry/WorkEntryList.vue'

const store = useWorkEntryStore()

const dateRange = ref<[string, string] | null>(null)

function buildParams() {
  const params: { startDate?: string; endDate?: string } = {}
  if (dateRange.value) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

async function loadEntries() {
  try {
    await store.fetchEntries(buildParams())
  } catch {
    ElMessage.error('載入工時紀錄失敗')
  }
}

function handleDateChange() {
  loadEntries()
}

function handleCreated() {
  loadEntries()
}

function handleUpdated() {
  loadEntries()
}

onMounted(loadEntries)
</script>

<template>
  <div>
    <h2>新增工時</h2>
    <el-card style="margin-bottom: 24px">
      <WorkEntryForm @created="handleCreated" />
    </el-card>

    <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px">
      <h2 style="margin: 0">
        近期工時紀錄
      </h2>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="開始日期"
        end-placeholder="結束日期"
        value-format="YYYY-MM-DD"
        style="width: 300px"
        @change="handleDateChange"
      />
    </div>

    <WorkEntryList
      :entries="store.entries"
      :loading="store.loading"
      @updated="handleUpdated"
    />
  </div>
</template>
