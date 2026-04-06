<script setup lang="ts">
import type { BoardDetail, SaveBoardPostPayload } from '~/types/api/board'

definePageMeta({
  middleware: 'auth'
})

const route = useRoute()
const router = useRouter()
const { fetchBoard, updateBoard } = useBoard()

const board = ref<BoardDetail | null>(null)
const errorMessage = ref('')
const isSubmitting = ref(false)

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

if (!boardPostUuid.value) {
  errorMessage.value = '유효하지 않은 게시글 주소입니다.'
} else {
  try {
    board.value = await fetchBoard(boardPostUuid.value)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '게시글 수정 정보를 불러오지 못했습니다.'
  }
}

async function handleSubmit(payload: SaveBoardPostPayload) {
  if (!board.value || isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const updatedBoard = await updateBoard(board.value.boardPostUuid, payload)
    const normalizedBoardPostUuid = resolveBoardPostUuid(updatedBoard.boardPostUuid)

    if (!normalizedBoardPostUuid) {
      throw new Error('게시글 식별자가 올바르지 않습니다.')
    }

    await router.push(`/board/${normalizedBoardPostUuid}`)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? (error as Error).message ?? '게시글 수정에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="page-container board-edit-page">
    <section class="content-panel board-edit-page__panel">
      <h1 class="section-title">게시글 수정</h1>
      <p class="section-description">
        제목, 본문, 첨부파일을 수정할 수 있습니다.
      </p>

      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <BoardPostEditor
        v-if="board"
        :initial-title="board.title"
        :initial-content="board.content"
        :initial-attachments="board.attachments"
        :submitting="isSubmitting"
        submit-label="수정 저장"
        @submit="handleSubmit"
        @cancel="boardPostUuid && $router.push(`/board/${boardPostUuid}`)"
      />

      <p v-else class="message-muted">
        게시글을 찾는 중입니다.
      </p>
    </section>
  </main>
</template>

<style scoped>
.board-edit-page__panel {
  padding: 28px;
}
</style>
