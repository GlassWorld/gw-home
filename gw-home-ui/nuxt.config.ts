export default defineNuxtConfig({
  modules: ['@pinia/nuxt'],
  css: ['~/assets/styles/main.css'],
  app: {
    head: {
      link: [
        {
          rel: 'icon',
          type: 'image/x-icon',
          href: '/favicon.ico'
        }
      ]
    }
  },
  devtools: {
    enabled: true
  },
  runtimeConfig: {
    public: {
      apiBase: process.env.NUXT_PUBLIC_API_BASE ?? 'http://localhost:8080'
    }
  },
  typescript: {
    strict: true,
    typeCheck: true
  }
})
