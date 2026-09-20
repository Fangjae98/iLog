import axios from 'axios'
import { session } from '@/utils/session'
import { resolveMessage } from '@/constants/errorMessages'

// 토큰 첨부, 봉투 해제, 에러 변환을 이 파일 한 곳에서만 한다. 컴포넌트는 봉투를 모른다.
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  // @metric FALLBACK: 서버가 느리거나 죽어도 10초 뒤에는 NETWORK_ERROR 문구가 나온다
  timeout: 10000,
  // 배열 파라미터를 ?hashtag=JWT&hashtag=Spring 으로 보낸다 (기본값은 hashtag[]=JWT)
  paramsSerializer: { indexes: null },
})

// 요청: 토큰 첨부
client.interceptors.request.use((config) => {
  const token = session.get()?.accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 응답: 성공이면 data만, 실패면 통일된 에러 객체로
client.interceptors.response.use(
  (res) => (res.status === 204 ? null : (res.data?.data ?? null)),
  (error) => {
    const res = error.response
    // @metric FALLBACK: 응답 자체가 없으면(서버 다운·네트워크 끊김) NETWORK_ERROR 로 변환한다
    const code = res ? (res.data?.error?.code ?? 'INTERNAL_ERROR') : 'NETWORK_ERROR'

    const fieldErrors = {}
    for (const d of (res?.data?.error?.details ?? [])) {
      fieldErrors[d.field] = d.reason // { nickname: '이미 사용 중입니다' }
    }

    // 세션 삭제는 UNAUTHORIZED 하나에서만.
    // LOGIN_FAILED·ACCOUNT_WITHDRAWN·PASSWORD_MISMATCH 도 401 이라 상태 코드로 지우면 안 된다.
    if (code === 'UNAUTHORIZED') {
      session.clear()
      // client.js 에서 router 를 import 하면 router → views → stores → api → client → router
      // 순환 참조가 생긴다. 전체 새로고침으로 가면 Pinia 상태도 깨끗하게 초기화된다.
      window.location.replace('/login?expired=1')
    }

    return Promise.reject({
      status: res?.status ?? 0,
      code,
      message: resolveMessage(code, res?.data?.error?.message),
      fieldErrors,
    })
  },
)

export default client
