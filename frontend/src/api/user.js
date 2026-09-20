// 백엔드 회원 담당 영역
import client from './client'
import { MOCK_ENABLED, mockUser } from './mock' // MOCK: 백엔드 머지 후 이 줄을 지운다

export const userApi = {
  signup: (body) =>
    MOCK_ENABLED ? mockUser.signup(body) : client.post('/users', body),
  // { email, password, passwordConfirm, name, nickname }
  checkEmail: (email) =>
    MOCK_ENABLED ? mockUser.checkEmail(email) : client.get('/users/email-availability', { params: { email } }),
  checkNickname: (nickname) =>
    MOCK_ENABLED ? mockUser.checkNickname(nickname) : client.get('/users/nickname-availability', { params: { nickname } }),
  updateNickname: (nickname) =>
    MOCK_ENABLED ? mockUser.updateNickname(nickname) : client.patch('/users/me', { nickname }),
  withdraw: (password) =>
    MOCK_ENABLED ? mockUser.withdraw(password) : client.post('/users/me/withdrawal', { password }),
}
