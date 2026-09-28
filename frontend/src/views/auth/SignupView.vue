<script setup>
// 회원가입 화면: 입력값 검증, 중복 확인, 회원가입 요청을 처리합니다.
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import FieldError from '@/components/common/FieldError.vue'
import { useDialog } from '@/composables/useDialog'
import { RULES } from '@/constants/rules'

const router = useRouter()
const dialog = useDialog()

// 회원가입 입력값과 중복 확인 상태입니다.
const emailLocal = ref('')
const emailDomain = ref('gmail.com')
const customDomain = ref('')
const form = reactive({ password: '', passwordConfirm: '', name: '', nickname: '' })

const emailChecked = ref(false)
const emailStatus = ref('')
const nicknameChecked = ref(false)
const checkingEmail = ref(false)
const checkingNickname = ref(false)
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

// 이메일 전체 주소와 비밀번호 규칙 충족 여부를 계산합니다.
const domain = computed(() => emailDomain.value || customDomain.value)
const email = computed(() => `${emailLocal.value}@${domain.value}`)
const passwordMatches = computed(
  () => !!form.passwordConfirm && form.password === form.passwordConfirm,
)

const passwordChecks = computed(() => ({
  length: form.password.length >= 8 && form.password.length <= 20,
  lettersAndNumbers: /[A-Za-z]/.test(form.password) && /\d/.test(form.password),
  special: /[!@#$%^&*]/.test(form.password),
}))

watch(email, () => {
  emailChecked.value = false
  emailStatus.value = ''
})

watch(() => form.nickname, () => {
  nicknameChecked.value = false
})

function setFieldError(field, message = '') {
  fieldErrors.value = { ...fieldErrors.value, [field]: message }
}

// 이메일 형식을 확인한 뒤 사용 가능 여부를 조회합니다.
async function checkEmail() {
  setFieldError('email')

  if (!RULES.EMAIL_PATTERN.test(email.value)) {
    return setFieldError('email', '이메일 형식을 확인해 주세요.')
  }

  checkingEmail.value = true
  try {
    const { available, reason } = await userApi.checkEmail(email.value)
    emailChecked.value = available
    emailStatus.value = available ? 'AVAILABLE' : reason
  } catch (error) {
    emailChecked.value = false
    setFieldError('email', error.fieldErrors?.email ?? error.message)
  } finally {
    checkingEmail.value = false
  }
}

// 닉네임 형식을 확인한 뒤 사용 가능 여부를 조회합니다.
async function checkNickname() {
  setFieldError('nickname')

  if (!RULES.NICKNAME_PATTERN.test(form.nickname)) {
    return setFieldError('nickname', '닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다.')
  }

  checkingNickname.value = true
  try {
    const { available } = await userApi.checkNickname(form.nickname)
    nicknameChecked.value = available
    if (!available) setFieldError('nickname', '이미 사용 중인 닉네임입니다.')
  } catch (error) {
    nicknameChecked.value = false
    setFieldError('nickname', error.fieldErrors?.nickname ?? error.message)
  } finally {
    checkingNickname.value = false
  }
}

// 회원가입 요청 전에 필수값과 입력 규칙을 한 번에 검사합니다.
function validate() {
  const errors = {}

  if (!emailLocal.value || !domain.value) errors.email = '이메일은 필수입니다.'
  else if (!RULES.EMAIL_PATTERN.test(email.value)) errors.email = '이메일 형식을 확인해 주세요.'
  else if (!emailChecked.value) errors.email = '이메일 중복 확인을 해주세요.'
  if (!form.password) errors.password = '비밀번호는 필수입니다.'
  else if (!RULES.PASSWORD_PATTERN.test(form.password)) {
    errors.password = '비밀번호 규칙을 모두 만족해야 합니다.'
  }
  if (!form.passwordConfirm) errors.passwordConfirm = '비밀번호를 한 번 더 입력해 주세요.'
  else if (!passwordMatches.value) errors.passwordConfirm = '비밀번호가 일치하지 않습니다.'
  if (!form.name) errors.name = '이름은 필수입니다.'
  if (!form.nickname) errors.nickname = '닉네임은 필수입니다.'
  else if (!RULES.NICKNAME_PATTERN.test(form.nickname)) {
    errors.nickname = '닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다.'
  } else if (!nicknameChecked.value) errors.nickname = '닉네임 중복 확인을 해주세요.'

  fieldErrors.value = errors
  formError.value = Object.keys(errors).length ? '입력한 내용을 확인해 주세요.' : ''
  return Object.keys(errors).length === 0
}

// 회원가입 완료 후 안내 팝업을 표시하고 로그인 화면으로 이동합니다.
async function onSubmit() {
  formError.value = ''
  if (!validate()) return

  submitting.value = true
  try {
    await userApi.signup({
      email: email.value,
      password: form.password,
      name: form.name,
      nickname: form.nickname,
    })
    await dialog.alert({ title: '회원가입이 완료되었습니다.' })
    router.replace({ name: 'login' })
  } catch (error) {
    let handledInline = false

    if (error.code === 'USER_DUPLICATE_EMAIL') {
      emailChecked.value = false
      emailStatus.value = 'DUPLICATE'
      handledInline = true
    }
    if (error.code === 'REJOIN_RESTRICTED') {
      emailChecked.value = false
      emailStatus.value = 'WITHDRAWN'
      handledInline = true
    }
    if (error.code === 'USER_DUPLICATE_NICKNAME') {
      nicknameChecked.value = false
      handledInline = true
    }

    fieldErrors.value = error.fieldErrors ?? {}
    if (emailStatus.value === 'DUPLICATE' || emailStatus.value === 'WITHDRAWN') {
      fieldErrors.value = { ...fieldErrors.value, email: '' }
    }
    formError.value = handledInline ? '' : error.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-top">
    <h1>회원가입</h1>
  </div>

  <!-- 회원가입 정보를 입력하고 제출하는 폼입니다. -->
  <form class="signup" novalidate @submit.prevent="onSubmit">
    <!-- 이메일 조합과 중복 확인을 처리합니다. -->
    <div class="field">
      <label class="label" for="signup-email-local">아이디 (이메일)</label>
      <div class="email-row">
        <input
          id="signup-email-local"
          v-model.trim="emailLocal"
          class="box email-local"
          autocomplete="username"
          aria-label="이메일 아이디"
        />
        <span class="at" aria-hidden="true">@</span>
        <select v-model="emailDomain" class="box email-domain" aria-label="이메일 도메인">
          <option value="gmail.com">gmail.com</option>
          <option value="naver.com">naver.com</option>
          <option value="daum.net">daum.net</option>
          <option value="kakao.com">kakao.com</option>
          <option value="">직접 입력</option>
        </select>
        <button
          type="button"
          class="btn btn-sm"
          :disabled="checkingEmail"
          @click="checkEmail"
        >
          {{ checkingEmail ? '확인 중' : '중복 확인' }}
        </button>
      </div>
      <input
        v-if="emailDomain === ''"
        v-model.trim="customDomain"
        class="box custom-domain"
        placeholder="example.com"
        aria-label="이메일 도메인 직접 입력"
      />
      <p v-if="emailStatus === 'AVAILABLE'" class="msg msg-ok" role="status">사용 가능한 이메일입니다.</p>
      <p v-else-if="emailStatus === 'DUPLICATE'" class="msg msg-err" role="alert">이미 사용 중인 이메일입니다.</p>
      <p v-else-if="emailStatus === 'WITHDRAWN'" class="msg msg-err" role="alert">
        탈퇴한 계정의 이메일입니다. <RouterLink :to="{ name: 'login' }">로그인 화면으로</RouterLink>
      </p>
      <FieldError :message="fieldErrors.email" />
    </div>

    <!-- 입력할 때마다 비밀번호 규칙 충족 상태를 표시합니다. -->
    <div class="field">
      <label class="label" for="signup-password">비밀번호</label>
      <input
        id="signup-password"
        v-model="form.password"
        class="box"
        type="password"
        autocomplete="new-password"
      />
      <ul class="rule-list" aria-label="비밀번호 규칙">
        <li :class="{ passed: passwordChecks.length }">8~20자</li>
        <li :class="{ passed: passwordChecks.lettersAndNumbers }">영문과 숫자 각각 1개 이상</li>
        <li :class="{ passed: passwordChecks.special }">특수문자 !@#$%^&amp;* 중 1개 이상</li>
      </ul>
      <FieldError :message="fieldErrors.password" />
    </div>

    <div class="field">
      <label class="label" for="signup-password-confirm">비밀번호 확인</label>
      <input
        id="signup-password-confirm"
        v-model="form.passwordConfirm"
        class="box"
        type="password"
        autocomplete="new-password"
      />
      <p v-if="passwordMatches" class="msg msg-ok">비밀번호가 일치합니다.</p>
      <FieldError :message="fieldErrors.passwordConfirm" />
    </div>

    <div class="field">
      <label class="label" for="signup-name">이름</label>
      <input id="signup-name" v-model.trim="form.name" class="box" autocomplete="name" maxlength="50" />
      <p class="msg msg-hint">실명을 입력해 주세요.</p>
      <FieldError :message="fieldErrors.name" />
    </div>

    <div class="field">
      <label class="label" for="signup-nickname">닉네임</label>
      <div class="row">
        <input id="signup-nickname" v-model.trim="form.nickname" class="box" maxlength="10" />
        <button
          type="button"
          class="btn btn-sm"
          :disabled="checkingNickname"
          @click="checkNickname"
        >
          {{ checkingNickname ? '확인 중' : '중복 확인' }}
        </button>
      </div>
      <p v-if="nicknameChecked" class="msg msg-ok" role="status">사용 가능한 닉네임입니다.</p>
      <p class="msg msg-hint">2~10자의 한글, 영문, 숫자</p>
      <FieldError :message="fieldErrors.nickname" />
    </div>

    <FieldError :message="formError" />
    <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">
      {{ submitting ? '가입 중' : '가입하기' }}
    </button>

    <p class="foot">이미 계정이 있으십니까? <RouterLink :to="{ name: 'login' }">로그인</RouterLink></p>
  </form>
</template>

<style scoped>
.signup { width: 100%; max-width: 420px; margin: 36px auto 0; }
.email-row { display: flex; align-items: flex-end; gap: 8px; }
.email-local { flex: 1 1 100px; min-width: 0; }
.email-domain { flex: 1 1 124px; min-width: 0; }
.at { padding-bottom: 7px; font-family: var(--font-hand); font-size: var(--fs-xl); font-weight: 700; line-height: 1; }
.custom-domain { margin-top: 8px; }
/* 미충족 규칙은 빈 원, 충족한 규칙은 채운 원으로 표시합니다. */
.rule-list { display: grid; gap: 6px; margin-top: 8px; color: var(--text); font-size: var(--fs-xs); }
.rule-list li { display: flex; align-items: center; gap: 8px; }
.rule-list li::before {
  content: '';
  width: 13px;
  height: 13px;
  flex: 0 0 13px;
  border: 1px solid var(--text);
  border-radius: 50%;
  background: transparent;
}
.rule-list li.passed { color: var(--label); text-decoration: line-through; }
.rule-list li.passed::before { background: var(--text); }
.submit { margin-top: 36px; }
.foot { margin-top: 14px; text-align: center; color: var(--muted); font-size: var(--fs-xs); }

@media (max-width: 560px) {
  .signup { margin-top: 26px; }
  .email-row { flex-wrap: wrap; }
  .email-row .btn { width: 100%; }
}
</style>
