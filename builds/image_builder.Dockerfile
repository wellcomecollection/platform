FROM alpine

LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "A Docker image for building Docker images for the Wellcome Digital Platform"

RUN apk update
RUN apk add docker git python3

RUN pip3 install docopt boto3

COPY . /builds

VOLUME /repo
WORKDIR /repo

ENTRYPOINT ["python3", "/builds/build_docker_image.py"]
