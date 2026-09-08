# Gradle 멀티모듈 구조 (#99)

[#74](https://github.com/chulgunhaza/chulgunhaza-backend/issues/74) 서비스 분리
로드맵의 1차 착수 작업. 서비스 3개(user/attendance/chatting)를 하나의 레포 안에서
Gradle 멀티모듈로 재구성하고, MySQL 인스턴스는 하나로 유지한 채 스키마만 나눴다.

## 모듈 구조

```
chulgunhaza-backend/            (루트, 애그리게이터 — 부팅 불가)
├── common/                     (라이브러리 모듈, java-library 플러그인)
├── user-server/                (기존 코드 전부 — :8081, 지금의 "그 앱")
├── attendance-server/          (신규, 최소 스켈레톤 — :8082)
└── chatting-server/            (신규, 최소 스켈레톤 — :8083)
```

- **common**: JWT 발급/검증(`security/jwt/*`, `security/filter/JwtAuthenticationFilter`)과
  `EmployeeCredentialDto`처럼 여러 서비스가 함께 필요로 하는 최소 코드만. 도메인
  엔티티는 절대 넣지 않는다 — 공유 엔티티가 생기는 순간 서비스 분리 자체가
  무의미해진다.
- **user-server**: 지금은 예전 `chulgunhaza-backend` 그 자체다. Employee/Attendance/
  Annual/Post/Chat 도메인이 전부 여기 있다. [#100](https://github.com/chulgunhaza/chulgunhaza-backend/issues/100)/[#101](https://github.com/chulgunhaza/chulgunhaza-backend/issues/101)에서
  Attendance/Chat이 각자 서버로 옮겨가면 점점 가벼워진다.
- **attendance-server / chatting-server**: 이번엔 **부팅 가능한 최소 스켈레톤만**.
  `spring-boot-starter-web` + `spring-boot-starter-actuator`뿐이고 데이터소스도
  아직 안 붙였다. `/actuator/health`로 "떠 있는지"만 증명하는 상태 — 실제 도메인
  코드 이전은 #100/#101의 몫이다.

## 왜 패키지명을 안 바꿨나

4개 모듈 전부 `com.example.chulgunhazabackend` 패키지를 그대로 쓴다. 보통
멀티모듈 전환에서는 모듈별로 패키지를 나누지만, 여기서는 일부러 안 나눴다 —

- `common`으로 뺀 클래스들(JWT, `EmployeeCredentialDto`)을 참조하던 기존 import문을
  **하나도 안 고쳐도** 되게 하기 위해서. 146개 파일을 옮기면서 동시에 몇백 개의
  import까지 다시 쓰면 리뷰도, 실수 발견도 훨씬 어려워진다.
- 같은 패키지가 여러 jar(모듈)에 걸쳐 있어도 JVM 클래스패스 기준으로는 아무
  문제가 없다. 각 서비스는 독립된 실행 아티팩트(각자의 `bootJar`)라 런타임에
  같은 클래스패스에 동시에 올라갈 일이 없어서, JPMS(Java 9+ 모듈 시스템)를 쓰는
  게 아니라면 split package 경고 이상의 문제가 생기지 않는다.
- #100/#101에서 실제 도메인 코드(`domain/attendance`, `domain/chat` 등)를 옮길
  때도 이 원칙을 그대로 유지한다 — 패키지명 불변, 물리적 위치만 이동.

## common의 api vs implementation 경계

`common`은 `java` 대신 **`java-library`** 플러그인을 쓴다 — `api`/`implementation`
구분이 `java-library`에서만 제공되기 때문이다. 판단 기준은 하나: **common의
public 클래스가 그 타입을 메서드 시그니처(파라미터/리턴 타입/상속)에 노출하는가?**

- `api`로 노출: `spring-boot-starter-security`(`EmployeeCredentialDto extends
  User`), `spring-boot-starter-web`(`CookieUtil`/`JwtAuthenticationFilter`가
  `jakarta.servlet.*` 타입을 시그니처에 노출), `jjwt-api`(`JwtProvider.parseClaims()`가
  `io.jsonwebtoken.Claims`를 리턴)
- `implementation`으로 캡슐화: `gson`(`JwtAuthenticationFilter` 내부에서만 JSON
  직렬화에 씀, 외부에 안 드러남), `jjwt-impl`/`jjwt-jackson`(런타임 구현체,
  `runtimeOnly`)

이 경계를 잘못 잡으면 `user-server`가 `common`을 참조하는 순간 "cannot find
symbol: Claims" 같은 컴파일 에러로 바로 드러난다 — 실제로 이 방식으로 검증했다.

`RefreshTokenStore`는 common으로 옮기지 않았다. Redis 기반 refresh 토큰 발급/
회전은 로그인 주체인 user-server만의 관심사라, 굳이 공유 모듈에 놓을 이유가
없다.

## MySQL 스키마

기존 단일 스키마 `chulgunhaza`를 `chulgunhaza_user`로 이름을 바꾸고(기존
스키마는 삭제하지 않고 방치), `chulgunhaza_attendance`/`chulgunhaza_chatting`을
새로 만들었다(`docs/sql/create-schemas.sql`). 인스턴스는 하나 그대로 — DB per
service가 아니라 스키마 분리인 이유는 [#99 이슈 본문](https://github.com/chulgunhaza/chulgunhaza-backend/issues/99)
참고. `attendance-server`/`chatting-server`는 아직 데이터소스를 안 붙였으므로
(위 "최소 스켈레톤" 참고), 스키마는 만들어만 뒀고 실제 연결은 #100/#101에서
붙는다.

## 실측으로 발견한 문제 2가지

전환하고 `./gradlew :user-server:bootRun`으로 실제로 띄워보니 두 가지가 깨져 있었다.

1. **CI가 이미 깨져 있었음**: `#98`(JWT) 머지 직후 `main` 브랜치 CI가 계속
   실패하고 있었다 — `JwtKeyProvider`의 `@PostConstruct`가 `.env`의
   `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`를 Base64 디코딩하는데, CI 워크플로에는
   이 값이 아예 없어서 모든 컨텍스트 로딩 테스트가 `IllegalArgumentException`으로
   실패하고 있었다. `ci.yml`에 CI 전용 테스트 키페어(로컬 `.env`와는 다른 별개
   키)를 추가해서 고쳤다 — 이번 스키마 이름 변경 작업 중 `ci.yml`을 다시 열어보다가
   발견.
2. **`spring-boot-docker-compose`가 compose 파일을 못 찾음**: `compose.yaml`이
   저장소 루트에 있었는데, `spring-boot-docker-compose`(devtools)는 **앱을
   부팅한 모듈의 작업 디렉터리 기준**으로 compose 파일을 찾는다. `user-server`
   모듈에서 `bootRun`을 실행하면 작업 디렉터리가 `user-server/`가 되므로
   `IllegalStateException: No Docker Compose file found`로 즉시 부팅 실패했다.
   `compose.yaml`을 `user-server/compose.yaml`로 옮겨서 해결.

## 실행 방법 변경

루트에 더 이상 부팅 가능한 앱이 없으므로, 모듈을 지정해서 실행해야 한다.

```bash
./gradlew :user-server:bootRun          # 기존 앱, :8081 (변경 없음)
./gradlew :attendance-server:bootRun    # 신규 스켈레톤, :8082
./gradlew :chatting-server:bootRun      # 신규 스켈레톤, :8083
```

`./gradlew clean test`는 그대로 루트에서 실행하면 4개 모듈 전체를 대상으로
돈다(멀티모듈이어도 서브프로젝트의 `test` 태스크가 루트 태스크에 자동으로
묶인다).
