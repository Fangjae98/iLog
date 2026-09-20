<template>
  <div class="auth-top"><h1>회원가입</h1></div>

  <!-- @metric A11Y: 모든 입력칸에 <label for> 또는 aria-label 을 붙여 스크린리더가 칸 이름을 읽는다 -->
  <form class="signup" novalidate @submit.prevent="onSubmit">
    <div class="field">
      <label class="label" for="signup-email-local">아이디</label>
      <div class="email-row">
        <input id="signup-email-local" v-model.trim="emailLocal" class="box local" autocomplete="off" aria-label="이메일 아이디" />
        <span class="at" aria-hidden="true">@</span>
        <select v-model="emailDomain" class="box domain" aria-label="이메일 도메인">
          <option value="gmail.com">gmail.com</option>
          <option value="naver.com">naver.com</option>
          <option value="daum.net">daum.net</option>
          <option value="">직접 입력</option>
        </select>
        <button type="button" class="btn btn-sm" :disabled="!emailLocal || checkingEmail" @click="checkEmail">중복 확인</button>
      </div>
      <input v-if="emailDomain === ''" v-model.trim="customDomain" class="box custom-domain" placeholder="도메인 입력" aria-label="도메인 직접 입력" />
      <p v-if="emailStatus === 'OK'" class="msg msg-ok">사용 가능한 아이디입니다!</p>
      <p v-else-if="emailStatus === 'IN_USE'" class="msg msg-err">이미 존재하는 계정입니다. 로그인 페이지에서 로그인해주세요.</p>
      <p v-else-if="emailStatus === 'WITHDRAWN'" class="msg msg-err">
        탈퇴한 계정의 이메일이에요. <RouterLink :to="{ name: 'login' }">로그인 화면으로</RouterLink>
      </p>
      <FieldError :message="fieldErrors.email" />
    </div>

    <div class="field">
      <label class="label" for="signup-password">비밀번호</label>
      <input id="signup-password" v-model="form.password" class="box" type="password" autocomplete="new-password" />
      <PasswordRuleList :password="form.password" />
      <FieldError :message="fieldErrors.password" />
    </div>

    <div class="field">
      <label class="label" for="signup-password-confirm">비밀번호 확인</label>
      <input id="signup-password-confirm" v-model="form.passwordConfirm" class="box" type="password" autocomplete="new-password" />
      <p v-if="form.passwordConfirm && form.password === form.passwordConfirm" class="msg msg-ok">비밀번호가 일치합니다</p>
      <p v-else-if="form.passwordConfirm" class="msg msg-err">* 비밀번호가 일치하지 않습니다</p>
      <FieldError :message="fieldErrors.passwordConfirm" />
    </div>

    <div class="field">
      <label class="label" for="signup-name">이름</label>
      <input id="signup-name" v-model.trim="form.name" class="box" autocomplete="name" />
      <p class="msg msg-hint">실명을 입력해주세요. (필수)</p>
      <FieldError :message="fieldErrors.name" />
    </div>

    <div class="field">
      <label class="label" for="signup-nickname">닉네임</label>
      <div class="row">
        <input id="signup-nickname" v-model.trim="form.nickname" class="box" :maxlength="RULES.NICKNAME_MAX ?? undefined" />
        <button type="button" class="btn btn-sm" :disabled="!form.nickname || checkingNickname" @click="checkNickname">중복 확인</button>
      </div>
      <p v-if="nicknameChecked" class="msg msg-ok">사용 가능한 닉네임입니다!</p>
      <FieldError :message="fieldErrors.nickname" />
    </div>

    <div class="submit-row">
      <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
      <button type="submit" class="btn btn-primary" :disabled="!canSubmit || submitting">가입하기</button>
      <FieldError :message="formError" />
    </div>
    <p class="foot">이미 계정이 있으신가요? <RouterLink :to="{ name: 'login' }">로그인</RouterLink></p>
  </form>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import FieldError from '@/components/common/FieldError.vue'
import PasswordRuleList from '@/components/common/PasswordRuleList.vue'
import { useDialog } from '@/composables/useDialog'
import { userApi } from '@/api/user'
import { RULES } from '@/constants/rules'

const router = useRouter()
const dialog = useDialog()

// 아이디는 [로컬] @ [도메인] 으로 나눠 받고, API 에는 합친 email 하나를 보낸다
const emailLocal = ref('')
const emailDomain = ref('gmail.com')
const customDomain = ref('')
const email = computed(() => `${emailLocal.value}@${emailDomain.value || customDomain.value}`)

const form = reactive({ password: '', passwordConfirm: '', name: '', nickname: '' })
const emailChecked = ref(false)
const emailStatus = ref('')
const nicknameChecked = ref(false)
const checkingEmail = ref(false)
const checkingNickname = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

// 확인 후 값을 바꾸면 다시 확인하게 한다
watch(email, () => { emailChecked.value = false; emailStatus.value = '' })
watch(() => form.nickname, () => { nicknameChecked.value = false })

const canSubmit = computed(
  () => emailChecked.value && nicknameChecked.value && !!form.password && form.password === form.passwordConfirm,
)

async function checkEmail() {
  checkingEmail.value = true
  fieldErrors.value = { ...fieldErrors.value, email: '' }
  try {
    const { available, reason } = await userApi.checkEmail(email.value)
    emailChecked.value = available
    emailStatus.value = reason ?? 'OK' // 'OK' | 'IN_USE' | 'WITHDRAWN'
  } catch (e) {
    emailChecked.value = false
    emailStatus.value = ''
    fieldErrors.value = { ...fieldErrors.value, email: e.fieldErrors?.email ?? e.message }
  } finally {
    checkingEmail.value = false
  }
}

async function checkNickname() {
  checkingNickname.value = true
  fieldErrors.value = { ...fieldErrors.value, nickname: '' }
  try {
    const { available } = await userApi.checkNickname(form.nickname)
    nicknameChecked.value = available
    if (!available) fieldErrors.value = { ...fieldErrors.value, nickname: '이미 사용 중인 닉네임입니다' }
  } catch (e) {
    nicknameChecked.value = false
    fieldErrors.value = { ...fieldErrors.value, nickname: e.fieldErrors?.nickname ?? e.message }
  } finally {
    checkingNickname.value = false
  }
}

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = {}

  if (!emailLocal.value || !form.password || !form.passwordConfirm || !form.name || !form.nickname) {
    return (formError.value = '* 미입력 값이 있습니다.')
  }

  submitting.value = true
  try {
    await userApi.signup({
      email: email.value,
      password: form.password,
      passwordConfirm: form.passwordConfirm,
      name: form.name,
      nickname: form.nickname,
    })
    await dialog.alert({ title: '회원가입이 완료되었습니다.' })
    router.replace({ name: 'login' })
  } catch (e) {
    // 확인 버튼을 누른 뒤 가입 전에 다른 사람이 먼저 가입한 경우 확인 상태를 되돌린다
    if (e.code === 'EMAIL_DUPLICATED') { emailChecked.value = false; emailStatus.value = 'IN_USE' }
    if (e.code === 'NICKNAME_DUPLICATED') nicknameChecked.value = false
    formError.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.signup { width: 100%; max-width: 360px; margin: 40px auto 0; }
.email-row { display: flex; align-items: flex-end; gap: 8px; }
.email-row .local { flex: 1 1 100px; min-width: 0; }
.email-row .domain { flex: 1 1 120px; min-width: 0; }
.at { padding-bottom: 8px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-xl); line-height: 1; }
.custom-domain { margin-top: 6px; }
.submit-row { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-top: 40px; }
.submit-row .btn { flex: 1 1 200px; }
.submit-row .msg { margin: 0; }
.foot { margin-top: 14px; text-align: center; font-size: var(--fs-xs); color: var(--muted); }
</style>
