FROM anapsix/alpine-java:8

RUN mkdir -p /opt/docker
ADD target/universal/stage /opt/docker

USER root
RUN chown -R daemon:daemon /opt/docker

EXPOSE 9001

USER daemon
CMD ["/opt/docker/bin/notifier"]
