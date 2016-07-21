#!/bin/bash

ppid=$(ps aux | grep 'waazdoh.client.app.AppLauncher' | grep -v grep | awk '{print $2}')
if [ ! -z "$ppid" ]; then
	echo kill ${ppid}
	kill ${ppid}
fi

java -XX:+HeapDumpOnOutOfMemoryError -Dservice.url=http://192.168.33.20:8080/service -cp target/client-1.5.2-SNAPSHOT-jar-with-dependencies.jar waazdoh.client.app.AppLauncher dev > log.log 2>error.log &
