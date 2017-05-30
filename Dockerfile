FROM anapsix/alpine-java

ARG project

RUN apk update && apk add python3 && pip3 install awscli && rm -rf /var/cache/apk

ADD $project/target/universal/stage /opt/docker
RUN chown -R daemon:daemon /opt/docker
USER daemon

COPY run.sh /run.sh

EXPOSE 8888

ENV PROJECT $project

CMD ["/run.sh"]
