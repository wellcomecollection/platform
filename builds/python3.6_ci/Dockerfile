FROM python:3.6

COPY requirements.txt /requirements.txt
RUN pip install -r /requirements.txt

WORKDIR /data
VOLUME ["/data"]

COPY src /app

ENTRYPOINT /app/run.sh
