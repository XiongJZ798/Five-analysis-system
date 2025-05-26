#!/bin/bash

java -server \
-Xms4096m \
-Xmx8192m \
-XX:MetaspaceSize=512m \
-XX:MaxMetaspaceSize=512m \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:ParallelGCThreads=8 \
-XX:ConcGCThreads=2 \
-XX:+UseStringDeduplication \
-XX:+AggressiveOpts \
-XX:+UseLargePages \
-XX:+UseCompressedOops \
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-Xloggc:logs/gc.log \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=logs/heap_dump.hprof \
-Dserver.tomcat.max-threads=1000 \
-Dserver.tomcat.max-connections=30000 \
-jar your-application.jar