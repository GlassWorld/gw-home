<script setup lang="ts">
import type { BoardShareAccessStatusInfo, PublicBoardShare } from '~/types/api/board-share'
import { formatDateTime } from '~/utils/date'
import { renderMarkdown } from '~/utils/markdown'

const route = useRoute()
const { accessBoardShare, fetchBoardShareStatus } = useBoardShare()

const isClientReady = ref(false)
const shareStatus = ref<BoardShareAccessStatusInfo | null>(null)
const sharedBoard = ref<PublicBoardShare | null>(null)
const password = ref('')
const errorMessage = ref('')
const isLoading = ref(true)
const isSubmitting = ref(false)

useHead({
  meta: [
    { name: 'robots', content: 'noindex, nofollow, noarchive' }
  ]
})

const shareToken = computed(() => {
  const rawToken = route.params.shareToken
  return typeof rawToken === 'string' ? rawToken.trim() : ''
})

const renderedHtml = computed(() => {
  if (!isClientReady.value || !sharedBoard.value) {
    return ''
  }

  return renderMarkdown(sharedBoard.value.content)
})

const shareStatusMessage = computed(() => {
  switch (shareStatus.value?.status) {
    case 'PASSWORD_REQUIRED':
      return '비밀번호를 입력하면 공유된 게시글을 읽을 수 있습니다.'
    case 'EXPIRED':
      return '만료된 링크입니다.'
    case 'REVOKED':
      return '해제된 링크입니다.'
    case 'UNAVAILABLE':
      return '삭제되었거나 더 이상 공유할 수 없는 게시글입니다.'
    case 'NOT_FOUND':
      return '존재하지 않는 링크입니다.'
    default:
      return ''
  }
})

onMounted(async () => {
  isClientReady.value = true
  await loadShareStatus()
})

async function loadShareStatus() {
  if (!shareToken.value) {
    errorMessage.value = '유효하지 않은 공유 링크입니다.'
    isLoading.value = false
    return
  }

  isLoading.value = true
  errorMessage.value = ''

  try {
    shareStatus.value = await fetchBoardShareStatus(shareToken.value)

    if (shareStatus.value.status === 'AVAILABLE') {
      await loadSharedBoard()
    }
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '공유 링크 상태를 확인하지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}

async function loadSharedBoard(inputPassword?: string) {
  if (!shareToken.value || isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  errorMessage.value = ''

  try {
    sharedBoard.value = await accessBoardShare(shareToken.value, inputPassword)
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    errorMessage.value = fetchError.data?.message ?? '공유 게시글을 불러오지 못했습니다.'
  } finally {
    isSubmitting.value = false
  }
}

async function handlePasswordSubmit() {
  await loadSharedBoard(password.value.trim())
}
</script>

<template>
  <main class="public-board-share-page">
    <section class="public-board-share-page__panel content-panel">
      <div class="public-board-share-page__hero">
        <p class="public-board-share-page__eyebrow">Board Share</p>
        <h1>공유 페이지</h1>
      </div>

      <p v-if="isLoading" class="message-muted">
        공유 링크를 확인하는 중입니다.
      </p>

      <template v-else-if="sharedBoard">
        <header class="public-board-share-page__header">
          <div>
            <p v-if="sharedBoard.categoryName" class="public-board-share-page__category">{{ sharedBoard.categoryName }}</p>
            <h2>{{ sharedBoard.title }}</h2>
          </div>
          <div class="public-board-share-page__meta">
            <span>작성자 {{ sharedBoard.author }}</span>
            <span>작성일 {{ formatDateTime(sharedBoard.createdAt) }}</span>
            <span>공유 만료 {{ formatDateTime(sharedBoard.expiresAt) }}</span>
          </div>
        </header>

        <article class="public-board-share-page__content board-markdown" v-html="renderedHtml" />

        <section class="public-board-share-page__attachment-section">
          <div class="public-board-share-page__section-header">
            <h3>첨부 정보</h3>
            <span class="message-muted">{{ sharedBoard.attachments.length }}개</span>
          </div>

          <ul v-if="sharedBoard.attachments.length" class="public-board-share-page__attachment-list">
            <li v-for="attachment in sharedBoard.attachments" :key="`${attachment.originalName}-${attachment.createdAt}`" class="public-board-share-page__attachment-item">
              <strong>{{ attachment.originalName }}</strong>
              <span class="message-muted">
                {{ attachment.mimeType }} · {{ Math.max(1, Math.round(attachment.fileSize / 1024)) }} KB
              </span>
            </li>
          </ul>
          <p v-else class="message-muted">
            첨부파일이 없습니다.
          </p>
        </section>

        <section class="public-board-share-page__comment-section">
          <div class="public-board-share-page__section-header">
            <h3>댓글</h3>
            <span class="message-muted">{{ sharedBoard.comments.length }}개</span>
          </div>

          <div v-if="sharedBoard.comments.length" class="public-board-share-page__comment-list">
            <BoardCommentReadonlyThread
              v-for="comment in sharedBoard.comments"
              :key="comment.boardCommentUuid"
              :comment="comment"
              :depth="1"
            />
          </div>
          <p v-else class="message-muted">
            댓글이 없습니다.
          </p>
        </section>
      </template>

      <template v-else>
        <section v-if="shareStatus?.status === 'PASSWORD_REQUIRED'" class="public-board-share-page__password-section">
          <h2>{{ shareStatus.title ?? '비밀번호가 필요한 공유 링크' }}</h2>
          <p class="message-muted">{{ shareStatusMessage }}</p>
          <input
            v-model="password"
            class="input-field"
            type="password"
            placeholder="공유 비밀번호 입력"
            @keyup.enter="handlePasswordSubmit"
          >
          <CommonBaseButton :disabled="isSubmitting" @click="handlePasswordSubmit">
            {{ isSubmitting ? '확인 중...' : '확인' }}
          </CommonBaseButton>
        </section>

        <section v-else class="public-board-share-page__empty-state">
          <h2>공유 게시글을 열 수 없습니다.</h2>
          <p class="message-muted">{{ errorMessage || shareStatusMessage || '공유 링크를 다시 확인해 주세요.' }}</p>
        </section>
      </template>

      <p v-if="errorMessage && shareStatus?.status === 'PASSWORD_REQUIRED'" class="message-error">
        {{ errorMessage }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.public-board-share-page {
  width: 100%;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px 16px;
  background: transparent;
  box-sizing: border-box;
}

.public-board-share-page__panel {
  width: min(920px, 100%);
  justify-self: center;
  padding: 28px;
  display: grid;
  gap: 24px;
  box-sizing: border-box;
}

.public-board-share-page__hero {
  display: grid;
  gap: 8px;
}

.public-board-share-page__hero h1,
.public-board-share-page__header h2,
.public-board-share-page__empty-state h2,
.public-board-share-page__password-section h2 {
  margin: 0;
}

.public-board-share-page__eyebrow,
.public-board-share-page__category {
  margin: 0;
  color: #7dd3fc;
  font-size: 0.85rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.public-board-share-page__header,
.public-board-share-page__section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.public-board-share-page__meta {
  display: grid;
  gap: 6px;
  color: var(--color-text-muted);
  text-align: right;
}

.public-board-share-page__content {
  min-height: 180px;
}

.public-board-share-page__attachment-section,
.public-board-share-page__comment-section,
.public-board-share-page__empty-state,
.public-board-share-page__password-section {
  display: grid;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 16px;
  background: rgba(15, 23, 42, 0.28);
}

.public-board-share-page__attachment-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.public-board-share-page__comment-list {
  display: grid;
  gap: 12px;
}

.public-board-share-page__attachment-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.3);
}

@media (max-width: 768px) {
  .public-board-share-page {
    padding: 20px 12px;
  }

  .public-board-share-page__panel {
    padding: 18px;
  }

  .public-board-share-page__header,
  .public-board-share-page__section-header,
  .public-board-share-page__attachment-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .public-board-share-page__meta {
    text-align: left;
  }
}
</style>
