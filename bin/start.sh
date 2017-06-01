#!/bin/bash
# 
# 
# author by yubing
# 
#
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
#set confg dir
CONF_DIR=$DEPLOY_DIR/conf
source $DEPLOY_DIR/bin/params.sh
echo "current deploy-dir is : $DEPLOY_DIR"

#set env
ALL_ENV=("dev" "product")
if [ ! -n "$1" ] ;then  
    echo "you have not input a env!"
    echo ${ALL_ENV[@]}
    exit 1
fi 

ENV=$1
if echo "${ALL_ENV[@]}" | grep -v "$ENV" &>/dev/null; then
    echo "not in env"
    echo ${ALL_ENV[@]}
    exit 1 
fi

#set config file name
CONF_FILE=application-$ENV.properties

echo "now use config filename is : $CONF_FILE"

echo "start server use main class : $SERVER_MAIN"

#set server name
SERVER_NAME=$DEPLOY_DIR
#set server port
SERVER_PORT=`sed '/server.port/!d;s/.*=//' $CONF_DIR/$CONF_FILE | tr -d '\r'`

echo "init server name[$SERVER_NAME],server port[$SERVER_PORT]"

if [ -z "$SERVER_NAME" ]; then
	SERVER_NAME=`hostname`
fi

#check the program is started or not
PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$CONF_DIR" |awk '{print $2}'`
if [ -n "$PIDS" ]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

#check the port is used or not
if [ -n "$SERVER_PORT" ]; then
	SERVER_PORT_COUNT=`netstat -tln | grep $SERVER_PORT | wc -l`
	if [ $SERVER_PORT_COUNT -gt 0 ]; then
		echo "ERROR: The $SERVER_NAME port $SERVER_PORT already used!"
		exit 1
	fi
fi


#set the classpath
LIB_DIR=$DEPLOY_DIR/lib
LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`

#java start
JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Denv=$ENV"
#debug start,port=8000
JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
    JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
fi
#JMX listner
JAVA_JMX_OPTS=""
if [ "$2" = "jmx" ]; then
    JAVA_JMX_OPTS=" -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
fi

#set the java mem
JAVA_MEM_OPTS=""
BITS=`file $JAVA_HOME/bin/java | grep 64-bit`
if [ -n "$BITS" ]; then
    let memTotal=`cat /proc/meminfo |grep MemTotal|awk '{printf "%d", $2/1024 }'`
    if [ $memTotal -gt 2500 ];then
        JAVA_MEM_OPTS=" -server -Xmx1024m -Xms512m -Xmn128m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
    else 
        JAVA_MEM_OPTS=" -server -Xmx1024m -Xms512m -Xmn128m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
    fi
else
	JAVA_MEM_OPTS=" -server -Xms512m -Xmx1024m -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi

echo -e "Starting the $SERVER_NAME ...\c"
CLASSPATH=$CONF_DIR:$LIB_JARS
export CLASSPATH
nohup java $JAVA_OPTS $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_JMX_OPTS $SERVER_MAIN --spring.profiles.active=$ENV  >/dev/null 2>&1 &

COUNT=0
while [ $COUNT -lt 1 ]; do    
    echo -e ".\c"
    sleep 1 
#   if [ -n "$SERVER_PORT" ]; then
#   	COUNT=`echo status | nc 127.0.0.1 $SERVER_PORT -i 1 | grep -c OK`
#   else
    	COUNT=`ps  --no-heading -C java -f --width 1000 | grep "$SERVER_MAIN" | awk '{print $2}' | wc -l`
#  fi
	if [ $COUNT -gt 0 ]; then
		break
	fi
done
echo "OK!"
PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$SERVER_MAIN" | awk '{print $2}'`
echo "PID: $PIDS"

