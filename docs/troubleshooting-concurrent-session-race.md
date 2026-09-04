# 트러블슈팅: 동시 요청 시 세션이 "invalidated"로 죽는 문제

## 증상

로그인 직후 대시보드에서 여러 API가 거의 동시에 호출되면, 그 중 일부가 401 또는 500으로
실패한다. 브라우저 콘솔에는 다음이 섞여서 찍힌다.

```
GET /v1/chat/find/rooms?page=0&size=100 → 401
GET /v1/chat/find/rooms?page=0&size=100 → 500
GET /v1/employee/list/212               → 200
GET /v1/post?category=공지&page=0&size=5 → 500
```

새로고침을 한 번 더 하면(요청이 순차적으로 나가면서) 대부분 사라져서 재현이 들쭉날쭉해
보이지만, 실제로는 "같은 세션으로 동시 요청 N개"라는 조건만 맞으면 항상 재현된다.

## 재현 방법

로그인 후 **같은 세션 쿠키로 동일 API를 동시에 5개** 호출.

```bash
curl -c cookies.txt -X POST http://localhost:8081/v1/employee/login \
  -d "username=test@chulgunhaza.com&password=test1234!"

for i in 1 2 3 4 5; do
  curl -b cookies.txt "http://localhost:8081/v1/chat/find/rooms?page=0&size=100" \
    -o /dev/null -w "req$i: %{http_code}\n" &
done; wait
```

결과 (여러 번 재현, 매번 같은 패턴):

```
req3: 200
req1: 500
req2: 500
req4: 500
req5: 500
```

## 서버 로그의 정확한 예외

```
java.lang.IllegalStateException: Session was invalidated
	at org.springframework.session.data.redis.RedisSessionRepository.save(RedisSessionRepository.java:129)
	at org.springframework.session.data.redis.RedisSessionRepository.save(RedisSessionRepository.java:45)
	at org.springframework.session.web.http.SessionRepositoryFilter$SessionRepositoryRequestWrapper.commitSession(SessionRepositoryFilter.java:229)
	at org.springframework.session.web.http.SessionRepositoryFilter.doFilterInternal(SessionRepositoryFilter.java:145)
	...
```

`spring-session-data-redis:3.3.5`의 `RedisSessionRepository.java:127-133`:

```java
@Override
public void save(RedisSession session) {
    if (!session.isNew) {
        String key = getSessionKey(session.hasChangedSessionId() ? session.originalSessionId : session.getId());
        Boolean sessionExists = this.sessionRedisOperations.hasKey(key);
        if (sessionExists == null || !sessionExists) {
            throw new IllegalStateException("Session was invalidated");  // ← 여기
        }
    }
    session.save();
}
```

## 진짜 원인 — 요청마다 세션 ID 자체가 새로 발급되고 있었다

처음엔 "동시 요청이면 `hasKey()` 체크가 막연히 경합한다" 정도로만 파악했는데, 더 파고드니
훨씬 구체적인 원인이 나왔다: **동시성과 무관하게, 이 서버는 요청 하나하나마다 세션 ID를
통째로 새로 발급하고 있었다.**

### 실측

```bash
# 로그인 → SID = d711a2b7-...
# 동시성 없이 그냥 GET 요청 1개만 보냄
curl -b cookies.txt "http://localhost:8081/v1/chat/find/rooms?page=0&size=5" -D -
```

응답 헤더:
```
Set-Cookie: SESSION=49aba897-e6a0-4704-83a4-c711a5b8a34f; Path=/; HttpOnly; SameSite=Lax
```
→ 로그인 때 받은 것과 다른 새 ID. 원래 세션 키는 Redis에서 사라짐(rename됨). **동시성
없이 요청 1개만으로도 재현된다.** 브라우저는 Set-Cookie를 자동으로 받아 다음 요청에
새 값을 보내니 겉으론 안 보이지만, 실제로는 요청마다 "새 세션 발급 + 이전 세션 rename"이
일어나고 있었다.

### 왜 이러는지 — Spring Security 공식 문서로 확인

[Spring Security 공식 세션 관리 문서](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html):

> "If the repository (SecurityContextRepository) doesn't [contain a security context], and
> the thread-local SecurityContext contains a (non-anonymous) Authentication object, the
> filter assumes they have been authenticated by a previous filter in the stack. It will
> then invoke the configured SessionAuthenticationStrategy [기본값: 세션 ID 변경]."

실제 `SessionManagementFilter` 소스(spring-security-web 7.1.0, 이 프로젝트가 쓰는
버전)로도 확인했다:

```java
// SessionManagementFilter.java:96-114
if (!this.securityContextRepository.containsContext(request)) {
    Authentication authentication = this.securityContextHolderStrategy.getContext().getAuthentication();
    if (this.trustResolver.isAuthenticated(authentication)) {
        // The user has been authenticated during the current request, so call the session strategy
        this.sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        // (기본 전략에 세션 ID 변경이 포함됨)
    }
}
```

### 코드상 트리거 — 우리 코드

[`SessionCheckFilter.java:67-69`](../src/main/java/com/example/chulgunhazabackend/security/filter/SessionCheckFilter.java):

```java
UsernamePasswordAuthenticationToken authenticationToken
        = new UsernamePasswordAuthenticationToken(employeeCredentialDto, password, employeeCredentialDto.getAuthorities());
SecurityContextHolder.getContext().setAuthentication(authenticationToken);
```

이 필터는 세션의 낱개 attribute(`id`,`email`,`password`...)를 직접 읽어서 **매 요청마다
`Authentication` 객체를 새로 조립**해 `SecurityContextHolder`에 꽂는다. Spring Security의
표준 저장 채널(`SecurityContextRepository`, 세션의 `SPRING_SECURITY_CONTEXT` 키)은 한 번도
쓰지 않는다. 그래서 위 `SessionManagementFilter`가 "저장소엔 아무 기록도 없는데 인증된
사용자가 있다" = "방금 로그인했다"고 **매 요청마다** 오판하고, 매번 세션 고정 공격 방지
로직(세션 ID 변경)을 발동시킨다.

실제로 로그인 직후 Redis 세션 해시를 확인해보면 `SPRING_SECURITY_CONTEXT` 필드 자체가
존재하지 않는다 — 로그인 시점에도 표준 저장소엔 아무것도 안 남기고 있었다:

```
sessionAttr:employeeNo, maxInactiveInterval, sessionAttr:id, sessionAttr:password,
sessionAttr:email, sessionAttr:department, lastAccessedTime, sessionAttr:name,
sessionAttr:roles, creationTime
```

### 동시 요청에서 500까지 이어지는 경로

동시 요청 N개는 각자 "지금 막 로그인함"으로 오인되어 **각자 다른 새 ID로 세션 회전**을
시도한다. 그 중 하나가 먼저 원본 키를 rename해버리면, 나머지 요청들이 `save()`에서
`hasKey(원본ID)`를 체크할 때 그 키가 이미 사라진 상태라 `IllegalStateException: Session
was invalidated`가 터진다. 요청이 순차적이면(브라우저가 Set-Cookie를 매번 챙기므로) 티가
안 나지만, 동시에 두 개 이상 몰리면 항상 재현된다.

## 배제한 원인 (직접 검증함)

- **세션 데이터 자체 오염 아님**: 로그인 직후 Redis 세션 해시의 `maxInactiveInterval`,
  `lastAccessedTime` 등은 정상, TTL도 설정값과 일치 → 조기 만료 설정 문제 아님.
- **인덱스형 세션 저장소(`@EnableRedisHttpSession`) 문제 아님**: 제거해봤지만(스택트레이스가
  `RedisIndexedSessionRepository` → 비인덱스 `RedisSessionRepository`로 바뀐 것 외엔)
  동일하게 재현됨. 인덱스형이든 비인덱스든 세션 ID가 매 요청 회전하는 한 똑같이 걸린다.
- **Redis 메모리 압박/eviction 아님**: `maxmemory=0`(무제한), `maxmemory-policy=noeviction`
  확인. 세션 개수도 적음(`DBSIZE` 한 자릿수~10 내외).

## 공식 자료로 재검증

### Spring Session — 동시 요청 시 `hasKey()` 경합 자체는 이미 알려진 미해결 버그

[`RedisSessionRepository.java` — spring-projects/spring-session (main 브랜치)](https://github.com/spring-projects/spring-session/blob/main/spring-session-data-redis/src/main/java/org/springframework/session/data/redis/RedisSessionRepository.java)
확인: 로컬 jar와 완전히 동일, 동시성 방어 로직 없음.

[spring-projects/spring-session#2893](https://github.com/spring-projects/spring-session/issues/2893)
— 동일 예외/클래스/메시지로 등록된 공식 이슈. 현재 상태: **Open**, 미해결. (그 이슈는
로그아웃 상황에서 재현됐지만, 이 프로젝트는 로그아웃 없이 순수 동시 GET만으로 재현했다 —
`hasKey()` 가드 자체가 동시성에 안전하지 않다는 걸 더 단순한 조건으로 보여준 셈이다.)

→ 즉 `hasKey()` 경합은 Spring Session 라이브러리 자체의 한계라 백엔드 코드로 근본 해결이
안 되지만, **이 프로젝트에서는 애초에 세션 ID가 매 요청 회전하지만 않으면 이 경합이 발생할
조건 자체가 안 생긴다** — 그래서 회전을 멈추는 쪽(아래 "적용한 수정")이 진짜 해결책이다.

## 결론

**원인**: `SessionCheckFilter`가 Spring Security 표준 `SecurityContextRepository`를 거치지
않고 매 요청마다 `Authentication`을 직접 재조립해 `SecurityContextHolder`에 꽂는다 →
`SessionManagementFilter`가 이를 "방금 로그인함"으로 오판 → 매 요청마다 세션 ID 회전
(`changeSessionId()`) → Redis에서 세션 키 rename.

**동시 요청에서 500까지**: 동시 요청 N개가 각자 다른 새 ID로 회전을 시도 → 하나만
성공하고 나머지는 원본 키가 이미 사라진 상태에서 `hasKey()` 체크에 걸려 `IllegalStateException:
Session was invalidated`.

## 적용한 수정 — 진짜 해결책

`LoginSuccessHandler`가 로그인 성공 시 표준 저장소(`HttpSessionSecurityContextRepository`)에
`SecurityContext`를 **한 번만** 저장하도록 한 줄 추가했다. 그 키는 로그아웃 전까지 계속
세션에 남아있으므로, `SessionCheckFilter`가 매 요청 자체 로직으로 `Authentication`을
새로 꽂더라도 `SessionManagementFilter`는 더 이상 "저장된 기록 없음"으로 오판하지 않는다.

```java
// LoginSuccessHandler.java
private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    ...
    httpSession.setMaxInactiveInterval(4 * 60 * 60);

    // 표준 저장소에도 한 번만 저장 — 이후 SessionManagementFilter가 "이미 저장된
    // 컨텍스트 있음"으로 정상 인식해서 더 이상 세션을 회전시키지 않는다.
    securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
    ...
}
```

### 검증 (총 150회 반복)

수정 반영 후 재기동, Redis 초기화(`FLUSHALL`) 후 처음부터 다시 테스트.

**1. 세션 ID 회전이 멈췄는지** — 로그인 후 단일 GET 요청에 더 이상 `Set-Cookie`가 안 옴
(회전 없음, 정상).

**2. 원래 재현 절차(동시 요청 5개)를 10회 반복, 총 50개 요청**: 50/50 성공, 실패 0.

**3. 동시성을 더 끌어올려서 재검증 — 동시 요청 10개 × 10회 반복, 총 100개 요청**
(로그인마다 세션을 새로 발급해 매 라운드 독립적으로 테스트):

```
run 1  (동시 10개): [200×10]  실패:0
run 2  (동시 10개): [200×10]  실패:0
run 3  (동시 10개): [200×10]  실패:0
run 4  (동시 10개): [200×10]  실패:0
run 5  (동시 10개): [200×10]  실패:0
run 6  (동시 10개): [200×10]  실패:0
run 7  (동시 10개): [200×10]  실패:0
run 8  (동시 10개): [200×10]  실패:0
run 9  (동시 10개): [200×10]  실패:0
run 10 (동시 10개): [200×10]  실패:0
=== 총 실패: 0 / 100 ===
```

두 단계 합쳐 **총 150개 요청 중 실패 0건.** 동시성을 5→10으로 두 배 올려도 결과는
동일했다 — 백엔드 로그에도 `IllegalStateException`/`ERROR` 0건. **애초에 세션 ID가 안
바뀌니 `hasKey()` 경합이 발생할 조건 자체가 사라졌다.**

## 프론트엔드 보완 조치 (안전망, 필수는 아님)

백엔드 수정 전에는 임시방편으로 `chulgunhaza-frontend`의 `src/api/client.ts`에 axios
요청 직렬화(동시 요청 1개로 제한)를 적용했었다. 백엔드가 근본 해결된 지금은 필수는
아니지만, "다른 탭/다른 클라이언트가 같은 세션으로 동시에 붙는" 것까지 프론트 요청
직렬화가 100% 막아주진 않으므로(이건 각 클라이언트 자체의 요청 패턴 문제라 프론트 큐로는
못 막는다 — 다만 이제 백엔드가 애초에 동시 요청을 견디니 이 잔여 리스크도 사실상 사라졌다)
안전망 성격으로 그대로 남겨뒀다.

## 참고: 남겨둔 백엔드 정리 (이 문제의 직접적인 원인은 아니었음)

`RedisConfig.java`에서 다음을 제거했다 — 원인은 아니었지만 죽은 코드였고 굳이 켜둘
이유가 없어서 정리는 유지하기로 했다:

- `@EnableRedisHttpSession` — 인덱스형 세션 저장소를 강제로 켜고 있었는데, 이 프로젝트는
  "특정 유저의 모든 세션 찾기" 같은 인덱스 조회 기능(`FindByIndexNameSessionRepository`)을
  전혀 쓰지 않는다.
- 미사용 `redisSessionRepository()` 빈 — `StringRedisSerializer`로 세션 값을 직렬화하는
  또 다른 세션 저장소를 수동으로 만들고 있었는데, 실제로는 `SessionRepositoryFilter`가
  쓰는 빈이 아니라 죽은 코드였다.

`application.yml`의 `spring.session.store-type: redis` 자동 구성(비인덱스
`RedisSessionRepository`)이 그대로 세션을 처리한다.
