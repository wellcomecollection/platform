FROM alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for deploying our Docker images to AWS"

RUN apk update
RUN apk add docker git python3

RUN pip3 install awscli docopt boto3

COPY publish_service_to_aws.py /builds/publish_service_to_aws.py
COPY tooling.py /builds/tooling.py

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["python3", "/builds/publish_service_to_aws.py"]
