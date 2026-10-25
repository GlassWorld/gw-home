import { allNavigationItems } from '~/constants/navigation-menu'

interface NavigationFavoriteState {
  favoriteMenus: string[]
  isLoaded: boolean
  isSaving: boolean
}

export const useNavigationFavoriteStore = defineStore('navigationFavorite', {
  state: (): NavigationFavoriteState => ({
    favoriteMenus: [],
    isLoaded: false,
    isSaving: false
  }),

  actions: {
    setFavoriteMenus(favoriteMenus: string[]) {
      const allowedPaths = new Set(allNavigationItems.filter(item => item.favoriteEnabled).map(item => item.to))
      this.favoriteMenus = favoriteMenus.filter(menu => allowedPaths.has(menu))
      this.isLoaded = true
    },

    setSaving(isSaving: boolean) {
      this.isSaving = isSaving
    },

    clearFavoriteMenus() {
      this.favoriteMenus = []
      this.isLoaded = false
      this.isSaving = false
    }
  }
})
