FROM alpine
LABEL maintainer "Alex Chan <a.chan@wellcome.ac.uk>"
LABEL description "Copy records from Miro XML dumps into DynamoDB"

RUN apk update
RUN apk add python3

# Required for lxml
RUN apk add build-base libxml2-dev libxslt-dev python3 python3-dev

COPY requirements.txt /requirements.txt
RUN pip3 install -r requirements.txt

COPY . /miro_adapter

CMD ["/miro_adapter/run.sh"]
