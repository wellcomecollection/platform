FROM miro_adapter
LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Docker image for running Miro adapter tests"

RUN pip3 install pytest

CMD ["py.test", "/miro_adapter"]
