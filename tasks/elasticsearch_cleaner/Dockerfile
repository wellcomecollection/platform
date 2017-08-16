FROM alpine:edge

RUN apk update
RUN apk add ca-certificates python3

COPY requirements.txt /requirements.txt
RUN pip3 install --requirement /requirements.txt

ENV BUCKET platform-infra
ENV KEY terraform.tfvars
ENV DRY_RUN false

COPY elasticsearch_cleaner.py /elasticsearch_cleaner.py
COPY run.sh /run.sh

CMD ["/run.sh"]
