FROM python:3.6-alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Copy records from the Sierra API into DynamoDB"

COPY requirements.txt /
RUN pip3 install -r /requirements.txt

COPY src /app

ENTRYPOINT ["/app/run.py"]
