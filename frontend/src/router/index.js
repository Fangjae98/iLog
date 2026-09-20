import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// @metric BUNDLE, LCP: component 를 동적 import 로 둬서 화면별로 JS를 나눠 받는다.
// 첫 화면에 필요한 청크만 내려받으므로 초기 로딩이 빨라진다.
const routes = [
  { path: '/', redirect: '/posts' },

  // 비로그인 전용
  { path: '/login', name: 'login', component: () => import('@/views/auth/LoginView.vue'), meta: { guestOnly: true } },
  { path: '/signup', name: 'signup', component: () => import('@/views/auth/SignupView.vue'), meta: { guestOnly: true } },
  { path: '/password/find', name: 'password-find', component: () => import('@/views/auth/PasswordFindView.vue'), meta: { guestOnly: true } },

  // 로그인 필요
  { path: '/password/change', name: 'password-change', component: () => import('@/views/auth/PasswordChangeView.vue'), meta: { requiresAuth: true } },
  { path: '/posts', name: 'post-list', component: () => import('@/views/post/PostListView.vue'), meta: { requiresAuth: true } },
  { path: '/posts/new', name: 'post-write', component: () => import('@/views/post/PostWriteView.vue'), meta: { requiresAuth: true } },
  { path: '/posts/:postId(\\d+)', name: 'post-detail', component: () => import('@/views/post/PostDetailView.vue'), props: true, meta: { requiresAuth: true } },
  { path: '/posts/:postId(\\d+)/edit', name: 'post-edit', component: () => import('@/views/post/PostEditView.vue'), props: true, meta: { requiresAuth: true } },
  { path: '/mypage', name: 'mypage', component: () => import('@/views/user/MyPageView.vue'), meta: { requiresAuth: true } },
  { path: '/about', name: 'about', component: () => import('@/views/AboutView.vue'), meta: { requiresAuth: true } },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFoundView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const loggedIn = auth.hasValidToken()

  // 만료된 토큰이 남아 있으면 정리
  if (!loggedIn && auth.accessToken) auth.clear()

  if (to.meta.requiresAuth && !loggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && loggedIn) {
    return { name: 'post-list' }
  }
  // 9/15 회의: 임시 비밀번호 로그인 시 이동 불가한 변경 화면 강제
  if (loggedIn && auth.passwordResetRequired && to.name !== 'password-change') {
    return { name: 'password-change' }
  }
})

export default router
