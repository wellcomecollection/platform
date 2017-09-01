FROM alpine

RUN apk update
RUN apk add bash python3 exiftool
RUN pip3 install boto3 docopt awscli

RUN mkdir /failed_tasks

COPY run.py /run.py
