#
# Scala and sbt Dockerfile
#
#

# Pull base image
FROM  ubuntu:yakkety

ENV SCALA_VERSION 2.11.8
ENV SBT_VERSION 0.13.13
ENV DOCKER_VERSION 17.06.0
ENV DOCKER_COMPOSE_VERSION 1.14.0

VOLUME ["/data", "/tmp/.ivy2"]

## Install JDK
RUN apt-get update && \
    apt-get install openjdk-8-jdk -y

## Install docker

# Install dependencies to add repos over HTTPS
RUN apt-get update && \
    apt-get install \
        apt-transport-https \
        ca-certificates \
        software-properties-common \
        curl -y

# Add Docker’s official GPG key
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add -

# Add Docker stable repo
RUN add-apt-repository \
       "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
       $(lsb_release -cs) \
       stable"

# Remove old versions of docker, add deps & docker-ce
RUN apt-get remove docker docker-engine docker.io && \
    apt-get update && \
    apt-get install docker-ce=$DOCKER_VERSION~ce-0~ubuntu -y

RUN curl -L https://github.com/docker/compose/releases/download/$DOCKER_COMPOSE_VERSION/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose && \
    chmod +x /usr/local/bin/docker-compose

## Install Scala & sbt

# Scala expects this file
RUN touch /usr/lib/jvm/java-8-openjdk-amd64/release

# Install Scala
## Piping curl directly in tar
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo 'export PATH=~/scala-$SCALA_VERSION/bin:$PATH' >> /root/.bashrc

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  apt-get update && \
  apt-get install sbt -y && \
  sbt sbtVersion

# Install requird python deps
RUN apt-get install python3-pip && \
    pip3 install --upgrade boto3 docker docopt


## Copy required scripts and run!

# Copy python scripts
COPY scripts /scripts

# Add run script
COPY run.sh /app/run.sh

ENTRYPOINT /app/run.sh