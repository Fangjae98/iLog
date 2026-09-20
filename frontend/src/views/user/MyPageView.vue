<template>
  <!-- ① 비밀번호 재확인. 성공 응답은 컴포넌트 로컬 state 에만 두므로 새로고침하면 다시 확인한다 -->
  <!-- @metric A11Y: 입력칸마다 <label for> 또는 aria-label 을 붙인다 -->
  <form v-if="!me" class="verify" novalidate @submit.prevent="onVerify">
    <p class="verify-title">현재 비밀번호를 입력해 주세요.</p>
    <div class="row">
      <input v-model="verifyPassword" class="box" type="password" autocomplete="current-password" aria-label="현재 비밀번호" />
      <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
      <button type="submit" class="btn btn-primary" :disabled="verifying">확인</button>
    </div>
    <FieldError :message="verifyError" />
  </form>

  <!-- ② 내 정보 -->
  <section v-else class="my">
    <h1>안녕하세요 {{ user.nickname }}님!</h1>

    <div class="field"><p class="label-lg">ID (이메일)</p><p class="readonly">{{ me.email }}</p></div>
    <div class="field"><p class="label-lg">이름</p><p class="readonly">{{ me.name }}</p></div>
    <div class="field"><p class="label-lg">가입일</p><p class="readonly num">{{ formatDotDate(me.createdAt) }}</p></div>

    <div class="field">
      <label class="label-lg" :for="editingNickname ? 'my-nickname' : undefined">닉네임</label>
      <div v-if="!editingNickname" class="nick-row">
        <p class="readonly">{{ user.nickname }}</p>
        <button type="button" class="btn" @click="startEdit">변경</button>
      </div>
      <div v-else class="nick-row">
        <input id="my-nickname" v-model.trim="nicknameDraft" class="box" :maxlength="RULES.NICKNAME_MAX ?? undefined" />
        <div class="btn-group">
          <button type="button" class="btn btn-primary" :disabled="savingNickname" @click="onChangeNickname">변경하기</button>
          <button type="button" class="btn" @click="editingNickname = false">취소</button>
        </div>
      </div>
      <FieldError :message="nicknameError" />
    </div>

    <RouterLink class="btn stack" :to="{ name: 'password-change' }">비밀번호 변경</RouterLink>
    <button v-if="!withdrawOpen" type="button" class="btn stack" @click="withdrawOpen = true">회원 탈퇴하기</button>

    <form v-else class="withdraw" novalidate @submit.prevent="onWithdraw">
      <label class="label-lg" for="withdraw-password">탈퇴하려면 비밀번호를 입력해 주세요</label>
      <div class="row">
        <input id="withdraw-password" v-model="withdrawPassword" class="box" type="password" autocomplete="current-password" />
        <!-- @metric INP, REQUEST: 요청 중에는 버튼을 막아 중복 요청을 방지한다 -->
        <button type="submit" class="btn btn-primary" :disabled="withdrawing">탈퇴하기</button>
        <button type="button" class="btn" @click="withdrawOpen = false">취소</button>
      </div>
      <FieldError :message="withdrawError" />
    </form>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import FieldError from '@/components/common/FieldError.vue'
import { useDialog } from '@/composables/useDialog'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { RULES } from '@/constants/rules'
import { formatDotDate } from '@/utils/date'

const router = useRouter()
const auth = useAuthStore()
const { user } = storeToRefs(auth) // 닉네임 수정 후 헤더까지 즉시 바뀌게
const dialog = useDialog()

// Pinia·localStorage 에 넣지 않는다 (새로고침하면 다시 확인)
const me = ref(null)
const verifyPassword = ref('')
const verifying = ref(false)
const verifyError = ref('')

const editingNickname = ref(false)
const nicknameDraft = ref('')
const savingNickname = ref(false)
const nicknameError = ref('')

const withdrawOpen = ref(false)
const withdrawPassword = ref('')
const withdrawing = ref(false)
const withdrawError = ref('')

async function onVerify() {
  verifyError.value = ''
  if (!verifyPassword.value) return (verifyError.value = '비밀번호를 입력해주세요!')
  verifying.value = true
  try {
    me.value = await authApi.verifyPassword({ password: verifyPassword.value })
    verifyPassword.value = ''
  } catch (e) {
    // PASSWORD_MISMATCH(401) 여도 세션은 유지된다
    verifyError.value = e.fieldErrors?.password ?? e.message
  } finally {
    verifying.value = false
  }
}

function startEdit() {
  nicknameDraft.value = user.value.nickname
  nicknameError.value = ''
  editingNickname.value = true
}

// @metric REQUEST: 닉네임 중복 확인은 `변경하기` 클릭 때 한 번만 부른다 (입력할 때마다 부르지 않음)
async function onChangeNickname() {
  nicknameError.value = ''
  if (!nicknameDraft.value) return (nicknameError.value = '닉네임을 입력해주세요.')
  if (nicknameDraft.value === user.value.nickname) return (nicknameError.value = '같은 닉네임으로 변경할 수 없습니다')

  savingNickname.value = true
  try {
    const { available } = await userApi.checkNickname(nicknameDraft.value)
    if (!available) return (nicknameError.value = '이미 사용 중인 닉네임입니다')

    const ok = await dialog.confirm({
      title: '닉네임을 변경하시겠습니까?',
      description: `변경할 닉네임: ${nicknameDraft.value}`,
      confirmText: '변경하기',
    })
    if (!ok) return

    const data = await userApi.updateNickname(nicknameDraft.value)
    auth.updateNickname(data.nickname)
    editingNickname.value = false
    await dialog.alert({ title: '닉네임이 성공적으로 변경되었습니다' })
  } catch (e) {
    nicknameError.value = e.fieldErrors?.nickname ?? e.message
  } finally {
    savingNickname.value = false
  }
}

async function onWithdraw() {
  withdrawError.value = ''
  if (!withdrawPassword.value) return (withdrawError.value = '비밀번호를 입력해주세요!')

  const ok = await dialog.confirm({ title: '정말로 계정을 삭제하시겠습니까?', confirmText: '삭제하기' })
  if (!ok) return

  withdrawing.value = true
  try {
    const data = await userApi.withdraw(withdrawPassword.value)
    // 보관 기간("30일")을 코드에 적지 않고 서버가 준 recoverableUntil 을 표시한다
    await dialog.alert({
      title: '계정이 삭제되었습니다',
      description: `${formatDotDate(data.recoverableUntil)}까지 보관 후 삭제돼요.`,
    })
    auth.clear()
    router.replace({ name: 'login' })
  } catch (e) {
    withdrawError.value = e.fieldErrors?.password ?? e.message
  } finally {
    withdrawing.value = false
  }
}
</script>

<style scoped>
.verify { width: 100%; max-width: 320px; margin: 96px auto 0; }
.verify-title { margin-bottom: 12px; font-size: var(--fs-md); }
.my { display: flex; flex-direction: column; align-items: flex-start; gap: 18px; max-width: 560px; margin: 40px auto 0; }
.my h1 { align-self: stretch; margin-bottom: 6px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.my .field { align-self: stretch; margin: 0; }
.my .readonly { width: 260px; max-width: 100%; }
.nick-row { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.nick-row .readonly, .nick-row .box { width: 160px; }
.stack { min-width: 132px; }
.withdraw { align-self: stretch; }
</style>
