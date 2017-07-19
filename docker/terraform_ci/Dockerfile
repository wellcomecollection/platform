FROM alpine

# Required for running git operations in the container
RUN apk add --update git bash openssh

RUN apk --no-cache update && \
    apk --no-cache add python py-pip py-setuptools ca-certificates curl groff less zip git && \
    pip --no-cache-dir install awscli && \
    rm -rf /var/cache/apk/*

# Install terraform.  This is the last build step because it changes with
# new Terraform releases, and we can cache the layers above.
ENV TERRAFORM_VERSION=0.9.11
COPY install_terraform.sh /install_terraform.sh
RUN /install_terraform.sh

COPY is_up_to_date_with_master.py /app/is_up_to_date_with_master.py
COPY plan.sh /app/plan.sh
COPY run.sh /app/run.sh

VOLUME ["/data"]
WORKDIR /data/terraform

ENTRYPOINT /app/run.sh
