#!/usr/bin/env bash
#
# 새 워크트리(또는 새 클론)에는 git 이 추적하지 않는 로컬 설정 파일이 없다.
# 홈의 원본 디렉터리에서 없는 것만 채운다. 이미 있는 파일은 절대 덮어쓰지 않는다.
#
#   ~/.pickflow/local.properties      -> local.properties
#   ~/.pickflow/secrets.properties    -> secrets.properties
#   ~/.pickflow/google-services.json  -> app/google-services.json
#
# Orca 를 쓴다면 repo 설정의 setup hook 에 `./scripts/bootstrap-worktree.sh` 를 걸어두면
# 워크트리를 만들 때마다 자동으로 돈다. 아니면 새 워크트리에서 한 번 직접 실행한다.
# 원본 위치는 PICKFLOW_LOCAL_CONFIG_DIR 로 바꿀 수 있다.
set -euo pipefail

src="${PICKFLOW_LOCAL_CONFIG_DIR:-$HOME/.pickflow}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [ ! -d "$src" ]; then
    echo "bootstrap: 원본 디렉터리가 없습니다: $src" >&2
    echo "bootstrap: 기존 체크아웃에서 local.properties / secrets.properties /" >&2
    echo "           app/google-services.json 을 그 경로로 복사해 두세요." >&2
    exit 1
fi

# copy <원본 파일명> <저장소 기준 상대 경로>
copy() {
    local from="$src/$1" to="$root/$2"
    if [ ! -f "$from" ]; then
        echo "bootstrap: 원본 없음, 건너뜀 — $1"
        return
    fi
    if [ -f "$to" ]; then
        echo "bootstrap: 이미 있음, 유지 — $2"
        return
    fi
    mkdir -p "$(dirname "$to")"
    cp "$from" "$to"
    echo "bootstrap: 복사함 — $2"
}

copy local.properties     local.properties
copy secrets.properties   secrets.properties
copy google-services.json app/google-services.json
