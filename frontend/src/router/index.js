// 각 화면의 URL과 연결할 페이지를 등록
// 담당자는 본인 도메인 페이지 구현 시 아래 routes 배열에 라우트를 추가한다.

import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  // 비로그인 전용
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignupView.vue'), meta: { guestOnly: true } },
  { path: '/password/find', name: 'password-find', component: () => import('@/views/auth/PasswordFindView.vue'), meta: { guestOnly: true } },

  // 로그인 필요
  { path: '/password/change', name: 'password-change', component: () => import('@/views/auth/PasswordChangeView.vue'), meta: { requiresAuth: true } },
  { path: '/mypage', name: 'mypage', component: () => import('@/views/user/MyPageView.vue'), meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const loggedIn = auth.hasValidToken()

  // 만료된 로그인 정보가 브라우저에 남아 있으면 먼저 정리한다.
  if (!loggedIn && auth.accessToken) auth.clear()

  // 로그인이 필요한 화면은 로그인 후 원래 주소로 돌아올 수 있게 redirect를 남긴다.
  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 임시 비밀번호 사용자는 비밀번호 변경을 마치기 전까지 다른 화면으로 갈 수 없다.
  if (loggedIn && auth.passwordResetRequired && to.name !== 'password-change') {
    return { name: 'password-change' }
  }

  // 게시글 화면이 다른 브랜치에서 병합되면 별도 수정 없이 로그인 화면 접근을 막는다.
  if (loggedIn && to.meta.guestOnly && router.hasRoute('post-list')) {
    return { name: 'post-list' }
  }
})

export default router
