# iLog 게시글 프론트 작업 완료 기록

- 기여자: 박기택
- 작업일: 2026-10-12
- 기준 계획서: `FEPROCEESS.MD` (0단계, 2~10시간차) + 계획서 밖 작업 1건(웹 글꼴)
- 기준 브랜치: `fe` (`3ecf215`)
- 상태: 세 브랜치 push 완료. **PR 생성 전**

---

## 1. 브랜치와 커밋

세 브랜치가 `fe` 위에 차례로 쌓여 있습니다. PR의 base는 모두 `fe`입니다.

### PR ① `feat/fe/post-foundation`

| 커밋 | 내용 |
| --- | --- |
| `7518dc2` | FE: 게시글 라우트와 404 화면 추가 |
| `0a9e136` | FE: 게시글 API·스토어·날짜 유틸 추가 및 에러 코드를 백엔드에 맞춤 |
| `861bea7` | FE: 헤더 탭·로고 링크와 페이지네이션 컴포넌트 추가 |

### PR ② `feat/fe/post-read`

| 커밋 | 내용 |
| --- | --- |
| `358c271` | FE: 게시글 목록 화면과 로딩·에러·빈 결과 처리 구현 |
| `6166659` | FE: 게시글 검색과 내 글 보기(author=me) 구현 |
| `737b77f` | FE: 게시글 상세와 삭제 구현 |

### PR ③ `feat/fe/post-write`

| 커밋 | 내용 |
| --- | --- |
| `c6d2347` | FE: 해시태그·URL 입력 컴포넌트와 게시글 입력 규칙을 백엔드 기준으로 추가 |
| `9e92f50` | FE: 게시글 작성 화면과 공용 폼 구현 (내용 필수, 서버 칸 에러 매핑) |
| `ed6a9df` | FE: 게시글 수정 구현 (PATCH 통째 교체, POST_NOT_OWNER 처리) |
| `d7404ba` | FE: 디자인 토큰이 쓰는 웹 글꼴(Gaegu, Noto Sans KR, IBM Plex Mono) 불러오기 |

`d7404ba`는 게시글 범위 밖의 공용 파일 수정입니다. 지금은 `feat/fe/post-write` 맨 위에 있어 그대로 두면 PR ③에 포함됩니다.

---

## 2. 한 일

### 0단계: 브랜치 정리

- [x] `feat/fe/post_init` → `feat/fe/post-foundation` 이름 변경
- [x] `FEPROCEESS.MD`를 `.git/info/exclude`에 추가

### 2시간차: 라우트와 404

- [x] `router/index.js`: `/` → `/posts`, 게시글 라우트 4개(`post-list`, `post-write`, `post-detail`, `post-edit`), 404 추가. 가드는 그대로
- [x] `views/NotFoundView.vue`를 정답지에서 가져옴

### 3시간차: 데이터 계층

- [x] `api/post.js`: `list`, `get`, `create`, `update`, `remove`. 백엔드에 없는 `addHashtags`, `removeHashtag` 삭제
- [x] `stores/post.js`, `utils/url.js`: 정답지 그대로
- [x] `utils/date.js`: 맨 위 주석만 수정 (서버는 시간대 표시 없는 KST)
- [x] `constants/errorMessages.js`: `POST_NOT_OWNED` → `POST_NOT_OWNER`, `HASHTAG_NOT_FOUND` 삭제, `HASHTAG_LIMIT_EXCEEDED` 추가
- [x] `api/mock.js`: 에러 코드 2곳 정정, 작성 시 내용 필수 검사

### 4시간차: 공통 레이아웃

- [x] `components/common/BasePagination.vue`: 정답지 그대로
- [x] `components/common/AppHeader.vue`: 로고를 목록 링크로, HOME/마이페이지 탭, 현재 탭 표시. 로그아웃 로직 유지

### 5시간차: 목록

- [x] `views/post/PostListView.vue`, `components/post/PostListItem.vue`, `components/post/PostSearchBar.vue`
- [x] 백엔드가 받지 않는 `sort` 파라미터 삭제
- [x] 로딩 / 에러 + 다시 시도 / 빈 결과 상태

### 6시간차: 검색과 내 글 보기

- [x] 내 글 보기를 `nickname=내닉네임`에서 `author=me`로 변경, `useAuthStore` 의존 제거
- [x] 내 글 보기 상태에서도 초기화 버튼 표시
- [x] `api/mock.js`의 `mockPost.list`에 `author=me` 필터

### 7시간차: 상세와 삭제

- [x] `views/post/PostDetailView.vue`: 에러 상태에 "다시 시도" 버튼 추가

### 8시간차: 입력 규칙과 입력 컴포넌트

- [x] `constants/rules.js`: `TITLE_MIN` 삭제, 내용 1000자, URL 5개(각 2048자), 해시태그 10개(각 20자), `HASHTAG_PATTERN` 추가. 회원 규칙은 건드리지 않음
- [x] `components/post/HashtagInput.vue`: 서버와 같은 방식으로 다듬기(`#` 제거·소문자) → 형식 → 중복 → 개수 검사
- [x] `components/post/UrlInput.vue`: 공백·길이 검사 추가

### 9시간차: 공용 폼과 작성

- [x] `components/post/PostForm.vue`: 내용 필수, `urls[0]` 같은 서버 에러를 칸 이름으로 모으는 `serverErrors`, 내용 `maxlength`
- [x] `views/post/PostWriteView.vue`: 칸에 못 붙는 에러는 팝업

### 10시간차: 수정

- [x] `views/post/PostEditView.vue`: `POST_NOT_OWNER` → 팝업 후 상세, `POST_NOT_FOUND` → 팝업 후 목록, 그 외는 칸 에러 또는 팝업. 에러 상태에 "다시 시도" 버튼

### 계획서 밖: 웹 글꼴 불러오기

- [x] `frontend/index.html`: Google Fonts 링크 추가 — Gaegu(400·700), Noto Sans KR(400·500·700), IBM Plex Mono(400·500·600)
- 이유: `tokens.css`가 세 글꼴을 지정하지만 `fe`에는 불러오는 코드가 없어, 글꼴이 설치되지 않은 PC에서는 대체 글꼴로 보였음. 로그인·마이페이지 화면도 함께 바뀜
- 정답지 `index.html`의 `lang="ko"`, 제목 `iLog`, `color-scheme`은 가져오지 않음 (탭 제목은 여전히 "Vite App")

---

## 3. 확인한 것

### 규칙 검사 (계획서 6절)

- [x] `POST_NOT_OWNED`, `addHashtags`, `removeHashtag`: 0건
- [x] `v-html`: 0건
- [x] `window.alert`, `window.confirm`: 0건
- [x] `.vue` 파일의 `axios` 직접 import: 0건
- [x] `console.log`: 0건
- [x] `npm run build` 통과

### 목업 모드 화면 확인 (`VITE_USE_MOCK=true`)

- [x] `/` 접속 → 비로그인이면 `/login?redirect=/posts`, 로그인 후 `/posts`
- [x] 목록 5건 표시, 내 글 보기(2건), 초기화, 태그 클릭 검색(3건), 뒤로 가기
- [x] 상세: 남의 글에는 수정·삭제 버튼 없음
- [x] 작성: 제목·내용 빈 값 차단, 작성 후 상세로 이동
- [x] 해시태그: `#Spring, spring, JPA` → `spring`, `jpa` / 형식 오류 / 중복 / 10개 초과
- [x] URL: 공백 차단, `javascript:` 차단, `vuejs.org` → `https://vuejs.org`
- [x] 수정: 기존 값이 폼에 채워짐, 제목·해시태그 수정 후 상세에 반영, "(업데이트 됨)" 표시
- [x] 삭제: 확인 팝업 → 삭제 → 목록으로 이동
- [x] `/posts/abc` → 404 화면
- [x] 없는 글(`/posts/99999`) → 팝업 후 목록
- [x] 남의 글 수정 주소 직접 접근 → 팝업 후 상세
- [x] 헤더 탭: 게시글 화면은 HOME, 마이페이지는 마이페이지가 활성

### 웹 글꼴

- [x] 글꼴 주소가 정상 응답(200), 개발 서버가 링크가 들어간 `index.html`을 내보냄
- [ ] 화면에서 글꼴이 실제로 바뀐 모습은 아직 눈으로 확인하지 않음

---

## 4. 남은 일

- [ ] **1시간차**: 백엔드(`origin/be`) 실행, Swagger로 API 5개와 에러 4종 확인 (DB 계정 필요)
- [ ] **11시간차**: `VITE_USE_MOCK=false`로 통합 테스트 시나리오 A·B·C
- [ ] 360px 폭에서 목록·상세·글쓰기 확인
- [ ] 브라우저에서 글꼴 적용 확인 (게시글 화면과 로그인·마이페이지 화면)
- [ ] 예진님에게 공용 파일 수정 공유: `router/index.js`, `AppHeader.vue`, `errorMessages.js`, `rules.js`, `mock.js`, `index.html`(글꼴)
- [ ] 글꼴 커밋 `d7404ba`를 PR ③에 둘지, PR ①로 옮기거나 따로 PR로 낼지 결정
- [ ] PR ①부터 차례로 `git fetch origin` → `git rebase origin/fe` → push → PR 생성 (base를 `fe`로 변경)

---

## 5. 계획서와 다르게 한 점

| 항목 | 내용 |
| --- | --- |
| 가이드 코드 | 가이드 원문이 저장소에 없어 "가이드 코드로 교체" 부분은 정답지와 `origin/be` 코드를 대조해 직접 작성. 대상: `api/post.js`, `HashtagInput`의 `add`, `PostForm` 스크립트, `PostEditView`의 `onSubmit`, `AppHeader` |
| 해시태그 개수 초과 | 정답지는 10개까지만 잘라 넣음. 여기서는 초과하면 넣지 않고 입력값을 남겨 고칠 수 있게 함 |
| 헤더 탭 | 정답지의 ABOUT 탭은 `AboutView`가 범위 밖이라 넣지 않음 |
| 브랜치 | PR ①·②가 머지되기 전이라 ②는 ① 위에, ③은 ② 위에서 땀. 앞 PR 머지 후 `git rebase origin/fe` 필요 |

---

## 6. 참고

- Node가 `v24.11.1`로 `package.json`의 `engines`(24.12 이상)에 못 미치지만 빌드와 실행은 정상
- `frontend/.env.development.local`(`VITE_USE_MOCK=true`)은 git에 올라가지 않는 로컬 파일
- 백엔드의 서버 오류 코드 `INTERNAL_SERVER_ERROR`는 `errorMessages.js`에 매핑이 없어 서버 문구가 그대로 나옴. 공용 블록이라 고치지 않음
