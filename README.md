# iLog (1일 1log)

매일 공부한 내용을 기록하고 공유하는 게시판 서비스입니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | Vue 3, Pinia, Vue Router, Axios |
| Backend | Java 25, Spring Boot 4.1.1, JPA, Spring Security, JWT |
| Database | PostgreSQL 16 |
| Infra | Docker, AWS, Jenkins, NginX, Prometheus & Grafana |

## 프로젝트 구조

```
iLog/
├── backend/            # Spring Boot (Gradle)
├── frontend/           # Vue 3 (Vite)
├── docker-compose.yml  # DB + 백엔드 + 프론트 통합 실행
├── .env.example        # 환경변수 견본 (복사해서 .env로 사용)
└── README.md
```

## 처음 세팅하기

### 1. 저장소 클론

```bash
git clone https://github.com/Fangjae98/iLog.git
cd iLog
```

### 2. 환경변수 파일 만들기

`.env.example`을 복사해서 `.env`를 만들고 실제 값을 채웁니다.
(실제 비밀번호는 팀 채팅으로 공유합니다)

```bash
cp .env.example .env
```

### 3. 데이터베이스 실행

DB는 Docker 컨테이너로 띄웁니다. Docker Desktop이 실행 중이어야 합니다.

```bash
docker compose up -d postgres
docker ps
```

`ilog-postgres`가 `Up` 상태면 성공입니다.

> **5432 포트 충돌이 난다면**: 컴퓨터에 PostgreSQL이 이미 설치되어 실행 중일 수 있습니다.
> `sudo lsof -i :5432`로 확인하고, Homebrew로 설치했다면 `brew services stop postgresql@버전`으로 중지하세요.

## 개발 중 실행 방법

평소 개발할 때는 **DB만 컨테이너로 띄우고, 백엔드/프론트엔드는 각자 로컬에서 실행**합니다.

### Backend

`backend/src/main/resources/application-local.properties` 파일을 직접 만듭니다.
(이 파일은 Git에 올라가지 않습니다)

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ilogdb
spring.datasource.username=ilog
spring.datasource.password=실제_비밀번호
```

실행:

```bash
cd backend
./gradlew bootRun
```

→ http://localhost:8080

**요구사항**: Java 25 (JDK). 없다면 `brew install --cask temurin@25`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

→ http://localhost:5173

### 전체를 컨테이너로 한 번에 실행 (배포 환경 확인용)

```bash
docker compose up --build
```

→ http://localhost (NginX가 프론트 서빙 + `/api`는 백엔드로 전달)

## 브랜치 전략

```
feat/fe/login     ──PR──> fe ─────┐
feat/be/user-api  ──PR──> be ─────┼──PR──> develop ──배포 PR──> main
feat/infra/jenkins ─PR──> infra ──┘
```

| 브랜치 | 용도 |
|---|---|
| `main` | 배포용. 항상 정상 동작하는 버전만 유지 |
| `develop` | 전체 통합 브랜치 |
| `fe` / `be` / `infra` | 파트별 통합 브랜치 |
| `feat/파트/기능명` | 개인 작업 브랜치 |

### 작업 흐름

```bash
# 1. 소속 파트 브랜치를 최신 상태로 (예: 프론트엔드)
git checkout fe
git pull

# 2. 작업 브랜치 생성
git checkout -b feat/fe/login

# 3. 작업 후 커밋
git add .
git commit -m "FE: 로그인 화면 구현"
git push -u origin feat/fe/login

# 4. GitHub에서 PR 생성 (base: fe ← compare: feat/fe/login)
# 5. 머지 후 작업 브랜치는 삭제
```

파트 브랜치에 모인 작업은 주기적으로 `develop`으로 PR을 올려 통합합니다.

> `main`과 `develop`은 직접 push가 차단되어 있습니다. 반드시 PR을 통해 합쳐주세요.

### 커밋 메시지 규칙

```
FE: 로그인 화면 구현
BE: 회원가입 API 추가
Infra: 도커 관련 파일 추가
```

## 주의사항

- `.env`, `application-local.properties`는 **절대 커밋하지 마세요** (`.gitignore`에 등록되어 있습니다)
- 비밀번호, API 키 등은 코드에 직접 적지 말고 환경변수로 관리합니다
- 이 저장소는 **Public**이므로, 올리는 모든 내용이 외부에 공개됩니다