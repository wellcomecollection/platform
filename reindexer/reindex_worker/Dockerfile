FROM anapsix/alpine-java

ADD target/universal/stage /opt/docker

RUN mkdir -p /opt/docker

EXPOSE 8888

ENTRYPOINT ["/opt/docker/bin/reindex_worker"]
