// 각 화면의 URL과 연결할 페이지를 등록
// 담당자는 본인 도메인 페이지 구현 시 아래 routes 배열에 라우트를 추가한다.

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guestOnly: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router