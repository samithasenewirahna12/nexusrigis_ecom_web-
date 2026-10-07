export type NotificationType = 'success' | 'error' | 'warning' | 'info'

export interface NotificationItem {
  id: string
  type: NotificationType
  title?: string
  message: string
  duration?: number
  dismissible?: boolean
  timestamp: number
}
