# Gatling is a highly capable load testing tool.
#
# Documentation: http://gatling.io/docs/2.2.2/
# Cheat sheet: http://gatling.io/#/cheat-sheet/2.2.2
#
# Copied from https://hub.docker.com/r/denvazh/gatling/

FROM java:8-jdk-alpine

# working directory for gatling
WORKDIR /opt

# gating version
ENV GATLING_VERSION 2.2.4

# create directory for gatling install
RUN mkdir -p gatling

# install some base packages
RUN apk --no-cache update && \
    apk --no-cache add findutils jq wget bash python py-pip py-setuptools ca-certificates curl groff less zip git && \
    pip --no-cache-dir install awscli && \
    rm -rf /var/cache/apk/*

# install gatling
RUN mkdir -p /tmp/downloads && \
    wget -q -O /tmp/downloads/gatling-$GATLING_VERSION.zip \
      https://repo1.maven.org/maven2/io/gatling/highcharts/gatling-charts-highcharts-bundle/$GATLING_VERSION/gatling-charts-highcharts-bundle-$GATLING_VERSION-bundle.zip && \
      mkdir -p /tmp/archive && cd /tmp/archive && \
      unzip /tmp/downloads/gatling-$GATLING_VERSION.zip && \
      mv /tmp/archive/gatling-charts-highcharts-bundle-$GATLING_VERSION/* /opt/gatling/ && \
      rm -rf /tmp/*

# change context to gatling directory
WORKDIR  /opt/gatling

# set environment variables
ENV PATH /opt/gatling/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ENV GATLING_HOME /opt/gatling

COPY user-files /opt/gatling/user-files
COPY data /opt/gatling/data

COPY run.sh /opt/gatling/run.sh
COPY notify.sh /opt/gatling/notify.sh

ENTRYPOINT ["/opt/gatling/run.sh"]
