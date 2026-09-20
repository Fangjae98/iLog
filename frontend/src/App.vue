<template>
  <div class="page" :class="{ 'page-flush': route.name === 'login' }">
    <AppHeader v-if="showHeader" />
    <main class="page-body">
      <RouterView />
    </main>
  </div>
  <AppDialog />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/common/AppHeader.vue'
import AppDialog from '@/components/common/AppDialog.vue'

const route = useRoute()
const auth = useAuthStore()

// 헤더를 숨기는 경우: 비로그인 화면, 로그인 안 된 상태(404 등), 강제 비밀번호 변경 화면.
// 마이페이지에서 들어온 일반 비밀번호 변경 화면에는 헤더를 보여준다.
//
// 토큰 만료 판정(hasValidToken)은 여기서 하지 않는다. Date.now() 는 반응형이 아니라
// computed 안에 넣으면 결과가 캐시된다. 만료 처리는 라우터 가드(router/index.js)가 맡는다.
const showHeader = computed(() => {
  if (route.meta.guestOnly || !auth.accessToken) return false
  if (route.name === 'password-change' && auth.passwordResetRequired) return false
  return true
})
</script>
