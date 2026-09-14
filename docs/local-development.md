# 로컬 개발 환경 실행 가이드

포트폴리오용 README에서 분리한 로컬 실행 절차입니다. 화면까지 보려면 이 저장소(백엔드)와
[chulgunhaza-frontend](https://github.com/chulgunhaza/chulgunhaza-frontend)를 각각 클론해서 같이 띄워야 합니다.

## 0. 필요한 것
- Java 17, Node.js 20+
- Docker (RabbitMQ/Redis 컨테이너용 — `spring-boot-docker-compose` 의존성이 있어서 직접 `docker compose up`을 안 해도, 아래 1번을 실행하면 자동으로 떠 있는 상태가 됩니다)
- 접속 가능한 MySQL 8.x (로컬에 별도로 띄우거나 이미 있는 인스턴스를 사용 — `compose.yaml`에는 포함되어 있지 않습니다)

## 1. 백엔드 (이 저장소)

`user-server`(사원/연차/게시판/대시보드, :8081), `attendance-server`(근태, :8082),
`chatting-server`(채팅, :8083) 3개 서비스가 각자 독립 실행됩니다(#99 멀티모듈 →
#100/#101로 물리 분리 완료 — 구조는 [multi-module-structure.md](multi-module-structure.md) 참고).
로컬에서 전체 기능을 확인하려면 셋 다 띄워야 합니다.

```bash
git clone https://github.com/chulgunhaza/chulgunhaza-backend.git
cd chulgunhaza-backend
cp .env.example .env   # 값 채우기(각 변수 설명은 .env.example 주석 참고)

# MySQL에 스키마부터 만들어야 합니다 (user/attendance/chatting 3개로 분리됨).
# root 계정 필요 — MYSQL_ROOT_PASSWORD는 MySQL 컨테이너를 처음 띄울 때 지정한 값.
docker exec -i chulgunhaza-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < docs/sql/create-schemas.sql

# .env는 파일로 존재하는 것만으로는 안 되고, 셸에 export까지 돼 있어야
# ${DATABASE_URL} 같은 플레이스홀더가 실제 값으로 치환됩니다.
set -a && source .env && set +a
./gradlew :user-server:bootRun --args='--server.port=8081'

# 별도 터미널 2개에서 나머지 서비스도 띄웁니다.
set -a && source .env && set +a
./gradlew :attendance-server:bootRun --args='--server.port=8082'

set -a && source .env && set +a
./gradlew :chatting-server:bootRun --args='--server.port=8083'
```
`user-server`가 뜨면 `spring-boot-docker-compose`가 redis/rabbitmq 컨테이너를 자동으로 띄우고, `DataInitializer`가 로그인 테스트 계정과 채팅 더미 데이터를 자동으로 만듭니다. 나머지 두 서비스는 이 컨테이너를 그대로 재사용하므로 `user-server`를 먼저 띄우는 걸 권장합니다.

## 2. 프론트엔드
```bash
git clone https://github.com/chulgunhaza/chulgunhaza-frontend.git
cd chulgunhaza-frontend
npm install
npm run dev   # http://localhost:3000
```
프론트는 백엔드가 `http://localhost:8081`(+ 8082/8083)에 떠 있다고 가정합니다 — 포트를 바꿨다면 `src/api/client.ts`의 각 baseURL도 맞춰야 합니다.

## 3. 로그인
서버를 처음 띄우면 `DataInitializer`가 아래 계정과 채팅 더미 데이터(동료 10명과의 1:1 방 10개, 방마다 메시지 250개)를 자동으로 만들어둡니다 — 회원가입 API가 따로 없어서, 이 계정 없이는 갓 받은 DB로 로그인할 방법이 없습니다.

| 이메일 | 비밀번호 | 비고 |
|---|---|---|
| `test@chulgunhaza.com` | `test1234!` | 기본 테스트 계정 |
| `seed-colleague-01@chulgunhaza.com` | `test1234!` | 동료 10명 중 1명(박서준) — 브라우저 2개로 각각 로그인하면 1:1 채팅을 실제로 주고받아볼 수 있습니다 |
| `manager@chulgunhaza.com` | `test1234!` | 근태 관리자(김관리, `UserRole.MANAGER`) — 관리자 페이지(`/admin/employees`, `/admin/attendance`) 접근용 |

운영 배포 시에는 `app.seed-demo-account=false`로 이 초기화 로직을 꺼둡니다.

## 접속 정보
| 서비스 | 주소 |
|---|---|
| 프론트엔드 | http://localhost:3000 |
| user-server API | http://localhost:8081 |
| attendance-server API | http://localhost:8082 |
| chatting-server API | http://localhost:8083 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html (user-server만 등록됨) |
| RabbitMQ 관리 UI | http://localhost:15672 (계정은 `.env`의 `RMQ_USER`/`RMQ_PASS`) |

## 로컬 보조 인프라 (docker-compose)
`user-server/compose.yaml`은 로컬 개발 편의를 위한 보조 인프라만 담당하고, MySQL은 포함되어 있지 않습니다(`DATABASE_URL`로 외부 MySQL을 직접 가리킴). `spring-boot-docker-compose`(devtools) 덕분에 로컬에서 앱을 띄우면 스프링이 이 compose 파일을 자동으로 `up` 시켜줍니다.

| 서비스 | 이미지 | 포트 | 역할 |
|---|---|---|---|
| rabbitmq | rabbitmq:management | 5672(AMQP), 15672(관리 UI) | 채팅/근태 메시지 큐 |
| redis | redis:latest | 6379 | refresh 토큰 저장소 + 채팅 Pub/Sub 팬아웃 |
| MySQL | (compose 밖, 외부) | 3306 | 메인 데이터베이스 |

실제 VM(kubeadm + Podman) 배포 절차는 [kubeadm-vm-deployment.md](kubeadm-vm-deployment.md)를 참고하세요 — 이 문서와는 별개로, 레지스트리 없이 컨테이너를 직접 빌드·배포하는 완전히 다른 절차입니다.
