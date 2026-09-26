// 인증 관련 API를 한 곳에 모으고, 환경 설정에 따라 mock 또는 실제 백엔드를 호출함
import client from './client'
import { MOCK_ENABLED, mockAuth } from './mock'

// VITE_USE_MOCK=true이면 mockAuth를, false이면 client를 통해 실제 백엔드를 사용
export const authApi = {
  // 이메일과 비밀번호를 전달하여 로그인한다
  login: (body) =>
    MOCK_ENABLED
      ? mockAuth.login(body)
      : client.post('/auth/tokens', body),
  // body: { email, password }

  // 현재 로그인 세션을 종료한다
  logout: () =>
    MOCK_ENABLED
      ? mockAuth.logout()
      : client.delete('/auth/tokens'),

  // 이메일과 이름을 확인하여 임시 비밀번호 발급을 요청한다
  issueTempPassword: (body) =>
    MOCK_ENABLED
      ? mockAuth.issueTempPassword(body)
      : client.post('/auth/temporary-passwords', body),
  // body: { email, name }

  // 현재 비밀번호가 일치하는지 확인한다
  verifyPassword: (body) =>
    MOCK_ENABLED
      ? mockAuth.verifyPassword(body)
      : client.post('/users/me/password-verification', body),
  // body: { password }

  // 현재 비밀번호를 확인한 후 새로운 비밀번호로 변경한다
  changePassword: (body) =>
    MOCK_ENABLED
      ? mockAuth.changePassword(body)
      : client.put('/users/me/password', body),
  // body: { currentPassword, newPassword, newPasswordConfirm }
}