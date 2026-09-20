<template>
  <div class="auth-top" :class="{ 'is-plain': !forced }"><h1>비밀번호 변경</h1></div>

  <!-- @metric A11Y: 모든 입력칸에 <label for> 를 붙인다 -->
  <form class="pw-change" novalidate @submit.prevent="onSubmit">
    <p v-if="forced" class="notice notice-warn">임시 비밀번호로 로그인했어요. 새 비밀번호를 설정해 주세요.</p>

    <div class="field">
      <label class="label-lg" for="pw-current">{{ forced ? '임시 비밀번호' : '현재 비밀번호' }}</label>
      <input id="pw-current" v-model="form.currentPassword" class="box" type="password" autocomplete="current-password" />
      <FieldError :message="fieldErrors.currentPassword" />
    </div>
    <div class="field">
      <label class="label-lg" for="pw-new">새로운 비밀번호를 입력해주세요</label>
      <input id="pw-new" v-model="form.newPassword" class="box" type="password" autocomplete="new-password" />
      <PasswordRuleList :password="form.newPassword" />
      <FieldError :message="fieldErrors.newPassword" />
    </div>
    <div class="field">
      <label class="label-lg" for="pw-confirm">다시 한 번 입력해주세요</label>
      <input id="pw-confirm" v-model="form.newPasswordConfirm" class="box" type="password" autocomplete="new-password" />
      <p v-if="form.newPasswordConfirm && form.newPassword !== form.newPasswordConfirm" class="msg msg-err">* 비밀번호가 일치하지 않습니다</p>
      <FieldError :message="fieldErrors.newPasswordConfirm" />
    </div>

    <div class="actions">
      <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
      <button type="submit" class="btn btn-primary" :disabled="submitting">비밀번호 변경하기</button>
      <!-- 강제 모드에서는 다른 화면으로 가는 링크를 두지 않는다 (라우터 가드가 막음) -->
      <button v-if="forced" type="button" class="btn" @click="onLogout">로그아웃</button>
      <RouterLink v-else class="btn" :to="{ name: 'mypage' }">취소</RouterLink>
    </div>
    <FieldError :message="formError" class="center" />
  </form>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import FieldError from '@/components/common/FieldError.vue'
import PasswordRuleList from '@/components/common/PasswordRuleList.vue'
import { useDialog } from '@/composables/useDialog'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const dialog = useDialog()

const forced = computed(() => auth.passwordResetRequired)
const form = reactive({ currentPassword: '', newPassword: '', newPasswordConfirm: '' })
const submitting = ref(false)
const formError = ref('')
const fieldErrors = ref({})

async function onLogout() {
  await auth.logout()
  router.replace({ name: 'login' })
}

async function onSubmit() {
  formError.value = ''
  fieldErrors.value = {}
  if (!form.currentPassword || !form.newPassword || !form.newPasswordConfirm) {
    return (formError.value = '* 미입력 값이 있습니다.')
  }

  if (!forced.value) {
    const ok = await dialog.confirm({ title: '비밀번호를 변경하시겠습니까?', confirmText: '변경하기' })
    if (!ok) return
  }

  submitting.value = true
  try {
    await authApi.changePassword({ ...form })
    const wasForced = forced.value
    auth.markPasswordChanged()
    await dialog.alert({ title: '비밀번호가 성공적으로 변경되었습니다' })
    router.replace({ name: wasForced ? 'post-list' : 'mypage' })
  } catch (e) {
    // PASSWORD_MISMATCH 는 401 이지만 세션을 유지한다 (client.js 는 UNAUTHORIZED 에서만 지움)
    formError.value = e.message
    fieldErrors.value = e.fieldErrors ?? {}
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-top.is-plain { padding-top: 24px; border-bottom: 0; }
.pw-change { width: 100%; max-width: 300px; margin: 40px auto 0; }
.actions { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-top: 36px; }
.center { text-align: center; }
</style>
