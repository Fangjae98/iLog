// 백엔드 인증 담당 영역
import client from './client'
import { MOCK_ENABLED, mockAuth } from './mock' // MOCK: 백엔드 머지 후 이 줄을 지운다

export const authApi = {
  login: (body) =>
    MOCK_ENABLED ? mockAuth.login(body) : client.post('/auth/tokens', body), // { email, password }
  logout: () =>
    MOCK_ENABLED ? mockAuth.logout() : client.delete('/auth/tokens'),
  issueTempPassword: (body) =>
    MOCK_ENABLED ? mockAuth.issueTempPassword(body) : client.post('/auth/temporary-passwords', body), // { email, name }
  verifyPassword: (body) =>
    MOCK_ENABLED ? mockAuth.verifyPassword(body) : client.post('/users/me/password-verification', body), // { password }
  changePassword: (body) =>
    MOCK_ENABLED ? mockAuth.changePassword(body) : client.put('/users/me/password', body),
  // { currentPassword, newPassword, newPasswordConfirm }
}
