FROM wellcome/finatra_service_base

ADD target/universal/stage /opt/docker

USER root
RUN chown -R daemon:daemon /opt/docker

ENV project=snapshot_generator

USER daemon
CMD ["/run.sh"]
