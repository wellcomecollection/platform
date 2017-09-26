FROM pvansia/scala-sbt:0.13.13

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for testing our sbt projects"

RUN apk update && apk add docker py-pip
RUN pip install docker-compose

COPY . /builds

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["sbt"]
