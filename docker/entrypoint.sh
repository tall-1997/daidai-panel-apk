#!/bin/sh
# daidai-panel 容器入口脚本
# 兼容飞牛 OS / 群晖 / 绿联 / unRAID 等第三方 NAS 部署场景。

set -e

DATA_DIR=${DATA_DIR:-/app/Dumb-Panel}
SERVER_PID_FILE="${DATA_DIR}/run/daidai-server.pid"
PANEL_PORT=${PANEL_PORT:-5700}
APP_CONFIG_FILE=${APP_CONFIG_FILE:-/app/config.yaml}

log() {
  printf '[entrypoint] %s\n' "$*"
}

fail() {
  printf '[entrypoint][ERROR] %s\n' "$*" >&2
  exit 1
}

# --- 数据目录初始化 -----------------------------------------------------------
mkdir -p \
  "${DATA_DIR}/scripts" \
  "${DATA_DIR}/logs" \
  "${DATA_DIR}/backups" \
  "${DATA_DIR}/run" \
  "${DATA_DIR}/deps/nodejs" \
  "${DATA_DIR}/deps/python"
mkdir -p /tmp
chmod 1777 /tmp

# --- PUID/PGID 支持（LinuxServer.io 风格，opt-in） ---------------------------
# 飞牛 OS / 群晖等 NAS 用户通常需要让容器以宿主机用户跑，方便 SMB/NFS 共享。
# 仅当显式传入 PUID 才切换用户；保持对历史部署（默认 root）的兼容。
RUN_AS_USER=""
if [ -n "${PUID}" ] || [ -n "${PGID}" ]; then
  TARGET_UID=${PUID:-0}
  TARGET_GID=${PGID:-${TARGET_UID}}

  if ! command -v su-exec >/dev/null 2>&1 && ! command -v gosu >/dev/null 2>&1; then
    log "未找到 su-exec/gosu，PUID/PGID 设置已忽略（继续以 root 运行）"
  else
    if command -v addgroup >/dev/null 2>&1; then
      if ! getent group daidai >/dev/null 2>&1; then
        addgroup -g "${TARGET_GID}" daidai 2>/dev/null || groupadd -g "${TARGET_GID}" daidai
      fi
    else
      groupadd -g "${TARGET_GID}" daidai 2>/dev/null || true
    fi

    if command -v adduser >/dev/null 2>&1; then
      if ! id -u daidai >/dev/null 2>&1; then
        adduser -D -H -u "${TARGET_UID}" -G daidai daidai 2>/dev/null || \
          useradd -M -u "${TARGET_UID}" -g "${TARGET_GID}" -s /sbin/nologin daidai
      fi
    else
      useradd -M -u "${TARGET_UID}" -g "${TARGET_GID}" -s /sbin/nologin daidai 2>/dev/null || true
    fi

    log "应用 PUID=${TARGET_UID} PGID=${TARGET_GID}，正在调整数据目录所有权..."
    chown -R "${TARGET_UID}:${TARGET_GID}" "${DATA_DIR}" /tmp 2>/dev/null || true
    RUN_AS_USER="daidai"
  fi
fi

# --- 数据目录可写性预检 -----------------------------------------------------
WRITE_PROBE="${DATA_DIR}/.daidai-write-probe-$$"
PROBE_CMD="true"
if [ -n "${RUN_AS_USER}" ]; then
  if command -v su-exec >/dev/null 2>&1; then
    PROBE_CMD="su-exec ${RUN_AS_USER} touch ${WRITE_PROBE}"
  elif command -v gosu >/dev/null 2>&1; then
    PROBE_CMD="gosu ${RUN_AS_USER} touch ${WRITE_PROBE}"
  fi
else
  PROBE_CMD="touch ${WRITE_PROBE}"
fi

if ! sh -c "${PROBE_CMD}" 2>/dev/null; then
  log "数据目录 ${DATA_DIR} 不可写。常见原因："
  log "  1) NAS 上挂载的宿主机目录所有权与容器用户不匹配。"
  log "     在宿主机执行：sudo chown -R \$(id -u):\$(id -g) <挂载点>，或在 compose 里设置 PUID/PGID。"
  log "  2) SELinux/AppArmor 拒绝写入。"
  log "  3) 只读卷挂载（compose 配置中 :ro 标志）。"
  fail "数据目录可写性预检失败，启动中止。"
fi
rm -f "${WRITE_PROBE}" 2>/dev/null || true

# --- PATH / NODE_PATH ------------------------------------------------------
export NODE_PATH="${DATA_DIR}/deps/nodejs/node_modules"
export PATH="${DATA_DIR}/deps/nodejs/node_modules/.bin:${DATA_DIR}/deps/python/venv/bin:${PATH}"

if [ -d "${DATA_DIR}/deps/python/venv" ]; then
  PY_MINOR=$(python3 -c 'import sys;print(f"{sys.version_info.minor}")' 2>/dev/null || echo "")
  if [ -n "${PY_MINOR}" ]; then
    export PYTHONPATH="${DATA_DIR}/deps/python/venv/lib/python3.${PY_MINOR}/site-packages"
  fi
fi

# 清理可能与面板内部 pip 调用冲突的环境变量（与代码侧 SanitizePipEnv 对称，双保险）。
# 用户在 docker run -e / systemd Environment= 中预设的 PIP_PREFIX 等会触发
# "Cannot set --home and --prefix together" 等冲突。
unset PIP_PREFIX PIP_HOME PIP_TARGET PIP_ROOT PIP_USER PIP_INSTALL_OPTION PYTHONUSERBASE

# --- nginx 监听端口替换 ----------------------------------------------------
NGINX_CONF_PATH=${NGINX_DEFAULT_CONF:-}
if [ -z "${NGINX_CONF_PATH}" ]; then
  for candidate in /etc/nginx/http.d/default.conf /etc/nginx/conf.d/default.conf /etc/nginx/sites-enabled/default; do
    if [ -f "${candidate}" ]; then
      NGINX_CONF_PATH="${candidate}"
      break
    fi
  done
fi

if [ -n "${NGINX_CONF_PATH}" ] && [ -f "${NGINX_CONF_PATH}" ]; then
  sed -i "s/listen [0-9]*/listen ${PANEL_PORT}/" "${NGINX_CONF_PATH}"
fi

# --- config.yaml 幂等生成 --------------------------------------------------
# 历史背景：v2.2.5 及更早的 entrypoint 每次启动都会用 heredoc 覆盖 config.yaml，
# 导致用户在面板里改过的 CORS / 信任代理 / JWT 过期时间等被强制丢失。
#
# v2.2.6 改成幂等，但镜像内置的 /app/config.yaml（Dockerfile COPY server/config.yaml
# 进去的）用的是相对路径 `./data/daidai.db`、`./data`。如果不识别这种"未初始化的
# 默认占位配置"，幂等逻辑会保留它，导致面板读到错误的相对路径数据库（cwd 下的
# /app/data/daidai.db），新建空 DB，旧数据（/app/Dumb-Panel/daidai.db）找不到，
# 表现为"面板刚装好一样的初始化界面"。
#
# 修复：判断"文件不存在"或"内容仍是镜像默认占位（含 ./data/ 相对路径）"才重写。
# 用户已经定制过的配置（用绝对路径或别的 data dir）不会被误覆盖。
build_cors_origins_yaml() {
  # CORS_ORIGINS 支持逗号/换行/空格分隔，例如：
  #   CORS_ORIGINS=https://nas.example.com,http://192.168.1.10:5700
  default_lines="    - http://localhost:5173
    - http://localhost:${PANEL_PORT}"
  user_input=${CORS_ORIGINS:-}
  if [ -z "${user_input}" ]; then
    printf '%s\n' "${default_lines}"
    return
  fi

  printf '%s\n' "${default_lines}"
  printf '%s' "${user_input}" | tr ',\n' '  ' | tr -s ' ' '\n' | while IFS= read -r origin; do
    origin=$(printf '%s' "${origin}" | sed 's/^[[:space:]]*//; s/[[:space:]]*$//')
    [ -z "${origin}" ] && continue
    printf '    - %s\n' "${origin}"
  done
}

config_looks_like_image_default() {
  # 判断 config.yaml 是否仍是 server/config.yaml 那一份"未初始化的占位配置"。
  # 占位特征：data.dir / database.path 是 ./data/* 相对路径。任何用户实际跑过的
  # 容器都被 entrypoint 改写成 ${DATA_DIR}/... 绝对路径，所以这两条特征不会误伤。
  [ -f "$1" ] || return 1
  if grep -Eq '^[[:space:]]*path:[[:space:]]*\./data/daidai\.db' "$1" 2>/dev/null; then
    return 0
  fi
  if grep -Eq '^[[:space:]]*dir:[[:space:]]*\./data[[:space:]]*$' "$1" 2>/dev/null; then
    return 0
  fi
  return 1
}

NEEDS_REGENERATE=0
if [ ! -f "${APP_CONFIG_FILE}" ]; then
  NEEDS_REGENERATE=1
  log "首次启动，生成默认配置：${APP_CONFIG_FILE}"
elif config_looks_like_image_default "${APP_CONFIG_FILE}"; then
  NEEDS_REGENERATE=1
  log "检测到 ${APP_CONFIG_FILE} 仍是镜像默认占位（./data/ 相对路径），重写为绝对路径以恢复数据访问"
else
  log "检测到已有配置：${APP_CONFIG_FILE}，跳过覆盖（保留用户自定义）"
fi

if [ "${NEEDS_REGENERATE}" = "1" ]; then
  CORS_BLOCK=$(build_cors_origins_yaml)
  cat > "${APP_CONFIG_FILE}" <<YAML
server:
  port: 5701
  mode: release

database:
  path: ${DATA_DIR}/daidai.db

jwt:
  secret: ""
  access_token_expire: 480h
  refresh_token_expire: 1440h

data:
  dir: ${DATA_DIR}
  scripts_dir: ${DATA_DIR}/scripts
  log_dir: ${DATA_DIR}/logs

cors:
  origins:
${CORS_BLOCK}
YAML
fi

# --- 自定义 ENTRYPOINT 透传 -------------------------------------------------
if [ $# -gt 0 ]; then
  exec "$@"
fi

# --- 启动 nginx + daidai-server ---------------------------------------------
nginx

shutdown() {
  if [ -n "${SERVER_PID:-}" ]; then
    kill "${SERVER_PID}" 2>/dev/null || true
  fi
  rm -f "${SERVER_PID_FILE}"
  exit 0
}
trap shutdown TERM INT

while true; do
  if [ -n "${RUN_AS_USER}" ] && command -v su-exec >/dev/null 2>&1; then
    su-exec "${RUN_AS_USER}" /app/daidai-server &
  elif [ -n "${RUN_AS_USER}" ] && command -v gosu >/dev/null 2>&1; then
    gosu "${RUN_AS_USER}" /app/daidai-server &
  else
    /app/daidai-server &
  fi
  SERVER_PID=$!
  echo "${SERVER_PID}" > "${SERVER_PID_FILE}"
  # 关闭 set -e 包住 wait：server 异常退出时仍要走重启循环，不能让 set -e 把脚本带出。
  set +e
  wait "${SERVER_PID}"
  EXIT_CODE=$?
  set -e
  rm -f "${SERVER_PID_FILE}"
  [ ${EXIT_CODE} -eq 0 ] && exit 0
  log "daidai-server 异常退出 (code=${EXIT_CODE})，2 秒后重启"
  sleep 2
done
