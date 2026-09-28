// 각 화면의 URL과 연결할 페이지를 등록
// 담당자는 본인 도메인 페이지 구현 시 아래 routes 배열에 라우트를 추가한다.

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignupView.vue'), meta: { guestOnly: true } },
  { path: '/password/find', name: 'password-find', component: () => import('@/views/auth/PasswordFindView.vue'), meta: { guestOnly: true } },
  { path: '/password/change', name: 'password-change', component: () => import('@/views/auth/PasswordChangeView.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
