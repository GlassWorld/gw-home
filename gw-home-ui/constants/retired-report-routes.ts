// 저장된 보고서 링크는 인증이 필요한 업무 할 일 화면의 종료 안내로 연결한다.
export const retiredReportPaths = [
  '/weekly-reports',
  '/work/daily-reports',
  '/work/weekly-reports',
  '/admin/daily-reports'
] as const

export const retiredReportDestination = '/work/todos?notice=reports-retired'

export function isRetiredReportPath(path: string): boolean {
  return retiredReportPaths.some(prefix => path === prefix || path.startsWith(`${prefix}/`))
}
