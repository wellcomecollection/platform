FROM anapsix/alpine-java

RUN apk update && \
    apk add python3 && \
    pip3 install awscli && \
    rm -rf /var/cache/apk

COPY run.sh /app/run.sh
COPY notify.sh /app/notify.sh

ENTRYPOINT /app/run.sh