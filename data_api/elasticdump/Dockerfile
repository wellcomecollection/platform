FROM alpine

RUN apk update && apk add jq nodejs python3
RUN pip3 install awscli
RUN npm install -g elasticdump

COPY run_elasticdump.sh /
CMD ["/run_elasticdump.sh"]
