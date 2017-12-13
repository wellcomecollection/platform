FROM wellcome/finatra_service_base

ADD target/universal/stage /opt/docker

USER root
RUN chown -R daemon:daemon /opt/docker

ENV project=reindexer

USER daemon
CMD ["/run.sh"]
