FROM wellcome/finatra_service_base

ADD target/universal/stage /opt/docker

USER root
RUN chown -R daemon:daemon /opt/docker

ENV project=sierra_bibs_to_dynamo

USER daemon
CMD ["/run.sh"]
