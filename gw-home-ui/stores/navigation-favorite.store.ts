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
      this.favoriteMenus = [...favoriteMenus]
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
