<script setup lang="ts">
import type { SaveBoardPostPayload } from '~/types/api/board'

definePageMeta({
  middleware: 'auth'
})

const router = useRouter()
const { createBoard } = useBoard()

const errorMessage = ref('')
const isSubmitting = ref(false)

function resolveBoardPostUuid(boardPostUuid: string | null | undefined): string {
  const normalizedBoardPostUuid = boardPostUuid?.trim()

  if (!normalizedBoardPostUuid || normalizedBoardPostUuid === 'undefined' || normalizedBoardPostUuid === 'null') {
    throw new Error('게시글 식별자가 올바르지 않습니다.')
  }

  return normalizedBoardPostUuid
}

async function handleSubmit(payload: SaveBoardPostPayload) {
  if (isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    const board = await createBoard(payload)
    await router.push(`/board/${resolveBoardPostUuid(board.boardPostUuid)}`)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? (error as Error).message ?? '게시글 저장에 실패했습니다.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="page-container board-write-page">
    <section class="content-panel create-board-page">
      <h1 class="section-title">게시글 작성</h1>
      <p class="section-description">
        마크다운 본문 작성, 이미지 복붙 업로드, 일반 첨부파일 등록을 지원합니다.
      </p>
      <p v-if="errorMessage" class="message-error">
        {{ errorMessage }}
      </p>

      <BoardPostEditor
        :submitting="isSubmitting"
        submit-label="게시글 등록"
        @submit="handleSubmit"
        @cancel="$router.push('/board')"
      />
    </section>
  </main>
</template>

<style scoped>
.create-board-page {
  padding: 28px;
}

.board-write-page {
  display: grid;
}
</style>
