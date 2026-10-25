import { retiredReportDestination, retiredReportPaths } from './constants/retired-report-routes'

export default defineNuxtConfig({
  routeRules: Object.fromEntries(retiredReportPaths.flatMap(path => [
    [path, { redirect: { to: retiredReportDestination, statusCode: 302 } }],
    [`${path}/**`, { redirect: { to: retiredReportDestination, statusCode: 302 } }]
  ])),
  modules: ['@pinia/nuxt'],
  css: ['~/assets/styles/main.css'],
  app: {
    head: {
      title: 'Glass World',
      meta: [
        { name: 'viewport', content: 'width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content' }
      ],
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
