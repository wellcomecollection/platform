FROM python:3.6

VOLUME ["/data"]

RUN pip install flake8

RUN pip install boto3

RUN pip install pytest

RUN pip install moto==1.0.1

RUN pip install simplejson

COPY run.sh /app/run.sh

ENTRYPOINT /app/run.sh
