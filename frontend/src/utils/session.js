// 로그인 세션(토큰·유저정보·비밀번호 강제변경 여부)을 브라우저 localStorage에 저장/조회/삭제하는 유틸.
// localStorage는 문자열만 저장 가능해서, 객체를 JSON으로 변환해 넣고 꺼낼 때 다시 파싱한다.
// 세션 데이터를 여기 한 곳에서만 다뤄서, localStorage 접근이 여러 파일에 흩어지지 않게 한다.
const KEY = 'ilog.session' // 토큰·유저·강제변경여부를 한 키에 묶어서 저장 (항상 같이 바뀌므로 따로 관리하면 꼬임)

export const session = {
  // 저장된 세션을 꺼낸다. 없거나 파싱 실패 시 null 반환 (= 비로그인 상태로 처리)
  get() {
    try {
      return JSON.parse(localStorage.getItem(KEY))
    } catch {
      return null
    }
  },

  // 로그인 성공 시 stores/auth.js 가 호출해서 세션을 저장한다
  set(value) {
    localStorage.setItem(KEY, JSON.stringify(value))
  },

  // 로그아웃 또는 토큰 만료 시 세션을 통째로 지운다
  clear() {
    localStorage.removeItem(KEY)
  },
}