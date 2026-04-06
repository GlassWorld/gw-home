<script setup lang="ts">
import type { BoardAttachment, SaveBoardPostPayload } from '~/types/api/board'

const props = withDefaults(defineProps<{
  initialTitle?: string
  initialContent?: string
  initialAttachments?: BoardAttachment[]
  submitLabel?: string
  submitting?: boolean
}>(), {
  initialTitle: '',
  initialContent: '',
  initialAttachments: () => [],
  submitLabel: '저장',
  submitting: false
})

const emit = defineEmits<{
  submit: [payload: SaveBoardPostPayload]
  cancel: []
}>()

const { uploadBoardFile } = useBoard()

const title = ref(props.initialTitle)
const content = ref(props.initialContent)
const attachments = ref<BoardAttachment[]>([...props.initialAttachments])
const errorMessage = ref('')
const isUploading = ref(false)
const editorTextarea = ref<HTMLTextAreaElement | null>(null)

watch(() => props.initialTitle, value => {
  title.value = value
})

watch(() => props.initialContent, value => {
  content.value = value
})

watch(() => props.initialAttachments, value => {
  attachments.value = [...value]
})

function formatFileSize(fileSize: number): string {
  if (fileSize >= 1024 * 1024) {
    return `${(fileSize / (1024 * 1024)).toFixed(1)} MB`
  }

  if (fileSize >= 1024) {
    return `${Math.round(fileSize / 1024)} KB`
  }

  return `${fileSize} B`
}

function removeAttachment(fileUuid: string) {
  attachments.value = attachments.value.filter(attachment => attachment.fileUuid !== fileUuid)
}

function insertTextAtCursor(insertValue: string) {
  const textarea = editorTextarea.value

  if (!textarea) {
    content.value += insertValue
    return
  }

  const selectionStart = textarea.selectionStart ?? content.value.length
  const selectionEnd = textarea.selectionEnd ?? content.value.length
  const nextContent = `${content.value.slice(0, selectionStart)}${insertValue}${content.value.slice(selectionEnd)}`

  content.value = nextContent

  nextTick(() => {
    const nextPosition = selectionStart + insertValue.length
    textarea.focus()
    textarea.setSelectionRange(nextPosition, nextPosition)
  })
}

async function uploadAttachmentFiles(fileList: FileList | File[]) {
  const files = Array.from(fileList)

  if (!files.length || isUploading.value) {
    return
  }

  isUploading.value = true
  errorMessage.value = ''

  try {
    const uploadedFiles = await Promise.all(
      files.map(file => uploadBoardFile(file, 'BOARD_ATTACHMENT'))
    )

    attachments.value = [
      ...attachments.value,
      ...uploadedFiles.map(file => ({
        fileUuid: file.fileUuid,
        originalName: file.originalName,
        fileUrl: file.fileUrl,
        mimeType: file.mimeType,
        fileSize: file.fileSize,
        uploaderType: 'BOARD_ATTACHMENT',
        createdAt: new Date().toISOString()
      }))
    ]
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '첨부파일 업로드에 실패했습니다.'
  } finally {
    isUploading.value = false
  }
}

async function uploadInlineImages(files: File[]) {
  if (!files.length || isUploading.value) {
    return
  }

  isUploading.value = true
  errorMessage.value = ''

  try {
    const uploadedFiles = await Promise.all(
      files.map(file => uploadBoardFile(file, 'BOARD_IMAGE'))
    )

    const markdownImageLines = uploadedFiles
      .map(file => `<img src="${file.fileUrl}" alt="${file.originalName}" width="640" />`)
      .join('\n')

    insertTextAtCursor(`${content.value && !content.value.endsWith('\n') ? '\n' : ''}${markdownImageLines}\n`)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '이미지 업로드에 실패했습니다.'
  } finally {
    isUploading.value = false
  }
}

function handleAttachmentSelect(event: Event) {
  const inputElement = event.target as HTMLInputElement

  if (!inputElement.files?.length) {
    return
  }

  void uploadAttachmentFiles(inputElement.files)
  inputElement.value = ''
}

function handleEditorPaste(event: ClipboardEvent) {
  const files = Array.from(event.clipboardData?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!files.length) {
    return
  }

  event.preventDefault()
  void uploadInlineImages(files)
}

function handleEditorDrop(event: DragEvent) {
  event.preventDefault()
  const files = Array.from(event.dataTransfer?.files ?? [])

  if (!files.length) {
    return
  }

  const imageFiles = files.filter(file => file.type.startsWith('image/'))
  const attachmentFiles = files.filter(file => !file.type.startsWith('image/'))

  if (imageFiles.length) {
    void uploadInlineImages(imageFiles)
  }

  if (attachmentFiles.length) {
    void uploadAttachmentFiles(attachmentFiles)
  }
}

function handleSubmit() {
  errorMessage.value = ''

  if (!title.value.trim()) {
    errorMessage.value = '제목을 입력해주세요.'
    return
  }

  if (!content.value.trim()) {
    errorMessage.value = '본문을 입력해주세요.'
    return
  }

  emit('submit', {
    title: title.value.trim(),
    content: content.value,
    attachmentFileUuids: attachments.value.map(attachment => attachment.fileUuid)
  })
}
</script>

<template>
  <section class="board-editor">
    <p v-if="errorMessage" class="message-error">
      {{ errorMessage }}
    </p>

    <div class="board-editor__form-grid">
      <label class="board-editor__field">
        <span>제목</span>
        <input v-model="title" class="input-field" type="text" maxlength="300" placeholder="제목을 입력하세요">
      </label>
    </div>

    <label class="board-editor__field">
      <span>본문 마크다운</span>
      <textarea
        ref="editorTextarea"
        v-model="content"
        class="input-field board-editor__textarea"
        placeholder="마크다운으로 본문을 작성하세요. 이미지 복붙 시 크기 조절 가능한 img 태그로 삽입됩니다."
        @paste="handleEditorPaste"
        @drop="handleEditorDrop"
        @dragover.prevent
      />
    </label>

    <p class="message-muted board-editor__guide">
      붙여넣은 이미지는 <code>&lt;img ... width="640" /&gt;</code> 형태로 들어갑니다. <code>width</code> 값을 수정하면 크기를 조절할 수 있습니다.
    </p>

    <section class="content-panel board-editor__attachment-panel">
      <div class="board-editor__attachment-header">
        <div>
          <strong>일반 첨부파일</strong>
          <p class="message-muted">이미지는 본문 복붙 또는 드롭으로, 일반 파일은 이 영역에서 관리합니다.</p>
        </div>
        <label class="board-editor__upload-label">
          <input
            type="file"
            multiple
            accept=".pdf,.docx,.xlsx,.zip,image/*"
            class="board-editor__file-input"
            @change="handleAttachmentSelect"
          >
          <span>{{ isUploading ? '업로드 중...' : '파일 추가' }}</span>
        </label>
      </div>

      <ul v-if="attachments.length" class="board-editor__attachment-list">
        <li v-for="attachment in attachments" :key="attachment.fileUuid" class="board-editor__attachment-item">
          <div>
            <strong>{{ attachment.originalName }}</strong>
            <p class="message-muted">{{ formatFileSize(attachment.fileSize) }} · {{ attachment.mimeType }}</p>
          </div>
          <CommonBaseButton variant="secondary" @click="removeAttachment(attachment.fileUuid)">
            제거
          </CommonBaseButton>
        </li>
      </ul>
      <p v-else class="message-muted">
        아직 첨부된 파일이 없습니다.
      </p>
    </section>

    <div class="board-editor__actions">
      <CommonBaseButton variant="secondary" @click="emit('cancel')">
        취소
      </CommonBaseButton>
      <CommonBaseButton :disabled="submitting || isUploading" @click="handleSubmit">
        {{ submitting ? '저장 중...' : submitLabel }}
      </CommonBaseButton>
    </div>
  </section>
</template>

<style scoped>
.board-editor {
  display: grid;
  gap: 20px;
}

.board-editor__form-grid {
  display: grid;
  gap: 16px;
}

.board-editor__field {
  display: grid;
  gap: 10px;
}

.board-editor__field span {
  font-weight: 700;
}

.board-editor__textarea {
  min-height: 360px;
  resize: vertical;
}

.board-editor__guide {
  margin: -4px 0 0;
}

.board-editor__attachment-header,
.board-editor__attachment-item,
.board-editor__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.board-editor__attachment-header {
  align-items: flex-start;
  margin-bottom: 16px;
}

.board-editor__attachment-panel {
  padding: 20px;
}

.board-editor__attachment-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.board-editor__attachment-item {
  padding: 14px 16px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: 10px;
}

.board-editor__upload-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.board-editor__file-input {
  display: none;
}

</style>
