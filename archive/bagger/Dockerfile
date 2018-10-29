FROM alpine:3.6

RUN apk add --update --no-cache --virtual=run-deps \
  python3 \
  ca-certificates

# Need to get env vars in ????
ENV EXAMPLE_VARIABLE example_value

WORKDIR /opt/app

COPY run.sh /opt/app/
RUN chmod +x /opt/app/run.sh

COPY src/requirements.txt /opt/app/
RUN pip3 install --no-cache-dir -r /opt/app/requirements.txt

COPY src /opt/app/

CMD ["/opt/app/run.sh"]