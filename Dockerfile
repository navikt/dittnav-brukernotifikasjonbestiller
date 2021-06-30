FROM navikt/java:15-appdynamics
COPY init.sh /init-scripts/init.sh
COPY build/libs/dittnav-brukernotifikasjonbestiller-all.jar /app/app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/oom-dump.hprof"
ENV PORT=8080
EXPOSE $PORT

USER root
RUN apt-get install curl
USER apprunner