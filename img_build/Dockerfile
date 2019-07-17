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
CMD java -jar indexupdater.jar 
