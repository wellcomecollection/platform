FROM alpine

RUN apk update && \
    apk add python3 py3-cffi py3-cryptography py3-markupsafe py3-yaml

RUN pip3 freeze

COPY requirements.txt /
RUN pip3 install -r /requirements.txt

WORKDIR /data
VOLUME ["/data"]

COPY src /app

ENTRYPOINT /app/run.sh
