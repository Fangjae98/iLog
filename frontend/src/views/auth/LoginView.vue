<template>
  <div class="login">
    <section class="brand-area">
      <p class="brand-eyebrow">1일 1LOG</p>
      <!-- TODO U-5: 로고 이미지 확정 전까지 텍스트 -->
      <p class="brand">iLog</p>
      <p class="brand-sub">매일 한 개의 로그를 남기는 학습 게시판</p>
    </section>

    <!-- @metric A11Y: 모든 입력칸에 <label for> 를 붙여 스크린리더가 칸 이름을 읽는다 -->
    <form class="login-form" novalidate @submit.prevent="onSubmit">
      <h1>Login</h1>
      <p v-if="route.query.expired" class="notice">로그인이 만료되었어요. 다시 로그인해 주세요.</p>
      <p v-if="withdrawn" class="notice notice-warn">탈퇴 처리된 계정이에요.</p>
      <!-- TODO D-10: 계정 복구 버튼은 확정 후 추가 -->

      <div class="field">
        <label class="label" for="login-email">아이디 (이메일)</label>
        <input id="login-email" v-model.trim="form.email" class="box" type="email" autocomplete="username" />
        <FieldError :message="fieldErrors.email" />
      </div>
      <div class="field">
        <label class="label" for="login-password">비밀번호</label>
        <input id="login-password" v-model="form.password" class="box" type="password" autocomplete="current-password" />
        <FieldError :message="fieldErrors.password" />
      </div>

      <FieldError :message="formError" />
      <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
      <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">로그인</button>

      <p class="links">
        <RouterLink :to="{ name: 'password-find' }">비밀번호 찾기</RouterLink>
        <span aria-hidden="true">|</span>
        <RouterLink :to="{ name: 'signup' }">회원 가입</RouterLink>
      </p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FieldError from '@/components/common/FieldError.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ email: '', password: '' })
const submitting = ref(false)
const formError = ref('')
const withdrawn = ref(false)
const fieldErrors = ref({})

async function onSubmit() {
  formError.value = ''
  withdrawn.value = false
  fieldErrors.value = {}

  // 빈 값이면 API 를 부르지 않는다
  if (!form.email) return (formError.value = '아이디를 입력해주세요!')
  if (!form.password) return (formError.value = '비밀번호를 입력해주세요!')

  submitting.value = true
  try {
    const data = await auth.login(form.email, form.password)
    if (data.passwordResetRequired) return router.replace({ name: 'password-change' })

    // 외부 주소로 튕겨 나가지 않도록 내부 경로만 허용
    const redirect = route.query.redirect
    const safe = typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')
    router.replace(safe ? redirect : { name: 'post-list' })
  } catch (e) {
    if (e.code === 'ACCOUNT_WITHDRAWN') withdrawn.value = true
    else formError.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login { display: grid; grid-template-columns: minmax(300px, 38%) 1fr; min-height: 100vh; }
.brand-area { display: flex; flex-direction: column; justify-content: center; padding: 40px 56px; border-right: 1px solid var(--line); }
.brand-eyebrow { font-size: var(--fs-xs); font-weight: 700; color: var(--accent); }
.brand { margin: 4px 0 10px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-4xl); line-height: 58px; }
.brand-sub { font-size: var(--fs-sm); color: var(--sub); }
.login-form { width: 100%; max-width: 400px; margin: 0 auto; padding: 64px 48px 36px; align-self: center; }
.login-form h1 { margin-bottom: 24px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-3xl); line-height: 45px; }
.login-form .field { margin-bottom: 20px; }
.submit { margin-top: 20px; }
.links { display: flex; justify-content: center; gap: 8px; margin-top: 14px; font-size: var(--fs-xs); color: var(--muted); }
@media (max-width: 760px) {
  .login { grid-template-columns: 1fr; min-height: 0; }
  .brand-area { padding: 32px 22px 22px; border-right: 0; border-bottom: 1px solid var(--line); }
  .login-form { padding: 24px 22px 32px; align-self: start; }
}
</style>
