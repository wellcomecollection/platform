FROM python:3.6

VOLUME ["/data"]

COPY src /app

RUN pip install -r /app/requirements.txt

WORKDIR /data

ENTRYPOINT /app/run.sh
