<script setup lang="ts">
import type { BoardComment } from '~/types/api/board'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

const props = defineProps<{
  comment: BoardComment
  depth?: number
  currentUserNickname?: string
  isAdmin?: boolean
  replyingToCommentUuid?: string | null
  editingCommentUuid?: string | null
  replyDraft?: string
  editDraft?: string
  savingCommentUuid?: string | null
  deletingCommentUuid?: string | null
}>()

const emit = defineEmits<{
  startReply: [commentUuid: string]
  cancelReply: []
  updateReplyDraft: [value: string]
  submitReply: [parentCommentUuid: string]
  startEdit: [payload: { commentUuid: string, currentContent: string }]
  cancelEdit: []
  updateEditDraft: [value: string]
  submitEdit: [commentUuid: string]
  deleteComment: [commentUuid: string]
}>()

const { uploadBoardFile } = useBoard()

const canManage = computed(() => {
  if (!props.currentUserNickname) {
    return false
  }

  return props.currentUserNickname === props.comment.author || props.isAdmin
})

const isReply = computed(() => Boolean(props.comment.parentCommentUuid))
const renderedContent = computed(() => renderMarkdown(props.comment.content))
const commentDepth = computed(() => props.depth ?? 1)
const canReply = computed(() => commentDepth.value < 3)

function updateTextareaValue(
  textarea: HTMLTextAreaElement,
  currentValue: string | undefined,
  emitEvent: (value: string) => void,
  insertValue: string
) {
  const baseValue = currentValue ?? ''
  const selectionStart = textarea.selectionStart ?? baseValue.length
  const selectionEnd = textarea.selectionEnd ?? baseValue.length
  const nextValue = `${baseValue.slice(0, selectionStart)}${insertValue}${baseValue.slice(selectionEnd)}`

  emitEvent(nextValue)

  nextTick(() => {
    const nextPosition = selectionStart + insertValue.length
    textarea.focus()
    textarea.setSelectionRange(nextPosition, nextPosition)
  })
}

async function uploadInlineImages(
  files: File[],
  textarea: HTMLTextAreaElement,
  currentValue: string | undefined,
  emitEvent: (value: string) => void
) {
  const imageFiles = files.filter(file => file.type.startsWith('image/'))

  if (!imageFiles.length) {
    return
  }

  try {
    const uploadedFiles = await Promise.all(
      imageFiles.map(file => uploadBoardFile(file, 'BOARD_IMAGE'))
    )

    const markdownImageLines = uploadedFiles
      .map(file => `<img src="${file.fileUrl}" alt="${file.originalName}" width="480" />`)
      .join('\n')

    const prefix = currentValue && !currentValue.endsWith('\n') ? '\n' : ''
    updateTextareaValue(textarea, currentValue, emitEvent, `${prefix}${markdownImageLines}\n`)
  } catch (error) {
    console.error('댓글 이미지 업로드에 실패했습니다.', error)
  }
}

function handleReplyPaste(event: ClipboardEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.clipboardData?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadInlineImages(files, textarea, props.replyDraft, value => emit('updateReplyDraft', value))
}

function handleReplyDrop(event: DragEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.dataTransfer?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadInlineImages(files, textarea, props.replyDraft, value => emit('updateReplyDraft', value))
}

function handleEditPaste(event: ClipboardEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.clipboardData?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadInlineImages(files, textarea, props.editDraft, value => emit('updateEditDraft', value))
}

function handleEditDrop(event: DragEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.dataTransfer?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadInlineImages(files, textarea, props.editDraft, value => emit('updateEditDraft', value))
}
</script>

<template>
  <article class="board-comment" :class="{ 'board-comment--reply': isReply }">
    <header class="board-comment__header">
      <div class="board-comment__meta">
        <span v-if="isReply" class="board-comment__reply-badge">대댓글 {{ commentDepth }}단계</span>
        <strong>{{ comment.author || '알 수 없는 사용자' }}</strong>
        <div class="meta-row">
          <span>{{ formatDateTime(comment.createdAt) }}</span>
          <span v-if="comment.updatedAt">수정 {{ formatDateTime(comment.updatedAt) }}</span>
        </div>
      </div>

      <div class="board-comment__actions">
        <CommonBaseButton v-if="canReply" variant="secondary" @click="emit('startReply', comment.boardCommentUuid)">
          대댓글
        </CommonBaseButton>
        <CommonBaseButton
          v-if="canManage && editingCommentUuid !== comment.boardCommentUuid"
          variant="secondary"
          @click="emit('startEdit', { commentUuid: comment.boardCommentUuid, currentContent: comment.content })"
        >
          수정
        </CommonBaseButton>
        <CommonBaseButton
          v-if="canManage"
          variant="danger"
          :disabled="deletingCommentUuid === comment.boardCommentUuid"
          @click="emit('deleteComment', comment.boardCommentUuid)"
        >
          {{ deletingCommentUuid === comment.boardCommentUuid ? '삭제 중...' : '삭제' }}
        </CommonBaseButton>
      </div>
    </header>

    <div
      v-if="editingCommentUuid !== comment.boardCommentUuid"
      class="board-comment__content markdown-body"
      v-html="renderedContent"
    />

    <div v-else class="board-comment__editor">
      <textarea
        :value="editDraft"
        class="input-field board-comment__textarea"
        placeholder="마크다운으로 댓글을 수정하세요. 이미지 복붙과 드래그 앤 드롭 업로드를 지원합니다."
        @input="emit('updateEditDraft', ($event.target as HTMLTextAreaElement).value)"
        @paste="handleEditPaste"
        @drop="handleEditDrop"
        @dragover.prevent
      />
      <div class="board-comment__editor-actions">
        <CommonBaseButton variant="secondary" @click="emit('cancelEdit')">
          취소
        </CommonBaseButton>
        <CommonBaseButton :disabled="savingCommentUuid === comment.boardCommentUuid" @click="emit('submitEdit', comment.boardCommentUuid)">
          {{ savingCommentUuid === comment.boardCommentUuid ? '저장 중...' : '수정 저장' }}
        </CommonBaseButton>
      </div>
    </div>

    <div v-if="replyingToCommentUuid === comment.boardCommentUuid" class="board-comment__editor board-comment__reply-editor">
      <textarea
        :value="replyDraft"
        class="input-field board-comment__textarea"
        placeholder="마크다운으로 대댓글을 작성하세요. 이미지 복붙과 드래그 앤 드롭 업로드를 지원합니다."
        @input="emit('updateReplyDraft', ($event.target as HTMLTextAreaElement).value)"
        @paste="handleReplyPaste"
        @drop="handleReplyDrop"
        @dragover.prevent
      />
      <div class="board-comment__editor-actions">
        <CommonBaseButton variant="secondary" @click="emit('cancelReply')">
          취소
        </CommonBaseButton>
        <CommonBaseButton :disabled="savingCommentUuid === comment.boardCommentUuid" @click="emit('submitReply', comment.boardCommentUuid)">
          {{ savingCommentUuid === comment.boardCommentUuid ? '등록 중...' : '대댓글 등록' }}
        </CommonBaseButton>
      </div>
    </div>

    <section v-if="comment.replies.length" class="board-comment__reply-panel">
      <div class="board-comment__replies">
        <BoardCommentThread
          v-for="reply in comment.replies"
          :key="reply.boardCommentUuid"
          :comment="reply"
          :depth="commentDepth + 1"
          :current-user-nickname="currentUserNickname"
          :is-admin="isAdmin"
          :replying-to-comment-uuid="replyingToCommentUuid"
          :editing-comment-uuid="editingCommentUuid"
          :reply-draft="replyDraft"
          :edit-draft="editDraft"
          :saving-comment-uuid="savingCommentUuid"
          :deleting-comment-uuid="deletingCommentUuid"
          @start-reply="emit('startReply', $event)"
          @cancel-reply="emit('cancelReply')"
          @update-reply-draft="emit('updateReplyDraft', $event)"
          @submit-reply="emit('submitReply', $event)"
          @start-edit="emit('startEdit', $event)"
          @cancel-edit="emit('cancelEdit')"
          @update-edit-draft="emit('updateEditDraft', $event)"
          @submit-edit="emit('submitEdit', $event)"
          @delete-comment="emit('deleteComment', $event)"
        />
      </div>
    </section>
  </article>
</template>

<style scoped>
.board-comment {
  display: grid;
  gap: 12px;
  padding: 18px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: 12px;
  background: rgba(7, 16, 38, 0.48);
  font-size: 0.93rem;
}

.board-comment--reply {
  margin-left: 0;
  padding: 12px;
  border: 1px solid rgba(110, 193, 255, 0.22);
  border-radius: 10px;
  background:
    linear-gradient(90deg, rgba(110, 193, 255, 0.08), rgba(110, 193, 255, 0.02)),
    rgba(11, 22, 48, 0.72);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.02);
}

.board-comment__header,
.board-comment__actions,
.board-comment__editor-actions {
  display: flex;
  gap: 10px;
}

.board-comment__header {
  justify-content: space-between;
  align-items: flex-start;
}

.board-comment__meta {
  display: grid;
  gap: 8px;
}

.board-comment__reply-badge {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.16);
  color: var(--color-accent);
  font-size: 0.72rem;
  font-weight: 700;
}

.board-comment__actions,
.board-comment__editor-actions {
  flex-wrap: wrap;
}

.board-comment__content {
  margin: 0;
  line-height: 1.6;
  overflow-wrap: anywhere;
  font-size: 0.93rem;
}

.board-comment__editor {
  display: grid;
  gap: 10px;
}

.board-comment__textarea {
  min-height: 96px;
  font-size: 0.92rem;
}

.board-comment__replies {
  display: grid;
  gap: 8px;
  position: relative;
  padding-left: 14px;
}

.board-comment__reply-panel {
  display: grid;
  gap: 8px;
  margin-top: 2px;
  padding: 10px 10px 10px 12px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: 12px;
  background: rgba(5, 12, 28, 0.46);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03);
  position: relative;
}

.board-comment__reply-panel::before,
.board-comment__replies::before {
  content: '';
  position: absolute;
  top: 10px;
  left: 8px;
  bottom: 10px;
  width: 2px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(110, 193, 255, 0.42), rgba(110, 193, 255, 0.06));
}

.board-comment__replies::before {
  display: none;
}

@media (max-width: 768px) {
  .board-comment--reply {
    padding: 10px;
  }

  .board-comment__header {
    flex-direction: column;
  }

  .board-comment__replies {
    padding-left: 12px;
  }

  .board-comment__reply-panel {
    padding: 8px 8px 8px 10px;
  }

  .board-comment__reply-panel::before {
    left: 6px;
  }

  .board-comment__replies::before {
    display: none;
  }
}
</style>
