# attendance-server 물리 분리 (#100)

[#74](https://github.com/chulgunhaza/chulgunhaza-backend/issues/74) 로드맵의 실제
물리 분리 1호. #98(JWT)/#99(멀티모듈+스키마)가 끝난 뒤, `AttendanceRecord`/
`AttendanceController`/`AttendanceService`/`AttendanceListener` 등 근태 도메인
전체를 `user-server`에서 `attendance-server` 모듈로 옮겼다.

## 최종 데이터 흐름

```
POST /v1/attendance/register (attendance-server)
  → 컨트롤러가 @AuthenticationPrincipal에서 employeeNo/employeeName을 뽑아
    클라이언트가 보낸 값을 덮어씀 (IDOR 방지, 아래 참고)
  → RabbitMQ(attendance_queue)에 발행
  → AttendanceListener가 소비 → AttendanceRecord 저장(employeeNo/employeeName
    비정규화 컬럼) + MainNotificationDto를 mainExchange/
    main_notification_queue_key로 발행
  → user-server의 MainNotificationListener가 소비 →
    AttendanceAlarmService.sendSseEvent(...)로 기존과 동일하게 SSE 푸시
```

## 왜 attendance부터인가

`AttendanceListener`가 이미 RabbitMQ 큐를 통해 근태 등록을 받고 있어서, 물리적으로
다른 프로세스가 돼도 메시징 경로는 바뀌지 않는다. WebSocket처럼 새로 설계해야
할 실시간 통신도 없다 — REST + RabbitMQ 리스너만 옮기면 됐다.

## 비정규화: employee_id JOIN → employeeNo/employeeName 컬럼

`AttendanceRecord.employee`가 실은 `Post`와 똑같이 `@ManyToOne Employee` 객체
참조였다(`docs/aggregate-boundaries.md`에 "이미 ID 참조라 문제없음"이라고 잘못
적어뒀던 부분 — 이번에 바로잡았다). 스키마가 분리되면 이 JOIN이 아예 불가능해져서,
`Long employeeNo` + `String employeeName`을 등록 시점에 그대로 저장하는 방식으로
바꿨다. 이름은 JWT 클레임에서 바로 채우므로 attendance-server는 `Employee`
리포지토리 자체가 필요 없다.

"관리자 목록 화면에 지금 이름이 아니라 근태 등록 당시 이름이 뜨는 게 맞다"는 게
이 방식의 전제 — 이력 데이터라 사원 개명 이후에도 등록 당시 이름이 남는 게
오히려 자연스럽다.

## 함께 잡은 IDOR: 클라이언트가 보낸 employeeNo를 신뢰하지 않음

예전 `AttendanceController.registerAttendance`는 요청 바디의 `employeeNo`를
그대로 믿고 그 사람 이름으로 근태를 등록했다 — 로그인한 사용자가 다른 사람의
사번을 body에 넣어 보내면 그 사람 대신 출근 등록이 되는 구조였다(프론트는
항상 로그인한 본인 사번만 보내고 있어서 실사용에선 드러나지 않았을 뿐). 지금은
컨트롤러가 `@AuthenticationPrincipal`에서 얻은 employeeNo/name으로 새 DTO를
만들어 큐에 실어 보내고, 클라이언트가 보낸 값은 버린다. curl로 다른 사번을
실어 보내도 실제 저장된 기록은 로그인한 사용자 본인 것으로 남는 것까지 확인했다.

## 교차 서비스 SSE 알림 (RabbitMQ)

`SseEmitterManager`/`AttendanceAlarmService`는 근태뿐 아니라 연차 사용 알림과도
공유하는 인프라라 user-server에 그대로 둬야 했다 — attendance-server가 별도
프로세스가 된 이상 이걸 직접 호출할 수 없다. 그래서 저장 성공 후
`MainNotificationDto`를 RabbitMQ(`mainExchange`/`main_notification_queue_key`)로
발행하고, user-server의 신규 `MainNotificationListener`가 그걸 소비해서 기존
`AttendanceAlarmService.sendSseEvent(...)`로 그대로 이어준다.

흥미로운 점: `MAIN_NOTIFICATION_QUEUE_NAME`/`mainNotificationQueue`/
`mainNotificationBinding`은 `RabbitMQConfig`에 **이미 있었지만 아무도 쓰지 않던**
배관이었다. 이번에 그 자리를 실제로 채웠다.

`MainNotificationDto`는 common 모듈로 옮겼다 — attendance-server(발행)와
user-server(소비) 양쪽 프로세스에 같은 클래스가 있어야 `Jackson2JsonMessageConverter`의
기본 FQCN 기준 타입 매핑이 동작한다.

## 서비스 간 동기 호출: 대시보드 통계

`DashboardStatsServiceImpl`(user-server)이 `AttendanceRecordRepository.
countByCheckInTimeBetween(...)`을 직접 호출하고 있었는데, 그 리포지토리가
attendance-server로 넘어가면서 컴파일이 깨졌다. attendance-server에
`GET /internal/attendance/today-count` 내부 API를 새로 만들고, user-server가
`RestTemplate`(`AttendanceStatsClient`)으로 호출하도록 바꿨다. attendance-server가
응답하지 않아도 예외를 삼키고 0으로 대체해서 대시보드 통계 API 전체가
죽지 않게 했다(실측: attendance-server를 내려도 `/v1/dashboard/stats`가
`todayAttendanceCount: 0`으로 정상 200 응답).

`/internal/**`은 SecurityConfig에서 JWT 없이 열려있다 — 아직 네트워크 경계가
없는 개발 단계라 서비스 간 인증 없이 호출한다(실제 물리 분리 시 클러스터
내부망으로만 열어두는 것으로 대체 예정).

## common 모듈에 생긴 변경 (다른 서비스도 바로 쓸 수 있게)

- `JwtAuthenticationFilter`의 "인증 없이 통과" 경로 목록이 예전엔 필터 안에
  하드코딩(로그인/재발급/swagger 4개 고정)돼 있었다 — 서비스마다 다른 예외
  경로(attendance-server의 `/internal`, `/actuator`)를 추가할 방법이 없어서,
  `JwtAuthProperties`(`jwt.auth.exempt-path-prefixes`)로 서비스별
  `application.yml`에서 주입하도록 바꿨다.
- `JwtKeyProvider`가 이제 private key가 없으면 파싱을 건너뛴다 —
  attendance-server는 토큰을 검증만 하고 발급은 안 하므로 public key만 있으면
  된다. 실측: `JWT_PRIVATE_KEY`를 비운 채로 attendance-server를 기동해도
  정상적으로 JWT 검증이 되는 것까지 확인.
- `AppCorsConfigurationSource`(도메인 의존 없음)를 user-server → common으로
  이동해서 attendance-server도 재사용.

## 각자 복제한 것 (서비스 간 wire 계약이 아니라서 공유 안 함)

- `BaseEntity`(JPA auditing 베이스): 각 서비스가 자기 스키마의 엔티티에만
  쓰는 순수 인프라라 공유 모듈에 안 넣고 복제했다.
- `PageDto`: 각 서비스가 자기 REST 응답을 스스로 직렬화하는 뷰 모델이라
  (서비스 간 역직렬화 계약이 아님) 마찬가지로 복제했다.

`MainNotificationDto`/`AppCorsConfigurationSource`(실제로 여러 서비스에 걸친
계약/설정 로직)와는 성격이 다르다는 걸 구분해서 판단했다.

## 검증

- `./gradlew clean test` — 4개 모듈 전체 컴파일 + 130개 테스트 전부 통과
  (분리 전후 테스트 개수 정확히 일치, 근태 테스트만 attendance-server로 이동)
- user-server(:8081) + attendance-server(:8082) 동시 기동
- SSE(`/v1/notifications/subscribe/main`)를 구독해둔 상태에서 attendance-server에
  근태 등록 → user-server가 실제로 SSE 알림을 전달하는 것 확인(교차 서비스
  알림 파이프라인 핵심 검증)
- 다른 사번(`99999999`)으로 등록 요청을 보내도 실제 저장/알림은 로그인한
  본인(JWT 클레임) 기준으로 되는 것 확인(IDOR 검증)
- attendance-server 없이 대시보드 통계 호출 → 근태 수 0 + 경고 로그로 우아하게
  degrade, attendance-server를 다시 켜면 정상 값 확인
- `/internal/attendance/today-count`는 쿠키 없이 200, `/v1/attendance`는 쿠키
  없이 401 확인
- `JWT_PRIVATE_KEY` 없이 attendance-server 정상 기동 + JWT 검증 정상 동작 확인

## 범위에서 제외

- Dockerfile/K8s manifest — 저장소에 아직 전혀 없어서(#99에서도 제외) 이번에도
  제외, 별도 인프라 이슈로.
