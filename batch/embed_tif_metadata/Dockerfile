FROM alpine

RUN apk update
RUN apk add awscli bash python3
RUN pip3 install boto3 docopt

COPY run.py /run.py
