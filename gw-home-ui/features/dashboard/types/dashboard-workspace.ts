export type DashboardTone = 'primary' | 'success' | 'warning' | 'danger' | 'neutral'

export interface DashboardTaskRow {
  workUnitUuid: string
  title: string
  category: string | null
  statusLabel: string
  statusTone: DashboardTone
  progressRate: number
  delayedCount: number
  latestReportDate: string | null
  updatedAt: string
}

export interface DashboardMetricCard {
  key: string
  label: string
  value: string
  hint: string
  tone: DashboardTone
}

export interface DashboardRecentReportItem {
  uuid: string
  title: string
  weekLabel: string
  openLabel: string
  publishedLabel: string
  excerpt: string
}

export interface DashboardIssueItem {
  key: string
  title: string
  description: string
  meta: string
  badge: string
  tone: DashboardTone
  to: string
}

export interface DashboardScheduleItem {
  key: string
  title: string
  workUnitTitle: string
  dueDateLabel: string
  statusLabel: string
  tone: DashboardTone
  to: string
}

export interface DashboardUpdateItem {
  key: string
  typeLabel: string
  title: string
  meta: string
  tone: DashboardTone
  to?: string
  noticeUuid?: string
}
