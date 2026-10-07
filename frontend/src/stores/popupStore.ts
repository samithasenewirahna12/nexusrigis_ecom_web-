import { defineStore } from 'pinia'
import { ref } from 'vue'

export type PopupType = 'info' | 'success' | 'warning' | 'error'

export interface PopupOptions {
  type?: PopupType
  title: string
  message: string
  showCancel?: boolean
  confirmText?: string
  cancelText?: string
}

export const usePopupStore = defineStore('popup', () => {
  const visible = ref(false)
  const type = ref<PopupType>('info')
  const title = ref('')
  const message = ref('')
  const showCancel = ref(false)
  const cancelText = ref('Cancel')
  const confirmText = ref('OK')

  let resolvePromise: ((value: boolean) => void) | null = null

  function show(options: PopupOptions): Promise<boolean> {
    type.value = options.type || 'info'
    title.value = options.title
    message.value = options.message
    showCancel.value = options.showCancel ?? false
    confirmText.value = options.confirmText || 'OK'
    cancelText.value = options.cancelText || 'Cancel'
    visible.value = true

    return new Promise<boolean>((resolve) => {
      resolvePromise = resolve
    })
  }

  function confirm(options: {
    title: string
    message: string
    type?: PopupType
    confirmText?: string
    cancelText?: string
  }): Promise<boolean> {
    return show({
      ...options,
      type: options.type || 'warning',
      showCancel: true,
      confirmText: options.confirmText || 'Confirm',
      cancelText: options.cancelText || 'Cancel'
    })
  }

  function success(messageText: string, titleText: string = 'Success'): Promise<boolean> {
    return show({
      type: 'success',
      title: titleText,
      message: messageText,
      showCancel: false,
      confirmText: 'OK'
    })
  }

  function error(messageText: string, titleText: string = 'Something went wrong'): Promise<boolean> {
    return show({
      type: 'error',
      title: titleText,
      message: messageText,
      showCancel: false,
      confirmText: 'OK'
    })
  }

  function warning(messageText: string, titleText: string = 'Notice'): Promise<boolean> {
    return show({
      type: 'warning',
      title: titleText,
      message: messageText,
      showCancel: false,
      confirmText: 'OK'
    })
  }

  function info(messageText: string, titleText: string = 'Information'): Promise<boolean> {
    return show({
      type: 'info',
      title: titleText,
      message: messageText,
      showCancel: false,
      confirmText: 'OK'
    })
  }

  function close() {
    visible.value = false
    if (resolvePromise) {
      resolvePromise(false)
      resolvePromise = null
    }
  }

  function handleConfirm() {
    visible.value = false
    if (resolvePromise) {
      resolvePromise(true)
      resolvePromise = null
    }
  }

  return {
    visible,
    type,
    title,
    message,
    showCancel,
    cancelText,
    confirmText,
    show,
    confirm,
    success,
    error,
    warning,
    info,
    close,
    handleConfirm
  }
})
