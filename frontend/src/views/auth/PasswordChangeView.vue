<script setup>
// 임시 비밀번호 로그인 후 새 비밀번호 설정을 처리함.
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/auth'
import FieldError from '@/components/common/FieldError.vue'
import { useDialog } from '@/composables/useDialog'
import { RULES } from '@/constants/rules'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const dialog = useDialog()

const forced = computed(() => auth.passwordResetRequired)
const form = reactive({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

const passwordChecks = computed(() => ({
  length: form.newPassword.length >= 8 && form.newPassword.length <= 20,
  lettersAndNumbers: /[A-Za-z]/.test(form.newPassword) && /\d/.test(form.newPassword),
  special: /[!@#$%^&*]/.test(form.newPassword),
}))

const passwordMatches = computed(
  () => !!form.newPasswordConfirm && form.newPassword === form.newPasswordConfirm,
)

// API 요청 전에 필수값·새 비밀번호 규칙·확인값 일치를 검사.
function validate() {
  const errors = {}

  if (!form.currentPassword) {
    errors.currentPassword = forced.value
      ? '임시 비밀번호는 필수입니다.'
      : '현재 비밀번호는 필수입니다.'
  }
  if (!form.newPassword) errors.newPassword = '새 비밀번호는 필수입니다.'
  else if (!RULES.PASSWORD_PATTERN.test(form.newPassword)) {
    errors.newPassword = '비밀번호 규칙을 모두 만족해야 합니다.'
  }
  if (!form.newPasswordConfirm) {
    errors.newPasswordConfirm = '새 비밀번호를 한 번 더 입력해 주세요.'
  } else if (!passwordMatches.value) {
    errors.newPasswordConfirm = '비밀번호가 일치하지 않습니다.'
  }

  fieldErrors.value = errors
  formError.value = Object.keys(errors).length ? '입력한 내용을 확인해 주세요.' : ''
  return Object.keys(errors).length === 0
}

async function onLogout() {
  await auth.logout()
  router.replace({ name: 'login' })
}

function goToServiceRoute(name) {
  if (router.hasRoute(name)) router.replace({ name })
}

function onCancel() {
  goToServiceRoute(router.hasRoute('mypage') ? 'mypage' : 'post-list')
}

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = {}
  if (!validate()) return

  // 강제 변경이 아닌 일반 변경은 사용자가 취소할 수 있도록 한 번 더 확인한다.
  if (!forced.value) {
    const confirmed = await dialog.confirm({
      title: '비밀번호를 변경하시겠습니까?',
      confirmText: '변경하기',
    })
    if (!confirmed) return
  }

  submitting.value = true
  try {
    await authApi.changePassword({ ...form })
    const wasForced = forced.value
    auth.markPasswordChanged()
    form.currentPassword = ''
    form.newPassword = ''
    form.newPasswordConfirm = ''
    await dialog.alert({ title: '비밀번호가 성공적으로 변경되었습니다.' })
    goToServiceRoute(wasForced ? 'post-list' : 'mypage')
  } catch (error) {
    // 현재 비밀번호 불일치는 인증 만료가 아니므로 로그인 세션을 유지합니다.
    formError.value = error.message
    fieldErrors.value = error.fieldErrors ?? {}
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-top" :class="{ 'is-plain': !forced }">
    <h1>비밀번호 변경</h1>
  </div>

  <form class="password-change" novalidate @submit.prevent="onSubmit">
    <p v-if="forced" class="notice notice-warn">임시 비밀번호로 로그인했어요. 새 비밀번호를 설정해 주세요.</p>

    <div class="field">
      <label class="label-lg" for="password-current">{{ forced ? '임시 비밀번호' : '현재 비밀번호' }}</label>
      <input
        id="password-current"
        v-model="form.currentPassword"
        class="box"
        type="password"
        autocomplete="current-password"
      />
      <FieldError :message="fieldErrors.currentPassword" />
    </div>

    <div class="field">
      <label class="label-lg" for="password-new">새 비밀번호</label>
      <input
        id="password-new"
        v-model="form.newPassword"
        class="box"
        type="password"
        autocomplete="new-password"
      />
      <ul class="rule-list" aria-label="비밀번호 규칙">
        <li :class="{ passed: passwordChecks.length }">8~20자</li>
        <li :class="{ passed: passwordChecks.lettersAndNumbers }">영문과 숫자 각각 1개 이상</li>
        <li :class="{ passed: passwordChecks.special }">특수문자 !@#$%^&amp;* 중 1개 이상</li>
      </ul>
      <FieldError :message="fieldErrors.newPassword" />
    </div>

    <div class="field">
      <label class="label-lg" for="password-confirm">새 비밀번호 확인</label>
      <input
        id="password-confirm"
        v-model="form.newPasswordConfirm"
        class="box"
        type="password"
        autocomplete="new-password"
      />
      <p v-if="passwordMatches" class="msg msg-ok">비밀번호가 일치합니다.</p>
      <FieldError :message="fieldErrors.newPasswordConfirm" />
    </div>

    <FieldError :message="formError" class="center" />
    <div class="actions">
      <button type="submit" class="btn btn-primary" :disabled="submitting">
        {{ submitting ? '변경 중' : '비밀번호 변경하기' }}
      </button>
      <button v-if="forced" type="button" class="btn" @click="onLogout">로그아웃</button>
      <button v-else type="button" class="btn" @click="onCancel">취소</button>
    </div>
  </form>
</template>

<style scoped>
.auth-top.is-plain { padding-top: 24px; border-bottom: 0; }
.password-change { width: 100%; max-width: 320px; margin: 40px auto 0; }
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
.actions { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-top: 36px; }
.center { text-align: center; }
</style>
