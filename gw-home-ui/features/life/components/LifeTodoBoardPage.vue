<script setup lang="ts">
import LifeTodoFormModal from './LifeTodoFormModal.vue'
import LifeCategoryModal from './LifeCategoryModal.vue'
import LifeDailyChecklist from './LifeDailyChecklist.vue'
import { useLifeTodos } from '../composables/use-life-todos'
import { lifeTodoGroups } from '../types/life-todo.types'

const route = useRoute()
const dailyView = ref(!route.query.group)

const { group, filters, page, result, summary, categories, isLoading, isSaving, isBusy, isCategoriesLoading,
  errorMessage, categoriesError, formError, formVisible, categoriesVisible, editingUuid, deleteTarget, form,
  refresh, loadCategories, applyFilters, selectGroup, changePage, openForm, closeForm, saveTodo, toggleCompletion, removeTodo, categoriesChanged } = useLifeTodos()
</script>

<template>
  <main class="page-container page-stack life-todos">
    <CommonBasePageHeader title="일상 할 일" eyebrow="Everyday" description="생활 속 작은 할 일을 가볍게 적고 하나씩 마무리하세요.">
      <template v-if="!dailyView" #actions>
        <CommonBaseButton variant="secondary" :disabled="isBusy" @click="categoriesVisible = true">카테고리 관리</CommonBaseButton>
        <CommonBaseButton :disabled="isBusy" @click="openForm()">+ 일상 할 일 추가</CommonBaseButton>
      </template>
    </CommonBasePageHeader>

    <nav class="life-todos__groups" aria-label="일상 보기">
      <button :aria-pressed="dailyView" @click="dailyView = true">매일 체크</button>
      <button :aria-pressed="!dailyView" @click="dailyView = false">일반 할 일</button>
    </nav>
    <LifeDailyChecklist v-if="dailyView" />
    <template v-else>

    <section v-if="summary" class="content-panel page-panel-padding-md life-todos__overview" aria-label="일상 완료 현황">
      <div><span>오늘 남은 일</span><strong>{{ summary.todayCount }}<small>건</small></strong></div>
      <div><span>현재 완료율</span><strong>{{ summary.progressRate }}<small>%</small></strong></div>
      <p class="section-description">{{ summary.asOfDate }} 기준 · 일상 할 일 {{ summary.totalCount }}건 중 {{ summary.completedCount }}건 완료</p>
    </section>

    <section class="content-panel page-panel-padding-md" aria-label="일상 할 일 검색">
      <form @submit.prevent="applyFilters">
        <fieldset class="life-todos__filters" :disabled="isBusy">
          <label>생활 카테고리
            <select v-model="filters.category" class="input-field" :disabled="isCategoriesLoading || !!categoriesError">
              <option value="">전체 카테고리</option><option value="UNCATEGORIZED">미분류</option>
              <option v-for="category in categories" :key="category.categoryUuid" :value="category.categoryUuid">{{ category.name }}</option>
            </select>
          </label>
          <label>검색<input v-model="filters.keyword" class="input-field" type="search" maxlength="200" placeholder="제목 또는 메모 검색"></label>
          <CommonBaseButton type="submit" variant="secondary">조회</CommonBaseButton>
        </fieldset>
      </form>
      <CommonBaseState v-if="categoriesError" state="error" :message="categoriesError" action-label="카테고리 다시 불러오기" @action="loadCategories" />
    </section>

    <nav class="life-todos__groups" aria-label="일상 할 일 분류">
      <button v-for="item in lifeTodoGroups" :key="item.value" :aria-pressed="group === item.value" :disabled="isBusy" @click="selectGroup(item.value)">
        {{ item.label }} <span v-if="summary">{{ summary[item.count] }}</span>
      </button>
    </nav>
    <CommonBaseState v-if="isLoading && !result" state="loading" message="일상 할 일을 불러오는 중입니다." />
    <CommonBaseState v-else-if="errorMessage" state="error" :message="errorMessage" action-label="다시 시도" @action="refresh" />
    <section v-else-if="result?.content.length" class="life-todos__list" aria-label="일상 할 일 목록" :aria-busy="isBusy">
      <article v-for="todo in result.content" :key="todo.todoUuid" class="content-panel page-panel-padding-md life-todos__card" :class="{ 'life-todos__card--done': todo.completed }">
        <div class="life-todos__copy">
          <div class="meta-row"><span>{{ todo.categoryName || '미분류' }}</span><span :class="{ 'life-todos__overdue': todo.group === 'OVERDUE' }">{{ lifeTodoGroups.find(item => item.value === todo.group)?.label }}</span></div>
          <h2>{{ todo.title }}</h2>
          <p v-if="todo.memo" class="life-todos__memo">{{ todo.memo }}</p>
          <p class="section-description">{{ todo.dueDate ? `일정 ${todo.dueDate}` : '일정 미지정' }}</p>
        </div>
        <div class="page-actions life-todos__actions">
          <CommonBaseButton :variant="todo.completed ? 'secondary' : 'primary'" :disabled="isBusy" :aria-label="`${todo.title} ${todo.completed ? '완료 취소' : '완료'}`" @click="toggleCompletion(todo)">{{ todo.completed ? '완료 취소' : '완료' }}</CommonBaseButton>
          <CommonBaseButton variant="secondary" :disabled="isBusy" :aria-label="`${todo.title} 수정`" @click="openForm(todo)">수정</CommonBaseButton>
          <CommonBaseButton variant="secondary" :disabled="isBusy" :aria-label="`${todo.title} 삭제`" @click="deleteTarget = todo">삭제</CommonBaseButton>
        </div>
      </article>
    </section>
    <CommonBaseState v-else state="empty" title="표시할 일상 할 일이 없습니다." message="새 할 일을 추가하거나 검색·분류 조건을 바꿔보세요." />
    <CommonBasePagination v-if="result && result.totalPages > 1" :page="page" :total-pages="result.totalPages" :disabled="isBusy" @previous="changePage(page - 1)" @next="changePage(page + 1)" />

    </template>
    <LifeTodoFormModal v-model="form" :visible="formVisible" :categories="categories" :is-editing="!!editingUuid" :is-saving="isSaving" :error-message="formError" @close="closeForm" @submit="saveTodo" />
    <LifeCategoryModal v-if="categoriesVisible" @close="categoriesVisible = false" @changed="categoriesChanged" />
    <CommonBaseModal :visible="!!deleteTarget" title="일상 할 일 삭제" width="480px" :dismissible="!isSaving" @close="deleteTarget = null">
      <p class="life-todos__delete-message">‘{{ deleteTarget?.title }}’을 삭제할까요?</p>
      <template #actions>
        <CommonBaseButton variant="secondary" :disabled="isSaving" @click="deleteTarget = null">취소</CommonBaseButton>
        <CommonBaseButton variant="danger" :loading="isSaving" @click="removeTodo">삭제</CommonBaseButton>
      </template>
    </CommonBaseModal>
  </main>
</template>

<style scoped>
.life-todos__overview { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--section-gap); }
.life-todos__overview span { display: block; color: var(--color-text-muted); }
.life-todos__overview strong { display: block; font-size: 1.8rem; }
.life-todos__overview small { margin-left: 4px; font-size: 0.9rem; }
.life-todos__overview > p { margin: 0; grid-column: 1 / -1; }
.life-todos__filters { display: grid; grid-template-columns: minmax(140px, 1fr) minmax(0, 2fr) auto; align-items: end; gap: 12px; border: 0; padding: 0; margin: 0; min-width: 0; }
.life-todos__filters label { display: grid; gap: 8px; min-width: 0; }
.life-todos__filters .input-field { width: 100%; min-width: 0; }
.life-todos__groups { display: flex; flex-wrap: wrap; gap: 8px; }
.life-todos__groups button { display: flex; gap: 6px; align-items: center; min-height: var(--control-height); padding: 8px 14px; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: var(--radius-small); color: var(--color-text-muted); font: inherit; cursor: pointer; }
.life-todos__groups button[aria-pressed="true"] { border-color: var(--color-accent); color: var(--color-accent); }
.life-todos__groups button:disabled { cursor: wait; }
.life-todos__list { display: grid; gap: 12px; }
.life-todos__card { display: flex; justify-content: space-between; align-items: flex-start; gap: var(--section-gap); }
.life-todos__copy { min-width: 0; overflow-wrap: anywhere; }
.life-todos__copy h2 { margin: 8px 0; font-size: 1.05rem; }
.life-todos__copy p { margin: 8px 0 0; }
.life-todos__memo { white-space: pre-wrap; color: var(--color-text-muted); }
.life-todos__card--done h2 { text-decoration: line-through; color: var(--color-text-muted); }
.life-todos__actions { flex-shrink: 0; }
.life-todos__overdue { color: var(--color-danger); }
.life-todos__delete-message { overflow-wrap: anywhere; }
@media (max-width: 767px) {
  .life-todos__filters { grid-template-columns: minmax(0, 1fr); }
  .life-todos__card { flex-direction: column; }
  .life-todos__actions { width: 100%; justify-content: flex-start; }
  .life-todos__actions :deep(.base-button) { flex: 1; }
}
</style>
