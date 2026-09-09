#!/usr/bin/env bash
#
# kubeadm 단일 노드(컨트롤플레인=워커) VM 클러스터에 전체 스택
# (user/attendance/chatting-server + 프론트 + MySQL/Redis/RabbitMQ)을
# 한 번에 배포하는 스크립트. Mac에는 이 VM의 kubeconfig가 없다는 전제라,
# 실제 빌드/배포 로직은 전부 SSH로 VM 안에 들어가서 실행한다(git pull →
# podman build → containerd import → kubectl apply).
#
# ===== 사전 준비 (최초 1회, VM 안에서) =====
#   1. git, podman, kubectl이 설치돼 있고 kubectl이 이 kubeadm 클러스터를
#      가리키고 있어야 함(대부분 컨트롤플레인 노드에 기본으로 준비돼 있음)
#   2. `sudo ctr`를 비밀번호 없이 쓸 수 있어야 함(containerd에 이미지 import할
#      때 필요) — 이 스크립트는 원격 스크립트 본문을 SSH의 표준입력(heredoc)으로
#      흘려보내기 때문에, sudo가 비밀번호를 물어봐도 그 프롬프트에 입력할 방법이
#      없다(표준입력이 이미 스크립트 내용으로 채워져 있음). 즉 필수 사전 설정:
#        echo "$USER ALL=(ALL) NOPASSWD: /usr/bin/ctr" | sudo tee /etc/sudoers.d/ctr-nopasswd
#   3. chulgunhaza-backend 레포를 받아서 k8s/.env.k8s.example을
#      .env.k8s로 복사하고 실제 값(DB/JWT/RabbitMQ 등)을 채워야 함:
#        git clone https://github.com/chulgunhaza/chulgunhaza-backend.git
#        cd chulgunhaza-backend
#        cp k8s/.env.k8s.example .env.k8s && vim .env.k8s
#      (레포/프론트 레포는 최초 1회만 수동 clone 필요 — 이후엔 이 스크립트가
#      알아서 pull한다. .env.k8s는 git에 안 올라가므로 최초 1회는 반드시
#      VM에서 직접 채워야 함)
#
# ===== 아래 두 값만 본인 환경에 맞게 채우면 됨 =====
VM_HOST="user@vm-ip-or-hostname"   # ssh 접속 정보 (예: ubuntu@192.168.64.5)
VM_IP="vm-ip-or-hostname"          # 브라우저가 NodePort로 접속할 IP — 보통 VM_HOST와 같은 호스트
# ===================================================

set -euo pipefail

BACKEND_REMOTE="https://github.com/chulgunhaza/chulgunhaza-backend.git"
FRONTEND_REMOTE="https://github.com/chulgunhaza/chulgunhaza-frontend.git"

usage() {
  echo "usage: $0 {up|down|logs <service>|restart}" >&2
  exit 1
}

cmd="${1:-}"

case "$cmd" in
  up)
    ssh -t "$VM_HOST" bash -s -- "$VM_IP" "$BACKEND_REMOTE" "$FRONTEND_REMOTE" <<'REMOTE_SCRIPT'
set -euo pipefail

VM_IP="$1"
BACKEND_REMOTE="$2"
FRONTEND_REMOTE="$3"
REPO_DIR="$HOME/chulgunhaza-backend"
FRONTEND_DIR="$HOME/chulgunhaza-frontend"

sync_repo() {
  local dir="$1" remote="$2"
  if [ ! -d "$dir/.git" ]; then
    echo ">>> $dir 없음 — clone"
    git clone "$remote" "$dir"
  else
    echo ">>> $dir git pull(강제 동기화)"
    git -C "$dir" fetch origin
    git -C "$dir" reset --hard origin/main
  fi
}

sync_repo "$REPO_DIR" "$BACKEND_REMOTE"
sync_repo "$FRONTEND_DIR" "$FRONTEND_REMOTE"

if [ ! -f "$REPO_DIR/.env.k8s" ]; then
  echo "ERROR: $REPO_DIR/.env.k8s 가 없음." >&2
  echo "  cd $REPO_DIR && cp k8s/.env.k8s.example .env.k8s 로 만들고 값을 채운 뒤 다시 실행하세요." >&2
  exit 1
fi

GIT_SHA="$(git -C "$REPO_DIR" rev-parse --short HEAD)"
echo ">>> 이미지 태그: $GIT_SHA"

# 매 배포마다 태그를 새로 줘서(git short-sha) kubelet이 이미지를 확실히
# 갱신하게 한다 — :latest 고정 태그는 캐시돼서 재배포해도 안 바뀔 수 있음.
build_and_import() {
  local name="$1" containerfile="$2" context="$3"
  shift 3
  echo ">>> $name 이미지 빌드"
  podman build -t "localhost/${name}:${GIT_SHA}" -f "$containerfile" "$@" "$context"
  podman save "localhost/${name}:${GIT_SHA}" -o "/tmp/${name}.tar"
  echo ">>> $name 이미지를 containerd로 import"
  sudo ctr -n k8s.io images import "/tmp/${name}.tar"
  rm -f "/tmp/${name}.tar"
}

build_and_import user-server "$REPO_DIR/user-server/Containerfile" "$REPO_DIR"
build_and_import attendance-server "$REPO_DIR/attendance-server/Containerfile" "$REPO_DIR"
build_and_import chatting-server "$REPO_DIR/chatting-server/Containerfile" "$REPO_DIR"
# 포트포워딩 없이 VM IP로 직접 접근하기로 했으므로, 프론트 정적 빌드에 VM IP
# 기준 API/WS 주소를 굽는다(런타임 주입이 안 되는 Vite 정적 빌드라 빌드 타임에
# 확정해야 함 — src/api/client.ts, src/hooks/useChatSocket.ts 참고).
build_and_import frontend "$FRONTEND_DIR/Containerfile" "$FRONTEND_DIR" \
  --build-arg "VITE_API_BASE_URL=http://${VM_IP}:30081" \
  --build-arg "VITE_ATTENDANCE_API_BASE_URL=http://${VM_IP}:30082" \
  --build-arg "VITE_CHATTING_API_BASE_URL=http://${VM_IP}:30083" \
  --build-arg "VITE_WS_URL=ws://${VM_IP}:30083/websocket"

echo ">>> app-env Secret 갱신"
kubectl create secret generic app-env \
  --from-env-file="$REPO_DIR/.env.k8s" \
  --dry-run=client -o yaml | kubectl apply -f -

echo ">>> k8s 매니페스트 적용"
kubectl apply -f "$REPO_DIR/k8s/"

echo ">>> 새로 빌드한 이미지로 배포 갱신"
for svc in user-server attendance-server chatting-server frontend; do
  kubectl set image "deployment/${svc}" "${svc}=localhost/${svc}:${GIT_SHA}"
done

echo ">>> 롤아웃 대기"
for svc in mysql redis rabbitmq user-server attendance-server chatting-server frontend; do
  kubectl rollout status "deployment/${svc}" --timeout=180s
done

cat <<SUMMARY

=== 배포 완료 ===
프론트:            http://${VM_IP}:30080
user-server:       http://${VM_IP}:30081
attendance-server: http://${VM_IP}:30082
chatting-server:   http://${VM_IP}:30083
시드 로그인 계정:   test@chulgunhaza.com / test1234!
SUMMARY
REMOTE_SCRIPT
    ;;

  down)
    ssh -t "$VM_HOST" "kubectl delete -f ~/chulgunhaza-backend/k8s/ --ignore-not-found"
    ;;

  restart)
    "$0" down
    "$0" up
    ;;

  logs)
    svc="${2:-}"
    if [ -z "$svc" ]; then
      echo "usage: $0 logs <service>  (예: user-server, attendance-server, chatting-server, frontend, mysql, redis, rabbitmq)" >&2
      exit 1
    fi
    ssh -t "$VM_HOST" "kubectl logs -f deployment/$svc"
    ;;

  *)
    usage
    ;;
esac
