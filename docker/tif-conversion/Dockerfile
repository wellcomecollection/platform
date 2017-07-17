FROM ubuntu:14.04

RUN apt-get update
RUN apt-get install -y python python-pip libyaml-dev
RUN apt-get install -y python-dev
RUN pip install awscli
RUN apt-get install -y jq

COPY libkdu_v74R.so /usr/lib/libkdu_v74R.so
COPY kdu_expand /usr/local/bin/kdu_expand

COPY run.sh /run.sh

CMD ["/run.sh"]
