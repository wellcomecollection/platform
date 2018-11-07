FROM anapsix/alpine-java

RUN apk add --update gettext && rm -rf /var/cache/apk

RUN mkdir -p /opt/docker
ADD target/universal/stage /opt/docker

EXPOSE 8888

COPY run.sh /run.sh

ENTRYPOINT ["/run.sh"]
