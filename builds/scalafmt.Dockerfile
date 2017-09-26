FROM pvansia/scala-sbt:0.13.13

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for running Scalafmt"

RUN apk update && apk add curl

# Installation instructions for scalafmt from
# http://scalameta.org/scalafmt/#Coursier
RUN curl --location --output coursier \
    https://github.com/alexarchambault/coursier/raw/master/coursier
RUN chmod +x coursier
RUN mv coursier /usr/local/bin
RUN coursier bootstrap com.geirsson:scalafmt-cli_2.12:1.2.0 \
  -o /usr/local/bin/scalafmt --standalone --main org.scalafmt.cli.Cli

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["sbt", "scalafmt"]
