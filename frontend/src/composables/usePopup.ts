import { usePopupStore, type PopupOptions, type PopupType } from '../stores/popupStore'

export function usePopup() {
  const store = usePopupStore()

  return {
    show: (options: PopupOptions) => store.show(options),
    confirm: (options: {
      title: string
      message: string
      type?: PopupType
      confirmText?: string
      cancelText?: string
    }) => store.confirm(options),
    success: (message: string, title?: string) => store.success(message, title),
    error: (message: string, title?: string) => store.error(message, title),
    warning: (message: string, title?: string) => store.warning(message, title),
    info: (message: string, title?: string) => store.info(message, title),
    close: () => store.close()
  }
}
