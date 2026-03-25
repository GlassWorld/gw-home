type ToastVariant = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  message: string
  variant: ToastVariant
}

interface ShowToastOptions {
  variant?: ToastVariant
  duration?: number
}

export function useToast() {
  const toastList = useState<ToastItem[]>('app-toast-list', () => [])

  function removeToast(id: number) {
    toastList.value = toastList.value.filter((toast) => toast.id !== id)
  }

  function showToast(message: string, options: ShowToastOptions = {}) {
    const id = Date.now() + Math.floor(Math.random() * 1000)
    const toast: ToastItem = {
      id,
      message,
      variant: options.variant ?? 'info'
    }

    toastList.value = [...toastList.value, toast]

    globalThis.setTimeout(() => {
      removeToast(id)
    }, options.duration ?? 2200)
  }

  return {
    toastList,
    showToast,
    removeToast
  }
}
