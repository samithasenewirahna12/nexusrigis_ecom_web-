import { useNotificationStore } from '../stores/notificationStore'
import type { NotificationType } from '../types/notification'

export function useNotification() {
  const store = useNotificationStore()

  return {
    notify: store.notify,
    success: (message: string, title?: string, duration?: number) => store.success(message, title, duration),
    error: (message: string, title?: string, duration?: number) => store.error(message, title, duration),
    warning: (message: string, title?: string, duration?: number) => store.warning(message, title, duration),
    info: (message: string, title?: string, duration?: number) => store.info(message, title, duration),
    remove: store.remove,
    clear: store.clear
  }
}
