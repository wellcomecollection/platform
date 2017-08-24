FROM alpine

# Required for running git operations in the container
RUN apk add --update git bash openssh

RUN apk --no-cache update && \
    apk --no-cache add findutils jq python3 ca-certificates curl groff less zip git && \
    pip3 --no-cache-dir install boto3 awscli aws_batch_helper && \
    rm -rf /var/cache/apk/*

# Install terraform.  This is the last build step because it changes with
# new Terraform releases, and we can cache the layers above.
ENV TERRAFORM_VERSION=0.10.2
COPY install_terraform.sh /install_terraform.sh
RUN /install_terraform.sh

COPY is_up_to_date_with_master.py /app/is_up_to_date_with_master.py
COPY plan.sh /app/plan.sh
COPY run.sh /app/run.sh
COPY notify.sh /app/notify.sh

VOLUME ["/data"]
WORKDIR /data

ENTRYPOINT /app/run.sh
