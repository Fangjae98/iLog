# 작업 로그 — B(회원) 2단계

## 2026-09-20 — 회원가입 · 중복 확인 · 비밀번호 재확인 · 닉네임 수정

브랜치: `feat/be/user-signup` (base: `be` + `feat/be/user-revise` 2커밋)
상태: 커밋·푸시 완료. 이 브랜치에 `user-revise`의 2커밋 위로 아래 3개가 쌓여 있다.

| 커밋 | 내용 |
|---|---|
| `BE: Swagger(springdoc-openapi) 설정 추가` | 8장 |
| `BE: 회원가입·중복 확인·비밀번호 재확인·닉네임 수정 API 구현` | 3~5장 |
| `docs: B(회원) 2단계 작업 로그 추가` | 이 문서 |

---

### 1. 환경 정리

| 작업 | 내용 |
|---|---|
| 폴더 초기화 | 기존 폴더를 비우고 `https://github.com/Fangjae98/iLog.git`을 `be` 브랜치로 새로 클론 |
| 삭제 전 확인 | 모든 커밋이 원격에 푸시되어 있었고 stash 없음. 유실된 것은 `User.java`의 미커밋 수정 하나(21줄)와 `backend/.gradle/` 캐시뿐. 옛 브랜치에는 `.gitignore`가 없어 무시 파일도 없었다. diff는 Claude 세션 scratchpad에 백업(`User.java.uncommitted.diff`) |
| 브랜치 생성 | `be`에서 `feat/be/user-signup` 생성 후 `origin`에 푸시(upstream 설정) |
| `user-revise` 가져오기 | `git merge --ff-only origin/feat/be/user-revise`. `User` 엔티티, `UserRepository`, Member→User 명명 통일이 들어 있음 |

> `feat/be/user-revise`는 작성자 이메일이 이 계정과 같아서 처음에는 본인 작업으로 판단했다.
> 하지만 `worklog.md`의 작업자는 **kitaek(global 담당)** 이다. 아직 `be`에 머지되지 않았으므로
> 이 브랜치로 PR을 올리면 그 커밋 2개가 함께 들어간다. `user-revise`를 먼저 `be`에 머지하는 편이 깔끔하다.

### 2. 명세서 확인

저장소에는 API 명세서가 없어서 Notion의 **API 기본 명세서(완성)** (v1.0, 2026-09-20)를 읽고 그대로 구현했다.
https://app.notion.com/p/3e1227bc66c580babfabf90bf7d4ae01

---

### 3. 구현한 API

Base URL `/api/v1`. 로그인이 필요한 API는 `@Login LoginUser`로 받고, 지금은 개발용 헤더 `X-User-Id`로 테스트한다.

| ID | API | 로그인 | 성공 | 주요 실패 |
|---|---|---|---|---|
| MBR-01 | `POST /users` | X | 201 `{userId}` | 400 `INVALID_INPUT` / `INVALID_PASSWORD_FORMAT`, 409 `USER_DUPLICATE_EMAIL` / `USER_DUPLICATE_NICKNAME` / `REJOIN_RESTRICTED` |
| MBR-02 | `GET /users/email-availability?email=` | X | 200 `{available, reason}` | 400 `INVALID_INPUT` |
| MBR-03 | `GET /users/nickname-availability?nickname=` | X | 200 `{available}` | 400 `INVALID_INPUT` |
| MBR-05 | `POST /users/me/password-verification` | O | 200 `{email, name, nickname, createdAt, updatedAt}` | 400 `PASSWORD_MISMATCH`, 401 |
| MBR-06 | `PATCH /users/me` | O | 200 `{userId, nickname}` | 400 `INVALID_INPUT` / `NICKNAME_UNCHANGED`, 409 `USER_DUPLICATE_NICKNAME`, 401 |

#### 동작 규칙

- **입력 정리:** email·name·nickname은 앞뒤 공백을 제거한 뒤 검증한다. 비밀번호는 정규식이 공백을 허용하지 않으므로 자르지 않는다. 이메일은 소문자로 저장한다.
- **회원가입 순서:** DTO 검증 → 비밀번호 정규식 → 이메일 중복(탈퇴 계정이면 `REJOIN_RESTRICTED`) → 닉네임 중복 → BCrypt 해시 저장.
- **동시 가입:** 사전 검사를 통과해도 유니크 제약(`uk_users_email`, `uk_users_nickname`)이 최종 방어선이다. 저장이 실패하면 원인을 다시 확인해 500 대신 409로 응답한다. 이 때문에 `signup()`에는 `@Transactional`을 걸지 않았다(PostgreSQL은 실패한 트랜잭션 안에서 추가 조회가 불가).
- **이메일 확인:** 탈퇴 후 30일 이내 계정은 `reason=WITHDRAWN`, 사용 중이면 `DUPLICATE`, 사용 가능하면 `reason=null`.
- **닉네임 확인:** 탈퇴 30일 이내 회원의 닉네임도 사용 중으로 본다(행이 남아 있으므로 자연히 그렇게 된다).
- **닉네임 수정:** 현재 닉네임과 같은 값이면 중복 검사보다 먼저 `NICKNAME_UNCHANGED`를 낸다(자기 자신과 중복으로 걸리지 않도록). 이메일·이름은 DTO에 필드가 없어서 body에 와도 무시된다.
- **탈퇴 회원:** 재확인·닉네임 수정 요청이 오면 `UNAUTHORIZED`. 명세서상 인증 필터가 막을 자리지만 필터가 붙기 전에도 같은 결과가 되도록 서비스에서도 막았다.
- **로그 보호:** `SignupRequest`, `PasswordVerificationRequest`의 `toString()`에서 비밀번호를 뺐다.

### 4. 변경 파일

새 파일 (`backend/src/main/java/com/ilog/ilog/user/`)

| 경로 | 역할 |
|---|---|
| `controller/UserController` | 5개 엔드포인트 |
| `service/UserService` | 비즈니스 로직 |
| `domain/UserPolicy` | 비밀번호 정규식, 닉네임 규칙, trim (A의 비밀번호 변경에서도 재사용) |
| `dto/` 10개 | `SignupRequest·Response`, `EmailAvailabilityRequest·Response`, `NicknameAvailabilityRequest·Response`, `PasswordVerificationRequest·Response`, `NicknameUpdateRequest·Response` |

새 테스트 (`backend/src/test/java/com/ilog/ilog/user/`): `service/UserServiceTest`, `controller/UserControllerTest`

기존 파일 수정 (3개)

| 파일 | 변경 | 이유 |
|---|---|---|
| `user/domain/User.java` | `name` 필드(NOT NULL, 50자) 추가, 빌더에 반영 | 명세서 필수값인데 엔티티에 없었음 |
| `global/error/ErrorCode.java` | `PASSWORD_MISMATCH` 401 → **400** | 명세서에서 확정. 401이면 프론트가 로그인 만료로 처리해 비밀번호만 틀려도 로그아웃됨. **global 파일이므로 확인 필요** |
| `global/error/GlobalExceptionHandlerTest.java` | `@WebMvcTest` → `@WebMvcTest(controllers = TestController.class)` | 컨트롤러를 지정하지 않아서 새 `UserController`가 딸려 들어와 `UserService` 빈이 없어 12개가 깨졌음. A·C가 컨트롤러를 추가해도 같은 문제를 막아준다 |

---

### 5. 검증

`./gradlew test` — **68개 통과, 실패 0**

| 테스트 | 개수 | 방식 |
|---|---|---|
| `UserServiceTest` | 27 | Mockito 단위 테스트, 실제 `BCryptPasswordEncoder` 사용 |
| `UserControllerTest` | 21 | `@WebMvcTest` + `@MockitoBean UserService` |
| `OpenApiDocsTest` | 8 | `@WebMvcTest` + springdoc 자동 설정. `/v3/api-docs` 내용과 Swagger UI 페이지 확인 (8장) |
| `GlobalExceptionHandlerTest` | 12 | 기존 테스트 (수정 후 통과) |

**검증하지 못한 것**

- Docker가 꺼져 있어 **실제 PostgreSQL 연동은 확인하지 못했다.** `IlogApplicationTests`(컨텍스트 로드)도 DB가 필요해 실행하지 않았다.
- `users.name`은 NOT NULL 컬럼이다. 이미 `users` 테이블에 행이 있는 로컬 DB는 `ddl-auto=update`가 실패할 수 있다(가입 API가 없었으니 행이 있을 가능성은 낮다).
- 유니크 제약 충돌 시 예외 변환은 Mockito로 흉내 낸 것이라, 실제 DB에서 `DataIntegrityViolationException`이 기대대로 나는지는 확인이 필요하다.

---

### 6. 결정이 필요한 것

| # | 내용 | 비고 |
|---|---|---|
| 1 | **명세서는 `memberId`·`MEMBER_*`, 코드는 `userId`·`USER_*`** | 코드 기준으로 구현했다. 프론트가 명세서를 보고 작업하면 어긋나므로 명세서를 고칠지, 코드를 되돌릴지 정해야 한다 |
| 2 | **`getActiveByEmail()` 시그니처** | A(로그인)가 기다리는 인터페이스. 예외/Optional, 탈퇴 회원 포함 여부를 몰라 만들지 않았다 |
| 3 | `user-revise`를 `be`에 먼저 머지할지 | 위 1번 참고 |
| 4 | DB 컬럼명 | 엔티티는 `withdrawn_at`, 명세서·테이블 명세서는 `deleted_at` |

### 7. 일부러 하지 않은 것

- **비밀번호 이력 저장:** 명세서 MBR-01은 가입 시 이력 1건 저장을 적었지만, `password_history` 테이블과 `PASSWORD_REUSED`는 비밀번호 변경 담당(A, 3단계) 범위라 제외했다.
- **`GET /users/me` (MBR-04):** 신규 추가된 🟡 항목이고 B 담당 목록에 없어 제외했다.
- **탈퇴·복구·30일 스케줄러:** 3단계.

---

### 8. Swagger (springdoc-openapi)

- **접속:** 앱 실행 후 `http://localhost:8080/swagger-ui.html` (명세 JSON은 `/v3/api-docs`)
- **의존성:** `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1`. 이 버전이 Spring Boot 4.1.0 기준으로 빌드되어 있어(POM 확인) 현재 4.1.1과 호환된다. Boot 3용 2.x 계열은 쓰지 않는다.
- **문서 내용:** 컨트롤러에 `@Tag`/`@Operation`/`@ApiResponse`(에러는 `ErrorResponse` 스키마 연결), DTO 필드에 `@Schema`(설명·예시). 검증 애노테이션(`@Size`, `@Pattern` 등)은 자동으로 스키마에 반영된다. 비밀번호 필드는 `format=password`라 UI에서 가려진다.

| 파일 | 변경 |
|---|---|
| `backend/build.gradle` | springdoc 의존성 1줄 추가 |
| `application.properties` (BE 블록) | `springdoc.*` 설정 추가. 낡은 주석 `X-Member-Id` → `X-User-Id` 정정 |
| `global/config/OpenApiConfig` (신규) | 문서 제목·설명, Bearer(JWT) 인증 스킴, `@Login LoginUser`를 문서에서 숨기는 전역 설정, `@Login` 메서드에 자물쇠 + 개발용 헤더 표시 |
| `global/auth/LoginUserArgumentResolver` | `DEV_HEADER` 상수를 `public`으로 (헤더 이름이 또 바뀌어도 문서가 어긋나지 않도록 공유) |
| `test/.../global/config/OpenApiDocsTest` (신규) | 문서 생성 회귀 테스트 8개 |

**동작 포인트**

- `@Login LoginUser`를 받는 메서드에는 자동으로 자물쇠(Bearer)와 `X-User-Id` 헤더 입력칸이 붙는다. 다른 팀원이 `@Login`을 쓰는 컨트롤러를 추가해도 별도 설정 없이 같은 처리가 된다.
- 개발용 헤더 입력칸은 `ilog.auth.dev-header-enabled=true`일 때만 보인다. JWT 적용 후 그 값을 끄면 사라지고 Authorize 버튼(Bearer)만 남는다.
- `OpenApiDocsTest`는 `@WebMvcTest(UserController.class)`로 띄운다. 다른 도메인 문서를 확인하려면 `controllers`에 추가하고 서비스를 `@MockitoBean`으로 채운다.

**A(인증)에게 전달할 것 — 3단계에서 `permitAll`을 풀 때**
`SecurityConfig`가 지금은 `anyRequest().permitAll()`이라 Swagger가 열리지만, 이걸 풀면 Swagger UI가 401로 막힌다. 아래 경로는 계속 열어두어야 한다.
`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`
운영 배포 프로필에서는 `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`로 내려 외부에 노출하지 않는다.

**확인하지 못한 것**
실제 앱을 띄운 상태(DB 연결)의 Swagger UI 화면은 눈으로 확인하지 못했다. 자동 설정을 슬라이스 테스트에 직접 붙여 `/v3/api-docs`와 `/swagger-ui/index.html` 응답까지만 검증했다. 화면에서 "Try it out"으로 실제 호출하는 것은 DB를 띄운 뒤 확인이 필요하다.

---

### 9. 알아둘 점

**IDE에서 `java.lang.Object cannot be resolved` 오류가 뜰 때**
코드 문제가 아니다(Gradle 컴파일·테스트는 통과). 프로젝트가 JDK 25 toolchain을 요구하는데 이 맥에 설치된 JDK는 21뿐이라, Gradle은 JDK 25를 `~/.gradle/jdks`에 받아 쓰지만 IDE의 Java 언어 서버는 그 JDK를 몰라서 JRE를 못 찾는 것이다.
로컬 `.vscode/settings.json`(`.gitignore` 대상)에 `java.configuration.runtimes`로 해당 JDK를 등록해 두었다. 적용하려면 VS Code에서
`Java: Clean Java Language Server Workspace` → Restart and delete 를 실행한다.
Gradle 캐시가 지워질 수 있으니 오래 쓸 거라면 `brew install --cask temurin@25`로 JDK 25를 설치하고 경로를 그쪽으로 바꾸는 것이 안정적이다.

**로컬 실행**
`.env`와 `application-local.properties`는 `.gitignore` 대상이라 클론할 때 딸려오지 않는다. `.env.example`, `application-local.properties.example`을 복사해서 채워야 DB에 붙는다.
