FROM golang:alpine

ENV TERRAFORM_VERSION=0.9.11

RUN apk add --update git bash openssh

ENV TF_DEV=true

WORKDIR $GOPATH/src/github.com/hashicorp/terraform

RUN git clone https://github.com/hashicorp/terraform.git ./ && \
    git checkout v${TERRAFORM_VERSION} && \
    /bin/bash scripts/build.sh

VOLUME ["/data"]

RUN apk --no-cache update && \
    apk --no-cache add bash python py-pip py-setuptools ca-certificates curl groff less zip git && \
    pip --no-cache-dir install awscli && \
    rm -rf /var/cache/apk/*

COPY is_up_to_date_with_master.py /app/is_up_to_date_with_master.py
COPY plan.sh /app/plan.sh
COPY run.sh /app/run.sh

WORKDIR /data/terraform

ENTRYPOINT /app/run.sh
