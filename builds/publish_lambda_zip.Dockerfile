FROM alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for publishing AWS Lambda ZIPs to S3"

RUN apk update
RUN apk add git python3
RUN pip3 install boto3 docopt

COPY publish_lambda_zip.py /publish_lambda_zip.py
COPY tooling.py /tooling.py

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["python3", "/publish_lambda_zip.py"]
