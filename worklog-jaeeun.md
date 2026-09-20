# 작업 일지 — 최재은

## 2026-09-20 — B(회원) 2단계: 회원 API 5종 + Swagger + 로컬 서버 실행

작업자: 최재은 (jaemkong)
브랜치: `feat/be/user-signup` (base: `be` + `feat/be/user-revise` 2커밋)

세부 내용(API 규칙, 변경 파일, 결정 필요 사항)은 `worklog-user-signup.md`에 있다. 이 문서는 하루 흐름 요약이다.

---

### 오늘 한 일

| # | 작업 | 결과 |
|---|---|---|
| 1 | 작업 환경 정리 | 기존 폴더를 비우고 저장소를 `be` 기준으로 새로 클론, `feat/be/user-signup` 생성·푸시 |
| 2 | `feat/be/user-revise` 가져오기 | `--ff-only` 머지. `User` 엔티티·`UserRepository`·Member→User 명명 통일 포함 (kitaek 작업) |
| 3 | Swagger 설정 | springdoc-openapi 3.1.1 추가, `OpenApiConfig`, 회귀 테스트 8개 |
| 4 | 회원 API 5종 구현 | MBR-01, 02, 03, 05, 06 (명세서 v1.0 기준) |
| 5 | 실제 PostgreSQL 검증 | 임시 서버(5433)로 API·동시 가입·경계값 확인 후 삭제 |
| 6 | 작업 로그 작성 | `worklog-user-signup.md` |
| 7 | 로컬 서버 실행 | 로컬 PostgreSQL(5432)에 개발 DB를 만들고 `bootRun` 성공 |
| 8 | 커밋 이력 정리 | 이전 커밋 3개에서 Claude 공동 작업자 표기 제거 |

### 커밋

| 시각 | 커밋 | 내용 |
|---|---|---|
| 15:06 | `BE: Swagger(springdoc-openapi) 설정 추가` | Swagger UI, Bearer 스킴, `@Login` 파라미터 숨김 |
| 15:07 | `BE: 회원가입·중복 확인·비밀번호 재확인·닉네임 수정 API 구현` | 서비스·컨트롤러·DTO 10개, 테스트 48개 |
| 15:07 | `docs: B(회원) 2단계 작업 로그 추가` | `worklog-user-signup.md` |
| 이후 | `docs: 2단계 작업 로그에 실제 DB 검증 결과 반영` | 임시 PostgreSQL 검증 결과, Swagger 실제 앱 확인, 결정 사항 2건 추가 |
| 이후 | `docs: 최재은 작업 일지 추가 (2026-09-20)` | 이 문서 |

---

### 구현한 API

Base URL `/api/v1`. 로그인이 필요한 API는 개발용 헤더 `X-User-Id`로 테스트한다.

| ID | API | 로그인 |
|---|---|---|
| MBR-01 | `POST /users` 회원가입 | X |
| MBR-02 | `GET /users/email-availability` | X |
| MBR-03 | `GET /users/nickname-availability` | X |
| MBR-05 | `POST /users/me/password-verification` | O |
| MBR-06 | `PATCH /users/me` 닉네임 수정 | O |

기존 파일 수정 3건: `User`에 `name`(NOT NULL) 추가, `PASSWORD_MISMATCH` 401→400, `GlobalExceptionHandlerTest`를 `@WebMvcTest(controllers = ...)`로 제한.

### 검증

- `./gradlew test` 68개 통과 (DB가 필요한 `IlogApplicationTests`까지 포함하면 69개)
- 실제 HTTP로 5개 API의 성공·실패 케이스 확인. 이메일 소문자 저장, 비밀번호 BCrypt 60자, 탈퇴 계정 처리 확인
- 동시 가입 12건 × 10라운드: 라운드마다 201 1건 + 409 11건, 500 없음
- 경계값(닉네임·비밀번호·이메일·이름) 통과, 앱 로그 ERROR 0건

---

### 로컬 서버 실행 (오후)

백엔드를 `./gradlew bootRun`으로 띄우려는데 막힌 것이 세 가지였다.

| 막힌 것 | 처리 |
|---|---|
| `application-local.properties`(DB 접속 정보) 없음 | 새로 만듦. `.gitignore` 대상이라 커밋되지 않음 |
| Docker 꺼져 있음, 5432는 이미 로컬 PostgreSQL 17이 사용 중 | Docker 대신 **로컬 PostgreSQL 5432**를 쓰기로 결정 |
| 프로젝트는 JDK 25 필요, 설치된 JDK는 21 | Gradle toolchain이 `~/.gradle/jdks`의 JDK 25를 사용해 문제 없음 |

- 로컬 PostgreSQL에 `ilog` 계정과 `ilogdb` 데이터베이스를 새로 만들었다. 기존 DB(`skala_db` 등)는 건드리지 않았다.
- DB 비밀번호는 개발용 임시 값이며 `application-local.properties`에만 있다. 저장소가 Public이라 문서에는 적지 않는다.
- 결과: Spring Boot 4.1.1이 1.3초 만에 기동, `users` 테이블 자동 생성.
  `/swagger-ui.html` 302(→ `/swagger-ui/index.html`), `/v3/api-docs` 200 확인.

> 오전 검증은 5433 임시 서버였고, 이번 DB는 5432 로컬 PostgreSQL에 새로 만든 것이다. 서로 별개다.

### 커밋 이력 정리

이전 커밋 3개(`64acdf1`, `eab2f52`, `6a244ea`)의 메시지 끝에 `Co-Authored-By: Claude` 줄이 붙어 있어 제거했다.

- `git filter-branch`로 메시지만 고쳤고 파일 내용은 백업과 동일함을 확인했다. 커밋 해시는 바뀐다 (`b28a56e`, `4cd1a94`, `35d695f`).
- 되돌릴 수 있도록 백업 브랜치 `backup/user-signup-before-coauthor-removal`을 남겼다.
- 원격 `origin/feat/be/user-signup`에는 `git push --force-with-lease`로 반영했다. 원격이 이전 해시(`6a244ea`) 그대로일 때만 덮어쓰도록 조건을 걸었다.

---

### 남은 것

| 구분 | 내용 |
|---|---|
| 확인 | 브라우저 Swagger UI에서 "Try it out"으로 호출하는 흐름 (curl로는 검증함) |
| 확인 | `docker compose`(PostgreSQL 16 + 컨테이너 백엔드) 환경 |
| 확인 | 프론트엔드는 실행하지 않음 (`node_modules` 없음, `npm install` 필요) |
| 결정 | 명세서는 `memberId`·`MEMBER_*`, 코드는 `userId`·`USER_*` — 명세서를 고칠지 코드를 되돌릴지 |
| 결정 | `getActiveByEmail()` 시그니처 (A 로그인 담당이 기다리는 인터페이스) |
| 결정 | `user-revise`를 `be`에 먼저 머지할지 |
| 결정 | DB 컬럼명 `withdrawn_at`(엔티티) vs `deleted_at`(명세서) |
| 결정 | 응답 시간 형식(소수점 초 포함)·검증 문구 언어 고정 (global 설정) |
| 다음 | 3단계: 회원 탈퇴·복구·30일 스케줄러, 비밀번호 이력은 A 담당 |
