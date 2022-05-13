#!/bin/sh

daemon=false
appname=gserver

Survivor=2 Old=16 NewSize=$[Survivor*10] Xmx=$[NewSize+Old] #NewSize=Survivor*(1+1+8) Xmx=NewSize+Old
JVM_OPS="-Xmx${Xmx}m -Xms${Xmx}m -XX:NewSize=${NewSize}m -XX:MaxNewSize=${NewSize}m -XX:SurvivorRatio=8 -Xss228k"
JVM_OPS="$JVM_OPS -Djava.compiler=none -Dlogserver -DcontextName=${appname} -Dtoken=${token:-xlongwei} -DincludeCallerData=${includeCallerData:-true}"
# JVM_OPS="$JVM_OPS -Dlight-4j-config-dir=config -Dlogback.configurationFile=logback.xml"
JVM_OPS="$JVM_OPS -Duser.timezone=GMT+8 -Dcom.mysql.cj.disableAbandonedConnectionCleanup=true"
ENV_OPS="$ENV_OPS server.ioThreads=2 server.workerThreads=3"
# ENV_OPS="$ENV_OPS server.enableHttps=true server.httpsPort=8443"
ENV_OPS="$ENV_OPS id.orderBy=pinyin"
# ENV_OPS="$ENV_OPS rpc-router.registerService=true"
# ENV_OPS="$ENV_OPS server.enableRegistry=true STATUS_HOST_IP=api.xlongwei.com"
# ENV_OPS="$ENV_OPS PATH=/usr/java/jdk1.8.0_161/bin:$PATH"
version=3.0.1

usage(){
    echo "Usage: start.sh ( commands ... )"
    echo "commands: "
    echo "  status      check the running status"
    echo "  start       start $appname"
    echo "  stop        stop $appname"
    echo "  restart     stop && start"
    echo "  clean       clean target and dist"
    echo "  package     package gserver and service"
    echo "  dist        build dist"
}

status(){
    PIDS=`ps -ef | grep java | grep "$jarfile" |awk '{print $2}'`

	if [ -z "$PIDS" ]; then
	    echo "$appname is not running!"
	else
		for PID in $PIDS ; do
		    echo "$appname has pid: $PID!"
		done
	fi
}

stop(){
    PIDS=`ps -ef | grep java | grep "$jarfile" |awk '{print $2}'`

	if [ -z "$PIDS" ]; then
	    echo "$appname is not running!"
	else
		echo -e "Stopping $appname ..."
		for PID in $PIDS ; do
			echo -e "kill $PID"
		    kill $PID > /dev/null 2>&1
		done
		COUNT=0
		while [ $COUNT -lt 1 ]; do
		    echo -e ".\c"
		    sleep 1
		    COUNT=1
		    for PID in $PIDS ; do
		        PID_EXIST=`ps -f -p $PID | grep "$jarfile"`
		        if [ -n "$PID_EXIST" ]; then
		            COUNT=0
		            break
		        fi
		    done
		done
	fi
}

start(){
    dist=`pwd`/dist
    cp='.'
    for file in `ls $dist/*.jar`; do
    cp=$cp:$file
    done
    # echo $cp
    echo "starting $appname ..."
	JVM_OPS="-server -Djava.awt.headless=true $JVM_OPS"
	if [ "$daemon" = "true" ]; then
		env $ENV_OPS setsid java $JVM_OPS -cp $cp com.networknt.server.Server >> /dev/null 2>&1 &
	else
		env $ENV_OPS java $JVM_OPS  -cp $cp com.networknt.server.Server 2>&1
	fi
}

clean(){
    mvn clean -f service/pom.xml
    rm -rf dist
}

package(){
    mvn package -DskipTests -Dmaven.javadoc.skip=true -f gserver/pom.xml
    mvn package -DskipTests -Dmaven.javadoc.skip=true -f service/pom.xml
}

dist(){
    [ ! -e dist ] && mkdir dist
    cp gserver/target/gserver-${version}.jar dist
    for service in `ls service|grep -v [.]` ; do
    # echo $service
    cp service/$service/target/$service-${version}.jar dist
    done
    tar zcvf dist.tgz dist start.sh
}

if [ $# -eq 0 ]; then 
    usage
else
	case $1 in
	status) status ;;
	start) start ;;
	stop) stop ;;
    restart) stop && start ;;
	clean) clean ;;
	package) package ;;
	dist) dist ;;
	*) usage ;;
	esac
fi