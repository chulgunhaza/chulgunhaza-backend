# 채팅 기능 확장 작업 정리 (2026-08-25)

오늘 백엔드(`chulgunhaza-backend`)와 프론트엔드(`chulgunhaza-frontend`) 양쪽에서 진행한 채팅 관련 기능 확장, 디자인 개선, CI/CD 구축 내용을 한 번에 정리한 문서입니다. 각 절에 해당 PR 링크를 달아뒀습니다.

## 목차

1. [단체 채팅 + 채팅방 나가기](#1-단체-채팅--채팅방-나가기)
2. [채팅 읽음/안읽음 사람별 추적 재설계](#2-채팅-읽음안읽음-사람별-추적-재설계)
3. [실시간 읽음 알림 (WebSocket push)](#3-실시간-읽음-알림-websocket-push)
4. [발견/수정한 버그 2건](#4-발견수정한-버그-2건)
5. [프론트엔드 비주얼 폴리시](#5-프론트엔드-비주얼-폴리시)
6. [CI/CD](#6-cicd)
7. [관련 PR 목록](#7-관련-pr-목록)

---

## 1. 단체 채팅 + 채팅방 나가기

`chat_room` ↔ `employee_chatroom`(N:M) 데이터 모델 자체는 원래 그룹 채팅을 지원할 수 있는 구조였지만, 서비스/DTO 레이어가 1:1 전제로 짜여 있어 실제로는 2명짜리 방만 만들 수 있었습니다. 방 생성부터 나가기까지 전 구간을 그룹 대응으로 정리했습니다.

- **방 생성**: `ChatRoomCreateRequestDto.receiverId`(단일) → `memberIds`(`List<Long>`)로 변경. 1명이면 1:1(기존 방 재사용), 2명 이상이면 항상 새 그룹방 생성.
- **방 목록 조회**: 그룹방에서 참여자 수만큼 같은 방이 중복 노출되던 버그를 "내 참여 기록" 기준 조회로 수정. `ChatRoomListResponseDto`에 `group`(boolean) / `roomName`("A, B 외 N명") / `members` 구조 추가.
- **메시지 전송**: `receiverId` 제거, `roomId`만으로 서버가 방 참여자 전원을 조회해 WS(접속 중) 또는 SSE(미접속)로 순회 발송.
- **나가기**: `DELETE /v1/chat/{roomId}/leave` — 내 `employee_chatroom` 참여 기록만 삭제. 방/메시지는 남아서 나머지 참여자에게는 계속 보임.
- **프론트**: 체크박스 다중 선택 멤버 피커, 그룹 뱃지, 나가기 버튼 + 커스텀 확인 모달(아래 5번 참고 — `window.confirm()`이 실제 사용 중 안 뜨는 문제가 있어서 자체 모달로 교체).

## 2. 채팅 읽음/안읽음 사람별 추적 재설계

기존 `ChatMessage.isRead`는 메시지당 boolean 하나였는데, 이건 1:1 채팅 전제였습니다. 그룹방에서 참여자 중 한 명만 방을 열어봐도 그 방의 메시지가 **전원한테** "읽음" 처리되던 버그가 있었습니다.

메시지 쪽이 아니라 참여자 쪽에서 추적하도록 옮겼습니다:

- `ChatMessage.isRead` 컬럼 제거.
- `EmployeeChatRoom`(참여자-채팅방 연결 테이블)에 `lastReadMessageId` 추가 — "이 사람이 이 방에서 마지막으로 읽은 메시지 id"(null = 아직 안 읽음). 방을 열어볼 때 그 시점 최신 메시지 id까지로 **본인 것만** 갱신(`markReadUpTo`).
- 메시지별 "안읽은 인원 수"(카카오톡 스타일)를 `unReadCount`로 계산 — 방 참여자 중 발신자 본인을 제외하고, `lastReadMessageId`가 그 메시지 id보다 작거나 null인 사람 수를 셈.

## 3. 실시간 읽음 알림 (WebSocket push)

메시지별 안읽은 인원 수를 붙인 뒤에도 REST로 방을 다시 불러오기 전까진 화면이 갱신되지 않는 문제가 있어, 읽음 처리 시점에 실시간으로 push하도록 확장했습니다.

- `getChatMessagesByRoomId` 호출 시 읽음 처리 **전** `lastReadMessageId`를 기억해두고, 처리 후 새로 "읽힌 구간"(`previousLastReadMessageId`, `maxMessageId`]에 속한 메시지들의 갱신된 `unReadCount`를 계산.
- 방에 실시간으로 접속 중인(WebSocket 세션이 열려 있는) 다른 참여자들에게 `ChatReadEventDto`(`type: "read"`, `roomId`, `readerId`, `updates: [{messageId, unReadCount}]`)를 push.
- 프론트는 `useChatSocket`으로 이 이벤트를 받아 해당 메시지들의 `unReadCount`를 즉시 갱신 — 페이지 새로고침 없이 안읽은 인원 수가 실시간으로 줄어듦.

curl + 브라우저 2계정으로 실제 재현 확인: 메시지 하나의 안읽은 인원 수가 2 → 1 → 0으로 새로고침 없이 순차 감소.

## 4. 발견/수정한 버그 2건

실시간 읽음 알림을 실제로 붙여보는 과정에서 발견한, 기존에 잠재해 있던 버그 2건입니다.

### 4-1. WebSocket 세션 맵 키 불일치

`WebSocketMessageHandler`의 세션 맵(`sessions`)은 `Employee.id`(PK) 기준으로 저장/조회하는데, `afterConnectionClosed`(연결 종료 시 정리 로직)만 `Employee.employeeNo`(PK + 10000000, 다른 값)로 지우고 있었습니다. 그 결과 연결이 끊겨도 세션이 맵에서 **절대 지워지지 않았고**, 나중에 그 죽은 세션 참조로 메시지를 보내려 하면 `IllegalStateException`(세션이 이미 닫힘)으로 요청 전체가 500 에러가 났습니다. 실측(실제 연결 끊김 재현)으로 처음 발견했습니다.

- 키를 `getId()`로 통일.
- 방어적으로 `removeSession(userId, roomId)` 메서드를 추가해, 메시지 전송 실패 시(`IOException`/`IllegalStateException`) 그 자리에서 바로 stale 세션을 정리하도록 함(정상 종료 핸드셰이크 없이 끊기는 경우 대비).

### 4-2. Hibernate 영속성 컨텍스트 stale 캐시

버그 4-1을 고치고 나니 실시간 알림 자체는 도착했지만, `unReadCount` 값이 한 박자씩 밀려서 왔습니다. 원인은 `markReadUpTo`(벌크 `UPDATE` 쿼리)가 영속성 컨텍스트(1차 캐시)를 갱신하지 않는 것이었습니다 — 같은 트랜잭션 안에서 그 UPDATE 이전에 이미 로딩해둔 `EmployeeChatRoom` 엔티티가 캐시에 갱신 전 값으로 남아있다가, 이후 조회에서 DB 대신 그 캐시된 엔티티를 그대로 돌려줬습니다.

- `@Modifying(clearAutomatically = true)` 추가로 벌크 UPDATE 직후 영속성 컨텍스트를 비워, 이후 조회가 항상 DB를 다시 보게 수정.

## 5. 프론트엔드 비주얼 폴리시

`window.confirm()`으로 구현했던 나가기 확인창이 실제 사용 환경에서 아예 뜨지 않는 문제가 있어 자체 모달로 교체하면서, 전반적인 비주얼도 함께 다듬었습니다.

- Pretendard Variable 웹폰트 실제 로드(이전엔 CSS에서 참조만 하고 실제 `<link>`가 없었음).
- 버튼 눌림 피드백(`:active { transform: scale(0.97) }`), 카드/팝오버 진입 애니메이션, 채팅 버블(그라디언트 + 비대칭 radius), 알약형 채팅 입력창.
- WebSocket 연결 상태를 텍스트 뱃지 대신 **초록/회색 점**(`.status-dot.online`/`.offline`)으로 표시.
- `window.confirm()` → 화면 중앙 고정 커스텀 모달(`.modal-overlay`/`.modal`)로 교체 — 나가기 등 파괴적 액션 확인에 사용.
- 방 목록 안읽은 메시지 뱃지, 메시지별 안읽은 인원 수 힌트 UI 추가.

## 6. CI/CD

pem 키나 서버 배포 없이, **테스트 CI만** 우선 구축했습니다(배포는 스코프 제외).

- **백엔드**(`.github/workflows/ci.yml`): MySQL 8 / Redis 7 / RabbitMQ 3-management를 GitHub Actions 서비스 컨테이너로 띄워 로컬 `.env` 기반 개발 환경과 동일하게 맞춤(연차 동시성 제어 테스트 등 실제 DB 필요한 통합 테스트가 있어서). `./gradlew clean test` → `./gradlew bootJar`.
- **프론트엔드**: `npm ci` → lint(oxlint) → typecheck + build. 이 과정에서 `useAnnualLeave`라는 일반 비동기 API 함수가 이름이 `use`로 시작한다는 이유로 oxlint의 `react-hooks(rules-of-hooks)` 규칙에 걸리는 걸 발견해서 `applyAnnualLeave`로 이름을 바꿈(실제 Hook 규칙 위반은 아니었지만 헷갈리는 네이밍이라 정리).

## 7. 관련 PR 목록

| 저장소 | PR | 내용 |
| --- | --- | --- |
| chulgunhaza-backend | [#67](https://github.com/chulgunhaza/chulgunhaza-backend/pull/67) | 채팅 실사용 테스트 중 발견한 버그 2건 + 메시지 최소 길이 완화 |
| chulgunhaza-backend | [#68](https://github.com/chulgunhaza/chulgunhaza-backend/pull/68) | 단체 채팅(그룹 채팅) + 채팅방 나가기 |
| chulgunhaza-backend | [#69](https://github.com/chulgunhaza/chulgunhaza-backend/pull/69) | GitHub Actions 테스트 워크플로 |
| chulgunhaza-backend | [#70](https://github.com/chulgunhaza/chulgunhaza-backend/pull/70) | 채팅 읽음/안읽음 사람별 추적 재설계 + 실시간 읽음 알림 |
| chulgunhaza-frontend | [#1](https://github.com/chulgunhaza/chulgunhaza-frontend/pull/1) | GitHub Actions 워크플로 + rules-of-hooks 버그 수정 |
| chulgunhaza-frontend | [#2](https://github.com/chulgunhaza/chulgunhaza-frontend/pull/2) | 비주얼 폴리시(웹폰트/마이크로 인터랙션/채팅 버블) |
| chulgunhaza-frontend | [#3](https://github.com/chulgunhaza/chulgunhaza-frontend/pull/3) | WebSocket 자동 재연결 |

🤖 Generated with [Claude Code](https://claude.com/claude-code)
