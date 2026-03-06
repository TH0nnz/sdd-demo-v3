#!/usr/bin/env bash
set -euo pipefail

# One-command deployment to the target server using rsync + docker compose.
REMOTE_USER="${REMOTE_USER:-infoadmin}"
REMOTE_HOST="${REMOTE_HOST:-192.168.10.248}"
REMOTE_PATH="${REMOTE_PATH:-/home/infoadmin/sdd-demo-v3}"
SSH_PASSWORD="${SSH_PASSWORD:-}"
SUDO_PASSWORD="${SUDO_PASSWORD:-}"
COMPOSE_CMD="${COMPOSE_CMD:-docker compose}"
BUILD_FLAG="--build"
USE_SUDO="${USE_SUDO:-}"

usage() {
  cat <<'EOF'
Usage:
  ./deploy.sh [options]

Options:
  --host <ip-or-host>      Remote host, default: 192.168.10.248
  --user <username>        SSH user, default: infoadmin
  --password <password>    SSH password (requires sshpass)
  --remote-path <path>     Remote project path, default: /home/infoadmin/sdd-demo-v3
  --sudo                   Run docker compose with sudo on remote (for permission denied)
  --sudo-password <pwd>    Sudo password on remote (default: use --password if set)
  --no-build               Run docker compose up -d without --build
  -h, --help               Show this help

Environment variables (optional):
  REMOTE_HOST, REMOTE_USER, REMOTE_PATH, SSH_PASSWORD, SUDO_PASSWORD, COMPOSE_CMD, USE_SUDO
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      REMOTE_HOST="$2"
      shift 2
      ;;
    --user)
      REMOTE_USER="$2"
      shift 2
      ;;
    --password)
      SSH_PASSWORD="$2"
      shift 2
      ;;
    --remote-path)
      REMOTE_PATH="$2"
      shift 2
      ;;
    --sudo)
      USE_SUDO=1
      shift
      ;;
    --sudo-password)
      SUDO_PASSWORD="$2"
      shift 2
      ;;
    --no-build)
      BUILD_FLAG=""
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Error: unknown option '$1'" >&2
      usage
      exit 1
      ;;
  esac
done

# When --sudo and (--sudo-password or --password): use sudo -S so password is read from stdin (non-interactive)
if [[ -n "${USE_SUDO}" && -z "${SUDO_PASSWORD}" && -n "${SSH_PASSWORD}" ]]; then
  SUDO_PASSWORD="${SSH_PASSWORD}"
fi
if [[ -n "${USE_SUDO}" && -z "${SUDO_PASSWORD}" ]]; then
  COMPOSE_CMD="sudo ${COMPOSE_CMD}"
fi

SSH_TARGET="${REMOTE_USER}@${REMOTE_HOST}"

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Error: missing command '$1'" >&2
    exit 1
  fi
}

require_cmd rsync
require_cmd ssh

print_source_revision() {
  if ! command -v git >/dev/null 2>&1; then
    echo "[0/4] Source revision: git not found, skip commit info"
    return
  fi

  if ! git -C "${PROJECT_ROOT}" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "[0/4] Source revision: not a git worktree, skip commit info"
    return
  fi

  local branch
  local commit
  local dirty

  branch="$(git -C "${PROJECT_ROOT}" rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
  commit="$(git -C "${PROJECT_ROOT}" rev-parse --short HEAD 2>/dev/null || echo unknown)"

  if [[ -n "$(git -C "${PROJECT_ROOT}" status --porcelain 2>/dev/null)" ]]; then
    dirty="yes"
  else
    dirty="no"
  fi

  echo "[0/4] Source revision: branch=${branch}, commit=${commit}, dirty=${dirty}"
}

print_source_revision

SSH_OPTS="-o StrictHostKeyChecking=accept-new"

run_ssh() {
  if [[ -n "${SSH_PASSWORD}" ]]; then
    require_cmd sshpass
    sshpass -p "${SSH_PASSWORD}" ssh ${SSH_OPTS} "$@"
  else
    ssh ${SSH_OPTS} "$@"
  fi
}

run_rsync() {
  if [[ -n "${SSH_PASSWORD}" ]]; then
    require_cmd sshpass
    sshpass -p "${SSH_PASSWORD}" rsync -e "ssh ${SSH_OPTS}" "$@"
  else
    rsync -e "ssh ${SSH_OPTS}" "$@"
  fi
}

echo "[1/4] Ensure remote path exists: ${SSH_TARGET}:${REMOTE_PATH}"
run_ssh "${SSH_TARGET}" "mkdir -p '${REMOTE_PATH}'"

echo "[2/4] Sync project files"
run_rsync -avz --delete \
  --exclude '.git' \
  --exclude 'backend/build' \
  --exclude 'backend/.gradle' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  "${PROJECT_ROOT}/" "${SSH_TARGET}:${REMOTE_PATH}/"

UP_CMD="${COMPOSE_CMD} up -d"
if [[ -n "${BUILD_FLAG}" ]]; then
  UP_CMD+=" ${BUILD_FLAG}"
fi

echo "[3/4] Start services on remote"
if [[ -n "${USE_SUDO}" && -n "${SUDO_PASSWORD}" ]]; then
  printf '%s\n' "${SUDO_PASSWORD}" | run_ssh "${SSH_TARGET}" "set -e; cd '${REMOTE_PATH}' && sudo -S ${COMPOSE_CMD} up -d ${BUILD_FLAG}"
else
  run_ssh "${SSH_TARGET}" "set -e; cd '${REMOTE_PATH}'; ${UP_CMD}"
fi

echo "[4/4] Service status"
if [[ -n "${USE_SUDO}" && -n "${SUDO_PASSWORD}" ]]; then
  printf '%s\n' "${SUDO_PASSWORD}" | run_ssh "${SSH_TARGET}" "cd '${REMOTE_PATH}' && sudo -S ${COMPOSE_CMD} ps"
else
  run_ssh "${SSH_TARGET}" "cd '${REMOTE_PATH}' && ${COMPOSE_CMD} ps"
fi

echo "Deployment completed."
