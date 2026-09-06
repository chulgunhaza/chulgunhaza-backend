# 타임라인 — 세션 회전 버그 수정 & 관리자 페이지 추가

## 1. 세션 동시 요청 버그

| 단계 | 내용 |
|---|---|
| 발견 | 로그인 직후 대시보드에서 여러 API가 동시에 호출되면 일부가 401/500(`IllegalStateException: Session was invalidated`) |
| 재현 | curl로 동시 요청 5개 → 매번 4~5개 실패 |
| 1차 원인 진단(수정) | 인덱스형 세션 저장소(`@EnableRedisHttpSession`) 의심 → 제거했으나 동일하게 재현되어 기각 |
| 공식 자료 검증 | [spring-projects/spring-session#2893](https://github.com/spring-projects/spring-session/issues/2893)(공식 이슈, open), `RedisSessionRepository.java` 소스 직접 대조 |
| 진짜 원인 발견 | `SessionCheckFilter`가 매 요청마다 `Authentication`을 표준 저장소 없이 직접 재조립 → `SessionManagementFilter`가 "방금 로그인함"으로 오판해 **매 요청마다 세션 ID 회전** → 동시 요청은 서로 다른 새 ID로 회전 경쟁 → 실패 |
| 근거 확인 | [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)로 `SessionManagementFilter`의 `containsContext()` 판단 로직 재확인 |
| 수정 | `LoginSuccessHandler`에서 로그인 성공 시 `HttpSessionSecurityContextRepository`에 `SecurityContext` 1회 저장 |
| 검증 | 동시 요청 5개×10회(50건) → 10개×10회(100건) → 20개×10회(200건), **누적 350건 실패 0** |
| 문서화 | [`docs/troubleshooting-concurrent-session-race.md`](troubleshooting-concurrent-session-race.md) |
| 이슈 정리 | [#65](https://github.com/chulgunhaza/chulgunhaza-backend/issues/65) 닫음(동일 버그, PR #82로 해결) / [#73](https://github.com/chulgunhaza/chulgunhaza-backend/issues/73) 세션 부분만 해결 코멘트(DLQ 부분은 열어둠) |
| PR | [#82](https://github.com/chulgunhaza/chulgunhaza-backend/pull/82) **merged** |
| 부수 정리 | `RedisConfig.java`의 죽은 코드(`@EnableRedisHttpSession` + 미사용 세션 리포지토리 빈) 제거 |
| 프론트 임시 조치 되돌림 | 프론트에 넣었던 요청 직렬화(`client.ts`)는 백엔드 수정만으로 충분해 제거, [frontend#8](https://github.com/chulgunhaza/chulgunhaza-frontend/pull/8) 닫음 |

## 2. 이슈 트래커 전수 검토

열려있던 17개 이슈를 코드 대비로 하나씩 재확인 — `#65` 하나만 실제로 해결된 상태였고 나머지 16개는 전부 유효한 미완료 작업으로 확인(README "진행 상황" 표와 교차 검증).

## 3. 관리자 페이지

| 단계 | 내용 |
|---|---|
| 논의 | 관리자 페이지 만들기로 결정, 백로그(Epic 1~6) 작성 |
| 선행 조건 확인 | `UserRole`에 MANAGER/ADMIN 이미 정의, `EmployeeController`도 이미 MANAGER 권한으로 막혀있었으나 **시드 계정이 전부 USER뿐이라 테스트 불가** — 이슈 [#83](https://github.com/chulgunhaza/chulgunhaza-backend/issues/83) 생성 |
| 백엔드 준비 | `DataInitializer`에 `manager@chulgunhaza.com` 추가, 로그인 → 사원 생성(201)/일반 유저 시도(403)/삭제(200)까지 curl로 검증 |
| 프론트 구현 | `/admin/employees` 페이지(목록/등록/수정/삭제), `AdminRoute`로 권한 없으면 리다이렉트, nav 조건부 노출 |
| 브라우저 실측 중 버그 발견 | 삭제 버튼이 CORS 프리플라이트에서 403 — **CORS 허용 메서드에 `PATCH` 누락** (사원 삭제·게시글 삭제 공통 영향, curl로는 못 잡는 종류) → 즉시 수정 |
| 브라우저 실측 중 버그 발견 2 | 목록 페이지네이션 값이 `isFirstPage`가 아니라 `firstPage`로 옴 — Lombok+Jackson의 `isXxx()` boolean 게터 관례 문제, 이슈 [#84](https://github.com/chulgunhaza/chulgunhaza-backend/issues/84) 생성 후 즉시 수정(게터 직접 선언 + `@JsonProperty`) |
| PR | 백엔드 [#85](https://github.com/chulgunhaza/chulgunhaza-backend/pull/85) **merged** (Closes #83, #84) / 프론트 [#9](https://github.com/chulgunhaza/chulgunhaza-frontend/pull/9) **merged** |
| 최종 검증 | MANAGER 로그인 → 메뉴 노출 → 등록/수정/삭제 전부 브라우저에서 실제 클릭, USER 로그인 → 메뉴 안 보임 + URL 직접 접근 시 리다이렉트 확인 |

## 결과 요약

- **머지된 PR**: 백엔드 #82, #85 / 프론트 #9
- **닫은 이슈**: #65, #83, #84
- **코멘트로 부분 정리**: #73 (DLQ 부분은 열어둠)
- **되돌린 것**: 프론트 요청 직렬화(불필요해져서 제거, #8 닫음)
- **새로 생긴 기능**: `/admin/employees` 관리자 페이지 (MANAGER/ADMIN 전용)
