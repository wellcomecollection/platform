FROM python:3-alpine

RUN pip3 install boto3 pyhcl

WORKDIR /terraform

ENTRYPOINT ["/terraform/switch_api_pins.py"]
