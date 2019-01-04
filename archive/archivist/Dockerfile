FROM openjdk

RUN mkdir -p /opt/docker

EXPOSE 8888

ADD target/universal/stage /opt/docker

ENTRYPOINT ["/opt/docker/bin/archivist"]
