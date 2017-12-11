FROM anapsix/alpine-java

RUN apk update

RUN apk add --virtual build_deps gettext

ADD run.sh /run.sh

ADD target/universal/stage/ /opt/docker
RUN chown -R daemon:daemon /opt/docker
USER daemon

EXPOSE 8080

CMD ["/run.sh"]