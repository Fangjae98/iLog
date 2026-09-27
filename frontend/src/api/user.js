// 회원 도메인의 가입, 중복 확인, 정보 수정, 탈퇴 API를 관리한다.
import client from './client'
import { MOCK_ENABLED, mockUser } from './mock' // MOCK: 백엔드 연동이 끝나면 mock import와 분기 코드를 제거한다.

export const userApi = {
  // 회원가입
  // 요청 데이터: { email, password, name, nickname }
  signup: (body) =>
    MOCK_ENABLED ? mockUser.signup(body) : client.post('/users', body),

  // 이메일 중복 확인
  // 요청 데이터: email
  checkEmail: (email) =>
    MOCK_ENABLED
      ? mockUser.checkEmail(email)
      : client.get('/users/email-availability', { params: { email } }),

  // 닉네임 중복 확인
  // 요청 데이터: nickname
  checkNickname: (nickname) =>
    MOCK_ENABLED
      ? mockUser.checkNickname(nickname)
      : client.get('/users/nickname-availability', { params: { nickname } }),

  // 닉네임 수정
  // 요청 데이터: { nickname }
  updateNickname: (nickname) =>
    MOCK_ENABLED ? mockUser.updateNickname(nickname) : client.patch('/users/me', { nickname }),

  // 회원 탈퇴
  // 요청 데이터: { password }
  withdraw: (password) =>
    MOCK_ENABLED ? mockUser.withdraw(password) : client.post('/users/me/withdrawal', { password }),
}
