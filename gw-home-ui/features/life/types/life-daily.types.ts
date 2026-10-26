export interface LifeDailyItem { itemUuid: string; title: string; startDate: string; endDate: string | null }
export interface LifeDailyCheck { itemUuid: string; date: string }
export interface LifeDailyMonth { month: string; today: string; timeZone: string; items: LifeDailyItem[]; checks: LifeDailyCheck[] }
export interface LifeDailyDay { date: string; day: number; total: number; completed: number }
