# 일일로그(ilog) Vue 화면 구현 지침 — UI 편

> 이 문서는 **화면 모양**을 정합니다. 함께 쓰는 문서는 두 개입니다.
> - `docs/frontend-guide.md` — 일일로그 Vue 프론트엔드 구현 지침 (API 계약, 라우터, 스토어, 에러 처리, 코딩 규칙)
> - `docs/prototype.html` — 팀 와이어프레임 기준으로 만든 동작 프로토타입 (브라우저로 열어서 모양 확인용)
>
> 두 문서가 다를 때의 우선순위는 [0.2](#02-우선순위)를 따릅니다.

---

## 0. 이 문서 사용법

### 0.1 Claude Code에 넣는 방법

세 파일을 레포의 `docs/`에 두고, 단계별로 범위를 좁혀서 요청합니다.

```text
docs/frontend-guide.md 와 docs/ui-spec.md 를 읽어.
ui-spec.md 2~5장(스타일 토큰, 전역 CSS, index.html, App.vue)만 먼저 적용해줘.
frontend-guide.md 8장 코딩 규칙을 지키고, 공통 파일(client.js, errorMessages.js, session.js, router/index.js)은 수정하지 마.
ui-spec.md 10.2 주석 태그 규칙대로, 10.3 표에 있는 위치에는 @metric 주석을 달아.
```

```text
ui-spec.md 6장 공통 컴포넌트를 만들어줘. props/emit 계약은 frontend-guide.md 6장을 따르고, 모양과 마크업은 ui-spec.md를 따라.
10.3 표의 위치에는 @metric 주석을 달아.
```

```text
ui-spec.md 10.5 방법으로 측정할 수 있게 준비됐는지 점검해줘.
grep -rn "@metric" src index.html 결과를 10.3 표와 비교해서 빠진 태그를 알려줘.
```

```text
ui-spec.md 7.5 게시글 목록 화면을 만들어줘. 스크립트 로직은 frontend-guide.md 7.5, 마크업과 스타일은 ui-spec.md 7.5.
docs/prototype.html 의 메인 화면과 모양이 같아야 해. 프로토타입의 JS 로직(목업 데이터, hash 라우터)은 옮기지 마.
```

### 0.2 우선순위

| 영역 | 따르는 문서 |
|---|---|
| API 호출, 요청·응답 필드, 에러 코드 분기, 라우터·가드, 스토어, 보안 규칙 | `frontend-guide.md` |
| 색, 글꼴, 크기, 간격, 배치, 버튼 종류, 화면 문구 | `ui-spec.md` (이 문서) |
| 이 문서에 없는 모양 | `prototype.html`을 열어서 확인 |

프로토타입에는 API 계약과 맞지 않는 동작이 섞여 있습니다(데모 계정, 임시 비밀번호를 화면에 보여주는 팝업, 탈퇴 계정 복구 팝업 등). 이런 동작은 옮기지 않습니다. 목록은 [9장](#9-프로토타입과-달라지는-부분)에 있습니다.

---

## 1. 디자인 원칙

- **배치는 팀 와이어프레임**, **글꼴·컴포넌트 모양·포인트 색은 디자인 시스템 캡처**(글씨체, 버튼·입력·팝업·표·해시태그, 포스트잇 4색)를 따릅니다.
- 코르크 보드 배경은 쓰지 않습니다. 배경은 흰색입니다.
- **다크 모드는 만들지 않습니다.** `color-scheme: light`로 고정합니다. (OS가 다크 모드여도 밝은 화면)
- 종이는 거의 직각(`border-radius: 3px`), 그림자는 짧게.
- 제목·로고는 손글씨(Gaegu), 읽는 글·버튼은 고딕(Noto Sans KR), **숫자(날짜·시간·번호)는 예외 없이 mono(IBM Plex Mono)**.

---

## 2. 추가·변경할 파일 목록

| 파일 | 작업 | 비고 |
|---|---|---|
| `index.html` | 수정 | 폰트 링크, `color-scheme` 메타 (3장) |
| `src/assets/styles/tokens.css` | 새로 | 디자인 토큰 (4장) |
| `src/assets/styles/base.css` | 새로 | 전역 공통 클래스 (5장) |
| `src/main.js` | 수정 | 전역 CSS import, 에러 기록, 개발용 지표 로그 (10.4) |
| `src/utils/vitals.js` | 새로 | 개발 중 Web Vitals 콘솔 출력 (10.4), `npm install -D web-vitals` |
| `src/App.vue` | 수정 | 화면 프레임 + `AppDialog` (5.3) |
| `src/utils/date.js` | 함수 추가 | 점 표기 날짜, 시각, 오늘 여부 (5.4) |
| `src/composables/useDialog.js` | 새로 | `alert`/`confirm` 대신 쓰는 팝업 (6.2) |
| `src/components/common/AppDialog.vue` | 새로 | 팝업 UI (6.2) |
| `src/components/common/AppHeader.vue` | 수정 | 와이어프레임 탭 구조 (6.1) |
| `src/components/common/FieldError.vue` | 수정 | 클래스만 변경 (6.3) |
| `src/components/common/BasePagination.vue` | 새로 | (6.4) |
| `src/components/common/PasswordRuleList.vue` | 새로 | 비밀번호 조건 표시 (6.5) |
| `src/components/post/HashtagInput.vue` | 수정 | 추가 버튼, 중복 문구, 칩 (6.6) |
| `src/components/post/UrlInput.vue` | 새로 | URL 입력 + 목록 (6.7) |
| `src/components/post/PostForm.vue` | 수정 | 와이어프레임 배치 (6.8) |
| `src/components/post/PostSearchBar.vue` | 새로 | (6.9) |
| `src/components/post/PostListItem.vue` | 새로 | 표의 한 행 `<tr>` (6.10) |
| `src/views/AboutView.vue` | 새로 | ABOUT 화면 (7.10) |
| `src/router/index.js` | **한 줄 추가 요청** | `/about` 라우트. 공통 파일이라 담당자에게 요청 (7.10) |

create-vue가 만든 기본 파일은 지웁니다: `src/components/HelloWorld.vue`, `TheWelcome.vue`, `WelcomeItem.vue`, `src/components/icons/`, `src/assets/base.css`, `src/assets/main.css`, `src/assets/logo.svg`, 기본 `src/views/HomeView.vue`, 기본 `src/views/AboutView.vue`(7.10으로 새로 만듦). `main.js`의 `import './assets/main.css'`도 지웁니다.

---

## 3. `index.html`

```html
<!DOCTYPE html>
<html lang="ko">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <meta name="color-scheme" content="light" />
    <title>iLog</title>
    <!-- @metric LCP, FCP: 폰트 서버에 미리 연결해 폰트 도착을 앞당긴다 -->
    <link rel="preconnect" href="https://fonts.googleapis.com" />
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
    <!-- @metric FCP: display=swap 으로 폰트가 오기 전에도 대체 글꼴로 글자를 먼저 그린다 -->
    <link
      href="https://fonts.googleapis.com/css2?family=Gaegu:wght@400;700&family=IBM+Plex+Mono:wght@400;500;600&family=Noto+Sans+KR:wght@400;500;700&display=swap"
      rel="stylesheet"
    />
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
```

---

## 4. 디자인 토큰 `src/assets/styles/tokens.css`

색·글꼴·크기는 **이 파일의 변수로만** 씁니다. 컴포넌트 CSS에 `#C1503F` 같은 값을 직접 쓰지 않습니다.

```css
:root {
  color-scheme: light;

  /* 표면 */
  --bg: #ffffff;
  --paper: #ffffff;
  --line: #d1cbc1;        /* 카드 테두리, 칩 테두리, 구분선 */
  --line-soft: #e5e0d3;   /* 표 테두리 */
  --head: #f3efe6;        /* 표 헤더 */

  /* 텍스트 */
  --text: #261f1a;
  --text-2: #2b2117;      /* 입력값, 표 본문 */
  --sub: #564e48;
  --muted: #867f79;
  --label: #8a8072;
  --placeholder: #b8b0a2;

  /* 강조 · 버튼 · 입력 */
  --accent: #d15c53;      /* 에러 문구, 브랜드 eyebrow */
  --btn: #c1503f;         /* 주요 버튼, 입력 포커스 밑줄, 표 번호 */
  --btn-text: #fbf8f4;
  --input: #c9c2b2;       /* 입력 밑줄, 보조 버튼 테두리 */
  --ok: #2f8a4a;          /* 성공 문구 (디자인 시스템에 없어 임시 지정) */
  --hashtag: #0f4c6b;
  --chip-x: #e5e0d3;
  --row-hover: #faf8f4;

  /* 팝업 */
  --modal: #ffffff;
  --overlay: rgba(43, 33, 23, 0.35);

  /* 포스트잇 4색 */
  --postit-yellow: #f6e084; /* 기본 · 이번 주 주제 → ABOUT */
  --postit-pink: #ffc4c8;   /* 마감 · 오늘 미작성 → 현재 탭, 경고 안내 */
  --postit-blue: #b1e5ff;   /* 체크리스트 · 안내 → 일반 안내 박스 */
  --postit-green: #c1edb7;  /* 연속 기록 · 칭찬 → 아직 미사용 */
  --postit-text: #261f1a;

  /* 글꼴 */
  --font-hand: 'Gaegu', 'Noto Sans KR', cursive;
  --font-body: 'Noto Sans KR', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;
  --font-mono: 'IBM Plex Mono', ui-monospace, 'SFMono-Regular', Menlo, monospace;

  /* 크기 스케일 (1.25 배율) */
  --fs-4xl: 46px;  /* 로그인 브랜드 */
  --fs-3xl: 36px;  /* 페이지 제목 */
  --fs-2xl: 28px;  /* 섹션 제목 · 카운터 */
  --fs-xl: 22px;   /* 카드 제목 · 포스트잇 손글씨 */
  --fs-lg: 18px;   /* 큰 본문 · 리드 문장 */
  --fs-md: 16px;   /* 본문 기본 */
  --fs-sm: 14px;   /* 탭 · 입력값 */
  --fs-xs: 12px;   /* 캡션 · 라벨 · 에러 문구 */
  --fs-2xs: 11px;  /* 입력 라벨, 표 헤더 */

  /* 모양 */
  --radius: 3px;
  --radius-chip: 19px;
  --shadow-paper: 0 1px 2px rgba(38, 31, 26, 0.08);
  --shadow-postit: 2px 2px 10px rgba(119, 119, 119, 0.25);
  --shadow-modal: 0 2px 6px rgba(43, 33, 23, 0.2);
}
```

---

## 5. 전역 스타일과 앱 틀

### 5.1 `src/assets/styles/base.css`

여러 화면에서 반복되는 요소(버튼, 입력, 라벨, 문구, 칩, 안내 박스)만 전역 클래스로 둡니다. 화면 배치는 각 컴포넌트의 `<style scoped>`에 씁니다.

```css
@import './tokens.css';

*, *::before, *::after { box-sizing: border-box; }
html { -webkit-text-size-adjust: 100%; }
body {
  margin: 0;
  background: var(--bg);
  color: var(--text);
  font-family: var(--font-body);
  font-size: var(--fs-md);
  line-height: 1.5;
}
h1, h2, h3, p, ul { margin: 0; }
ul { padding: 0; list-style: none; }
a { color: inherit; }
/* @metric A11Y: 키보드 포커스 위치가 항상 보이게 한다 */
:focus-visible { outline: 2px solid var(--btn); outline-offset: 2px; }

.num { font-family: var(--font-mono); font-feature-settings: 'tnum'; }
.sr-only {
  position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px;
  overflow: hidden; clip: rect(0 0 0 0); white-space: nowrap; border: 0;
}

/* ===== 화면 프레임 ===== */
/* @metric CLS: min-height 로 화면 전환·로딩 중 프레임 높이가 출렁이지 않게 한다 */
.page {
  max-width: 720px;
  min-height: 560px;
  margin: 32px auto;
  padding: 26px 40px 48px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow-paper);
}
.page.page-flush { padding: 0; min-height: 420px; }
@media (max-width: 760px) {
  .page { margin: 0; border-width: 0 0 1px; border-radius: 0; padding: 20px 18px 36px; min-height: 100vh; }
  .page.page-flush { min-height: 0; }
}

/* 로그인 전 화면 제목 (회원가입, 비밀번호 찾기·변경) */
.auth-top { padding: 4px 0 14px; border-bottom: 1px solid var(--line); }
.auth-top h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }

/* ===== 버튼 ===== */
.btn {
  display: inline-flex; align-items: center; justify-content: center;
  min-height: 36px; padding: 8px 18px;
  background: transparent; color: var(--text-2);
  border: 1px solid var(--input); border-radius: var(--radius);
  font-family: var(--font-body); font-size: 13px; font-weight: 400; line-height: 16px;
  cursor: pointer; white-space: nowrap; text-decoration: none;
}
.btn:hover { background: rgba(193, 80, 63, 0.06); }
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary { background: var(--btn); border-color: var(--btn); color: var(--btn-text); }
.btn-primary:hover { background: var(--btn); filter: brightness(0.93); }
.btn-sm { min-height: 26px; padding: 3px 10px; font-size: var(--fs-xs); }
.btn-block { width: 100%; }
.btn-group { display: flex; gap: 8px; flex-wrap: wrap; }

/* ===== 입력 (밑줄형) ===== */
.box {
  width: 100%; height: 36px; padding: 6px 2px;
  background: transparent; color: var(--text-2);
  border: 0; border-bottom: 1px solid var(--input); border-radius: 0;
  font-family: var(--font-body); font-size: var(--fs-sm);
}
.box::placeholder { color: var(--placeholder); }
.box:focus { outline: none; border-bottom: 2px solid var(--btn); padding-bottom: 5px; }
textarea.box {
  height: auto; min-height: 110px; padding: 10px 12px;
  border: 1px solid var(--input); border-radius: var(--radius);
  line-height: 1.7; resize: vertical;
}
textarea.box:focus { border: 2px solid var(--btn); padding: 9px 11px; }
select.box { cursor: pointer; }

/* 입력 불가 상태 (읽기 전용 값) */
.readonly {
  min-height: 36px; padding: 7px 2px 6px;
  border-bottom: 2px solid var(--text-2);
  font-size: var(--fs-sm); color: var(--text-2);
}

/* ===== 필드 · 라벨 · 문구 ===== */
.field { margin-bottom: 26px; }
.label { display: block; margin-bottom: 4px; font-size: var(--fs-2xs); line-height: 13px; color: var(--label); }
.label-lg { display: block; margin-bottom: 6px; font-size: var(--fs-xs); font-weight: 500; line-height: 15px; color: var(--label); }
.label-row { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-bottom: 6px; }
.label-row .label-lg { margin: 0; }
.helper { font-size: var(--fs-xs); color: var(--muted); }
.row { display: flex; align-items: flex-end; gap: 10px; }
.row > .box { flex: 1; min-width: 0; }

.msg { margin-top: 6px; font-size: var(--fs-xs); line-height: 15px; }
.msg-err { color: var(--accent); }
.msg-ok { color: var(--ok); }
.msg-hint { color: var(--muted); }
.label-row .msg { margin: 0; }

/* ===== 해시태그 칩 ===== */
.chips { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px;
  background: var(--paper); color: var(--hashtag);
  border: 1px solid var(--line); border-radius: var(--radius-chip);
  font-family: var(--font-body); font-size: var(--fs-xs); line-height: 15px;
}
button.chip { cursor: pointer; }
button.chip:hover { border-color: var(--hashtag); }
.chip-sm { padding: 2px 8px; font-size: var(--fs-2xs); line-height: 14px; }
.chip-x {
  width: 14px; height: 14px; padding: 0;
  border: 0; border-radius: 50%;
  background: var(--chip-x); color: var(--text-2);
  font-size: 10px; line-height: 14px; cursor: pointer;
}

/* ===== 안내 박스 (포스트잇) ===== */
.notice {
  margin-bottom: 18px; padding: 12px 14px;
  background: var(--postit-blue); color: var(--postit-text);
  font-size: 13px; line-height: 1.6;
  box-shadow: var(--shadow-postit);
}
.notice-warn { background: var(--postit-pink); }
```

### 5.2 `src/main.js`

구현 지침 5.1 코드에 import 한 줄을 추가합니다. 에러 기록과 개발용 지표 로그까지 넣은 최종본은 [10.4](#104-에러-기록과-개발용-지표-로그)입니다.

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@/assets/styles/base.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

### 5.3 `src/App.vue`

구현 지침 5.11을 이렇게 바꿉니다. 헤더와 본문을 한 장의 "종이"(`.page`) 안에 넣고, 팝업을 한 번만 올립니다.

```vue
<template>
  <div class="page" :class="{ 'page-flush': route.name === 'login' }">
    <AppHeader v-if="showHeader" />
    <main>
      <RouterView />
    </main>
  </div>
  <AppDialog />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppHeader from '@/components/common/AppHeader.vue'
import AppDialog from '@/components/common/AppDialog.vue'

const route = useRoute()
const auth = useAuthStore()

// 헤더를 숨기는 경우: 비로그인 화면, 로그인 안 된 상태(404 등), 강제 비밀번호 변경 화면
// 마이페이지에서 들어온 일반 비밀번호 변경 화면에는 헤더를 보여준다
const showHeader = computed(() => {
  if (route.meta.guestOnly || !auth.hasValidToken()) return false
  if (route.name === 'password-change' && auth.passwordResetRequired) return false
  return true
})
</script>
```

### 5.4 `src/utils/date.js` 에 추가

화면 표기는 `2026.09.19`, `14:20`입니다. 구현 지침의 `formatDate`(`2026. 09. 19.`)는 그대로 두고 함수를 추가합니다. 날짜 표시는 이 파일의 함수로만 합니다(구현 지침 8.3-15).

```js
const partsFmt = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
})

const kstParts = (value) =>
  Object.fromEntries(partsFmt.formatToParts(new Date(value)).map((p) => [p.type, p.value]))

/** 2026.09.19 */
export const formatDotDate = (value) => {
  if (!value) return ''
  const p = kstParts(value)
  return `${p.year}.${p.month}.${p.day}`
}

/** 14:20 */
export const formatClock = (value) => {
  if (!value) return ''
  const p = kstParts(value)
  return `${p.hour}:${p.minute}`
}

/** KST 기준 오늘인지 */
export const isTodayKst = (value) => !!value && formatDotDate(value) === formatDotDate(Date.now())
```

---

## 6. 공통 컴포넌트

### 6.1 `AppHeader.vue`

와이어프레임 상단: 로고 · HOME / ABOUT / 마이페이지 탭 · 닉네임 · 로그아웃. 현재 탭은 분홍 포스트잇 색.

```vue
<template>
  <header class="app-header">
    <RouterLink class="logo" :to="{ name: 'post-list' }">iLog</RouterLink>
    <nav class="tabs" aria-label="주요 메뉴">
      <RouterLink
        v-for="tab in tabs"
        :key="tab.name"
        class="tab"
        :class="{ 'is-active': tab.active }"
        :to="{ name: tab.name }"
      >
        {{ tab.label }}
      </RouterLink>
    </nav>
    <span v-if="user" class="nickname">{{ user.nickname }}님</span>
    <button type="button" class="btn btn-sm" @click="onLogout">로그아웃</button>
  </header>
</template>

<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const { user } = storeToRefs(auth)
const { logout } = auth
const route = useRoute()
const router = useRouter()

const tabs = computed(() => [
  { name: 'post-list', label: 'HOME', active: String(route.name).startsWith('post') },
  { name: 'about', label: 'ABOUT', active: route.name === 'about' },
  { name: 'mypage', label: '마이페이지', active: route.name === 'mypage' || route.name === 'password-change' },
])

async function onLogout() {
  await logout()
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.app-header { display: flex; align-items: center; gap: 16px; height: 50px; border-bottom: 1px solid var(--line); }
.logo { flex: none; font-family: var(--font-hand); font-weight: 700; font-size: 26px; line-height: 1; text-decoration: none; }
.tabs { display: flex; flex: 1; gap: 4px; min-width: 0; }
.tab { padding: 4px 10px; border-radius: 2px; font-size: var(--fs-sm); font-weight: 500; color: var(--sub); text-decoration: none; white-space: nowrap; }
.tab:hover { color: var(--text); }
.tab.is-active { background: var(--postit-pink); color: var(--postit-text); font-weight: 700; }
.nickname { font-size: 13px; color: var(--muted); white-space: nowrap; }
@media (max-width: 560px) { .nickname { display: none; } }
</style>
```

> 구현 지침 6.1의 헤더 "글쓰기" 링크는 와이어프레임에 따라 목록 화면의 `+ 글쓰기` 버튼으로 옮깁니다.

### 6.2 팝업 `useDialog.js` + `AppDialog.vue`

`window.alert` / `window.confirm` 대신 씁니다. 구현 지침에서 `alert(e.message)`, `confirm`이라고 적힌 곳은 모두 이것으로 바꿉니다.

```js
// src/composables/useDialog.js
import { reactive, readonly } from 'vue'

// 앱 전체에서 팝업은 하나만 뜨므로 모듈 단위 상태로 둔다
const state = reactive({
  open: false,
  title: '',
  description: '',
  confirmText: '확인',
  cancelText: '',
  resolve: null,
})

function show(options) {
  return new Promise((resolve) => {
    Object.assign(state, {
      open: true,
      title: options.title,
      description: options.description ?? '',
      confirmText: options.confirmText ?? '확인',
      cancelText: options.cancelText ?? '',
      resolve,
    })
  })
}

function close(result) {
  state.open = false
  state.resolve?.(result)
  state.resolve = null
}

export function useDialog() {
  return {
    state: readonly(state),
    /** 확인 버튼 하나. await 하면 닫힐 때까지 기다린다 */
    alert: (options) => show(options),
    /** 취소 + 실행 버튼. true(실행) / false(취소) */
    confirm: (options) => show({ cancelText: '취소', ...options }),
    close,
  }
}
```

```vue
<!-- src/components/common/AppDialog.vue -->
<template>
  <Teleport to="body">
    <div v-if="state.open" class="overlay" @click.self="close(false)">
      <!-- @metric A11Y: role="dialog" + aria-modal, 열리면 확인 버튼에 포커스, Esc 로 닫기 -->
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="app-dialog-title" @keydown.esc="close(false)">
        <p id="app-dialog-title" class="dialog-title">{{ state.title }}</p>
        <p v-if="state.description" class="dialog-desc">{{ state.description }}</p>
        <div class="dialog-actions">
          <button v-if="state.cancelText" type="button" class="btn" @click="close(false)">
            {{ state.cancelText }}
          </button>
          <button ref="confirmButton" type="button" class="btn btn-primary" @click="close(true)">
            {{ state.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'
import { useDialog } from '@/composables/useDialog'

const { state, close } = useDialog()
const confirmButton = ref(null)

watch(
  () => state.open,
  async (open) => {
    if (!open) return
    await nextTick()
    confirmButton.value?.focus()
  },
)
</script>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 50; display: grid; place-items: center; padding: 20px; background: var(--overlay); }
.dialog {
  width: 300px; max-width: 100%; padding: 26px 24px 22px;
  background: var(--modal); color: var(--text-2);
  border-radius: 4px; box-shadow: var(--shadow-modal);
  font-size: var(--fs-sm); line-height: 1.5;
}
.dialog-desc { margin-top: 4px; font-size: var(--fs-xs); color: var(--label); white-space: pre-line; }
.dialog-actions { display: flex; gap: 10px; margin-top: 20px; }
.dialog-actions .btn { flex: 1; }
</style>
```

사용 예:

```js
import { useDialog } from '@/composables/useDialog'
const dialog = useDialog()

const ok = await dialog.confirm({ title: '게시글을 정말로 삭제하시겠습니까?', confirmText: '글 삭제' })
if (!ok) return

await dialog.alert({ title: '게시글이 삭제되었습니다.' })
```

버튼 순서는 디자인 시스템대로 **취소가 왼쪽, 실행이 오른쪽**입니다.

### 6.3 `FieldError.vue`

구현 지침 6.2에서 클래스만 바꿉니다.

```vue
<template>
  <p v-if="message" class="msg msg-err" role="alert">{{ message }}</p>
</template>

<script setup>
defineProps({ message: { type: String, default: '' } })
</script>
```

### 6.4 `BasePagination.vue`

계약은 구현 지침 6.3과 같습니다(`page`는 1부터, `change(page)` emit). 한 번에 최대 5개 번호를 보여줍니다.

```vue
<template>
  <nav v-if="totalPages > 0" class="pagination" aria-label="페이지">
    <button type="button" class="btn btn-sm" :disabled="page <= 1" @click="emit('change', page - 1)">이전</button>
    <button
      v-for="n in pages"
      :key="n"
      type="button"
      class="btn btn-sm num"
      :class="{ 'btn-primary': n === page }"
      :aria-current="n === page ? 'page' : undefined"
      @click="emit('change', n)"
    >
      {{ n }}
    </button>
    <button type="button" class="btn btn-sm" :disabled="page >= totalPages" @click="emit('change', page + 1)">다음</button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  totalPages: { type: Number, required: true },
})
const emit = defineEmits(['change'])

const WINDOW = 5
const pages = computed(() => {
  const start = Math.max(1, Math.min(props.page - 2, props.totalPages - WINDOW + 1))
  const end = Math.min(props.totalPages, start + WINDOW - 1)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})
</script>

<style scoped>
.pagination { display: flex; justify-content: center; gap: 6px; margin-top: 24px; flex-wrap: wrap; }
.pagination .num { min-width: 32px; }
</style>
```

### 6.5 `PasswordRuleList.vue`

와이어프레임의 비밀번호 조건 표시입니다. 조건을 만족하면 원이 채워지고 취소선이 그어집니다.
**표시 전용**입니다. 비밀번호 규칙(D-16)이 확정되기 전까지 가입·변경 버튼을 이 조건으로 막지 않습니다(구현 지침 5.10).

```vue
<template>
  <ul class="rules" aria-label="비밀번호 조건">
    <li v-for="rule in rules" :key="rule.label" :class="{ 'is-on': rule.ok }">{{ rule.label }}</li>
  </ul>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ password: { type: String, default: '' } })

// TODO D-16: 확정되면 constants/rules.js 의 PASSWORD_PATTERN 과 문구를 맞춘다
const rules = computed(() => [
  { label: '영문 + 숫자 조합', ok: /[A-Za-z]/.test(props.password) && /\d/.test(props.password) },
  { label: '특수문자 가능', ok: /[^A-Za-z0-9]/.test(props.password) },
  { label: '8자 이상', ok: props.password.length >= 8 },
])
</script>

<style scoped>
.rules { display: grid; gap: 3px; margin: 8px 0 0 2px; font-size: var(--fs-2xs); color: var(--muted); }
.rules li { display: flex; align-items: center; gap: 8px; }
.rules li::before { content: ''; flex: none; width: 8px; height: 8px; border: 1px solid var(--muted); border-radius: 50%; }
.rules li.is-on { color: var(--label); text-decoration: line-through; }
.rules li.is-on::before { background: var(--text-2); border-color: var(--text-2); }
</style>
```

### 6.6 `HashtagInput.vue`

구현 지침 6.4를 와이어프레임 모양으로 바꿉니다. 입력칸 + `추가` 버튼, 중복이면 `* 중복된 값입니다.`, 아래에 칩(×로 삭제).

```vue
<template>
  <div class="hashtag-input">
    <div class="row">
      <input
        :id="inputId"
        v-model="draft"
        class="box"
        :maxlength="RULES.HASHTAG_MAX_LENGTH ?? undefined"
        placeholder="Enter 또는 쉼표로 여러 개 입력"
        @keydown.enter="onEnter"
      />
      <button type="button" class="btn" @click="add">추가</button>
    </div>
    <FieldError :message="error" />
    <ul v-if="tags.length" class="chips tag-list">
      <li v-for="tag in tags" :key="tag" class="chip">
        # {{ tag }}
        <button type="button" class="chip-x" :aria-label="`${tag} 삭제`" @click="remove(tag)">×</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import { RULES } from '@/constants/rules'

defineProps({ inputId: { type: String, default: 'hashtag-input' } })
const tags = defineModel({ type: Array, default: () => [] })
const draft = ref('')
const error = ref('')

function add() {
  error.value = ''
  const names = draft.value
    .split(',')
    .map((s) => s.trim().replace(/^#+/, ''))
    .filter(Boolean)
  if (!names.length) return

  const fresh = [...new Set(names)].filter((n) => !tags.value.includes(n))
  if (fresh.length < names.length) error.value = '* 중복된 값입니다.'

  const merged = [...tags.value, ...fresh]
  if (RULES.HASHTAG_MAX_COUNT && merged.length > RULES.HASHTAG_MAX_COUNT) {
    error.value = `* 해시태그는 ${RULES.HASHTAG_MAX_COUNT}개까지 넣을 수 있어요.`
  }
  tags.value = RULES.HASHTAG_MAX_COUNT ? merged.slice(0, RULES.HASHTAG_MAX_COUNT) : merged
  draft.value = ''
}

function onEnter(e) {
  if (e.isComposing) return // 한글 조합 중 Enter 무시
  e.preventDefault()        // form submit 방지
  add()
}

function remove(tag) {
  tags.value = tags.value.filter((t) => t !== tag)
}
</script>

<style scoped>
.tag-list { margin-top: 10px; }
</style>
```

대소문자 통일은 서버(D-09)가 하므로 프론트에서 바꾸지 않습니다. 중복 판정도 대소문자를 구분한 그대로 합니다.

### 6.7 `UrlInput.vue` (새로)

구현 지침 6.5의 "한 줄에 하나 textarea"를 와이어프레임의 **입력칸 + 추가 + 목록(삭제)** 으로 바꿉니다. 부모에게는 똑같이 `string[]`을 넘깁니다.

```vue
<template>
  <div class="url-input">
    <div class="row">
      <input
        :id="inputId"
        v-model.trim="draft"
        class="box"
        inputmode="url"
        placeholder="https://"
        @keydown.enter="onEnter"
      />
      <button type="button" class="btn" @click="add">추가</button>
    </div>
    <FieldError :message="error" />
    <ul v-if="urls.length" class="url-list">
      <li v-for="url in urls" :key="url">
        <a v-if="isSafeUrl(url)" :href="url" target="_blank" rel="noopener noreferrer">{{ url }}</a>
        <span v-else>{{ url }}</span>
        <button type="button" class="btn btn-sm" @click="remove(url)">삭제</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import { RULES } from '@/constants/rules'
import { isSafeUrl } from '@/utils/url'

defineProps({ inputId: { type: String, default: 'url-input' } })
const urls = defineModel({ type: Array, default: () => [] })
const draft = ref('')
const error = ref('')

function add() {
  error.value = ''
  if (!draft.value) return
  const value = /^https?:\/\//i.test(draft.value) ? draft.value : `https://${draft.value}`

  if (!isSafeUrl(value)) return (error.value = '* http 또는 https 주소만 넣을 수 있어요.')
  if (urls.value.includes(value)) return (error.value = '* 중복된 값입니다.')
  if (RULES.URL_MAX_COUNT && urls.value.length >= RULES.URL_MAX_COUNT) {
    return (error.value = `* URL은 ${RULES.URL_MAX_COUNT}개까지 넣을 수 있어요.`)
  }
  urls.value = [...urls.value, value]
  draft.value = ''
}

function onEnter(e) {
  if (e.isComposing) return
  e.preventDefault()
  add()
}

function remove(url) {
  urls.value = urls.value.filter((u) => u !== url)
}
</script>

<style scoped>
.url-list { display: grid; gap: 8px; margin-top: 10px; }
.url-list li { display: flex; align-items: center; gap: 12px; }
.url-list a, .url-list span { font-size: var(--fs-sm); color: var(--hashtag); word-break: break-all; }
</style>
```

### 6.8 `PostForm.vue` (작성·수정 공용)

props/emit 계약은 구현 지침 6.5를 유지하되, 와이어프레임 배치에 맞게 두 가지를 바꿉니다.

- `submitLabel` 대신 `mode`(`'create' | 'edit'`)와 `headingDate`를 받습니다.
- **작성**은 아래 가운데 `글 게시하기`, **수정**은 오른쪽 위에 `수정 완료` / `취소`(`cancel` emit).

```vue
<template>
  <form class="post-form" novalidate @submit.prevent="onSubmit">
    <div class="form-head">
      <h1><span class="num">{{ headingDate }}</span> 의 기록</h1>
      <div v-if="mode === 'edit'" class="btn-group">
        <button type="submit" class="btn btn-primary" :disabled="submitting">수정 완료</button>
        <button type="button" class="btn" @click="emit('cancel')">취소</button>
      </div>
    </div>

    <div class="field">
      <!-- @metric CLS: 에러 문구를 라벨 옆에 둬서 문구가 생겨도 입력칸이 밀리지 않는다 -->
      <div class="label-row">
        <label class="label-lg" for="post-title">제목</label>
        <FieldError :message="titleError || fieldErrors.title" />
      </div>
      <input id="post-title" v-model="form.title" class="box" :maxlength="RULES.TITLE_MAX" />
    </div>

    <div class="field">
      <div class="label-row">
        <label class="label-lg" for="post-content">내용</label>
        <FieldError :message="fieldErrors.content" />
      </div>
      <textarea id="post-content" v-model="form.content" class="box" rows="6" />
    </div>

    <div class="field">
      <label class="label-lg" for="post-url">URL (하이퍼링크)</label>
      <UrlInput v-model="form.urls" input-id="post-url" />
      <FieldError :message="fieldErrors.urls" />
    </div>

    <div class="field">
      <div class="label-row">
        <label class="label-lg" for="post-hashtag">#해시태그</label>
        <span class="helper">공부 주제, 스터디 이름 등을 입력해보세요</span>
      </div>
      <HashtagInput v-model="form.hashtags" input-id="post-hashtag" />
      <FieldError :message="fieldErrors.hashtags" />
    </div>

    <div v-if="mode === 'create'" class="form-foot">
      <button type="submit" class="btn btn-primary" :disabled="submitting">글 게시하기</button>
    </div>
  </form>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import FieldError from '@/components/common/FieldError.vue'
import HashtagInput from './HashtagInput.vue'
import UrlInput from './UrlInput.vue'
import { RULES } from '@/constants/rules'

const props = defineProps({
  mode: { type: String, default: 'create' }, // 'create' | 'edit'
  headingDate: { type: String, required: true }, // formatDotDate 결과
  initial: {
    type: Object,
    default: () => ({ title: '', content: '', urls: [], hashtags: [] }),
  },
  submitting: { type: Boolean, default: false },
  fieldErrors: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['submit', 'cancel'])

const form = reactive({ title: '', content: '', urls: [], hashtags: [] })
const titleError = ref('')

watch(
  () => props.initial,
  (v) => {
    form.title = v.title
    form.content = v.content
    form.urls = [...v.urls]
    form.hashtags = [...v.hashtags]
  },
  { immediate: true },
)

function onSubmit() {
  titleError.value = ''
  if (!form.title.trim()) {
    titleError.value = '* 제목을 입력해주세요'
    return
  }
  emit('submit', {
    title: form.title.trim(),
    content: form.content,
    urls: form.urls,
    hashtags: form.hashtags,
  })
}
</script>

<style scoped>
.post-form { max-width: 580px; margin: 32px auto 0; }
.form-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
.form-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.form-head .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.form-foot { display: flex; justify-content: center; margin-top: 40px; }
</style>
```

> 내용 필수 여부는 [12장](#12-미결-사항) U-1을 보세요. 지금은 프론트에서 막지 않고 서버 `INVALID_INPUT`의 `fieldErrors.content`를 표시합니다.

### 6.9 `PostSearchBar.vue`

와이어프레임의 `검색어 입력 | 내용+제목 ▾ | 📆 날짜 선택 | 🔍`을 API 검색 파라미터(`keyword`, `hashtag`, `nickname`, `date`)에 맞춥니다. 드롭다운으로 검색어를 어느 파라미터로 보낼지 고릅니다.

| 드롭다운 | 보내는 파라미터 |
|---|---|
| 제목+내용 | `keyword` |
| 해시태그 | `hashtag` (쉼표로 여러 개 → 배열) |
| 닉네임 | `nickname` |

```vue
<template>
  <form class="search" role="search" @submit.prevent="onSubmit">
    <label class="sr-only" for="search-text">검색어</label>
    <input id="search-text" v-model="text" class="box search-text" placeholder="검색어 입력" />
    <select v-model="field" class="box" aria-label="검색 범위">
      <option value="keyword">제목+내용</option>
      <option value="hashtag">해시태그</option>
      <option value="nickname">닉네임</option>
    </select>
    <input v-model="date" class="box" type="date" aria-label="날짜 선택" />
    <button type="submit" class="btn btn-primary" aria-label="검색">🔍</button>
    <button v-if="active" type="button" class="btn" @click="emit('reset')">초기화</button>
    <button type="button" class="btn" @click="emit('mine')">내 글 보기</button>
  </form>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({ initial: { type: Object, default: () => ({}) } }) // 현재 URL 쿼리
const emit = defineEmits(['search', 'reset', 'mine'])

const field = ref('keyword')
const text = ref('')
const date = ref('')

watch(
  () => props.initial,
  (q) => {
    if (q.hashtag) {
      field.value = 'hashtag'
      text.value = [].concat(q.hashtag).join(', ')
    } else if (q.nickname) {
      field.value = 'nickname'
      text.value = q.nickname
    } else {
      field.value = 'keyword'
      text.value = q.keyword ?? ''
    }
    date.value = q.date ?? ''
  },
  { immediate: true },
)

const active = computed(() => ['keyword', 'hashtag', 'nickname', 'date'].some((k) => props.initial[k]))

function onSubmit() {
  const value = text.value.trim()
  const filters = {}
  if (value) {
    filters[field.value] =
      field.value === 'hashtag'
        ? value.split(',').map((s) => s.trim().replace(/^#+/, '')).filter(Boolean)
        : value
  }
  if (date.value) filters.date = date.value
  emit('search', filters)
}
</script>

<style scoped>
.search { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; margin-bottom: 20px; }
.search-text { flex: 1 1 180px; }
.search select, .search input[type='date'] { width: auto; }
</style>
```

### 6.10 `PostListItem.vue`

표의 한 행입니다. 구현 지침 6.6 계약에 `tag` emit 하나를 추가합니다(해시태그 칩 클릭 → 목록에서 해당 태그 검색).

```vue
<template>
  <tr class="post-row" @click="goDetail">
    <td class="c-no num">{{ post.postId }}</td>
    <td class="c-title">
      <RouterLink :to="detailRoute" @click.stop>{{ post.title }}</RouterLink>
      <ul v-if="post.hashtags.length" class="chips row-tags">
        <li v-for="tag in post.hashtags" :key="tag">
          <button type="button" class="chip chip-sm" @click.stop="emit('tag', tag)"># {{ tag }}</button>
        </li>
      </ul>
    </td>
    <td class="c-nick">{{ post.nickname }}</td>
    <td class="c-time num">{{ timeLabel }}</td>
  </tr>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatClock, formatDotDate, isTodayKst } from '@/utils/date'

const props = defineProps({ post: { type: Object, required: true } })
const emit = defineEmits(['tag'])
const router = useRouter()

const detailRoute = computed(() => ({ name: 'post-detail', params: { postId: props.post.postId } }))
// 오늘 글은 시각(14:20), 지난 글은 월.일(09.18)
const timeLabel = computed(() =>
  isTodayKst(props.post.createdAt) ? formatClock(props.post.createdAt) : formatDotDate(props.post.createdAt).slice(5),
)

function goDetail() {
  router.push(detailRoute.value)
}
</script>

<style scoped>
.post-row { cursor: pointer; }
.post-row:hover td { background: var(--row-hover); }
td { padding: 12px 14px; border-top: 1px solid var(--line-soft); font-size: 13px; color: var(--text-2); vertical-align: middle; }
.c-no { width: 64px; color: var(--btn); }
.c-title a { text-decoration: none; }
.c-title a:hover { text-decoration: underline; }
.row-tags { gap: 4px; margin-top: 5px; }
.c-nick { width: 110px; white-space: nowrap; }
.c-time { width: 84px; color: var(--label); white-space: nowrap; }
@media (max-width: 560px) { .c-no { display: none; } }
</style>
```

---

## 7. 화면별 마크업과 스타일

각 화면의 **스크립트 로직(API 호출, 에러 분기, 이동)은 구현 지침 7장**을 따릅니다. 여기서는 마크업, 문구, 스타일, 팝업 사용 위치만 정합니다. 모든 화면에 로딩 / 정상 / 빈 결과 / 에러 상태를 둡니다.

### 7.1 로그인 `LoginView.vue`

와이어프레임: 왼쪽 브랜드 영역 | 오른쪽 로그인 폼. (`App.vue`가 이 화면에서 `.page-flush`로 안쪽 여백을 없앱니다.)

```vue
<template>
  <div class="login">
    <section class="brand-area">
      <p class="brand-eyebrow">1일 1LOG</p>
      <p class="brand">iLog</p>
      <p class="brand-sub">매일 한 개의 로그를 남기는 학습 게시판</p>
    </section>

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
      <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">로그인</button>

      <p class="links">
        <RouterLink :to="{ name: 'password-find' }">비밀번호 찾기</RouterLink>
        <span aria-hidden="true">|</span>
        <RouterLink :to="{ name: 'signup' }">회원 가입</RouterLink>
      </p>
    </form>
  </div>
</template>

<style scoped>
.login { display: grid; grid-template-columns: 320px 1fr; min-height: 420px; }
.brand-area { display: flex; flex-direction: column; justify-content: center; padding: 40px 36px; border-right: 1px solid var(--line); }
.brand-eyebrow { font-size: var(--fs-xs); font-weight: 700; color: var(--accent); }
.brand { margin: 4px 0 10px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-4xl); line-height: 58px; }
.brand-sub { font-size: var(--fs-sm); color: var(--sub); }
.login-form { padding: 64px 48px 36px; }
.login-form h1 { margin-bottom: 24px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-3xl); line-height: 45px; }
.login-form .field { margin-bottom: 20px; }
.submit { margin-top: 20px; }
.links { display: flex; justify-content: center; gap: 8px; margin-top: 14px; font-size: var(--fs-xs); color: var(--muted); }
@media (max-width: 760px) {
  .login { grid-template-columns: 1fr; }
  .brand-area { padding: 32px 22px 22px; border-right: 0; border-bottom: 1px solid var(--line); }
  .login-form { padding: 24px 22px 32px; }
}
</style>
```

- 입력이 비어 있으면 API를 부르지 않고 `formError`에 `아이디를 입력해주세요!` / `비밀번호를 입력해주세요!`.
- `LOGIN_FAILED` 문구는 `errorMessages.js` 값을 그대로 씁니다.

### 7.2 회원가입 `SignupView.vue`

와이어프레임: 아이디를 `[아이디] @ [도메인 ▾] [중복 확인]`으로 나눠 받습니다. API에는 합친 `email` 하나를 보냅니다.

```vue
<template>
  <div class="auth-top"><h1>회원가입</h1></div>

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
        <button type="button" class="btn btn-sm" :disabled="!emailLocal" @click="checkEmail">중복 확인</button>
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
        <input id="signup-nickname" v-model.trim="form.nickname" class="box" />
        <button type="button" class="btn btn-sm" :disabled="!form.nickname" @click="checkNickname">중복 확인</button>
      </div>
      <p v-if="nicknameChecked" class="msg msg-ok">사용 가능한 닉네임입니다!</p>
      <FieldError :message="fieldErrors.nickname" />
    </div>

    <div class="submit-row">
      <button type="submit" class="btn btn-primary" :disabled="!canSubmit || submitting">가입하기</button>
      <FieldError :message="formError" />
    </div>
    <p class="foot">이미 계정이 있으신가요? <RouterLink :to="{ name: 'login' }">로그인</RouterLink></p>
  </form>
</template>

<script setup>
// 나머지 로직은 구현 지침 7.2. 이메일만 이렇게 합쳐서 쓴다
const emailLocal = ref('')
const emailDomain = ref('gmail.com')
const customDomain = ref('')
const email = computed(() => `${emailLocal.value}@${emailDomain.value || customDomain.value}`)
watch(email, () => { emailChecked.value = false; emailStatus.value = '' })
// 구현 지침 7.2의 form.email 자리에 email.value 를 쓴다
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
```

- 가입 성공(201) → `dialog.alert({ title: '회원가입이 완료되었습니다.' })` 후 `login`으로 `replace`.
- 제출 전 빈 칸이 있으면 `formError = '* 미입력 값이 있습니다.'`.

### 7.3 비밀번호 찾기 `PasswordFindView.vue`

와이어프레임에는 링크만 있고 화면이 없습니다. 회원가입과 같은 틀로 만듭니다.

```vue
<template>
  <div class="auth-top"><h1>비밀번호 찾기</h1></div>

  <div class="find">
    <template v-if="sentTo">
      <p class="notice">{{ sentTo }}로 임시 비밀번호를 보냈어요.</p>
      <!-- TODO D-17: 안내 문구 확정 후 교체 -->
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
      <button type="submit" class="btn btn-primary btn-block submit" :disabled="submitting">임시 비밀번호 받기</button>
      <p class="foot"><RouterLink :to="{ name: 'login' }">로그인으로 돌아가기</RouterLink></p>
    </form>
  </div>
</template>

<style scoped>
.find { width: 100%; max-width: 320px; margin: 40px auto 0; }
.submit { margin-top: 12px; }
.foot { margin-top: 14px; text-align: center; font-size: var(--fs-xs); color: var(--muted); }
</style>
```

`sentTo`에는 응답의 마스킹된 `data.email`을 넣습니다. **임시 비밀번호를 화면에 보여주지 않습니다.**

### 7.4 비밀번호 변경 `PasswordChangeView.vue`

와이어프레임 F-1(비밀번호 강제 변경)의 배치에 구현 지침의 **현재 비밀번호 칸**을 더합니다.

```vue
<template>
  <div class="auth-top" :class="{ 'is-plain': !forced }"><h1>비밀번호 변경</h1></div>

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
      <button type="submit" class="btn btn-primary" :disabled="submitting">비밀번호 변경하기</button>
      <button v-if="forced" type="button" class="btn" @click="onLogout">로그아웃</button>
      <RouterLink v-else class="btn" :to="{ name: 'mypage' }">취소</RouterLink>
    </div>
    <FieldError :message="formError" class="center" />
  </form>
</template>

<style scoped>
.auth-top.is-plain { padding-top: 24px; border-bottom: 0; }
.pw-change { width: 100%; max-width: 300px; margin: 40px auto 0; }
.actions { display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-top: 36px; }
.center { text-align: center; }
</style>
```

- `forced`는 `auth.passwordResetRequired`.
- 빈 칸이 있으면 `formError = '* 미입력 값이 있습니다.'`.
- 일반 모드는 제출 전 `dialog.confirm({ title: '비밀번호를 변경하시겠습니까?', confirmText: '변경하기' })`.
- 성공(204) → `dialog.alert({ title: '비밀번호가 성공적으로 변경되었습니다' })` → 구현 지침 7.4대로 이동.

### 7.5 게시글 목록 `PostListView.vue`

와이어프레임 메인: `{오늘 날짜} 오늘의 기록` + `+ 글쓰기` / 검색줄 / 표.

```vue
<template>
  <section class="post-list">
    <div class="list-head">
      <h1><span class="num">{{ today }}</span> 오늘의 기록</h1>
      <RouterLink class="btn btn-primary" :to="{ name: 'post-write' }">+ 글쓰기</RouterLink>
    </div>

    <PostSearchBar :initial="route.query" @search="onSearch" @reset="onReset" @mine="showMyPosts" />

    <div class="table-wrap">
      <table class="post-table">
        <!-- @metric FCP, CLS: 로딩 중에도 머리글을 먼저 그려 결과가 들어올 때 위쪽 배치가 그대로다 -->
        <!-- @metric A11Y: scope="col" 로 머리글과 칸의 관계를 스크린리더에 알린다 -->
        <thead>
          <tr>
            <th scope="col" class="c-no">인덱스</th>
            <th scope="col">제목</th>
            <th scope="col" class="c-nick">닉네임</th>
            <th scope="col" class="c-time">작성 시간</th>
          </tr>
        </thead>
        <tbody>
          <!-- @metric FALLBACK: 로딩 / 에러(다시 시도) / 빈 결과를 모두 그린다 -->
          <tr v-if="loading" class="state-row"><td colspan="4">불러오는 중이에요…</td></tr>
          <tr v-else-if="error" class="state-row">
            <td colspan="4">{{ error }} <button type="button" class="btn btn-sm" @click="reload">다시 시도</button></td>
          </tr>
          <tr v-else-if="!items.length" class="state-row"><td colspan="4">조건에 맞는 로그가 없어요</td></tr>
          <template v-else>
            <PostListItem v-for="post in items" :key="post.postId" :post="post" @tag="searchTag" />
          </template>
        </tbody>
      </table>
    </div>

    <BasePagination :page="currentPage" :total-pages="pageInfo.totalPages" @change="onPageChange" />
  </section>
</template>

<script setup>
// 구현 지침 7.5 스크립트에 아래를 더한다
import { formatDotDate } from '@/utils/date'

const today = formatDotDate(Date.now())

function onReset() {
  router.push({ query: { page: 1 } })
}
function searchTag(tag) {
  router.push({ query: { hashtag: tag, page: 1 } })
}
function reload() {
  postStore.fetchList(toApiParams(route.query))
}
</script>

<style scoped>
.list-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; flex-wrap: wrap; margin: 40px 0 32px; }
.list-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-3xl); line-height: 45px; }
.list-head .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.table-wrap { overflow-x: auto; }
.post-table {
  width: 100%; border-collapse: separate; border-spacing: 0; overflow: hidden;
  background: var(--paper); border: 1px solid var(--line-soft); border-radius: 6px;
}
.post-table th {
  padding: 12px 14px; background: var(--head);
  font-size: var(--fs-2xs); font-weight: 400; color: var(--label); text-align: left; white-space: nowrap;
}
.post-table th.c-no { width: 64px; }
.post-table th.c-nick { width: 110px; }
.post-table th.c-time { width: 84px; }
.state-row td { padding: 32px 14px; border-top: 1px solid var(--line-soft); text-align: center; font-size: 13px; color: var(--muted); }
@media (max-width: 560px) { .post-table th.c-no { display: none; } }
</style>
```

### 7.6 게시글 상세 `PostDetailView.vue`

와이어프레임: 제목 줄(내 글이면 `날짜 의 기록`, 남의 글이면 `날짜 닉네임의 기록`) 오른쪽에 작성 일자와 `수정하기`/`글 삭제`, 아래로 제목·내용·URL·해시태그 칸, 맨 아래 `이전`.

```vue
<template>
  <p v-if="loading" class="state">불러오는 중이에요…</p>
  <p v-else-if="loadError" class="state">{{ loadError }}</p>

  <article v-else-if="post" class="post-detail">
    <div class="detail-head">
      <h1>
        <span class="num">{{ formatDotDate(post.createdAt) }}</span>
        {{ post.isMine ? '의 기록' : `${post.author.nickname}의 기록` }}
      </h1>
      <div class="side">
        <p class="meta">
          작성 일자 <span class="num">{{ formatDotDate(post.createdAt) }} {{ formatClock(post.createdAt) }}</span>
          <template v-if="post.updatedAt"> (업데이트 됨)</template>
        </p>
        <div v-if="post.isMine" class="btn-group">
          <RouterLink class="btn" :to="{ name: 'post-edit', params: { postId } }">수정하기</RouterLink>
          <button type="button" class="btn" :disabled="deleting" @click="onDelete">글 삭제</button>
        </div>
      </div>
    </div>

    <div class="field">
      <p class="label-lg">제목</p>
      <p class="readonly">{{ post.title }}</p>
    </div>
    <div class="field">
      <p class="label-lg">내용</p>
      <div class="content" :class="{ 'is-empty': !post.content }">{{ post.content || '작성된 내용이 없습니다.' }}</div>
    </div>
    <div class="field">
      <p class="label-lg">URL (하이퍼링크)</p>
      <ul v-if="safeUrls.length" class="url-list">
        <li v-for="url in safeUrls" :key="url">
          <a :href="url" target="_blank" rel="noopener noreferrer">{{ url }}</a>
        </li>
      </ul>
      <p v-else class="msg msg-hint">등록된 URL이 없습니다.</p>
    </div>
    <div class="field">
      <p class="label-lg">#해시태그</p>
      <ul v-if="post.hashtags.length" class="chips">
        <li v-for="tag in post.hashtags" :key="tag">
          <button type="button" class="chip" @click="goTag(tag)"># {{ tag }}</button>
        </li>
      </ul>
      <p v-else class="msg msg-hint">등록된 해시태그가 없습니다.</p>
    </div>

    <div class="foot">
      <RouterLink class="btn" :to="{ name: 'post-list' }">이전</RouterLink>
    </div>
  </article>
</template>

<script setup>
// 구현 지침 7.6 스크립트 + 아래
import { computed } from 'vue'
import { useDialog } from '@/composables/useDialog'
import { formatClock, formatDotDate } from '@/utils/date'
import { isSafeUrl } from '@/utils/url'

const dialog = useDialog()
const safeUrls = computed(() => (post.value?.urls ?? []).filter(isSafeUrl))

function goTag(tag) {
  router.push({ name: 'post-list', query: { hashtag: tag, page: 1 } })
}

async function onDelete() {
  const ok = await dialog.confirm({ title: '게시글을 정말로 삭제하시겠습니까?', confirmText: '글 삭제' })
  if (!ok) return
  deleting.value = true
  try {
    await postApi.remove(props.postId)
    await dialog.alert({ title: '게시글이 삭제되었습니다.' })
    router.replace({ name: 'post-list' })
  } catch (e) {
    await dialog.alert({ title: e.message })
    if (e.code === 'POST_NOT_FOUND') router.replace({ name: 'post-list' })
  } finally {
    deleting.value = false
  }
}
// 구현 지침 7.6 load()의 alert(e.message) 는 await dialog.alert({ title: e.message }) 로 바꾼다
</script>

<style scoped>
.state { margin-top: 48px; text-align: center; color: var(--muted); }
.post-detail { max-width: 580px; margin: 32px auto 0; }
.detail-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; flex-wrap: wrap; margin-bottom: 24px; }
.detail-head h1 { font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.detail-head h1 .num { margin-right: 4px; font-size: 0.7em; font-weight: 500; }
.side { display: flex; flex-direction: column; align-items: flex-end; gap: 10px; }
.meta { font-size: var(--fs-xs); color: var(--muted); }
.content {
  min-height: 72px; padding: 12px 14px;
  border: 1px solid var(--line-soft); border-radius: var(--radius);
  font-size: var(--fs-md); line-height: 1.8; color: var(--text-2);
  white-space: pre-wrap; word-break: break-word;
}
.content.is-empty { font-size: var(--fs-sm); color: var(--muted); }
.url-list { display: grid; gap: 8px; }
.url-list a { font-size: var(--fs-sm); color: var(--hashtag); word-break: break-all; }
.foot { display: flex; justify-content: center; margin-top: 40px; }
.foot .btn { min-width: 110px; }
</style>
```

본문은 `{{ }}` + `white-space: pre-wrap`입니다. `v-html` 금지(구현 지침 8.4-16).

### 7.7 글쓰기 `PostWriteView.vue`

```vue
<template>
  <PostForm
    mode="create"
    :heading-date="today"
    :submitting="submitting"
    :field-errors="fieldErrors"
    @submit="onSubmit"
  />
</template>

<script setup>
import { formatDotDate } from '@/utils/date'
const today = formatDotDate(Date.now())
// 나머지는 구현 지침 7.7
</script>
```

### 7.8 글 수정 `PostEditView.vue`

```vue
<template>
  <p v-if="loading" class="state">불러오는 중이에요…</p>
  <PostForm
    v-else-if="initial"
    mode="edit"
    :heading-date="formatDotDate(createdAt)"
    :initial="initial"
    :submitting="submitting"
    :field-errors="fieldErrors"
    @submit="onSubmit"
    @cancel="router.push({ name: 'post-detail', params: { postId } })"
  />
</template>
```

- `createdAt`은 상세 조회 응답에서 꺼내 둡니다.
- `POST_NOT_OWNED` / `POST_NOT_FOUND`의 안내는 `dialog.alert` 후 구현 지침 7.8대로 이동합니다.

### 7.9 마이페이지 `MyPageView.vue`

와이어프레임의 세로 배치(인사 → ID → 이름 → 닉네임 → 비밀번호 변경 → 회원 탈퇴하기)를 따르되, 구현 지침 7.9의 단계(①재확인 → ②내 정보)를 지킵니다.

```vue
<template>
  <!-- ① 비밀번호 재확인 -->
  <form v-if="!me" class="verify" novalidate @submit.prevent="onVerify">
    <p class="verify-title">현재 비밀번호를 입력해 주세요.</p>
    <div class="row">
      <input v-model="verifyPassword" class="box" type="password" autocomplete="current-password" aria-label="현재 비밀번호" />
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
        <input id="my-nickname" v-model.trim="nicknameDraft" class="box" />
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
        <button type="submit" class="btn btn-primary" :disabled="withdrawing">탈퇴하기</button>
        <button type="button" class="btn" @click="withdrawOpen = false">취소</button>
      </div>
      <FieldError :message="withdrawError" />
    </form>
  </section>
</template>

<script setup>
// 구현 지침 7.9 로직 + 아래 팝업 흐름
// 닉네임: 와이어프레임 메모대로 "변경하기"를 누르면 자동으로 중복 확인 → 통과하면 확인 팝업
async function onChangeNickname() {
  nicknameError.value = ''
  if (!nicknameDraft.value) return (nicknameError.value = '닉네임을 입력해주세요.')
  if (nicknameDraft.value === user.value.nickname) return (nicknameError.value = '같은 닉네임으로 변경할 수 없습니다')

  savingNickname.value = true
  try {
    const { available } = await userApi.checkNickname(nicknameDraft.value)
    if (!available) return (nicknameError.value = '이미 사용 중인 닉네임입니다')

    const ok = await dialog.confirm({ title: '닉네임을 변경하시겠습니까?', description: `변경할 닉네임: ${nicknameDraft.value}`, confirmText: '변경하기' })
    if (!ok) return

    const data = await userApi.updateNickname(nicknameDraft.value)
    auth.updateNickname(data.nickname)
    editingNickname.value = false
    await dialog.alert({ title: '닉네임이 성공적으로 변경되었습니다' })
  } catch (e) {
    nicknameError.value = e.fieldErrors.nickname ?? e.message
  } finally {
    savingNickname.value = false
  }
}

// 탈퇴: 확인 팝업 → API → 보관 기한 안내 팝업 → 세션 삭제 → 로그인
async function onWithdraw() {
  const ok = await dialog.confirm({ title: '정말로 계정을 삭제하시겠습니까?', confirmText: '삭제하기' })
  if (!ok) return
  // ... POST /users/me/withdrawal (구현 지침 7.9)
  await dialog.alert({
    title: '계정이 삭제되었습니다',
    description: `${formatDotDate(data.recoverableUntil)}까지 보관 후 삭제돼요.`,
  })
  auth.clear()
  router.replace({ name: 'login' })
}
</script>

<style scoped>
.verify { width: 100%; max-width: 320px; margin: 96px auto 0; }
.verify-title { margin-bottom: 12px; font-size: var(--fs-md); }
.my { display: flex; flex-direction: column; align-items: flex-start; gap: 18px; max-width: 400px; margin: 40px auto 0; }
.my h1 { align-self: stretch; margin-bottom: 6px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.my .field { align-self: stretch; margin: 0; }
.my .readonly { width: 260px; max-width: 100%; }
.nick-row { display: flex; align-items: flex-end; gap: 12px; flex-wrap: wrap; }
.nick-row .readonly, .nick-row .box { width: 160px; }
.stack { min-width: 132px; }
.withdraw { align-self: stretch; }
</style>
```

"30일" 같은 보관 기간을 코드에 쓰지 않고 서버가 준 `recoverableUntil`을 표시합니다(구현 지침 7.9).

### 7.10 ABOUT `AboutView.vue` (새로)

와이어프레임의 ABOUT 화면입니다. 노란 포스트잇(기본) 한 장.

라우터 담당자에게 아래 한 줄 추가를 요청합니다(`router/index.js`는 공통 파일).

```js
{ path: '/about', name: 'about', component: () => import('@/views/AboutView.vue'), meta: { requiresAuth: true } },
```

```vue
<template>
  <section class="about">
    <h1>iLog 에 오신 것을 환영합니다!</h1>
    <!-- TODO: 서비스 설명 문구 팀 확정 후 교체 -->
    <p>
      iLog는 하루에 하나씩 공부한 내용을 기록하는 게시판입니다.<br />
      제목과 내용, 참고한 URL, 해시태그를 남기고<br />
      날짜와 해시태그로 지난 기록을 다시 찾아볼 수 있습니다.
    </p>
  </section>
</template>

<style scoped>
.about {
  max-width: 400px; margin: 48px auto 0; padding: 36px 28px 40px;
  background: var(--postit-yellow); color: var(--postit-text);
  box-shadow: var(--shadow-postit); transform: rotate(-0.8deg);
  text-align: center;
}
.about h1 { margin-bottom: 14px; font-family: var(--font-hand); font-weight: 700; font-size: var(--fs-2xl); line-height: 35px; }
.about p { font-size: var(--fs-md); line-height: 1.8; }
</style>
```

### 7.11 404 `NotFoundView.vue`

```vue
<template>
  <div class="not-found" role="alert">
    <p>페이지를 찾을 수 없어요</p>
    <RouterLink class="btn btn-primary" :to="{ name: 'post-list' }">목록으로</RouterLink>
  </div>
</template>

<style scoped>
.not-found {
  width: 300px; max-width: 100%; margin: 96px auto 0; padding: 32px 26px 24px;
  background: var(--modal); border: 1px solid var(--line); border-radius: 4px;
  box-shadow: var(--shadow-paper); text-align: center;
}
.not-found .btn { min-width: 110px; margin-top: 24px; }
</style>
```

---

## 8. 화면 문구 모음

| 위치 | 문구 |
|---|---|
| 로그인 빈 아이디 / 빈 비밀번호 | `아이디를 입력해주세요!` / `비밀번호를 입력해주세요!` |
| 회원가입 빈 칸 | `* 미입력 값이 있습니다.` |
| 이메일 확인 | `사용 가능한 아이디입니다!` / `이미 존재하는 계정입니다. 로그인 페이지에서 로그인해주세요.` |
| 닉네임 확인 | `사용 가능한 닉네임입니다!` / `이미 사용 중인 닉네임입니다` / `같은 닉네임으로 변경할 수 없습니다` |
| 비밀번호 확인 | `비밀번호가 일치합니다` / `* 비밀번호가 일치하지 않습니다` |
| 이름 안내 | `실명을 입력해주세요. (필수)` |
| 글 제목 | `* 제목을 입력해주세요` |
| 해시태그·URL 중복 | `* 중복된 값입니다.` |
| 해시태그 안내 | `공부 주제, 스터디 이름 등을 입력해보세요` |
| 삭제 확인 / 완료 | `게시글을 정말로 삭제하시겠습니까?` / `게시글이 삭제되었습니다.` |
| 닉네임 변경 확인 / 완료 | `닉네임을 변경하시겠습니까?` / `닉네임이 성공적으로 변경되었습니다` |
| 비밀번호 변경 확인 / 완료 | `비밀번호를 변경하시겠습니까?` / `비밀번호가 성공적으로 변경되었습니다` |
| 탈퇴 확인 / 완료 | `정말로 계정을 삭제하시겠습니까?` / `계정이 삭제되었습니다` |

서버 에러 코드에 대한 문구는 `constants/errorMessages.js`가 소유합니다(구현 지침 5.3). 위 표는 프론트가 직접 띄우는 문구만입니다.

---

## 9. 프로토타입과 달라지는 부분

프로토타입(`docs/prototype.html`)에서 **옮기지 않는** 것들입니다. API 계약·구현 지침과 맞지 않아서입니다.

| 프로토타입 | Vue 구현 | 근거 |
|---|---|---|
| hash 라우터(`#/main`), 목업 데이터, 데모 계정 포스트잇 | Vue Router + 실제 API | 구현 지침 5.7, 10장 |
| 경로 `/main`, `/write`, `/post/1` | `/posts`, `/posts/new`, `/posts/1` | 구현 지침 5.7 |
| 탈퇴 계정 로그인 → 복구 팝업 | 안내 문구만 (복구는 D-10) | 구현 지침 7.1 |
| 비밀번호 찾기 팝업(이메일만), 임시 비밀번호를 화면에 표시 | 별도 화면(이메일+이름), 비밀번호 표시 안 함 | 구현 지침 7.3 |
| 비밀번호 변경 전 현재 비밀번호 팝업 | 마이페이지 진입 시 재확인 화면 + 변경 화면에 현재 비밀번호 칸 | 구현 지침 7.4, 7.9 |
| 403 "잘못된 접근 입니다!" 화면 | `POST_NOT_OWNED` 안내 팝업 후 상세로 이동 | 구현 지침 7.8 |
| 검색 드롭다운 `제목 / 내용` 분리 | `제목+내용(keyword) / 해시태그 / 닉네임` | API 검색 파라미터 |
| 비밀번호 조건으로 가입 버튼 막기 | 조건은 표시만 | D-16 미확정 |
| 페이지 이동 없음 | `BasePagination` 추가 | 구현 지침 6.3 |
| 헤더에 닉네임 없음 | 로그아웃 왼쪽에 `닉네임님` | 구현 지침 6.1 |

---

## 10. 성능·품질 지표

프론트엔드는 백엔드 레이턴시처럼 숫자 하나로 끝나지 않아서, 아래 7개 축으로 나눠 봅니다. 이 장의 목적은 두 가지입니다.

1. 각 지표를 위해 **이 프로젝트 코드에서 무엇을 하는지** 정한다 (10.1, 10.3)
2. 생성된 코드에 **어느 지표를 위한 코드인지 주석 태그**를 남겨서, 나중에 `grep` 한 번으로 찾을 수 있게 한다 (10.2)

### 10.1 지표 한눈에 보기

| 축 | 지표 | 뜻 | 태그 |
|---|---|---|---|
| 1. 로딩 | LCP (Largest Contentful Paint) | 화면에서 가장 큰 콘텐츠가 그려진 시점. "다 떴다"고 느끼는 때 | `LCP` |
| | FCP (First Contentful Paint) | 첫 텍스트·이미지가 그려진 시점 | `FCP` |
| | TTFB (Time to First Byte) | 요청 후 응답 첫 바이트가 올 때까지. 백엔드 처리 시간 + 네트워크(DNS·TLS·리다이렉트) | `TTFB` |
| | TBT (Total Blocking Time) | 첫 콘텐츠 이후 메인 스레드가 50ms 넘게 막힌 시간의 합. 예전 TTI 자리를 대신함 | `TBT` |
| 2. 반응성 | INP (Interaction to Next Paint) | 클릭·입력 후 다음 화면이 그려지기까지. 2024년 3월 FID를 대체한 Core Web Vitals 지표 | `INP` |
| 3. 시각적 안정성 | CLS (Cumulative Layout Shift) | 로딩 중 레이아웃이 튀는 정도. 백엔드에 대응 개념이 없는 프론트 고유 지표 | `CLS` |
| 4. 리소스 효율 | 번들 크기, 코드 스플리팅 | 내려받는 JS·CSS 양, 화면별로 나눠 받는지 | `BUNDLE` |
| | 네트워크 요청 수 | 중복 API 호출이 없는지 | `REQUEST` |
| 5. 안정성 | JS 런타임 에러 | 잡히지 않은 에러를 한 곳에서 기록하는지 | `ERROR` |
| | 폴백 UI | 로딩·빈 결과·에러 상태를 화면이 그리는지 | `FALLBACK` |
| 6. 접근성 | a11y | 라벨, 키보드 조작, 포커스, 색 대비 | `A11Y` |
| 7. 유지보수성 | 코드 품질 | 재사용, 한 곳에서 관리, prop drilling 없음 | `MAINT` |

> **자주 헷갈리는 지점 — 도구마다 재는 지표가 다릅니다.**
> - **TTI는 Lighthouse 10(2023)부터 점수에서 빠졌습니다.** 실험실 측정에서는 TBT를 봅니다.
> - **Lighthouse 기본 모드(Navigation)는 INP를 재지 못합니다.** 페이지를 열기만 하고 클릭하지 않기 때문입니다. INP는 Lighthouse Timespan 모드나 DevTools Performance 패널의 실시간 지표로 잽니다 (10.5).
> - TTFB는 백엔드 레이턴시만이 아닙니다. 로컬에서 백엔드를 띄우고 재면 네트워크 구간이 거의 없어서 실제보다 훨씬 빠르게 나옵니다.
> - 7번 코드 품질은 Lighthouse가 재지 않습니다. PR 리뷰와 구현 지침 8장 코딩 규칙으로 봅니다.

Google이 "좋음"으로 보는 기준값입니다. 팀 목표치는 따로 정합니다(12장 U-12).

| 지표 | 좋음 |
|---|---|
| LCP | 2.5초 이하 |
| INP | 200ms 이하 |
| CLS | 0.1 이하 |
| FCP | 1.8초 이하 |
| TTFB | 0.8초 이하 |

### 10.2 주석 태그 규칙

지표를 위해 **일부러 한 선택**에는 바로 위에 `@metric` 주석을 답니다.

```js
// @metric LCP, FCP: 폰트 서버에 미리 연결해 첫 글자가 그려지는 시점을 앞당긴다
```

```vue
<!-- @metric CLS: 로딩 중에도 표 머리글을 먼저 그려서 결과가 들어올 때 아래 내용이 밀리지 않게 한다 -->
```

```css
/* @metric CLS: 폰트가 바뀌어도 줄 높이가 같도록 px로 고정 */
```

규칙:

1. 형식은 `@metric 태그[, 태그]: 무엇을 했는지 + 그게 지표에 어떻게 도움이 되는지` 한 줄.
2. 태그는 10.1 표의 12개만 씁니다: `LCP` `FCP` `TTFB` `TBT` `INP` `CLS` `BUNDLE` `REQUEST` `ERROR` `FALLBACK` `A11Y` `MAINT`.
3. 모든 줄에 달지 않습니다. 기본 문법, 평범한 로직에는 달지 않고 **10.3 표에 있는 위치와, 지표 때문에 다른 방법 대신 고른 코드**에만 답니다.
4. 공통 파일(`client.js`, `errorMessages.js`, `session.js`, `router/index.js`)에 태그를 달아야 하면 담당자에게 요청합니다.
5. 확인 명령:

```bash
grep -rn "@metric" src index.html           # 전체
grep -rn "@metric.*CLS" src                 # 지표별로 모아 보기
```

### 10.3 적용 위치 (필수 태그)

아래 위치에는 반드시 태그를 답니다. 앞 장의 코드에 태그가 없는 곳은 Claude Code가 생성할 때 추가합니다.

| 태그 | 파일 | 코드 | 이유 |
|---|---|---|---|
| `LCP, FCP` | `index.html` | `<link rel="preconnect">` 두 줄 | 폰트 서버 연결(DNS·TLS)을 미리 해서 폰트 도착을 앞당김 |
| `FCP` | `index.html` | 폰트 URL의 `display=swap` | 폰트가 오기 전에도 대체 글꼴로 글자를 먼저 그림 |
| `BUNDLE, LCP` | `router/index.js` (담당자) | `component: () => import(...)` | 화면별로 JS를 나눠 첫 화면에 필요한 것만 받음 |
| `BUNDLE` | `main.js` | `import.meta.env.DEV` 안의 `import('@/utils/vitals')` | 운영 빌드에서는 측정 코드가 빠짐 |
| `FCP, CLS` | `PostListView.vue` | 로딩 중에도 제목·검색줄·표 머리글을 그림 | 첫 페인트가 빨라지고, 결과가 들어올 때 위쪽 배치가 그대로 |
| `CLS` | `base.css` `.page` | `min-height` | 화면 전환·로딩 중 프레임 높이가 출렁이지 않게 |
| `CLS` | 제목 스타일 전반 | `line-height`를 px로 고정 | Gaegu가 늦게 도착해 글꼴이 바뀌어도 줄 높이가 같음 |
| `CLS` | `PostForm.vue` | 제목 에러를 라벨 옆(`.label-row`)에 표시 | 에러가 생겨도 입력칸이 아래로 밀리지 않음 |
| `CLS` | `PostDetailView.vue` | `.content`의 `min-height` | 본문이 짧거나 늦게 와도 아래 칸 위치가 크게 안 바뀜 |
| `INP, REQUEST` | 제출 버튼 전부 | `:disabled="submitting"` | 연타로 요청이 중복되지 않고, 누른 즉시 상태가 바뀌어 반응이 보임 |
| `INP` | `PostSearchBar.vue` | 입력마다가 아니라 제출 시 검색 | 타이핑 중에 요청·렌더링이 돌지 않음 |
| `TBT, INP` | `PostListView.vue` | `PAGE_SIZE = 10` + 페이지네이션 | 한 번에 그리는 행 수를 제한 |
| `REQUEST` | `PostListView.vue` | `watch(..., { immediate: true })`만 쓰고 `onMounted` 호출 없음 | 첫 진입 요청이 두 번 나가지 않음 (구현 지침 7.5) |
| `REQUEST` | `MyPageView.vue` | 닉네임 중복 확인은 `변경하기` 클릭 때 한 번 | 입력할 때마다 확인 API를 부르지 않음 |
| `ERROR` | `main.js` | `app.config.errorHandler`, `unhandledrejection` | 잡히지 않은 에러를 한 곳에서 기록 (10.4) |
| `FALLBACK` | 모든 View | 로딩 / 빈 결과 / 에러 + 다시 시도 | API가 실패해도 빈 화면이 되지 않음 |
| `FALLBACK` | `api/client.js` (담당자) | `NETWORK_ERROR` 변환, `timeout` | 서버가 죽거나 느려도 문구가 나옴 |
| `A11Y` | 모든 입력 | `<label for>` 또는 `aria-label` | 스크린리더가 칸 이름을 읽음 |
| `A11Y` | `FieldError.vue` | `role="alert"` | 에러가 생기면 스크린리더가 바로 읽음 |
| `A11Y` | `AppDialog.vue` | `role="dialog"`, `aria-modal`, 열릴 때 확인 버튼 포커스, Esc로 닫기 | 키보드만으로 팝업 조작 |
| `A11Y` | `base.css` | `:focus-visible` 외곽선 | 키보드 포커스 위치가 보임 |
| `A11Y` | `PostListView.vue` | `<th scope="col">` | 표 머리글과 칸의 관계를 읽어줌 |
| `A11Y` | `index.html` | `<html lang="ko">` | 스크린리더가 한국어로 읽음 |
| `MAINT` | `tokens.css` | 색·글꼴·크기를 변수 한 곳에서 | 디자인이 바뀌면 한 파일만 수정 |
| `MAINT` | `useDialog.js` | 팝업 상태를 컴포저블 하나로 | 화면마다 모달을 따로 만들지 않음 |
| `MAINT` | `PostForm.vue` | 작성·수정 공용 | 같은 폼을 두 번 만들지 않음 |

> **자주 헷갈리는 지점 — 입력 직후 문구가 생겨 칸이 밀리는 건 CLS에 안 잡힙니다.**
> 사용자가 입력하거나 클릭한 뒤 0.5초 안에 생기는 레이아웃 이동은 CLS 계산에서 빠집니다. 회원가입의 "사용 가능한 아이디입니다!" 같은 문구가 여기 해당합니다. CLS에서 신경 쓸 곳은 **API 응답이나 폰트처럼 사용자가 아무것도 안 했는데 바뀌는 곳**입니다.

### 10.4 에러 기록과 개발용 지표 로그

`src/main.js` (5.2를 이것으로 바꿉니다)

```js
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import '@/assets/styles/base.css'

const app = createApp(App)

// @metric ERROR: 렌더링·이벤트 핸들러에서 잡히지 않은 에러를 한 곳에서 기록한다 (에러 수집 도구 연결 자리)
app.config.errorHandler = (err, instance, info) => {
  console.error('[vue-error]', info, err)
  // TODO U-11: 에러 수집 도구(Sentry 등) 도입 시 여기서 전송
}

// @metric ERROR: await 없이 버려진 Promise 실패도 기록한다
window.addEventListener('unhandledrejection', (e) => {
  console.error('[unhandled-rejection]', e.reason)
})

app.use(createPinia())
app.use(router)
app.mount('#app')

// @metric BUNDLE: 개발 서버에서만 불러와 운영 빌드에는 측정 코드가 들어가지 않는다
if (import.meta.env.DEV) {
  import('@/utils/vitals').then(({ reportWebVitals }) => reportWebVitals())
}
```

`src/utils/vitals.js` (새로, `npm install -D web-vitals`)

```js
// @metric LCP, FCP, TTFB, INP, CLS: 개발 중 Core Web Vitals 값을 콘솔에서 바로 확인한다
export async function reportWebVitals() {
  const { onLCP, onFCP, onTTFB, onINP, onCLS } = await import('web-vitals')

  const log = ({ name, value, rating }) => {
    const shown = name === 'CLS' ? value.toFixed(3) : `${Math.round(value)}ms`
    console.info(`[web-vitals] ${name} ${shown} (${rating})`)
  }

  onLCP(log)
  onFCP(log)
  onTTFB(log)
  onINP(log) // 화면을 조작한 뒤 탭을 숨기거나 떠날 때 보고된다
  onCLS(log)
}
```

- `rating`은 `good` / `needs-improvement` / `poor`로 나옵니다.
- 개발 서버 값은 번들링·압축 전이라 운영보다 느립니다. **기록용 수치는 10.5 방식으로 잰 값만** 씁니다.
- 구현 지침 5단계 점검의 "`console.log` 0건"과 겹치지 않도록 위 두 파일은 `console.error` / `console.info`만 씁니다. 운영에서 `console.error`를 남길지는 U-11과 같이 정합니다.

### 10.5 측정 방법

1. **운영 빌드로 잽니다.** 개발 서버(`npm run dev`)는 번들링·압축을 하지 않아서 값이 의미가 없습니다.
   ```bash
   npm run build     # 출력되는 파일별 크기(gzip)를 BUNDLE 기록에 옮긴다
   npm run preview   # http://localhost:4173
   ```
2. Chrome **시크릿 창**(확장 프로그램 영향 제거) → DevTools → **Lighthouse**
   - Mode: Navigation / Device: Mobile로 한 번, Desktop으로 한 번
   - Categories: Performance, Accessibility, Best practices
3. **로그인이 필요한 화면**은 먼저 로그인한 뒤, Lighthouse 설정(톱니바퀴)에서 **Clear storage 체크를 해제**하고 잽니다. 체크돼 있으면 localStorage 토큰이 지워져 로그인 화면이 측정됩니다.
4. **INP**: DevTools **Performance 패널의 실시간 지표**를 켠 상태에서 검색, 해시태그 추가, 글 삭제 팝업을 직접 조작합니다. Lighthouse를 쓰려면 Mode를 Timespan으로 바꾸고 같은 조작을 합니다.
5. **REQUEST**: Network 탭 → Fetch/XHR 필터. 목록 첫 진입에 `/posts`가 1번, 상세 진입에 `/posts/{id}`가 1번인지 봅니다.
6. **A11Y**: Lighthouse 점수 + 마우스 없이 Tab·Enter·Esc만으로 "로그인 → 글쓰기 → 해시태그 추가 → 게시 → 삭제"가 끝까지 되는지.
7. **ERROR**: 콘솔에 `[vue-error]`, `[unhandled-rejection]`이 한 번도 안 찍히는지. 백엔드를 끈 상태에서 각 화면에 들어가 FALLBACK 문구가 나오는지.
8. PageSpeed Insights는 **공개 URL로 배포한 뒤**에만 쓸 수 있습니다. 방문자가 적으면 실사용자(필드) 데이터는 나오지 않고 실험실 데이터만 나옵니다.

### 10.6 기록 양식

측정값은 **실제로 잰 숫자만** 적습니다. 환경(기기 모드, 백엔드 위치)이 다르면 비교하지 않습니다.

| 측정일 | 환경 | 화면 | Performance | Accessibility | LCP | CLS | TBT | INP(수동) | 초기 JS gzip | 메모 |
|---|---|---|---|---|---|---|---|---|---|---|
| | preview / Mobile / 로컬 백엔드 | 로그인 | | | | | | | | |
| | | 게시글 목록 | | | | | | | | |
| | | 게시글 상세 | | | | | | | | |
| | | 글쓰기 | | | | | | | | |
| | | 마이페이지 | | | | | | | | |

---

## 11. 완료 점검 (화면)

- [ ] OS를 다크 모드로 바꿔도 흰 배경 그대로
- [ ] 제목·로고는 Gaegu, 날짜·시간·번호는 IBM Plex Mono, 나머지는 Noto Sans KR
- [ ] 컴포넌트 CSS에 색 값(`#...`)을 직접 쓴 곳 0건 (`tokens.css` 제외)
- [ ] 주요 버튼만 빨간 채움(`btn-primary`), 나머지는 테두리 버튼
- [ ] 입력칸은 밑줄형, 포커스 시 빨간 2px 밑줄
- [ ] 팝업 버튼 순서: 취소 왼쪽, 실행 오른쪽. `window.alert` / `window.confirm` 검색 0건
- [ ] 현재 탭이 분홍으로 표시됨 (상세·글쓰기·수정은 HOME)
- [ ] 해시태그 칩 클릭 → 목록에서 해당 태그 검색
- [ ] 한글 해시태그 입력 후 Enter 시 글자가 두 번 들어가지 않음
- [ ] 폭 360px에서 가로 스크롤 없음 (표는 표 영역 안에서만 스크롤)
- [ ] `docs/prototype.html`과 화면을 나란히 놓고 비교
- [ ] `grep -rn "@metric" src index.html` 결과에 10.3 표의 위치가 모두 있음
- [ ] 10.5 방법으로 Lighthouse(Mobile·Desktop)를 돌리고 10.6 표에 기록
- [ ] 백엔드를 끈 상태에서 모든 화면이 에러 문구를 보여줌 (빈 화면 없음)
- [ ] 운영 빌드(`npm run build`) 결과에 `web-vitals` 청크가 없음

---

## 12. 미결 사항

| # | 내용 | 지금 구현 | 고칠 곳 |
|---|---|---|---|
| U-1 | 게시글 **내용 필수 여부**. 와이어프레임 메모는 "내용 널 허용", 구현 지침 6.5 원본 코드는 `required` | 프론트에서 막지 않고 서버 `INVALID_INPUT` 표시 | `PostForm.vue` (D-08과 같이 확정) |
| U-2 | 제목 최소 길이. 와이어프레임 메모 "3글자 이상이어야 할 것 같음" | 빈 값만 막음 | `constants/rules.js`에 `TITLE_MIN` 추가 후 `PostForm.vue` |
| U-3 | `/about` 라우트 추가 | 7.10의 한 줄 | `router/index.js` (공통 파일 담당자) |
| U-4 | ABOUT 서비스 설명 문구 | 임시 문구 | `AboutView.vue` |
| U-5 | 로고 이미지. 와이어프레임은 이미지 자리(X 박스) | "iLog" 텍스트 | `AppHeader.vue`, `LoginView.vue` |
| U-6 | 성공 문구 색 `--ok`(#2F8A4A). 디자인 시스템에 초록 글자색이 없음 | 임시 색 | `tokens.css` |
| U-7 | 포스트잇 초록(연속 기록·칭찬) 사용처 | 미사용 | — |
| U-8 | 비밀번호 조건 문구(영문+숫자, 특수문자 가능, 8자 이상) | 와이어프레임 문구로 표시만 | `PasswordRuleList.vue` (D-16) |
| U-9 | **색 대비(A11Y)**. 흰 배경 위 작은 글자 기준 WCAG AA는 4.5:1인데, 계산상 `--muted`(#867F79)·`--label`(#8A8072)·`--accent`(#D15C53)는 약 3.9:1, 빨간 버튼 글자(`--btn-text` on `--btn`)는 약 4.4:1. Lighthouse 접근성에서 경고가 나올 수 있음 | 디자인 시스템 색 그대로 | 디자인 유지 / 토큰만 조금 어둡게 중 결정 → `tokens.css` |
| U-10 | `web-vitals` 개발 의존성 추가 | 10.4대로 추가 | `package.json` |
| U-11 | 운영 에러 수집 도구(Sentry 등)와 운영 콘솔 출력 여부 | `errorHandler`에서 콘솔 출력만 | `main.js` (배포 위치 확정 후) |
| U-12 | 팀 목표치 (예: Lighthouse Performance·Accessibility 점수, LCP) | 10.1의 Google "좋음" 기준을 참고로만 표시 | 10.6 표 위에 목표 줄 추가 |
| U-13 | **비밀번호를 두 번 입력하게 됨.** 구현 지침 7.9는 마이페이지 진입 시 재확인(`POST /users/me/password-verification`)을, 7.4는 변경 화면에 현재 비밀번호 칸을 요구한다. 마이페이지 → 비밀번호 변경 경로에서 같은 비밀번호를 연속 두 번 입력하게 된다 | 두 명세를 그대로 구현(2회 입력) | 재확인 직후 진입 시 현재 비밀번호 칸을 생략할지 팀 결정 → `MyPageView.vue`, `PasswordChangeView.vue` |
