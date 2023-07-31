FROM ghcr.io/navikt/baseimages/temurin:17
COPY build/libs/dittnav-brukernotifikasjonbestiller-all.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/oom-dump.hprof"
ENV PORT=8080
EXPOSE $PORT
