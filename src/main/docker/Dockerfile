FROM oraclelinux:7-slim
FROM openjdk:8-jre-slim

ENV http_proxy=http://www-proxy.us.oracle.com:80
ENV https_proxy=http://www-proxy.us.oracle.com:80
ENV psr_mount="http://adc4110092.us.oracle.com/download/helidonArchive"
#ENV JAVA_HOME=/u01/jdk

RUN mkdir -p /u01/jdk
RUN mkdir -p /app
WORKDIR /app
#COPY jdk1.8.0_40 /u01/jdk
COPY libs /app/libs
COPY psr_quickstart_mp.jar /app
#COPY start.sh /app
#RUN mkdir /scratch 
#RUN chmod 777 /app/start.sh
#CMD /app/start.sh
#CMD matchStr=$(cat /proc/sys/kernel/hostname);mkdir /scratch/$matchStr;/u01/jdk/bin/java -DpsrLoc="/scratch" -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -XX:FlightRecorderOptions=defaultrecording=true,settings=default,disk=true,repository=/scratch/$matchStr,maxage=30m,dumponexit=true,dumponexitpath=/scratch/$matchStr_testDump.jfr,loglevel=info -jar psr_quickstart_mp.jar
CMD matchStr=$(cat /proc/sys/kernel/hostname);mkdir /scratch/$matchStr;java -DpsrLoc="/scratch"  -jar psr_quickstart_mp.jar