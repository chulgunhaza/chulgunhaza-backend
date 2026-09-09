# kubeadm VM 배포

지금까지 4개 백엔드 모듈(user/attendance/chatting-server + common) + 프론트를
전부 로컬에서 `./gradlew bootRun`으로 손으로 하나씩 띄워왔다. CI/CD 착수
전에, 이미 VM에 떠 있는 **kubeadm 단일 노드(컨트롤플레인=워커) k8s
클러스터**에 전체 스택(백엔드 3개 + 프론트 + MySQL/Redis/RabbitMQ)을 한 번에
올리는 `scripts/vm-deploy.sh`를 만들었다. 컨테이너 빌드 도구는 (처음
검토했던 rkt는 2020년 CNCF에서 아카이브된 폐기 프로젝트라 제외하고) Podman으로
정했다.

Mac에는 그 VM의 kubeconfig가 없어서 **SSH로 VM에 들어가서** git pull →
podman build → containerd import → kubectl apply까지 전부 처리한다.
이미지 레지스트리는 안 쓰고(등록/운영 부담), VM 안에서 직접 빌드해서
containerd 로컬 이미지 저장소에 바로 넣는 방식으로 간다. 클러스터 접속은
포트포워딩 없이 **VM IP + NodePort로 직접** 접근한다 — 이 결정 때문에 프론트의
하드코딩된 `localhost:808X` URL을 빌드 타임에 주입 가능한 환경변수로 바꾸는
실제 코드 변경이 필요했다(아래 참고).

## 왜 이런 구조인가

- **레지스트리 없음**: VM 안에서 `podman build` → `podman save` →
  `sudo ctr -n k8s.io images import`로 containerd 로컬 이미지 저장소에 바로
  넣는다. `imagePullPolicy: Never` + `localhost/` 접두사로 레지스트리 접근
  자체를 안 함. 태그는 `latest` 고정이 아니라 **git 커밋 short-sha**로 매번
  다르게 줘서, 재배포할 때마다 kubelet이 확실히 새 이미지를 쓰게 한다
  (`kubectl set image`로 Deployment의 이미지 태그를 그 sha로 갱신 → 자동
  롤링 재시작).
- **`localhost` 대신 Service DNS명 + 환경변수 오버라이드**: 로컬 개발
  (`./gradlew bootRun`)은 전부 `localhost` 기준이지만 k8s는 Pod마다 네트워크
  네임스페이스가 분리돼 있다. 기존 애플리케이션 코드가 전부 `${VAR}` 환경변수로
  접속 정보를 받게 돼있어서 **코드 변경 없이** k8s 매니페스트의 env 값만
  Service명(`mysql`/`redis`/`rabbitmq`/`user-server`/`attendance-server`)으로
  바꿔주면 그대로 동작한다.
    - `ATTENDANCE_SERVER_INTERNAL_URL=http://attendance-server:8082`,
      `USER_SERVER_INTERNAL_URL=http://user-server:8081` — 이미
      `${VAR:default}` 형태라 그대로 오버라이드
    - `SPRING_REDIS_HOST=redis` — 이미 `${SPRING_REDIS_HOST}` 형태
    - DB는 `DATABASE_URL`/`ATTENDANCE_DATABASE_URL`/`CHATTING_DATABASE_URL`이
      통짜 JDBC URL 문자열이라 호스트 부분만 `mysql`(Service명)로 바꿔서
      그대로 주입
    - RabbitMQ는 attendance/chatting-server의 `application.yml`에
      `host: localhost`가 **리터럴로 하드코딩**돼 있는데, Spring Boot의
      프로퍼티 우선순위는 OS 환경변수가 `application.yml`보다 높기 때문에
      **코드 변경 없이** `SPRING_RABBITMQ_HOST=rabbitmq` 환경변수만 주면 그
      값이 이긴다 — 실측으로 로그(`Created new connection:
      ...amqp://guest@<rabbitmq ClusterIP>:5672`)까지 확인했다. user-server는
      이 설정 자체가 없어서 스프링 기본 자동설정이 `SPRING_RABBITMQ_HOST`
      환경변수를 그대로 읽는다
    - CORS는 3개 서비스 전부 `cors.allowed-origins:
      ${CORS_ALLOWED_ORIGINS:...}`라 `CORS_ALLOWED_ORIGINS=http://<VM_IP>:30080`
      으로 오버라이드
- **프론트만 실제 코드 변경**: 포트포워딩 없이 VM IP로 직접 접근하기로 해서,
  브라우저가 부르는 API/WebSocket 주소가 더 이상 `localhost`가 아니다. Vite
  정적 빌드는 런타임 설정 주입이 안 되므로(빌드 타임에 확정돼야 함)
  `chulgunhaza-frontend`의 `src/api/client.ts`/`src/hooks/useChatSocket.ts`의
  하드코딩된 URL을 `import.meta.env.VITE_*`로 빼고, 컨테이너 빌드 시점
  (`vm-deploy.sh`의 `podman build --build-arg`)에만 VM IP를 주입한다.
  기본값은 그대로 `localhost:808X`라 `npm run dev` 워크플로는 전혀 안
  바뀐다(타입은 `src/vite-env.d.ts`에 선언).
- **NodePort로 직접 노출 (Ingress 없음)**: 기본 NodePort 대역(30000-32767,
  apiserver 플래그 안 건드림)에 맞춰 `frontend 30080`, `user-server 30081`,
  `attendance-server 30082`, `chatting-server 30083`(WebSocket도 같은
  포트로 같이 나감).
- **시크릿은 통짜로**: `.env.k8s`(VM에만 존재, git에 안 올라감) 하나를
  `kubectl create secret generic app-env --from-env-file=.env.k8s`로 통째로
  Secret에 넣고, 모든 Deployment가 `envFrom: secretRef: app-env`로 한 번에
  받는다. 엄밀히는 `CORS_ALLOWED_ORIGINS`처럼 민감하지 않은 값도 섞여
  있지만, 이 스크립트 하나짜리 배포 범위에서는 오브젝트를 여러 개로 쪼개는
  것보다 단순함을 택했다.

## 파일 구성

| 파일 | 내용 |
|---|---|
| `k8s/mysql.yaml` | Deployment+Service+ConfigMap(스키마 3개 생성, `docs/sql/create-schemas.sql`과 동일 내용, hostPath로 데이터 유지) |
| `k8s/redis.yaml` | Deployment+Service |
| `k8s/rabbitmq.yaml` | Deployment+Service (계정은 app-env Secret에서 매핑) |
| `k8s/user-server.yaml` / `k8s/attendance-server.yaml` / `k8s/chatting-server.yaml` | 각 서비스 Deployment+NodePort Service (30081/30082/30083) |
| `k8s/frontend.yaml` | 프론트 Deployment+NodePort Service (30080) |
| `k8s/.env.k8s.example` | VM에서 `.env.k8s`로 복사해서 채우는 템플릿 |
| `{user,attendance,chatting}-server/Containerfile` | 멀티스테이지(`eclipse-temurin:17-jdk` 빌드 → `17-jre` 런타임 — amd64/arm64 멀티아키 태그, alpine 변형은 arm64 미지원이라 안 씀), 빌드 컨텍스트는 레포 루트(멀티모듈이라 `common` 필요) |
| `chulgunhaza-frontend/Containerfile` | 멀티스테이지(`node:20-alpine` 빌드 → `nginx:alpine` 서빙), React Router SPA 폴백은 `nginx.conf` |
| `scripts/vm-deploy.sh` | Mac에서 실행하는 SSH 래퍼. `up`/`down`/`logs <service>`/`restart` |

## 실행 절차

### 1. Mac ↔ VM SSH 키 설정 (최초 1회)

`vm-deploy.sh`는 SSH로 원격 스크립트 본문을 표준입력(heredoc)으로 흘려보내는
구조라, 비밀번호 프롬프트에 답할 방법이 없다 — **키 기반 인증이 필수**다.

```bash
# Mac에서
ssh-keygen -t ed25519 -f ~/.ssh/id_ed25519_chulgunhaza_vm -N "" -C "vm-deploy@chulgunhaza"
cat ~/.ssh/id_ed25519_chulgunhaza_vm.pub   # 아래에서 VM에 등록할 값

cat >> ~/.ssh/config <<'EOF'

Host chulgunhaza-vm
    HostName <VM_IP>
    User <VM_USER>
    IdentityFile ~/.ssh/id_ed25519_chulgunhaza_vm
EOF
```

```bash
# VM에서(콘솔 또는 비밀번호로 최초 1회 로그인해서)
echo "<위에서 출력된 공개키(ssh-ed25519 AAAA... 전체)>" >> ~/.ssh/authorized_keys
```

Mac에서 `ssh chulgunhaza-vm`이 비밀번호 없이 붙으면 완료.

### 2. VM 쪽 최초 1회 준비

**메모리 4GB 미만이면 스왑부터 만들어라 — 필수, 선택 아님.** 이 클러스터
(kubeadm 컨트롤플레인 전체: etcd/kube-apiserver/kubelet/coredns 등) +
MySQL/Redis/RabbitMQ + 앱 파드 3개가 이미 떠 있는 상태에서 `podman build
--no-cache`로 Gradle 멀티모듈 3개를 새로 빌드하면 순간적으로 메모리를
크게 잡아먹는다 — 실측(RAM 3.5GB, 스왑 0B인 VM)으로 `free -h`가 64Mi
남기고 꽉 차서 `kswapd0`가 CPU를 갈아넣고 load average가 **180**까지
치솟는 걸 확인했다. 스왑이 없으면 이 상태에서 OOM killer가 무작위로
프로세스를 죽이기 시작할 수 있고, 하필 etcd나 kube-apiserver가 죽으면
클러스터 자체가 망가진다. 4GB 스왑 추가 후 같은 배포가 문제없이(load
average 한 자릿수로) 끝나는 것까지 확인했다.

```bash
# VM 안에서 — 스왑이 이미 있는지 확인
free -h
# Swap 줄이 0B면 아래로 4GB 스왑 파일 추가(재부팅해도 유지되게 fstab 등록)
sudo fallocate -l 4G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

```bash
# VM 안에서 (배포판에 맞게 — 예: Rocky/RHEL 계열은 dnf, Ubuntu/Debian은 apt)
dnf install -y git podman   # 또는: sudo apt install -y git podman
# kubectl/kubeadm은 이미 클러스터가 떠 있다는 전제라 보통 이미 있음

# SSH 접속 유저가 root가 아니면, ctr import에 sudo 비밀번호를 못 물어보므로
# (스크립트가 SSH stdin을 스크립트 전달용으로 이미 쓰고 있어서) 필수:
echo "$USER ALL=(ALL) NOPASSWD: /usr/bin/ctr" | sudo tee /etc/sudoers.d/ctr-nopasswd
# (root로 접속한다면 이 단계는 필요 없음 — sudo -n true로 미리 확인 가능)

git clone https://github.com/chulgunhaza/chulgunhaza-backend.git
cd chulgunhaza-backend
cp k8s/.env.k8s.example .env.k8s
vim .env.k8s   # DB/JWT/RabbitMQ 비밀번호, <VM_IP>를 이 VM의 실제 IP로 채우기
```

### 3. Mac 쪽 스크립트 설정

`scripts/vm-deploy.sh` 상단의 두 줄만 채운다:

```bash
VM_HOST="chulgunhaza-vm"       # 1번에서 등록한 ssh config alias(또는 user@ip)
VM_IP="192.168.64.5"           # 브라우저가 NodePort로 접속할 IP
```

### 4. 배포

```bash
./scripts/vm-deploy.sh up
```

내부적으로: VM에서 `git pull` → 4개 이미지 `podman build`/`save`/
`ctr import` → `app-env` Secret 갱신 → `kubectl apply -f k8s/` →
`kubectl set image`로 방금 빌드한 태그로 갱신 → 롤아웃 대기. 끝나면 접속 URL
요약이 출력된다.

### 5. 확인

`http://<VM_IP>:30080`에서 `test@chulgunhaza.com` / `test1234!`로 로그인해서
대시보드/출근 등록/채팅(실시간 메시지 포함)까지 확인한다.

### 6. 재배포 / 로그 / 종료

```bash
# 코드 수정 → git push 한 뒤
./scripts/vm-deploy.sh up        # 재배포 (git short-sha 태그로 항상 새로 빌드)

./scripts/vm-deploy.sh logs chatting-server   # 특정 서비스 로그 스트리밍
./scripts/vm-deploy.sh down                   # 전체 리소스 삭제
./scripts/vm-deploy.sh restart                # down + up
```

## 로컬(Mac)에서 kind로 미리 검증하기 (선택, 권장)

`vm-deploy.sh`는 VM 전용(containerd `ctr` import 사용)이라 그대로는 Mac에서
못 돌린다. 대신 **kind**(Kubernetes IN Docker)로 진짜 k8s 클러스터를 Mac에
띄워서 매니페스트 자체가 맞는지, 서비스 간 통신(Service DNS, RabbitMQ host
오버라이드 등)이 실제로 되는지 검증할 수 있다 — 이 문서에 적힌 설계 전부
이 방법으로 실측 검증했다(로그인, 출근 등록, 실시간 채팅까지 브라우저로
확인).

```bash
brew install kind kubectl

cat > /tmp/kind-config.yaml <<'EOF'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 30080
      - containerPort: 30081
        hostPort: 30081
      - containerPort: 30082
        hostPort: 30082
      - containerPort: 30083
        hostPort: 30083
EOF
kind create cluster --name chulgunhaza-verify --config /tmp/kind-config.yaml
```

`Containerfile`들은 `eclipse-temurin:17-jdk`/`17-jre`(멀티아키: amd64+arm64)를
쓰므로 Apple Silicon Mac에서도, arm64 VM에서도 그대로 빌드된다 — 원래
alpine 변형(`17-jdk-alpine`/`17-jre-alpine`)은 amd64 전용이라 arm64에서
`no match for platform in manifest`로 실패해서 멀티아키 태그로 바꿨다(실측
확인, 트러블슈팅 표 참고).

```bash
GIT_SHA=verify$(date +%s)
for svc in user-server attendance-server chatting-server; do
  docker build -f $svc/Containerfile -t localhost/$svc:$GIT_SHA .
done

cd ../chulgunhaza-frontend
docker build -f Containerfile -t localhost/frontend:$GIT_SHA \
  --build-arg VITE_API_BASE_URL=http://localhost:30081 \
  --build-arg VITE_ATTENDANCE_API_BASE_URL=http://localhost:30082 \
  --build-arg VITE_CHATTING_API_BASE_URL=http://localhost:30083 \
  --build-arg VITE_WS_URL=ws://localhost:30083/websocket \
  .
cd ../chulgunhaza-backend

for svc in user-server attendance-server chatting-server frontend; do
  kind load docker-image localhost/$svc:$GIT_SHA --name chulgunhaza-verify
done
```

시크릿/매니페스트 적용(`.env.k8s.example`을 복사하되 `<VM_IP>` 자리엔
`localhost`를 넣는다 — kind가 NodePort를 host로 매핑해뒀으므로):

```bash
cp k8s/.env.k8s.example /tmp/verify.env.k8s
# CORS_ALLOWED_ORIGINS=http://localhost:30080 로, 비밀번호류는 아무 값이나,
# JWT_PRIVATE_KEY/JWT_PUBLIC_KEY는 레포 루트 .env에 있는 값을 그대로 복사

kubectl --context kind-chulgunhaza-verify create secret generic app-env \
  --from-env-file=/tmp/verify.env.k8s
kubectl --context kind-chulgunhaza-verify apply -f k8s/

for svc in user-server attendance-server chatting-server frontend; do
  kubectl --context kind-chulgunhaza-verify set image deployment/$svc $svc=localhost/$svc:$GIT_SHA
done
```

`kubectl --context kind-chulgunhaza-verify get pods`로 확인 — **MySQL/Redis/
RabbitMQ가 뜨기 전에 3개 Spring 서비스 파드가 `Error`로 한두 번 재시작하는
건 정상이다**(Deployment가 자동 재시도, 인프라 준비되면 곧 `1/1 Running`으로
안정됨). 전부 `1/1 Running`이 되면 `open http://localhost:30080`으로 접속해서
확인한다.

정리:

```bash
kind delete cluster --name chulgunhaza-verify
docker rmi localhost/user-server:$GIT_SHA localhost/attendance-server:$GIT_SHA \
  localhost/chatting-server:$GIT_SHA localhost/frontend:$GIT_SHA
rm -rf /tmp/verify.env.k8s /tmp/kind-config.yaml
```

## 트러블슈팅

| 증상 | 원인 / 조치 |
|---|---|
| `ErrImageNeverPull` | 이미지가 containerd `k8s.io` 네임스페이스에 없음 — `sudo ctr -n k8s.io images import`가 실패했는지 스크립트 출력 확인. sudoers 설정 빠졌을 가능성 큼 |
| Spring 서비스 파드가 처음에 `Error`/`CrashLoopBackOff` | MySQL/Redis/RabbitMQ가 아직 준비되기 전이라 정상 — `kubectl get pods`로 몇 초 뒤 재확인(자동 재시작됨) |
| `no match for platform in manifest` | `eclipse-temurin:*-alpine`은 amd64 전용이라 arm64(Apple Silicon Mac, arm64 VM 등)에서 못 씀 — `Containerfile`이 이미 멀티아키 태그(`17-jdk`/`17-jre`)를 쓰므로 정상이면 안 나야 함. 나온다면 레포가 최신인지(`git pull`) 확인 |
| VM/SSH 관련 명령이 전부 `Permission denied` | 키 기반 인증이 안 돼 있음 — "1. Mac ↔ VM SSH 키 설정" 단계부터 다시 확인. 비밀번호 인증만으로는 `vm-deploy.sh`가 원격 스크립트를 SSH 표준입력으로 흘려보내는 구조라 동작하지 않는다 |
| RabbitMQ 연결 실패 | `SPRING_RABBITMQ_HOST=rabbitmq`가 `.env.k8s`에 있는지, `app-env` Secret이 최신인지(`kubectl get secret app-env -o yaml`) 확인 |
| `sudo: a password is required` 로 스크립트가 멈춤 | sudoers 설정을 안 함 — 스크립트가 원격 스크립트 본문을 SSH 표준입력으로 흘려보내는 구조라 sudo 비밀번호 프롬프트에 답할 방법이 없음(필수 설정, 선택 아님) |
| 로그인은 되는데 채팅방 생성이 503 | chatting-server → user-server 내부 API(`USER_SERVER_INTERNAL_URL=http://user-server:8081`) 실패 — user-server 파드가 떠 있는지, Service명이 `user-server`가 맞는지 확인 |
| `up` 실행 중 SSH가 자꾸 끊기거나 응답이 없음, `uptime`의 load average가 수십~수백 | VM 메모리 부족 — "2. VM 쪽 최초 1회 준비"의 스왑 설정을 안 했을 가능성이 큼. `free -h`로 `Swap` 줄이 `0B`인지 확인하고 스왑부터 추가할 것(스왑 없이 RAM이 꽉 차면 OOM killer가 etcd/kube-apiserver를 죽여서 클러스터가 망가질 수 있음) |
| 재배포했는데 화면/동작이 그대로(옛 버전) | ~~예전엔 이미지 태그를 백엔드 레포 git sha 하나로만 매겼어서, 프론트만 바뀌고 백엔드가 안 바뀌면 태그가 그대로라 `kubectl set image`가 무변경으로 판단해 롤아웃 자체가 안 걸렸다~~ — 타임스탬프 태그로 고쳐져서 지금은 매번 새 태그가 나온다. 그래도 재현되면 `kubectl get deployment <서비스> -o jsonpath='{.spec.template.spec.containers[0].image}'`로 실제 적용된 태그를 확인 |

## 범위 밖

- Ingress controller 도입, TLS
- CI(GitHub Actions)에서 이 Containerfile로 이미지를 빌드해 VM에 자동
  배포하는 파이프라인 — 여기서 만든 Containerfile/k8s 매니페스트가 그
  기반이 됨, 실제 자동화는 별도 진행
- 여러 VM(멀티노드)으로 확장 — 지금은 단일 노드 기준
