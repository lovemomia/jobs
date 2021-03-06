#!/bin/bash

JAVA_OPTS="-Xms256M -Xmx256M -Xmn64M \
	-XX:PermSize=32M -XX:MaxPermSize=32M \
	-XX:+UseConcMarkSweepGC -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=85 \
	-XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+DisableExplicitGC \
	-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=7777"
