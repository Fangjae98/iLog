// (로그인) 인증 관련 상태를 관리하는 Pinia store

import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { session } from '@/utils/session'
import { MOCK_ENABLED, mockRestoreSession } from '@/api/mock' // MOCK: 백엔드 머지 후 이 줄을 지운다

// 로그인 세션을 브라우저 localStorage에 저장/조회/삭제하는 유틸
const saved = session.get()

// MOCK: 새로고침해도 목업 로그인 상태가 유지되게 한다. 백엔드 머지 후 이 블록을 지운다.
if (MOCK_ENABLED && saved?.user?.userId) mockRestoreSession(saved.user.userId)

// 로그인 세션을 Pinia store에 저장, localStorage에도 저장. 로그아웃 시 둘 다 삭제
export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: saved?.accessToken ?? null,
    expiresAt: saved?.expiresAt ?? 0,
    user: saved?.user ?? null, // { userId, nickname }
    passwordResetRequired: saved?.passwordResetRequired ?? false,
  }),

  actions: {
    // 토큰 유효 여부를 확인하는 getter를 만들면, 결과가 캐시되어 시간이 지나도 다시 계산되지 않아서 함수로 둔다.
    hasValidToken() {
      return !!this.accessToken && Date.now() < this.expiresAt
    },

    // 로그인 API를 호출하고, 성공하면 토큰·유저정보·비밀번호 강제변경 여부를 저장
    async login(email, password) {
      const data = await authApi.login({ email, password })
      this.accessToken = data.accessToken
      this.expiresAt = Date.now() + data.expiresIn * 1000 // expiresIn 단위: 초
      this.user = data.user
      this.passwordResetRequired = data.passwordResetRequired // 임시 비밀번호로 로그인했으면 true, 아니면 false
      this.persist()
      return data
    },

    // 로그아웃 API를 호출하고, 성공/실패와 상관없이 토큰·유저정보·비밀번호 강제변경 여부를 삭제
    async logout() {
      try {
        await authApi.logout() // 로그아웃 API 존치 여부 미결 — 실패해도 아래는 실행
      } catch {
        // 서버 로그아웃에 실패해도 무시
      } finally {
        this.clear()
      }
    },

    // 비밀번호/닉네임 변경 후 세션을 갱신한다. (로그인 상태 유지)
    markPasswordChanged() {
      this.passwordResetRequired = false
      this.persist()
    },

    updateNickname(nickname) {
      this.user = { ...this.user, nickname }
      this.persist()
    },

    // 로그인 성공 시 stores/auth.js 가 호출해서 세션을 저장
    persist() {
      session.set({
        accessToken: this.accessToken,
        expiresAt: this.expiresAt,
        user: this.user,
        passwordResetRequired: this.passwordResetRequired,
      })
    },

    clear() {
      // $reset()은 state()를 다시 실행하는데, 이 store의 state()는 모듈 로드 시점의
      // saved 를 읽는다. 그래서 $reset() 뒤에 값을 명시적으로 비운다.
      this.$reset()
      this.accessToken = null
      this.expiresAt = 0
      this.user = null
      this.passwordResetRequired = false
      session.clear()
    },
  },
})
