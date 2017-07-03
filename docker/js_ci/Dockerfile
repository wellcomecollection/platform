FROM alpine:3.3

VOLUME ["/data"]

RUN apk add --no-cache --virtual .run-deps nodejs \
 && npm install -g jshint

COPY run.sh /app/run.sh

ENTRYPOINT /app/run.sh