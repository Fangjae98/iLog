// 프론트의 모든 api 요청이 지나가는 출입구
// 화면 -> api 요청 (ex.auth.js) -> client.js -> axios -> 서버
// 주소 설정, 토큰 첨부, 성공/실패 데이터 정리 -> 백엔드
import axios from 'axios'
import { session } from '@/utils/session'
import { resolveMessage } from '@/constants/errorMessages'

// 서버 주소, 요청 제한 시간 등 모든 API의 공통 설정
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 10000,
  paramsSerializer: { indexes: null },
})

// 요청 전 저장된 Access Token을 Authorization 헤더에 첨부
client.interceptors.request.use((config) => {
  const token = session.get()?.accessToken

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

// 성공 응답은 실제 데이터만 반환하고, 실패 응답은 공통 오류 형태로 변환
client.interceptors.response.use(
  (res) => (res.status === 204 ? null : res.data),

  (error) => {
    const res = error.response

    // 서버 응답이 없으면 네트워크 오류로 처리
    const code = res
      ? (res.data?.code ?? 'INTERNAL_ERROR')
      : 'NETWORK_ERROR'

    // 백엔드의 입력 항목별 오류 배열을 화면에서 사용할 객체로 변환
    const fieldErrors = {}

    for (const item of res?.data?.errors ?? []) {
      fieldErrors[item.field] = item.reason
    }

    // 로그인 세션이 유효하지 않을 때만 저장된 세션을 삭제
    if (code === 'UNAUTHORIZED' || code === 'TOKEN_EXPIRED') {
      session.clear()
      window.location.replace('/login?expired=1')
    }

    // 모든 API 오류를 동일한 형태로 변환하여 호출한 곳으로 전달
    return Promise.reject({
      status: res?.status ?? 0,
      code,
      message: resolveMessage(code, res?.data?.message),
      fieldErrors,
    })
  },
)

export default client
