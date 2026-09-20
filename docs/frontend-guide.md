# 일일로그(ilog) Vue 프론트엔드 구현 지침

> 기준 문서
> - **Vue 3 실행 흐름 맵** (강의 Full-stack Engineering · Vue.js 2·5·6·7·8장 압축본)
> - **일일로그 응답 명세** (9/18, 엔드포인트 17건 · 공통 규약 결정 1~8 적용본) ← API 계약의 최종 기준
> - **일일로그 API 공통 규약** (9/18), **일일로그 결정 보드** (9/18)
>
> 교안 원본은 이 문서 작성 시점에 첨부되지 않았습니다. 교안 연결은 흐름 맵에 적힌 챕터 번호까지만 반영했습니다.
> 아직 팀에서 정하지 않은 값은 임의로 채우지 않고 `D-xx` 안건 번호와 `TODO`로 남겼습니다. 목록은 [12. 미결 사항](#12-미결-사항)에 있습니다.

---

## 0. 이 문서 사용법

- 구현 순서는 [11. 구현 순서](#11-구현-순서-체크리스트)를 따릅니다. 0단계(공통 틀)를 머지하기 전에 화면 작업을 시작하지 않습니다.
- 3~6장의 코드는 그대로 복사해서 쓰는 **공통 틀**입니다. 7장은 화면별 **명세**라서 표를 보고 직접 구현합니다.
- 8장 코딩 규칙은 리뷰 기준입니다. PR에서 이 규칙 위반은 수정 요청 대상입니다.
- AI 코딩 도구에 넣을 때는 이 파일 전체를 컨텍스트로 주고 "8장 규칙을 지켜서 7.x 화면을 구현해"처럼 범위를 지정합니다.

---

## 1. 기술 스택과 전제

| 항목 | 선택 | 비고 |
|---|---|---|
| 프레임워크 | Vue 3, Composition API, `<script setup>` | 교안 5장 |
| 빌드 | Vite (create-vue로 생성) | 교안 2장 |
| 라우팅 | Vue Router 4 | 교안 6장 |
| 상태 관리 | Pinia | 교안 7장 |
| HTTP | Axios | 교안 8장 |
| 언어 | JavaScript | TypeScript 미사용 |
| 백엔드 | Spring Boot, `/api/v1`, JSON camelCase | 응답 명세 기준 |
| 인증 | Access Token 1개, `Authorization: Bearer` | 공통 규약 결정 6 (A안) |
| 디자인 | 팀 Figma 디자인 시스템 미사용, **와이어프레임 기준** | 화면 배치는 와이어프레임을 따름 |

Node는 Vite가 요구하는 버전(20.19+ 또는 22.12+)을 씁니다. 팀원 간 Node 메이저 버전을 맞춥니다.

---

## 2. 전체 실행 흐름 (흐름 맵 요약)

```
브라우저 ──(주소 입력)──▶ index.html (빈 #app + main.js 로드)
   ▶ main.js : createApp(App) → use(Pinia) → use(router) → mount('#app')
   ▶ App.vue : AppHeader(고정) + <RouterView/>(갈아끼우는 자리)
   ▶ router  : URL → View 선택, beforeEach 가드로 로그인·강제 비밀번호 변경 판정
   ▶ Views   : 페이지 단위, Components 조립, onMounted/watch 시점에 API 호출
   ▶ Components : props 로 받고 emit 으로 올림
   ▶ Pinia   : 로그인 세션(auth), 게시글 목록(post) 공유
   ▶ Axios   : api/client.js 한 곳에서 토큰 첨부·봉투 해제·에러 변환
   ▶ Backend : Spring Boot /api/v1 → 봉투 { success, data, error, timestamp }
```

| 교안 챕터 | 이 프로젝트에서 해당하는 파일 |
|---|---|
| 2. Getting Started | `index.html`, `src/main.js`, `vite.config.js` |
| 5. Components (+ 생명주기) | `src/components/**`, `src/views/**`, `onMounted` |
| 6. Vue Router | `src/router/index.js` |
| 7. Pinia | `src/stores/auth.js`, `src/stores/post.js` |
| 8. Axios | `src/api/**` |

---

## 3. 프로젝트 생성과 설정

### 3.1 생성

```bash
npm create vue@latest ilog-front
# Router: Yes / Pinia: Yes / ESLint: Yes / Prettier: Yes
# TypeScript: No / JSX: No
# 베타·실험 기능(예: 새 포매터, 프리릴리스 Vue)은 선택하지 않음

cd ilog-front
npm install
npm install axios
npm run dev
```

> 폴더 안에서 다시 `npm create vue`를 실행하면 `ilog-front/ilog-front`처럼 중첩 폴더가 생겨 `package.json not found`가 납니다. 레포 루트에서 한 번만 실행합니다.

### 3.2 개발 서버 프록시 (CORS 회피)

개발 중에는 프론트(5173)가 백엔드(8080)를 직접 부르지 않고 Vite 프록시를 거칩니다. 그러면 백엔드에 CORS 설정이 없어도 로컬 개발이 됩니다.

```js
// vite.config.js
import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
```

```bash
# .env.development
VITE_API_BASE_URL=/api/v1
```

- 환경변수는 `VITE_` 접두사가 붙은 것만 브라우저 코드에서 읽힙니다.
- 운영 배포 위치(프론트를 어디서 서빙할지, 백엔드 포트)는 미결입니다. 운영용 `.env.production`은 그때 추가합니다. 백엔드 포트는 결정 보드에서 환경변수(`ILOG_PORT`, 기본 8081)로 빼기로 했으므로 프론트 코드에 포트를 박지 않습니다.

### 3.3 폴더 구조

```
ilog-front/
├─ index.html
├─ vite.config.js
├─ .env.development
└─ src/
   ├─ main.js
   ├─ App.vue
   ├─ router/
   │  └─ index.js
   ├─ stores/
   │  ├─ auth.js          # 로그인 세션
   │  └─ post.js          # 게시글 목록·페이지 정보
   ├─ api/
   │  ├─ client.js        # axios 인스턴스 + 인터셉터 (공통 파일)
   │  ├─ auth.js          # /auth/**, 비밀번호 관련 (백엔드 인증 담당 영역)
   │  ├─ user.js          # /users/** (백엔드 회원 담당 영역)
   │  └─ post.js          # /posts/** (백엔드 게시글 담당 영역)
   ├─ constants/
   │  ├─ errorMessages.js # error.code → 화면 문구 (공통 파일)
   │  └─ rules.js         # 길이·개수 제한 (D-08, D-09, D-16 확정 후 채움)
   ├─ utils/
   │  ├─ session.js       # localStorage 세션 읽기/쓰기
   │  ├─ date.js          # UTC → KST 표시
   │  └─ url.js           # 안전한 링크 판별
   ├─ components/
   │  ├─ common/
   │  │  ├─ AppHeader.vue
   │  │  ├─ BasePagination.vue
   │  │  └─ FieldError.vue
   │  └─ post/
   │     ├─ PostListItem.vue
   │     ├─ PostSearchBar.vue
   │     ├─ PostForm.vue
   │     └─ HashtagInput.vue
   └─ views/
      ├─ auth/
      │  ├─ LoginView.vue
      │  ├─ SignupView.vue
      │  ├─ PasswordFindView.vue
      │  └─ PasswordChangeView.vue
      ├─ post/
      │  ├─ PostListView.vue
      │  ├─ PostDetailView.vue
      │  ├─ PostWriteView.vue
      │  └─ PostEditView.vue
      ├─ user/
      │  └─ MyPageView.vue
      └─ NotFoundView.vue
```

`api/` 파일을 백엔드 도메인 담당(인증·회원·게시글)과 똑같이 나눈 이유는, 명세가 바뀌었을 때 누구에게 물어볼지와 어느 파일을 고칠지가 바로 대응되기 때문입니다.

---

## 4. API 계약 요약 (응답 명세 기준)

### 4.1 응답 봉투

```json
// 성공 (200, 201)
{ "success": true, "data": { }, "error": null, "timestamp": "2026-09-19T05:30:00Z" }

// 실패 (4xx, 5xx)
{
  "success": false,
  "data": null,
  "error": {
    "code": "NICKNAME_DUPLICATED",
    "message": "이미 사용 중인 닉네임입니다",
    "details": [ { "field": "nickname", "reason": "이미 사용 중입니다" } ]
  },
  "timestamp": "2026-09-19T05:30:00Z"
}
```

- **204는 본문이 없습니다.** 봉투도 없습니다.
- `details`는 `INVALID_INPUT`일 때만 채워지고 나머지는 `null`입니다.
- 화면 분기는 **`error.code`로만** 합니다. `message` 문자열 비교 금지.
- 날짜·시각은 **UTC ISO 8601**(`...Z`)로 옵니다. KST 변환은 프론트가 `utils/date.js` 한 곳에서 합니다.

### 4.2 엔드포인트 17건

| 도메인 | Method | URL | 성공 | 로그인 | 사용 화면 |
|---|---|---|---|---|---|
| 인증 | POST | `/auth/tokens` | 201 | ✕ | 로그인 |
| 인증 | DELETE | `/auth/tokens` | 204 | ○ | 헤더 로그아웃 |
| 인증 | POST | `/auth/temporary-passwords` | 201 | ✕ | 비밀번호 찾기 |
| 인증 | POST | `/users/me/password-verification` | 200 | ○ | 마이페이지 진입 |
| 인증 | PUT | `/users/me/password` | 204 | ○ | 비밀번호 변경 |
| 회원 | POST | `/users` | 201 | ✕ | 회원가입 |
| 회원 | GET | `/users/email-availability?email=` | 200 | ✕ | 회원가입 |
| 회원 | GET | `/users/nickname-availability?nickname=` | 200 | ✕/○ | 회원가입, 마이페이지 |
| 회원 | PATCH | `/users/me` | 200 | ○ | 마이페이지 닉네임 수정 |
| 회원 | POST | `/users/me/withdrawal` | 200 | ○ | 마이페이지 탈퇴 |
| 게시글 | POST | `/posts` | 201 | ○ | 글쓰기 |
| 게시글 | GET | `/posts` (목록·검색 통합) | 200 | ○ | 목록 |
| 게시글 | GET | `/posts/{postId}` | 200 | ○ | 상세, 수정 폼 채우기 |
| 게시글 | PATCH | `/posts/{postId}` | 200 | ○ | 수정 |
| 게시글 | DELETE | `/posts/{postId}` | 204 | ○ | 상세 삭제 |
| 해시태그 | POST | `/posts/{postId}/hashtags` | 201 | ○ | D-06 결과에 따라 |
| 해시태그 | DELETE | `/posts/{postId}/hashtags/{hashtagId}` | 204 | ○ | D-06 결과에 따라 |

### 4.3 에러 코드 전체

| error.code | 상태 | 발생 API | 프론트 처리 |
|---|---|---|---|
| `UNAUTHORIZED` | 401 | 로그인 필요 API 전부 | 세션 삭제 → 로그인 화면 (인터셉터가 처리) |
| `INTERNAL_ERROR` | 500 | 전부 | 기본 문구 토스트 |
| `INVALID_INPUT` | 400 | 입력 받는 API 전부 | `details`를 필드 아래에 표시 |
| `LOGIN_FAILED` | 401 | 로그인 | 폼 상단 문구 |
| `ACCOUNT_WITHDRAWN` | 401 | 로그인 | 탈퇴 계정 안내 (복구는 D-10) |
| `MEMBER_NOT_MATCHED` | 400 | 임시 비밀번호 발급 | 폼 상단 문구 |
| `PASSWORD_MISMATCH` | 401 | 비밀번호 재확인, 비밀번호 변경, 탈퇴 | 비밀번호 필드 아래 문구, **로그아웃하지 않음** |
| `PASSWORD_CONFIRM_MISMATCH` | 400 | 회원가입, 비밀번호 변경 | 확인 필드 아래 문구 |
| `PASSWORD_RECENTLY_USED` | 400 | 비밀번호 변경 | 새 비밀번호 필드 아래 문구 |
| `EMAIL_DUPLICATED` | 400 | 회원가입 | 이메일 필드, 중복확인 상태 초기화 |
| `NICKNAME_DUPLICATED` | 400 | 회원가입, 닉네임 수정 | 닉네임 필드, 중복확인 상태 초기화 |
| `NICKNAME_SAME_AS_CURRENT` | 400 | 닉네임 수정 | 닉네임 필드 |
| `POST_NOT_FOUND` | 404 | 게시글 상세·수정·삭제, 해시태그 | 토스트 후 목록으로 `replace` |
| `POST_NOT_OWNED` | 403 | 게시글 수정·삭제, 해시태그 | 토스트 후 상세로 이동 |
| `HASHTAG_NOT_FOUND` | 404 | 해시태그 삭제 | 상세 다시 불러오기 |

> **자주 헷갈리는 지점 — 401이라고 전부 로그아웃시키면 안 됩니다.**
> 응답 명세에서 `LOGIN_FAILED`, `ACCOUNT_WITHDRAWN`, `PASSWORD_MISMATCH`도 401입니다. 인터셉터가 상태 코드 401만 보고 세션을 지우면, 마이페이지에서 비밀번호를 한 번 틀린 사용자가 로그아웃됩니다. 세션 삭제 조건은 **`error.code === 'UNAUTHORIZED'`** 하나뿐입니다.

---

## 5. 공통 틀 코드 (0단계에서 한 명이 만들고 머지)

### 5.1 `src/main.js`

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia()) // ① Pinia를 먼저: 라우터 가드가 첫 이동 때 useAuthStore()를 부른다
app.use(router)        // ②
app.mount('#app')      // ③
```

> **자주 헷갈리는 지점** — `use(router)` 순간 첫 화면 이동이 시작되고 `beforeEach`가 실행됩니다. 그 전에 Pinia가 없으면 가드 안의 `useAuthStore()`가 "no active Pinia" 에러를 냅니다.

### 5.2 `src/utils/session.js`

```js
// 세션은 한 키에 묶어서 저장한다 (토큰·사용자·강제변경 여부가 항상 같이 바뀌므로)
const KEY = 'ilog.session'

export const session = {
  get() {
    try {
      return JSON.parse(localStorage.getItem(KEY))
    } catch {
      return null
    }
  },
  set(value) {
    localStorage.setItem(KEY, JSON.stringify(value))
  },
  clear() {
    localStorage.removeItem(KEY)
  },
}
```

### 5.3 `src/constants/errorMessages.js`

결정 보드 논점 03(B안)에 따라 **화면 문구는 프론트가 소유**합니다. 서버 `message`는 매핑이 없을 때의 예비 문구로만 씁니다.

```js
export const ERROR_MESSAGES = {
  UNAUTHORIZED: '로그인이 필요해요. 다시 로그인해 주세요.',
  INTERNAL_ERROR: '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.',
  INVALID_INPUT: '입력값을 확인해 주세요.',
  LOGIN_FAILED: '이메일 또는 비밀번호를 다시 확인해 주세요.',
  ACCOUNT_WITHDRAWN: '탈퇴 처리된 계정이에요.',
  MEMBER_NOT_MATCHED: '입력하신 이름과 이메일로 가입된 회원이 없어요.',
  PASSWORD_MISMATCH: '비밀번호가 일치하지 않아요.',
  PASSWORD_CONFIRM_MISMATCH: '비밀번호 확인이 일치하지 않아요.',
  PASSWORD_RECENTLY_USED: '최근에 사용한 비밀번호는 다시 쓸 수 없어요.',
  EMAIL_DUPLICATED: '이미 가입된 이메일이에요.',
  NICKNAME_DUPLICATED: '이미 사용 중인 닉네임이에요.',
  NICKNAME_SAME_AS_CURRENT: '지금 쓰고 있는 닉네임과 같아요.',
  POST_NOT_FOUND: '삭제되었거나 존재하지 않는 글이에요.',
  POST_NOT_OWNED: '본인이 작성한 글만 수정·삭제할 수 있어요.',
  HASHTAG_NOT_FOUND: '이미 삭제된 해시태그예요.',
  NETWORK_ERROR: '서버에 연결할 수 없어요. 네트워크를 확인해 주세요.',
}

const DEFAULT_MESSAGE = '요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.'

export const resolveMessage = (code, serverMessage) =>
  ERROR_MESSAGES[code] ?? serverMessage ?? DEFAULT_MESSAGE
```

`?? DEFAULT_MESSAGE`가 빠지면 백엔드가 새 코드를 추가했을 때 화면에 `undefined`가 찍힙니다.

### 5.4 `src/api/client.js`

토큰 첨부, 봉투 해제, 에러 변환을 **이 파일 한 곳에서만** 합니다. 컴포넌트는 봉투를 모릅니다.

```js
import axios from 'axios'
import { session } from '@/utils/session'
import { resolveMessage } from '@/constants/errorMessages'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  timeout: 10000,
  // 배열 파라미터를 ?hashtag=JWT&hashtag=Spring 으로 보낸다 (기본값은 hashtag[]=JWT)
  paramsSerializer: { indexes: null },
})

// 요청: 토큰 첨부
client.interceptors.request.use((config) => {
  const token = session.get()?.accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 응답: 성공이면 data만, 실패면 통일된 에러 객체로
client.interceptors.response.use(
  (res) => (res.status === 204 ? null : res.data?.data ?? null),
  (error) => {
    const res = error.response
    const code = res ? res.data?.error?.code ?? 'INTERNAL_ERROR' : 'NETWORK_ERROR'

    const fieldErrors = {}
    for (const d of res?.data?.error?.details ?? []) {
      fieldErrors[d.field] = d.reason // { nickname: '이미 사용 중입니다' }
    }

    // 세션 삭제는 UNAUTHORIZED 하나에서만 (4.3 참고)
    if (code === 'UNAUTHORIZED') {
      session.clear()
      window.location.replace('/login?expired=1')
    }

    return Promise.reject({
      status: res?.status ?? 0,
      code,
      message: resolveMessage(code, res?.data?.error?.message),
      fieldErrors,
    })
  },
)

export default client
```

> **왜 `router.push`가 아니라 `window.location.replace`인가** — `client.js`에서 router를 import하면 `router → views → stores → api → client → router` 순환 참조가 생깁니다. 전체 새로고침으로 로그인 화면에 가면 Pinia 상태도 깨끗하게 초기화됩니다.

### 5.5 `src/api/auth.js`, `user.js`, `post.js`

```js
// src/api/auth.js — 백엔드 인증 담당 영역
import client from './client'

export const authApi = {
  login: (body) => client.post('/auth/tokens', body), // { email, password }
  logout: () => client.delete('/auth/tokens'),
  issueTempPassword: (body) => client.post('/auth/temporary-passwords', body), // { email, name }
  verifyPassword: (body) => client.post('/users/me/password-verification', body), // { password }
  changePassword: (body) => client.put('/users/me/password', body),
  // { currentPassword, newPassword, newPasswordConfirm }
}
```

```js
// src/api/user.js — 백엔드 회원 담당 영역
import client from './client'

export const userApi = {
  signup: (body) => client.post('/users', body),
  // { email, password, passwordConfirm, name, nickname }
  checkEmail: (email) => client.get('/users/email-availability', { params: { email } }),
  checkNickname: (nickname) => client.get('/users/nickname-availability', { params: { nickname } }),
  updateNickname: (nickname) => client.patch('/users/me', { nickname }),
  withdraw: (password) => client.post('/users/me/withdrawal', { password }),
}
```

```js
// src/api/post.js — 백엔드 게시글 담당 영역
import client from './client'

export const postApi = {
  list: (params) => client.get('/posts', { params }),
  // { page, size, sort, keyword, hashtag: [], nickname, date }
  get: (postId) => client.get(`/posts/${postId}`),
  create: (body) => client.post('/posts', body), // { title, content, urls, hashtags }
  update: (postId, body) => client.patch(`/posts/${postId}`, body), // 보낸 필드만 수정
  remove: (postId) => client.delete(`/posts/${postId}`),
  addHashtags: (postId, names) => client.post(`/posts/${postId}/hashtags`, { names }),
  removeHashtag: (postId, hashtagId) => client.delete(`/posts/${postId}/hashtags/${hashtagId}`),
}
```

### 5.6 `src/stores/auth.js`

```js
import { defineStore } from 'pinia'
import { authApi } from '@/api/auth'
import { session } from '@/utils/session'

const saved = session.get()

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: saved?.accessToken ?? null,
    expiresAt: saved?.expiresAt ?? 0,
    user: saved?.user ?? null, // { userId, nickname }
    passwordResetRequired: saved?.passwordResetRequired ?? false,
  }),

  actions: {
    // getter로 만들면 결과가 캐시되어 시간이 지나도 다시 계산되지 않는다 → 함수로 둔다
    hasValidToken() {
      return !!this.accessToken && Date.now() < this.expiresAt
    },

    async login(email, password) {
      const data = await authApi.login({ email, password })
      this.accessToken = data.accessToken
      this.expiresAt = Date.now() + data.expiresIn * 1000 // expiresIn 단위: 초
      this.user = data.user
      this.passwordResetRequired = data.passwordResetRequired
      this.persist()
      return data
    },

    async logout() {
      try {
        await authApi.logout() // 로그아웃 API 존치 여부 미결 — 실패해도 아래는 실행
      } catch {
        /* 무시 */
      } finally {
        this.clear()
      }
    },

    markPasswordChanged() {
      this.passwordResetRequired = false
      this.persist()
    },

    updateNickname(nickname) {
      this.user = { ...this.user, nickname }
      this.persist()
    },

    persist() {
      session.set({
        accessToken: this.accessToken,
        expiresAt: this.expiresAt,
        user: this.user,
        passwordResetRequired: this.passwordResetRequired,
      })
    },

    clear() {
      this.$reset()
      this.accessToken = null
      this.expiresAt = 0
      this.user = null
      this.passwordResetRequired = false
      session.clear()
    },
  },
})
```

> `$reset()`은 `state()`를 다시 실행하는데, 이 store의 `state()`는 모듈 로드 시점의 `saved`를 읽습니다. 그래서 `$reset()` 뒤에 값을 명시적으로 비웁니다.

### 5.7 `src/router/index.js`

```js
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

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
```

- 게시글 화면 전체가 `requiresAuth`인 이유는 응답 명세에서 게시글 API가 전부 "회원"이기 때문입니다. 비로그인 열람을 열기로 하면 `post-list`, `post-detail`의 meta만 바꿉니다.
- `:postId(\\d+)`는 `/posts/new`가 상세 라우트로 잡히지 않게 하고, 숫자가 아닌 주소는 404로 보냅니다.
- `props: true`로 받은 `postId`는 **문자열**입니다. API 호출에는 그대로 써도 되지만 비교할 때는 `Number()`로 바꿉니다.

### 5.8 `src/stores/post.js`

```js
import { defineStore } from 'pinia'
import { postApi } from '@/api/post'

export const usePostStore = defineStore('post', {
  state: () => ({
    items: [],
    pageInfo: { page: 0, size: 10, totalElements: 0, totalPages: 0, hasNext: false },
    loading: false,
    error: null,
  }),

  actions: {
    async fetchList(params) {
      this.loading = true
      this.error = null
      try {
        const data = await postApi.list(params)
        this.items = data.content
        const { page, size, totalElements, totalPages, hasNext } = data
        this.pageInfo = { page, size, totalElements, totalPages, hasNext }
      } catch (e) {
        this.items = []
        this.error = e.message
      } finally {
        this.loading = false // 실패해도 로딩은 반드시 내린다
      }
    },
  },
})
```

### 5.9 `src/utils/date.js`, `src/utils/url.js`

```js
// src/utils/date.js — 서버는 UTC(...Z), 화면은 KST
const dateTimeFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit',
})
const dateFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
})

export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '')
export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '')
```

```js
// src/utils/url.js — 사용자가 입력한 URL을 링크로 걸기 전에 검사
export const isSafeUrl = (value) => {
  try {
    return ['http:', 'https:'].includes(new URL(value).protocol)
  } catch {
    return false
  }
}
```

> **자주 헷갈리는 지점** — Vue는 `{{ }}` 출력은 이스케이프하지만 `:href`에 들어가는 `javascript:alert(1)`은 막지 않습니다. 게시글 URL은 사용자가 입력한 값이라 `isSafeUrl`을 통과한 것만 `<a>`로 그립니다.

### 5.10 `src/constants/rules.js`

```js
// 팀 확정 전까지 값을 임의로 넣지 않는다. 확정되면 여기만 고친다.
export const RULES = {
  PASSWORD_PATTERN: null, // TODO D-16
  NICKNAME_MIN: null,     // TODO D-16
  NICKNAME_MAX: null,     // TODO D-16
  TITLE_MAX: 100,         // 응답 명세 요청 표: 1~100자 (D-08 최종 확정 필요)
  CONTENT_MAX: null,      // TODO D-08
  URL_MAX_COUNT: null,    // TODO D-08
  HASHTAG_MAX_COUNT: null,// TODO D-09
  HASHTAG_MAX_LENGTH: 50, // 응답 명세 요청 표: 각 1~50자 (D-09 최종 확정 필요)
}
```

프론트 검증은 사용자 편의용입니다. 값이 `null`인 동안은 필수값 확인과 "확인 필드 일치"만 프론트에서 막고, 나머지는 서버 `INVALID_INPUT`의 `details`로 표시합니다.

### 5.11 `src/App.vue`

```vue
<template>
  <AppHeader v-if="showHeader" />
  <main class="container">
    <RouterView />
  </main>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'

const route = useRoute()
// 비로그인 화면과 강제 비밀번호 변경 화면에서는 메뉴를 숨긴다
const showHeader = computed(() => !route.meta.guestOnly && route.name !== 'password-change')
</script>
```

---

## 6. 공통 컴포넌트

### 6.1 `AppHeader.vue`

| 항목 | 내용 |
|---|---|
| 표시 | 로고(→ `/posts`), 글쓰기(→ `/posts/new`), 마이페이지(→ `/mypage`), `{{ nickname }}`, 로그아웃 버튼 |
| 로그아웃 | `await auth.logout()` 후 `router.replace({ name: 'login' })` |
| 닉네임 | `const { user } = storeToRefs(auth)` 로 꺼내야 닉네임 수정 후 즉시 바뀜 |

```vue
<script setup>
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const { user } = storeToRefs(auth) // 값은 storeToRefs
const { logout } = auth            // 액션은 그냥 꺼내도 됨
const router = useRouter()

async function onLogout() {
  await logout()
  router.replace({ name: 'login' })
}
</script>
```

### 6.2 `FieldError.vue`

```vue
<template>
  <p v-if="message" class="field-error" role="alert">{{ message }}</p>
</template>

<script setup>
defineProps({ message: { type: String, default: '' } })
</script>
```

### 6.3 `BasePagination.vue`

- props: `page`(화면 기준 **1부터**), `totalPages`
- emit: `change(page)` — 부모가 URL 쿼리를 바꿈
- `totalPages === 0`이면 렌더링하지 않음
- 이전 버튼은 `page <= 1`, 다음 버튼은 `page >= totalPages`일 때 `disabled`

### 6.4 `HashtagInput.vue`

```vue
<template>
  <div class="hashtag-input">
    <ul class="tags">
      <li v-for="tag in tags" :key="tag">
        #{{ tag }}
        <button type="button" @click="remove(tag)" :aria-label="`${tag} 삭제`">×</button>
      </li>
    </ul>
    <input
      v-model="draft"
      placeholder="태그 입력 후 Enter (쉼표로 여러 개)"
      @keydown.enter="onEnter"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { RULES } from '@/constants/rules'

const tags = defineModel({ type: Array, default: () => [] })
const draft = ref('')

function onEnter(e) {
  if (e.isComposing) return // 한글 조합 중 Enter는 무시 (마지막 글자가 두 번 들어가는 문제)
  e.preventDefault()        // form 안에서 Enter가 submit 되지 않게

  const names = draft.value
    .split(',')
    .map((s) => s.trim().replace(/^#/, ''))
    .filter(Boolean)
    .filter((s) => !RULES.HASHTAG_MAX_LENGTH || s.length <= RULES.HASHTAG_MAX_LENGTH)

  const merged = [...new Set([...tags.value, ...names])]
  tags.value = RULES.HASHTAG_MAX_COUNT ? merged.slice(0, RULES.HASHTAG_MAX_COUNT) : merged
  draft.value = ''
}

function remove(tag) {
  tags.value = tags.value.filter((t) => t !== tag)
}
</script>
```

- `#` 제거와 중복 제거를 프론트에서도 합니다. 대소문자 통일 여부(D-09)는 서버 `normalizeTag` 한 곳에서 정하므로 프론트는 건드리지 않습니다.
- `tags.value.push()`로 넣지 않고 새 배열을 대입합니다. `defineModel`은 대입해야 부모에게 `update:modelValue`가 나갑니다.

### 6.5 `PostForm.vue` (작성·수정 공용)

```vue
<template>
  <form @submit.prevent="onSubmit">
    <label>제목 <input v-model="form.title" required :maxlength="RULES.TITLE_MAX" /></label>
    <FieldError :message="fieldErrors.title" />

    <label>내용 <textarea v-model="form.content" required rows="12" /></label>
    <FieldError :message="fieldErrors.content" />

    <label>참고 URL (한 줄에 하나) <textarea v-model="form.urlsText" rows="3" /></label>
    <FieldError :message="fieldErrors.urls" />

    <HashtagInput v-model="form.hashtags" />
    <FieldError :message="fieldErrors.hashtags" />

    <button type="submit" :disabled="submitting">{{ submitLabel }}</button>
  </form>
</template>

<script setup>
import { reactive, watch } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import HashtagInput from './HashtagInput.vue'
import { RULES } from '@/constants/rules'

const props = defineProps({
  initial: {
    type: Object,
    default: () => ({ title: '', content: '', urls: [], hashtags: [] }),
  },
  submitting: { type: Boolean, default: false },
  submitLabel: { type: String, default: '등록' },
  fieldErrors: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['submit'])

// props를 직접 고치지 않고 로컬 복사본을 편집한다
const form = reactive({ title: '', content: '', urlsText: '', hashtags: [] })

watch(
  () => props.initial,
  (v) => {
    form.title = v.title
    form.content = v.content
    form.urlsText = v.urls.join('\n')
    form.hashtags = [...v.hashtags]
  },
  { immediate: true },
)

function onSubmit() {
  emit('submit', {
    title: form.title.trim(),
    content: form.content,
    urls: form.urlsText.split('\n').map((s) => s.trim()).filter(Boolean),
    hashtags: form.hashtags,
  })
}
</script>
```

### 6.6 `PostListItem.vue`, `PostSearchBar.vue`

| 컴포넌트 | props | emit | 표시 |
|---|---|---|---|
| `PostListItem` | `post` (`{ postId, title, nickname, hashtags, createdAt }`) | 없음 | 제목(RouterLink → 상세), 닉네임, `#태그`, `formatDate(createdAt)` |
| `PostSearchBar` | `initial` (현재 URL 쿼리) | `search(filters)` | keyword, 해시태그, nickname, date(`<input type="date">`), 검색·초기화 버튼 |

- 목록 응답에는 **본문(content)이 없습니다.** 목록 카드에 본문 미리보기를 넣으려면 명세 변경(D-05)이 먼저입니다.
- 검색은 버튼 제출 방식이라 `v-model`로 충분합니다. 입력할 때마다 걸러내는 실시간 검색으로 바꾸면 한글 조합 중에는 `v-model`이 갱신되지 않으므로 `:value` + `@input`으로 바꿉니다.

---

## 7. 화면별 명세

각 화면은 **로딩 / 정상 / 빈 결과 / 에러** 네 상태를 모두 그립니다. 버튼은 요청 중 `disabled`로 막아 중복 요청을 방지합니다.

### 7.1 로그인 `/login` — `LoginView.vue`

| 항목 | 내용 |
|---|---|
| API | `POST /auth/tokens` (`auth.login`) |
| 입력 | email, password |
| 성공 | `passwordResetRequired === true` → `password-change`로 `replace` / 아니면 `redirect` 쿼리 또는 `post-list` |
| `LOGIN_FAILED` | 폼 상단 문구 |
| `ACCOUNT_WITHDRAWN` | 탈퇴 계정 안내 영역 표시. 복구 버튼은 D-10 확정 전까지 넣지 않음 |
| `INVALID_INPUT` | 필드 아래 `fieldErrors` |
| 진입 쿼리 | `?expired=1`이면 "로그인이 만료되었어요" 안내 |
| 링크 | 회원가입, 비밀번호 찾기 |

```js
async function onSubmit() {
  submitting.value = true
  formError.value = ''
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
    fieldErrors.value = e.fieldErrors
  } finally {
    submitting.value = false
  }
}
```

### 7.2 회원가입 `/signup` — `SignupView.vue`

| 항목 | 내용 |
|---|---|
| API | `GET /users/email-availability`, `GET /users/nickname-availability`, `POST /users` |
| 입력 | email, password, passwordConfirm, name(실명), nickname |
| 중복 확인 | 이메일·닉네임 각각 "중복 확인" 버튼. 확인 후 값을 고치면 확인 상태를 `false`로 되돌림 |
| 이메일 결과 | `available: true` → 사용 가능 / `reason: 'IN_USE'` → 사용 불가 / `reason: 'WITHDRAWN'` → "탈퇴한 계정의 이메일이에요" + 로그인 화면 링크 (D-11에 따라 바뀔 수 있음) |
| 가입 버튼 | 두 확인이 모두 `true`이고 password === passwordConfirm일 때만 활성 |
| 성공 (201) | 완료 안내 후 `login`으로 `replace` |
| `EMAIL_DUPLICATED` / `NICKNAME_DUPLICATED` | 확인 버튼을 누른 뒤 가입 전에 다른 사람이 먼저 가입한 경우. 해당 확인 상태 `false` + 필드 문구 |
| `PASSWORD_CONFIRM_MISMATCH`, `INVALID_INPUT` | 필드 아래 표시 |

```js
const emailChecked = ref(false)
const nicknameChecked = ref(false)

// 확인 후 값을 바꾸면 다시 확인하게
watch(() => form.email, () => { emailChecked.value = false })
watch(() => form.nickname, () => { nicknameChecked.value = false })

async function checkEmail() {
  try {
    const { available, reason } = await userApi.checkEmail(form.email.trim())
    emailChecked.value = available
    emailStatus.value = reason ?? 'OK' // 'OK' | 'IN_USE' | 'WITHDRAWN'
  } catch (e) {
    emailChecked.value = false
    fieldErrors.value = { ...fieldErrors.value, email: e.fieldErrors.email ?? e.message }
  }
}
```

### 7.3 비밀번호 찾기 `/password/find` — `PasswordFindView.vue`

| 항목 | 내용 |
|---|---|
| API | `POST /auth/temporary-passwords` |
| 입력 | email, name |
| 성공 (201) | 응답 `data.email`은 마스킹된 값(`p***@example.com`). "`{email}`로 임시 비밀번호를 보냈어요" 표시 + 로그인 링크. 문구는 D-17 확정 후 교체 |
| `MEMBER_NOT_MATCHED` | 폼 상단 문구. 이메일과 이름 중 어느 쪽이 틀렸는지 표시하지 않음 |
| 주의 | 응답에는 임시 비밀번호가 없습니다. 화면에 비밀번호를 보여주는 UI를 만들지 않습니다 |

### 7.4 비밀번호 변경 `/password/change` — `PasswordChangeView.vue`

| 항목 | 내용 |
|---|---|
| API | `PUT /users/me/password` |
| 입력 | currentPassword, newPassword, newPasswordConfirm |
| 두 가지 모드 | `auth.passwordResetRequired === true` → **강제 모드**: 헤더 숨김, "임시 비밀번호로 로그인했어요. 새 비밀번호를 설정해 주세요." 안내, 현재 비밀번호 칸 라벨은 "임시 비밀번호", 로그아웃 버튼만 제공 / `false` → 마이페이지에서 들어온 일반 모드 |
| 성공 (204) | `auth.markPasswordChanged()` → 강제 모드였으면 `post-list`, 일반 모드였으면 `mypage`로 `replace` |
| `PASSWORD_MISMATCH` (401) | 현재 비밀번호 칸 아래 문구. **세션 유지** |
| `PASSWORD_CONFIRM_MISMATCH` | 확인 칸 아래 |
| `PASSWORD_RECENTLY_USED` | 새 비밀번호 칸 아래 |
| 이동 차단 | 강제 모드의 다른 화면 이동은 5.7 라우터 가드가 막음. 화면 안에 다른 페이지 링크를 두지 않음 |

### 7.5 게시글 목록·검색 `/posts` — `PostListView.vue`

검색 조건과 페이지를 **URL 쿼리에 저장**합니다. 그래야 상세에 들어갔다가 뒤로 가기를 눌렀을 때 보던 검색 결과와 페이지로 돌아옵니다.

| 항목 | 내용 |
|---|---|
| API | `GET /posts` |
| URL 쿼리 | `?page=2&keyword=JWT&hashtag=Spring&hashtag=JPA&nickname=기택&date=2026-09-19` |
| 페이지 번호 | **URL·화면은 1부터, API는 0부터.** 변환은 `toApiParams` 한 곳에서만 |
| sort | `createdAt,desc` 고정 (MVP는 정렬 선택 없음) |
| 내 글 보기 | `nickname=내 닉네임`으로 검색 (정확히 일치) |
| 빈 결과 | 200 + `content: []` → "조건에 맞는 로그가 없어요" |
| 에러 | `postStore.error` 표시 + 다시 시도 버튼 |

```vue
<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { usePostStore } from '@/stores/post'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const postStore = usePostStore()
const { items, pageInfo, loading, error } = storeToRefs(postStore)
const auth = useAuthStore()

const PAGE_SIZE = 10

// URL 쿼리(화면 기준) → API 파라미터(서버 기준)
function toApiParams(q) {
  const params = {
    page: Math.max(Number(q.page) || 1, 1) - 1,
    size: PAGE_SIZE,
    sort: 'createdAt,desc',
    keyword: q.keyword,
    hashtag: [].concat(q.hashtag ?? []), // 1개면 문자열, 여러 개면 배열로 옴
    nickname: q.nickname,
    date: q.date,
  }
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== '' && !(Array.isArray(v) && !v.length)),
  )
}

// 쿼리가 바뀔 때마다(첫 진입 포함) 다시 불러옴
watch(() => route.query, (q) => postStore.fetchList(toApiParams(q)), { immediate: true })

const currentPage = computed(() => pageInfo.value.page + 1)

function onSearch(filters) {
  router.push({ query: { ...filters, page: 1 } }) // 조건이 바뀌면 1페이지부터
}
function onPageChange(page) {
  router.push({ query: { ...route.query, page } })
}
function showMyPosts() {
  router.push({ query: { nickname: auth.user.nickname, page: 1 } })
}
</script>
```

> `watch(..., { immediate: true })`가 `onMounted` 역할까지 합니다. 여기에 `onMounted`에서 한 번 더 부르면 첫 진입 때 요청이 두 번 나갑니다.

### 7.6 게시글 상세 `/posts/:postId` — `PostDetailView.vue`

| 항목 | 내용 |
|---|---|
| API | `GET /posts/{postId}`, `DELETE /posts/{postId}` |
| 호출 시점 | `watch(() => props.postId, load, { immediate: true })` — 상세에서 다른 상세로 이동해도 다시 불러옴 |
| 표시 | 제목, `author.nickname`, 작성일, 수정일(`updatedAt`이 `null`이면 표시 안 함), 본문, URL 목록, 해시태그 |
| 본문 | `{{ post.content }}` + CSS `white-space: pre-wrap`. **`v-html` 금지** |
| URL | `isSafeUrl`을 통과한 것만 `<a :href target="_blank" rel="noopener noreferrer">` |
| 해시태그 클릭 | `post-list`로 `?hashtag=태그` 이동 |
| 수정·삭제 버튼 | `post.isMine === true`일 때만 표시. 닉네임 비교로 판단하지 않음 |
| 삭제 | `confirm` → `DELETE` (204) → `post-list`로 `replace` |
| `POST_NOT_FOUND` | 안내 후 `post-list`로 `replace` (뒤로 가기로 다시 404가 뜨지 않게) |
| `POST_NOT_OWNED` | 안내만 (버튼이 숨겨져 있으므로 정상 흐름에서는 발생하지 않음) |

```js
const props = defineProps({ postId: { type: String, required: true } })
const post = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    post.value = await postApi.get(props.postId)
  } catch (e) {
    if (e.code === 'POST_NOT_FOUND') {
      alert(e.message)
      router.replace({ name: 'post-list' })
    } else {
      loadError.value = e.message
    }
  } finally {
    loading.value = false
  }
}
watch(() => props.postId, load, { immediate: true })
```

### 7.7 글쓰기 `/posts/new` — `PostWriteView.vue`

| 항목 | 내용 |
|---|---|
| API | `POST /posts` |
| 구성 | `<PostForm submit-label="등록" :submitting :field-errors @submit="onSubmit" />` |
| 성공 (201) | 응답 `data.postId`로 `post-detail`에 `replace` (뒤로 가기로 빈 작성 폼이 다시 뜨지 않게) |
| `INVALID_INPUT` | `fieldErrors`를 PostForm에 전달 |
| 작성자·작성일 | 요청에 넣지 않음. 서버가 토큰과 현재 시각으로 정함 |

### 7.8 글 수정 `/posts/:postId/edit` — `PostEditView.vue`

| 항목 | 내용 |
|---|---|
| API | `GET /posts/{postId}` (폼 채우기), `PATCH /posts/{postId}` |
| 진입 | 상세 조회 후 `isMine === false`면 `post-detail`로 `replace` |
| 초기값 | `{ title, content, urls, hashtags }`를 PostForm `initial`로 전달 |
| 요청 | 응답 명세 기준으로 `hashtags`까지 포함해 **통째로 교체** (D-06) |
| 성공 (200) | `post-detail`로 `replace` |
| `POST_NOT_OWNED` | 안내 후 `post-detail`로 |
| `POST_NOT_FOUND` | 안내 후 `post-list`로 |

> D-06이 "해시태그는 별도 API"로 정해지면 PATCH 본문에서 `hashtags`를 빼고, 상세 화면에 `addHashtags` / `removeHashtag`를 붙입니다. 그때 해시태그 삭제에는 `hashtagId`가 필요한데, 현재 응답 명세의 `hashtags`는 문자열 배열이라 id가 없습니다. [12. 미결 사항](#12-미결-사항)의 **F-2**를 먼저 닫아야 합니다.

### 7.9 마이페이지 `/mypage` — `MyPageView.vue`

한 화면 안에서 단계가 바뀝니다.

| 단계 | 내용 |
|---|---|
| ① 비밀번호 재확인 | `POST /users/me/password-verification` → 성공 시 `data`(userId, email, name, nickname, createdAt)를 **컴포넌트 로컬 state**에만 저장. Pinia·localStorage에 넣지 않음 (새로고침하면 다시 확인) |
| ① 실패 | `PASSWORD_MISMATCH`(401) → 필드 문구. 세션 유지 |
| ② 내 정보 | 이메일·이름·가입일은 읽기 전용. 가입일은 `formatDate` |
| ③ 닉네임 수정 | 중복 확인(`checkNickname`) 통과 시 저장 버튼 활성 → `PATCH /users/me` → `auth.updateNickname(data.nickname)` |
| ③ 실패 | `NICKNAME_SAME_AS_CURRENT`, `NICKNAME_DUPLICATED`, `INVALID_INPUT` → 필드 문구 |
| ④ 비밀번호 변경 | `password-change`로 이동 (일반 모드) |
| ⑤ 탈퇴 | 비밀번호 입력 + `confirm` → `POST /users/me/withdrawal` (200) → "`{formatDate(recoverableUntil)}`까지 보관 후 삭제돼요" 안내 → `auth.clear()` → `login`으로 `replace` |
| ⑤ 실패 | `PASSWORD_MISMATCH` → 필드 문구 |

- 보관 기간 "30일"을 화면 코드에 적지 않습니다. 서버가 주는 `recoverableUntil`을 표시합니다.
- D-15가 "확인과 조회 분리"로 정해지면 ①의 응답이 `{ verified: true }`가 되고 `GET /users/me`가 추가됩니다. `api/user.js`에 함수 하나를 더하고 ①→② 사이에 호출만 넣으면 됩니다.

### 7.10 404 `/:pathMatch(.*)*` — `NotFoundView.vue`

"페이지를 찾을 수 없어요" + 목록으로 가기 링크.

---

## 8. 코딩 규칙

### 8.1 구조

1. 모든 컴포넌트는 `<script setup>` + Composition API.
2. 페이지는 `views/`, 재사용 조각은 `components/`. 파일 이름은 PascalCase, 페이지는 `~View.vue`로 끝남. 컴포넌트 이름은 두 단어 이상(`Header.vue` ✕ → `AppHeader.vue`).
3. **컴포넌트에서 `axios`를 직접 import하지 않습니다.** 서버 호출은 `src/api/*.js` 함수로만. 여러 화면이 공유하는 데이터면 store 액션을 거칩니다.
4. `client.js`, `errorMessages.js`, `session.js`, `router/index.js`는 **공통 파일**입니다. 담당자 1명이 고치고, 다른 사람은 요청합니다.
5. import 경로는 `@/` 별칭을 씁니다. `../../..` 금지.

### 8.2 반응형과 데이터 흐름

6. 부모 → 자식은 `defineProps`, 자식 → 부모는 `defineEmits`. **props를 직접 수정하지 않습니다.** 편집이 필요하면 로컬 복사본(`reactive`)을 만듭니다 (6.5 참고).
7. Pinia에서 값(state·getter)을 꺼낼 때는 `storeToRefs(store)`. 그냥 구조분해하면 반응형이 끊겨 화면이 안 바뀝니다. 액션은 그냥 꺼내도 됩니다.
8. `v-for`에는 항상 고유한 `:key`(게시글은 `postId`). 배열 index를 key로 쓰지 않습니다.
9. `v-if`와 `v-for`를 같은 태그에 쓰지 않습니다. 걸러야 하면 `computed`로 거른 배열을 돌립니다.
10. 원본 배열을 `sort()`·`splice()`로 직접 바꾸지 않습니다. 가공은 `computed`에서 새 배열로.

### 8.3 API·에러

11. 화면 분기는 `e.code`로만. `e.message`는 화면에 표시하는 용도.
12. 모든 요청 함수는 `try / catch / finally`. 로딩 해제는 `finally`에서.
13. 입력 에러는 `e.fieldErrors[필드명]`을 `<FieldError>`로 해당 칸 아래에 표시.
14. 204 응답은 `null`을 반환합니다. 반환값을 쓰지 않습니다.
15. 날짜는 `formatDate` / `formatDateTime`으로만 표시. `createdAt.slice(0, 10)` 금지 (UTC 날짜가 잘려 나와 한국 시간과 하루 어긋날 수 있음).

### 8.4 보안

16. **`v-html` 금지.** 게시글 본문은 평문이므로 `{{ }}` + `white-space: pre-wrap`.
17. 사용자 입력 URL은 `isSafeUrl` 통과 후에만 링크. 외부 링크는 `rel="noopener noreferrer"`.
18. 비밀번호·토큰을 `console.log` 하지 않습니다.
19. 로그인 후 `redirect` 쿼리는 `/`로 시작하는 내부 경로만 허용 (7.1).
20. 토큰은 `localStorage`에 저장합니다(Access Token 단일 방식, MVP). 이 방식은 XSS에 토큰이 노출될 수 있으므로 16·17번 규칙이 토큰 보호와 직결됩니다.

### 8.5 입력

21. 한글 입력 중 Enter 처리는 `e.isComposing` 확인 (6.4).
22. 입력할 때마다 API를 부르는 기능을 만들면 `:value` + `@input`을 쓰고 디바운스를 겁니다. `v-model`은 한글 조합이 끝나야 값이 갱신됩니다.
23. 입력값은 보낼 때 `trim()`. 비밀번호는 trim하지 않습니다.

### 8.6 Git

24. 브랜치는 기능 단위(`feat/front-login`, `feat/front-post-list`). 한 브랜치에 여러 화면을 담지 않습니다.
25. `main`, `develop` 직접 push 금지 (결정 보드 논점 02의 브랜치 보호 규칙).
26. `.env.*.local`은 커밋하지 않습니다.

---

## 9. 자주 헷갈리는 지점 모음

| 증상 | 원인 | 해결 |
|---|---|---|
| 첫 화면에서 "getActivePinia was called with no active Pinia" | `main.js`에서 router를 Pinia보다 먼저 `use` | Pinia → router 순서 (5.1) |
| 비밀번호를 틀렸는데 로그아웃됨 | 인터셉터가 상태 코드 401로 세션 삭제 | `code === 'UNAUTHORIZED'`일 때만 (5.4) |
| 해시태그 검색이 서버에서 안 먹음 | axios 기본 직렬화가 `hashtag[]=JWT` | `paramsSerializer: { indexes: null }` (5.4) |
| 목록 첫 페이지가 두 번 보이거나 한 페이지가 빠짐 | 화면 1부터 / API 0부터 변환 누락 | `toApiParams` 한 곳에서만 `-1` (7.5) |
| 닉네임을 바꿨는데 헤더가 그대로 | store를 구조분해해서 반응형이 끊김 | `storeToRefs` (6.1) |
| 태그를 한글로 치고 Enter 치면 마지막 글자가 따로 한 번 더 들어감 | 조합 중 keydown | `e.isComposing` 체크 (6.4) |
| 작성일이 하루 전 날짜로 보임 | UTC 문자열을 잘라서 표시 | `formatDate` (5.9) |
| 상세에서 다른 글 링크를 눌렀는데 내용이 안 바뀜 | 같은 컴포넌트가 재사용되어 `onMounted`가 다시 안 돎 | `watch(() => props.postId, ..., { immediate: true })` (7.6) |
| 목록 첫 진입 때 요청이 2번 | `watch immediate`와 `onMounted`를 같이 씀 | 하나만 (7.5) |
| `/posts/new`가 상세 화면으로 열림 | `:postId`가 `new`를 잡음 | `:postId(\\d+)` (5.7) |
| 수정 폼에서 부모 데이터가 같이 바뀜 / props 수정 경고 | props 객체를 `v-model`에 직접 연결 | 로컬 복사본 (6.5) |
| 로그인 상태인데 새로고침하면 로그아웃됨 | 토큰을 store에만 두고 저장 안 함 | `session.set` (5.6) |

---

## 10. 백엔드가 준비되기 전에 작업하는 법

- 화면은 `src/api/*.js` 함수만 바라보므로, 백엔드가 늦으면 **해당 api 파일의 함수만** 임시로 응답 명세의 예시 JSON을 돌려주게 바꿔서 화면을 먼저 만듭니다.
- 이때도 반환 모양은 봉투를 벗긴 `data`와 같아야 합니다(인터셉터를 거친 뒤 모양).
- 백엔드 API가 머지되면 임시 코드를 지우고 실제 호출로 되돌립니다. 임시 코드는 `// MOCK:` 주석을 달아 검색으로 찾을 수 있게 합니다.

```js
// src/api/post.js — 백엔드 준비 전 임시
export const postApi = {
  // MOCK: 게시글 목록 API 머지 후 삭제
  list: async () => ({
    content: [{ postId: 12, title: 'JWT 정리', nickname: '기택', hashtags: ['JWT', 'Spring'], createdAt: '2026-09-19T05:30:00Z' }],
    page: 0, size: 10, totalElements: 1, totalPages: 1, hasNext: false,
  }),
  // ...
}
```

---

## 11. 구현 순서 체크리스트

### 0단계 · 공통 틀 (담당 1명, 머지 전 화면 작업 금지)
- [ ] create-vue로 생성, axios 설치, `vite.config.js` 프록시·별칭, `.env.development`
- [ ] `utils/session.js`, `utils/date.js`, `utils/url.js`
- [ ] `constants/errorMessages.js`, `constants/rules.js`
- [ ] `api/client.js` + `api/auth.js`·`user.js`·`post.js`
- [ ] `stores/auth.js`, `stores/post.js`
- [ ] `router/index.js` (모든 라우트 + 가드, View는 빈 파일로라도 생성)
- [ ] `App.vue`, `AppHeader.vue`, `FieldError.vue`, `NotFoundView.vue`
- [ ] ESLint·Prettier 통과 확인 후 머지

### 1단계 · 인증
- [ ] 로그인 (+ `?expired=1`, `redirect`, `ACCOUNT_WITHDRAWN`)
- [ ] 로그아웃 (헤더)
- [ ] 회원가입 (중복 확인 2종 + 확인 상태 초기화)
- [ ] 비밀번호 찾기
- [ ] 비밀번호 변경 (강제 모드 / 일반 모드)

### 2단계 · 게시글 조회
- [ ] 목록 + 페이지네이션 (URL 쿼리 동기화)
- [ ] 검색 (keyword·hashtag 복수·nickname·date) + 내 글 보기
- [ ] 상세 (`isMine`, 안전한 URL, 404 처리)

### 3단계 · 게시글 쓰기
- [ ] `HashtagInput`, `PostForm`
- [ ] 글쓰기
- [ ] 글 수정 (본인 확인, 통째 교체)
- [ ] 삭제

### 4단계 · 마이페이지
- [ ] 비밀번호 재확인 → 내 정보
- [ ] 닉네임 수정 (헤더 즉시 반영)
- [ ] 탈퇴 (`recoverableUntil` 표시)

### 5단계 · 마무리 점검
- [ ] 4.3 에러 코드 표의 모든 코드를 실제로 발생시켜 화면 처리 확인 (토큰 없이 호출, 남의 글 수정, 없는 글 조회, 비밀번호 틀리기)
- [ ] 로딩·빈 결과·에러 상태가 모든 화면에 있음
- [ ] `v-html`, 컴포넌트 내 `axios` 직접 import, `console.log` 검색해서 0건
- [ ] 새로고침 후 로그인 유지, 토큰 만료 후 로그인 화면 이동
- [ ] 뒤로 가기 시 목록 검색 조건·페이지 복원
- [ ] 모바일 폭에서 가로 스크롤 없음

---

## 12. 미결 사항

값이 정해지면 **표시된 파일만** 고치면 되도록 코드가 짜여 있습니다.

### 12.1 팀 안건 (D-xx)

| 안건 | 내용 | 프론트 영향 | 고칠 곳 |
|---|---|---|---|
| D-01 | 항목별 검색 vs 통합 검색(keyword) | 검색바 입력 칸 구성 | `PostSearchBar.vue`, `toApiParams` |
| D-03 | 매칭 방식(부분/정확), 조건 여럿일 때 AND/OR | 안내 문구 | `PostSearchBar.vue` |
| D-04 | 해시태그 복수 검색 AND/OR | 안내 문구 | `PostSearchBar.vue` |
| D-05 | 페이지 시작값, 목록 노출 항목 | 현재 API 0부터로 구현. 1부터로 바뀌면 `-1` 제거 | `toApiParams` |
| D-06 | 수정 API에 해시태그 포함 여부 | 현재 포함으로 구현 | `PostEditView.vue`, `PostDetailView.vue` |
| D-08 | 제목·본문 길이, URL 최대 개수 | 프론트 사전 검증 | `constants/rules.js` |
| D-09 | 해시태그 표기·최대 개수·대소문자 | 입력 제한 | `constants/rules.js` |
| D-10 | 30일 내 계정 복구 | 로그인 화면 복구 버튼 | `LoginView.vue`, `api/auth.js` |
| D-11 | 탈퇴 이메일 재가입 | 이메일 확인 `WITHDRAWN` 처리 | `SignupView.vue` |
| D-12 | 임시 비밀번호 유효기간 | 만료 코드 추가 시 문구 | `errorMessages.js`, `LoginView.vue` |
| D-15 | 비밀번호 확인과 정보 조회 분리 | `GET /users/me` 추가 여부 | `api/user.js`, `MyPageView.vue` |
| D-16 | 비밀번호 정규식, 닉네임 길이 | 프론트 사전 검증 | `constants/rules.js` |
| D-17 | 임시 비밀번호 안내 문구 | 완료 문구 | `PasswordFindView.vue` |
| — | 로그아웃 API 존치 여부 | 없어지면 `authApi.logout` 호출만 제거 | `stores/auth.js` |
| — | 게시글 비로그인 열람 | 라우트 meta | `router/index.js` |
| — | Access Token 유효기간 (응답 명세 예시 7200초) | 코드는 `expiresIn`을 읽으므로 변경 없음 | 없음 |
| — | 프론트 운영 배포 위치·도메인 | `.env.production`, 백엔드 CORS | 배포 설정 |

### 12.2 문서 간 불일치 (확인 필요)

이 지침은 **응답 명세(9/18 최신)** 를 따랐습니다. 아래는 다른 문서와 값이 다른 부분이라, 백엔드 담당과 한 번 맞춰야 합니다.

| # | 항목 | 응답 명세 (이 지침 기준) | 다른 문서 |
|---|---|---|---|
| F-1 | 해시태그 응답 모양 | 문자열 배열 `["JWT", "Spring"]` | API 프론트 가이드(9/16): `[{ hashtagId, name }]` |
| F-2 | 해시태그 삭제 식별자 | URL은 `{hashtagId}`인데 응답에 id가 없음 | → F-1을 객체 배열로 바꾸거나, 삭제 URL을 태그 이름 기준으로 바꿔야 삭제 기능 구현 가능 |
| F-3 | 에러 코드 형식 | 문자열 코드 `POST_NOT_OWNED` + `details` | 결정 보드 논점 03 예시: `M001`/`P002` 형식 + `errors` |
| F-4 | 페이지 응답 필드 | `hasNext`, page 0부터 | 결정 보드 논점 04 예시: `last`, `one-indexed-parameters: true`(1부터) |
| F-5 | 중복 에러 상태 코드 | 400 | API 프론트 가이드(9/16): 409 |
| F-6 | 비밀번호 불일치 상태 코드 | 401 | API 프론트 가이드(9/16): 400 권장 (401이면 로그아웃 위험) → 이 지침은 인터셉터를 code 기준으로 짜서 둘 다 동작함 |
| F-7 | 게시글 작성자 필드 | `author: { userId, nickname }` (등록·상세), 목록은 `nickname` | API 프론트 가이드: 전부 `nickname` |

---

## 13. 요약

- 흐름은 `index.html → main.js(Pinia → router → mount) → App.vue(RouterView) → router 가드 → View → Component`, 데이터는 `View/Store → api/*.js → client.js → 백엔드`.
- 봉투 해제, 토큰 첨부, 에러 문구 변환은 `client.js` 한 곳. 컴포넌트는 `e.code`, `e.message`, `e.fieldErrors`만 봅니다.
- 세션 삭제는 `UNAUTHORIZED`에서만. 같은 401인 `PASSWORD_MISMATCH`·`LOGIN_FAILED`로는 로그아웃시키지 않습니다.
- 목록 검색 조건과 페이지는 URL 쿼리에 둡니다. 화면은 1부터, API는 0부터, 변환은 한 함수에서.
- `v-html` 금지, 사용자 URL은 `http(s)`만 링크, 날짜는 KST 변환 함수로만.
- 정해지지 않은 값은 `rules.js`와 12장 표에 모아 두었습니다. 추측으로 채우지 않습니다.
