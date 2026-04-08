<script setup lang="ts">
import type { BoardComment, BoardDetail, BoardFavoriteState } from '~/types/api/board'
import type { BoardShareSettings } from '~/types/api/board-share'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const {
  createComment,
  deleteBoard,
  deleteComment,
  downloadBoardFile,
  fetchBoard,
  fetchBoardFavorite,
  fetchComments,
  toggleBoardFavorite,
  uploadBoardFile,
  updateComment
} = useBoard()
const { fetchBoardShare } = useBoardShare()
const { showToast } = useToast()

const board = ref<BoardDetail | null>(null)
const boardShareSettings = ref<BoardShareSettings | null>(null)
const commentList = ref<BoardComment[]>([])
const errorMessage = ref('')
const isDeleting = ref(false)
const isCommentsLoading = ref(false)
const commentDraft = ref('')
const replyDraft = ref('')
const editDraft = ref('')
const replyingToCommentUuid = ref<string | null>(null)
const editingCommentUuid = ref<string | null>(null)
const savingCommentUuid = ref<string | null>(null)
const deletingCommentUuid = ref<string | null>(null)
const isClientReady = ref(false)
const favoriteState = ref<BoardFavoriteState>({ favorited: false, favoriteCount: 0 })
const isFavoriteLoading = ref(false)
const isShareModalVisible = ref(false)

function resolveBoardPostUuid(value: unknown): string | null {
  if (typeof value !== 'string') {
    return null
  }

  const normalizedBoardPostUuid = value.trim()

  if (!normalizedBoardPostUuid || normalizedBoardPostUuid === 'undefined' || normalizedBoardPostUuid === 'null') {
    return null
  }

  return normalizedBoardPostUuid
}

const boardPostUuid = computed(() => resolveBoardPostUuid(route.params.boardUuid))

const canManagePost = computed(() => {
  if (!isClientReady.value || !board.value || !authStore.currentUser) {
    return false
  }

  return board.value.author === authStore.currentUser.nickname || authStore.currentUser.role === 'ADMIN'
})

const isAdmin = computed(() => isClientReady.value && authStore.currentUser?.role === 'ADMIN')
const currentUserNickname = computed(() => isClientReady.value ? authStore.currentUser?.nickname : undefined)
const renderedHtml = computed(() => renderMarkdown(board.value?.content))

onMounted(() => {
  isClientReady.value = true
})

if (!boardPostUuid.value) {
  errorMessage.value = '유효하지 않은 게시글 주소입니다.'
} else {
  try {
    board.value = await fetchBoard(boardPostUuid.value)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '게시글 상세를 불러오지 못했습니다.'
  }
}

if (boardPostUuid.value) {
  await loadComments()
  await loadFavorite()
}

watch(
  () => canManagePost.value,
  async (value) => {
    if (!value || !boardPostUuid.value) {
      return
    }

    try {
      boardShareSettings.value = await fetchBoardShare(boardPostUuid.value)
    } catch (error) {
      const fetchError = error as { data?: { message?: string } }
      showToast(fetchError.data?.message ?? '공유 설정을 불러오지 못했습니다.', { variant: 'error' })
    }
  },
  { immediate: true }
)

async function loadComments() {
  if (!boardPostUuid.value) {
    commentList.value = []
    return
  }

  isCommentsLoading.value = true

  try {
    commentList.value = await fetchComments(boardPostUuid.value)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '댓글을 불러오지 못했습니다.'
  } finally {
    isCommentsLoading.value = false
  }
}

async function handleDelete() {
  if (!board.value || isDeleting.value) {
    return
  }

  isDeleting.value = true

  try {
    await deleteBoard(board.value.boardPostUuid)
    await navigateTo('/board')
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '게시글 삭제에 실패했습니다.'
  } finally {
    isDeleting.value = false
  }
}

async function handleAttachmentDownload(fileUuid: string, originalName: string) {
  try {
    await downloadBoardFile(fileUuid, originalName)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '첨부파일 다운로드에 실패했습니다.'
  }
}

async function loadFavorite() {
  if (!boardPostUuid.value) {
    favoriteState.value = { favorited: false, favoriteCount: 0 }
    return
  }

  try {
    favoriteState.value = await fetchBoardFavorite(boardPostUuid.value)
  } catch {
    favoriteState.value = {
      favorited: false,
      favoriteCount: board.value?.favoriteCount ?? 0
    }
  }
}

async function handleMoveToEdit() {
  if (!boardPostUuid.value) {
    errorMessage.value = '유효하지 않은 게시글 주소입니다.'
    return
  }

  await router.push(`/board/${boardPostUuid.value}/edit`)
}

async function handleToggleFavorite() {
  if (!boardPostUuid.value || isFavoriteLoading.value) {
    return
  }

  isFavoriteLoading.value = true

  try {
    favoriteState.value = await toggleBoardFavorite(boardPostUuid.value)

    if (board.value) {
      board.value.favoriteCount = favoriteState.value.favoriteCount
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '좋아요 처리에 실패했습니다.'
  } finally {
    isFavoriteLoading.value = false
  }
}

function openShareModal() {
  isShareModalVisible.value = true
}

function closeShareModal() {
  isShareModalVisible.value = false
}

function handleBoardShareSaved(settings: BoardShareSettings) {
  boardShareSettings.value = settings
}

function getBoardShareStatusLabel(status: BoardShareSettings['status'] | undefined): string {
  switch (status) {
    case 'SHARING':
      return '공유중'
    case 'EXPIRING_SOON':
      return '만료 예정'
    case 'EXPIRED':
      return '만료됨'
    case 'REVOKED':
      return '공유 해제'
    default:
      return '공유 해제'
  }
}

async function handleCreateComment() {
  if (!commentDraft.value.trim() || savingCommentUuid.value) {
    return
  }

  savingCommentUuid.value = 'root'

  try {
    if (!boardPostUuid.value) {
      throw new Error('유효하지 않은 게시글 주소입니다.')
    }

    await createComment(boardPostUuid.value, { content: commentDraft.value.trim() })
    commentDraft.value = ''
    await loadComments()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? (error as Error).message ?? '댓글 등록에 실패했습니다.'
  } finally {
    savingCommentUuid.value = null
  }
}

function startReply(commentUuid: string) {
  replyingToCommentUuid.value = commentUuid
  replyDraft.value = ''
  editingCommentUuid.value = null
  editDraft.value = ''
}

function cancelReply() {
  replyingToCommentUuid.value = null
  replyDraft.value = ''
}

function startEdit(payload: { commentUuid: string, currentContent: string }) {
  editingCommentUuid.value = payload.commentUuid
  editDraft.value = payload.currentContent
  replyingToCommentUuid.value = null
  replyDraft.value = ''
}

function cancelEdit() {
  editingCommentUuid.value = null
  editDraft.value = ''
}

async function submitReply(parentCommentUuid: string) {
  if (!replyDraft.value.trim()) {
    return
  }

  savingCommentUuid.value = parentCommentUuid

  try {
    if (!boardPostUuid.value) {
      throw new Error('유효하지 않은 게시글 주소입니다.')
    }

    await createComment(boardPostUuid.value, {
      parentCommentUuid,
      content: replyDraft.value.trim()
    })
    cancelReply()
    await loadComments()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? (error as Error).message ?? '대댓글 등록에 실패했습니다.'
  } finally {
    savingCommentUuid.value = null
  }
}

async function submitEdit(commentUuid: string) {
  if (!editDraft.value.trim()) {
    return
  }

  savingCommentUuid.value = commentUuid

  try {
    await updateComment(commentUuid, editDraft.value.trim())
    cancelEdit()
    await loadComments()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '댓글 수정에 실패했습니다.'
  } finally {
    savingCommentUuid.value = null
  }
}

async function handleDeleteComment(commentUuid: string) {
  deletingCommentUuid.value = commentUuid

  try {
    await deleteComment(commentUuid)
    if (replyingToCommentUuid.value === commentUuid) {
      cancelReply()
    }
    if (editingCommentUuid.value === commentUuid) {
      cancelEdit()
    }
    await loadComments()
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '댓글 삭제에 실패했습니다.'
  } finally {
    deletingCommentUuid.value = null
  }
}

function insertCommentTextAtCursor(textarea: HTMLTextAreaElement, insertValue: string) {
  const selectionStart = textarea.selectionStart ?? commentDraft.value.length
  const selectionEnd = textarea.selectionEnd ?? commentDraft.value.length
  const nextContent = `${commentDraft.value.slice(0, selectionStart)}${insertValue}${commentDraft.value.slice(selectionEnd)}`

  commentDraft.value = nextContent

  nextTick(() => {
    const nextPosition = selectionStart + insertValue.length
    textarea.focus()
    textarea.setSelectionRange(nextPosition, nextPosition)
  })
}

async function uploadCommentImages(files: File[], textarea: HTMLTextAreaElement) {
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

    const prefix = commentDraft.value && !commentDraft.value.endsWith('\n') ? '\n' : ''
    insertCommentTextAtCursor(textarea, `${prefix}${markdownImageLines}\n`)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '댓글 이미지 업로드에 실패했습니다.'
  }
}

function handleCommentPaste(event: ClipboardEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.clipboardData?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadCommentImages(files, textarea)
}

function handleCommentDrop(event: DragEvent) {
  const textarea = event.target as HTMLTextAreaElement | null
  const files = Array.from(event.dataTransfer?.files ?? []).filter(file => file.type.startsWith('image/'))

  if (!textarea || !files.length) {
    return
  }

  event.preventDefault()
  void uploadCommentImages(files, textarea)
}
</script>

<template>
  <main class="page-container board-detail-page">
    <section class="content-panel board-detail-page__panel">
      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <template v-else-if="board">
        <div class="board-detail-page__header">
          <div>
            <h1 class="section-title">{{ board.title }}</h1>
            <div class="board-detail-page__meta-list">
              <div class="board-detail-page__meta-item">
                <span class="board-detail-page__meta-label">작성자</span>
                <span class="board-detail-page__meta-value">{{ board.author }}</span>
              </div>
              <div class="board-detail-page__meta-item">
                <span class="board-detail-page__meta-label">작성일</span>
                <span class="board-detail-page__meta-value">{{ formatDateTime(board.createdAt) }}</span>
              </div>
              <div class="board-detail-page__meta-item">
                <span class="board-detail-page__meta-label">조회</span>
                <span class="board-detail-page__meta-value">{{ board.viewCount }}</span>
              </div>
              <div class="board-detail-page__meta-item">
                <span class="board-detail-page__meta-label">좋아요</span>
                <span class="board-detail-page__meta-value">{{ favoriteState.favoriteCount }}</span>
              </div>
          </div>
        </div>

          <div v-if="canManagePost && boardPostUuid" class="board-detail-page__actions">
            <span class="board-detail-page__share-status">{{ getBoardShareStatusLabel(boardShareSettings?.status) }}</span>
            <CommonBaseButton
              variant="secondary"
              :disabled="isFavoriteLoading"
              @click="handleToggleFavorite"
            >
              {{ isFavoriteLoading ? '처리 중...' : favoriteState.favorited ? '좋아요 취소' : '좋아요' }}
            </CommonBaseButton>
            <CommonBaseButton variant="secondary" @click="openShareModal">
              공유
            </CommonBaseButton>
            <CommonBaseButton variant="secondary" @click="handleMoveToEdit">
              수정
            </CommonBaseButton>
            <CommonBaseButton variant="danger" :disabled="isDeleting" @click="handleDelete">
              {{ isDeleting ? '삭제 중...' : '삭제' }}
            </CommonBaseButton>
          </div>
        </div>

        <div v-if="board.tags.length" class="board-detail-page__tags">
          <span v-for="tag in board.tags" :key="tag.tagUuid">
            #{{ tag.name }}
          </span>
        </div>

        <article class="board-detail-page__content board-markdown" v-html="renderedHtml" />

        <section class="board-detail-page__attachment-panel">
          <div class="board-detail-page__section-header">
            <h2>첨부파일</h2>
            <span class="message-muted">{{ board.attachments.length }}개</span>
          </div>

          <ul v-if="board.attachments.length" class="board-detail-page__attachment-list">
            <li v-for="attachment in board.attachments" :key="attachment.fileUuid" class="board-detail-page__attachment-item">
              <div>
                <strong>{{ attachment.originalName }}</strong>
                <p class="message-muted">{{ attachment.mimeType }} · {{ Math.max(1, Math.round(attachment.fileSize / 1024)) }} KB</p>
              </div>
              <CommonBaseButton variant="secondary" @click="handleAttachmentDownload(attachment.fileUuid, attachment.originalName)">
                다운로드
              </CommonBaseButton>
            </li>
          </ul>
          <p v-else class="message-muted">
            첨부파일이 없습니다.
          </p>
        </section>

        <section class="board-detail-page__comment-panel">
          <div class="board-detail-page__section-header">
            <h2>댓글</h2>
            <span class="message-muted">{{ board.commentCount }}개</span>
          </div>

          <div class="board-detail-page__comment-editor">
            <textarea
              v-model="commentDraft"
              class="input-field board-detail-page__comment-textarea"
              placeholder="마크다운으로 댓글을 작성하세요. 이미지 복붙 시 크기 조절 가능한 img 태그로 삽입됩니다."
              @paste="handleCommentPaste"
              @drop="handleCommentDrop"
              @dragover.prevent
            />
            <p class="message-muted board-detail-page__comment-guide">
              붙여넣은 이미지는 <code>&lt;img ... width="480" /&gt;</code> 형태로 들어갑니다. <code>width</code> 값을 바꾸면 크기를 조절할 수 있습니다.
            </p>
            <div class="board-detail-page__comment-actions">
              <CommonBaseButton :disabled="savingCommentUuid === 'root'" @click="handleCreateComment">
                {{ savingCommentUuid === 'root' ? '등록 중...' : '댓글 등록' }}
              </CommonBaseButton>
            </div>
          </div>

          <p v-if="isCommentsLoading" class="message-muted">
            댓글을 불러오는 중입니다.
          </p>
          <div v-else-if="commentList.length" class="board-detail-page__comment-list">
            <BoardCommentThread
              v-for="comment in commentList"
              :key="comment.boardCommentUuid"
              :comment="comment"
              :depth="1"
              :current-user-nickname="currentUserNickname"
              :is-admin="isAdmin"
              :replying-to-comment-uuid="replyingToCommentUuid"
              :editing-comment-uuid="editingCommentUuid"
              :reply-draft="replyDraft"
              :edit-draft="editDraft"
              :saving-comment-uuid="savingCommentUuid"
              :deleting-comment-uuid="deletingCommentUuid"
              @start-reply="startReply"
              @cancel-reply="cancelReply"
              @update-reply-draft="replyDraft = $event"
              @submit-reply="submitReply"
              @start-edit="startEdit"
              @cancel-edit="cancelEdit"
              @update-edit-draft="editDraft = $event"
              @submit-edit="submitEdit"
              @delete-comment="handleDeleteComment"
            />
          </div>
          <p v-else class="message-muted">
            첫 댓글을 남겨보세요.
          </p>
        </section>
      </template>

      <p v-else class="message-muted">
        게시글을 찾는 중입니다.
      </p>
    </section>

    <BoardShareModal
      v-if="boardPostUuid"
      :visible="isShareModalVisible"
      :board-post-uuid="boardPostUuid"
      :share-settings="boardShareSettings"
      @close="closeShareModal"
      @saved="handleBoardShareSaved"
    />
  </main>
</template>

<style scoped>
.board-detail-page__panel {
  padding: 32px;
}

.board-detail-page__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.board-detail-page__actions,
.board-detail-page__tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.board-detail-page__share-status {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(14, 165, 233, 0.14);
  color: #7dd3fc;
  font-size: 0.86rem;
  font-weight: 700;
}

.board-detail-page__meta-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.board-detail-page__meta-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid rgba(147, 210, 255, 0.14);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
}

.board-detail-page__meta-label {
  color: var(--color-text-muted);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.board-detail-page__meta-value {
  color: var(--color-text);
  font-size: 0.92rem;
  font-weight: 600;
}

.board-detail-page__attachment-panel,
.board-detail-page__comment-panel {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid rgba(116, 87, 51, 0.12);
}

.board-detail-page__section-header,
.board-detail-page__attachment-item,
.board-detail-page__comment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.board-detail-page__tags {
  margin-top: 20px;
}

.board-detail-page__tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(29, 107, 99, 0.12);
  color: var(--color-accent);
}

.board-detail-page__content {
  margin-top: 24px;
  min-height: 320px;
  line-height: 1.75;
}

.board-detail-page__attachment-list,
.board-detail-page__comment-list {
  display: grid;
  gap: 14px;
  margin: 16px 0 0;
  padding: 0;
  list-style: none;
}

.board-detail-page__attachment-item {
  padding: 16px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: 12px;
  font-size: 0.92rem;
}

.board-detail-page__attachment-item strong {
  font-size: 0.92rem;
}

.board-detail-page__comment-editor {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.board-detail-page__comment-guide {
  margin: -4px 0 0;
}

.board-detail-page__comment-textarea {
  min-height: 120px;
  font-size: 0.94rem;
}

:deep(.board-markdown img) {
  max-width: 100%;
  border-radius: 12px;
}

:deep(.board-markdown pre) {
  overflow-x: auto;
  padding: 14px;
  border-radius: 12px;
  background: rgba(7, 16, 38, 0.88);
}

@media (max-width: 768px) {
  .board-detail-page__header {
    flex-direction: column;
  }

  .board-detail-page__meta-list {
    gap: 8px;
  }

  .board-detail-page__attachment-item,
  .board-detail-page__section-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
