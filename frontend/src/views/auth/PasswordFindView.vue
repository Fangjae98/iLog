<template>
  <div class="auth-top"><h1>비밀번호 찾기</h1></div>

  <!-- @metric FALLBACK: 발송 완료 / 입력 폼 / 에러 문구를 모두 그린다 -->
  <!-- @metric A11Y: 모든 입력칸에 <label for> 를 붙인다 -->
  <div class="find">
    <template v-if="sentTo">
      <!-- 응답에는 임시 비밀번호가 없다. 화면에 비밀번호를 보여주지 않는다. -->
      <!-- TODO D-17: 안내 문구 확정 후 교체 -->
      <p class="notice">{{ sentTo }}로 임시 비밀번호를 보냈어요.</p>
      <RouterLink class="btn btn-primary btn-block" :to="{ name: 'login' }">로그인하러 가기</RouterLink>
    </template>

    <form v-else novalidate @submit.prevent="onSubmit">
      <div class="field">
        <label class="label" for="find-email">아이디 (이메일)</label>
        <input id="find-email" v-model.trim="form.email" class="box" type="email" autocomplete="username" />
        <FieldError :message="fieldErrors.email" />
      </div>
      <div class="field">
        <label class="label" for="find-name">이름</label>
        <input id="find-name" v-model.trim="form.name" class="box" autocomplete="name" />
        <FieldError :message="fieldErrors.name" />
      </div>
      <FieldError :message="formError" />
      <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
      <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">임시 비밀번호 받기</button>
      <p class="foot"><RouterLink :to="{ name: 'login' }">로그인으로 돌아가기</RouterLink></p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import { authApi } from '@/api/auth'

const form = reactive({ email: '', name: '' })
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})
const sentTo = ref('') // 응답의 마스킹된 data.email

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = {}
  if (!form.email || !form.name) return (formError.value = '* 미입력 값이 있습니다.')

  submitting.value = true
  try {
    const data = await authApi.issueTempPassword({ email: form.email, name: form.name })
    sentTo.value = data.email
  } catch (e) {
    // MEMBER_NOT_MATCHED: 이메일과 이름 중 어느 쪽이 틀렸는지 표시하지 않는다
    formError.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.find { width: 100%; max-width: 320px; margin: 40px auto 0; }
.submit { margin-top: 12px; }
.foot { margin-top: 14px; text-align: center; font-size: var(--fs-xs); color: var(--muted); }
</style>
