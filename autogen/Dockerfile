FROM alpine
LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Code autogen for the Wellcome Digital Platform"

RUN apk update
RUN apk add git python3

COPY requirements.txt /requirements.txt
RUN pip3 install -r requirements.txt

VOLUME /repo
WORKDIR /repo

COPY run_autogen.py /run_autogen.py
COPY autogen_helpers /autogen_helpers

ENTRYPOINT ["/run_autogen.py"]
