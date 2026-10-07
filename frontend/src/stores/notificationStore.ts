import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { NotificationItem, NotificationType } from '../types/notification'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationItem[]>([])

  function notify(payload: {
    type?: NotificationType
    title?: string
    message: string
    duration?: number
    dismissible?: boolean
  }): string {
    const id = `${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
    const duration = payload.duration !== undefined ? payload.duration : 4500
    const dismissible = payload.dismissible !== undefined ? payload.dismissible : true

    const item: NotificationItem = {
      id,
      type: payload.type || 'info',
      title: payload.title,
      message: payload.message,
      duration,
      dismissible,
      timestamp: Date.now()
    }

    notifications.value.push(item)

    if (duration > 0) {
      setTimeout(() => {
        remove(id)
      }, duration)
    }

    return id
  }

  function success(message: string, title: string = 'Success', duration: number = 4000): string {
    return notify({ type: 'success', title, message, duration })
  }

  function error(message: string, title: string = 'Error', duration: number = 6000): string {
    return notify({ type: 'error', title, message, duration })
  }

  function warning(message: string, title: string = 'Notice', duration: number = 5000): string {
    return notify({ type: 'warning', title, message, duration })
  }

  function info(message: string, title: string = 'Information', duration: number = 4000): string {
    return notify({ type: 'info', title, message, duration })
  }

  function remove(id: string) {
    const index = notifications.value.findIndex(n => n.id === id)
    if (index !== -1) {
      notifications.value.splice(index, 1)
    }
  }

  function clear() {
    notifications.value = []
  }

  return {
    notifications,
    notify,
    success,
    error,
    warning,
    info,
    remove,
    clear
  }
})
