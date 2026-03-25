type DialogKind = 'alert' | 'confirm'

interface DialogState {
  visible: boolean
  kind: DialogKind
  title: string
  message: string
  confirmText: string
  cancelText: string
}

interface DialogOptions {
  title?: string
  confirmText?: string
  cancelText?: string
}

let dialogResolver: ((value: boolean) => void) | null = null

export function useDialog() {
  const dialogState = useState<DialogState>('app-dialog-state', () => ({
    visible: false,
    kind: 'alert',
    title: '',
    message: '',
    confirmText: '확인',
    cancelText: '취소'
  }))

  function openDialog(kind: DialogKind, message: string, options: DialogOptions = {}) {
    dialogState.value = {
      visible: true,
      kind,
      title: options.title ?? (kind === 'confirm' ? '확인' : '안내'),
      message,
      confirmText: options.confirmText ?? '확인',
      cancelText: options.cancelText ?? '취소'
    }

    return new Promise<boolean>((resolve) => {
      dialogResolver = resolve
    })
  }

  function closeDialog(result: boolean) {
    dialogState.value = {
      ...dialogState.value,
      visible: false
    }

    if (dialogResolver) {
      dialogResolver(result)
      dialogResolver = null
    }
  }

  async function alert(message: string, options: DialogOptions = {}) {
    await openDialog('alert', message, options)
  }

  function confirm(message: string, options: DialogOptions = {}) {
    return openDialog('confirm', message, options)
  }

  return {
    dialogState,
    alert,
    confirm,
    closeDialog
  }
}
