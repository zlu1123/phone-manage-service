#!/bin/bash

# ============================================
# 健康检查脚本 — 定期检测服务是否假死
# 用法: */5 * * * * /home/project-file/healthcheck.sh >> /home/project-file/logs/healthcheck.log 2>&1
# ============================================

CHECK_URL="http://localhost:8090/captchaImage"
CHECK_TIMEOUT=15
MAX_FAILURES=3
FAILURE_FILE="/tmp/healthcheck_failures"
RESTART_SCRIPT="/home/project-file/runserver.sh"
LOG_PREFIX="[$(date '+%Y-%m-%d %H:%M:%S')]"

# ---------- 1. 检查进程是否存在 ----------
PID=$(ps aux | grep 'ruoyi-admin.jar' | grep -v grep | awk '{print $2}')

if [ -z "$PID" ]; then
    echo "${LOG_PREFIX} ❌ 进程不存在，直接重启..."
    bash ${RESTART_SCRIPT}
    exit 0
fi

# ---------- 2. 检查 HTTP 接口是否响应 ----------
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time ${CHECK_TIMEOUT} -k "${CHECK_URL}" 2>/dev/null)

if [ "$HTTP_CODE" = "200" ]; then
    echo "${LOG_PREFIX} ✅ 服务正常 (HTTP ${HTTP_CODE})"
    # 成功后清零失败计数
    rm -f ${FAILURE_FILE}
    exit 0
fi

# ---------- 3. 请求失败，累积计数 ----------
if [ -f ${FAILURE_FILE} ]; then
    FAIL_COUNT=$(cat ${FAILURE_FILE})
else
    FAIL_COUNT=0
fi

FAIL_COUNT=$((FAIL_COUNT + 1))
echo "${FAIL_COUNT}" > ${FAILURE_FILE}

echo "${LOG_PREFIX} ⚠️ 第 ${FAIL_COUNT}/${MAX_FAILURES} 次检测失败 (HTTP: ${HTTP_CODE:-timeout}, PID: ${PID})"

# ---------- 4. 连续失败达到阈值，执行重启 ----------
if [ ${FAIL_COUNT} -ge ${MAX_FAILURES} ]; then
    echo "${LOG_PREFIX} 🔄 连续 ${MAX_FAILURES} 次检测失败，开始重启..."

    # 4a. 发送告警（可选，取消注释即可）
    # curl -X POST -H "Content-Type: application/json" \
    #     -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"⚠️ ybxz.top 服务假死，正在自动重启...\"}}" \
    #     "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_KEY"

    # 4b. 保存当前进程的线程堆栈（用于事后排查）
    echo "${LOG_PREFIX} 📋 保存线程堆栈到 /home/project-file/logs/threaddump_$(date +%Y%m%d_%H%M%S).txt"
    jstack ${PID} > /home/project-file/logs/threaddump_$(date +%Y%m%d_%H%M%S).txt 2>&1 || true

    # 4c. 保存内存 dump（可选的，文件较大）
    # jmap -dump:live,format=b,file=/home/project-file/logs/heapdump_$(date +%Y%m%d_%H%M%S).hprof ${PID}

    # 4d. 杀掉旧进程
    echo "${LOG_PREFIX} 🔪 结束旧进程 PID=${PID}"
    kill ${PID}
    sleep 5

    # 确保进程被杀死
    if ps -p ${PID} > /dev/null 2>&1; then
        echo "${LOG_PREFIX} 🔪 强制结束 PID=${PID}"
        kill -9 ${PID}
        sleep 2
    fi

    # 4e. 启动新进程
    echo "${LOG_PREFIX} 🚀 启动新进程..."
    bash ${RESTART_SCRIPT}

    # 4f. 等待启动完成
    sleep 15

    # 4g. 验证新服务
    NEW_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time ${CHECK_TIMEOUT} -k "${CHECK_URL}" 2>/dev/null)
    if [ "$NEW_CODE" = "200" ]; then
        echo "${LOG_PREFIX} ✅ 重启成功，服务恢复正常"
        rm -f ${FAILURE_FILE}
    else
        echo "${LOG_PREFIX} ❌ 重启后仍有问题 (HTTP: ${NEW_CODE})"
    fi
fi
