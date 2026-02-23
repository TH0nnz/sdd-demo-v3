import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { WorkEntry } from '@/types'
import { workEntryApi } from '@/api/work-entries'

export const useWorkEntryStore = defineStore('workEntries', () => {
  const entries = ref<WorkEntry[]>([])
  const totalElements = ref(0)
  const loading = ref(false)

  async function fetchEntries(params?: { startDate?: string; endDate?: string; taskId?: number }) {
    loading.value = true
    try {
      const { data } = await workEntryApi.getWorkEntries(params)
      entries.value = data.content
      totalElements.value = data.totalElements
    } finally {
      loading.value = false
    }
  }

  return { entries, totalElements, loading, fetchEntries }
})
