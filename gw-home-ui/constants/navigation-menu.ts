export interface NavigationMenuItem {
  key: string
  to: string
  label: string
  adminOnly: boolean
  favoriteEnabled: boolean
}

export const primaryNavigationItems: NavigationMenuItem[] = [
  { key: 'notices', to: '/notices', label: '공지사항', adminOnly: false, favoriteEnabled: true },
  { key: 'board', to: '/board', label: '게시글', adminOnly: false, favoriteEnabled: true },
  { key: 'weekly-reports', to: '/work/weekly-reports', label: '주간보고', adminOnly: false, favoriteEnabled: true }
]

export const personalNavigationItems: NavigationMenuItem[] = [
  { key: 'work', to: '/work', label: '업무관리', adminOnly: false, favoriteEnabled: true },
  { key: 'daily-reports', to: '/work/daily-reports', label: '일일보고관리', adminOnly: false, favoriteEnabled: true },
  { key: 'vault', to: '/vault', label: '자격증명관리', adminOnly: false, favoriteEnabled: true }
]

export const adminNavigationItems: NavigationMenuItem[] = [
  { key: 'admin-accounts', to: '/admin/accounts', label: '계정관리', adminOnly: true, favoriteEnabled: true },
  { key: 'admin-notices', to: '/admin/notices', label: '공지관리', adminOnly: true, favoriteEnabled: true },
  { key: 'admin-vault-categories', to: '/admin/vault-categories', label: '범주관리', adminOnly: true, favoriteEnabled: true }
]

export const allNavigationItems: NavigationMenuItem[] = [
  ...primaryNavigationItems,
  ...personalNavigationItems,
  ...adminNavigationItems
]

export const maxHeaderFavoriteMenuCount = 4
