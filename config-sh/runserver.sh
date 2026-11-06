
#!/bin/bash

APP_NAME="ruoyi-admin"
JAR_PATH="/home/project-file/ruoyi-admin.jar"
LOG_DIR="/home/project-file/logs"
LOG_FILE="/home/project-file/app.log"

# ---------- 1. 先停旧进程 ----------
OLD_PID=$(ps aux | grep 'ruoyi-admin.jar' | grep -v grep | awk '{print $2}')
if [ -n "$OLD_PID" ]; then
    echo "发现旧进程 PID=${OLD_PID}，正在停止..."
    kill ${OLD_PID}
    sleep 3
    if ps -p ${OLD_PID} > /dev/null 2>&1; then
        echo "强制结束 PID=${OLD_PID}"
        kill -9 ${OLD_PID}
        sleep 2
    fi
    echo "旧进程已停止"
else
    echo "没有发现旧进程"
fi

# ---------- 2. 启动服务 ----------
echo "正在启动 ${APP_NAME}..."

nohup java \
    -Duser.timezone=Asia/Shanghai \
    -Xms512m \
    -Xmx1024m \
    -XX:MetaspaceSize=128m \
    -XX:MaxMetaspaceSize=256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:G1HeapRegionSize=16m \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -XX:+PrintGCTimeStamps \
    -XX:+PrintHeapAtGC \
    -XX:+PrintGCApplicationStoppedTime \
    -Xloggc:${LOG_DIR}/gc.log \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=10 \
    -XX:GCLogFileSize=50M \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -jar ${JAR_PATH} \
    > ${LOG_FILE} 2>&1 &

PID=$!
echo "启动完成，PID: ${PID}"
echo "日志文件: ${LOG_FILE}"
echo "GC 日志: ${LOG_DIR}/gc.log"
echo "Heap Dump 路径: ${LOG_DIR}/heapdump.hprof"
