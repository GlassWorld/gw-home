<script setup lang="ts">
import type { AdminAccount, AdminAccountListPage } from '~/types/admin'

const props = defineProps<{
  accountPage: AdminAccountListPage
  isLoading: boolean
  formatDateTime: (value: string) => string
  currentPage: number
}>()

const emit = defineEmits<{
  manage: [account: AdminAccount]
  previous: []
  next: []
}>()
</script>

<template>
  <section class="admin-accounts-table">
    <div class="admin-accounts-table__summary">
      <strong>총 {{ props.accountPage.totalCount }}건</strong>
      <span v-if="props.isLoading">불러오는 중...</span>
    </div>

    <div class="admin-accounts-table__table-wrap">
      <table class="admin-accounts-table__table">
        <thead>
          <tr>
            <th>로그인 ID</th>
            <th>이메일</th>
            <th>권한</th>
            <th>상태</th>
            <th>잠금</th>
            <th>가입일</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="account in props.accountPage.content" :key="account.uuid">
            <td>{{ account.loginId }}</td>
            <td>{{ account.email }}</td>
            <td><span class="admin-accounts-table__state-badge">{{ account.role }}</span></td>
            <td><span class="admin-accounts-table__state-badge admin-accounts-table__state-badge--status">{{ account.acctStat }}</span></td>
            <td>
              <span :class="['admin-accounts-table__lock-badge', { 'admin-accounts-table__lock-badge--locked': account.lckYn }]">
                {{ account.lckYn ? '잠김' : '정상' }}
              </span>
            </td>
            <td>{{ props.formatDateTime(account.createdAt) }}</td>
            <td>
              <CommonBaseButton variant="secondary" size="small" @click="emit('manage', account)">
                관리
              </CommonBaseButton>
            </td>
          </tr>
          <tr v-if="!props.isLoading && props.accountPage.content.length === 0">
            <td colspan="7" class="admin-accounts-table__empty">
              조회된 계정이 없습니다.
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <CommonBasePagination
      :page="props.accountPage.page"
      :total-pages="props.accountPage.totalPages"
      @previous="emit('previous')"
      @next="emit('next')"
    />
  </section>
</template>

<style scoped>
.admin-accounts-table__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.admin-accounts-table__summary span {
  color: var(--color-text-muted);
}

.admin-accounts-table__table-wrap {
  overflow: visible;
}

.admin-accounts-table__table {
  width: 100%;
  border-collapse: collapse;
}

.admin-accounts-table__table th,
.admin-accounts-table__table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(147, 210, 255, 0.12);
  text-align: left;
  vertical-align: middle;
}

.admin-accounts-table__table th {
  color: var(--color-text-muted);
  font-size: 0.86rem;
  font-weight: 700;
}

.admin-accounts-table__state-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 74px;
  padding: 5px 12px;
  border: 1px solid rgba(124, 209, 255, 0.2);
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: rgba(222, 242, 255, 0.96);
  font-size: 0.8rem;
  font-weight: 700;
}

.admin-accounts-table__state-badge--status {
  background: rgba(148, 235, 184, 0.12);
  border-color: rgba(148, 235, 184, 0.2);
  color: rgba(210, 255, 227, 0.96);
}

.admin-accounts-table__lock-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(110, 193, 255, 0.12);
  color: var(--color-accent);
  font-size: 0.85rem;
  font-weight: 700;
}

.admin-accounts-table__lock-badge--locked {
  background: rgba(255, 143, 143, 0.16);
  color: #ff9f9f;
}

.admin-accounts-table__empty {
  color: var(--color-text-muted);
  text-align: center;
}

@media (max-width: 900px) {
  .admin-accounts-table__table-wrap {
    overflow-x: auto;
  }

  .admin-accounts-table__summary {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
