FROM python:3.6

VOLUME ["/data"]

RUN pip install flake8

COPY run.sh /app/run.sh

ENTRYPOINT /app/run.sh
