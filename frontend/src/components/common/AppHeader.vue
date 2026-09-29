<!--
  앱 전체에서 공통으로 사용하는 로그인 사용자용 헤더
  마이페이지 이동 링크와 현재 로그인한 사용자의 닉네임·로그아웃 버튼을 표시하고,
  로그아웃하면 인증 정보와 브라우저 세션을 삭제한 뒤 로그인 화면으로 이동함.
-->

<script setup>
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const { user } = storeToRefs(auth) // 닉네임이 변경되면 화면에도 바로 반영
const { logout } = auth
const router = useRouter()

// 로그아웃 처리 후 이전 인증 화면을 기록에 남기지 않고 로그인 화면으로 이동
async function onLogout() {
  await logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <span class="logo">iLog</span>

      <div class="account">
        <RouterLink class="btn btn-sm" :to="{ name: 'mypage' }">
          마이페이지
        </RouterLink>

        <span v-if="user" class="nickname">
          {{ user.nickname }}님
        </span>

        <button type="button" class="btn btn-sm" @click="onLogout">
          로그아웃
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  border-bottom: 1px solid var(--line);
}

.app-header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: var(--content-max);
  height: 50px;
  margin: 0 auto;
  padding: 0 40px;
}

.logo {
  font-family: var(--font-hand);
  font-size: 26px;
  font-weight: 700;
}

.account {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nickname {
  color: var(--muted);
  font-size: 13px;
}

@media (max-width: 760px) {
  .app-header-inner {
    padding: 0 18px;
  }
}

@media (max-width: 560px) {
  .nickname {
    display: none;
  }
}
</style>
