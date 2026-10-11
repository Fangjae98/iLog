<!--
  앱 전체에서 공통으로 사용하는 로그인 사용자용 헤더
  로고·HOME/마이페이지 탭과 현재 로그인한 사용자의 닉네임·로그아웃 버튼을 표시하고,
  로그아웃하면 인증 정보와 브라우저 세션을 삭제한 뒤 로그인 화면으로 이동함.
-->

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const { user } = storeToRefs(auth) // 닉네임이 변경되면 화면에도 바로 반영
const { logout } = auth
const route = useRoute()
const router = useRouter()

// 현재 화면이 속한 탭을 표시한다. 글쓰기는 헤더가 아니라 목록 화면의 버튼에 둔다.
const tabs = computed(() => [
  { name: 'post-list', label: 'HOME', active: String(route.name).startsWith('post-') },
  { name: 'mypage', label: '마이페이지', active: route.name === 'mypage' || route.name === 'password-change' },
])

// 로그아웃 처리 후 이전 인증 화면을 기록에 남기지 않고 로그인 화면으로 이동
async function onLogout() {
  await logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <div class="app-header-inner">
      <RouterLink class="logo" :to="{ name: 'post-list' }">iLog</RouterLink>

      <nav class="tabs" aria-label="주요 메뉴">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.name"
          class="tab"
          :class="{ 'is-active': tab.active }"
          :to="{ name: tab.name }"
        >
          {{ tab.label }}
        </RouterLink>
      </nav>

      <div class="account">
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
  gap: 16px;
  max-width: var(--content-max);
  height: 50px;
  margin: 0 auto;
  padding: 0 40px;
}

.logo {
  flex: none;
  font-family: var(--font-hand);
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  text-decoration: none;
}

.tabs {
  display: flex;
  flex: 1;
  gap: 4px;
  min-width: 0;
}

.tab {
  padding: 4px 10px;
  border-radius: 2px;
  color: var(--sub);
  font-size: var(--fs-sm);
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
}

.tab:hover {
  color: var(--text);
}

.tab.is-active {
  background: var(--postit-pink);
  color: var(--postit-text);
  font-weight: 700;
}

.account {
  display: flex;
  flex: none;
  align-items: center;
  gap: 12px;
}

.nickname {
  color: var(--muted);
  font-size: 13px;
  white-space: nowrap;
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
