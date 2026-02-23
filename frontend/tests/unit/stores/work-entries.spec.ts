import { beforeEach, describe, expect, it, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useWorkEntryStore } from '@/stores/work-entries'

vi.mock('@/api/work-entries', () => ({
  workEntryApi: {
    getWorkEntries: vi.fn().mockResolvedValue({
      data: {
        content: [
          {
            id: 1,
            taskId: 10,
            taskName: 'Task A',
            projectName: 'Proj A',
            workDate: '2026-02-24',
            hours: 2,
            editable: true,
            createdAt: '2026-02-24T10:00:00+08:00',
            updatedAt: '2026-02-24T10:00:00+08:00',
          },
        ],
        totalElements: 1,
      },
    }),
  },
}))

describe('work-entries store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('loads entries and total count', async () => {
    const store = useWorkEntryStore()
    await store.fetchEntries()

    expect(store.entries.length).toBe(1)
    expect(store.totalElements).toBe(1)
    expect(store.loading).toBe(false)
  })
})
