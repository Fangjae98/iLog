// 세션은 한 키에 묶어서 저장한다 (토큰·사용자·강제변경 여부가 항상 같이 바뀌므로)
const KEY = 'ilog.session'

export const session = {
  get() {
    try {
      return JSON.parse(localStorage.getItem(KEY))
    } catch {
      return null
    }
  },
  set(value) {
    localStorage.setItem(KEY, JSON.stringify(value))
  },
  clear() {
    localStorage.removeItem(KEY)
  },
}
