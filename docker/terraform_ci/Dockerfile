FROM hashicorp/terraform:0.10.6

RUN apk update && apk add git jq python3
RUN pip3 install awscli

COPY plan.sh /app/plan.sh
COPY run.sh /app/run.sh
COPY notify.sh /app/notify.sh

VOLUME ["/data"]
WORKDIR /data

ENTRYPOINT /app/run.sh
