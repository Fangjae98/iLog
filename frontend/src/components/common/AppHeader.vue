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
    <span v-if="user" class="nickname">{{ user.nickname }}님</span>
    <button type="button" class="btn btn-sm" @click="onLogout">로그아웃</button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const { user } = storeToRefs(auth) // 값은 storeToRefs 로 꺼내야 닉네임 수정 후 즉시 바뀜
const { logout } = auth            // 액션은 그냥 꺼내도 됨
const route = useRoute()
const router = useRouter()

// 글쓰기는 헤더가 아니라 목록 화면의 `+ 글쓰기` 버튼에 둔다 (UI 지침 6.1)
const tabs = computed(() => [
  { name: 'post-list', label: 'HOME', active: String(route.name).startsWith('post') },
  { name: 'about', label: 'ABOUT', active: route.name === 'about' },
  { name: 'mypage', label: '마이페이지', active: route.name === 'mypage' || route.name === 'password-change' },
])

async function onLogout() {
  await logout()
  router.replace({ name: 'login' })
}
</script>

<style scoped>
/* 구분선은 화면 끝까지, 내용은 본문과 같은 폭으로 중앙에 모은다 */
.app-header { border-bottom: 1px solid var(--line); }
.app-header-inner {
  display: flex; align-items: center; gap: 16px; height: 50px;
  max-width: var(--content-max); margin: 0 auto; padding: 0 40px;
}
@media (max-width: 760px) { .app-header-inner { padding: 0 18px; } }
.logo { flex: none; font-family: var(--font-hand); font-weight: 700; font-size: 26px; line-height: 1; text-decoration: none; }
.tabs { display: flex; flex: 1; gap: 4px; min-width: 0; }
.tab { padding: 4px 10px; border-radius: 2px; font-size: var(--fs-sm); font-weight: 500; color: var(--sub); text-decoration: none; white-space: nowrap; }
.tab:hover { color: var(--text); }
.tab.is-active { background: var(--postit-pink); color: var(--postit-text); font-weight: 700; }
.nickname { font-size: 13px; color: var(--muted); white-space: nowrap; }
@media (max-width: 560px) { .nickname { display: none; } }
</style>
