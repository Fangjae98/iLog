<script setup>
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { userApi } from '@/api/user'
import FieldError from '@/components/common/FieldError.vue'
import { useDialog } from '@/composables/useDialog'
import { RULES } from '@/constants/rules'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const dialog = useDialog()

// 마이페이지 진입 전 비밀번호 재확인
// 개인정보는 Pinia나 localStorage에 저장하지 않아 새로고침하면 다시 확인한다.
const profile = ref(null)
const password = ref('')
const verifying = ref(false)
const passwordError = ref('')

// 닉네임 수정 화면과 요청 상태
const editingNickname = ref(false)
const nicknameDraft = ref('')
const savingNickname = ref(false)
const nicknameError = ref('')

// 회원 탈퇴 API 연결 전 화면 상태
const withdrawing = ref(false)

const joinedAtFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
})

function formatJoinedAt(value) {
  return value ? joinedAtFormatter.format(new Date(value)) : '-'
}

// 비밀번호가 일치하면 개인정보를 이어서 보여주고 마이페이지를 연다.
async function verifyPassword() {
  passwordError.value = ''
  if (!password.value) {
    passwordError.value = '비밀번호를 입력해 주세요.'
    return
  }

  verifying.value = true

  try {
    profile.value = await authApi.verifyPassword({ password: password.value })
    password.value = ''
  } catch (error) {
    // PASSWORD_MISMATCH는 로그인 만료가 아니므로 세션은 유지한다.
    passwordError.value = error.fieldErrors?.password ?? error.message
  } finally {
    verifying.value = false
  }
}

// 닉네임 수정: 입력 검사와 중복 확인 후 개인정보와 로그인 상태를 함께 갱신한다.
function startNicknameEdit() {
  nicknameDraft.value = profile.value.nickname
  nicknameError.value = ''
  editingNickname.value = true
}

function cancelNicknameEdit() {
  nicknameDraft.value = ''
  nicknameError.value = ''
  editingNickname.value = false
}

async function changeNickname() {
  nicknameError.value = ''

  if (!nicknameDraft.value) {
    nicknameError.value = '닉네임을 입력해 주세요.'
    return
  }
  if (!RULES.NICKNAME_PATTERN.test(nicknameDraft.value)) {
    nicknameError.value = '닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다.'
    return
  }
  if (nicknameDraft.value === profile.value.nickname) {
    nicknameError.value = '현재 닉네임과 같은 값으로 변경할 수 없습니다.'
    return
  }

  savingNickname.value = true

  try {
    const { available } = await userApi.checkNickname(nicknameDraft.value)
    if (!available) {
      nicknameError.value = '이미 사용 중인 닉네임입니다.'
      return
    }

    const confirmed = await dialog.confirm({
      title: '닉네임을 변경하시겠습니까?',
      description: '변경할 닉네임: ' + nicknameDraft.value,
      confirmText: '변경하기',
    })
    if (!confirmed) return

    const data = await userApi.updateNickname(nicknameDraft.value)
    profile.value = { ...profile.value, nickname: data.nickname }
    auth.updateNickname(data.nickname)
    cancelNicknameEdit()
    await dialog.alert({ title: '닉네임이 변경되었습니다.' })
  } catch (error) {
    nicknameError.value = error.fieldErrors?.nickname ?? error.message
  } finally {
    savingNickname.value = false
  }
}

// 회원 탈퇴: 화면 흐름만 준비하고 실제 요청은 백엔드 API 계약 확정 후 연결한다.
async function requestWithdrawal() {
  const confirmed = await dialog.confirm({
    title: '정말로 회원 탈퇴하시겠습니까?',
    description: '탈퇴 API가 연결되면 이 확인 이후 탈퇴 요청이 진행됩니다.',
    confirmText: '탈퇴하기',
  })
  if (!confirmed) return

  withdrawing.value = true

  try {
    // TODO: 탈퇴 API의 메서드·URL·응답 계약이 확정되면 userApi 호출을 연결한다.
    await dialog.alert({ title: '회원 탈퇴 기능을 준비 중입니다.' })
  } finally {
    withdrawing.value = false
  }
}
</script>

<template>
  <section class="mypage">
    <!-- 비밀번호 재확인 단계: 성공하면 내 정보 화면으로 전환한다. -->
    <form v-if="!profile" class="verify-card" novalidate @submit.prevent="verifyPassword">
      <div class="page-heading">
        <h1>마이페이지</h1>
        <p class="description">내 정보를 확인하기 위해 비밀번호를 입력해 주세요.</p>
      </div>

      <div class="field">
        <label class="label-lg" for="mypage-password">현재 비밀번호</label>
        <div class="row">
          <input
            id="mypage-password"
            v-model="password"
            class="box"
            type="password"
            autocomplete="current-password"
          />
          <button type="submit" class="btn btn-primary" :disabled="verifying">
            {{ verifying ? '확인 중' : '확인' }}
          </button>
        </div>
        <FieldError :message="passwordError" />
      </div>
    </form>

    <!-- 개인정보 표시: 비밀번호 확인 후 이 화면의 로컬 상태로만 표시한다. -->
    <section v-else class="profile-card">
      <div class="page-heading">
        <h1>{{ profile.nickname }}님, 안녕하세요!</h1>
      </div>

      <div class="profile-list">
        <div class="field">
          <p class="label-lg">이메일</p>
          <p class="readonly">{{ profile.email }}</p>
        </div>
        <div class="field">
          <p class="label-lg">이름</p>
          <p class="readonly">{{ profile.name }}</p>
        </div>
        <!-- 닉네임 수정: 현재 값 표시와 수정 입력을 같은 자리에서 전환한다. -->
        <div class="field">
          <p class="label-lg">닉네임</p>
          <div v-if="!editingNickname" class="nickname-row">
            <p class="readonly">{{ profile.nickname }}</p>
            <button type="button" class="btn" @click="startNicknameEdit">변경</button>
          </div>
          <div v-else class="nickname-edit">
            <div class="row">
              <input
                id="mypage-nickname"
                v-model.trim="nicknameDraft"
                class="box"
                :maxlength="RULES.NICKNAME_MAX"
                aria-label="변경할 닉네임"
              />
              <button
                type="button"
                class="btn btn-primary"
                :disabled="savingNickname"
                @click="changeNickname"
              >
                {{ savingNickname ? '변경 중' : '변경하기' }}
              </button>
              <button type="button" class="btn" :disabled="savingNickname" @click="cancelNicknameEdit">
                취소
              </button>
            </div>
            <FieldError :message="nicknameError" />
          </div>
        </div>
        <div class="field">
          <p class="label-lg">가입일</p>
          <p class="readonly num">{{ formatJoinedAt(profile.createdAt) }}</p>
        </div>
      </div>

      <!-- 계정 관리: 기존 비밀번호 변경 화면을 재사용하고 탈퇴는 UI만 준비한다. -->
      <div class="account-actions">
        <RouterLink class="btn" :to="{ name: 'password-change' }">비밀번호 변경</RouterLink>
        <button type="button" class="btn" :disabled="withdrawing" @click="requestWithdrawal">
          {{ withdrawing ? '처리 중' : '회원 탈퇴' }}
        </button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.mypage {
  width: 100%;
  max-width: 560px;
  margin: 40px auto 0;
}

.verify-card {
  max-width: 400px;
  margin: 64px auto 0;
}

.page-heading {
  margin-bottom: 32px;
}

.page-heading h1 {
  font-family: var(--font-hand);
  font-size: var(--fs-2xl);
  line-height: 35px;
}

.description {
  margin-top: 10px;
  color: var(--muted);
  font-size: var(--fs-sm);
}

.profile-list {
  display: grid;
  gap: 22px;
}

.profile-list .field {
  margin: 0;
}

.nickname-row,
.account-actions {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.nickname-row .readonly {
  flex: 1;
  min-width: 160px;
}

.nickname-edit .row {
  flex-wrap: wrap;
}

.nickname-edit .box {
  min-width: 160px;
}

.account-actions {
  margin-top: 36px;
}

@media (max-width: 560px) {
  .mypage {
    margin-top: 24px;
  }

  .verify-card {
    margin-top: 36px;
  }

  .nickname-edit .row {
    align-items: stretch;
  }
}
</style>
