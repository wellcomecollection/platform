FROM node:alpine

VOLUME ["/data"]

RUN npm install -g jshint

COPY run.sh /app/run.sh

ENTRYPOINT /app/run.sh