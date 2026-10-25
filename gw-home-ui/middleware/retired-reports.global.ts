import { isRetiredReportPath, retiredReportDestination } from '~/constants/retired-report-routes'

export default defineNuxtRouteMiddleware(to => {
  if (isRetiredReportPath(to.path)) {
    return navigateTo(retiredReportDestination, { replace: true })
  }
})
