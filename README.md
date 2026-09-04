## 출근하자! 
출퇴근 기록과 실시간 소통 기능을 강화해 효율적이고 원활한 업무 환경을 제공합니다. 직원들이 편리하게 소통하고 근무를 관리할 수 있는 간단한 그룹웨어 시스템입니다. 

## 실행 가이드

이 저장소(백엔드)와 [chulgunhaza-frontend](https://github.com/chulgunhaza/chulgunhaza-frontend)를 각각 클론해서 같이 띄워야 화면까지 볼 수 있습니다.

### 0. 필요한 것
- Java 17, Node.js 20+
- Docker (RabbitMQ/Redis 컨테이너용 — `spring-boot-docker-compose` 의존성이 있어서 직접 `docker compose up`을 안 해도, 아래 1번을 실행하면 자동으로 떠 있는 상태가 됩니다)
- 접속 가능한 MySQL 8.x (로컬에 별도로 띄우거나 이미 있는 인스턴스를 사용 — `compose.yaml`에는 포함되어 있지 않습니다)

### 1. 백엔드 (이 저장소)
```bash
git clone https://github.com/chulgunhaza/chulgunhaza-backend.git
cd chulgunhaza-backend
cp .env.example .env   # 값 채우기(각 변수 설명은 .env.example 주석 참고)

# .env는 파일로 존재하는 것만으로는 안 되고, 셸에 export까지 돼 있어야
# ${DATABASE_URL} 같은 플레이스홀더가 실제 값으로 치환됩니다 — 안 하면
# "Unable to determine Dialect without JDBC metadata" 에러가 납니다.
set -a && source .env && set +a
./gradlew bootRun --args='--server.port=8081'
```
서버가 뜨면 `spring-boot-docker-compose`가 redis/rabbitmq 컨테이너를 자동으로 띄우고, `DataInitializer`가 로그인 테스트 계정과 채팅 더미 데이터를 자동으로 만듭니다(아래 3번 참고).

### 2. 프론트엔드
```bash
git clone https://github.com/chulgunhaza/chulgunhaza-frontend.git
cd chulgunhaza-frontend
npm install
npm run dev   # http://localhost:3000
```
프론트는 백엔드가 `http://localhost:8081`에 떠 있다고 가정하고 요청을 보냅니다 — 포트를 바꿨다면 프론트 쪽 `src/api/client.ts`의 `baseURL`도 맞춰야 합니다.

### 3. 로그인
서버를 처음 띄우면 `DataInitializer`가 아래 계정과 채팅 더미 데이터(동료 10명과의 1:1 방 10개, 방마다 메시지 250개)를 자동으로 만들어둡니다 — 회원가입 API가 따로 없어서, 이 계정 없이는 갓 받은 DB로 로그인할 방법이 없습니다.

| 이메일 | 비밀번호 |
|---|---|
| `test@chulgunhaza.com` | `test1234!` |

운영 배포 시에는 `app.seed-demo-account=false`로 이 초기화 로직을 꺼둡니다.

### 접속 정보
| 서비스 | 주소 |
|---|---|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui/index.html |
| RabbitMQ 관리 UI | http://localhost:15672 (계정은 `.env`의 `RMQ_USER`/`RMQ_PASS`) |

## 기간
2025.01 ~ 진행 중


## 프로젝트 목표
- 직원 관리
- 실시간 소통 및 알림 시스템
- 연차 및 근무 시간 관리
- 근태 현황 시각화
- 관리자와 근태 담당자의 업무 효율화
- 동시성 제어
- 데이터베이스 샤딩
- 부하 테스트


## 주요 기능 (실제 구현 기준)
- **실시간 채팅** - WebSocket(메시지 본문 전달) + SSE(미접속자 알림) 조합, RabbitMQ로 저장 비동기 처리
  - 1:1 채팅 + 단체(그룹) 채팅, 채팅방 나가기
  - 읽음/안읽음을 **참여자별로 독립 추적**(`lastReadMessageId`) — 그룹방에서 한 명이 읽어도 다른 사람 안읽음 수는 그대로
- **근태(출근) 알림** - 출근 등록/연차 사용 처리 결과를 SSE(MAIN 채널)로 실시간 통지
- **연차 사용 동시성 제어** - 비관적 락(`@Lock(PESSIMISTIC_WRITE)`)으로 동시 요청 시 잔여 연차 초과 사용 방지, TDD/동시성 테스트로 검증
- **페이징 처리**
- **권한 관리** - Admin, 근태 담당자, 사원 분리
- **파일 업로드** - MultiPartFile 사용(게시글 첨부, 사원 프로필 이미지). S3 마이그레이션은 아직 미착수(로컬 디스크 저장)
- **트래픽 처리** - RabbitMQ를 통한 출근 등록/채팅 저장 비동기 처리, DLQ + 재시도(`@Retryable`)
- **로그인** - 세션 로그인(Redis 세션 스토어)
- **소프트 딜리트** - `delFlag` 필드를 통한 소프트 딜리트
- **CORS 화이트리스트** - 설정 기반 origin 화이트리스트(`cors.allowed-origins`)
- **API 문서화** - Swagger/OpenAPI(springdoc), 세션 쿠키 인증에 맞춰 커스텀 스킴 등록
- **CI** - GitHub Actions로 PR/push 시 테스트 자동 실행

### 아직 구현되지 않은 것 (README 목표 대비)
- Spring Batch 기반 출근 정산
- 데이터베이스 샤딩
- 부하 테스트(현재는 Hibernate Statistics 기반 쿼리 카운트 회귀 테스트로 N+1 여부만 가볍게 관리 — 아래 "테스트" 참고)
- AOP 기반 로그 추적 + 로그 파일 스케줄러
- 파일 업로드 S3 마이그레이션
- HTTPS 적용
- 레플리카(Redis/DB 다운 시 자동 복구) — 세션은 Redis에 위임돼 있어 WAS 다중화의 기반은 마련돼 있으나, 실제 장애 복구 구성은 아직 없음

## 테스트
단위 테스트 + 실제 DB가 붙는 통합 테스트를 합쳐 12개 테스트 클래스가 있습니다(`AnnualLeaveServiceImplConcurrencyTest`, `AppCorsConfigurationSourceTest`, `AttendanceAlarmServiceImplTest`, `ChatMessageListenerTest`, `ChatMessageServiceImplTest`, `ChatRoomServiceImplTest`, `ChatRoomServiceImplQueryCountTest` 등). 특히:
- **동시성**: `CountDownLatch`로 여러 스레드를 정확히 같은 순간에 출발시켜 연차 초과 사용이 절대 발생하지 않는지 검증
- **쿼리 카운트 회귀 테스트**: Hibernate Statistics(`getPrepareStatementCount`)로 채팅방 목록 조회가 N+1 없이 상수 쿼리 수로 끝나는지 실측 검증 — 별도 부하테스트 도구 없이 프로젝트 규모에 맞게 가볍게 성능을 관리
- 나머지 서비스 계층(Attendance/Post 등) TDD 테스트 확충은 [#63](https://github.com/chulgunhaza/chulgunhaza-backend/issues/63)으로 트래킹 중

## 기술
![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring 3.x](https://img.shields.io/badge/Spring-3.x.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.x-006F39)
![Data JPA](https://img.shields.io/badge/Data%20JPA-2.x-0076B3)
![Spring Security](https://img.shields.io/badge/Spring%20Security-5.x-6DB33F)
![Spring Batch](https://img.shields.io/badge/Spring%20Batch-4.x-0062A1)
![Redis](https://img.shields.io/badge/Redis-6.x-DC382D)
![Docker](https://img.shields.io/badge/Docker-20.x-blue)
![Nginx](https://img.shields.io/badge/Nginx-1.x-lightgray)
![Git](https://img.shields.io/badge/Git-2.x-F05032)
![Swagger](https://img.shields.io/badge/Swagger-API%20Docs-brightgreen)
![AWS EC2](https://img.shields.io/badge/AWS%20EC2-lightgrey)


## 아키텍처

```mermaid
graph TB
    Browser["Browser / Client"]

    subgraph App["Spring Boot App (chulgunhaza-backend)"]
        Security["Spring Security<br/>Session 인증 (BCrypt, DaoAuthenticationProvider)"]
        Controllers["Controllers<br/>Employee / Attendance / Chat / Post / Notification"]
        Services["Services / ServiceImpl"]
        Events["ApplicationEvent<br/>Employee/Attendance/Chat EventHandler"]
        WS["WebSocketMessageHandler<br/>/websocket"]
        SSE["SseEmitterManager<br/>CHAT / MAIN Emitter"]
        Repos["Spring Data JPA Repositories"]
    end

    MySQL[("MySQL<br/>(외부, DATABASE_URL)")]
    Redis[("Redis<br/>Session Store")]
    RabbitMQ{{"RabbitMQ<br/>chat / main Exchange"}}
    DLQ["attendanceDeadLetterQueue<br/>(Spring Retry 3회)"]

    Browser -->|"HTTP + JSESSIONID 쿠키"| Security
    Browser -->|"WebSocket"| WS
    Browser -->|"SSE 구독"| SSE

    Security --> Controllers --> Services
    Services --> Repos --> MySQL
    Services --> Events
    Services -->|"convertAndSend"| RabbitMQ
    RabbitMQ -->|"@RabbitListener"| Services
    Security <-->|"세션 저장/조회"| Redis
    Services -->|"emitter.send"| SSE
    Services -->|"session.sendMessage"| WS
    RabbitMQ -->|"처리 실패 시 DLX 라우팅"| DLQ
    DLQ -->|"@Retryable 재처리"| Services
```

### 계층 구조
- **Controller** → **Service(Impl)** → **Repository(JPA)** → **MySQL** 로 이어지는 전형적인 계층형 구조이며, 도메인은 `member`, `attendance`, `annual`, `leaveWork`, `board`, `chat` 로 분리되어 있습니다.
- 인증/인가는 `SecurityConfig` 에서 세션 기반(`SessionCreationPolicy.NEVER`, 세션 자체는 필요 시에만 생성)으로 구성되고, 세션 저장소는 `spring-session-data-redis` 로 Redis에 위임합니다(`RedisConfig`, `SessionConfig`). 이를 통해 WAS(애플리케이션 서버)가 여러 대여도 세션을 공유할 수 있고, README에 언급된 "레플리카" 목표(Redis/DB 다운 시 복구)의 기반이 됩니다.
- 도메인 간 부수 효과(알림 발송 등)는 RabbitMQ를 직접 호출하는 대신 **Spring `ApplicationEvent`** (`event/attendance`, `event/chat`, `event/employee`)로 한 번 더 분리되어 있어, 이벤트 발행부와 처리부가 느슨하게 결합되어 있습니다.
- `Chat` 도메인은 `Employee`를 `@ManyToOne` 객체 참조가 아니라 `Long employeeId` 값으로만 참조합니다 — 서로 다른 애그리게이트가 객체 그래프로 직접 결합돼 있으면 한쪽 데이터 정합성 문제가 다른 도메인 쿼리까지 깨뜨릴 수 있다는 걸 실제 인시던트로 겪은 뒤([#72](https://github.com/chulgunhaza/chulgunhaza-backend/pull/72)) 정리했고, 장기적으로는 Chat을 별도 서비스로 분리하는 것까지 염두에 두고 있습니다([#74](https://github.com/chulgunhaza/chulgunhaza-backend/issues/74)).

### 메시징(RabbitMQ) 토폴로지
- `chulgunhazabackend_main` Exchange(Direct) → `attendance_queue`(출근 등록), `leave_work_queue`(연차), `main_notification_queue`(근태 알림)
- `chulgunhazabackend_chat` Exchange(Direct) → `chat_queue`(채팅 저장), `chat_notification_queue`(채팅 알림)
- `attendance_queue` 는 `x-dead-letter-exchange` 로 `deadLetterExchange` 를 지정해두어, 컨슈머가 `basicNack` 하면 메시지가 `attendanceDeadLetterQueue` 로 이동하고, `AttendanceDeadLetterListener` 가 `@Retryable`(최대 3회, 1초 간격)로 재처리를 시도한 뒤 실패하면 `@Recover` 로 넘어갑니다.
- 채팅은 WebSocket으로 받은 메시지를 바로 DB에 쓰지 않고 RabbitMQ에 적재한 뒤 `ChatMessageListener` 가 비동기로 저장하도록 해서, 순간적으로 채팅이 몰려도 DB 부하를 큐가 완충합니다.

### 실시간 통신 (WebSocket + SSE 조합)
- **WebSocket**(`/websocket`, `WebSocketMessageHandler`): 채팅방 `subscribe`/`unsubscribe` 세션을 `ConcurrentHashMap<userId, ConcurrentHashMap<chatRoomId, WebSocketSession>>` 형태로 들고 있다가 새 메시지가 오면 바로 push 합니다. STOMP 없이 순수 `WebSocketHandler` + 커스텀 프로토콜로 라우팅하고, `SecurityContextInterceptor` 로 핸드셰이크 시점에 인증 정보를 함께 실어 보냅니다.
- **SSE**(`SseEmitterManager`, `EmitterType.CHAT`/`EmitterType.MAIN`): 두 채널 모두 구현돼 있습니다.
  - `/v1/notifications/subscribe/chat`: 그 채팅방에 지금 WebSocket으로 접속해 있지 않은 사용자에게만 "새 메시지 알림"을 보내는 폴백 채널(`ChatAlarmService`). WebSocket 세션이 있으면 SSE는 안 타고 WS로 바로 전달됩니다.
  - `/v1/notifications/subscribe/main`: 출근 등록/연차 사용 등 근태 관련 이벤트가 **본인에게만** 오는 개인 알림 채널(`AttendanceAlarmService`). 여러 도메인이 공유하는 채널이라 도메인 전용 필드 없이 공통 알림 형태(수신자/메시지/시각)로 통일돼 있습니다. 팀원 전체의 출퇴근 현황을 실시간으로 흘려보내는 기능은 아닙니다(의도적 — 개인정보 노출 방지).
  - 두 채널 모두 로그인한 사용자당 emitter 슬롯이 **하나**뿐이라, 같은 채널을 여러 곳에서 동시에 구독하면 나중 연결이 앞선 연결을 밀어냅니다 — 프론트에서는 채널당 구독을 한 곳으로 모아 공유하는 방식으로 대응했습니다.
- 즉 "메시지 본문 전달"은 WebSocket, "안 보고 있는 사람에게 알림만 찌르기"는 SSE로 역할이 나뉘어 있습니다.

## 인프라 / docker-compose

`compose.yaml` 은 로컬 개발 편의를 위한 보조 인프라만 담당하고, MySQL은 포함되어 있지 않습니다(환경변수 `DATABASE_URL` 로 외부 MySQL을 직접 가리킵니다). `spring-boot-docker-compose`(devtools) 의존성 덕분에 로컬에서 앱을 뜨우면 스프링이 이 compose 파일을 자동으로 `up` 시켜줍니다.

| 서비스 | 이미지 | 포트 | 역할 |
|---|---|---|---|
| rabbitmq | rabbitmq:management | 5672(AMQP), 15672(관리 UI) | 채팅/근태 메시지 큐 |
| redis | redis:latest | 6379 | 세션 스토어 |
| MySQL | (compose 밖, 외부) | 3306 | 메인 데이터베이스 |

자세한 실행 순서는 맨 위 "실행 가이드"를 참고하세요 — 시드 계정에 딸린 채팅 더미 데이터(동료 10명과의 1:1 방 10개, 방마다 메시지 250개)까지 거기서 함께 안내합니다.

## 확장 제안: 모니터링 & Docker/Kubernetes 배포

아래 두 가지는 저장소에 아직 반영되지 않은 **제안 사항**입니다.

### 모니터링 (현재 미구현)
`build.gradle` 에 actuator/micrometer/prometheus 관련 의존성이 전혀 없어서, RabbitMQ 관리 UI(`:15672`)가 사실상 유일한 모니터링 창구입니다. `spring-boot-starter-actuator` + Micrometer(Prometheus registry) + Prometheus + Grafana 를 추가하면 API 응답시간, RabbitMQ 큐 적재량, DB 커넥션풀 등을 시각화할 수 있습니다. 아래 "AOP 로그 추적" 이슈와 로그 수집(Loki/ELK)을 함께 엮으면 관측 스택이 완결됩니다.

### Docker & Kubernetes 배포 (현재 미구현)
저장소에 `Dockerfile`/K8s manifest가 전혀 없고, `compose.yaml`은 로컬 개발용 redis/rabbitmq만 담당합니다. 제안하는 흐름은 다음과 같습니다.

- 멀티스테이지 `Dockerfile`(gradle build → JRE 런타임 이미지) → Container Registry(ECR/DockerHub)
- K8s `Deployment`(App Pod) + `Service` + `Ingress`, 환경변수는 `ConfigMap`/`Secret` 으로 주입
- 트래픽 대응을 위한 `HPA`(Horizontal Pod Autoscaler)
- 운영 환경에서는 MySQL/Redis/RabbitMQ를 컨테이너 대신 RDS / ElastiCache / Amazon MQ 같은 관리형 서비스로 옮기는 것을 권장 (특히 **파일 업로드 S3 마이그레이션이 K8s 멀티 Pod 배포보다 반드시 선행**되어야 합니다 — 로컬 디스크 저장인 채로 여러 Pod를 띄우면 Pod마다 파일이 따로 저장되어 조회가 깨집니다)

## 진행 상황

### ✅ 완료
- CORS 화이트리스트 적용, 연차 사용 동시성 제어, 근태(MAIN) SSE 알림, Swagger/OpenAPI 문서화, 사원 프로필 이미지 업로드, Post–User 연동 정리 — 초기 로드맵의 Phase 0~2 항목 전부
- 그룹 채팅 + 채팅방 나가기, 채팅 읽음/안읽음 참여자별 추적, 채팅 애그리게이트 경계 정리(Employee를 id로만 참조), 채팅 실시간 전달을 DB 저장 성공 이후로 이동
- GitHub Actions CI(테스트 자동 실행), 쿼리 카운트 기반 성능 회귀 테스트

### 🚧 남은 작업 (GitHub Issues로 트래킹 중)
| 이슈 | 내용 |
|---|---|
| [#79](https://github.com/chulgunhaza/chulgunhaza-backend/issues/79) | 연차 사용 이력 조회 API + 프론트 캘린더 서버 동기화 |
| [#76](https://github.com/chulgunhaza/chulgunhaza-backend/issues/76) | AttendanceListener가 DLQ 이동 후 원본 메시지 ack/nack 안 함 |
| [#74](https://github.com/chulgunhaza/chulgunhaza-backend/issues/74), [#75](https://github.com/chulgunhaza/chulgunhaza-backend/issues/75) | Chat↔Employee MSA 전환 준비 (아키텍처 문서 포함) |
| [#73](https://github.com/chulgunhaza/chulgunhaza-backend/issues/73), [#65](https://github.com/chulgunhaza/chulgunhaza-backend/issues/65) | 로그인 직후 Redis 세션 레이스 컨디션, 채팅 큐 데드레터 부재 |
| [#63](https://github.com/chulgunhaza/chulgunhaza-backend/issues/63) | 나머지 서비스 계층(Attendance/Post 등) TDD 테스트 확충 |
| [#60](https://github.com/chulgunhaza/chulgunhaza-backend/issues/60) | HTTPS 적용 |
| [#53](https://github.com/chulgunhaza/chulgunhaza-backend/issues/53) | 파일 업로드 S3 마이그레이션 |
| [#51](https://github.com/chulgunhaza/chulgunhaza-backend/issues/51) | AOP 기반 로그 추적 + 로그 파일 스케줄러 |
| [#50](https://github.com/chulgunhaza/chulgunhaza-backend/issues/50) | 부하 테스트 수행 및 결과 문서화 |
| [#49](https://github.com/chulgunhaza/chulgunhaza-backend/issues/49) | 데이터베이스 샤딩 검토/적용 |
| [#47](https://github.com/chulgunhaza/chulgunhaza-backend/issues/47) | Spring Batch 기반 출근 정산 |
| [#56](https://github.com/chulgunhaza/chulgunhaza-backend/issues/56), [#57](https://github.com/chulgunhaza/chulgunhaza-backend/issues/57) | SseEmitter 타임아웃 조정, RabbitMQ 리스너 배치 처리 설계(읽음 처리) |

우선순위 판단 기준은 대체로 "데이터 정합성/보안 > 관측 가능성 > 배포 인프라 > 스케일 검증" 순 — 예를 들어 부하 테스트(#50)나 DB 샤딩(#49)은 동시성 제어가 끝난 뒤, 모니터링이 갖춰진 뒤에 하는 게 의미가 있어서 뒤로 미뤄뒀습니다.

## 팀원 
|임솔|김태동|
|-------------------|---------------------------|
| <img src="https://github.com/user-attachments/assets/cb9eb08e-0cff-4c0c-9637-836e0d2fcac2" width="200" height="200">| <img src="https://github.com/user-attachments/assets/7b47759d-0325-46f2-bb7f-188f222ac894" width="200" height="200">|
| 백엔드 개발 | 백엔드 개발 |
| [깃허브 링크](https://github.com/saulsol)    | [깃허브 링크](https://github.com/rlaxoehd4234) |
