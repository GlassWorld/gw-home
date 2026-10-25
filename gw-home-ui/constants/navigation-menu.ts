export interface NavigationMenuItem {
  key: string
  to: string
  label: string
  adminOnly: boolean
  favoriteEnabled: boolean
}

export const primaryNavigationItems: NavigationMenuItem[] = [
  { key: 'dashboard', to: '/dashboard', label: '대시보드', adminOnly: false, favoriteEnabled: true },
  { key: 'notices', to: '/notices', label: '공지사항', adminOnly: false, favoriteEnabled: true },
  { key: 'board', to: '/board', label: '게시글', adminOnly: false, favoriteEnabled: true },
]

export const personalNavigationItems: NavigationMenuItem[] = [
  { key: 'work', to: '/work', label: '업무관리', adminOnly: false, favoriteEnabled: true },
  { key: 'work-todos', to: '/work/todos', label: '업무 할 일', adminOnly: false, favoriteEnabled: true },
  { key: 'life-todos', to: '/life/todos', label: '일상 할 일', adminOnly: false, favoriteEnabled: true },
  { key: 'work-gantt', to: '/work/gantt', label: '업무 간트차트', adminOnly: false, favoriteEnabled: true },
  { key: 'work-statistics', to: '/work/statistics', label: '업무 완료 통계', adminOnly: false, favoriteEnabled: true },
  { key: 'work-git-accounts', to: '/work/git-accounts', label: '깃 계정관리', adminOnly: false, favoriteEnabled: true },
  { key: 'vault', to: '/vault', label: '자격증명관리', adminOnly: false, favoriteEnabled: true },
  { key: 'settings', to: '/settings', label: '계정설정', adminOnly: false, favoriteEnabled: true },
  { key: 'security', to: '/security', label: '보안설정', adminOnly: false, favoriteEnabled: true }
]

export const adminNavigationItems: NavigationMenuItem[] = [
  { key: 'admin-accounts', to: '/admin/accounts', label: '계정관리', adminOnly: true, favoriteEnabled: true },
  { key: 'admin-board-categories', to: '/admin/board-categories', label: '게시판범주관리', adminOnly: true, favoriteEnabled: true },
  { key: 'admin-notices', to: '/admin/notices', label: '공지관리', adminOnly: true, favoriteEnabled: true },
  { key: 'admin-vault-categories', to: '/admin/vault-categories', label: '범주관리', adminOnly: true, favoriteEnabled: true }
]

export const allNavigationItems: NavigationMenuItem[] = [
  ...primaryNavigationItems,
  ...personalNavigationItems,
  ...adminNavigationItems
]

export const maxHeaderFavoriteMenuCount = 5
export const maxFavoriteMenuCount = 5
