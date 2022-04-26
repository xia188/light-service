
FROM openjdk:8-jre-alpine
#EXPOSE  8083
ADD /target/gserver-3.0.1.jar server.jar
CMD ["/bin/sh","-c","java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:MaxRAMFraction=1 -Dlight-4j-config-dir=/config -Dlogback.configurationFile=/config/logback.xml -cp /server.jar:/service/* com.networknt.server.Server"]
