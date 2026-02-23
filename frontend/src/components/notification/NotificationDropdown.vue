<template>
  <el-popover
    placement="bottom-end"
    :width="360"
    trigger="click"
    @show="onOpen"
  >
    <template #reference>
      <el-badge :value="store.unreadCount" :hidden="store.unreadCount === 0" class="notification-badge">
        <el-icon :size="20" class="bell-icon"><Bell /></el-icon>
      </el-badge>
    </template>

    <div class="notification-header">
      <span class="notification-title">通知</span>
      <el-button
        v-if="store.notifications.length > 0"
        type="primary"
        link
        size="small"
        @click="markAllAsRead"
      >
        全部標記已讀
      </el-button>
    </div>

    <el-scrollbar max-height="400px">
      <div v-if="store.loading" class="notification-empty">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>載入中...</span>
      </div>
      <div v-else-if="store.notifications.length === 0" class="notification-empty">
        <el-icon :size="40"><BellFilled /></el-icon>
        <span>目前沒有通知</span>
      </div>
      <div
        v-else
        v-for="item in store.notifications"
        :key="item.id"
        class="notification-item"
        :class="{ unread: !item.isRead }"
        @click="handleClick(item)"
      >
        <el-icon :size="20" class="type-icon">
          <component :is="getTypeIcon(item.type)" />
        </el-icon>
        <div class="notification-body">
          <div class="notification-msg">{{ item.title }}</div>
          <div class="notification-content">{{ item.content }}</div>
          <div class="notification-time">{{ formatRelativeTime(item.createdAt) }}</div>
        </div>
        <div v-if="!item.isRead" class="unread-dot" />
      </div>
    </el-scrollbar>
  </el-popover>
</template>

<script setup lang="ts">
import { Bell, BellFilled, ChatDotRound, Warning, CircleCheck, RemoveFilled, Loading } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notifications'
import type { Notification, NotificationType } from '@/types'

const store = useNotificationStore()

function onOpen() {
  store.fetchNotifications()
}

function getTypeIcon(type: NotificationType) {
  const map: Record<NotificationType, typeof Bell> = {
    TASK_HOURS_EXHAUSTED: Warning,
    HOURS_REQUEST_SUBMITTED: ChatDotRound,
    HOURS_REQUEST_APPROVED: CircleCheck,
    HOURS_REQUEST_REJECTED: RemoveFilled,
    TASK_COMPLETED: CircleCheck,
    TASK_UNASSIGNED: Warning,
  }
  return map[type] ?? Bell
}

function formatRelativeTime(dateStr: string): string {
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = Math.floor((now - date) / 1000)

  if (diff < 60) return '剛剛'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分鐘前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小時前`
  if (diff < 2592000) return `${Math.floor(diff / 86400)} 天前`
  return new Date(dateStr).toLocaleDateString('zh-TW')
}

async function handleClick(item: Notification) {
  if (!item.isRead) {
    await store.markAsRead(item.id)
  }
}

async function markAllAsRead() {
  const unread = store.notifications.filter((n) => !n.isRead)
  await Promise.all(unread.map((n) => store.markAsRead(n.id)))
}
</script>

<style scoped>
.bell-icon {
  cursor: pointer;
  color: #606266;
}

.bell-icon:hover {
  color: #409eff;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 8px;
}

.notification-title {
  font-weight: 600;
  font-size: 14px;
}

.notification-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 32px 0;
  color: #909399;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid #f2f6fc;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item.unread {
  background-color: #ecf5ff;
}

.type-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: #909399;
}

.notification-item.unread .type-icon {
  color: #409eff;
}

.notification-body {
  flex: 1;
  min-width: 0;
}

.notification-msg {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  line-height: 1.4;
}

.notification-content {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
  line-height: 1.4;
}

.notification-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #409eff;
  flex-shrink: 0;
  margin-top: 6px;
}
</style>
