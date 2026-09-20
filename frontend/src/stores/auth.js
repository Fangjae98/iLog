import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { session } from '@/utils/session'
import { MOCK_ENABLED, mockRestoreSession } from '@/api/mock' // MOCK: 백엔드 머지 후 이 줄을 지운다

const saved = session.get()

// MOCK: 새로고침해도 목업 로그인 상태가 유지되게 한다. 백엔드 머지 후 이 블록을 지운다.
if (MOCK_ENABLED && saved?.user?.userId) mockRestoreSession(saved.user.userId)

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: saved?.accessToken ?? null,
    expiresAt: saved?.expiresAt ?? 0,
    user: saved?.user ?? null, // { userId, nickname }
    passwordResetRequired: saved?.passwordResetRequired ?? false,
  }),

  actions: {
    // getter로 만들면 결과가 캐시되어 시간이 지나도 다시 계산되지 않는다 → 함수로 둔다
    hasValidToken() {
      return !!this.accessToken && Date.now() < this.expiresAt
    },

    async login(email, password) {
      const data = await authApi.login({ email, password })
      this.accessToken = data.accessToken
      this.expiresAt = Date.now() + data.expiresIn * 1000 // expiresIn 단위: 초
      this.user = data.user
      this.passwordResetRequired = data.passwordResetRequired
      this.persist()
      return data
    },

    async logout() {
      try {
        await authApi.logout() // 로그아웃 API 존치 여부 미결 — 실패해도 아래는 실행
      } catch {
        /* 무시 */
      } finally {
        this.clear()
      }
    },

    markPasswordChanged() {
      this.passwordResetRequired = false
      this.persist()
    },

    updateNickname(nickname) {
      this.user = { ...this.user, nickname }
      this.persist()
    },

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
