# 프론트엔드 작업 로그

- 작업일: 2026-09-20
- 브랜치: `feat/fe/init` (커밋·푸쉬 안 함, working tree 상태)
- 기준 문서
  - `Docs/frontend-guide.md` — **동작** (API 계약, 라우터·가드, 스토어, 에러 처리, 코딩 규칙)
  - `Docs/ilog-ui-spec.md` — **모양** (디자인 토큰, 마크업, 문구, `@metric` 규칙)
  - `Docs/prototype.html` — 모양 대조용 참고. 기능·흐름은 옮기지 않음 (ui-spec 9장)

---

## 1. 실행 방법

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

백엔드가 없어도 전 화면이 동작합니다. `frontend/.env.development` 의 `VITE_USE_MOCK=true` 가 기본값입니다.

```bash
VITE_API_BASE_URL=/api/v1
VITE_USE_MOCK=true    # false 로 바꾸면 실제 백엔드 호출
```

## 2. 데모 계정 (목업 전용)

`VITE_USE_MOCK=true` 일 때만 쓸 수 있는 가짜 계정입니다. 실제 백엔드에는 없습니다.

| 용도 | 이메일 | 비밀번호 | 닉네임 | 확인할 것 |
|---|---|---|---|---|
| 일반 | `test@gmail.com` | `test1234!` | 쇠똥구리 | 기본 흐름 전부 |
| 임시 비밀번호 | `temp@gmail.com` | `temp1234` | 뷰정복러 | 로그인 직후 **강제 비밀번호 변경** 화면으로 끌려감 |
| 탈퇴 계정 | `bye@gmail.com` | `bye12345` | 잠깐쉬는중 | `ACCOUNT_WITHDRAWN` 안내 (복구 버튼 없음, D-10) |
| 일반 | `fairy@naver.com` | `fairy1234` | 로그요정 | 남의 글 상세 (수정·삭제 버튼 안 보임) |

목업 데이터는 메모리에만 있습니다. **새로고침하면 글 작성·수정·삭제가 초기화됩니다.** (로그인 세션은 localStorage 라 유지됩니다.)

## 3. 만든 파일

```
src/assets/styles/  tokens.css, base.css                  ui-spec 4·5.1장
src/utils/          session.js, url.js, date.js, vitals.js
src/constants/      errorMessages.js, rules.js
src/api/            client.js, auth.js, user.js, post.js, mock.js
src/stores/         auth.js, post.js
src/router/         index.js  (전체 라우트 + 가드 + /about)
src/composables/    useDialog.js
src/components/common/  AppHeader, AppDialog, FieldError, BasePagination, PasswordRuleList
src/components/post/    HashtagInput, UrlInput, PostForm, PostSearchBar, PostListItem
src/views/auth/     LoginView, SignupView, PasswordFindView, PasswordChangeView
src/views/post/     PostListView, PostDetailView, PostWriteView, PostEditView
src/views/user/     MyPageView
src/views/          AboutView, NotFoundView
```

지운 것: create-vue 기본 파일 전부 (`HelloWorld.vue`, `TheWelcome.vue`, `WelcomeItem.vue`, `components/icons/`, `assets/base.css`, `assets/main.css`, `assets/logo.svg`, 기본 `HomeView`/`AboutView`, `stores/counter.js`)

추가한 의존성: `web-vitals`, `eslint`, `prettier`, `eslint-plugin-vue`, `@vue/eslint-config-prettier` (전부 devDependencies)

## 4. 작업 순서

| 단계 | 내용 |
|---|---|
| 0 | `index.html` 교체(폰트·`color-scheme`), `vite.config.js` 프록시, `.env.development`, 스캐폴드 삭제 |
| 1 | `tokens.css` / `base.css` — ui-spec 4·5.1장 그대로 |
| 2 | utils · constants · api(client + 도메인 3종 + mock) · stores |
| 3 | 라우터 전체 + 가드 + `/about` |
| 4 | 공통 컴포넌트 5종 + `useDialog` + `App.vue` |
| 5 | post 컴포넌트 5종 |
| 6 | 인증 화면 4종 |
| 7 | 게시글 화면 4종 |
| 8 | 마이페이지 · ABOUT · 404 |
| 9 | `@metric` 태그 점검, 레이아웃 폭 조정 |

## 5. 문서 충돌을 어떻게 정했나

ui-spec 9장 표에 없던 충돌만 적습니다.

| # | 지점 | 결정 |
|---|---|---|
| A-1 | 마이페이지 재확인(7.9) + 변경 화면 현재 비밀번호 칸(7.4) → **비밀번호를 두 번 입력** | 두 명세 그대로 구현. ui-spec 12장에 `U-13` 으로 올림 |
| A-2 | 검색 조건: guide 7.5는 동시 조합, ui-spec 6.9는 드롭다운 택1 | UI는 **택1**(ui-spec). `toApiParams` 는 동시 조합도 처리 가능하게 남겨둠 → D-01 확정 시 UI만 교체 |
| A-3 | 닉네임 문구가 두 벌 (`errorMessages.js` vs ui-spec 8장) | 화면 문구는 **ui-spec 8장**으로 통일. `errorMessages.js` 는 서버발 코드의 fallback |
| A-4 | ui-spec 5.3이 `hasValidToken()` 을 `computed` 안에서 호출 | **바꿨음.** `Date.now()` 는 비반응형이라 guide 5.6이 action으로 둔 이유(캐시 방지)가 무의미해짐. `showHeader` 는 `auth.accessToken` 유무로만 판정하고 만료는 라우터 가드가 담당 |
| B | 헤더 글쓰기 링크 위치, `showHeader` 조건, `PostForm` 계약, 로그인 빈 값 처리, 해시태그 `maxlength`, `/about`, 날짜 점 표기 | 전부 **ui-spec 본문대로** |

### 화면 폭 (팀 요청으로 ui-spec에서 벗어난 부분)

ui-spec 5.1 / prototype.html 의 `.page { max-width: 720px }` "종이 한 장" 프레임이 너무 좁다는 요청으로 전체 폭 레이아웃으로 바꿨습니다.

- `.page` → 화면 전체 폭, 테두리·그림자·바깥 여백 제거
- `.page-body` (새로) → `max-width: var(--content-max)` 로 안쪽 내용만 중앙 정렬
- `tokens.css` 에 `--content-max: 1280px` 추가 — **더 넓히거나 좁히려면 이 값 하나만** 바꾸면 됩니다
- 헤더는 구분선만 화면 끝까지, 내용은 본문과 같은 폭
- 화면별 안쪽 폭: 상세·작성폼 580 → 920px, 마이페이지 400 → 560px, 로그인은 화면을 꽉 채우되 폼은 400px 중앙
- 회원가입·비밀번호 찾기·변경 폼은 **일부러 좁게 유지** (입력 폼은 넓으면 오히려 쓰기 불편)

> ui-spec 1장·5.1장과 어긋나므로, 디자인 담당과 한 번 맞춰야 합니다.

## 6. 백엔드 연동할 때 할 일

현재 백엔드는 `develop` 기준 17개 엔드포인트 중 **0개**입니다. (미머지 `origin/feat/be/user-signup` 에 회원 5개)

1. `.env.development` 의 `VITE_USE_MOCK` 을 `false` 로
2. `grep -rn "MOCK" src` 로 나오는 것 전부 제거 — `src/api/mock.js` 파일 전체, `api/auth.js`·`user.js`·`post.js` 의 삼항 분기, `stores/auth.js` 의 `mockRestoreSession` 블록
3. **아래 두 가지는 붙일 때 반드시 맞춰야 합니다.** 지금은 추측으로 고치지 않고 `frontend-guide.md` 명세 그대로 두었습니다.

| 항목 | frontend-guide.md | 백엔드 실제 | 고칠 곳 |
|---|---|---|---|
| 응답 봉투 | `{ success, data, error, timestamp }` | `ErrorResponse(status, code, message, errors, timestamp)` — 평면 구조, `success`/`data` 래퍼 없음 | `api/client.js` 인터셉터 |
| 에러 코드 이름 | `POST_NOT_OWNED`, `EMAIL_DUPLICATED`, `NICKNAME_DUPLICATED`, `ACCOUNT_WITHDRAWN`, `PASSWORD_RECENTLY_USED`, `NICKNAME_SAME_AS_CURRENT`, `INTERNAL_ERROR`, `MEMBER_NOT_MATCHED` | `POST_NOT_OWNER`, `USER_DUPLICATE_EMAIL`, `USER_DUPLICATE_NICKNAME`, `USER_WITHDRAWN`, `PASSWORD_REUSED`, `NICKNAME_UNCHANGED`, `INTERNAL_SERVER_ERROR`, (없음) | `constants/errorMessages.js` |

그 외 확인 필요:
- `ErrorCode` enum 이름이 브랜치마다 다름 (`USER_*` vs `MEMBER_*`) — 백엔드 내부에서 먼저 정리 필요
- JWT 없음. 현재 `X-User-Id` 개발용 헤더가 임시 인증
- `frontend/nginx.conf` 의 `proxy_pass http://backend:8080/` — **끝 슬래시가 `/api` 접두사를 잘라먹어** 도커에서 404. 인프라 담당 확인 필요

## 7. 검증 결과

브라우저(Chrome)에서 직접 확인했습니다.

| 항목 | 결과 |
|---|---|
| 로그인 → 목록 → 상세 → 글쓰기 → 마이페이지 → ABOUT → 404 | 정상 |
| **비밀번호 틀림(401 `PASSWORD_MISMATCH`)** | 필드 문구만 뜨고 **로그아웃 안 됨** (guide 4.3 핵심 규칙) |
| 토큰 만료 후 재진입 | 세션 삭제 + `/login?redirect=/posts` |
| 강제 비밀번호 변경 | 헤더 숨김 → 다른 화면 이동 시 가드가 되돌림 → 변경 후 헤더 복귀 |
| 팝업 | 취소 왼쪽 / 실행 오른쪽, Esc로 닫힘, 확인 버튼 자동 포커스 |
| 제목 빈 값 / URL `https://` 자동 / 해시태그 `#` 제거 / 중복 감지 | 정상 |
| 검색 · 내 글 보기 | URL 쿼리 동기화, 뒤로 가기로 조건 복원 |
| 백엔드 끈 상태 | 표 머리글 유지 + 에러 문구 + 다시 시도 (빈 화면 없음) |
| 다크 모드 | `prefers-color-scheme` 규칙 0건 → 흰 배경 고정 |
| 빌드 | 화면별 청크 분리, 운영 번들에 `web-vitals` 0건 |

규칙 검사 (전부 0건):

```bash
grep -rn "v-html" src
grep -rn "window.alert\|window.confirm" src
grep -rn "from 'axios'" src --include="*.vue"
grep -rn "console.log" src
grep -rnE "#[0-9a-fA-F]{3,8}" src --include="*.vue"   # 색값 직접 사용
```

`@metric` 태그 43개, ui-spec 10.1의 12개 지표 전부 포함. `grep -rn "@metric" src index.html` 로 확인.

## 8. 아직 확인 못 한 것

| 항목 | 이유 | 누가 |
|---|---|---|
| 한글 해시태그 Enter 중복 | `e.isComposing` 가드는 넣었으나 IME 조합은 자동화로 재현 불가 | 직접 입력해서 확인 |
| 폭 360px 모바일 | Chrome 최소 창 너비 때문에 리사이즈 불가. 미디어 쿼리(560/760px)와 `.table-wrap` 가로 스크롤은 코드로 확인 | DevTools 기기 모드 |
| Lighthouse 측정 | 실측 항목 | ui-spec 10.5 방법으로 돌리고 10.6 표에 기록 |

## 9. 열려 있는 안건

- `constants/rules.js` 의 `null` 값 — D-08(제목·본문 길이, URL 개수), D-09(해시태그 개수·대소문자), D-16(비밀번호 정규식, 닉네임 길이). **추측으로 채우지 않았습니다.** 확정되면 이 파일만 고치면 됩니다
- `U-13` (새로 추가) — 마이페이지 → 비밀번호 변경 시 비밀번호 2회 입력
- D-01 검색 방식, D-06 해시태그 수정 API, D-10 계정 복구, D-17 임시 비밀번호 문구
- ui-spec `U-1`(내용 필수 여부) ~ `U-12` 는 기존 그대로
- 화면 폭 변경 (5장 마지막) — 디자인 담당 확인 필요
