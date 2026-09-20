// 서버는 UTC(...Z), 화면은 KST. 날짜 표시는 이 파일의 함수로만 한다.
const dateTimeFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit',
})
const dateFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
})

export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '')
export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '')

// 화면 표기는 2026.09.19 / 14:20 이라 점 표기 함수를 따로 둔다.
const partsFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})

const kstParts = (value) =>
  Object.fromEntries(partsFmt.formatToParts(new Date(value)).map((p) => [p.type, p.value]))

/** 2026.09.19 */
export const formatDotDate = (value) => {
  if (!value) return ''
  const p = kstParts(value)
  return `${p.year}.${p.month}.${p.day}`
}

/** 14:20 */
export const formatClock = (value) => {
  if (!value) return ''
  const p = kstParts(value)
  return `${p.hour}:${p.minute}`
}

/** KST 기준 오늘인지 */
export const isTodayKst = (value) => !!value && formatDotDate(value) === formatDotDate(Date.now())
