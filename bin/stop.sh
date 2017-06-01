#!/bin/bash
# 
# author by yubing
# 后台服务关闭shell脚本
# 这里的服务是指不需要web等容器加载的,直接java命令关闭
# 
#
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
source $DEPLOY_DIR/bin/params.sh

echo "begin stopping $SERVER_MAIN"

#找到服务对应的pid来停掉,根据服务名或者端口号来找都可以吧
PID=`ps -ef|grep "$SERVER_MAIN"|grep -v "grep"|awk '{print $2}'|head -n 1`

if [ -z "$PID" ]; then
    echo "ERROR: The $SERVER_MAIN does not started!"
    exit 1
fi

echo "Find server name[$SERVER_MAIN] macth PID is:$PID"

echo -e "Stopping the $SERVER_MAIN ..."

#kill target pid
kill -9 $PID

sleep 2

echo "Stopped $SERVER_MAIN [$PID] Done!!!"

