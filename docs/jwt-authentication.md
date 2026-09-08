# 인증 설계: 세션(Spring Session Redis) → JWT(RS256)

[#98](https://github.com/chulgunhaza/chulgunhaza-backend/issues/98)에서 진행한
전환의 배경과 설계를 정리한다. 관련 이슈: [#74](https://github.com/chulgunhaza/chulgunhaza-backend/issues/74)(서비스 분리 로드맵), [#99](https://github.com/chulgunhaza/chulgunhaza-backend/issues/99)~[#102](https://github.com/chulgunhaza/chulgunhaza-backend/issues/102)(후속 서비스 분리 작업).

## 왜 바꿨나

쿠버네티스로 옮기면서 서비스를 `user` / `attendance` / `chatting` 3개로 쪼개기로
했다(#74). 세션 기반 인증을 유지하려면 서비스가 늘어날 때마다 각 서비스가 매 요청마다
Redis에 세션을 왕복 조회해야 한다. JWT는 서명된 토큰 자체가 인증 증명이라, 검증
키(public key)만 나눠 가지면 Redis 왕복 없이 각 서비스가 독립적으로 인증을 검증할 수
있다.

## 핵심 결정

| 항목 | 결정 | 이유 |
|---|---|---|
| 서명 알고리즘 | RS256 (RSA 2048bit) | user-server만 private key로 서명하고, 이후 분리될 attendance/chatting-server는 public key로 검증만 하면 됨. HS256(대칭키)은 서비스가 늘어날 때마다 비밀키를 공유해야 해서 서버 하나만 뚫려도 위조 가능 |
| 토큰 보관 위치 | httpOnly 쿠키 (`access_token`, `refresh_token`) | WebSocket 핸드셰이크는 브라우저 네이티브 API라 커스텀 헤더를 못 보내는데, 쿠키는 자동으로 실린다 — 아래 "WebSocket 인증이 코드 변경 없이 그대로 동작한 이유" 참고 |
| Access 토큰 TTL | 15분 | 탈취돼도 피해 시간을 짧게 |
| Refresh 토큰 TTL | 7일, Redis에 jti만 저장(값 자체는 저장 안 함) | 로그아웃/재발급 시 jti만 지우거나 바꾸면 이전 토큰이 즉시 무효화됨 |
| 클레임 | `id`, `email`, `name`, `employeeNo`, `roles`, `department` | Employee 애그리거트 자신의 공개 필드만. **`password`는 절대 포함하지 않음** — JWT payload는 base64만 씌워진 것이라 토큰을 가진 누구나 디코딩해서 볼 수 있다(세션 attribute였을 땐 서버 안에만 있어서 문제가 없었지만, JWT로 그대로 옮기면서 발견해 뺐다) |

## 토큰 발급/검증 흐름

- 로그인(`LoginSuccessHandler`): access/refresh 토큰 발급 → `RefreshTokenStore`에
  jti 저장 → 둘 다 `CookieUtil`로 httpOnly 쿠키 응답
- 요청 인증(`JwtAuthenticationFilter`): `access_token` 쿠키 검증 → 통과하면
  `SecurityContextHolder`에 `Authentication` 심음. 예전 `SessionCheckFilter`와 같은
  위치(`UsernamePasswordAuthenticationFilter` 앞)에 꽂혀 있어서, 그 이후 로직
  (`@AuthenticationPrincipal`, `@PreAuthorize` 등)은 손댈 게 없었다
- 재발급(`AuthController#refresh`, `POST /v1/employee/token/refresh`):
  `refresh_token` 쿠키 검증 → Redis에 저장된 jti와 일치하는지 확인(다르면 이미
  로그아웃됐거나 재사용 공격 — 재로그인 요구) → 최신 사원 정보(role/부서)를 다시
  조회해서 access/refresh 둘 다 재발급(회전)
- 로그아웃(`JwtLogoutHandler`): `refresh_token` 쿠키를 직접 파싱해 employeeId를
  뽑아 Redis jti 삭제 + 쿠키 둘 다 삭제

### 로그아웃 핸들러가 `Authentication` 대신 쿠키를 직접 파싱하는 이유

처음엔 `LogoutHandler.logout(request, response, authentication)`의 `authentication`
파라미터에서 employeeId를 꺼내려고 했는데, **실측해보니 항상 null이었다.** Spring
Security 기본 필터 체인에서 `LogoutFilter`가 `UsernamePasswordAuthenticationFilter`
보다 먼저 실행되는데, `JwtAuthenticationFilter`는
`addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`로만
꽂혀 있어서 `LogoutFilter` 시점엔 아직 `SecurityContext`가 채워지기 전이었다 — 그
결과 로그아웃해도 Redis의 refresh 토큰이 안 지워지는 버그가 났다(curl로 직접
재현·확인). `refresh_token` 쿠키를 요청에서 직접 읽어 파싱하는 방식으로 바꿔서
해결했다 — 필터 순서와 무관하고, access 토큰이 이미 만료된 채로 로그아웃을 호출하는
경우에도 문제없이 동작한다.

## WebSocket 인증이 코드 변경 없이 그대로 동작한 이유

`WebSocketConfig`/`SecurityContextInterceptor`는 핸드셰이크 시점에
`SecurityContextHolder.getContext()`에 있는 값을 그대로 읽어다 WebSocket 세션
attribute(`SPRING_SECURITY_CONTEXT`)에 옮겨 담을 뿐, 그 값이 세션에서 왔는지 JWT에서
왔는지는 전혀 신경 쓰지 않는 구조였다. WebSocket 핸드셰이크도 결국 하나의 HTTP
요청이라 `JwtAuthenticationFilter`를 그대로 통과하고, 그 필터가
`SecurityContextHolder`를 채워두면 이후는 예전과 동일하게 동작한다. 실제로 curl로
`Connection: Upgrade` 요청에 `access_token` 쿠키만 실어 보내서 `101 Switching
Protocols`를 확인했고, 쿠키 없이 보내면 `JwtAuthenticationFilter`가 업그레이드
전에 401을 반환하는 것도 확인했다. 이게 토큰 보관 위치를 httpOnly 쿠키로 정한
핵심 이유다 — Authorization 헤더 + localStorage 방식이었다면 브라우저 네이티브
WebSocket이 커스텀 헤더를 못 보내서 핸드셰이크 URL 쿼리 파라미터로 토큰을 실어야
했고, `WebSocketConfig` 쪽에 새 인터셉터가 필요했을 것이다.

## 프론트엔드: 401 시 자동 재발급

`access_token`은 15분마다 만료되므로, `src/api/client.ts`의 axios 응답
인터셉터가 401을 받으면 `POST /v1/employee/token/refresh`를 한 번 호출하고
원래 요청을 재시도한다. 동시에 여러 요청이 401나도 재발급 호출은 하나의
프라미스로 합쳐서 한 번만 나간다 — 안 그러면 재발급 엔드포인트가 refresh
토큰을 매번 회전시키므로, 경쟁하는 요청끼리 서로의 새 토큰을 무효화시킨다.
실제로 access 토큰 TTL을 5초로 낮춰서 재현해보니, 동시에 401난 요청 2개가
재발급 호출 1개로 합쳐지고 둘 다 정상 재시도되는 걸 확인했다.

또한 `GET /v1/employee/me`를 새로 추가해서, 새로고침(특히 `sessionStorage` 캐시가
없는 새 탭)에서도 서버에 직접 로그인 상태를 물어볼 수 있게 했다 — 세션 시절엔 이
확인 API 자체가 없어서 `sessionStorage` 캐시만 믿었던 갭이었다.

## 검증 기록

- 로그인 → `Set-Cookie`에 `access_token`/`refresh_token`이 httpOnly로 내려오고
  `JSESSIONID`는 더 이상 없음을 curl로 확인
- 보호된 API(`/v1/employee/me`)를 유효한 쿠키로 호출 → 200, 쿠키 없이 호출 → 401
- 위조/오염된 `access_token` → 401(`로그인이 만료되었거나 유효하지 않습니다`)
- `/token/refresh`로 재발급 → 새 쿠키 발급 확인, **재발급 전 refresh 토큰을 재사용하면
  401**(회전 확인)
- 로그아웃 → Redis의 `refresh_token:{employeeId}` 키 삭제 확인, 이후 그 refresh
  토큰으로 재발급 시도 시 401
- WebSocket 핸드셰이크 → 유효한 `access_token` 쿠키면 `101`, 없으면 `401`
- 프론트 실브라우저 테스트: 로그인 → 새로고침(다시 로그인 화면 안 뜸, `/me` 200
  확인) → access 토큰 TTL을 5초로 낮춰 재현한 만료 상황에서 페이지 이동 → 네트워크
  로그에서 `401 → 단일 /token/refresh 호출 → 200 재시도` 순서 확인
- 백엔드 유닛 테스트: `JwtProviderTest`(발급/파싱 라운드트립, password 미포함
  확인, 위조/만료 토큰 거부) + 기존 125개 전체 통과(총 130개)
