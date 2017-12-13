FROM finatra_service_base:latest

ADD target/universal/stage /opt/docker

USER root
RUN chown -R daemon:daemon /opt/docker

ENV project=id_minter

USER daemon
CMD ["/run.sh"]