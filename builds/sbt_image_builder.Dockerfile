FROM java:openjdk-8-jdk-alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for building sbt-based Docker images for the Wellcome Digital Platform"

# Based on https://bitbucket.org/iflavours/sbt-openjdk-8-alpine
# but with our version of sbt.  Also, this one actually works.
ENV SBT_VERSION 0.13.13
ENV SBT_HOME /usr/local/sbt
ENV PATH=${PATH}:${SBT_HOME}/bin
ENV SBT_JAR http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz

RUN apk update
RUN apk add bash findutils tar wget
RUN wget ${SBT_JAR} -O sbt-$SBT_VERSION.tgz
RUN tar -xf sbt-$SBT_VERSION.tgz -C /usr/local --strip-components 1
RUN sbt -verbose sbt-version

RUN apk add docker git python3
RUN pip3 install docopt

COPY . /builds

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["python3", "/builds/build_sbt_image.py"]
