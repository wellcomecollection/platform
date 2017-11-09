FROM python:3.6-alpine

LABEL maintainer "Wellcome Digital Platform <wellcomedigitalplatform@wellcome.ac.uk>"
LABEL description "Copy records from the Sierra API into DynamoDB"

COPY requirements.txt /

RUN apk add --no-cache gcc musl-dev openssl-dev libffi-dev

RUN pip3 install -r /requirements.txt

COPY src /app

ENTRYPOINT ["/app/sierra_objects_to_s3.py"]
