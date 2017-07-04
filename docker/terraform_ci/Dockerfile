FROM hashicorp/terraform:0.9.8

VOLUME ["/data"]

RUN apk --no-cache update && \
    apk --no-cache add bash python py-pip py-setuptools ca-certificates curl groff less zip git && \
    pip --no-cache-dir install awscli && \
    rm -rf /var/cache/apk/*

COPY is_up_to_date_with_master.py /app/is_up_to_date_with_master.py
COPY run.sh /app/run.sh

WORKDIR /data/terraform

ENTRYPOINT /app/run.sh
