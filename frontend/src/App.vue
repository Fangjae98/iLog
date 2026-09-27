<!--
  앱 전체의 공통 화면 구조를 관리하는 최상위 컴포넌트
  현재 라우트가 로그인 필요 화면이면 공통 헤더를 표시하고,
  RouterView에 현재 주소의 화면을 렌더링하며 공통 다이얼로그를 함께 배치.
-->

<script setup>
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppDialog from '@/components/common/AppDialog.vue'

const route = useRoute()
</script>

<template>
  <!-- 로그인 화면은 전체 너비를 사용하고, 나머지 화면은 기본 페이지 틀을 사용 -->
  <div class="page" :class="{ 'page-flush': route.name === 'login' }">
    <!-- 로그인한 사용자만 접근하는 화면에 공통 헤더 표시 -->
    <AppHeader v-if="route.meta.requiresAuth" />

    <!-- 현재 URL과 연결된 화면 표시 -->
    <main class="page-body">
      <RouterView />
    </main>
  </div>

  <!-- 앱 전체에서 공통으로 사용하는 알림·확인 모달 -->
  <AppDialog />
</template>
