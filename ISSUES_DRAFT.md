# chulgunhaza-backend 재개 이슈 초안

코드/README/TODO 코멘트를 분석해서 뽑은 이슈 후보입니다. 각 항목을 그대로 GitHub Issue 제목+본문으로 복붙하시면 됩니다.

---

## 🗺️ 리팩토링 우선순위 로드맵

의존관계와 리스크를 기준으로 5단계로 나눴습니다. 뒤 단계로 갈수록 앞 단계가 끝나 있어야 효과가 제대로 나오는 순서예요.

### Phase 0 — 즉시 처리 (보안/버그, 반나절~1일)
리스크 대비 작업량이 작아서 먼저 치우는 게 이득인 항목들입니다.
- **#9 CORS 와일드카드 + credentials 조합 수정** — 가장 먼저. 지금 배포되면 바로 보안 이슈로 이어질 수 있는 부분.
- **#15 HTTPS 설정** — #9와 같이 묶어서 처리하면 좋음.
- **#14 Post–User 연동 주석 정리, #13 사원 프로필 이미지 업로드 완성** — 반쯤 끝난 기능을 마무리해서 기술부채를 늘리지 않기.
- **#10 AsyncConfig, #11 SseEmitter 타임아웃, #12 RabbitMQ 리스너 정리** — 코드에 남은 HACK 주석 3개, 작고 독립적이라 언제 해도 되지만 초반에 정리하면 이후 작업이 깔끔해짐.

### Phase 1 — 관측 가능성 확보 (약 1주)
Phase 2 이후부터는 기능을 건드리기 시작하는데, 그 전에 "무엇이 얼마나 걸리는지" 볼 수 있는 눈을 먼저 달아두는 게 순서상 맞습니다.
- **모니터링 (Actuator + Micrometer + Prometheus + Grafana)** — 다이어그램 5번 섹션에서 제안한 내용.
- **#6 AOP 로그 추적 + 로그 스케줄러**
- **#7 Swagger/OpenAPI 문서화** — 팀 재합류 시 온보딩/협업에도 바로 도움.

### Phase 2 — 미구현 핵심 기능 완성 (약 1~2주)
- **#3 연차 사용 동시성 제어** — 데이터 정합성 문제라 다른 기능보다 먼저. 이게 안 된 상태로 부하 테스트를 하면 동시성 버그만 계속 잡게 됨.
- **#1 근태(MAIN) SSE 알림 구현** — `SseEmitterManager` 골격이 이미 있어서 상대적으로 빠르게 끝낼 수 있음.

### Phase 3 — 배포 인프라 (약 1주)
- **#8 파일 업로드 S3 마이그레이션 — 반드시 K8s 배포보다 먼저.** 로컬 디스크 저장인 채로 K8s에 여러 Pod로 띄우면 Pod마다 파일이 따로 저장되어 조회가 깨집니다.
- **Docker & Kubernetes 배포 (Dockerfile, Deployment/Service/Ingress, ConfigMap/Secret, HPA)** — 다이어그램 6번 섹션. S3 마이그레이션 이후에 진행해야 멀티 Pod에서도 파일 업로드가 안전합니다. 이 시점부터 Phase 1의 모니터링이 실제 운영 지표를 보여주기 시작합니다.

### Phase 4 — 검증 및 스케일 판단 (1주 이상)
- **#5 부하 테스트** — 반드시 Phase 2(동시성 수정)와 Phase 3(배포 + 모니터링) 이후에. 그래야 "진짜 병목"과 "이미 알려진 버그"를 구분할 수 있습니다.
- **#4 DB 샤딩** — 부하 테스트 결과를 보고 실제로 필요한지 먼저 판단. 지금 바로 시작하기엔 가장 리스크가 큰 변경이라 데이터로 검증 후 착수 권장.
- **#2 Spring Batch 출근 정산** — 새 기능이라 이 단계 어디에 넣어도 무방하지만, 인프라가 안정된 뒤에 붙이는 걸 권장.

### Phase 5 — 마무리
- **#16 README 기술 스택 동기화** — 모든 변경이 끝난 뒤 마지막에 한 번에 정리.

---

## 🟥 미구현 기능 (README 목표 vs 실제 코드 갭)

### 1. [기능] 근태(MAIN) SSE 알림 구현
**라벨**: `enhancement`, `notification`
**설명**: `NotificationController` 에 `MAIN Controller 생성` TODO만 남아있고, 채팅(CHAT) SSE만 실제로 열려 있습니다. `SseEmitterManager` 는 이미 `EmitterType.MAIN` 을 지원하도록 구조가 잡혀 있으니, 근태(출근/연차 승인 등) 알림용 subscribe 엔드포인트와 이벤트 트리거를 추가해야 합니다.
**참고 위치**: `controller/NotificationController.java:30`

### 2. [기능] Spring Batch 기반 출근 정산 구현
**라벨**: `enhancement`, `batch`
**설명**: README에는 "정산 처리 - Spring Batch를 활용한 출근 정산" 이 주요 기능으로 명시되어 있지만, 실제로는 `build.gradle` 에 Spring Batch 의존성이 없고 관련 Job/Step 코드도 없습니다. 배치 대상(월별 근태 요약 등), 스케줄 방식(Quartz/cron), 실패 시 재처리 전략부터 설계가 필요합니다.

### 3. [기능] 연차 사용 동시성 제어
**라벨**: `enhancement`, `concurrency`
**설명**: README 목표에 "연차 사용 동시성 제어"가 있으나 `domain/annual`, 관련 서비스 어디에도 `@Lock`, `synchronized`, 분산락 관련 코드가 없습니다. 동시에 같은 연차를 여러 요청이 차감하는 race condition을 막기 위한 비관적 락(`@Lock(PESSIMISTIC_WRITE)`) 또는 Redis 분산락 적용이 필요합니다.

### 4. [기능] 데이터베이스 샤딩 검토/적용
**라벨**: `enhancement`, `database`
**설명**: README 목표에 있으나 현재 `application.yml` 은 단일 `DATABASE_URL` 만 사용합니다. 어떤 기준(회사/부서/기간)으로 샤딩할지, ShardingSphere 같은 라이브러리를 쓸지 먼저 설계 문서가 필요합니다.

### 5. [기능] 부하 테스트 수행 및 결과 문서화
**라벨**: `testing`
**설명**: 현재 테스트 코드는 `EmployeeRepositoryTest`, `ChugunhazaBackendApplicationTests` 2개뿐이라 부하 테스트 인프라가 전혀 없습니다. k6/nGrinder/JMeter 등으로 채팅 메시지 큐, 출근 등록 API에 대한 부하 테스트를 진행하고 결과를 README/문서에 남기면 좋을 것 같습니다.

### 6. [기능] AOP 기반 로그 추적 + 로그 파일 스케줄러
**라벨**: `enhancement`, `observability`
**설명**: README 주요 기능에 "AOP를 활용해 로그 추적 및 스케쥴러로 파일 관리"가 있지만, 코드에 `@Aspect`/`@Around` 등 AOP 관련 클래스가 하나도 없습니다. 컨트롤러/서비스 단 공통 로깅(요청/응답, 소요시간, 예외)을 AOP로 빼고, 로그 파일 롤링/보관 스케줄러를 추가해야 합니다.

### 7. [기능] Swagger/OpenAPI 문서화 연동
**라벨**: `documentation`
**설명**: README 기술 배지에 Swagger가 있지만 `build.gradle` 에 `springdoc-openapi` 등 관련 의존성이 없어 실제로는 API 문서가 생성되지 않는 상태입니다. `springdoc-openapi-starter-webmvc-ui` 추가 후 컨트롤러에 `@Operation`/`@Schema` 어노테이션을 붙이는 작업이 필요합니다.

### 8. [기능] 파일 업로드 S3 마이그레이션
**라벨**: `enhancement`, `infra`
**설명**: 현재 `LocalFileServiceImpl` 로 로컬 디스크(`file.upload-dir`)에만 저장하고 있습니다. `FileService` 인터페이스가 이미 분리되어 있으니, `S3FileServiceImpl` 을 추가하고 프로파일/설정으로 스위칭할 수 있게 하면 좋겠습니다.

---

## 🟧 코드에 남아있는 TODO / HACK 정리

### 9. [버그/보안] CORS 와일드카드 + credentials 조합 재검토
**라벨**: `security`, `bug`
**설명**: `SecurityConfig.corsConfigurationSource()` 에서 `setAllowedOriginPatterns(Arrays.asList("*"))` 와 `setAllowCredentials(true)` 를 함께 쓰고 있습니다. 코드 내 TODO(`"http://localhost:3000 추가"`)에도 나와있듯 프로덕션에서는 명시적 origin 화이트리스트로 좁혀야 하고, 와일드카드+credentials 조합은 브라우저에 따라 아예 차단될 수도 있습니다.
**참고 위치**: `config/SecurityConfig.java:127`

### 10. [리팩토링] AsyncConfig 임시 설정 재검토
**라벨**: `refactor`
**설명**: "우선 간단한 비동기 설정, 추후 논의 후에 다시 처리 필요" 라는 HACK 코멘트가 남아있습니다. 스레드풀 사이즈/큐 용량/거부 정책 등을 트래픽 특성에 맞게 다시 튜닝해야 합니다.
**참고 위치**: `config/AsyncConfig.java:14`

### 11. [리팩토링] SseEmitter 타임아웃 값 조정
**라벨**: `refactor`
**설명**: 현재 `10000000L`(약 2.7시간)로 하드코딩되어 있고, "추후 Emitter 시간 조정 필요" HACK이 남아있습니다. 클라이언트 재연결 전략과 함께 적절한 타임아웃/heartbeat 정책을 정해야 합니다.
**참고 위치**: `service/sse/SseEmitterManager.java:30`

### 12. [리팩토링] RabbitMQ 리스너/서비스 로직 정리
**라벨**: `refactor`
**설명**: `ChatRabbitMQMessageServiceImpl`(리스너 내부로 로직 이동 필요), `ChatMessageServiceImpl`(배치 처리 필요), `ChatController`(return 값 정리, 배치 처리 필요) 세 곳에 관련 HACK 코멘트가 남아있습니다. 채팅 저장 경로를 한 번에 정리하면 좋겠습니다.
**참고 위치**: `service/impl/ChatRabbitMQMessageServiceImpl.java:64`, `service/impl/ChatMessageServiceImpl.java:65`, `controller/ChatController.java:44,51`

### 13. [기능] 사원 프로필 이미지 업로드 서비스 완성
**라벨**: `enhancement`
**설명**: `EmployeeServiceImpl` 에 "사원 파일 이미지 저장 서비스 추가 예정" TODO가 2곳 남아있습니다. `EmployeeImage` 도메인은 이미 있으니 실제 업로드/조회 로직을 마저 연결해야 합니다.
**참고 위치**: `service/impl/EmployeeServiceImpl.java:78,122`

### 14. [리팩토링] Post - User 연동 주석 정리
**라벨**: `refactor`
**설명**: `Post.java`, `PostServiceImpl.java` 에 "User 추가 후 주석해제/추가 예정" 상태로 남은 코드가 있습니다. 게시판 작성자 연동이 실제로 필요한지 확인 후 정리해야 합니다.
**참고 위치**: `domain/board/Post.java:26`, `service/impl/PostServiceImpl.java:32`

---

## 🟨 문서/설정

### 15. [설정] HTTPS 적용
**라벨**: `infra`, `security`
**설명**: `application.yml` 에 "https 설정 추가 예정" 주석만 있고 실제 설정은 없습니다. 배포 환경(Nginx 리버스 프록시 or Spring 자체 SSL) 방식을 정하고 쿠키 `secure` 옵션도 함께 켜야 합니다.

### 16. [문서] README 기술 스택과 실제 구현 동기화
**라벨**: `documentation`
**설명**: README 기술 배지에 Nginx/Swagger가 있지만 저장소에는 관련 설정/의존성이 보이지 않습니다. 실제로 적용된 뒤 배지를 유지하거나, 계획 단계라면 "예정" 표시를 추가하는 식으로 README와 코드 상태를 맞추면 좋겠습니다.
