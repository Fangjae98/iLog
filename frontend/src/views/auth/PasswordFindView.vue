<!-- 사용자가 이메일과 이름을 입력하면, 프론트에서 먼저 검사한 뒤 임시 비밀번호 발급 API를 요청하고 결과 화면을 보여주는 컴포넌트 -->

<script setup>
// 비밀번호 찾기 화면: 회원 정보를 확인하고 임시 비밀번호 발급을 요청합니다.
import { reactive, ref } from 'vue'
import { authApi } from '@/api/auth'
import FieldError from '@/components/common/FieldError.vue'
import { RULES } from '@/constants/rules'

const form = reactive({ email: '', name: '' })
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})
const sentTo = ref('')

// API 요청 전에 필수값과 이메일 형식을 확인합니다.
function validate() {
  const errors = {}

  if (!form.email) errors.email = '이메일은 필수입니다.'
  else if (!RULES.EMAIL_PATTERN.test(form.email)) errors.email = '이메일 형식을 확인해 주세요.'
  if (!form.name) errors.name = '이름은 필수입니다.'

  fieldErrors.value = errors
  formError.value = Object.keys(errors).length ? '입력한 내용을 확인해 주세요.' : ''
  return Object.keys(errors).length === 0
}

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = {}
  if (!validate()) return

  submitting.value = true
  try {
    const data = await authApi.issueTempPassword({ email: form.email, name: form.name })
    sentTo.value = data.email
  } catch (error) {
    // 이메일과 이름 중 어느 값이 틀렸는지 구분해 노출하지 않습니다.
    formError.value = error.message
  } finally {
    submitting.value = false
  }
}
</script>

  <!-- @metric FALLBACK: 발송 완료 / 입력 폼 / 에러 문구를 모두 그린다 -->
  <!-- @metric A11Y: 모든 입력칸에 <label for> 를 붙인다 -->
<template>
  <div class="auth-top">
    <h1>비밀번호 찾기</h1>
  </div>

  <div class="password-find">
    <template v-if="sentTo">
      <!-- 응답에는 임시 비밀번호가 없다. 화면에 비밀번호를 보여주지 않는다. -->
      <!-- TODO D-17: 안내 문구 확정 후 교체 -->
      <p class="notice" role="status">{{ sentTo }}로 임시 비밀번호를 보냈어요.</p>
      <RouterLink class="btn btn-primary btn-block" :to="{ name: 'login' }">로그인하러 가기</RouterLink>
    </template>

    <form v-else novalidate @submit.prevent="onSubmit">
      <div class="field">
        <label class="label" for="password-find-email">아이디 (이메일)</label>
        <input
          id="password-find-email"
          v-model.trim="form.email"
          class="box"
          type="email"
          autocomplete="username"
        />
        <FieldError :message="fieldErrors.email" />
      </div>

      <div class="field">
        <label class="label" for="password-find-name">이름</label>
        <input
          id="password-find-name"
          v-model.trim="form.name"
          class="box"
          autocomplete="name"
          maxlength="50"
        />
        <FieldError :message="fieldErrors.name" />
      </div>

      <FieldError :message="formError" />
      <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">
        {{ submitting ? '발급 중' : '임시 비밀번호 받기' }}
      </button>
      <p class="foot"><RouterLink :to="{ name: 'login' }">로그인으로 돌아가기</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.password-find { width: 100%; max-width: 320px; margin: 40px auto 0; }
.submit { margin-top: 12px; }
.foot { margin-top: 14px; text-align: center; color: var(--muted); font-size: var(--fs-xs); }
</style>
