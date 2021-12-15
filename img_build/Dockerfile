FROM openjdk:11-slim

USER root

WORKDIR /home/indexupdater
COPY tmp/indexupdater.jar /home/indexupdater

RUN cd /home/indexupdater && \
    chown -R 1001:0 /home/indexupdater && \
    chmod -R g+rw /home/indexupdater && \
    ls -la /home/indexupdater

USER 1001
EXPOSE 8080

#Log4j 2 CVE-2021-44228
ENV LOG4J_FORMAT_MSG_NO_LOOKUPS=true

CMD java -XX:MaxRAMPercentage=80.0 -jar indexupdater.jar
