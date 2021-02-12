FROM redboxoss/scuttle:latest AS scuttle
FROM navikt/java:15-appdynamics

COPY --from=scuttle /scuttle /bin/scuttle
COPY init.sh /init-scripts/init.sh
COPY build/libs/dittnav-brukernotifikasjonbestiller-all.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/oom-dump.hprof"
ENV ENVOY_ADMIN_API=http://127.0.0.1:15000
ENV ISTIO_QUIT_API=http://127.0.0.1:15020
ENV PORT=8080

EXPOSE $PORT

ENTRYPOINT ["scuttle", "/dumb-init", "--", "/entrypoint.sh"]
