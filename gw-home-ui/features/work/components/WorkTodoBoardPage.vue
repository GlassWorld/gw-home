<script setup lang="ts">
import type { WorkUnit, WorkUnitStatus } from '../types/work.types'
import { useWorkUnitApi } from '../api/work-unit.api'

const { fetchWorkUnitList } = useWorkUnitApi()
const { showToast } = useToast()

const workUnitList = ref<WorkUnit[]>([])
const isLoading = ref(false)

const statusLabelMap: Record<WorkUnitStatus, string> = {
  IN_PROGRESS: '진행중',
  DONE: '완료',
  ON_HOLD: '보류'
}

async function loadWorkUnits() {
  isLoading.value = true

  try {
    workUnitList.value = await fetchWorkUnitList({
      useYn: 'Y',
      sort: 'updated'
    })
  } catch (error) {
    const fetchError = error as { data?: { message?: string } }
    showToast(fetchError.data?.message ?? '업무 목록을 불러오지 못했습니다.', { variant: 'error' })
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadWorkUnits()
})
</script>

<template>
  <main class="work-todo-board-page page-container">
    <section class="content-panel work-todo-board-page__hero">
      <div>
        <p class="work-todo-board-page__eyebrow">Work Todo</p>
        <h1 class="section-title">업무 세부 작업</h1>
        <p class="section-description">
          등록된 업무를 선택해 연관 TODO 트리, 진행률, 일정 상태를 함께 관리합니다.
        </p>
      </div>

      <CommonBaseButton variant="secondary" to="/work">
        업무관리로 이동
      </CommonBaseButton>
    </section>

    <section class="content-panel work-todo-board-page__panel">
      <div class="work-todo-board-page__panel-header">
        <div>
          <h2 class="section-title">업무 선택</h2>
          <p class="section-description">세부 작업을 관리할 업무를 선택하세요.</p>
        </div>
      </div>

      <p v-if="isLoading" class="message-muted">
        업무 목록을 불러오는 중입니다.
      </p>

      <div v-else-if="workUnitList.length" class="work-todo-board-page__list">
        <article
          v-for="workUnit in workUnitList"
          :key="workUnit.workUnitUuid"
          class="work-todo-board-page__card"
        >
          <div class="work-todo-board-page__card-main">
            <div class="work-todo-board-page__card-head">
              <strong>{{ workUnit.title }}</strong>
              <span class="work-todo-board-page__badge">
                {{ statusLabelMap[workUnit.status] }}
              </span>
            </div>
            <p class="work-todo-board-page__card-description">
              {{ workUnit.description || '설명 없음' }}
            </p>
            <div class="work-todo-board-page__meta">
              <span><em>카테고리</em> {{ workUnit.category || '-' }}</span>
              <span><em>최근수정</em> {{ workUnit.updatedAt.slice(0, 10) }}</span>
            </div>
          </div>

          <CommonBaseButton :to="`/work/todos/${workUnit.workUnitUuid}`">
            세부 작업 열기
          </CommonBaseButton>
        </article>
      </div>

      <section v-else class="work-todo-board-page__empty">
        <h3>활성 업무가 없습니다.</h3>
        <p class="section-description">먼저 업무관리 화면에서 업무를 등록한 뒤 세부 작업을 추가하세요.</p>
        <CommonBaseButton to="/work">
          업무관리로 이동
        </CommonBaseButton>
      </section>
    </section>
  </main>
</template>

<style scoped>
.work-todo-board-page {
  display: grid;
  gap: 24px;
}

.work-todo-board-page__hero,
.work-todo-board-page__panel,
.work-todo-board-page__empty {
  padding: 22px;
}

.work-todo-board-page__hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}

.work-todo-board-page__eyebrow {
  margin: 0 0 6px;
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.work-todo-board-page__panel {
  display: grid;
  gap: 18px;
}

.work-todo-board-page__list {
  display: grid;
  gap: 14px;
}

.work-todo-board-page__card {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid rgba(147, 210, 255, 0.16);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.04);
}

.work-todo-board-page__card-main {
  display: grid;
  gap: 10px;
}

.work-todo-board-page__card-head {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.work-todo-board-page__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.work-todo-board-page__card-description {
  margin: 0;
  color: var(--color-text-muted);
}

.work-todo-board-page__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: var(--color-text-muted);
  font-size: 0.88rem;
}

.work-todo-board-page__meta em {
  font-style: normal;
  color: rgba(147, 210, 255, 0.72);
  font-weight: 700;
}

.work-todo-board-page__empty {
  display: grid;
  gap: 12px;
  justify-items: start;
  border: 1px dashed rgba(147, 210, 255, 0.22);
  border-radius: var(--radius-medium);
  background: rgba(255, 255, 255, 0.03);
}

@media (max-width: 768px) {
  .work-todo-board-page__hero,
  .work-todo-board-page__card {
    flex-direction: column;
  }
}
</style>
