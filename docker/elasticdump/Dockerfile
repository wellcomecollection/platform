FROM alpine

RUN apk update
RUN apk add nodejs-npm python3
RUN npm install -g elasticdump
RUN pip3 install awscli

COPY run_elasticdump.sh /run_elasticdump.sh
CMD ["/run_elasticdump.sh"]
